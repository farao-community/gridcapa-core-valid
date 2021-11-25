/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class StudyPointResult {
    private String id;
    private Status status;
    private String shiftedCgmUrl;

    public StudyPointResult(String id) {
        this.id = id;
        this.status = Status.NOT_STARTED;
        this.shiftedCgmUrl = "";
    }

    public String getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getShiftedCgmUrl() {
        return shiftedCgmUrl;
    }

    public void setShiftedCgmUrl(String shiftedCgmUrl) {
        this.shiftedCgmUrl = shiftedCgmUrl;
    }

    enum Status {
        NOT_STARTED,
        SUCCESS,
        ERROR
    }
}
