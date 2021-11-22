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
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.ucte.UcteGlskDocument;
import org.junit.jupiter.api.Test;
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

    @MockBean
    private MinioAdapter minioAdapter;

    @Autowired
    private CoreValidHandler coreValidHandler;

    private final String testDirectory = "/20210723";

    @Test
    void handleCoreValidRequestTest() {
        String requestId = "Test request";
        String networkFileName = "20210723_0030_2D5_CGM_limits.uct";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource("", getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        CoreValidFileResource cbcoraFile = new CoreValidFileResource("cbcora", "url");

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);
    }

    private CoreValidFileResource createFileResource(String filename, URL resource) {
        return new CoreValidFileResource(filename, resource.toExternalForm());
    }

    @Test
    void importGlskTest() {
        CoreValidFileResource glskFile = createFileResource("glsk", getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        GlskDocument glskDocument = coreValidHandler.importGlskFile(glskFile);
        assertEquals(4, ((UcteGlskDocument) glskDocument).getListGlskSeries().size());
        assertEquals(1, glskDocument.getGlskPoints("10YFR-RTE------C").size());
    }
}
