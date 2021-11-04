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
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

    public CoreValidHandler(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        Network cgm = loadNetwork(coreValidRequest.getCgm());
        ReferenceProgram referenceProgram = importReferenceProgram(coreValidRequest.getRefProg(), coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = computeCoreReferenceNetPositions(referenceProgram);
        List<StudyPoint> studyPoints = importStudyPoints(coreValidRequest.getStudyPoints(), coreValidRequest.getTimestamp());
        Map<String, Double> studyPointNPByCountry = computeStudyPointNP();
        shiftNetPosition(cgm, coreValidRequest.getGlsk(), coreNetPositions);
        shiftAlegroNP(cgm);
        saveShiftedCgm();
        return new CoreValidResponse(coreValidRequest.getId());
    }

    ReferenceProgram importReferenceProgram(CoreValidFileResource refProgFile, LocalDateTime timestamp) {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(timestamp, ZoneOffset.UTC);
        try (InputStream refProgStream = new URL(refProgFile.getUrl()).openStream()) {
            return RefProgImporter.importRefProg(refProgStream, offsetDateTime);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", refProgFile.getUrl()), e);
        }
    }

    private List<StudyPoint> importStudyPoints(CoreValidFileResource studyPointsFileResource, LocalDateTime timestamp) {
        //todo
        return new ArrayList<>();
    }

    private void shiftAlegroNP(Network cgm) {
        //todo
    }

    private void saveShiftedCgm() {
        //todo
    }

    private void shiftNetPosition(Network cgm, CoreValidFileResource glsk, Map<String, Double> referenceNPByCountry) {
        //todo
    }

    private Map<String, Double> computeStudyPointNP() {
        return null;
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

    private Network loadNetwork(CoreValidFileResource network) {
        try (InputStream networkStream = new URL(network.getUrl()).openStream()) {
            return Importers.loadNetwork(network.getFilename(), networkStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download network file from URL '%s'", network.getUrl()), e);
        }
    }
}
