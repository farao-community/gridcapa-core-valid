/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Interface to export on minIO a results file for all study points computations on a given timestamp.
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public interface ResultFileExporter {
    String exportStudyPointResult(List<StudyPointResult> studyPointResults, OffsetDateTime timestamp);

    default String getFormattedFilename(String regex, OffsetDateTime timestamp, MinioAdapter minioAdapter) {
        String filePath = String.format(regex, timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")));
        String fileVersionned = filePath.replace("[v]", "0");

        for (int versionNumber = 0; minioAdapter.fileExists(fileVersionned) && versionNumber <= 99; versionNumber++) {
            fileVersionned = filePath.replace("[v]", String.valueOf(versionNumber));
        }

        return fileVersionned;
    }

    /**
     * Several types of results file can co-exist.
     * They are enumerated here.
     */
    enum ResultType {
        MAIN_RESULT("AUTO-RESULT"),
        REMEDIAL_ACTIONS_RESULT("REMEDIAL-ACTIONS-RESULT"),
        REX_RESULT("REX-RESULT");

        private final String fileType;

        ResultType(String fileType) {
            this.fileType = fileType;
        }

        public String getFileType() {
            return fileType;
        }
    }
}
