/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
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
        Network network = Importers.loadNetwork(filename, inputStream);
        processNetworkForCore(network);
        return network;
    }

    private static void processNetworkForCore(Network network) {
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

    private static void createGeneratorOnAlegroNodes(Network network) {
        createGeneratorOnXnode(network, "XLI_OB1B");
        createGeneratorOnXnode(network, "XLI_OB1A");
    }

    private static void createGeneratorOnXnode(Network network, String xNodeId) {
        Optional<DanglingLine> danglingLine = network.getDanglingLineStream()
                .filter(dl -> dl.getUcteXnodeCode().equals(xNodeId)).findAny();

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
