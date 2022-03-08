/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidResponseTest {

    @Test
    void checkDCoreValidResponse() {
        Instant computationStartInstant = Instant.parse("2021-01-01T00:30:00Z");
        Instant computationEndInstant = Instant.parse("2021-01-01T00:35:00Z");
        String resultFileUrl = "testUrl";
        CoreValidResponse coreValidResponse = new CoreValidResponse("id", resultFileUrl, resultFileUrl, resultFileUrl, computationStartInstant, computationEndInstant);
        assertNotNull(coreValidResponse);
        assertEquals("id", coreValidResponse.getId());
        assertEquals(resultFileUrl, coreValidResponse.getMainResultFileUrl());
        assertEquals(resultFileUrl, coreValidResponse.getRexResultFileUrl());
        assertEquals(resultFileUrl, coreValidResponse.getRemedialActionsFileUrl());
        assertEquals("2021-01-01T00:30:00Z", coreValidResponse.getComputationStartInstant().toString());
        assertEquals("2021-01-01T00:35:00Z", coreValidResponse.getComputationEndInstant().toString());
    }
}
