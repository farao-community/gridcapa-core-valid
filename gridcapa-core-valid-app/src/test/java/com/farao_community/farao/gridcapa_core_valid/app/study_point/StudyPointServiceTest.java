/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.gridcapa_core_valid.app.MinioAdapter;
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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class StudyPointServiceTest {

    @MockBean
    private MinioAdapter minioAdapter;

    @Autowired StudyPointService studyPointService;

    private final String testDirectory = "/20210723";
    private List<StudyPoint> studyPoints;
    private GlskDocument glskDocument;
    private Network network;
    private ZonalData<Scalable> scalableZonalData;
    private Map<String, Double> coreNetPositions = new HashMap<>();

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

    }

    @Test
    void checkStudyPointComputationSucceed() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("http://url");
        StudyPointResult result = studyPointService.computeStudyPoint(studyPoints.get(0), network, scalableZonalData, coreNetPositions);
        assertEquals("0_9", result.getId());
        assertEquals(StudyPointResult.Status.SUCCESS, result.getStatus());
        assertEquals("http://url", result.getShiftedCgmUrl());
    }

    @Test
    void checkStudyPointComputationFailed() {
        scalableZonalData = null;
        StudyPointResult result = studyPointService.computeStudyPoint(studyPoints.get(0), network, scalableZonalData, coreNetPositions);
        assertEquals("0_9", result.getId());
        assertEquals(StudyPointResult.Status.ERROR, result.getStatus());
        assertEquals("", result.getShiftedCgmUrl());
    }
}
