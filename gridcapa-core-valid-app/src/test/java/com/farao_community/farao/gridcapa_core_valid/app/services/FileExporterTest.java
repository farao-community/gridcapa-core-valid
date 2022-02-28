/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_api.State;
import com.farao_community.farao.data.crac_impl.CracImpl;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.ResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;

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
    void exportMainAndRexStudyPointResultTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("resultUrl");
        List<StudyPointResult> studyPointsResult = new ArrayList<>();
        StudyPointResult studyPointResult = mockStudyPointResult();
        studyPointsResult.add(studyPointResult);
        CoreValidRequest coreValidRequest = Mockito.mock(CoreValidRequest.class);
        Mockito.when(coreValidRequest.getTimestamp()).thenReturn(dateTime);
        Mockito.when(coreValidRequest.getLaunchedAutomatically()).thenReturn(true);
        String resultUrl = fileExporter.exportStudyPointResult(studyPointsResult, coreValidRequest).get(ResultFileExporter.ResultType.MAIN_RESULT);
        ArgumentCaptor<ByteArrayOutputStream> argumentCaptor = ArgumentCaptor.forClass(ByteArrayOutputStream.class);
        Mockito.verify(minioAdapter, Mockito.times(2)).uploadFile(Mockito.any(), argumentCaptor.capture());
        List<ByteArrayOutputStream> resultsBaos = argumentCaptor.getAllValues();
        assertEquals("Period;Vertice ID;Branch ID;Branch Status;RAM before;RAM after\r\n;;;;0;0\r\n", resultsBaos.get(0).toString());
        assertEquals("Period;Vertice ID;Branch ID;Branch Name;Outage Name;Branch Status;RAM before;RAM after;flow before;flow after\r\n;;;;;;0;0;0;0\r\n", resultsBaos.get(1).toString());
        assertEquals("resultUrl", resultUrl);
    }

    @Test
    void exportRexOnlyStudyPointResultTest() {
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("resultUrl");
        List<StudyPointResult> studyPointsResult = new ArrayList<>();
        StudyPointResult studyPointResult = mockStudyPointResult();
        studyPointsResult.add(studyPointResult);
        CoreValidRequest coreValidRequest = Mockito.mock(CoreValidRequest.class);
        Mockito.when(coreValidRequest.getTimestamp()).thenReturn(dateTime);
        Mockito.when(coreValidRequest.getLaunchedAutomatically()).thenReturn(false);
        Map<ResultFileExporter.ResultType, String> resultUrls = fileExporter.exportStudyPointResult(studyPointsResult, coreValidRequest);
        assertNull(resultUrls.get(ResultFileExporter.ResultType.MAIN_RESULT));
        String resultUrl = resultUrls.get(ResultFileExporter.ResultType.REX_RESULT);
        ArgumentCaptor<ByteArrayOutputStream> argumentCaptor = ArgumentCaptor.forClass(ByteArrayOutputStream.class);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), argumentCaptor.capture());
        List<ByteArrayOutputStream> resultsBaos = argumentCaptor.getAllValues();
        assertEquals("Period;Vertice ID;Branch ID;Branch Name;Outage Name;Branch Status;RAM before;RAM after;flow before;flow after\r\n;;;;;;0;0;0;0\r\n", resultsBaos.get(0).toString());
        assertEquals("resultUrl", resultUrl);
    }

    private StudyPointResult mockStudyPointResult() {
        State state = Mockito.mock(State.class);
        LimitingBranchResult limitingBranchResult = Mockito.mock(LimitingBranchResult.class);
        Mockito.when(limitingBranchResult.getBranchStatus()).thenReturn("");
        Mockito.when(limitingBranchResult.getCriticalBranchId()).thenReturn("");
        Mockito.when(limitingBranchResult.getCriticalBranchName()).thenReturn("");
        Mockito.when(limitingBranchResult.getFlowAfter()).thenReturn(0.0);
        Mockito.when(limitingBranchResult.getFlowBefore()).thenReturn(0.0);
        Mockito.when(limitingBranchResult.getRamBefore()).thenReturn(0.0);
        Mockito.when(limitingBranchResult.getRamAfter()).thenReturn(0.0);
        Mockito.when(limitingBranchResult.getRemedialActions()).thenReturn(new HashSet<>());
        Mockito.when(limitingBranchResult.getState()).thenReturn(state);
        Mockito.when(limitingBranchResult.getVerticeId()).thenReturn("");
        List<LimitingBranchResult> limitingBranchResults = Collections.singletonList(limitingBranchResult);
        StudyPointResult studyPointResult = Mockito.mock(StudyPointResult.class);
        Mockito.when(studyPointResult.getListLimitingBranchResult()).thenReturn(limitingBranchResults);
        return studyPointResult;
    }

    @Test
    void saveRaoParametersTest() {
        RaoParameters raoParameters = RaoParameters.load();
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("raoParametersUrl");
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), Mockito.any(ByteArrayOutputStream.class));
        assertEquals("raoParametersUrl", raoParametersUrl);
    }

    @Test
    void saveCracInJsonFormatTest() {
        Crac crac = new CracImpl("id");
        Mockito.when(minioAdapter.generatePreSignedUrl(Mockito.any())).thenReturn("cracUrl");
        String cracUrl = fileExporter.saveCracInJsonFormat(crac, dateTime);
        Mockito.verify(minioAdapter, Mockito.times(1)).uploadFile(Mockito.any(), Mockito.any(InputStream.class));
        assertEquals("cracUrl", cracUrl);
    }
}
