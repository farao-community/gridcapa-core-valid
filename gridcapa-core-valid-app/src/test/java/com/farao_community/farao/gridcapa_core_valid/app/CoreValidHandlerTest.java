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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.URL;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CoreValidHandlerTest {

    @MockBean
    private MinioAdapter minioAdapter;

    @Autowired
    private CoreValidHandler coreValidHandler;

    @Test
    void handleCoreValidRequestTest() {
        String testDirectory = "/20210723_0030";
        String requestId = "Test request";
        CoreValidFileResource networkFile = createFileResource(getClass().getResource(testDirectory + "/20210723_0030_2D5_CGM.uct"));

        String timestamp = "20210723T22:30"; //todo check timestamp zone time local or UTC
        CoreValidFileResource cbcoraFile = new CoreValidFileResource("cbcora", "url");
        CoreValidFileResource glskFile = new CoreValidFileResource("glsk", "url");
        CoreValidFileResource refProgFile = new CoreValidFileResource("refprog", "url");
        CoreValidFileResource studyPointsFile = new CoreValidFileResource("study-points", "url");

        CoreValidRequest request = new CoreValidRequest(requestId, timestamp, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);
    }

    private CoreValidFileResource createFileResource(URL resource) {
        return new CoreValidFileResource(resource.getFile(), resource.toExternalForm());
    }
}
