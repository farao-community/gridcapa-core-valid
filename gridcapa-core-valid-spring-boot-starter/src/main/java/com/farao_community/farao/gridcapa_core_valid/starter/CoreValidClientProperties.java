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
public class CoreValidClientProperties {
    private BindingConfiguration binding;

    public BindingConfiguration getBinding() {
        return binding;
    }

    public void setBinding(final BindingConfiguration binding) {
        this.binding = binding;
    }

    public static class BindingConfiguration {
        private String destination;
        private String routingKey;
        private String expiration;
        private String applicationId;

        public String getDestination() {
            return destination;
        }

        public void setDestination(final String destination) {
            this.destination = destination;
        }

        public String getRoutingKey() {
            return Optional.ofNullable(routingKey).orElse("#");
        }

        public void setRoutingKey(final String routingKey) {
            this.routingKey = routingKey;
        }

        public String getExpiration() {
            return expiration;
        }

        public void setExpiration(final String expiration) {
            this.expiration = expiration;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(final String applicationId) {
            this.applicationId = applicationId;
        }
    }
}
