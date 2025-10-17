/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.farao_community.farao.gridcapa_core_valid.app.CoreValidConstants.PARIS_ZONE_ID;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
public final class StudyPointsImporter {

    private StudyPointsImporter() {
        throw new IllegalStateException("Utility class");
    }

    public static List<StudyPoint> importStudyPoints(final InputStream inputStream) {
        return importStudyPoints(new InputStreamReader(inputStream));
    }

    public static List<StudyPoint> importStudyPoints(final InputStream studyPointsStream, final OffsetDateTime timestamp) {
        final List<StudyPoint> allStudyPoints = importStudyPoints(studyPointsStream);
        final int period = timestamp.atZoneSameInstant(PARIS_ZONE_ID).getHour();
        return allStudyPoints.stream()
                .filter(studyPoint -> studyPoint.getPeriod() == period)
                .toList();
    }

    private static List<StudyPoint> importStudyPoints(final Reader reader) {
        try {
            final List<StudyPoint> studyPoints = new ArrayList<>();
            final CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser(buildParser())
                    .build();
            final List<String[]> lines = csvReader.readAll();
            final String[] headers = lines.getFirst();
            for (int i = 1; i < lines.size(); i++) {
                studyPoints.add(importStudyPoint(headers, lines.get(i)));
            }
            return studyPoints;
        } catch (final Exception e) {
            throw new CoreValidInvalidDataException("Exception occurred during parsing study point file", e);
        }
    }

    private static StudyPoint importStudyPoint(final String[] headers, final String[] data) throws ParseException {
        final int period = Integer.parseInt(data[0]);
        final String id = data[1];
        final Map<String, Double> positions = new HashMap<>();
        for (int i = 2; i < data.length; i++) {
            positions.put(headers[i], NumberFormat.getInstance(Locale.FRANCE).parse(data[i]).doubleValue());
        }
        return new StudyPoint(period, id, positions);
    }

    private static CSVParser buildParser() {
        return new CSVParserBuilder()
                .withSeparator(';')
                .build();
    }
}
