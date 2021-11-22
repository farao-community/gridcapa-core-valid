/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.data.glsk.api.GlskDocument;
import com.farao_community.farao.data.glsk.api.io.GlskDocumentImporters;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.gridcapa_core_valid.app.net_position.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private final MinioAdapter minioAdapter;
    private final UrlValidationService urlValidationService;

    public CoreValidHandler(MinioAdapter minioAdapter, UrlValidationService urlValidationService) {
        this.minioAdapter = minioAdapter;
        this.urlValidationService = urlValidationService;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        Network cgm = loadNetwork(coreValidRequest.getCgm());
        ReferenceProgram referenceProgram = importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);
        GlskDocument glskDocument = importGlskFile(coreValidRequest.getGlsk());
        List<StudyPoint> studyPoints = importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
        //todo setPminPmaxToDefaultValue
        studyPoints.forEach(studyPoint -> computeStudyPoint(studyPoint, cgm, glskDocument, coreNetPositions, coreValidRequest.getTimestamp()));
        return new CoreValidResponse(coreValidRequest.getId());
    }

    GlskDocument importGlskFile(CoreValidFileResource glskFileResource) {
        try (InputStream glskStream = urlValidationService.openUrlStream(glskFileResource.getUrl())) {
            LOGGER.info("Import of Glsk file {} ", glskFileResource.getFilename());
            GlskDocument glskDocument = GlskDocumentImporters.importGlsk(glskStream);
            removeAlegroGskSeries(glskDocument);
            return glskDocument;
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", glskFileResource.getUrl()), e);
        }
    }

    private void removeAlegroGskSeries(GlskDocument glskDocument) {
        //todo
    }

    private void computeStudyPoint(StudyPoint studyPoint, Network cgm, GlskDocument glskDocument, Map<String, Double> coreNetPositions, OffsetDateTime timestamp) {
        LOGGER.info("Running computation for study point {} ", studyPoint.getId());
        NetPositionsHandler.shiftNetPositionToStudyPoint(cgm, studyPoint, glskDocument, coreNetPositions, timestamp);
        //todo resetInitialPminPmax
        saveShiftedCgm(cgm, studyPoint);
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

    private void saveShiftedCgm(Network cgm, StudyPoint studyPoint) { //todo save in minio
        String fileName = cgm.getNameOrId() + "_" + studyPoint.getId() + ".uct";
        Path path = Paths.get(fileName);
        Exporters.export("UCTE", cgm, new Properties(), path);
    }

    private Network loadNetwork(CoreValidFileResource networkFileResource) {
        try (InputStream networkStream = urlValidationService.openUrlStream(networkFileResource.getUrl())) {
            LOGGER.info("IIDM import of network : {}", networkFileResource.getFilename());
            return Importers.loadNetwork(networkFileResource.getFilename(), networkStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download networkFileResource file from URL '%s'", networkFileResource.getUrl()), e);
        }
    }
}
