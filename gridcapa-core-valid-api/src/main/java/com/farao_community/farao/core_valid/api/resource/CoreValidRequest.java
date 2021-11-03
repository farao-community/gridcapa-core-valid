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

import java.time.LocalDateTime;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Type("core-valid-request")
public class CoreValidRequest {
    @Id
    private final String id;
    private final LocalDateTime timestamp;
    private final CoreValidFileResource cgm;
    private final CoreValidFileResource cbcora;
    private final CoreValidFileResource glsk;
    private final CoreValidFileResource refProg;
    private final CoreValidFileResource studyPoints;

    @JsonCreator
    public CoreValidRequest(@JsonProperty("id") String id,
                            @JsonProperty("timestamp") LocalDateTime timestamp,
                            @JsonProperty("cgm") CoreValidFileResource cgm,
                            @JsonProperty("cbcora") CoreValidFileResource cbcora,
                            @JsonProperty("glsk") CoreValidFileResource glsk,
                            @JsonProperty("refProg") CoreValidFileResource refProg,
                            @JsonProperty("studyPoints") CoreValidFileResource studyPoints) {
        this.id = id;
        this.timestamp = timestamp;
        this.cgm = cgm;
        this.cbcora = cbcora;
        this.glsk = glsk;
        this.refProg = refProg;
        this.studyPoints = studyPoints;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public CoreValidFileResource getCgm() {
        return cgm;
    }

    public CoreValidFileResource getCbcora() {
        return cbcora;
    }

    public CoreValidFileResource getGlsk() {
        return glsk;
    }

    public CoreValidFileResource getRefProg() {
        return refProg;
    }

    public CoreValidFileResource getStudyPoints() {
        return studyPoints;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
