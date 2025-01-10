/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.gridcapa_core_valid.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CoreValidClient {
    private static final int DEFAULT_PRIORITY = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidClient.class);
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final AmqpTemplate amqpTemplate;
    private final CoreValidClientProperties coreValidClientProperties;
    private final JsonApiConverter jsonConverter;

    public CoreValidClient(AmqpTemplate amqpTemplate, CoreValidClientProperties coreValidClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.coreValidClientProperties = coreValidClientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public void run(CoreValidRequest coreValidRequest, int priority) {
        LOGGER.info("Core valid request sent: {}", coreValidRequest);
        amqpTemplate.send(coreValidClientProperties.getBinding().getDestination(), buildMessage(coreValidRequest, priority));
    }

    public void run(CoreValidRequest coreValidRequest) {
        run(coreValidRequest, DEFAULT_PRIORITY);
    }

    public Message buildMessage(CoreValidRequest coreValidRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(coreValidRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(coreValidClientProperties.getBinding().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(coreValidClientProperties.getBinding().getExpiration())
                .setPriority(priority)
                .build();
    }
}
