/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

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
    private final State state;

    public LimitingBranchResult(String verticeId, String criticalBranchId, Double ramBefore, Double ramAfter, Double flowBefore, Double flowAfter, Set<RemedialAction<?>> remedialActions, State state) {
        this.verticeId = verticeId;
        this.criticalBranchId = criticalBranchId;
        this.ramBefore = ramBefore;
        this.ramAfter = ramAfter;
        this.flowBefore = flowBefore;
        this.flowAfter = flowAfter;
        this.remedialActions = remedialActions;
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

    public State getState() {
        return state;
    }
}
