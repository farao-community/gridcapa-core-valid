/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.configuration;

import com.farao_community.farao.gridcapa_core_valid.app.CoreValidListener;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Configuration
public class AmqpMessagesConfiguration {

    @Value("${core-valid-runner.bindings.response.destination}")
    private String responseDestination;
    @Value("${core-valid-runner.bindings.response.expiration}")
    private String responseExpiration;
    @Value("${core-valid-runner.bindings.request.destination}")
    private String requestDestination;
    @Value("${core-valid-runner.bindings.request.routing-key}")
    private String requestRoutingKey;

    @Value("${core-valid-runner.async-time-out}")
    private long asyncTimeOut;

    @Bean
    AsyncAmqpTemplate asyncTemplate(RabbitTemplate rabbitTemplate) {
        AsyncRabbitTemplate asyncTemplate = new AsyncRabbitTemplate(rabbitTemplate);
        asyncTemplate.setReceiveTimeout(asyncTimeOut);
        return asyncTemplate;
    }

    @Bean
    public Queue coreValidRequestQueue() {
        return new Queue(requestDestination);
    }

    @Bean
    public TopicExchange coreValidTopicExchange() {
        return new TopicExchange(requestDestination);
    }

    @Bean
    public Binding coreValidRequestBinding() {
        return BindingBuilder.bind(coreValidRequestQueue()).to(coreValidTopicExchange()).with(Optional.ofNullable(requestRoutingKey).orElse("#"));
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
        return new FanoutExchange(responseDestination);
    }

    public String coreValidResponseExpiration() {
        return responseExpiration;
    }
}
