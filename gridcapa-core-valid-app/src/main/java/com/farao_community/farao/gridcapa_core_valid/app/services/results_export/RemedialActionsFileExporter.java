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
import com.powsybl.openrao.data.cracapi.RemedialAction;
import com.powsybl.openrao.data.cracio.fbconstraint.CriticalBranchCreationContext;
import com.powsybl.openrao.data.cracio.fbconstraint.FbConstraintCreationContext;
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
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Component
public class RemedialActionsFileExporter extends AbstractResultFileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialActionsFileExporter.class);
    private static final String REMEDIAL_ACTIONS_SAMPLE_CSV_FILE = "outputs/%s-RemedialActions-REX-v[v].csv";
    private static final CSVFormat REMEDIAL_ACTIONS_CSV_FORMAT = CSVFormat.EXCEL.builder()
            .setDelimiter(';')
            .setHeader("Period", "Vertice ID", "State", "RA ID", "RA name")
            .build();

    private final MinioAdapter minioAdapter;

    public RemedialActionsFileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public void exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp, FbConstraintCreationContext cracCreationContext) {
        ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        try {
            CSVPrinter resultCsvPrinter = new CSVPrinter(new OutputStreamWriter(resultBaos), REMEDIAL_ACTIONS_CSV_FORMAT);

            List<List<String>> resultCsvItems = studyPointResults.stream()
                    .map(studyPointResult -> getResultCsvItemsFromStudyPointResult(studyPointResult, cracCreationContext))
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
        String filePath = getFormattedFilename(REMEDIAL_ACTIONS_SAMPLE_CSV_FILE, timestamp, minioAdapter);
        InputStream inStream = new ByteArrayInputStream(resultBaos.toByteArray());
        minioAdapter.uploadOutputForTimestamp(filePath, inStream, "CORE_VALID", ResultType.REMEDIAL_ACTIONS_RESULT.getFileType(), timestamp);
        LOGGER.info("Remedial Actions result file was successfully uploaded on minIO");
    }

    private static List<List<String>> getResultCsvItemsFromStudyPointResult(StudyPointResult studyPointResult, FbConstraintCreationContext cracCreationContext) {
        return studyPointResult.getListLimitingBranchResult().stream()
                .map(limitingBranchResult -> getResultCsvItemsFromLimitingBranchResult(limitingBranchResult, studyPointResult, cracCreationContext))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<List<String>> getResultCsvItemsFromLimitingBranchResult(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, FbConstraintCreationContext cracCreationContext) {
        return limitingBranchResult.getRemedialActions().stream()
                .map(remedialAction -> getRemedialActionResultFields(limitingBranchResult, studyPointResult, cracCreationContext, remedialAction))
                .collect(Collectors.toList());
    }

    private static List<String> getRemedialActionResultFields(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, FbConstraintCreationContext cracCreationContext, RemedialAction<?> remedialAction) {
        List<String> remedialActionResultFields = new ArrayList<>();
        CriticalBranchCreationContext branchCnecCreationContext = cracCreationContext.getBranchCnecCreationContext(limitingBranchResult.getCriticalBranchId());
        String contingencyName = branchCnecCreationContext.getContingencyId()
                .flatMap(id -> cracCreationContext.getCrac().getContingency(id).getName())
                .orElse("BASECASE");

        remedialActionResultFields.add(studyPointResult.getPeriod());
        remedialActionResultFields.add(studyPointResult.getId());
        remedialActionResultFields.add(contingencyName);
        remedialActionResultFields.add(remedialAction.getId());
        remedialActionResultFields.add(remedialAction.getName());

        return remedialActionResultFields;
    }
}
