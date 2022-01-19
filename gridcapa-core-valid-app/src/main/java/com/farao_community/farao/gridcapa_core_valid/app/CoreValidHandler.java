/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.gridcapa_core_valid.app.limiting_branch.LimitingBranchResultService;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.MinioAdapter;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.powsybl.action.util.Scalable;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private final MinioAdapter minioAdapter;
    private final RaoRunnerClient raoRunnerClient;
    private final FileImporter fileImporter;
    private final LimitingBranchResultService limitingBranchResult;
    public static final String ARTIFACTS_S = "artifacts/%s";

    public CoreValidHandler(MinioAdapter minioAdapter, RaoRunnerClient raoRunnerClient, FileImporter fileImporter, LimitingBranchResultService limitingBranchResult) {
        this.minioAdapter = minioAdapter;
        this.raoRunnerClient = raoRunnerClient;
        this.fileImporter = fileImporter;
        this.limitingBranchResult = limitingBranchResult;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        try {
            StudyPointService studyPointService = new StudyPointService(minioAdapter, raoRunnerClient, limitingBranchResult);
            Network network = fileImporter.importNetwork(coreValidRequest.getCgm().getFilename(), coreValidRequest.getCgm().getUrl());
            ReferenceProgram referenceProgram = fileImporter.importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
            Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
            GlskDocument glskDocument = fileImporter.importGlskFile(coreValidRequest.getGlsk());
            List<StudyPoint> studyPoints = fileImporter.importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
            ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
            Crac crac = fileImporter.importCrac(coreValidRequest.getCbcora().getUrl(), coreValidRequest.getTimestamp(), network);
            String jsonCracUrl = saveCracInJsonFormat(crac, coreValidRequest.getTimestamp());
            studyPoints.forEach(studyPoint -> studyPointService.computeStudyPoint(studyPoint, network, scalableZonalData, coreNetPositions, jsonCracUrl));
            return new CoreValidResponse(coreValidRequest.getId());
        } catch (Exception e) {
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        }
    }

    private String saveCracInJsonFormat(Crac crac, OffsetDateTime timestamp) {
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
}
