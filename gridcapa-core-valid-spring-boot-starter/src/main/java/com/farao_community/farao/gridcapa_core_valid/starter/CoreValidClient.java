/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

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
    private final CoreValidMessageHandler coreValidMessageHandler;

    public CoreValidClient(AmqpTemplate amqpTemplate, CoreValidClientProperties coreValidClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.coreValidClientProperties = coreValidClientProperties;
        this.coreValidMessageHandler = new CoreValidMessageHandler(coreValidClientProperties);
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass, int priority) {
        LOGGER.info("Core valid request sent: {}", request);
        Message responseMessage = amqpTemplate.sendAndReceive(
                coreValidClientProperties.getBinding().getDestination(),
                coreValidClientProperties.getBinding().getRoutingKey(),
                coreValidMessageHandler.buildMessage(request, requestClass, priority));
        J response = coreValidMessageHandler.readMessage(responseMessage, responseClass);
        LOGGER.info("Response received: {}", response);
        return response;
    }

    public <I, J> J run(I request, Class<I> requestClass, Class<J> responseClass) {
        return run(request, requestClass, responseClass, DEFAULT_PRIORITY);
    }
}
