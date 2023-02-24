/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

/**
 * Several types of results file can co-exist.
 * They are enumerated here.
 *
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public enum ResultType {
    MAIN_RESULT("AUTO-RESULT"),
    REMEDIAL_ACTIONS_RESULT("REMEDIAL-ACTIONS-RESULT"),
    REX_RESULT("REX-RESULT");

    private final String fileType;

    ResultType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileType() {
        return fileType;
    }
}
