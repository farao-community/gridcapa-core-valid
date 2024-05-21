/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Instant;
import com.powsybl.openrao.data.cracapi.InstantKind;
import com.powsybl.openrao.data.cracapi.State;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.craccreator.FbConstraintCreationContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class LimitingBranchResultTest {

    private State state;
    private LimitingBranchResult limitingBranchResult;

    @Autowired
    private LimitingBranchResultService limitingBranchResultService;

    @Autowired
    private FileImporter fileImporter;

    @BeforeEach
    void setUp() {
        state = mock(State.class);
        limitingBranchResult = new LimitingBranchResult(
                "verticeId",
                "criticalBranchId",
                0.0,
                0.0,
                0.0,
                0.0,
                new HashSet<>(),
                "criticalBranchName",
                state);
    }

    @Test
    void importRaoResultTest() {
        final String directory = "/rao-result";
        final OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");
        Network network = Network.read("network.uct", getClass().getResourceAsStream(directory + "/network.uct"));
        FbConstraintCreationContext fbConstraintCreationContext = fileImporter.importCrac(getClass().getResource(directory + "/crac.xml").toExternalForm(), dateTime, network);
        List<LimitingBranchResult> limitingBranchResults = limitingBranchResultService.importRaoResult(new StudyPoint(1, "id", null), fbConstraintCreationContext, getClass().getResource(directory + "/raoResult.json").toExternalForm());

        assertEquals(6, limitingBranchResults.size());
        assertEquals("BE_CBCO_000003", limitingBranchResults.get(4).getCriticalBranchId());
        assertEquals("BE_CO_00001 - curative", limitingBranchResults.get(4).getState().getId());
        assertEquals(0, limitingBranchResults.get(4).getRemedialActions().size());
        assertEquals("id", limitingBranchResults.get(4).getVerticeId());
        assertEquals(-1564, Math.floor(limitingBranchResults.get(4).getRamAfter()));
        assertEquals(-1564, Math.floor(limitingBranchResults.get(4).getRamBefore()));
        assertEquals(1939, Math.floor(limitingBranchResults.get(4).getFlowAfter()));
        assertEquals(1939, Math.floor(limitingBranchResults.get(4).getFlowBefore()));
    }

    @Test
    void ramPlusFlowIsConstantTest() {
        final int delta = 1;
        final String directory = "/rao-result-bis";
        final OffsetDateTime dateTime = OffsetDateTime.parse("2019-01-08T00:30Z");
        Network network = Network.read("network.uct", getClass().getResourceAsStream(directory + "/network.uct"));
        FbConstraintCreationContext fbConstraintCreationContext = fileImporter.importCrac(getClass().getResource(directory + "/crac.xml").toExternalForm(), dateTime, network);
        List<LimitingBranchResult> limitingBranchResults = limitingBranchResultService.importRaoResult(new StudyPoint(1, "id", null), fbConstraintCreationContext, getClass().getResource(directory + "/raoResult.json").toExternalForm());

        limitingBranchResults.removeIf(result -> result.getRamAfter().equals(result.getRamBefore()));

        SoftAssertions assertions = new SoftAssertions();

        assertions.assertThat(limitingBranchResults).isNotEmpty();
        for (LimitingBranchResult result : limitingBranchResults) {
            assertions.assertThat(result.getRamAfter()).isNotEqualTo(result.getRamBefore());
            assertions.assertThat(result.getFlowAfter()).isNotEqualTo(result.getFlowBefore());
            assertions.assertThat(Math.abs((result.getRamAfter() + result.getFlowAfter()) - (result.getRamBefore() + result.getFlowBefore()))).isLessThanOrEqualTo(delta);
        }

        assertions.assertAll();
    }

    @Test
    void getCriticalBranchName() {
        assertEquals("criticalBranchName", limitingBranchResult.getCriticalBranchName());
    }

    @Test
    void getPreventiveBranchStatus() {
        Instant instant = mock(Instant.class);
        when(instant.getKind()).thenReturn(InstantKind.PREVENTIVE);
        when(state.getInstant()).thenReturn(instant);
        assertEquals("P", limitingBranchResult.getBranchStatus());
    }

    @Test
    void getOutageBranchStatus() {
        Instant instant = mock(Instant.class);
        when(instant.getKind()).thenReturn(InstantKind.OUTAGE);
        when(state.getInstant()).thenReturn(instant);
        assertEquals("O", limitingBranchResult.getBranchStatus());
    }

    @Test
    void getCurativeBranchStatus() {
        Instant instant = mock(Instant.class);
        when(instant.getKind()).thenReturn(InstantKind.CURATIVE);
        when(state.getInstant()).thenReturn(instant);
        assertEquals("C", limitingBranchResult.getBranchStatus());
    }

    @Test
    void notSupportedStateInstant() {
        Instant instant = mock(Instant.class);
        when(instant.getKind()).thenReturn(InstantKind.AUTO);
        when(state.getInstant()).thenReturn(instant);
        try {
            limitingBranchResult.getBranchStatus();
        } catch (CoreValidInvalidDataException e) {
            assertEquals("Invalid value in CBCORA file, for cnec criticalBranchId", e.getMessage());
        }
    }
}
