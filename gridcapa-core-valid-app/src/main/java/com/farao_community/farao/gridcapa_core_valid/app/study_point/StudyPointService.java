/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidRaoException;
import com.farao_community.farao.gridcapa_core_valid.app.CoreAreasId;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResultService;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetPositionsHandler;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.AsynchronousRaoRunnerClient;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class StudyPointService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyPointService.class);
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    private final MinioAdapter minioAdapter;
    private final AsynchronousRaoRunnerClient asynchronousRaoRunnerClient;
    private final LimitingBranchResultService limitingBranchResultService;
    private final FileExporter fileExporter;
    private final Logger eventsLogger;

    public StudyPointService(MinioAdapter minioAdapter, AsynchronousRaoRunnerClient asynchronousRaoRunnerClient, LimitingBranchResultService limitingBranchResultService, FileExporter fileExporter, Logger eventsLogger) {
        this.minioAdapter = minioAdapter;
        this.asynchronousRaoRunnerClient = asynchronousRaoRunnerClient;
        this.limitingBranchResultService = limitingBranchResultService;
        this.fileExporter = fileExporter;
        this.eventsLogger = eventsLogger;
    }

    public RaoRequest computeStudyPointShift(StudyPoint studyPoint, StudyPointData studyPointData, OffsetDateTime timestamp, String coreValidRequesttId) {
        LOGGER.info("Running computation for study point {} ", studyPoint.getVerticeId());
        Network network = studyPointData.getNetwork();
        ZonalData<Scalable> scalableZonalData = studyPointData.getScalableZonalData();
        Map<String, Double> coreNetPositions = studyPointData.getCoreNetPositions();
        String jsonCracUrl = studyPointData.getJsonCracUrl();
        String raoParametersUrl = studyPointData.getRaoParametersUrl();
        RaoRequest raoRequest = null;
        String initialVariant = network.getVariantManager().getWorkingVariantId();
        String newVariant = initialVariant + "_" + studyPoint.getVerticeId();
        network.getVariantManager().cloneVariant(initialVariant, newVariant);
        network.getVariantManager().setWorkingVariant(newVariant);
        try {
            Map<String, InitGenerator> initGenerators = setPminPmaxToDefaultValue(network, scalableZonalData);
            NetPositionsHandler.shiftNetPositionToStudyPoint(network, studyPoint, scalableZonalData, coreNetPositions);
            resetInitialPminPmax(network, scalableZonalData, initGenerators);
            String shiftedCgmUrl = fileExporter.saveShiftedCgm(network, studyPoint);
            studyPoint.getStudyPointResult().setShiftedCgmUrl(shiftedCgmUrl);
            String raoDirPath = String.format("%s/artifacts/RAO-%s-%s/", minioAdapter.getProperties().getBasePath(), timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'_'HH-mm")), studyPoint.getVerticeId());
            // For rao logs dispatcher, the rao request should correspond to the core valid request
            raoRequest = new RaoRequest.RaoRequestBuilder()
                    .withId(coreValidRequesttId)
                    .withNetworkFileUrl(shiftedCgmUrl)
                    .withCracFileUrl(jsonCracUrl)
                    .withRaoParametersFileUrl(raoParametersUrl)
                    .withResultsDestination(raoDirPath)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error during study point {} computation", studyPoint.getVerticeId(), e);
            studyPoint.getStudyPointResult().setStatus(StudyPointResult.Status.ERROR);
        } finally {
            network.getVariantManager().setWorkingVariant(initialVariant);
            network.getVariantManager().removeVariant(newVariant);
        }
        return raoRequest;
    }

    public CompletableFuture<RaoResponse> computeStudyPointRao(StudyPoint studyPoint, RaoRequest raoRequest) {
        eventsLogger.info("Running RAO for studypoint {} ...", studyPoint.getVerticeId());
        try {
            return asynchronousRaoRunnerClient.runRaoAsynchronously(raoRequest);
        } catch (Exception e) {
            String message = String.format("Error during RAO %s: %s", studyPoint.getVerticeId(), e.getMessage());
            eventsLogger.error(message);
            throw new CoreValidRaoException(message, e);
        }
    }

    public StudyPointResult postTreatRaoResult(StudyPoint studyPoint, StudyPointData studyPointData, RaoResponse raoResponse) {
        List<LimitingBranchResult> limitingBranchResults = limitingBranchResultService.importRaoResult(studyPoint, studyPointData.getFbConstraintCreationContext(), raoResponse.getRaoResultFileUrl());
        setSuccessResult(studyPoint, raoResponse, limitingBranchResults);
        return studyPoint.getStudyPointResult();
    }

    private Map<String, InitGenerator> setPminPmaxToDefaultValue(Network network, ZonalData<Scalable> scalableZonalData) {
        Map<String, InitGenerator> initGenerators = new HashMap<>();
        CoreAreasId.getCountriesId().stream()
                .map(zone -> new CountryEICode(Country.valueOf(zone)).getCode())
                .map(scalableZonalData::getData)
                .filter(Objects::nonNull)
                .map(scalable -> scalable.filterInjections(network).stream()
                        .filter(Generator.class::isInstance)
                        .map(Generator.class::cast)
                        .collect(Collectors.toList()))
                .forEach(generators -> generators.forEach(generator -> {
                    if (Double.isNaN(generator.getTargetP())) {
                        generator.setTargetP(0.);
                    }
                    InitGenerator initGenerator = new InitGenerator();
                    initGenerator.setpMin(generator.getMinP());
                    initGenerator.setpMax(generator.getMaxP());
                    initGenerators.put(generator.getId(), initGenerator);
                    generator.setMinP(DEFAULT_PMIN);
                    generator.setMaxP(DEFAULT_PMAX);
                }));
        LOGGER.info("Pmax and Pmin are set to default values for network {}", network.getNameOrId());
        return initGenerators;
    }

    private void resetInitialPminPmax(Network network, ZonalData<Scalable> scalableZonalData, Map<String, InitGenerator> initGenerators) {
        CoreAreasId.getCountriesId().forEach(zone -> {
            String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
            Scalable scalable = scalableZonalData.getData(zoneEiCode);
            if (scalable != null) {
                List<Generator> generators = scalable.filterInjections(network).stream()
                        .filter(Generator.class::isInstance)
                        .map(Generator.class::cast)
                        .collect(Collectors.toList());

                generators.forEach(generator -> {
                    generator.setMaxP(Math.max(generator.getTargetP(), initGenerators.get(generator.getId()).getpMax()));
                    generator.setMinP(Math.min(generator.getTargetP(), initGenerators.get(generator.getId()).getpMin()));
                });
            }
        });
        LOGGER.info("Pmax and Pmin are reset to initial values for network {}", network.getNameOrId());
    }

    private void setSuccessResult(StudyPoint studyPoint, RaoResponse raoResponse, List<LimitingBranchResult> limitingBranchResults) {
        StudyPointResult result = studyPoint.getStudyPointResult();
        result.setListLimitingBranchResult(limitingBranchResults);
        result.setStatus(StudyPointResult.Status.SUCCESS);
        result.setNetworkWithPraUrl(raoResponse.getNetworkWithPraFileUrl());
        result.setRaoResultFileUrl(raoResponse.getRaoResultFileUrl());
        result.setPeriod(String.valueOf(studyPoint.getPeriod()));
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
