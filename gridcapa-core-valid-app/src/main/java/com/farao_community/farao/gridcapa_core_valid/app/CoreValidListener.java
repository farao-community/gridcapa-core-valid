/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.core_valid.api.JsonConverter;
import com.farao_community.farao.core_valid.api.exception.AbstractCoreValidException;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.AmqpMessagesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.stereotype.Component;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidListener.class);
    private static final String APPLICATION_ID = "core-valid-server";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;

    private final JsonConverter jsonConverter;
    private final AmqpTemplate amqpTemplate;
    private final CoreValidHandler coreValidHandler;
    private final AmqpMessagesConfiguration amqpMessagesConfiguration;

    public CoreValidListener(CoreValidHandler coreValidHandler, AmqpTemplate amqpTemplate, AmqpMessagesConfiguration amqpMessagesConfiguration) {
        this.jsonConverter = new JsonConverter();
        this.coreValidHandler = coreValidHandler;
        this.amqpTemplate = amqpTemplate;
        this.amqpMessagesConfiguration = amqpMessagesConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        try {
            CoreValidRequest coreValidRequest = jsonConverter.fromJsonMessage(message.getBody(), CoreValidRequest.class);
            LOGGER.info("Core valid request received: {}", coreValidRequest);
            CoreValidResponse coreValidResponse = coreValidHandler.handleCoreValidRequest(coreValidRequest);
            LOGGER.info("Core valid response sent: {}", coreValidResponse);
            sendCoreValidResponse(coreValidResponse, replyTo, correlationId);
        } catch (AbstractCoreValidException e) {
            LOGGER.error("Core valid exception occured", e);
            sendErrorResponse(e, replyTo, correlationId);
        } catch (Exception e) {
            LOGGER.error("Unknown exception occured", e);
            AbstractCoreValidException wrappingException = new CoreValidInternalException("Unknown exception", e);
            sendErrorResponse(wrappingException, replyTo, correlationId);
        }
    }

    private void sendErrorResponse(AbstractCoreValidException e, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(e, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.coreValidResponseExchange().getName(), "", createErrorResponse(e, correlationId));
        }
    }

    private void sendCoreValidResponse(CoreValidResponse coreValidResponse, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createMessageResponse(coreValidResponse, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.coreValidResponseExchange().getName(), "", createMessageResponse(coreValidResponse, correlationId));
        }
    }

    private Message createMessageResponse(CoreValidResponse coreValidResponse, String correlationId) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(coreValidResponse))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private Message createErrorResponse(AbstractCoreValidException exception, String correlationId) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(exception))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private MessageProperties buildMessageResponseProperties(String correlationId) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(APPLICATION_ID)
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setCorrelationId(correlationId)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(amqpMessagesConfiguration.coreValidResponseExpiration())
                .setPriority(PRIORITY)
                .build();
    }

}
