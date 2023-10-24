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
import com.farao_community.farao.data.crac_api.cnec.Side;
import com.farao_community.farao.data.crac_api.network_action.NetworkAction;
import com.farao_community.farao.data.crac_api.range_action.RangeAction;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.rao_result_api.RaoResult;
import com.farao_community.farao.data.rao_result_json.RaoResultImporter;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.services.UrlValidationService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
        Double ramBefore = raoResult.getMargin(null, cnec, Unit.MEGAWATT);
        Double ramAfter = raoResult.getMargin(cnec.getState().getInstant(), cnec, Unit.MEGAWATT);
        Side cnecSide = cnec.getMonitoredSides().stream().collect(toOne());
        Double flowBefore = getFlow(raoResult, null, cnec, cnecSide);
        Double flowAfter = getFlow(raoResult, cnec.getState().getInstant(), cnec, cnecSide);
        Set<RemedialAction<?>> remedialActions = getRemedialActions(raoResult, cnec);
        String criticalBranchName = cnec.getName();
        State state = cnec.getState();
        return new LimitingBranchResult(
                studyPointId,
                criticalBranchId,
                ramBefore,
                ramAfter,
                flowBefore,
                flowAfter,
                remedialActions,
                criticalBranchName,
                state
        );
    }

    private static double getFlow(RaoResult raoResult, Instant optimizedInstant, FlowCnec cnec, Side cnecSide) {
        Optional<Double> upperBound = cnec.getUpperBound(cnecSide, Unit.MEGAWATT);
        Optional<Double> lowerBound = cnec.getLowerBound(cnecSide, Unit.MEGAWATT);

        double flow = raoResult.getFlow(optimizedInstant, cnec, cnecSide, Unit.MEGAWATT);
        if (upperBound.isEmpty() && lowerBound.isPresent()) {
            flow = -flow;
        } else if (upperBound.isPresent() && lowerBound.isPresent()) {
            flow = Math.abs(flow);
        }

        return flow;
    }

    private Set<RemedialAction<?>> getRemedialActions(RaoResult raoResult, Cnec<?> cnec) {
        Set<NetworkAction> networkActions = raoResult.getActivatedNetworkActionsDuringState(cnec.getState());
        Set<RangeAction<?>> rangeActions = raoResult.getActivatedRangeActionsDuringState(cnec.getState());
        Set<RemedialAction<?>> remedialActionsActivated = new HashSet<>();
        remedialActionsActivated.addAll(networkActions);
        remedialActionsActivated.addAll(rangeActions);
        return remedialActionsActivated;
    }

    /**
     * This collector only allows 1 element in the stream. It returns the result.
     *
     * @param <T> Type of the element for the collector.
     * @return The value if there is exactly one in the stream.
     * It would throw an exception if there isn't exactly one element (zero or more) in the stream.
     */
    private static <T> Collector<T, ?, T> toOne() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() == 1) {
                return list.get(0);
            }
            throw new CoreValidInvalidDataException("Found " + list.size() + " element(s), expected exactly one.");
        });
    }
}
