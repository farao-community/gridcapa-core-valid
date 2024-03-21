/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_core_valid.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_core_valid.api.exception.AbstractCoreValidException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.AmqpMessagesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidListener.class);
    private static final String APPLICATION_ID = "core-valid-runner";
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";
    private static final int PRIORITY = 1;
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final JsonApiConverter jsonApiConverter;
    private final AmqpTemplate amqpTemplate;
    private final CoreValidHandler coreValidHandler;
    private final AmqpMessagesConfiguration amqpMessagesConfiguration;
    private final StreamBridge streamBridge;

    public CoreValidListener(CoreValidHandler coreValidHandler, AmqpTemplate amqpTemplate, AmqpMessagesConfiguration amqpMessagesConfiguration, StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        this.jsonApiConverter = new JsonApiConverter();
        this.coreValidHandler = coreValidHandler;
        this.amqpTemplate = amqpTemplate;
        this.amqpMessagesConfiguration = amqpMessagesConfiguration;
    }

    @Override
    public void onMessage(Message message) {
        String replyTo = message.getMessageProperties().getReplyTo();
        String correlationId = message.getMessageProperties().getCorrelationId();
        try {
            CoreValidRequest coreValidRequest = jsonApiConverter.fromJsonMessage(message.getBody(), CoreValidRequest.class);
            runCoreValidRequest(coreValidRequest, replyTo, correlationId);
        } catch (AbstractCoreValidException e) {
            LOGGER.error("Core valid exception occurred", e);
            sendRequestErrorResponse(e, replyTo, correlationId);
        } catch (RuntimeException e) {
            AbstractCoreValidException wrappingException = new CoreValidInvalidDataException("Unhandled exception: " + e.getMessage(), e);
            sendRequestErrorResponse(wrappingException, replyTo, correlationId);
        }
    }

    private void sendRequestErrorResponse(AbstractCoreValidException e, String replyTo, String correlationId) {
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(e, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.coreValidResponseExchange().getName(), "", createErrorResponse(e, correlationId));
        }
    }

    private void runCoreValidRequest(CoreValidRequest coreValidRequest, String replyTo, String correlationId) {
        try {
            LOGGER.info("Core valid request received: {}", coreValidRequest);
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidRequest.getId()), TaskStatus.RUNNING));
            CoreValidResponse coreValidResponse = coreValidHandler.handleCoreValidRequest(coreValidRequest);
            sendCoreValidResponse(coreValidResponse, replyTo, correlationId);
        } catch (AbstractCoreValidException e) {
            LOGGER.error("Core valid exception occurred", e);
            sendErrorResponse(coreValidRequest.getId(), e, replyTo, correlationId);
        } catch (RuntimeException e) {
            LOGGER.error("Unknown exception occurred", e);
            AbstractCoreValidException wrappingException = new CoreValidInternalException("Unknown exception", e);
            sendErrorResponse(coreValidRequest.getId(), wrappingException, replyTo, correlationId);
        }
    }

    private void sendErrorResponse(String requestId, AbstractCoreValidException e, String replyTo, String correlationId) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createErrorResponse(e, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.coreValidResponseExchange().getName(), "", createErrorResponse(e, correlationId));
        }
    }

    private void sendCoreValidResponse(CoreValidResponse coreValidResponse, String replyTo, String correlationId) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidResponse.getId()), TaskStatus.SUCCESS));
        if (replyTo != null) {
            amqpTemplate.send(replyTo, createMessageResponse(coreValidResponse, correlationId));
        } else {
            amqpTemplate.send(amqpMessagesConfiguration.coreValidResponseExchange().getName(), "", createMessageResponse(coreValidResponse, correlationId));
        }
        LOGGER.info("Core valid response sent: {}", coreValidResponse);
    }

    private Message createMessageResponse(CoreValidResponse coreValidResponse, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(coreValidResponse))
                .andProperties(buildMessageResponseProperties(correlationId))
                .build();
    }

    private Message createErrorResponse(AbstractCoreValidException exception, String correlationId) {
        return MessageBuilder.withBody(jsonApiConverter.toJsonMessage(exception))
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
