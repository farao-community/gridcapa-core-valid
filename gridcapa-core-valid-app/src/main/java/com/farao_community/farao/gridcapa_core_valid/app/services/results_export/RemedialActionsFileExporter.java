/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.data.crac_api.RemedialAction;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.services.MinioAdapter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Component
public class RemedialActionsFileExporter implements ResultFileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialActionsFileExporter.class);
    private static final String REMEDIAL_ACTIONS_SAMPLE_CSV_FILE = "outputs/%s-RemedialActions-REX-v0.csv";

    private final MinioAdapter minioAdapter;

    public RemedialActionsFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    @Override
    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream rexResultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter rexResultCsvPrinter = new CSVPrinter(new OutputStreamWriter(rexResultBaos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "State", "RA applied"));
            for (StudyPointResult studyPointResult : studyPointResults) {
                LOGGER.info("ADD");
                addStudyPointResultToRemedialActionsOutputFile(studyPointResult, rexResultCsvPrinter);
            }
            rexResultCsvPrinter.flush();
            rexResultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = String.format(REMEDIAL_ACTIONS_SAMPLE_CSV_FILE, timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")));
        minioAdapter.uploadFile(filePath, rexResultBaos);
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
            mainResultFields.add(remedialAction.getName());
            csvPrinter.printRecord(mainResultFields);
        }
    }
}
