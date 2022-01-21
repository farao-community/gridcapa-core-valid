/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.core_valid.api.JsonApiConverter;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.mockito.Mockito;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidClientTest {
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();

    @Test
    void checkThatClientHandleMessageCorrectly() throws IOException {
        AmqpTemplate amqpTemplate = Mockito.mock(AmqpTemplate.class);
        CoreValidClient client = new CoreValidClient(amqpTemplate, buildProperties());
        CoreValidRequest request = jsonApiConverter.fromJsonMessage(getClass().getResourceAsStream("/coreValidRequest.json").readAllBytes(), CoreValidRequest.class);
        Message responseMessage = Mockito.mock(Message.class);

        Mockito.when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/coreValidResponse.json").readAllBytes());
        Mockito.when(amqpTemplate.sendAndReceive(Mockito.same("my-queue"), Mockito.any())).thenReturn(responseMessage);
        CoreValidResponse response = client.run(request);

        assertEquals("test", response.getId());
    }

    private CoreValidClientProperties buildProperties() {
        CoreValidClientProperties properties = new CoreValidClientProperties();
        CoreValidClientProperties.AmqpConfiguration amqpConfiguration = new CoreValidClientProperties.AmqpConfiguration();
        amqpConfiguration.setQueueName("my-queue");
        amqpConfiguration.setExpiration("60000");
        amqpConfiguration.setApplicationId("application-id");
        properties.setAmqp(amqpConfiguration);
        return properties;
    }
}
