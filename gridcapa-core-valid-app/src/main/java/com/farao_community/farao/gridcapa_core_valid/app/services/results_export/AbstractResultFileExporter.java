/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package com.farao_community.farao.gridcapa_core_valid.app.services.results_export;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointResult;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.farao_community.farao.gridcapa_core_valid.app.CoreValidConstants.PARIS_ZONE_ID;

/**
 * Abstract class to export on minIO a results file for all study points computations on a given timestamp.
 *
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public abstract class AbstractResultFileExporter {
    protected String getFormattedFilename(final String regex,
                                          final OffsetDateTime timestamp,
                                          final MinioAdapter minioAdapter) {
        final String formattedDate = timestamp.atZoneSameInstant(PARIS_ZONE_ID).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH"));
        final String filePath = String.format(regex, formattedDate);
        String fileWithVersion = filePath.replace("[v]", "0");

        for (int versionNumber = 0; minioAdapter.fileExists(fileWithVersion) && versionNumber <= 99; versionNumber++) {
            fileWithVersion = filePath.replace("[v]", String.valueOf(versionNumber));
        }

        return fileWithVersion;
    }

    protected abstract MinioAdapter getMinioAdapter();

    protected abstract CSVFormat getCsvFormat();

    protected abstract String getCsvFile();

    protected abstract ResultType getResultType();

    protected void exportStudyPointResult(final List<StudyPointResult> studyPointResults,
                                          final OffsetDateTime timestamp,
                                          final Function<StudyPointResult, List<List<String>>> studyPointMapper) {
        final ByteArrayOutputStream resultBaos = new ByteArrayOutputStream();
        try {
            final CSVPrinter resultCsvPrinter = new CSVPrinter(new OutputStreamWriter(resultBaos), getCsvFormat());

            final List<List<String>> resultCsvItems = studyPointResults.stream()
                    .map(studyPointMapper)
                    .flatMap(Collection::stream)
                    .distinct()
                    .toList();

            for (final List<String> resultCsvItem : resultCsvItems) {
                resultCsvPrinter.printRecord(resultCsvItem);
            }

            resultCsvPrinter.flush();
            resultCsvPrinter.close();

        } catch (final IOException e) {
            throw new CoreValidInvalidDataException("Error during export of studypoint results on Minio", e);
        }
        final String filePath = getFormattedFilename(getCsvFile(), timestamp, getMinioAdapter());
        final InputStream baInStream = new ByteArrayInputStream(resultBaos.toByteArray());
        getMinioAdapter().uploadOutputForTimestamp(filePath,
                                                   baInStream,
                                                   "CORE_VALID",
                                                   getResultType().getFileType(),
                                                   timestamp);
    }
}
