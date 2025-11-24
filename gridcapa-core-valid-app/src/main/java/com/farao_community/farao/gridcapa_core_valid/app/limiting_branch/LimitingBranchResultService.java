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
import com.powsybl.openrao.data.crac.api.Instant;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;
import com.powsybl.openrao.data.crac.api.cnec.Cnec;
import com.powsybl.openrao.data.crac.api.cnec.FlowCnec;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import com.powsybl.openrao.data.crac.api.rangeaction.RangeAction;
import com.powsybl.openrao.data.crac.io.commons.api.stdcreationcontext.BranchCnecCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.data.raoresult.io.json.RaoResultJsonImporter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;

import static com.powsybl.openrao.commons.Unit.MEGAWATT;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

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

    public List<LimitingBranchResult> importRaoResult(final StudyPoint studyPoint,
                                                      final FbConstraintCreationContext cracCreationContext,
                                                      final String raoResultUrl) {
        final String vertexId = studyPoint.getVertexId();
        try (final InputStream raoResultStream = urlValidationService.openUrlStream(raoResultUrl)) {

            final RaoResult raoResult = new RaoResultJsonImporter().importData(raoResultStream,
                                                                               cracCreationContext.getCrac());
            final List<LimitingBranchResult> listLimitingBranches = new ArrayList<>();
            cracCreationContext.getBranchCnecCreationContexts()
                    .stream()
                    .filter(BranchCnecCreationContext::isImported)
                    .forEach(context -> {
                        final String criticalBranchId = context.getNativeObjectId();
                        context.getCreatedCnecsIds().values()
                                .stream()
                                .map(id -> cracCreationContext.getCrac().getFlowCnec(id))
                                .map(cnec -> createLimitingBranchResult(vertexId, criticalBranchId, raoResult, cnec))
                                .forEach(listLimitingBranches::add);

                    });
            return listLimitingBranches;
        } catch (final IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot import RaoResult file from URL '%s'", raoResultUrl), e);
        }
    }

    private LimitingBranchResult createLimitingBranchResult(final String studyPointId,
                                                            final String criticalBranchId,
                                                            final RaoResult raoResult,
                                                            final FlowCnec cnec) {
        final Double ramBefore = raoResult.getMargin(null, cnec, MEGAWATT);
        final Double ramAfter = raoResult.getMargin(cnec.getState().getInstant(), cnec, MEGAWATT);
        final TwoSides cnecSide = cnec.getMonitoredSides().stream().collect(toOne());
        final Double flowBefore = getFlow(raoResult, null, cnec, cnecSide);
        final Double flowAfter = getFlow(raoResult, cnec.getState().getInstant(), cnec, cnecSide);
        final Set<RemedialAction<?>> remedialActions = getRemedialActions(raoResult, cnec);
        final String criticalBranchName = cnec.getName();
        final State state = cnec.getState();

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

    private static double getFlow(final RaoResult raoResult,
                                  final Instant optimizedInstant,
                                  final FlowCnec cnec,
                                  final TwoSides cnecSide) {
        final Optional<Double> upperBound = cnec.getUpperBound(cnecSide, MEGAWATT);
        final Optional<Double> lowerBound = cnec.getLowerBound(cnecSide, MEGAWATT);

        double flow = raoResult.getFlow(optimizedInstant, cnec, cnecSide, MEGAWATT);
        if (upperBound.isEmpty() && lowerBound.isPresent()) {
            flow = -flow;
        } else if (upperBound.isPresent() && lowerBound.isPresent()) {
            flow = Math.abs(flow);
        }

        return flow;
    }

    private Set<RemedialAction<?>> getRemedialActions(final RaoResult raoResult,
                                                      final Cnec<?> cnec) {
        final Set<NetworkAction> networkActions = raoResult.getActivatedNetworkActionsDuringState(cnec.getState());
        final Set<RangeAction<?>> rangeActions = raoResult.getActivatedRangeActionsDuringState(cnec.getState());
        final Set<RemedialAction<?>> remedialActionsActivated = new HashSet<>();
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
        return collectingAndThen(toList(), list -> {
            if (list.size() == 1) {
                return list.getFirst();
            }
            throw new CoreValidInvalidDataException("Found " + list.size() + " element(s), expected exactly one.");
        });
    }
}
