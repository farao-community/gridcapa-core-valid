/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public MainResultFileExporter(final MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public void exportStudyPointResult(final List<StudyPointResult> studyPointResults,
                                       final OffsetDateTime timestamp) {
        exportStudyPointResult(studyPointResults,
                               timestamp,
                               MainResultFileExporter::getResultCsvItemsFromStudyPointResult);
        LOGGER.info("Main result file was successfully uploaded on minIO");
    }

    private static List<List<String>> getResultCsvItemsFromStudyPointResult(final StudyPointResult studyPointResult) {
        return studyPointResult.getLimitingBranchResults().stream()
                .map(limitingBranchResult -> getMainResultFields(limitingBranchResult, studyPointResult))
                .toList();
    }

    private static List<String> getMainResultFields(final LimitingBranchResult limitingBranchResult,
                                                    final StudyPointResult studyPointResult) {
        final List<String> mainResultFields = new ArrayList<>();

        mainResultFields.add(studyPointResult.getPeriod());
        mainResultFields.add(studyPointResult.getId());
        mainResultFields.add(limitingBranchResult.criticalBranchId());
        mainResultFields.add(limitingBranchResult.getBranchStatus());
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.ramBefore())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.ramAfter())));

        return mainResultFields;
    }

    @Override
    protected MinioAdapter getMinioAdapter() {
        return minioAdapter;
    }

    @Override
    protected CSVFormat getCsvFormat() {
        return MAIN_CSV_FORMAT;
    }

    @Override
    protected String getCsvFile() {
        return MAIN_SAMPLE_CSV_FILE;
    }

    @Override
    protected ResultType getResultType() {
        return ResultType.MAIN_RESULT;
    }
}
