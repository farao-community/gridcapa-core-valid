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

import static com.farao_community.farao.gridcapa_core_valid.app.CoreValidConstants.ALEGRO_BE_NODE_ID;
import static com.farao_community.farao.gridcapa_core_valid.app.CoreValidConstants.ALEGRO_DE_NODE_ID;
import static com.powsybl.iidm.modification.scalable.ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED;

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
        final Map<String, Double> coreNetPositions = new TreeMap<>();
        referenceProgram.getReferenceExchangeDataList().forEach(refXData -> {
            final String areaIn = refXData.getAreaIn().toString();
            final String areaOut = refXData.getAreaOut().toString();
            final double refFlow = refXData.getFlow();

            if (isAreaInCore(areaIn) && isAreaInCore(areaOut)) {
                coreNetPositions.put(areaIn, coreNetPositions.getOrDefault(areaIn, 0.) - refFlow);
                coreNetPositions.put(areaOut, coreNetPositions.getOrDefault(areaOut, 0.) + refFlow);
            }
        });
        return coreNetPositions;
    }

    private static boolean isAreaInCore(final String area) {
        return CoreAreasId.ID_MAPPING.containsValue(area);
    }

    public static void shiftNetPositionToStudyPoint(final Network network,
                                                    final StudyPoint studyPoint,
                                                    final ZonalData<Scalable> scalableZonalData,
                                                    final Map<String, Double> coreNetPositions) {
        studyPoint.getPositions().forEach((studyPointZoneId, netPosition) -> {
            try {
                if (studyPointZoneId.equals("NP_BE_ALEGrO")) {
                    // XLI_OB1B
                    final Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                            .filter(dl -> ALEGRO_BE_NODE_ID.equals(dl.getPairingKey()))
                            .findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(-netPosition));
                } else if (studyPointZoneId.equals("NP_DE_ALEGrO")) {
                    // XLI_OB1A
                    final Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                            .filter(dl -> ALEGRO_DE_NODE_ID.equals(dl.getPairingKey()))
                            .findAny();
                    danglingLine.ifPresent(dl -> dl.setP0(-netPosition));
                } else {
                    final String zone = CoreAreasId.ID_MAPPING.get(studyPointZoneId);
                    final double refProg = coreNetPositions.getOrDefault(zone, 0.);
                    final double shift = netPosition - refProg;
                    LOGGER.info("Shift for zone {} : Study point {} | Ref prog {} | variation {}", zone, netPosition, refProg, shift);
                    final String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
                    final Scalable scalable = scalableZonalData.getData(zoneEiCode);
                    if (scalable != null) {
                        final ScalingParameters scalingParameters = new ScalingParameters().setPriority(RESPECT_OF_VOLUME_ASKED);
                        scalable.scale(network, shift, scalingParameters);
                    }
                }
            } catch (final Exception e) {
                throw new CoreValidInternalException("Error during the net position shift for zone " + CoreAreasId.ID_MAPPING.get(studyPointZoneId), e);
            }
        });
    }
}
