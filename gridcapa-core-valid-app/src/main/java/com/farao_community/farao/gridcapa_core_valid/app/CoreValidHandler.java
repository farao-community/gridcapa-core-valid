/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.exception.CoreValidRaoException;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.SearchTreeRaoConfiguration;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.MinioAdapter;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.ResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointData;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class CoreValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private final Logger eventsLogger;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final MinioAdapter minioAdapter;
    private final SearchTreeRaoConfiguration searchTreeRaoConfiguration;
    private final StudyPointService studyPointService;
    private final DateTimeFormatter artifactsFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm");
    private final Map<StudyPoint, CompletableFuture<RaoResponse>> studyPointCompletableFutures = new HashMap<>();
    private final Map<StudyPoint, RaoRequest> studyPointRaoRequests = new HashMap<>();
    private final List<StudyPointResult> studyPointResults = new ArrayList<>();
    private Instant computationStartInstant;
    private Instant computationEndInstant;
    private String formattedTimestamp;
    private List<StudyPoint> studyPoints;
    private Map<ResultFileExporter.ResultType, String> resultFileUrls;

    public CoreValidHandler(StudyPointService studyPointService, FileImporter fileImporter, FileExporter fileExporter, SearchTreeRaoConfiguration searchTreeRaoConfiguration, MinioAdapter minioAdapter, Logger eventsLogger) {
        this.studyPointService = studyPointService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.searchTreeRaoConfiguration = searchTreeRaoConfiguration;
        this.minioAdapter = minioAdapter;
        this.eventsLogger = eventsLogger;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        try {
            preTreatment(coreValidRequest);
            computeStudyPoints(coreValidRequest);
            postTreatment(coreValidRequest);
        } catch (InterruptedException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            Thread.currentThread().interrupt();
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        } catch (ExecutionException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        }
        return new CoreValidResponse(coreValidRequest.getId(), resultFileUrls.get(ResultFileExporter.ResultType.MAIN_RESULT), resultFileUrls.get(ResultFileExporter.ResultType.REX_RESULT), resultFileUrls.get(ResultFileExporter.ResultType.REMEDIAL_ACTIONS_RESULT), computationStartInstant, computationEndInstant);
    }

    private void preTreatment(CoreValidRequest coreValidRequest) {
        clearData();
        setUpEventLogging(coreValidRequest);
        computationStartInstant = Instant.now();
    }

    private void clearData() {
        if (studyPoints != null) {
            studyPoints.clear();
        }
        if (resultFileUrls != null) {
            resultFileUrls.clear();
        }
        studyPointCompletableFutures.clear();
        studyPointRaoRequests.clear();
        studyPointResults.clear();
    }

    private void setUpEventLogging(CoreValidRequest coreValidRequest) {
        MDC.put("gridcapa-task-id", coreValidRequest.getId());
        formattedTimestamp = timestampFormatter.format(coreValidRequest.getTimestamp());
    }

    private void computeStudyPoints(CoreValidRequest coreValidRequest) throws InterruptedException, ExecutionException {
        importStudyPoints(coreValidRequest);
        if (!studyPoints.isEmpty()) {
            StudyPointData studyPointData = fillStudyPointData(coreValidRequest);
            studyPoints.forEach(studyPoint -> studyPointRaoRequests.put(studyPoint, studyPointService.computeStudyPointShift(studyPoint, studyPointData, coreValidRequest.getTimestamp(), coreValidRequest.getId())));
            eventsLogger.info("All studypoints shifts are done for timestamp {}", formattedTimestamp);
            runRaoForEachStudyPoint();
            fillResultsForEachStudyPoint(studyPointData);
        }
    }

    private void importStudyPoints(CoreValidRequest coreValidRequest) {
        studyPoints = fileImporter.importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
    }

    private StudyPointData fillStudyPointData(CoreValidRequest coreValidRequest) {
        Network network = fileImporter.importNetwork(coreValidRequest.getCgm());
        ReferenceProgram referenceProgram = fileImporter.importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = fileImporter.importGlskFile(coreValidRequest.getGlsk());
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
        FbConstraintCreationContext cracCreationContext = fileImporter.importCrac(coreValidRequest.getCbcora().getUrl(), coreValidRequest.getTimestamp(), network);
        String jsonCracUrl = fileExporter.saveCracInJsonFormat(cracCreationContext.getCrac(), coreValidRequest.getTimestamp());
        RaoParameters raoParameters = getRaoParameters();
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        return new StudyPointData(network, coreNetPositions, scalableZonalData, cracCreationContext, jsonCracUrl, raoParametersUrl);
    }

    private RaoParameters getRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);

        addParametersToSearchTreeRao(searchTreeRaoParameters);

        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        return raoParameters;
    }

    private void addParametersToSearchTreeRao(SearchTreeRaoParameters searchTreeRaoParameters) {
        searchTreeRaoParameters.setMaxCurativePstPerTso(searchTreeRaoConfiguration.getMaxCurativePstPerTso());
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(searchTreeRaoConfiguration.getMaxCurativeTopoPerTso());
        searchTreeRaoParameters.setMaxCurativeRaPerTso(searchTreeRaoConfiguration.getMaxCurativeRaPerTso());
    }

    private void runRaoForEachStudyPoint() throws ExecutionException, InterruptedException {
        studyPointRaoRequests.forEach((studyPoint, raoRequest) -> {
            CompletableFuture<RaoResponse> raoResponse = studyPointService.computeStudyPointRao(studyPoint, raoRequest);
            studyPointCompletableFutures.put(studyPoint, raoResponse);
            raoResponse.thenApply(raoResponse1 -> {
                eventsLogger.info("End of RAO computation for studypoint {} .", studyPoint.getVerticeId());
                return null;
            }).exceptionally(exception -> {
                studyPoint.getStudyPointResult().setStatusToError();
                eventsLogger.error("Error during RAO computation for studypoint {}.", studyPoint.getVerticeId());
                throw new CoreValidRaoException(String.format("Error during RAO computation for studypoint %s .", studyPoint.getVerticeId()));
            });
        });
        CompletableFuture.allOf(studyPointCompletableFutures.values().toArray(new CompletableFuture[0])).get();
    }

    private void fillResultsForEachStudyPoint(StudyPointData studyPointData) throws InterruptedException, ExecutionException {
        for (Map.Entry<StudyPoint, CompletableFuture<RaoResponse>> entry : studyPointCompletableFutures.entrySet()) {
            StudyPoint studyPoint = entry.getKey();
            RaoResponse raoResponse = entry.getValue().get();
            studyPointResults.add(studyPointService.postTreatRaoResult(studyPoint, studyPointData, raoResponse));
        }
    }

    private void postTreatment(CoreValidRequest coreValidRequest) {
        computationEndInstant = Instant.now();
        resultFileUrls = saveProcessOutputs(studyPointResults, coreValidRequest);
        if (coreValidRequest.getLaunchedAutomatically()) {
            deleteArtifacts(coreValidRequest);
        }
        eventsLogger.info("Process done for timestamp {}.", formattedTimestamp);
    }

    private Map<ResultFileExporter.ResultType, String> saveProcessOutputs(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest) {
        return fileExporter.exportStudyPointResult(studyPointResults, coreValidRequest);
    }

    private void deleteArtifacts(CoreValidRequest coreValidRequest) {
        deleteCgmBeforeRao(artifactsFormatter.format(coreValidRequest.getTimestamp().atZoneSameInstant(ZoneId.of("Europe/Paris"))));
        deleteCgmAfterRao("RAO");
    }

    private void deleteCgmBeforeRao(String prefix) {
        Iterable<Result<Item>> results = listMinioArtifactsStartingWith(prefix);
        minioAdapter.deleteObjects(results);
    }

    private void deleteCgmAfterRao(String prefix) {
        Iterable<Result<Item>> results = listMinioArtifactsStartingWith(prefix);
        List<Result<Item>> listObjectsToDelete = filterMinioObjectsOnName(results);
        minioAdapter.deleteObjects(listObjectsToDelete);
    }

    private Iterable<Result<Item>> listMinioArtifactsStartingWith(String prefix) {
        return minioAdapter.listArtifacts(prefix);
    }

    private List<Result<Item>> filterMinioObjectsOnName(Iterable<Result<Item>> results) {
        List<Result<Item>> collect = new ArrayList<>();

        try {
            collect = StreamSupport.stream(results.spliterator(), false)
                    .filter(res -> {
                        try {
                            return res.get().objectName().equals("networkWithPRA.xiidm");
                        } catch (Exception e) {
                            LOGGER.error("Cant get the name of the Minio file");
                        }
                        return false;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error("Cant get the name of the Minio file");
        }

        return collect;
    }
}
