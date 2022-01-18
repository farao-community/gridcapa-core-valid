/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.starter;

import com.farao_community.farao.core_valid.api.JsonApiConverter;
import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import org.springframework.amqp.core.Message;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class CoreValidResponseConversionHelper {

    private CoreValidResponseConversionHelper() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static CoreValidResponse convertCoreValidResponse(Message message, JsonApiConverter jsonConverter) {
        try {
            return jsonConverter.fromJsonMessage(message.getBody(), CoreValidResponse.class);
        } catch (ResourceParseException resourceParseException) {
            // exception details from core-valid-runner app is wrapped into a ResourceParseException on json Api Error format.
            String originCause = resourceParseException.getErrors().getErrors().get(0).getDetail();
            throw new CoreValidInvalidDataException(originCause);
        } catch (Exception unknownException) {
            throw new CoreValidInvalidDataException("Unsupported exception thrown by core-valid-runner app", unknownException);
        }
    }
}
