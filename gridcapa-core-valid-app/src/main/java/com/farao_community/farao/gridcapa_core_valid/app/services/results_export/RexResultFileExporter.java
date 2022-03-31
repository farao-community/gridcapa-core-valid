/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.data.crac_api.Contingency;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ResultFileExporter implementation generating a zip archive with the following files:
 * <ul>
 *     <li>An overview of all limitingBranch for each study-point of the timestamp</li>
 *     <li>An overview of Remedial Actions (RAs) selected by the RAO runner for each study point (TODO)</li>
 *     <li>all shifted network files, before and after application of preventive RAs, for each study point(TODO)</li>
 * </ul>
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Component
public class RexResultFileExporter implements ResultFileExporter {

    private static final String REX_SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-REX-v[v].csv";
    private static final Logger LOGGER = LoggerFactory.getLogger(RexResultFileExporter.class);
    private final MinioAdapter minioAdapter;

    public RexResultFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    @Override
    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream rexResultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter rexResultCsvPrinter = new CSVPrinter(new OutputStreamWriter(rexResultBaos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "Branch ID", "Branch Name", "Outage Name", "Branch Status", "RAM before", "RAM after", "flow before", "flow after"));
            for (StudyPointResult studyPointResult : studyPointResults) {
                addStudyPointResultToRexOutputFile(studyPointResult, rexResultCsvPrinter);
            }
            rexResultCsvPrinter.flush();
            rexResultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = getFormattedFilename(REX_SAMPLE_CSV_FILE, timestamp, minioAdapter);
        minioAdapter.uploadFile(filePath, rexResultBaos);
        LOGGER.info("Rex result file was successfully uploaded on minIO");
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private void addStudyPointResultToRexOutputFile(StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        for (LimitingBranchResult limitingBranchResult : studyPointResult.getListLimitingBranchResult()) {
            addLimitingBranchResultToRexOutputFile(limitingBranchResult, studyPointResult, csvPrinter);
        }
    }

    private void addLimitingBranchResultToRexOutputFile(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        List<String> mainResultFields = new ArrayList<>();
        mainResultFields.add(studyPointResult.getPeriod());
        mainResultFields.add(studyPointResult.getId());
        mainResultFields.add(limitingBranchResult.getCriticalBranchId());
        mainResultFields.add(limitingBranchResult.getCriticalBranchName());
        Optional<Contingency> optionalContingency = limitingBranchResult.getState().getContingency();
        if (optionalContingency.isPresent()) {
            mainResultFields.add(optionalContingency.get().getName());
        } else {
            mainResultFields.add("");
        }
        mainResultFields.add(limitingBranchResult.getBranchStatus());
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamBefore())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getRamAfter())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getFlowBefore())));
        mainResultFields.add(String.valueOf(Math.round(limitingBranchResult.getFlowAfter())));
        csvPrinter.printRecord(mainResultFields);
    }
}
