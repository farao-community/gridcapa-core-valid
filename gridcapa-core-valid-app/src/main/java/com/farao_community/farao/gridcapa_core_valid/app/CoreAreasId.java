/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import java.util.Map;

import static java.util.Map.entry;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CoreAreasId {
    public static final Map<String, String> ID_MAPPING = Map.ofEntries(
            entry("AT", "NP_AT"),
            entry("BE", "NP_BE"),
            entry("CZ", "NP_CZ"),
            entry("DE", "NP_DE"),
            entry("FR", "NP_FR"),
            entry("HR", "NP_HR"),
            entry("HU", "NP_HU"),
            entry("NL", "NP_NL"),
            entry("PL", "NP_PL"),
            entry("RO", "NP_RO"),
            entry("SI", "NP_SI"),
            entry("SK", "NP_SK"),
            entry("22Y201903144---9", "NP_BE_ALEGrO"),
            entry("22Y201903145---4", "NP_DE_ALEGrO")
    );

    public static Map<String, String> getIdMapping() {
        return ID_MAPPING;
    }
}
