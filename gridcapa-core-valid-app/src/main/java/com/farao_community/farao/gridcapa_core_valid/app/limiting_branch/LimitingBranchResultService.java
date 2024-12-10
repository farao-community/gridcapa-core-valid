/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.services.UrlValidationService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.cracapi.cnec.Cnec;
import com.powsybl.openrao.data.cracapi.cnec.FlowCnec;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import com.powsybl.openrao.data.cracapi.rangeaction.RangeAction;
import com.powsybl.openrao.data.cracio.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultJsonImporter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class LimitingBranchResultService {

    private final UrlValidationService urlValidationService;

    public LimitingBranchResultService(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public List<LimitingBranchResult> importRaoResult(StudyPoint studyPoint, FbConstraintCreationContext cracCreationContext, String raoResultUrl) {
        String verticeId = studyPoint.getVerticeId();
        try (InputStream raoResultStream = urlValidationService.openUrlStream(raoResultUrl)) {

            RaoResult raoResult = new RaoResultJsonImporter().importData(raoResultStream, cracCreationContext.getCrac());
            List<LimitingBranchResult> listLimitingBranches = new ArrayList<>();
            cracCreationContext.getBranchCnecCreationContexts().forEach(branchCnecCreationContext -> {
                if (branchCnecCreationContext.isImported()) {
                    String criticalBranchId = branchCnecCreationContext.getNativeObjectId();
                    Map<String, String> flowCnecsIds = branchCnecCreationContext.getCreatedCnecsIds();
                    flowCnecsIds.forEach((instant, flowCnecId) -> {
                        FlowCnec cnec = cracCreationContext.getCrac().getFlowCnec(flowCnecId);
                        LimitingBranchResult branchResult = createLimitingBranchResult(verticeId, criticalBranchId, raoResult, cnec);
                        listLimitingBranches.add(branchResult);
                    });
                }
            });
            return listLimitingBranches;
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot import RaoResult file from URL '%s'", raoResultUrl), e);
        }
    }

    private LimitingBranchResult createLimitingBranchResult(String studyPointId, String criticalBranchId, RaoResult raoResult, FlowCnec cnec) {
        Double ramBefore = raoResult.getMargin(null, cnec, Unit.MEGAWATT);
        Double ramAfter = raoResult.getMargin(cnec.getState().getInstant(), cnec, Unit.MEGAWATT);
        TwoSides cnecSide = cnec.getMonitoredSides().stream().collect(toOne());
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

    private static double getFlow(RaoResult raoResult, Instant optimizedInstant, FlowCnec cnec, TwoSides cnecSide) {
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
