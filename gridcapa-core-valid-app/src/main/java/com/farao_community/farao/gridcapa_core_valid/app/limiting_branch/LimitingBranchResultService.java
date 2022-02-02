/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Instant;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_api.cnec.Cnec;
import com.farao_community.farao.data.crac_api.cnec.FlowCnec;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.UrlValidationService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class LimitingBranchResultService {

    private final UrlValidationService urlValidationService;
    private final RaoResultImporter raoResultImporter;

    public LimitingBranchResultService(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
        this.raoResultImporter = new RaoResultImporter();
    }

    public List<LimitingBranchResult> importRaoResult(StudyPoint studyPoint, FbConstraintCreationContext cracCreationContext, String raoResultUrl) {
        String verticeId = studyPoint.getVerticeId();
        RaoResult raoResult = raoResultImporter.importRaoResult(urlValidationService.openUrlStream(raoResultUrl), cracCreationContext.getCrac());
        List<LimitingBranchResult> listLimitingBranches = new ArrayList<>();
        cracCreationContext.getBranchCnecCreationContexts().forEach(branchCnecCreationContext -> {
            if (branchCnecCreationContext.isImported()) {
                String criticalBranchId = branchCnecCreationContext.getNativeId();
                Map<Instant, String> flowCnecsIds = branchCnecCreationContext.getCreatedCnecsIds();
                flowCnecsIds.forEach((instant, flowCnecId) -> {
                    FlowCnec cnec = cracCreationContext.getCrac().getFlowCnec(flowCnecId);
                    LimitingBranchResult branchResult = createLimitingBranchResult(verticeId, criticalBranchId, raoResult, cnec);
                    listLimitingBranches.add(branchResult);
                });
            }
        });
        return listLimitingBranches;
    }

    private LimitingBranchResult createLimitingBranchResult(String studyPointId, String criticalBranchId, RaoResult raoResult, FlowCnec cnec) {
        Double ramBefore = raoResult.getMargin(OptimizationState.INITIAL, cnec, Unit.MEGAWATT);
        Double ramAfter = raoResult.getMargin(OptimizationState.afterOptimizing(cnec.getState()), cnec, Unit.MEGAWATT);
        Double flowBefore = raoResult.getFlow(OptimizationState.INITIAL, cnec, Unit.MEGAWATT);
        Double flowAfter = raoResult.getFlow(OptimizationState.afterOptimizing(cnec.getState()), cnec, Unit.MEGAWATT);
        Set<RemedialAction<?>> remedialActions = getRemedialActions(raoResult, cnec);
        State state = cnec.getState();
        return new LimitingBranchResult(
                studyPointId,
                criticalBranchId,
                ramBefore,
                ramAfter,
                flowBefore,
                flowAfter,
                remedialActions,
                state
        );
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
