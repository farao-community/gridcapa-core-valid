/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.MainResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.RemedialActionsFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.RexResultFileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
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

    public FileExporter(final MinioAdapter minioAdapter,
                        final MainResultFileExporter mainResultFileExporter,
                        final RemedialActionsFileExporter remedialActionsFileExporter,
                        final RexResultFileExporter rexResultFileExporter) {
        this.minioAdapter = minioAdapter;
        this.mainResultFileExporter = mainResultFileExporter;
        this.remedialActionsFileExporter = remedialActionsFileExporter;
        this.rexResultFileExporter = rexResultFileExporter;
    }

    //region Export of Results
    public void exportStudyPointResult(final List<StudyPointResult> studyPointResults,
                                       final CoreValidRequest coreValidRequest,
                                       final FbConstraintCreationContext cracCreationContext) {
        final OffsetDateTime validRequestTimestamp = coreValidRequest.getTimestamp();
        if (coreValidRequest.getLaunchedAutomatically()) {
            mainResultFileExporter.exportStudyPointResult(studyPointResults, validRequestTimestamp);
        }
        rexResultFileExporter.exportStudyPointResult(studyPointResults, validRequestTimestamp);
        remedialActionsFileExporter.exportStudyPointResult(studyPointResults, validRequestTimestamp, cracCreationContext);
    }
    //endregion

    //region Shifted CGM uploading on minIO
    public String saveShiftedCgm(final Network network,
                                 final StudyPoint studyPoint) {
        final String fileName = network.getNameOrId() + "_" + studyPoint.getVertexId() + ".xiidm";
        final String networkPath = getNetworkPathOf(fileName);
        final MemDataSource memDataSource = new MemDataSource();
        NetworkHandler.removeAlegroVirtualGeneratorsFromNetwork(network);
        network.write("XIIDM", new Properties(), memDataSource);
        try (final InputStream is = memDataSource.newInputStream("", "xiidm")) {
            LOGGER.info("Uploading shifted cgm to {}", networkPath);
            minioAdapter.uploadArtifact(networkPath, is);
        } catch (final IOException e) {
            throw new CoreValidInternalException("Error while trying to save shifted network", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }
    //endregion

    //region Shifted CGM with Pra uploading on minIO
    public String saveShiftedCgmWithPra(final Network network,
                                        final String fileName) {
        final String networkPath = getNetworkPathOf(fileName);
        final MemDataSource memDataSource = new MemDataSource();
        network.write("UCTE", new Properties(), memDataSource);
        try (final InputStream is = memDataSource.newInputStream("", "uct")) {
            LOGGER.info("Uploading shifted cgm with pra to {}", networkPath);
            minioAdapter.uploadArtifact(networkPath, is);
        } catch (final IOException e) {
            throw new CoreValidInternalException("Error while trying to save shifted network with pra", e);
        }
        return minioAdapter.generatePreSignedUrl(networkPath);
    }
    //endregion

    //region RaoParameters uploading on minIO
    public String saveRaoParametersAndGetUrl(RaoParameters raoParameters) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        final String raoParametersDestinationPath = getNetworkPathOf(RAO_PARAMETERS_FILE_NAME);
        final ByteArrayInputStream inStream = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadArtifact(raoParametersDestinationPath, inStream);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }
    //endregion

    //region Crac uploading on minIO
    public String saveCracInJsonFormat(Crac crac, OffsetDateTime timestamp) {
        final MemDataSource memDataSource = new MemDataSource();
        final String jsonCracFileName = String.format("crac_%s.json", removeIllegalCharacter(timestamp.toString()));
        try (final OutputStream os = memDataSource.newOutputStream(jsonCracFileName, false)) {
            crac.write("JSON", os);
        } catch (IOException e) {
            throw new CoreValidInternalException("Error while trying to save converted CRAC file.", e);
        }
        final String cracPath = getNetworkPathOf(jsonCracFileName);
        try (final InputStream is = memDataSource.newInputStream(jsonCracFileName)) {
            minioAdapter.uploadArtifact(cracPath, is);
        } catch (final IOException e) {
            throw new CoreValidInternalException("Error while trying to upload converted CRAC file.", e);
        }
        return minioAdapter.generatePreSignedUrl(cracPath);
    }

    private String getNetworkPathOf(final String fileName) {
        return String.format(ARTIFACTS_S, fileName);
    }

    private String removeIllegalCharacter(String url) {
        return url.replace(":", "");
    }
    //endregion
}
