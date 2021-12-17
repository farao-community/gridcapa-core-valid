/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.CoreValidServerProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringJoiner;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final CoreValidServerProperties.SecurityProperties securityProperties;

    public UrlValidationService(CoreValidServerProperties serverProperties) {
        this.securityProperties = serverProperties.getSecurity();
    }

    public InputStream openUrlStream(String urlString) {
        if (securityProperties.getWhitelist().stream().noneMatch(urlString::startsWith)) {
            StringJoiner sj = new StringJoiner(", ", "Whitelist: ", ".");
            securityProperties.getWhitelist().forEach(sj::add);
            throw new CoreValidInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's", urlString));
        }
        try {
            URL url = new URL(urlString);
            return url.openStream();
        } catch (IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download networkFileResource file from URL '%s'", urlString), e);
        }
    }
}
