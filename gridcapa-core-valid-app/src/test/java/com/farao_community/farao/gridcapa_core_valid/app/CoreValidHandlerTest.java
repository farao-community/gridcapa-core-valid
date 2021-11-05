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
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Map;

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

    private String testDirectory = "/20210723_0030";

    @Test
    void handleCoreValidRequestTest() {
        String requestId = "Test request";
        CoreValidFileResource networkFile = createFileResource(getClass().getResource(testDirectory + "/20210723_0030_2D5_CGM.uct"));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F110-v1-17XTSO-CS------W-to-10V1001C--00085T.xml"));
        CoreValidFileResource studyPointsFile = createFileResource(getClass().getResource(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F226-v1-17XTSO-CS------W-to-10V1001C--00085T.xml"));
        CoreValidFileResource cbcoraFile = new CoreValidFileResource("cbcora", "url");

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);
    }

    private CoreValidFileResource createFileResource(URL resource) {
        return new CoreValidFileResource(resource.getFile(), resource.toExternalForm());
    }

    @Test
    void computeCoreNetPositionsTest() {
        CoreValidFileResource refProgFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F110-v1-17XTSO-CS------W-to-10V1001C--00085T.xml"));
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        ReferenceProgram referenceProgram = coreValidHandler.importReferenceProgram(refProgFile, dateTime);
        assertEquals(-50, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"));
        assertEquals(-450, referenceProgram.getGlobalNetPosition("10YCB-GERMANY--8"));
        assertEquals(225, referenceProgram.getGlobalNetPosition("10YNL----------L"));
        assertEquals(275, referenceProgram.getGlobalNetPosition("10YBE----------2"));
        Map<String, Double> coreNetPositions = coreValidHandler.computeCoreReferenceNetPositions(referenceProgram);
        assertEquals(4, coreNetPositions.size());
        assertEquals(-50, coreNetPositions.get("FR"));
        assertEquals(-450, coreNetPositions.get("DE"));
        assertEquals(225, coreNetPositions.get("NL"));
        assertEquals(275, coreNetPositions.get("BE"));
    }
}
