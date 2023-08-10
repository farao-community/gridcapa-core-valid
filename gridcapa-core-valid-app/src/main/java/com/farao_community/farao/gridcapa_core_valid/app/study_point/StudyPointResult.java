/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class StudyPointResult {
    private final String id;
    private Status status;
    private String shiftedCgmUrl;
    private String networkWithPraUrl;
    private String raoResultFileUrl;
    private String period;
    private List<LimitingBranchResult> listLimitingBranchResult;

    public StudyPointResult(String id) {
        this.id = id;
        this.status = Status.NOT_STARTED;
        this.shiftedCgmUrl = "";
        this.networkWithPraUrl = "";
        this.raoResultFileUrl = "";
        this.listLimitingBranchResult = new ArrayList<>();
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

    public void setStatusToError() {
        setStatus(Status.ERROR);
    }

    public String getShiftedCgmUrl() {
        return shiftedCgmUrl;
    }

    public void setShiftedCgmUrl(String shiftedCgmUrl) {
        this.shiftedCgmUrl = shiftedCgmUrl;
    }

    public String getNetworkWithPraUrl() {
        return networkWithPraUrl;
    }

    public void setNetworkWithPraUrl(String networkWithPraUrl) {
        this.networkWithPraUrl = networkWithPraUrl;
    }

    public String getRaoResultFileUrl() {
        return raoResultFileUrl;
    }

    public void setRaoResultFileUrl(String raoResultFileUrl) {
        this.raoResultFileUrl = raoResultFileUrl;
    }

    public List<LimitingBranchResult> getListLimitingBranchResult() {
        return listLimitingBranchResult;
    }

    public void setListLimitingBranchResult(List<LimitingBranchResult> listLimitingBranchResult) {
        this.listLimitingBranchResult = listLimitingBranchResult;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public enum Status {
        NOT_STARTED,
        SUCCESS,
        ERROR
    }
}
