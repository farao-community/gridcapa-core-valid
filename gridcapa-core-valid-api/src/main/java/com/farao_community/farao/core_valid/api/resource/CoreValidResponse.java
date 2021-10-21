/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Type("core-valid-response")
public class CoreValidResponse {
    @Id
    private final String id;

    @JsonCreator
    public CoreValidResponse(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
