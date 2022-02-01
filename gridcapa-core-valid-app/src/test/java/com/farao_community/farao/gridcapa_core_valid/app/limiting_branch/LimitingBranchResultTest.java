/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@SpringBootTest
class LimitingBranchResultTest {

    private final String raoDirectory = "/rao-result";
    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");

    @Autowired
    private LimitingBranchResultService limitingBranchResultService;

    @Autowired
    private FileImporter fileImporter;

    @Test
    void importRaoResultTest() {
        Network network = Importers.loadNetwork("network.uct", getClass().getResourceAsStream(raoDirectory + "/network.uct"));
        FbConstraintCreationContext crac = fileImporter.importCrac(getClass().getResource(raoDirectory + "/crac.xml").toExternalForm(), dateTime, network);
        List<LimitingBranchResult> limitingBranchResults = limitingBranchResultService.importRaoResult(new StudyPoint(1, "id", null), crac, getClass().getResource(raoDirectory + "/raoResult.json").toExternalForm());

        assertEquals(6, limitingBranchResults.size());
        assertEquals("BE_CBCO_000003", limitingBranchResults.get(4).getCriticalBranchId());
        assertEquals("BE_CO_00001 - curative", limitingBranchResults.get(4).getState().getId());
        assertEquals(0, limitingBranchResults.get(4).getRemedialActions().size());
        assertEquals("id", limitingBranchResults.get(4).getVerticeID());
        assertEquals(-1564, Math.floor(limitingBranchResults.get(4).getRamAfter()));
        assertEquals(-1564, Math.floor(limitingBranchResults.get(4).getRamBefore()));
        assertEquals(1939, Math.floor(limitingBranchResults.get(4).getFlowAfter()));
        assertEquals(1939, Math.floor(limitingBranchResults.get(4).getFlowBefore()));
    }

}
