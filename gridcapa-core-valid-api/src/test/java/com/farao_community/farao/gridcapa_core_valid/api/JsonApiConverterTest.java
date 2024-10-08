/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.api;

import com.farao_community.farao.gridcapa_core_valid.api.exception.AbstractCoreValidException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class JsonApiConverterTest {
    @Test
    void checkCoreValidInputsJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        String inputMessage = Files.readString(Paths.get(getClass().getResource("/validRequest.json").toURI()));
        CoreValidRequest coreValidRequest = jsonApiConverter.fromJsonMessage(inputMessage.getBytes(), CoreValidRequest.class);
        assertEquals("id", coreValidRequest.getId());
        assertEquals("cgm.txt", coreValidRequest.getCgm().getFilename());
        assertEquals("https://cgm/file/url", coreValidRequest.getCgm().getUrl());
        assertEquals("cbcora.txt", coreValidRequest.getCbcora().getFilename());
        assertEquals("https://cbcora/file/url", coreValidRequest.getCbcora().getUrl());
        assertEquals("glsk.txt", coreValidRequest.getGlsk().getFilename());
        assertEquals("https://glsk/file/url", coreValidRequest.getGlsk().getUrl());
    }

    @Test
    void checkInternalExceptionJsonConversion() throws URISyntaxException, IOException {
        JsonApiConverter jsonApiConverter = new JsonApiConverter();
        AbstractCoreValidException exception = new CoreValidInternalException("Something really bad happened");
        String expectedMessage = Files.readString(Paths.get(getClass().getResource("/coreValidInternalError.json").toURI()));
        assertEquals(expectedMessage, new String(jsonApiConverter.toJsonMessage(exception)));
    }

}
