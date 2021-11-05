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
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        Map<String, Double> coreNetPositions = computeCoreReferenceNetPositions(referenceProgram);
        List<StudyPoint> studyPoints = importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());

        studyPoints.forEach(studyPoint -> computeStudyPoint(studyPoint, cgm, coreNetPositions));
        return new CoreValidResponse(coreValidRequest.getId());
    }

    private void computeStudyPoint(StudyPoint studyPoint, Network cgm, Map<String, Double> coreNetPositions) {
        shiftNetPosition(cgm, coreNetPositions, studyPoint.getPositions());
        shiftAlegroNP(cgm);
        saveShiftedCgm();
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
            return StudyPointsImporter.importStudyPoints(studyPointsStream, timestamp);
        } catch (Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download study points file from URL '%s'", studyPointsFileResource.getUrl()), e);
        }
    }

    private void shiftAlegroNP(Network cgm) {
        //todo
    }

    private void saveShiftedCgm() {
        //todo
    }

    private void shiftNetPosition(Network cgm, Map<String, Double> glsk, Map<String, Double> referenceNPByCountry) {
        //todo
    }

    Map<String, Double> computeCoreReferenceNetPositions(ReferenceProgram referenceProgram) {
        Map<String, Double> coreNetPositions = new TreeMap<>();
        referenceProgram.getReferenceExchangeDataList().forEach(referenceExchangeData -> {
            String areaIn = referenceExchangeData.getAreaIn().toString();
            String areaOut = referenceExchangeData.getAreaOut().toString();
            if (CoreAreasId.ID_MAPPING.containsKey(areaIn) && CoreAreasId.ID_MAPPING.containsKey(areaOut)) {
                coreNetPositions.put(areaIn, coreNetPositions.getOrDefault(areaIn, 0.) - referenceExchangeData.getFlow());
                coreNetPositions.put(areaOut, coreNetPositions.getOrDefault(areaOut, 0.) + referenceExchangeData.getFlow());
            }
        });
        return coreNetPositions;
    }

    private Network loadNetwork(CoreValidFileResource networkFileResource) {
        try (InputStream networkStream = urlValidationService.openUrlStream(networkFileResource.getUrl())) {
            return Importers.loadNetwork(networkFileResource.getFilename(), networkStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download networkFileResource file from URL '%s'", networkFileResource.getUrl()), e);
        }
    }
}
