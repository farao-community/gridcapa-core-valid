/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
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
 * ResultFileExporter implementation generating a file which gives an
 * overview of the limitingBranch for each study pointof the timestamp.
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Component
public class MainResultFileExporter implements ResultFileExporter {

    private static final String MAIN_SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-v0.csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(MainResultFileExporter.class);
    private final MinioAdapter minioAdapter;

    public MainResultFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    @Override
    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        LOGGER.info("MAIN FILE");
        ByteArrayOutputStream mainResultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter mainResultCsvPrinter = new CSVPrinter(new OutputStreamWriter(mainResultBaos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "Branch ID", "Branch Status", "RAM before", "RAM after"));
            for (StudyPointResult studyPointResult : studyPointResults) {
                addStudyPointResultToMainOutputFile(studyPointResult, mainResultCsvPrinter);
            }
            mainResultCsvPrinter.flush();
            mainResultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = String.format(MAIN_SAMPLE_CSV_FILE, timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")));
        minioAdapter.uploadFile(filePath, mainResultBaos);
        LOGGER.info("Main result file was successfully uploaded on minIO");
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private void addStudyPointResultToMainOutputFile(StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        for (LimitingBranchResult limitingBranchResult : studyPointResult.getListLimitingBranchResult()) {
            addLimitingBranchResultToMainOutputFile(limitingBranchResult, studyPointResult, csvPrinter);
        }
    }

    private void addLimitingBranchResultToMainOutputFile(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        List<String> mainResultFields = new ArrayList<>();
        mainResultFields.add(studyPointResult.getPeriod());
        mainResultFields.add(studyPointResult.getId());
        mainResultFields.add(limitingBranchResult.getCriticalBranchId());
        mainResultFields.add(limitingBranchResult.getBranchStatus());
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamBefore())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamAfter())));
        csvPrinter.printRecord(mainResultFields);
    }
}
