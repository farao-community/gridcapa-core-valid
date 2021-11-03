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
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Double> referenceNPByCountry = computeRefProgNP(coreValidRequest.getRefProg().getUrl());
        importStudyPoints(coreValidRequest.getStudyPoints().getUrl());
        Map<String, Double> studyPointNPByCountry = computeStudyPointNP();
        shiftNetPosition(cgm, coreValidRequest.getGlsk(), referenceNPByCountry);
        shiftAlegroNP(cgm);
        saveShiftedCgm();
        return new CoreValidResponse(coreValidRequest.getId());
    }

    private void importStudyPoints(String url) {
        //todo
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

    private Map<String, Double> computeRefProgNP(String url) {
        //todo compute for each country NPref in Core
        return new HashMap<>();
    }

    private Network loadNetwork(CoreValidFileResource network) {
        try (InputStream networkStream = new URL(network.getUrl()).openStream()) {
            return Importers.loadNetwork(network.getFilename(), networkStream);
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download network file from URL '%s'", network.getUrl()), e);
        }
    }
}
