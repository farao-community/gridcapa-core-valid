/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.core_valid.api.JsonApiConverter;
import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Error;
import com.github.jasminb.jsonapi.models.errors.Errors;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidResponseConversionHelperTest {

    @Test
    void checkThatExceptionIsConvertedCorrectly() throws IOException {
        Message responseMessage = mock(Message.class);
        when(responseMessage.getBody()).thenReturn(getClass().getResourceAsStream("/coreValidResponse.json").readAllBytes());
        JsonApiConverter jsonApiConverter = mock(JsonApiConverter.class);
        Errors errors = new Errors();
        Error error = new Error();
        error.setDetail("exception conversion test");
        errors.setErrors(Collections.singletonList(error));
        when(jsonApiConverter.fromJsonMessage(responseMessage.getBody(), CoreValidResponse.class)).thenThrow(new ResourceParseException(errors));
        Exception exception = assertThrows(CoreValidInvalidDataException.class, () -> CoreValidResponseConversionHelper.convertCoreValidResponse(responseMessage, jsonApiConverter));
        String expectedMessage = "exception conversion test";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

}
