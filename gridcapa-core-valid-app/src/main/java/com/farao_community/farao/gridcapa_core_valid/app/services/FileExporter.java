/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResult;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
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
import java.util.Properties;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileExporter.class);
    private static final String SAMPLE_CSV_FILE = "outputs/%s-ValidationCORE-v0.csv";
    public static final String ARTIFACTS_S = "artifacts/%s";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private final MinioAdapter minioAdapter;

    public FileExporter(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String filePath;
        try {
            CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(baos), CSVFormat.EXCEL.withDelimiter(';')
                    .withHeader("Period", "Vertice ID", "Branch ID", "Branch Status", "RAM before", "RAM after"));
            for (StudyPointResult studyPointResult : studyPointResults) {
                addStudyPointResultToOutputFile(studyPointResult, csvPrinter);
            }
            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        byte[] barray = baos.toByteArray();
        InputStream is = new ByteArrayInputStream(barray);
        filePath = String.format(SAMPLE_CSV_FILE, timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")));
        minioAdapter.uploadFile(filePath, is);
        LOGGER.info("Result file was successfully uploaded on minIO");
        return minioAdapter.generatePreSignedUrl(filePath);
    }

    public String saveShiftedCgm(Network network, StudyPoint studyPoint) {
        String fileName = network.getNameOrId() + "_" + studyPoint.getVerticeId() + ".xiidm";
        String networkPath = String.format(ARTIFACTS_S, fileName);
        MemDataSource memDataSource = new MemDataSource();
        NetworkHandler.removeAlegroVirtualGeneratorsFromNetwork(network);
        Exporters.export("XIIDM", network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", "xiidm")) {
            LOGGER.info("Uploading shifted cgm to {}", networkPath);
            minioAdapter.uploadFile(networkPath, is);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to save shifted network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }

    public String saveRaoParametersAndGetUrl(RaoParameters raoParameters) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = String.format(ARTIFACTS_S, RAO_PARAMETERS_FILE_NAME);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadFile(raoParametersDestinationPath, bais);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    public String saveCracInJsonFormat(Crac crac, OffsetDateTime timestamp) {
        MemDataSource memDataSource = new MemDataSource();
        String jsonCracFileName = String.format("crac_%s.json", timestamp.toString());
        try (OutputStream os = memDataSource.newOutputStream(jsonCracFileName, false)) {
            CracExporters.exportCrac(crac, "Json", os);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to save converted CRAC file.", e);
        }
        String cracPath = String.format(ARTIFACTS_S, jsonCracFileName);
        try (InputStream is = memDataSource.newInputStream(jsonCracFileName)) {
            minioAdapter.uploadFile(cracPath, is);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    private void addStudyPointResultToOutputFile(StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        for (LimitingBranchResult limitingBranchResult : studyPointResult.getListLimitingBranchResult()) {
            addLimitingBranchResultToOutputFile(limitingBranchResult, studyPointResult, csvPrinter);
        }
    }

    private void addLimitingBranchResultToOutputFile(LimitingBranchResult limitingBranchResult, StudyPointResult studyPointResult, CSVPrinter csvPrinter) throws IOException {
        String period = studyPointResult.getPeriod();
        String verticeId = studyPointResult.getId();
        String branchId = limitingBranchResult.getCriticalBranchId();
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
