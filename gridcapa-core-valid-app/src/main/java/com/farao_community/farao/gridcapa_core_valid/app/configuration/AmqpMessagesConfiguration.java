/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.configuration;

import com.farao_community.farao.gridcapa_core_valid.app.CoreValidListener;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Configuration
public class AmqpMessagesConfiguration {

    private final CoreValidServerProperties serverProperties;

    public AmqpMessagesConfiguration(CoreValidServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean
    public Queue coreValidRequestQueue() {
        return new Queue(serverProperties.getRequests().getQueueName());
    }

    @Bean
    public MessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory,
                                                             Queue coreValidRequestQueue,
                                                             CoreValidListener listener) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
        simpleMessageListenerContainer.setConnectionFactory(connectionFactory);
        simpleMessageListenerContainer.setQueues(coreValidRequestQueue);
        simpleMessageListenerContainer.setMessageListener(listener);
        return simpleMessageListenerContainer;
    }

    @Bean
    public FanoutExchange coreValidResponseExchange() {
        return new FanoutExchange(serverProperties.getResponses().getExchange());
    }

    public String coreValidResponseExpiration() {
        return serverProperties.getResponses().getExpiration();
    }
}
