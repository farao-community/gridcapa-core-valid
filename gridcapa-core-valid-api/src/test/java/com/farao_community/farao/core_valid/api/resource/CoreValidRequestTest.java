/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidRequestTest {

    private CoreValidFileResource cgm;
    private CoreValidFileResource cbcora;
    private CoreValidFileResource glsk;
    private CoreValidFileResource refProg;
    private CoreValidFileResource studyPoints;
    private OffsetDateTime dateTime;

    @BeforeEach
    void setUp() {
        cgm = new CoreValidFileResource("network.txt", "http://path/to/cgm/file");
        cbcora = new CoreValidFileResource("cbcora.txt", "http://path/to/cbcora/file");
        glsk = new CoreValidFileResource("glsk.txt", "http://path/to/glsk/file");
        refProg = new CoreValidFileResource("refprog.txt", "http://path/to/refProg/file");
        studyPoints = new CoreValidFileResource("study-points.txt", "http://path/to/studyPoints/file");
        dateTime = OffsetDateTime.parse("2021-10-03T00:30Z");
    }

    @Test
    void checkManualCoreValidRequest() {
        CoreValidRequest coreValidRequest = new CoreValidRequest.CoreValidRequestBuilder("id", dateTime, cgm, cbcora, glsk, refProg, studyPoints)
                .build();
        assertNotNull(coreValidRequest);
        assertEquals("id", coreValidRequest.getId());
        assertEquals("2021-10-03T00:30Z", coreValidRequest.getTimestamp().toString());
        assertEquals("network.txt", coreValidRequest.getCgm().getFilename());
        assertEquals("cbcora.txt", coreValidRequest.getCbcora().getFilename());
        assertEquals("refprog.txt", coreValidRequest.getRefProg().getFilename());
        assertEquals("glsk.txt", coreValidRequest.getGlsk().getFilename());
        assertEquals("http://path/to/studyPoints/file", coreValidRequest.getStudyPoints().getUrl());
        assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void checkAutoCoreValidRequest() {
        CoreValidRequest coreValidRequest = new CoreValidRequest.CoreValidRequestBuilder("id", dateTime, cgm, cbcora, glsk, refProg, studyPoints)
                .isLaunchedAutomatically()
                .build();
        assertTrue(coreValidRequest.getLaunchedAutomatically());
    }

}
