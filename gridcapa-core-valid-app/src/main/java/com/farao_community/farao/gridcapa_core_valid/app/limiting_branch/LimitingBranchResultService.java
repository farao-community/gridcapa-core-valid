/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.commons.Unit;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.rao_result_api.OptimizationState;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.UrlValidationService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public List<LimitingBranchResult> importRaoResult(StudyPoint studyPoint, Crac crac, String raoResultUrl) {
        RaoResult raoResult = raoResultImporter.importRaoResult(urlValidationService.openUrlStream(raoResultUrl), crac);
        List<LimitingBranchResult> listLimitingBranches = new ArrayList<>();

        crac.getFlowCnecs().forEach(cnec -> {
            LimitingBranchResult limitingBranch = new LimitingBranchResult(
                    studyPoint.getPeriod(),
                    studyPoint.getId(),
                    raoResult.getMargin(OptimizationState.INITIAL, cnec, Unit.MEGAWATT),
                    raoResult.getMargin(OptimizationState.AFTER_CRA, cnec, Unit.MEGAWATT),
                    raoResult.getFlow(OptimizationState.INITIAL, cnec, Unit.MEGAWATT),
                    raoResult.getFlow(OptimizationState.AFTER_CRA, cnec, Unit.MEGAWATT),
                    addRemedialActions(raoResult.getActivatedNetworkActionsDuringState(cnec.getState()), raoResult.getActivatedRangeActionsDuringState(cnec.getState())),
                    cnec.getId(),
                    cnec.getState()
            );
            listLimitingBranches.add(limitingBranch);
        });

        return listLimitingBranches;
    }

    private Set<RemedialAction> addRemedialActions(Set<NetworkAction> networkActions, Set<RangeAction<?>> rangeActions) {
        Set<RemedialAction> remedialActionsActivated = new HashSet<>();
        remedialActionsActivated.addAll(networkActions);
        remedialActionsActivated.addAll(rangeActions);
        return remedialActionsActivated;
    }
}
