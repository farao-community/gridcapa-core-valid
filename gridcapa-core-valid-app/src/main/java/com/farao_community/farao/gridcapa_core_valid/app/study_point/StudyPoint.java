/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class StudyPoint {
    private int period;
    private String id;
    private Map<String, Double> positions;

    public StudyPoint(int period, String id, Map<String, Double> positions) {
        this.period = period;
        this.id = id;
        this.positions = positions;
    }

    public int getPeriod() {
        return period;
    }

    public String getId() {
        return id;
    }

    public Map<String, Double> getPositions() {
        return positions;
    }
}
