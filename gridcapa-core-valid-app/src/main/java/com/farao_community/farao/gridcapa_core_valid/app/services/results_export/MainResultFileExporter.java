/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ResultFileExporter implementation generating a file which gives an
 * overview of the limitingBranch for each study pointof the timestamp.
 *
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Component
public class MainResultFileExporter extends AbstractResultFileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainResultFileExporter.class);
    private static final String MAIN_SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-v[v].csv";
    private static final CSVFormat MAIN_CSV_FORMAT = CSVFormat.EXCEL.builder()
        .setDelimiter(';')
        .setHeader("Period", "Vertice ID", "Branch ID", "Branch Status", "RAM before", "RAM after")
        .build();

    private final MinioAdapter minioAdapter;

    public MainResultFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter resultCsvPrinter = new CSVPrinter(new OutputStreamWriter(resultBaos), MAIN_CSV_FORMAT);

            List<List<String>> resultCsvItems = studyPointResults.stream()
                .map(this::getResultCsvItemsFromStudyPointResult)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());

            for (List<String> resultCsvItem : resultCsvItems) {
                resultCsvPrinter.printRecord(resultCsvItem);
            }

            resultCsvPrinter.flush();
            resultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = getFormattedFilename(MAIN_SAMPLE_CSV_FILE, timestamp, minioAdapter);
        InputStream inStream = new ByteArrayInputStream(resultBaos.toByteArray());
        minioAdapter.uploadOutputForTimestamp(filePath, inStream, "CORE_VALID", ResultType.MAIN_RESULT.getFileType(), timestamp);
        LOGGER.info("Main result file was successfully uploaded on minIO");
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private List<List<String>> getResultCsvItemsFromStudyPointResult(StudyPointResult studyPointResult) {
        return studyPointResult.getListLimitingBranchResult().stream()
            .map(limitingBranchResult -> getMainResultFields(limitingBranchResult, studyPointResult))
            .collect(Collectors.toList());
    }

    private static List<String> getMainResultFields(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult) {
        List<String> mainResultFields = new ArrayList<>();

        mainResultFields.add(studyPointResult.getPeriod());
        mainResultFields.add(studyPointResult.getId());
        mainResultFields.add(limitingBranchResult.getCriticalBranchId());
        mainResultFields.add(limitingBranchResult.getBranchStatus());
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamBefore())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamAfter())));

        return mainResultFields;
    }
}
