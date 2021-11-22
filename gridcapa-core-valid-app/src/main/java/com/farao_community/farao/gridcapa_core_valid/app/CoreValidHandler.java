/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.commons.CountryEICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.gridcapa_core_valid.app.net_position.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.powsybl.action.util.Scalable;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private final MinioAdapter minioAdapter;
    private final UrlValidationService urlValidationService;
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    public static final String ARTIFACTS_S = "artifacts/%s";

    public CoreValidHandler(MinioAdapter minioAdapter, UrlValidationService urlValidationService) {
        this.minioAdapter = minioAdapter;
        this.urlValidationService = urlValidationService;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        InputStream networkStream = urlValidationService.openUrlStream(coreValidRequest.getCgm().getUrl());
        Network network = NetworkHandler.loadNetwork(coreValidRequest.getCgm().getFilename(), networkStream);
        ReferenceProgram referenceProgram = importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = importGlskFile(coreValidRequest.getGlsk());
        List<StudyPoint> studyPoints = importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
        Map<String, InitGenerator> initGenerators = setPminPmaxToDefaultValue(network, scalableZonalData);
        studyPoints.forEach(studyPoint -> computeStudyPoint(studyPoint, network, scalableZonalData, coreNetPositions, initGenerators));
        return new CoreValidResponse(coreValidRequest.getId());
    }

    GlskDocument importGlskFile(CoreValidFileResource glskFileResource) {
        try (InputStream glskStream = urlValidationService.openUrlStream(glskFileResource.getUrl())) {
            LOGGER.info("Import of Glsk file {} ", glskFileResource.getFilename());
            return GlskDocumentImporters.importGlsk(glskStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", glskFileResource.getUrl()), e);
        }
    }

    private void computeStudyPoint(StudyPoint studyPoint, Network network, ZonalData<Scalable> scalableZonalData, Map<String, Double> coreNetPositions, Map<String, InitGenerator> initGenerators) {
        LOGGER.info("Running computation for study point {} ", studyPoint.getId());
        NetPositionsHandler.shiftNetPositionToStudyPoint(network, studyPoint, scalableZonalData, coreNetPositions);
        resetInitialPminPmax(network, scalableZonalData, initGenerators);
        saveShiftedCgm(network, studyPoint);
    }

    ReferenceProgram importReferenceProgram(CoreValidFileResource refProgFile, OffsetDateTime timestamp) {
        try (InputStream refProgStream = urlValidationService.openUrlStream(refProgFile.getUrl())) {
            return RefProgImporter.importRefProg(refProgStream, timestamp);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", refProgFile.getUrl()), e);
        }
    }

    private List<StudyPoint> importStudyPoints(CoreValidFileResource studyPointsFileResource, OffsetDateTime timestamp) {
        try (InputStream studyPointsStream = urlValidationService.openUrlStream(studyPointsFileResource.getUrl())) {
            LOGGER.info("Import of study points from {} file for timestamp {} ", studyPointsFileResource.getFilename(), timestamp);
            return StudyPointsImporter.importStudyPoints(studyPointsStream, timestamp);
        } catch (Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download study points file from URL '%s'", studyPointsFileResource.getUrl()), e);
        }
    }

    private String saveShiftedCgm(Network network, StudyPoint studyPoint) {
        String fileName = network.getNameOrId() + "_" + studyPoint.getId() + ".uct";
        String networkPath = String.format(ARTIFACTS_S, fileName);
        MemDataSource memDataSource = new MemDataSource();
        NetworkHandler.removeAlegroVirtualGeneratorsFromNetwork(network);
        Exporters.export("UCTE", network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", "uct")) {
            LOGGER.info("Uploading shifted cgm to {}", networkPath);
            minioAdapter.uploadFile(networkPath, is);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to save shifted network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }

    private Map<String, InitGenerator> setPminPmaxToDefaultValue(Network network, ZonalData<Scalable> scalableZonalData) {
        Map<String, InitGenerator> initGenerators = new HashMap<>();
        CoreAreasId.getCountriesId().forEach(zone -> {
            String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
            Scalable scalable = scalableZonalData.getData(zoneEiCode);
            if (scalable != null) {
                List<Generator> generators = scalable.filterInjections(network).stream()
                        .filter(injection -> injection instanceof Generator)
                        .map(injection -> (Generator) injection)
                        .collect(Collectors.toList());

                generators.forEach(generator -> {
                    if (Double.isNaN(generator.getTargetP())) {
                        generator.setTargetP(0.);
                    }
                    InitGenerator initGenerator = new InitGenerator();
                    initGenerator.setpMin(generator.getMinP());
                    initGenerator.setpMax(generator.getMaxP());
                    initGenerators.put(generator.getId(), initGenerator);
                    generator.setMinP(DEFAULT_PMIN);
                    generator.setMaxP(DEFAULT_PMAX);
                });
            }
        });
        LOGGER.info("Pmax and Pmin are set to default values for network {}", network.getNameOrId());
        return initGenerators;
    }

    private void resetInitialPminPmax(Network network, ZonalData<Scalable> scalableZonalData, Map<String, InitGenerator> initGenerators) {
        CoreAreasId.getCountriesId().forEach(zone -> {
            String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
            Scalable scalable = scalableZonalData.getData(zoneEiCode);
            if (scalable != null) {
                List<Generator> generators = scalable.filterInjections(network).stream()
                        .filter(injection -> injection instanceof Generator)
                        .map(injection -> (Generator) injection)
                        .collect(Collectors.toList());

                generators.forEach(generator -> {
                    generator.setMaxP(Math.max(generator.getTargetP(), initGenerators.get(generator.getId()).getpMax()));
                    generator.setMinP(Math.min(generator.getTargetP(), initGenerators.get(generator.getId()).getpMin()));
                });
            }
        });
        LOGGER.info("Pmax and Pmin are reset to initial values for network {}", network.getNameOrId());
    }

    private static class InitGenerator {
        double pMin;
        double pMax;

        public double getpMin() {
            return pMin;
        }

        public void setpMin(double pMin) {
            this.pMin = pMin;
        }

        public double getpMax() {
            return pMax;
        }

        public void setpMax(double pMax) {
            this.pMax = pMax;
        }
    }
}
