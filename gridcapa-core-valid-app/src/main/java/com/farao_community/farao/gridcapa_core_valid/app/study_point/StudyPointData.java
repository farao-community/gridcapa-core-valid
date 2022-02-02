/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
public class StudyPointData {

    private Network network;
    private Map<String, Double> coreNetPositions;
    private ZonalData<Scalable> scalableZonalData;
    private FbConstraintCreationContext fbConstraintCreationContext;
    private String jsonCracUrl;

    public StudyPointData(Network network, Map<String, Double> coreNetPositions, ZonalData<Scalable> scalableZonalData, FbConstraintCreationContext cracCreationContext, String jsonCracUrl) {
        this.network = network;
        this.coreNetPositions = coreNetPositions;
        this.scalableZonalData = scalableZonalData;
        this.fbConstraintCreationContext = cracCreationContext;
        this.jsonCracUrl = jsonCracUrl;
    }

    public Network getNetwork() {
        return network;
    }

    public Map<String, Double> getCoreNetPositions() {
        return coreNetPositions;
    }

    public ZonalData<Scalable> getScalableZonalData() {
        return scalableZonalData;
    }

    public String getJsonCracUrl() {
        return jsonCracUrl;
    }

    public FbConstraintCreationContext getFbConstraintCreationContext() {
        return fbConstraintCreationContext;
    }
}