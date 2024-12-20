/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;

import java.util.Map;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public class StudyPointData {

    private final Network network;
    private final Map<String, Double> coreNetPositions;
    private final ZonalData<Scalable> scalableZonalData;
    private final FbConstraintCreationContext fbConstraintCreationContext;
    private final String jsonCracUrl;
    private final String raoParametersUrl;

    public StudyPointData(Network network, Map<String, Double> coreNetPositions, ZonalData<Scalable> scalableZonalData, FbConstraintCreationContext cracCreationContext, String jsonCracUrl, String raoParametersUrl1) {
        this.network = network;
        this.coreNetPositions = coreNetPositions;
        this.scalableZonalData = scalableZonalData;
        this.fbConstraintCreationContext = cracCreationContext;
        this.jsonCracUrl = jsonCracUrl;
        this.raoParametersUrl = raoParametersUrl1;
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

    public String getRaoParametersUrl() {
        return raoParametersUrl;
    }
}
