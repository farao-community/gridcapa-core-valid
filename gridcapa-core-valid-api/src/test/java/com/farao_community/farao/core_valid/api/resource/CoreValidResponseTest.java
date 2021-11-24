/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidResponseTest {

    @Test
    void checkDCoreValidResponse() {
        CoreValidResponse coreValidResponse = new CoreValidResponse("id");
        assertNotNull(coreValidResponse);
        assertEquals("id", coreValidResponse.getId());
        //todo complete test when result files added
    }
}
