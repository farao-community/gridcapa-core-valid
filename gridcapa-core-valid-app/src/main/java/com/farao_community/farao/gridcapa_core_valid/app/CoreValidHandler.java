/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import com.farao_community.farao.gridcapa_core_valid.app.services.*;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.ResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointData;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoParameters;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private final StudyPointService studyPointService;
    private final FileImporter fileImporter;
    private final FileExporter fileExporter;
    private final SearchTreeRaoConfiguration searchTreeRaoConfiguration;

    public CoreValidHandler(StudyPointService studyPointService, FileImporter fileImporter, FileExporter fileExporter, SearchTreeRaoConfiguration searchTreeRaoConfiguration) {
        this.studyPointService = studyPointService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.searchTreeRaoConfiguration = searchTreeRaoConfiguration;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        try {
            Map<StudyPoint, CompletableFuture<RaoResponse>> studyPointCompletableFutures = new HashMap<>();
            Instant computationStartInstant = Instant.now();
            List<StudyPoint> studyPoints = fileImporter.importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
            Map<StudyPoint, RaoRequest> studyPointRaoRequests = new HashMap<>();
            List<StudyPointResult> studyPointResults = new ArrayList<>();
            if (!studyPoints.isEmpty()) {
                StudyPointData studyPointData = fillStudyPointData(coreValidRequest);
                studyPoints.forEach(studyPoint -> studyPointRaoRequests.put(studyPoint, studyPointService.computeStudyPointShift(studyPoint, studyPointData)));
                studyPointRaoRequests.forEach((studyPoint, raoRequest) -> {
                    CompletableFuture<RaoResponse> raoResponse = studyPointService.computeStudyPointRao(studyPoint, raoRequest);
                    studyPointCompletableFutures.put(studyPoint, raoResponse);
                    raoResponse.thenApply(raoResponse1 -> {
                        LOGGER.info("End of RAO computation for studypoint {} .", studyPoint.getVerticeId());
                        return null;
                    }
                    )
                            .exceptionally(exception -> {
                                studyPoint.getStudyPointResult().setStatusToError();
                                throw new CoreValidRaoException(String.format("Error during RAO computation for studypoint %s .", studyPoint.getVerticeId()));
                            });
                });
                CompletableFuture.allOf(studyPointCompletableFutures.values().toArray(new CompletableFuture[0])).get();
                for (Map.Entry<StudyPoint, CompletableFuture<RaoResponse>> entry : studyPointCompletableFutures.entrySet()) {
                    StudyPoint studyPoint = entry.getKey();
                    RaoResponse raoResponse = entry.getValue().get();
                    studyPointResults.add(studyPointService.postTreatRaoResult(studyPoint, studyPointData, raoResponse));
                }
            }
            Instant computationEndInstant = Instant.now();
            Map<ResultFileExporter.ResultType, String> resultFileUrls = saveProcessOutputs(studyPointResults, coreValidRequest);
            return new CoreValidResponse(coreValidRequest.getId(), resultFileUrls.get(ResultFileExporter.ResultType.MAIN_RESULT), resultFileUrls.get(ResultFileExporter.ResultType.REX_RESULT), computationStartInstant, computationEndInstant);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        } catch (ExecutionException e) {
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        }
    }

    private StudyPointData fillStudyPointData(CoreValidRequest coreValidRequest) {
        Network network = fileImporter.importNetwork(coreValidRequest.getCgm().getFilename(), coreValidRequest.getCgm().getUrl());
        ReferenceProgram referenceProgram = fileImporter.importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = fileImporter.importGlskFile(coreValidRequest.getGlsk());
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
        FbConstraintCreationContext cracCreationContext = fileImporter.importCrac(coreValidRequest.getCbcora().getUrl(), coreValidRequest.getTimestamp(), network);
        String jsonCracUrl = fileExporter.saveCracInJsonFormat(cracCreationContext.getCrac(), coreValidRequest.getTimestamp());
        RaoParameters raoParameters = getRaoParametersConfig();
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        return new StudyPointData(network, coreNetPositions, scalableZonalData, cracCreationContext, jsonCracUrl, raoParametersUrl);
    }

    private RaoParameters getRaoParametersConfig() {
        RaoParameters raoParameters = RaoParameters.load();
        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);

        searchTreeRaoParameters.setMaxCurativePstPerTso(searchTreeRaoConfiguration.getMaxCurativePstPerTso());
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(searchTreeRaoConfiguration.getMaxCurativeTopoPerTso());
        searchTreeRaoParameters.setMaxCurativeRaPerTso(searchTreeRaoConfiguration.getMaxCurativeRaPerTso());

        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        return raoParameters;
    }

    private Map<ResultFileExporter.ResultType, String> saveProcessOutputs(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest) {
        return fileExporter.exportStudyPointResult(studyPointResults, coreValidRequest);
    }
}
