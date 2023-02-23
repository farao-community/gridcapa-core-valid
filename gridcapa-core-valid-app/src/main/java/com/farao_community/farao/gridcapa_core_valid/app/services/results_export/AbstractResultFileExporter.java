/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.minio_adapter.starter.MinioAdapter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Abstract class to export on minIO a results file for all study points computations on a given timestamp.
 *
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public abstract class AbstractResultFileExporter {
    protected String getFormattedFilename(String regex, OffsetDateTime timestamp, MinioAdapter minioAdapter) {
        String formattedDate = timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH"));
        String filePath = String.format(regex, formattedDate);
        String fileWithVersion = filePath.replace("[v]", "0");

        for (int versionNumber = 0; minioAdapter.fileExists(fileWithVersion) && versionNumber <= 99; versionNumber++) {
            fileWithVersion = filePath.replace("[v]", String.valueOf(versionNumber));
        }

        return fileWithVersion;
    }
}
