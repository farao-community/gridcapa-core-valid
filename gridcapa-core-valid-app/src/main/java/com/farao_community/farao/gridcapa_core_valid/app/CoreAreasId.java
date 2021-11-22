/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CoreAreasId {
    public static final Map<String, String> ID_MAPPING = Map.ofEntries(
            entry("NP_AT", "AT"),
            entry("NP_BE", "BE"),
            entry("NP_CZ", "CZ"),
            entry("NP_DE", "DE"),
            entry("NP_FR", "FR"),
            entry("NP_HR", "HR"),
            entry("NP_HU", "HU"),
            entry("NP_NL", "NL"),
            entry("NP_PL", "PL"),
            entry("NP_RO", "RO"),
            entry("NP_SI", "SI"),
            entry("NP_SK", "SK"),
            entry("NP_BE_ALEGrO", "22Y201903144---9"),
            entry("NP_DE_ALEGrO", "22Y201903145---4")
    );

    public static List<String> getCountriesId() {
        List<String> ids = new ArrayList<>(ID_MAPPING.values());
        ids.removeIf(id -> id.equals("22Y201903144---9") || id.equals("22Y201903145---4"));
        return ids;
    }
}
