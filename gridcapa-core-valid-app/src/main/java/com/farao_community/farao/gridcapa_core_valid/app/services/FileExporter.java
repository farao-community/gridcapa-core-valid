/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.MainResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.RemedialActionsFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.ResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.RexResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Service
public class FileExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileExporter.class);
    public static final String ARTIFACTS_S = "artifacts/%s";
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private final MinioAdapter minioAdapter;
    private final MainResultFileExporter mainResultFileExporter;
    private final RemedialActionsFileExporter remedialActionsFileExporter;
    private final RexResultFileExporter rexResultFileExporter;

    public FileExporter(MinioAdapter minioAdapter, MainResultFileExporter mainResultFileExporter, RemedialActionsFileExporter remedialActionsFileExporter, RexResultFileExporter rexResultFileExporter) {
        this.minioAdapter = minioAdapter;
        this.mainResultFileExporter = mainResultFileExporter;
        this.remedialActionsFileExporter = remedialActionsFileExporter;
        this.rexResultFileExporter = rexResultFileExporter;
    }

    //region Export of Results
    public Map<ResultFileExporter.ResultType, String> exportStudyPointResult(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest) {
        Map<ResultFileExporter.ResultType, String> resultMap = new EnumMap<>(ResultFileExporter.ResultType.class);
        if (coreValidRequest.getLaunchedAutomatically()) {
            resultMap.put(ResultFileExporter.ResultType.MAIN_RESULT, mainResultFileExporter.exportStudyPointResult(studyPointResults, coreValidRequest.getTimestamp()));
        }
        resultMap.put(ResultFileExporter.ResultType.REX_RESULT, rexResultFileExporter.exportStudyPointResult(studyPointResults, coreValidRequest.getTimestamp()));
        resultMap.put(ResultFileExporter.ResultType.REMEDIAL_ACTIONS_RESULT, remedialActionsFileExporter.exportStudyPointResult(studyPointResults, coreValidRequest.getTimestamp()));
        return resultMap;
    }
    //endregion

    //region Shifted CGM uploading on minIO
    public String saveShiftedCgm(Network network, StudyPoint studyPoint) {
        String fileName = network.getNameOrId() + "_" + studyPoint.getVerticeId() + ".xiidm";
        String networkPath = String.format(ARTIFACTS_S, fileName);
        MemDataSource memDataSource = new MemDataSource();
        NetworkHandler.removeAlegroVirtualGeneratorsFromNetwork(network);
        Exporters.export("XIIDM", network, new Properties(), memDataSource);
        try (InputStream is = memDataSource.newInputStream("", "xiidm")) {
            LOGGER.info("Uploading shifted cgm to {}", networkPath);
            minioAdapter.uploadArtifact(networkPath, is);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to save shifted network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }

    //endregion

    //region RaoParameters uploading on minIO
    public String saveRaoParametersAndGetUrl(RaoParameters raoParameters) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = String.format(ARTIFACTS_S, RAO_PARAMETERS_FILE_NAME);
        ByteArrayInputStream inStream = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifact(raoParametersDestinationPath, inStream);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }
    //endregion

    //region Crac uploading on minIO
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
            minioAdapter.uploadArtifact(cracPath, is);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }
    //endregion
}
