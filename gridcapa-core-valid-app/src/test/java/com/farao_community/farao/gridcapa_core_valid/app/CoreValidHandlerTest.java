/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidRaoException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointData;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.AbstractRaoResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoFailureResponse;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoSuccessResponse;
import com.powsybl.iidm.network.Network;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class CoreValidHandlerTest {

    @Autowired
    private CoreValidHandler coreValidHandler;

    @MockitoBean
    private MinioAdapter minioAdapter;

    @MockitoBean
    private StudyPointService studyPointService;

    @MockitoBean
    private FileExporter fileExporter;

    @MockitoBean
    private FileImporter fileImporter;

    @Test
    void handleCoreValidRequestTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("http://url");
        RaoRequest raoRequest = Mockito.mock(RaoRequest.class);
        Mockito.when(studyPointService.computeStudyPointShift(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(raoRequest);
        CompletableFuture<AbstractRaoResponse> future = new CompletableFuture<>();
        RaoSuccessResponse raoResponse = new RaoSuccessResponse.Builder()
                .withId("id")
                .withInstant("instant")
                .withNetworkWithPraFileUrl("praUrl")
                .withCracFileUrl("cracUrl")
                .withRaoResultFileUrl("raoUrl")
                .withComputationStartInstant(Instant.now())
                .withComputationEndInstant(Instant.now())
                .build();
        Mockito.when(studyPointService.computeStudyPointRao(Mockito.any(), Mockito.any())).thenReturn(future);
        future.complete(raoResponse);
        Mockito.when(fileImporter.importNetworkFromUrl(Mockito.any())).thenReturn(null);
        Mockito.when(fileExporter.saveShiftedCgmWithPra(Mockito.any(), Mockito.any())).thenReturn("");

        String requestId = "Test request";
        String runId = "Test run id";
        String networkFileName = "20210723_0030_2D5_CGM_limits.uct";
        String testDirectory = "/20210723";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        CoreValidFileResource cbcoraFile = createFileResource("cbcora", getClass().getResource(testDirectory + "/20210723-F301_CBCORA_hvdcvh-outage.xml"));

        CoreValidRequest request = new CoreValidRequest(requestId, runId, dateTime, networkFile, cbcoraFile, glskFile, refProgFile, studyPointsFile, true);
        coreValidHandler.handleCoreValidRequest(request);
        Mockito.verify(minioAdapter, Mockito.times(1)).deleteFiles(Mockito.any());
        Mockito.verify(fileExporter, Mockito.times(1)).exportStudyPointResult(Mockito.any(), Mockito.any(), Mockito.any());
    }

    private CoreValidFileResource createFileResource(String filename, URL resource) {
        return new CoreValidFileResource(filename, resource.toExternalForm());
    }

    @Test
    void fillResultsForEachStudyPointWithFailedRao() {
        final StudyPointData studyPointData = Mockito.mock(StudyPointData.class);
        final StudyPoint studyPoint = new StudyPoint(1, "a", Map.of());
        final RaoFailureResponse raoFailureResponse = new RaoFailureResponse.Builder()
                .withErrorMessage("test message")
                .build();
        final CompletableFuture<AbstractRaoResponse> completableFuture = CompletableFuture.completedFuture(raoFailureResponse);
        final Map<StudyPoint, CompletableFuture<AbstractRaoResponse>> studyPointCompletableFutures = Map.of(studyPoint, completableFuture);

        Assertions.assertThatExceptionOfType(CoreValidRaoException.class)
                .isThrownBy(() -> coreValidHandler.fillResultsForEachStudyPoint(studyPointData, studyPointCompletableFutures))
                .withMessageContaining("test message");
    }

    @Test
    void fillResultsForEachStudyPoint() throws ExecutionException, InterruptedException {
        final StudyPointData studyPointData = Mockito.mock(StudyPointData.class);
        final StudyPoint studyPoint = new StudyPoint(1, "a", Map.of());
        final RaoSuccessResponse raoSuccessResponse = new RaoSuccessResponse.Builder()
                .withId("id")
                .withNetworkWithPraFileUrl("networkUrl")
                .build();
        final CompletableFuture<AbstractRaoResponse> completableFuture = CompletableFuture.completedFuture(raoSuccessResponse);
        final Map<StudyPoint, CompletableFuture<AbstractRaoResponse>> studyPointCompletableFutures = Map.of(studyPoint, completableFuture);

        final Network network = Mockito.mock(Network.class);
        Mockito.when(fileImporter.importNetworkFromUrl("networkUrl")).thenReturn(network);
        final StudyPointResult studyPointResult = new StudyPointResult("resultId");
        Mockito.when(studyPointService.postTreatRaoResult(studyPoint, studyPointData, raoSuccessResponse)).thenReturn(studyPointResult);

        final List<StudyPointResult> results = coreValidHandler.fillResultsForEachStudyPoint(studyPointData, studyPointCompletableFutures);

        Mockito.verify(fileExporter, Mockito.times(1)).saveShiftedCgmWithPra(Mockito.eq(network), Mockito.anyString());
        Assertions.assertThat(results).containsExactly(studyPointResult);
    }
}
