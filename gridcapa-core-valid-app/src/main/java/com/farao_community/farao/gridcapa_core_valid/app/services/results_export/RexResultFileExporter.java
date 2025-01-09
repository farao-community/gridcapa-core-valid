/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.powsybl.contingency.Contingency;
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
import java.util.Optional;

/**
 * ResultFileExporter implementation generating a zip archive with the following files:
 * <ul>
 *     <li>An overview of all limitingBranch for each study-point of the timestamp</li>
 * </ul>
 *
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class RexResultFileExporter extends AbstractResultFileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RexResultFileExporter.class);
    private static final String REX_SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-REX-v[v].csv";
    private static final CSVFormat REX_CSV_FORMAT = CSVFormat.EXCEL.builder()
            .setDelimiter(';')
            .setHeader("Period", "Vertice ID", "Branch ID", "Branch Name", "Outage Name", "Branch Status", "RAM before", "RAM after", "flow before", "flow after")
            .build();

    private final MinioAdapter minioAdapter;

    public RexResultFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public void exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter resultCsvPrinter = new CSVPrinter(new OutputStreamWriter(resultBaos), REX_CSV_FORMAT);

            List<List<String>> resultCsvItems = studyPointResults.stream()
                    .map(RexResultFileExporter::getResultCsvItemsFromStudyPointResult)
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();

            for (List<String> resultCsvItem : resultCsvItems) {
                resultCsvPrinter.printRecord(resultCsvItem);
            }

            resultCsvPrinter.flush();
            resultCsvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        String filePath = getFormattedFilename(REX_SAMPLE_CSV_FILE, timestamp, minioAdapter);
        InputStream inStream = new ByteArrayInputStream(resultBaos.toByteArray());
        minioAdapter.uploadOutputForTimestamp(filePath, inStream, "CORE_VALID", ResultType.REX_RESULT.getFileType(), timestamp);
        LOGGER.info("Rex result file was successfully uploaded on minIO");
    }

    private static List<List<String>> getResultCsvItemsFromStudyPointResult(StudyPointResult studyPointResult) {
        return studyPointResult.getListLimitingBranchResult().stream()
                .map(limitingBranchResult -> getRexResultFields(limitingBranchResult, studyPointResult))
                .toList();
    }

    private static List<String> getRexResultFields(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult) {
        List<String> rexResultFields = new ArrayList<>();

        rexResultFields.add(studyPointResult.getPeriod());
        rexResultFields.add(studyPointResult.getId());
        rexResultFields.add(limitingBranchResult.criticalBranchId());
        rexResultFields.add(limitingBranchResult.criticalBranchName());
        Optional<Contingency> optionalContingency = limitingBranchResult.state().getContingency();
        if (optionalContingency.isPresent()) {
            rexResultFields.add(optionalContingency.get().getName().orElse(""));
        } else {
            rexResultFields.add("");
        }
        rexResultFields.add(limitingBranchResult.getBranchStatus());
        rexResultFields.add(String.valueOf(Math.round(limitingBranchResult.ramBefore())));
        rexResultFields.add(String.valueOf(Math.round(limitingBranchResult.ramAfter())));
        rexResultFields.add(String.valueOf(Math.round(limitingBranchResult.flowBefore())));
        rexResultFields.add(String.valueOf(Math.round(limitingBranchResult.flowAfter())));

        return rexResultFields;
    }
}
