/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Interface to export on minIO a results file for all study points computations on a given timestamp.
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public interface ResultFileExporter {
    String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp);

    /**
     * Several types of results file can co-exist.
     * They are enumerated here.
     * TODO: remove this when the choice of the results file type can be done in the configuration file
     */
    enum ResultType {
        MAIN_RESULT,
        REMEDIAL_ACTIONS_RESULT,
        REX_RESULT
    }
}
