/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class SearchTreeRaoConfigurationTest {

    @Autowired
    private SearchTreeRaoConfiguration searchTreeRaoConfiguration;

    @Test
    void getRaoConfigurationTest() {
        assertEquals(1, searchTreeRaoConfiguration.getMaxCurativeTopoPerTso().getOrDefault("FR", 0));
        assertEquals(0, searchTreeRaoConfiguration.getMaxCurativePstPerTso().get("FR"));
        assertEquals(1, searchTreeRaoConfiguration.getMaxCurativeRaPerTso().get("FR"));
    }
}
