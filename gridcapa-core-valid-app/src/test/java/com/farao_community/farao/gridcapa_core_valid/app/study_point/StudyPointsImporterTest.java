/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class StudyPointsImporterTest {

    private String testDirectory = "/study_point";

    @Test
    void importAllStudyPointsTest() throws IOException {
        InputStream inputStream = getClass().getResource(testDirectory + "/20210723-Points_Etudes.csv").openStream();
        List<StudyPoint> studyPointList = StudyPointsImporter.importStudyPoints(inputStream);
        assertEquals(6, studyPointList.size());
        assertEquals("0_1", studyPointList.get(0).getVertexId());
        assertEquals(0, studyPointList.get(0).getPeriod());
        assertEquals(3000.0, studyPointList.get(0).getPositions().get("NP_AT"));
    }

    @Test
    void importTimestampStudyPointsTest() throws IOException {
        // Test period 0
        InputStream inputStream = getClass().getResource(testDirectory + "/20210723-Points_Etudes.csv").openStream();
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-23T22:30Z");
        List<StudyPoint> studyPointList = StudyPointsImporter.importStudyPoints(inputStream, dateTime);
        assertEquals(3, studyPointList.size());
        assertEquals("0_1", studyPointList.get(0).getVertexId());
        assertEquals(0, studyPointList.get(0).getPeriod());
        assertEquals(3000.0, studyPointList.get(0).getPositions().get("NP_AT"));

        // Test period 1
        inputStream = getClass().getResource(testDirectory + "/20210723-Points_Etudes.csv").openStream();
        dateTime = OffsetDateTime.parse("2021-07-23T23:30Z");
        studyPointList = StudyPointsImporter.importStudyPoints(inputStream, dateTime);
        assertEquals(3, studyPointList.size());
        assertEquals("1_1", studyPointList.get(0).getVertexId());
        assertEquals(1, studyPointList.get(0).getPeriod());
        assertEquals(4500.0, studyPointList.get(0).getPositions().get("NP_AT"));

        // Test period not found
        inputStream = getClass().getResource(testDirectory + "/20210723-Points_Etudes.csv").openStream();
        dateTime = OffsetDateTime.parse("2021-07-23T00:30Z");
        studyPointList = StudyPointsImporter.importStudyPoints(inputStream, dateTime);
        assertEquals(0, studyPointList.size());
    }
}
