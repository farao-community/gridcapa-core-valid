/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidFileResource;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class FileImporterTest {

    @Autowired
    private FileImporter fileImporter;

    private final String testDirectory = "/20210723";
    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");

    @Test
    void importGlskTest() {
        CoreValidFileResource glskFile = createFileResource("glsk", getClass().getResource(testDirectory + "/20210723-F226-v1.xml"));
        GlskDocument glskDocument = fileImporter.importGlskFile(glskFile);
        assertEquals(4, ((UcteGlskDocument) glskDocument).getListGlskSeries().size());
        assertEquals(1, glskDocument.getGlskPoints("10YFR-RTE------C").size());
    }

    @Test
    void importReferenceProgram() {
        CoreValidFileResource refProgFile = createFileResource("refprog", getClass().getResource(testDirectory + "/20210723-F110.xml"));
        ReferenceProgram referenceProgram = fileImporter.importReferenceProgram(refProgFile, dateTime);
        assertEquals(-50, referenceProgram.getGlobalNetPosition("10YFR-RTE------C"));
        assertEquals(-450, referenceProgram.getGlobalNetPosition("10YCB-GERMANY--8"));
        assertEquals(225, referenceProgram.getGlobalNetPosition("10YNL----------L"));
        assertEquals(275, referenceProgram.getGlobalNetPosition("10YBE----------2"));
    }

    @Test
    void importCrac() {
        InputStream networkStream = getClass().getResourceAsStream(testDirectory + "/20210723_0030_2D5_CGM.uct");
        Network network = Network.read("20210723_0030_2D5_CGM.uct", networkStream);
        CoreValidFileResource cbcoraFile = createFileResource("cbcora", getClass().getResource(testDirectory + "/20210723-F301_CBCORA_hvdcvh-outage.xml"));
        FbConstraintCreationContext fbConstraintCreationContext = fileImporter.importCrac(cbcoraFile.getUrl(), dateTime, network);
        Crac crac = fbConstraintCreationContext.getCrac();
        Assertions.assertNotNull(crac);
        assertEquals("17XTSO-CS------W-20190108-F301v1", crac.getId());
    }

    @Test
    void importNetworkTest() {
        String networkFileName = "20210723_0030_2D5_CGM.uct";
        CoreValidFileResource networkFile = createFileResource(networkFileName, getClass().getResource(testDirectory + "/" + networkFileName));
        Network network = fileImporter.importNetwork(networkFile);
        assertNotNull(network);
        assertEquals("20210723_0030_2D5_CGM", network.getNameOrId());
    }

    @Test
    void importNetworkFromUrlTest() {
        String cgmUrl = getClass().getResource(testDirectory + "/20210723_0030_2D5_CGM.uct").toExternalForm();
        Network network = fileImporter.importNetworkFromUrl(cgmUrl);
        assertNotNull(network);
        assertEquals("20210723_0030_2D5_CGM", network.getNameOrId());
    }

    private CoreValidFileResource createFileResource(String filename, URL resource) {
        return new CoreValidFileResource(filename, resource.toExternalForm());
    }
}
