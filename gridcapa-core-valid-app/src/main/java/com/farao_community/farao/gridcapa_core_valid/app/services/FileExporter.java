/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileExporter.class);
    private static final String SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-v0.csv";
    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String filePath;
        try {
            final CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(baos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "Branch ID", "Branch Status", "RAM before", "RAM after"));
            studyPointResults.forEach(studyPointResult -> addStudyPointResultToOutputFile(studyPointResult, csvPrinter));
            csvPrinter.flush();
            csvPrinter.close();
            byte[] barray = baos.toByteArray();
            InputStream is = new ByteArrayInputStream(barray);
            filePath = String.format(SAMPLE_CSV_FILE, timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")));
            minioAdapter.uploadFile(filePath, is);
            LOGGER.info("Result file was successfully uploaded on minIO");
        } catch (IOException e) {
            e.printStackTrace();
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    private void addStudyPointResultToOutputFile(StudyPointResult studyPointResult, CSVPrinter csvPrinter) {
        studyPointResult.getListLimitingBranchResult().forEach(
            limitingBranchResult -> {
                try {
                    addLimitingBranchResultToOutputFile(limitingBranchResult, studyPointResult, csvPrinter);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        );
    }

    private void addLimitingBranchResultToOutputFile(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        String period = studyPointResult.getPeriod();
        String verticeId = studyPointResult.getId();
        String branchId = limitingBranchResult.getCnecId();
        String branchStatus;
        switch (limitingBranchResult.getState().getInstant()) {
            case PREVENTIVE:
                branchStatus = "P";
                break;
            case OUTAGE:
                branchStatus = "O";
                break;
            case CURATIVE:
                branchStatus = "C";
                break;
            default:
                throw new CoreValidInvalidDataException(String.format("Invalid value in CBCORA file, for cnec {}", branchId));
        }
        String ramBefore = String.valueOf(Math.round(limitingBranchResult.getRamBefore()));
        String ramAfter = String.valueOf(Math.round(limitingBranchResult.getRamAfter()));
        csvPrinter.printRecord(period, verticeId, branchId, branchStatus, ramBefore, ramAfter);
    }

}
