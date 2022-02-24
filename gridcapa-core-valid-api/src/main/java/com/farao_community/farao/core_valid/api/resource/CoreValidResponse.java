/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_valid.api.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.Instant;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Type("core-valid-response")
public class CoreValidResponse {

    @Id
    private final String id;
    private final String mainResultFileUrl;
    private final String rexResultFileUrl; // TODO: remove this once choice of result file type can be done in the configuration
    private final Instant computationStartInstant;
    private final Instant computationEndInstant;

    @JsonCreator
    public CoreValidResponse(@JsonProperty("id") String id, @JsonProperty("mainResultFileUrl") String mainResultFileUrl, @JsonProperty("rexResultFileUrl") String rexResultFileUrl, @JsonProperty("computationStartInstant") Instant computationStartInstant, @JsonProperty("computationEndInstant") Instant computationEndInstant) {
        this.id = id;
        this.mainResultFileUrl = mainResultFileUrl;
        this.rexResultFileUrl = rexResultFileUrl;
        this.computationStartInstant = computationStartInstant;
        this.computationEndInstant = computationEndInstant;
    }

    public String getId() {
        return id;
    }

    public String getMainResultFileUrl() {
        return mainResultFileUrl;
    }

    public String getRexResultFileUrl() {
        return rexResultFileUrl;
    }

    public Instant getComputationStartInstant() {
        return computationStartInstant;
    }

    public Instant getComputationEndInstant() {
        return computationEndInstant;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
