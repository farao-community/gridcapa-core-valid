/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CoreValidClient {
    private static final int DEFAULT_PRIORITY = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidClient.class);

    private final AmqpTemplate amqpTemplate;
    private final CoreValidClientProperties coreValidClientProperties;
    private final CoreValidMessageHandler coreValidMessageHandler;

    public CoreValidClient(AmqpTemplate amqpTemplate, CoreValidClientProperties coreValidClientProperties) {
        this.amqpTemplate = amqpTemplate;
        this.coreValidClientProperties = coreValidClientProperties;
        this.coreValidMessageHandler = new CoreValidMessageHandler(coreValidClientProperties);
    }

    public CoreValidResponse run(CoreValidRequest coreValidRequest, int priority) {
        LOGGER.info("Core valid request sent: {}", coreValidRequest);
        Message responseMessage = amqpTemplate.sendAndReceive(coreValidClientProperties.getAmqp().getQueueName(), coreValidMessageHandler.buildMessage(coreValidRequest, priority));
        CoreValidResponse coreValidResponse = coreValidMessageHandler.readMessage(responseMessage);
        LOGGER.info("Core valid response received: {}", coreValidResponse);
        return coreValidResponse;
    }

    public CoreValidResponse run(CoreValidRequest coreValidRequest) {
        return run(coreValidRequest, DEFAULT_PRIORITY);
    }
}
