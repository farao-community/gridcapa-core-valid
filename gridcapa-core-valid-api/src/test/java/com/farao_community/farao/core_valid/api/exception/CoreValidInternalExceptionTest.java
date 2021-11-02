/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidInternalExceptionTest {

    @Test
    void checkStandardExceptionUsage() {
        AbstractCoreValidException exception = new CoreValidInternalException("Exception message");
        assertEquals("Exception message", exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(500, exception.getStatus());
        assertEquals("500-InternalException", exception.getCode());
        assertEquals("Exception message", exception.getTitle());
        assertEquals("Exception message", exception.getDetails());

        Exception cause = new RuntimeException("Cause");
        exception = new CoreValidInternalException("Exception message", cause);
        assertEquals("Exception message", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(500, exception.getStatus());
        assertEquals("500-InternalException", exception.getCode());
        assertEquals("Exception message", exception.getTitle());
        assertEquals("Exception message; nested exception is java.lang.RuntimeException: Cause", exception.getDetails());
    }
}
