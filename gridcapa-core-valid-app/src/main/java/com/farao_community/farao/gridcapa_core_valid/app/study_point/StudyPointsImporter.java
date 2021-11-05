/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.study_point;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public final class StudyPointsImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyPointsImporter.class);

    public static List<StudyPoint> importStudyPoints(InputStream inputStream) {
        return importStudyPoints(new InputStreamReader(inputStream));
    }

    public static List<StudyPoint> importStudyPoints(InputStream studyPointsStream, OffsetDateTime timestamp) {
        List<StudyPoint> allStudyPoints = StudyPointsImporter.importStudyPoints(studyPointsStream);
        int period = timestamp.atZoneSameInstant(ZoneId.of("Europe/Paris")).getHour();
        return allStudyPoints.stream().filter(studyPoint -> studyPoint.getPeriod() == period).collect(Collectors.toList());
    }

    private static List<StudyPoint> importStudyPoints(Reader reader) {
        try {
            List<StudyPoint> studyPoints = new ArrayList<>();
            CSVReader csvReader = new CSVReaderBuilder(reader)
                    .withCSVParser(buildParser())
                    .build();
            List<String[]> lines = csvReader.readAll();
            String[] headers = lines.get(0);
            for (int i = 1; i < lines.size(); i++) {
                studyPoints.add(importStudyPoint(headers, lines.get(i)));
            }
            return studyPoints;
        } catch (Exception e) {
            LOGGER.error("Exception occurred during parsing.", e);
            return Collections.emptyList();
        }
    }

    private static StudyPoint importStudyPoint(String[] headers, String[] data) throws ParseException {
        int period = Integer.parseInt(data[0]);
        String id = data[1];
        Map<String, Double> positions = new HashMap<>();
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
