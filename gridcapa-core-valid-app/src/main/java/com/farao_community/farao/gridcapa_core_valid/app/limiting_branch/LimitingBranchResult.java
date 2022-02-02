/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class LimitingBranchResult {

    private final String verticeID;
    private final Double ramBefore;
    private final Double ramAfter;
    private final Double flowBefore;
    private final Double flowAfter;
    private final Set<RemedialAction<?>> remedialActions;
    private final String criticalBranchId;
    private final State state;

    public LimitingBranchResult(String verticeID, String criticalBranchId, RaoResult raoResult, FlowCnec cnec) {
        this.verticeID = verticeID;
        this.ramBefore = raoResult.getMargin(OptimizationState.INITIAL, cnec, Unit.MEGAWATT);
        this.ramAfter = raoResult.getMargin(OptimizationState.afterOptimizing(cnec.getState()), cnec, Unit.MEGAWATT);
        this.flowBefore = raoResult.getFlow(OptimizationState.INITIAL, cnec, Unit.MEGAWATT);
        this.flowAfter = raoResult.getFlow(OptimizationState.afterOptimizing(cnec.getState()), cnec, Unit.MEGAWATT);
        this.remedialActions = getRemedialActions(raoResult, cnec);
        this.criticalBranchId = criticalBranchId;
        this.state = cnec.getState();
    }

    public String getVerticeID() {
        return verticeID;
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

    private Set<RemedialAction<?>> getRemedialActions(RaoResult raoResult, Cnec cnec) {
        Set<NetworkAction> networkActions = raoResult.getActivatedNetworkActionsDuringState(cnec.getState());
        Set<RangeAction<?>> rangeActions = raoResult.getActivatedRangeActionsDuringState(cnec.getState());
        Set<RemedialAction<?>> remedialActionsActivated = new HashSet<>();
        remedialActionsActivated.addAll(networkActions);
        remedialActionsActivated.addAll(rangeActions);
        return remedialActionsActivated;
    }
}
