/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CoreValidListenerTest {
    @MockBean
    public CoreValidHandler coreValidHandler;

    @Autowired
    public CoreValidListener coreValidListener;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @TestConfiguration
    static class ProcessPublicationServiceTestConfiguration {
        @Bean
        @Primary
        public AmqpTemplate amqpTemplate() {
            return Mockito.mock(AmqpTemplate.class);
        }
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(amqpTemplate, coreValidHandler);
    }

    @Test
    void checkThatCorrectMessageIsHandledCorrectly() throws URISyntaxException, IOException {
        byte[] correctMessage = Files.readAllBytes(Paths.get(getClass().getResource("/validRequest.json").toURI()));
        Message message = MessageBuilder.withBody(correctMessage).build();
        coreValidListener.onMessage(message);
        Mockito.verify(coreValidHandler, Mockito.times(1)).handleCoreValidRequest(Mockito.any(CoreValidRequest.class));
    }

    @Test
    void checkThatInvalidMessageReturnsError() throws URISyntaxException, IOException {
        byte[] invalidMessage = Files.readAllBytes(Paths.get(getClass().getResource("/invalidRequest.json").toURI()));
        Message message = MessageBuilder.withBody(invalidMessage).build();
        coreValidListener.onMessage(message);
        Mockito.verify(coreValidHandler, Mockito.times(0)).handleCoreValidRequest(Mockito.any(CoreValidRequest.class));
    }

}