/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.cse.CseGlskDocumentImporter;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CoreValidHandlerTest {

    @Autowired
    private CoreValidHandler coreValidHandler;

    @MockBean
    private MinioAdapter minioAdapter;

    @MockBean
    private StudyPointService studyPointService;

    @MockBean
    private FileExporter fileExporter;

    @MockBean
    private FileImporter fileImporter;

    @Test
    void handleCoreValidRequestTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("http://url");
        RaoRequest raoRequest = Mockito.mock(RaoRequest.class);
        Mockito.when(studyPointService.computeStudyPointShift(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(raoRequest);
        CompletableFuture<RaoResponse> future = new CompletableFuture<>();
        RaoResponse raoResponse = new RaoResponse("id", "instant", "praUrl", "cracUrl", "raoUrl", Instant.now(), Instant.now());
        Mockito.when(studyPointService.computeStudyPointRao(Mockito.any(), Mockito.any())).thenReturn(future);
        future.complete(raoResponse);
        Mockito.when(fileExporter.exportStudyPointResult(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new HashMap<>());
        Mockito.when(fileImporter.importNetworkFromUrl(Mockito.any())).thenReturn(null);
        Mockito.when(fileExporter.saveShiftedCgmWithPra(Mockito.any(), Mockito.any())).thenReturn("");

        String requestId = "Test request";
        String networkFileName = "20230703_0030_2D5_CGM_limits.uct";
        String testDirectory = "/20230703";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-F226-v1.xml"));
        CoreValidFileResource cbcoraFile = createFileResource("cbcora",  getClass().getResource(testDirectory + "/20230703-F301_CBCORA_hvdcvh-outage.xml"));

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile, true);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);
        assertEquals(requestId, response.getId());
        Mockito.verify(minioAdapter, Mockito.times(1)).deleteFiles(Mockito.any());
    }

    private CoreValidFileResource createFileResource(String filename, URL resource) {
        return new CoreValidFileResource(filename, resource.toExternalForm());
    }

    @Test
    void testRobin() {
        String testDirectory = "/testRobin";
        String networkFileName = "20230703_1230_F119.uct";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));
        CoreValidFileResource refProgFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-Points_Etude-v01.csv"));
        CoreValidFileResource glskFile = createFileResource("", getClass().getResource(testDirectory + "/20230703-F226.xml"));
        CoreValidFileResource cbcoraFile = createFileResource("cbcora",  getClass().getResource(testDirectory + "/20230703-F301-1.xml"));

        String requestId = "Test request";
        OffsetDateTime dateTime = OffsetDateTime.parse("2023-07-03T10:30Z");

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile, true);
        CoreValidResponse coreValidResponse = coreValidHandler.handleCoreValidRequest(request);

    }
}
