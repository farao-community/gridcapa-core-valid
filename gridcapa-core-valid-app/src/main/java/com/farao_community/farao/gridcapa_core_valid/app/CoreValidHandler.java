/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.commons.ZonalData;
import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_creation.creator.api.CracCreators;
import com.farao_community.farao.data.crac_io_api.CracExporters;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.data.native_crac_api.NativeCrac;
import com.farao_community.farao.data.native_crac_io_api.NativeCracImporters;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.gridcapa_core_valid.app.net_position.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.starter.RaoRunnerClient;
import com.farao_community.farao.search_tree_rao.SearchTreeRaoParameters;
import com.powsybl.action.util.Scalable;
import com.powsybl.commons.datasource.MemDataSource;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private static final String RAO_PARAMETERS_FILE_NAME = "raoParameters.json";
    private final UrlValidationService urlValidationService;
    private final MinioAdapter minioAdapter;
    private final RaoRunnerClient raoRunnerClient;
    public static final String ARTIFACTS_S = "artifacts/%s";
    private static final String FLOW_BASED_CRAC_PROVIDER = "FlowBasedConstraintDocument";

    public CoreValidHandler(UrlValidationService urlValidationService, MinioAdapter minioAdapter, RaoRunnerClient raoRunnerClient) {
        this.urlValidationService = urlValidationService;
        this.minioAdapter = minioAdapter;
        this.raoRunnerClient = raoRunnerClient;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        StudyPointService studyPointService = new StudyPointService(minioAdapter, raoRunnerClient);
        InputStream networkStream = urlValidationService.openUrlStream(coreValidRequest.getCgm().getUrl());
        Network network = NetworkHandler.loadNetwork(coreValidRequest.getCgm().getFilename(), networkStream);
        ReferenceProgram referenceProgram = importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = importGlskFile(coreValidRequest.getGlsk());
        List<StudyPoint> studyPoints = importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());
        String raoParametersUrl = saveRaoParameters();
        Crac crac = importCrac(coreValidRequest.getCbcora(), coreValidRequest.getTimestamp(), network);
        String jsonCracUrl = saveCracInJsonFormat(crac, coreValidRequest.getTimestamp());
        studyPoints.forEach(studyPoint -> studyPointService.computeStudyPoint(studyPoint, network, scalableZonalData, coreNetPositions, raoParametersUrl, jsonCracUrl));
        return new CoreValidResponse(coreValidRequest.getId());
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

    private String saveRaoParameters() {
        RaoParameters raoParameters = RaoParameters.load();
        SearchTreeRaoParameters searchTreeRaoParameters = getSearchTreeRaoParameters();
        raoParameters.addExtension(SearchTreeRaoParameters.class, searchTreeRaoParameters);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, baos);
        String raoParametersDestinationPath = RAO_PARAMETERS_FILE_NAME;
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        minioAdapter.uploadFile(raoParametersDestinationPath, bais);
        return minioAdapter.generatePreSignedUrl(raoParametersDestinationPath);
    }

    private SearchTreeRaoParameters getSearchTreeRaoParameters() { //todo modify params in itools config file
        SearchTreeRaoParameters searchTreeRaoParameters = new SearchTreeRaoParameters();
        HashMap<String, Integer> mapParameters = new HashMap<>();
        mapParameters.put("FR", 1);
        searchTreeRaoParameters.setMaxCurativePstPerTso(mapParameters);
        searchTreeRaoParameters.setMaxCurativeRaPerTso(mapParameters);
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(mapParameters);
        return searchTreeRaoParameters;
    }

    GlskDocument importGlskFile(CoreValidFileResource glskFileResource) {
        try (InputStream glskStream = urlValidationService.openUrlStream(glskFileResource.getUrl())) {
            LOGGER.info("Import of Glsk file {} ", glskFileResource.getFilename());
            return GlskDocumentImporters.importGlsk(glskStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", glskFileResource.getUrl()), e);
        }
    }

    ReferenceProgram importReferenceProgram(CoreValidFileResource refProgFile, OffsetDateTime timestamp) {
        try (InputStream refProgStream = urlValidationService.openUrlStream(refProgFile.getUrl())) {
            return RefProgImporter.importRefProg(refProgStream, timestamp);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", refProgFile.getUrl()), e);
        }
    }

    private List<StudyPoint> importStudyPoints(CoreValidFileResource studyPointsFileResource, OffsetDateTime timestamp) {
        try (InputStream studyPointsStream = urlValidationService.openUrlStream(studyPointsFileResource.getUrl())) {
            LOGGER.info("Import of study points from {} file for timestamp {} ", studyPointsFileResource.getFilename(), timestamp);
            return StudyPointsImporter.importStudyPoints(studyPointsStream, timestamp);
        } catch (Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download study points file from URL '%s'", studyPointsFileResource.getUrl()), e);
        }
    }

    private Crac importCrac(CoreValidFileResource cbcoraFile, OffsetDateTime targetProcessDateTime, Network network) {
        try (InputStream cracInputStream = urlValidationService.openUrlStream(cbcoraFile.getUrl())) {
            NativeCrac nativeCrac = NativeCracImporters.findImporter(FLOW_BASED_CRAC_PROVIDER).importNativeCrac(cracInputStream);
            return CracCreators.createCrac(nativeCrac, network, targetProcessDateTime).getCrac();
        } catch (Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download cbcora file from URL '%s'", cbcoraFile.getUrl()), e);
        }
    }

}
