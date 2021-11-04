/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class StudyPointsImporterTest {

    private String testDirectory = "/study_point";

    @Test
    void importStudyPointsTest() {
        File file = new File(getClass().getResource(testDirectory + "/20210723-Points_Etudes.csv").getFile());
        List<StudyPoint> studyPointList = StudyPointsImporter.importStudyPoints(file);
        assertEquals(6, studyPointList.size());
        assertEquals("0_1", studyPointList.get(0).getId());
        assertEquals(0, studyPointList.get(0).getPeriod());
        assertEquals(3000.0, studyPointList.get(0).getPositions().get("NP_AT"));
    }
}