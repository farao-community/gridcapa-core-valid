/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@ConfigurationProperties("core-valid-runner")
public record CoreValidClientProperties(BindingConfiguration binding) {
    public record BindingConfiguration(String destination, String routingKey, String expiration, String applicationId) {
        public BindingConfiguration(final String destination,
                                    final String routingKey,
                                    final String expiration,
                                    final String applicationId) {
            this.destination = destination;
            this.routingKey = Optional.ofNullable(routingKey).orElse("#");
            this.expiration = expiration;
            this.applicationId = applicationId;
        }
    }
}
