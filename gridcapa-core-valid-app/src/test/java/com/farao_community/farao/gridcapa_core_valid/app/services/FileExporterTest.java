/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class FileExporterTest {

    @Autowired
    private FileExporter fileExporter;

    @MockBean
    private MinioAdapter minioAdapter;

    private final OffsetDateTime dateTime = OffsetDateTime.parse("2021-07-22T22:30Z");

    @Test
    void exportStudyPointResultTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("resultUrl");
        List<StudyPointResult> studyPointsResult = new ArrayList<>();
        String resultUrl = fileExporter.exportStudyPointResult(studyPointsResult, dateTime);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), Mockito.any());
        assertEquals("resultUrl", resultUrl);
    }

    @Test
    void saveRaoParametersTest() {
        RaoParameters raoParameters = RaoParameters.load();
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("raoParametersUrl");
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), Mockito.any());
        assertEquals("raoParametersUrl", raoParametersUrl);
    }

    @Test
    void saveCracInJsonFormatTest() {
        Crac crac = new CracImpl("id");
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("cracUrl");
        String cracUrl = fileExporter.saveCracInJsonFormat(crac, dateTime);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), Mockito.any());
        assertEquals("cracUrl", cracUrl);
    }
}
