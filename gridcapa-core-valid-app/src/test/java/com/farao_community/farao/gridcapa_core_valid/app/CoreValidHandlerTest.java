/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.app.services.MinioAdapter;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.URL;
import java.time.OffsetDateTime;

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
    private RaoRunnerClient raoRunnerClient;

    private final String testDirectory = "/20210723";

    @Test
    void handleCoreValidRequestTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("http://url");
        Mockito.when(raoRunnerClient.runRao(Mockito.any())).thenReturn(new RaoResponse("id", "instant", "praUrl", "cracUrl", "raoUrl"));
        String requestId = "Test request";
        String networkFileName = "20210723_0030_2D5_CGM_limits.uct";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        CoreValidFileResource cbcoraFile = createFileResource("cbcora",  getClass().getResource(testDirectory + "/20210723-F301_CBCORA_hvdcvh-outage.xml"));

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);
        assertEquals(requestId, response.getId());
    }

    private CoreValidFileResource createFileResource(String filename, URL resource) {
        return new CoreValidFileResource(filename, resource.toExternalForm());
    }
}
