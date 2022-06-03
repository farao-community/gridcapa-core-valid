/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class RemedialActionsFileExporter implements ResultFileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialActionsFileExporter.class);
    private static final String REMEDIAL_ACTIONS_SAMPLE_CSV_FILE = "outputs/%s-RemedialActions-REX-v[v].csv";
    private final MinioAdapter minioAdapter;

    public RemedialActionsFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    @Override
    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream remedialActionsResultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter remedialActionsResultCsvPrinter = new CSVPrinter(new OutputStreamWriter(remedialActionsResultBaos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "State", "RA ID", "RA name"));
            for (StudyPointResult studyPointResult : studyPointResults) {
                addStudyPointResultToRemedialActionsOutputFile(studyPointResult, remedialActionsResultCsvPrinter);
            }
            remedialActionsResultCsvPrinter.flush();
            remedialActionsResultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = getFormattedFilename(REMEDIAL_ACTIONS_SAMPLE_CSV_FILE, timestamp, minioAdapter);
        InputStream inStream = new ByteArrayInputStream(remedialActionsResultBaos.toByteArray());
        minioAdapter.uploadOutput(filePath, inStream);
        LOGGER.info("Remedial Actions result file was successfully uploaded on minIO");
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private void addStudyPointResultToRemedialActionsOutputFile(StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        for (LimitingBranchResult limitingBranchResult : studyPointResult.getListLimitingBranchResult()) {
            addLimitingBranchResultToRemedialActionsOutputFile(limitingBranchResult, studyPointResult, csvPrinter);
        }
    }

    private void addLimitingBranchResultToRemedialActionsOutputFile(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        List<String> mainResultFields = new ArrayList<>();
        for (RemedialAction<?> remedialAction : limitingBranchResult.getRemedialActions()) {
            mainResultFields.add(studyPointResult.getPeriod());
            mainResultFields.add(studyPointResult.getId());
            mainResultFields.add(limitingBranchResult.getCriticalBranchId());
            mainResultFields.add(remedialAction.getId());
            mainResultFields.add(remedialAction.getName());
            csvPrinter.printRecord(mainResultFields);
        }
    }
}
