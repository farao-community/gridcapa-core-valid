/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.net_position;

import com.farao_community.farao.commons.CountryEICode;
import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.gridcapa_core_valid.app.CoreAreasId;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetPositionsHandler {

    public static Map<String, Double> computeCoreReferenceNetPositions(ReferenceProgram referenceProgram) {
        Map<String, Double> coreNetPositions = new TreeMap<>();
        referenceProgram.getReferenceExchangeDataList().forEach(referenceExchangeData -> {
            String areaIn = referenceExchangeData.getAreaIn().toString();
            String areaOut = referenceExchangeData.getAreaOut().toString();
            if (CoreAreasId.ID_MAPPING.containsValue(areaIn) && CoreAreasId.ID_MAPPING.containsValue(areaOut)) {
                coreNetPositions.put(areaIn, coreNetPositions.getOrDefault(areaIn, 0.) - referenceExchangeData.getFlow());
                coreNetPositions.put(areaOut, coreNetPositions.getOrDefault(areaOut, 0.) + referenceExchangeData.getFlow());
            }
        });
        return coreNetPositions;
    }

    public static void shiftNetPositionToStudyPoint(Network network, StudyPoint studyPoint, ZonalData<Scalable> scalableZonalData, Map<String, Double> coreNetPositions) {
        studyPoint.getPositions().forEach((studyPointZoneId, netPosition) -> {
            try {
                if (studyPointZoneId.equals("NP_BE_ALEGrO")) {
                    // XLI_OB1B
                    Optional<DanglingLine> danglingLine = network.getDanglingLineStream().filter(dl -> dl.getUcteXnodeCode().equals("XLI_OB1B")).findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(netPosition));
                } else if (studyPointZoneId.equals("NP_DE_ALEGrO")) {
                    // XLI_OB1A
                    Optional<DanglingLine> danglingLine = network.getDanglingLineStream().filter(dl -> dl.getUcteXnodeCode().equals("XLI_OB1A")).findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(netPosition));
                } else {
                    String zone = CoreAreasId.ID_MAPPING.get(studyPointZoneId);
                    double shift = netPosition - coreNetPositions.getOrDefault(zone, 0.);
                    String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
                    Scalable scalable = scalableZonalData.getData(zoneEiCode);
                    if (scalable != null) {
                        scalable.scale(network, shift);
                    }
                }
            } catch (Exception e) {
                throw new CoreValidInternalException("Error during the net position shift for zone " + CoreAreasId.ID_MAPPING.get(studyPointZoneId), e);
            }
        });
    }
}
