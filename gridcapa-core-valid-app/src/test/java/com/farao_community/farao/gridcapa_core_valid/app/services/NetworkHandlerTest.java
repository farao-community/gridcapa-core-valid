/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
class NetworkHandlerTest {

    @Test
    void updateVoltageLevelNominalV() {
        Network network = Network.read("20210723_0030_2D5_CGM.uct", getClass().getResourceAsStream("/20210723/20210723_0030_2D5_CGM.uct"));
        assertEquals(380, network.getVoltageLevel("BBE1AA1").getNominalV(), 1);
        assertEquals(220, network.getVoltageLevel("BBE2AA2").getNominalV(), 1);

        NetworkHandler.updateVoltageLevelNominalV(network);
        assertEquals(400, network.getVoltageLevel("BBE1AA1").getNominalV(), 1);
        assertEquals(225, network.getVoltageLevel("BBE2AA2").getNominalV(), 1);
    }
}
