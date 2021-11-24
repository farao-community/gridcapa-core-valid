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
import org.springframework.amqp.core.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CoreValidMessageHandler {
    private static final String CONTENT_ENCODING = "UTF-8";
    private static final String CONTENT_TYPE = "application/vnd.api+json";

    private final CoreValidClientProperties clientProperties;
    private final JsonApiConverter jsonConverter;

    public CoreValidMessageHandler(CoreValidClientProperties clientProperties) {
        this.clientProperties = clientProperties;
        this.jsonConverter = new JsonApiConverter();
    }

    public Message buildMessage(CoreValidRequest coreValidRequest, int priority) {
        return MessageBuilder.withBody(jsonConverter.toJsonMessage(coreValidRequest))
                .andProperties(buildMessageProperties(priority))
                .build();
    }

    private MessageProperties buildMessageProperties(int priority) {
        return MessagePropertiesBuilder.newInstance()
                .setAppId(clientProperties.getAmqp().getApplicationId())
                .setContentEncoding(CONTENT_ENCODING)
                .setContentType(CONTENT_TYPE)
                .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                .setExpiration(clientProperties.getAmqp().getExpiration())
                .setPriority(priority)
                .build();
    }

    public CoreValidResponse readMessage(Message message) {
        if (message != null) {
            return jsonConverter.fromJsonMessage(message.getBody(), CoreValidResponse.class);
        } else {
            throw new CoreValidInternalException("Core valid server did not respond");
        }
    }
}
