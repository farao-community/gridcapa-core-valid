/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CoreValidRequestTest {

    @Test
    void checkCoreValidRequest() {
        CoreValidFileResource cgm = new CoreValidFileResource("network.txt", "http://path/to/cgm/file");
        CoreValidFileResource cbcora = new CoreValidFileResource("cbcora.txt", "http://path/to/cbcora/file");
        CoreValidFileResource glsk = new CoreValidFileResource("glsk.txt", "http://path/to/glsk/file");
        CoreValidFileResource refProg = new CoreValidFileResource("refprog.txt", "http://path/to/refProg/file");
        CoreValidFileResource studyPoints = new CoreValidFileResource("study-points.txt", "http://path/to/studyPoints/file");

        CoreValidRequest coreValidRequest = new CoreValidRequest("id", "timestamp", cgm, cbcora, glsk, refProg, studyPoints);
        assertNotNull(coreValidRequest);
        assertEquals("id", coreValidRequest.getId());
        assertEquals("timestamp", coreValidRequest.getTimestamp());
        assertEquals("network.txt", coreValidRequest.getCgm().getFilename());
        assertEquals("cbcora.txt", coreValidRequest.getCbcora().getFilename());
        assertEquals("refprog.txt", coreValidRequest.getRefProg().getFilename());
        assertEquals("glsk.txt", coreValidRequest.getGlsk().getFilename());
        assertEquals("http://path/to/studyPoints/file", coreValidRequest.getStudyPoints().getUrl());
    }

}