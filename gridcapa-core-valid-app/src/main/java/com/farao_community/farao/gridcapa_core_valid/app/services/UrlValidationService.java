/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.UrlWhitelistConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.StringJoiner;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final UrlWhitelistConfiguration urlWhitelistConfiguration;

    public UrlValidationService(final UrlWhitelistConfiguration urlWhitelistConfiguration) {
        this.urlWhitelistConfiguration = urlWhitelistConfiguration;
    }

    public InputStream openUrlStream(final String urlString) {
        if (urlWhitelistConfiguration.getWhitelist().stream().noneMatch(urlString::startsWith)) {
            StringJoiner sj = new StringJoiner(", ", "Whitelist: ", ".");
            urlWhitelistConfiguration.getWhitelist().forEach(sj::add);
            throw new CoreValidInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's %s", urlString, sj));
        }
        try {
            final URL url = new URI(urlString).toURL();
            return url.openStream();
        } catch (final IOException | URISyntaxException | IllegalArgumentException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download FileResource file from URL '%s'", urlString), e);
        }
    }
}
