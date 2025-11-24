/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app;

import java.time.ZoneId;

public final class CoreValidConstants {

    private CoreValidConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ALEGRO_DE_NODE_ID = "XLI_OB1A";
    public static final String ALEGRO_BE_NODE_ID = "XLI_OB1B";
    public static final ZoneId PARIS_ZONE_ID = ZoneId.of("Europe/Paris");
}
