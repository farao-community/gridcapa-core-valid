/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
class StudyPointResultTest {

    private StudyPointResult studyPointResult;

    @BeforeEach
    void setUp() {
        String id = "id";
        studyPointResult = new StudyPointResult(id);
    }

    @Test
    void setStatusToError() {
        assertEquals(StudyPointResult.Status.NOT_STARTED, studyPointResult.getStatus());
        studyPointResult.setStatusToError();
        assertEquals(StudyPointResult.Status.ERROR, studyPointResult.getStatus());
    }

    @Test
    void getListLimitingBranchResult() {
        assertEquals(0, studyPointResult.getListLimitingBranchResult().size());
    }

    @Test
    void getPeriod() {
        String period = "period";
        studyPointResult.setPeriod(period);
        assertEquals(period, studyPointResult.getPeriod());
    }
}