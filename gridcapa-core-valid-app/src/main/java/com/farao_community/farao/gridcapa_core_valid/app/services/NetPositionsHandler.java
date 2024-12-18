/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.app.CoreAreasId;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public final class NetPositionsHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetPositionsHandler.class);

    private NetPositionsHandler() {
        throw new IllegalStateException("Utility class");
    }

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
                    Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                            .filter(dl -> dl.getPairingKey().equals("XLI_OB1B"))
                            .findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(-netPosition));
                } else if (studyPointZoneId.equals("NP_DE_ALEGrO")) {
                    // XLI_OB1A
                    Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                            .filter(dl -> dl.getPairingKey().equals("XLI_OB1A"))
                            .findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(-netPosition));
                } else {
                    String zone = CoreAreasId.ID_MAPPING.get(studyPointZoneId);
                    double shift = netPosition - coreNetPositions.getOrDefault(zone, 0.);
                    LOGGER.info("Shift for zone {} : Study point {} | Ref prog {} | variation {}", zone, netPosition, coreNetPositions.getOrDefault(zone, 0.), shift);
                    String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
                    Scalable scalable = scalableZonalData.getData(zoneEiCode);
                    if (scalable != null) {
                        ScalingParameters scalingParameters = new ScalingParameters().setPriority(ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED);
                        scalable.scale(network, shift, scalingParameters);
                    }
                }
            } catch (Exception e) {
                throw new CoreValidInternalException("Error during the net position shift for zone " + CoreAreasId.ID_MAPPING.get(studyPointZoneId), e);
            }
        });
    }
}
