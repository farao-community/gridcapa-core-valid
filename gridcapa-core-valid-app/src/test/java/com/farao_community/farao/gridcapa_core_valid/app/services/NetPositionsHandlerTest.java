/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
class NetPositionsHandlerTest {

    private final String testDirectory = "/20210723";
    private InputStream refProgStream;
    private List<StudyPoint> allStudyPoints;
    private GlskDocument glskDocument;

    @BeforeEach
    void setup() {
        refProgStream = getClass().getResourceAsStream(testDirectory + "/20210723-F110.xml");
        allStudyPoints = StudyPointsImporter.importStudyPoints(getClass().getResourceAsStream(testDirectory + "/20210723-Points_Etudes-v01.csv"));
        glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream(testDirectory + "/20210723-F226-v1.xml"));

    }

    @Test
    void computeCoreNetPositionsTest() {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgStream, dateTime);
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        assertEquals(4, coreNetPositions.size());
        assertEquals(-50, coreNetPositions.get("FR"));
        assertEquals(-450, coreNetPositions.get("DE"));
        assertEquals(225, coreNetPositions.get("NL"));
        assertEquals(275, coreNetPositions.get("BE"));
    }

    @Test
    void shiftNetPositionTestWithAlegroShift() {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgStream, dateTime);
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        InputStream networkStream = getClass().getResourceAsStream(testDirectory + "/20210723_0030_2D5_CGM.uct");
        Network network = Network.read("20210723_0030_2D5_CGM.uct", networkStream);
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, dateTime.toInstant());
        NetPositionsHandler.shiftNetPositionToStudyPoint(network, allStudyPoints.get(0), scalableZonalData, coreNetPositions);
        assertEquals(2917.5, network.getGenerator("BBE1AA1 _generator").getTargetP(), 0.01);
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
        assertEquals(600.0, network.getDanglingLineStream().filter(dl -> dl.getPairingKey().equals("XLI_OB1B")).findAny().get().getP0(), 0.01);
        assertEquals(-600.0, network.getDanglingLineStream().filter(dl -> dl.getPairingKey().equals("XLI_OB1A")).findAny().get().getP0(), 0.01);
    }

    @Test
    void shiftNetPositionTestWithoutAlegro() {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-23T02:30Z");
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgStream, dateTime);
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        InputStream networkStream = getClass().getResourceAsStream(testDirectory + "/20210723_0430_2D5_CGM.uct");
        Network network = Network.read("20210723_0430_2D5_CGM.uct", networkStream);
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, dateTime.toInstant());
        NetPositionsHandler.shiftNetPositionToStudyPoint(network, allStudyPoints.get(1), scalableZonalData, coreNetPositions);

        assertEquals(1335.0, network.getGenerator("BBE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(2670.0, network.getGenerator("BBE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(2445.0, network.getGenerator("BBE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(875.0, network.getGenerator("DDE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(375.0, network.getGenerator("DDE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(-1750.0, network.getGenerator("DDE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(1640.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.01);
        assertEquals(1640.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.01);
        assertEquals(2820.0, network.getGenerator("FFR3AA1 _generator").getTargetP(), 0.01);
        assertEquals(1762.5, network.getGenerator("NNL1AA1 _generator").getTargetP(), 0.01);
        assertEquals(1112.5, network.getGenerator("NNL2AA1 _generator").getTargetP(), 0.01);
        assertEquals(2875.0, network.getGenerator("NNL3AA1 _generator").getTargetP(), 0.01);

    }

    @Test
    void shiftNetPositionTestWithAlegroRefProg() {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-23T09:30Z");
        refProgStream = getClass().getResourceAsStream(testDirectory + "/20210723-F110-Alegro.xml");
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgStream, dateTime);
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        InputStream networkStream = getClass().getResourceAsStream(testDirectory + "/20210723_1130_2D5_CGM.uct");
        Network network = Network.read("20210723_1130_2D5_CGM.uct", networkStream);
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, dateTime.toInstant());
        NetPositionsHandler.shiftNetPositionToStudyPoint(network, allStudyPoints.get(2), scalableZonalData, coreNetPositions);

        assertEquals(-585.0, network.getGenerator("BBE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(-1170.0, network.getGenerator("BBE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(1805.0, network.getGenerator("BBE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(1237.5, network.getGenerator("DDE1AA1 _generator").getTargetP(), 0.01);
        assertEquals(737.5, network.getGenerator("DDE2AA1 _generator").getTargetP(), 0.01);
        assertEquals(-1025.0, network.getGenerator("DDE3AA1 _generator").getTargetP(), 0.01);
        assertEquals(4300.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.01);
        assertEquals(4300.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.01);
        assertEquals(4150.0, network.getGenerator("FFR3AA1 _generator").getTargetP(), 0.01);
        assertEquals(1425.0, network.getGenerator("NNL1AA1 _generator").getTargetP(), 0.01);
        assertEquals(325.0, network.getGenerator("NNL2AA1 _generator").getTargetP(), 0.01);
        assertEquals(1750.0, network.getGenerator("NNL3AA1 _generator").getTargetP(), 0.01);
        assertEquals(-234.0, network.getDanglingLineStream().filter(dl -> dl.getPairingKey().equals("XLI_OB1B")).findAny().get().getP0(), 0.01);
        assertEquals(234.0, network.getDanglingLineStream().filter(dl -> dl.getPairingKey().equals("XLI_OB1A")).findAny().get().getP0(), 0.01);
    }
}
