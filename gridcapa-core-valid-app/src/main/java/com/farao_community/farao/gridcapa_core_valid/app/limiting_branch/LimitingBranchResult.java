/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.gridcapa_core_valid.app.limiting_branch;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.powsybl.openrao.data.crac.api.RemedialAction;
import com.powsybl.openrao.data.crac.api.State;

import java.util.Set;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public record LimitingBranchResult(String verticeId,
                                                                String criticalBranchId,
                                                                Double ramBefore,
                                                                Double ramAfter,
                                                                Double flowBefore,
                                                                Double flowAfter,
                                                                Set<RemedialAction<?>> remedialActions,
                                                                String criticalBranchName,
                                                                State state) {

    public String getBranchStatus() {
        return switch (state().getInstant().getKind()) {
            case PREVENTIVE -> "P";
            case OUTAGE -> "O";
            case CURATIVE -> "C";
            default ->
                    throw new CoreValidInvalidDataException(String.format("Invalid value in CBCORA file, for cnec %s", criticalBranchId()));
        };
    }
}
