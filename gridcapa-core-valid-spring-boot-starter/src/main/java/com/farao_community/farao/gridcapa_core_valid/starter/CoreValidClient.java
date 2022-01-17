/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.core_valid.api.JsonApiConverter;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;

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

    public CoreValidResponse run(CoreValidRequest coreValidRequest, int priority) {
        LOGGER.info("Core valid request sent: {}", coreValidRequest);
        Message responseMessage = amqpTemplate.sendAndReceive(coreValidClientProperties.getAmqp().getQueueName(), buildMessage(coreValidRequest, priority));
        if (responseMessage != null) {
            return CoreValidResponseConversionHelper.convertCoreValidResponse(responseMessage, jsonConverter);
        } else {
            throw new CoreValidInternalException("Core valid Runner server did not respond");
        }
    }

    public CoreValidResponse run(CoreValidRequest coreValidRequest) {
        return run(coreValidRequest, DEFAULT_PRIORITY);
    }

    public Message buildMessage(CoreValidRequest coreValidRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(coreValidRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(coreValidClientProperties.getAmqp().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(coreValidClientProperties.getAmqp().getExpiration())
                .setPriority(priority)
                .build();
    }
}
