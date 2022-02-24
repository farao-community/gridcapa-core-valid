/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;

import java.util.Set;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class LimitingBranchResult {

    private final String verticeId;
    private final Double ramBefore;
    private final Double ramAfter;
    private final Double flowBefore;
    private final Double flowAfter;
    private final Set<RemedialAction<?>> remedialActions;
    private final String criticalBranchId;
    private final String criticalBranchName;
    private final State state;

    public LimitingBranchResult(String verticeId, String criticalBranchId, Double ramBefore, Double ramAfter, Double flowBefore, Double flowAfter, Set<RemedialAction<?>> remedialActions, String criticalBranchName, State state) {
        this.verticeId = verticeId;
        this.criticalBranchId = criticalBranchId;
        this.ramBefore = ramBefore;
        this.ramAfter = ramAfter;
        this.flowBefore = flowBefore;
        this.flowAfter = flowAfter;
        this.remedialActions = remedialActions;
        this.criticalBranchName = criticalBranchName;
        this.state = state;
    }

    public String getVerticeId() {
        return verticeId;
    }

    public Double getRamBefore() {
        return ramBefore;
    }

    public Double getRamAfter() {
        return ramAfter;
    }

    public Double getFlowBefore() {
        return flowBefore;
    }

    public Double getFlowAfter() {
        return flowAfter;
    }

    public Set<RemedialAction<?>> getRemedialActions() {
        return remedialActions;
    }

    public String getCriticalBranchId() {
        return criticalBranchId;
    }

    public String getCriticalBranchName() {
        return criticalBranchName;
    }

    public State getState() {
        return state;
    }

    public String getBranchStatus() {
        String branchStatus;
        switch (getState().getInstant()) {
            case PREVENTIVE:
                branchStatus = "P";
                break;
            case OUTAGE:
                branchStatus = "O";
                break;
            case CURATIVE:
                branchStatus = "C";
                break;
            default:
                throw new CoreValidInvalidDataException(String.format("Invalid value in CBCORA file, for cnec %s", getCriticalBranchId()));
        }
        return branchStatus;
    }
}
