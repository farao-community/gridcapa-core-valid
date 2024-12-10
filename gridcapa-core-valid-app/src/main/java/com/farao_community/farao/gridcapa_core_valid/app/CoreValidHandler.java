/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidRaoException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointData;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracio.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class CoreValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private static final DateTimeFormatter ARTIFACTS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm");

    private final Logger eventsLogger;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final MinioAdapter minioAdapter;
    private final StudyPointService studyPointService;

    public CoreValidHandler(StudyPointService studyPointService, FileImporter fileImporter, FileExporter fileExporter, MinioAdapter minioAdapter, Logger eventsLogger) {
        this.studyPointService = studyPointService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.eventsLogger = eventsLogger;
    }

    public String handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        final String formattedTimestamp = setUpEventLogging(coreValidRequest);

        try {
            Network network = fileImporter.importNetwork(coreValidRequest.getCgm());
            FbConstraintCreationContext cracCreationContext = fileImporter.importCrac(coreValidRequest.getCbcora().getUrl(), coreValidRequest.getTimestamp(), network);

            List<StudyPointResult> studyPointResults = computeStudyPoints(coreValidRequest, network, cracCreationContext, formattedTimestamp);

            postTreatment(studyPointResults, coreValidRequest, cracCreationContext, formattedTimestamp);
            return coreValidRequest.getId();
        } catch (InterruptedException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            Thread.currentThread().interrupt();
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        } catch (ExecutionException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        }
    }

    private static String setUpEventLogging(CoreValidRequest coreValidRequest) {
        MDC.put("gridcapa-task-id", coreValidRequest.getId());
        return TIMESTAMP_FORMATTER.format(coreValidRequest.getTimestamp());
    }

    private List<StudyPointResult> computeStudyPoints(CoreValidRequest coreValidRequest, Network network, FbConstraintCreationContext cracCreationContext, String formattedTimestamp) throws InterruptedException, ExecutionException {
        Map<StudyPoint, RaoRequest> studyPointRaoRequests = new HashMap<>();
        Map<StudyPoint, CompletableFuture<AbstractRaoResponse>> studyPointCompletableFutures = new HashMap<>();
        List<StudyPointResult> studyPointResults = new ArrayList<>();

        List<StudyPoint> studyPoints = fileImporter.importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
        if (!studyPoints.isEmpty()) {
            StudyPointData studyPointData = fillStudyPointData(coreValidRequest, network, cracCreationContext);
            studyPoints.forEach(studyPoint -> studyPointRaoRequests.put(studyPoint, studyPointService.computeStudyPointShift(studyPoint, studyPointData, coreValidRequest.getTimestamp(), coreValidRequest.getId(), coreValidRequest.getCurrentRunId())));
            eventsLogger.info("All studypoints shifts are done for timestamp {}", formattedTimestamp);
            runRaoForEachStudyPoint(studyPointRaoRequests, studyPointCompletableFutures);
            studyPointResults = fillResultsForEachStudyPoint(studyPointData, studyPointCompletableFutures);
        }
        return studyPointResults;
    }

    private StudyPointData fillStudyPointData(CoreValidRequest coreValidRequest, Network network, FbConstraintCreationContext cracCreationContext) {
        ReferenceProgram referenceProgram = fileImporter.importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = fileImporter.importGlskFile(coreValidRequest.getGlsk());
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
        String jsonCracUrl = fileExporter.saveCracInJsonFormat(cracCreationContext.getCrac(), coreValidRequest.getTimestamp());
        RaoParameters raoParameters = RaoParameters.load();
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        return new StudyPointData(network, coreNetPositions, scalableZonalData, cracCreationContext, jsonCracUrl, raoParametersUrl);
    }

    private void runRaoForEachStudyPoint(Map<StudyPoint, RaoRequest> studyPointRaoRequests, Map<StudyPoint, CompletableFuture<AbstractRaoResponse>> studyPointCompletableFutures) throws ExecutionException, InterruptedException {
        studyPointRaoRequests.forEach((studyPoint, raoRequest) -> {
            CompletableFuture<AbstractRaoResponse> futureRaoResponse = studyPointService.computeStudyPointRao(studyPoint, raoRequest);
            studyPointCompletableFutures.put(studyPoint, futureRaoResponse);
            futureRaoResponse.thenApply(raoResponse -> {
                LOGGER.info("End of RAO for studypoint {} ...", studyPoint.getVerticeId());
                return null;
            }).exceptionally(exception -> {
                studyPoint.getStudyPointResult().setStatusToError();
                final String message = String.format("Error during RAO computation for studypoint %s.", studyPoint.getVerticeId());
                eventsLogger.error(message);
                throw new CoreValidRaoException(message);
            });
        });
        CompletableFuture.allOf(studyPointCompletableFutures.values().toArray(new CompletableFuture[0])).get();
    }

    List<StudyPointResult> fillResultsForEachStudyPoint(StudyPointData studyPointData, Map<StudyPoint, CompletableFuture<AbstractRaoResponse>> studyPointCompletableFutures) throws InterruptedException, ExecutionException {
        List<StudyPointResult> studyPointResults = new ArrayList<>();
        for (Map.Entry<StudyPoint, CompletableFuture<AbstractRaoResponse>> entry : studyPointCompletableFutures.entrySet()) {
            StudyPoint studyPoint = entry.getKey();

            final AbstractRaoResponse abstractRaoResponse = entry.getValue().get();
            if (abstractRaoResponse.isRaoFailed()) {
                RaoFailureResponse failureResponse = (RaoFailureResponse) abstractRaoResponse;
                final String message = String.format("Error during RAO computation for studypoint %s: %s.", studyPoint.getVerticeId(), failureResponse.getErrorMessage());
                eventsLogger.error(message);
                throw new CoreValidRaoException(message);
            }
            RaoSuccessResponse raoResponse = (RaoSuccessResponse) abstractRaoResponse;
            Network networkWithPra = fileImporter.importNetworkFromUrl(raoResponse.getNetworkWithPraFileUrl());
            String fileName = networkWithPra.getNameOrId() + "_" + studyPoint.getVerticeId() + "_withPra.uct";
            fileExporter.saveShiftedCgmWithPra(networkWithPra, fileName);
            studyPointResults.add(studyPointService.postTreatRaoResult(studyPoint, studyPointData, raoResponse));
        }
        return studyPointResults;
    }

    private void postTreatment(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest, FbConstraintCreationContext cracCreationContext, String formattedTimestamp) {
        saveProcessOutputs(studyPointResults, coreValidRequest, cracCreationContext);
        if (coreValidRequest.getLaunchedAutomatically()) {
            deleteArtifacts(coreValidRequest);
        }
        eventsLogger.info("Process done for timestamp {}.", formattedTimestamp);
    }

    private void saveProcessOutputs(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest, FbConstraintCreationContext cracCreationContext) {
        fileExporter.exportStudyPointResult(studyPointResults, coreValidRequest, cracCreationContext);
    }

    private void deleteArtifacts(CoreValidRequest coreValidRequest) {
        deleteCgmBeforeRao(ARTIFACTS_FORMATTER.format(coreValidRequest.getTimestamp().atZoneSameInstant(ZoneId.of("Europe/Paris"))));
    }

    private void deleteCgmBeforeRao(String prefix) {
        List<String> results = minioAdapter.listFiles("artifacts/" + prefix);
        minioAdapter.deleteFiles(results);
    }
}
