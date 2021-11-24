/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Configuration
@EnableConfigurationProperties(CoreValidClientProperties.class)
public class CoreValidClientAutoConfiguration {
    private final CoreValidClientProperties clientProperties;

    public CoreValidClientAutoConfiguration(CoreValidClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    @Bean
    public CoreValidClient cseClient(AmqpTemplate amqpTemplate) {
        return new CoreValidClient(amqpTemplate, clientProperties);
    }
}
