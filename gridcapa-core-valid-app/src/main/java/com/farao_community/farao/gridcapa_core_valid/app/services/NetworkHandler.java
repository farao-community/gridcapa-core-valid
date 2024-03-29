/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class NetworkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkHandler.class);
    private static final String ALEGRO_GEN_BE = "XLI_OB1B_generator";
    private static final String ALEGRO_GEN_DE = "XLI_OB1A_generator";

    private NetworkHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static Network loadNetwork(String filename, InputStream inputStream) {
        LOGGER.info("IIDM import of network : {}", filename);
        Network network = Network.read(filename, inputStream);
        processNetworkForCore(network);
        return network;
    }

    static void processNetworkForCore(Network network) {
        /*
         UCTE-DEF file does not provide configuration for default nominal voltage setup.

         This post processor modifies default nominal voltages in order to adapt it to FMAX
         calculation based on IMAX.

         By default, UCTE sets nominal voltage to 220 and 380kV for the voltage levels 6 and 7, whereas
         default values of Core countries are 225 and 400 kV instead.
         */
        updateVoltageLevelNominalV(network);

        /*
        When importing an UCTE network file, powsybl merges its X-nodes into dangling lines.
        It can cause an error if a GLSK file associated to this network includes some factors on
        xNodes. The GLSK importers looks for a Generator (GSK) or Load (LSK) associated to this
        xNode. If the Generator/Load does not exist, the GLSK cannot be created.

        This post processor fix this problem, by creating for these two nodes a fictitious generator (P, Q = 0),
        connected to the voltage level on which the dangling lines are linked.
        */
        createGeneratorOnAlegroNodes(network);

    }

    static void updateVoltageLevelNominalV(Network network) {
        network.getVoltageLevelStream().forEach(voltageLevel -> {
            if (safeDoubleEquals(voltageLevel.getNominalV(), 380)) {
                voltageLevel.setNominalV(400);
            } else if (safeDoubleEquals(voltageLevel.getNominalV(), 220)) {
                voltageLevel.setNominalV(225);
            }
            // Else it should not be changed cause is not equal to the default nominal voltage of voltage levels 6 or 7
        });
    }

    private static boolean safeDoubleEquals(double a, double b) {
        return Math.abs(a - b) < 1e-3;
    }

    static void createGeneratorOnAlegroNodes(Network network) {
        createGeneratorOnXnode(network, "XLI_OB1B");
        createGeneratorOnXnode(network, "XLI_OB1A");
    }

    private static void createGeneratorOnXnode(Network network, String xNodeId) {
        Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                .filter(dl -> dl.getPairingKey().equals(xNodeId))
                .findAny();

        if (danglingLine.isPresent() && danglingLine.get().getTerminal().isConnected()) {
            Bus xNodeBus = danglingLine.get().getTerminal().getBusBreakerView().getConnectableBus();
            xNodeBus.getVoltageLevel().newGenerator()
                    .setBus(xNodeBus.getId())
                    .setEnsureIdUnicity(true)
                    .setId(xNodeId + "_generator")
                    .setMaxP(9999)
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setTargetV(xNodeBus.getVoltageLevel().getNominalV())
                    .setVoltageRegulatorOn(false)
                    .add()
                    .newMinMaxReactiveLimits().setMaxQ(99999).setMinQ(99999).add();
        }
    }

    public static void removeAlegroVirtualGeneratorsFromNetwork(Network network) {
        Optional.ofNullable(network.getGenerator(ALEGRO_GEN_BE)).ifPresent(Generator::remove);
        Optional.ofNullable(network.getGenerator(ALEGRO_GEN_DE)).ifPresent(Generator::remove);
    }

}
