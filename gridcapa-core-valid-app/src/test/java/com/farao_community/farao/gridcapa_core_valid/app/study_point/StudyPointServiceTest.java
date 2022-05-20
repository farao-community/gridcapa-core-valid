/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResultService;
import com.farao_community.farao.gridcapa_core_valid.app.services.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.AsynchronousRaoRunnerClient;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class StudyPointServiceTest {

    @MockBean
    private MinioAdapter minioAdapter;

    @MockBean
    private LimitingBranchResultService limitingBranchResult;

    @MockBean
    private AsynchronousRaoRunnerClient asynchronousRaoRunnerClient;

    @Autowired StudyPointService studyPointService;

    private final String testDirectory = "/20210723";
    private List<StudyPoint> studyPoints;
    private GlskDocument glskDocument;
    private Network network;
    private ZonalData<Scalable> scalableZonalData;
    private Map<String, Double> coreNetPositions = new HashMap<>();
    private CoreValidRequest coreValidRequest;

    @BeforeEach
    public void setup() {
        coreNetPositions.put("FR", -50.);
        coreNetPositions.put("DE", -450.);
        coreNetPositions.put("NL", 225.);
        coreNetPositions.put("BE", 275.);
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        studyPoints = StudyPointsImporter.importStudyPoints(getClass().getResourceAsStream(testDirectory + "/20210723-Points_Etudes-v01.csv"), dateTime);
        glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream(testDirectory + "/20210723-F226-v1.xml"));
        InputStream networkStream = getClass().getResourceAsStream(testDirectory + "/20210723_0030_2D5_CGM.uct");
        network = Importers.loadNetwork("20210723_0030_2D5_CGM.uct", networkStream);
        scalableZonalData = glskDocument.getZonalScalable(network, dateTime.toInstant());
        coreValidRequest = new CoreValidRequest("id", null, null, new CoreValidFileResource("name", "url"), null, null, null);
    }

    @Test
    void checkStudyPointComputationSucceed() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("http://url");
        CompletableFuture<RaoResponse> future = new CompletableFuture<>();
        Mockito.when(asynchronousRaoRunnerClient.runRaoAsynchronously(Mockito.any())).thenReturn(future);
        Mockito.when(limitingBranchResult.importRaoResult(Mockito.any(), Mockito.any(), Mockito.anyString())).thenReturn(null);
        StudyPointData studyPointData = new StudyPointData(network, coreNetPositions, scalableZonalData, null, "", "");
        RaoRequest raoRequest = studyPointService.computeStudyPointShift(studyPoints.get(0), studyPointData, OffsetDateTime.now(), "id");
        CompletableFuture<RaoResponse> raoResponseCompletableFuture = studyPointService.computeStudyPointRao(studyPoints.get(0), raoRequest);
        RaoResponse raoResponse = new RaoResponse("id", "instant", "praUrl", " cracUrl", "raoUrl", Instant.now(), Instant.now());
        raoResponseCompletableFuture.complete(raoResponse);
        studyPointService.postTreatRaoResult(studyPoints.get(0), studyPointData, raoResponse);
        StudyPointResult result = studyPoints.get(0).getStudyPointResult();
        assertEquals("0_9", result.getId());
        assertEquals(StudyPointResult.Status.SUCCESS, result.getStatus());
        assertEquals("http://url", result.getShiftedCgmUrl());
        assertEquals("praUrl", result.getNetworkWithPraUrl());
        assertEquals("raoUrl", result.getRaoResultFileUrl());
    }

    @Test
    void checkStudyPointComputationFailed() {
        scalableZonalData = null;
        StudyPointData studyPointData = new StudyPointData(network, coreNetPositions, scalableZonalData, null, "", "");
        studyPointService.computeStudyPointShift(studyPoints.get(0), studyPointData, OffsetDateTime.now(), "id");
        StudyPointResult result = studyPoints.get(0).getStudyPointResult();
        assertEquals("0_9", result.getId());
        assertEquals(StudyPointResult.Status.ERROR, result.getStatus());
        assertEquals("", result.getShiftedCgmUrl());
    }

    @Test
    void exceptionCaughtWhenRaoFails() {
        String exceptionMessage = "exceptionMessage";
        Mockito.when(asynchronousRaoRunnerClient.runRaoAsynchronously(Mockito.any())).thenThrow(new RuntimeException(exceptionMessage));
        StudyPoint studyPoint = Mockito.mock(StudyPoint.class);
        String verticeId = "verticeId";
        Mockito.when(studyPoint.getVerticeId()).thenReturn(verticeId);
        RaoRequest raoRequest = Mockito.mock(RaoRequest.class);
        try {
            studyPointService.computeStudyPointRao(studyPoint, raoRequest);
            fail();
        } catch (Exception e) {
            assertEquals(exceptionMessage, e.getMessage());
        }
    }
}
