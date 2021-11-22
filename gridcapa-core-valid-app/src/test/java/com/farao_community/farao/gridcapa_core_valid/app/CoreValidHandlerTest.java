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
        CoreValidFileResource networkFile = createFileResource(getClass().getResource(testDirectory + "/20210723_0030_2D5_CGM_limits.uct"));

        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        CoreValidFileResource refProgFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F110.xml"));
        CoreValidFileResource studyPointsFile = createFileResource(getClass().getResource(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        CoreValidFileResource glskFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        CoreValidFileResource cbcoraFile = new CoreValidFileResource("cbcora", "url");

        CoreValidRequest request = new CoreValidRequest(requestId, dateTime, networkFile, cbcoraFile, glskFile,  refProgFile, studyPointsFile);
        CoreValidResponse response = coreValidHandler.handleCoreValidRequest(request);

        /*assertEquals(2917.5, network.getGenerator("BBE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(2917.5, network.getGenerator("BBE1AA1 _generator").getMaxP(), 0.01);
        assertEquals(-9000., network.getGenerator("BBE1AA1 _generator").getMinP(), 0.01);
        assertEquals(5835.0, network.getGenerator("BBE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(2972.5, network.getGenerator("BBE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(2620.0, network.getGenerator("DDE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(2120.0, network.getGenerator("DDE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(1740.0, network.getGenerator("DDE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(420.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.01);
        assertEquals(420.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.01);
        assertEquals(2210.0, network.getGenerator("FFR3AA1 _generator").getTargetP(), 0.01);
        assertEquals(716.25, network.getGenerator("NNL1AA1 _generator").getTargetP(), 0.01);
        assertEquals(-1328.75, network.getGenerator("NNL2AA1 _generator").getTargetP(), 0.01);
        assertEquals(-612.5, network.getGenerator("NNL3AA1 _generator").getTargetP(), 0.01);
        assertEquals(-600.0, network.getDanglingLineStream().filter(dl -> dl.getUcteXnodeCode().equals("XLI_OB1B")).findAny().get().getP0(), 0.01);
        assertEquals(600.0, network.getDanglingLineStream().filter(dl -> dl.getUcteXnodeCode().equals("XLI_OB1A")).findAny().get().getP0(), 0.01);*/
    }

    private CoreValidFileResource createFileResource(URL resource) {
        return new CoreValidFileResource(resource.getFile(), resource.toExternalForm());
    }

    @Test
    void importGlskTest() {
        CoreValidFileResource glskFile = createFileResource(getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        GlskDocument glskDocument = coreValidHandler.importGlskFile(glskFile);
        assertEquals(4, ((UcteGlskDocument) glskDocument).getListGlskSeries().size());
        assertEquals(1, glskDocument.getGlskPoints("10YFR-RTE------C").size());
    }
}
