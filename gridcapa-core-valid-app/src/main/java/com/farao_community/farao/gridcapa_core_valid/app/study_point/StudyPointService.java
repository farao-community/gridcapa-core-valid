/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.commons.CountryEICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.app.CoreAreasId;
import com.farao_community.farao.gridcapa_core_valid.app.MinioAdapter;
import com.farao_community.farao.gridcapa_core_valid.app.NetworkHandler;
import com.farao_community.farao.gridcapa_core_valid.app.net_position.NetPositionsHandler;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class StudyPointService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyPointService.class);
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    public static final String ARTIFACTS_S = "artifacts/%s";
    private final MinioAdapter minioAdapter;

    public StudyPointService(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public StudyPointResult computeStudyPoint(StudyPoint studyPoint, Network network, ZonalData<Scalable> scalableZonalData, Map<String, Double> coreNetPositions) {
        LOGGER.info("Running computation for study point {} ", studyPoint.getId());
        StudyPointResult result = new StudyPointResult(studyPoint.getId());
        String initialVariant = network.getVariantManager().getWorkingVariantId();
        String newVariant = initialVariant + "_" + studyPoint.getId();
        network.getVariantManager().cloneVariant(initialVariant, newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        try {
            Map<String, InitGenerator> initGenerators = setPminPmaxToDefaultValue(network, scalableZonalData);
            NetPositionsHandler.shiftNetPositionToStudyPoint(network, studyPoint, scalableZonalData, coreNetPositions);
            resetInitialPminPmax(network, scalableZonalData, initGenerators);
            String url = saveShiftedCgm(network, studyPoint);
            result.setStatus(StudyPointResult.Status.SUCCESS);
            result.setShiftedCgmUrl(url);
        } catch (Exception e) {
            LOGGER.error("Error during study point {} computation", studyPoint.getId(), e);
            result.setStatus(StudyPointResult.Status.ERROR);
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariant);
            network.getVariantManager().removeVariant(newVariant);
        }
        return result;
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