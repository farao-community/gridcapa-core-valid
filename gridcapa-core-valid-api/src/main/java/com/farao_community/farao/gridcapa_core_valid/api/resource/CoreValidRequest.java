/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.api.resource;

import com.farao_community.farao.gridcapa_core_valid.api.OffsetDateTimeDeserializer;
import com.farao_community.farao.gridcapa_core_valid.api.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Type("core-valid-request")
public class CoreValidRequest {
    @Id
    private final String id;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private final OffsetDateTime timestamp;
    private final CoreValidFileResource cgm;
    private final CoreValidFileResource cbcora;
    private final CoreValidFileResource glsk;
    private final CoreValidFileResource refProg;
    private final CoreValidFileResource studyPoints;
    private final boolean launchedAutomatically;

    @JsonCreator
    public CoreValidRequest(@JsonProperty("id") String id,
                            @JsonProperty("timestamp") OffsetDateTime timestamp,
                            @JsonProperty("cgm") CoreValidFileResource cgm,
                            @JsonProperty("cbcora") CoreValidFileResource cbcora,
                            @JsonProperty("glsk") CoreValidFileResource glsk,
                            @JsonProperty("refProg") CoreValidFileResource refProg,
                            @JsonProperty("studyPoints") CoreValidFileResource studyPoints,
                            @JsonProperty("launchedAutomatically") boolean launchedAutomatically) {
        this.id = id;
        this.timestamp = timestamp;
        this.cgm = cgm;
        this.cbcora = cbcora;
        this.glsk = glsk;
        this.refProg = refProg;
        this.studyPoints = studyPoints;
        this.launchedAutomatically = launchedAutomatically;
    }

    public CoreValidRequest(String id,
                            OffsetDateTime timestamp,
                            CoreValidFileResource cgm,
                            CoreValidFileResource cbcora,
                            CoreValidFileResource glsk,
                            CoreValidFileResource refProg,
                            CoreValidFileResource studyPoints) {
        this(id, timestamp, cgm, cbcora, glsk, refProg, studyPoints, false);
    }

    public String getId() {
        return id;
    }

    public OffsetDateTime getTimestamp() {
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

    public boolean getLaunchedAutomatically() {
        return launchedAutomatically;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
