/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInvalidDataException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPoint;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointsImporter;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintCreationContext;
import com.powsybl.openrao.data.crac.io.fbconstraint.FbConstraintImporter;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.List;

import static com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_SIDE_ONE;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class FileImporter {
    private final UrlValidationService urlValidationService;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    public FileImporter(final UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public Network importNetwork(final CoreValidFileResource cgmFile) {
        try (final InputStream networkStream = urlValidationService.openUrlStream(cgmFile.getUrl())) {
            return NetworkHandler.loadNetwork(cgmFile.getFilename(), networkStream);
        } catch (final IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download network file from URL '%s'", cgmFile.getUrl()), e);
        }
    }

    public Network importNetworkFromUrl(final String cgmUrl) {
        try (final InputStream networkStream = urlValidationService.openUrlStream(cgmUrl)) {
            return Network.read(getFilenameFromUrl(cgmUrl), networkStream);
        } catch (final IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download network file from URL '%s'", cgmUrl), e);
        }
    }

    public GlskDocument importGlskFile(final CoreValidFileResource glskFileResource) {
        try (final InputStream glskStream = urlValidationService.openUrlStream(glskFileResource.getUrl())) {
            LOGGER.info("Import of Glsk file {} ", glskFileResource.getFilename());
            return GlskDocumentImporters.importGlsk(glskStream);
        } catch (final IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download reference program file from URL '%s'", glskFileResource.getUrl()), e);
        }
    }

    public ReferenceProgram importReferenceProgram(final CoreValidFileResource refProgFile,
                                                   final OffsetDateTime timestamp) {
        try (final InputStream refProgStream = urlValidationService.openUrlStream(refProgFile.getUrl())) {
            return RefProgImporter.importRefProg(refProgStream, timestamp);
        } catch (final IOException e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download GLSK file from URL '%s'", refProgFile.getUrl()), e);
        }
    }

    public List<StudyPoint> importStudyPoints(final CoreValidFileResource studyPointsFileResource,
                                              final OffsetDateTime timestamp) {
        try (final InputStream studyPointsStream = urlValidationService.openUrlStream(studyPointsFileResource.getUrl())) {
            LOGGER.info("Import of study points from {} file for timestamp {} ", studyPointsFileResource.getFilename(), timestamp);
            return StudyPointsImporter.importStudyPoints(studyPointsStream, timestamp);
        } catch (final Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download study points file from URL '%s'", studyPointsFileResource.getUrl()), e);
        }
    }

    public FbConstraintCreationContext importCrac(final String cbcoraUrl,
                                                  final OffsetDateTime targetProcessDateTime,
                                                  final Network network) {
        final CracCreationParameters cracCreationParameters = new CracCreationParameters();
        cracCreationParameters.setDefaultMonitoredLineSide(MONITOR_LINES_ON_SIDE_ONE);
        cracCreationParameters.addExtension(FbConstraintCracCreationParameters.class,
                                            new FbConstraintCracCreationParameters());
        cracCreationParameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(targetProcessDateTime);
        try (final InputStream cracInputStream = urlValidationService.openUrlStream(cbcoraUrl)) {
            return (FbConstraintCreationContext) new FbConstraintImporter().importData(cracInputStream, cracCreationParameters, network);
        } catch (final Exception e) {
            throw new CoreValidInvalidDataException(String.format("Cannot download cbcora file from URL '%s'", cbcoraUrl), e);
        }
    }

    String getFilenameFromUrl(final String url) {
        try {
            return FilenameUtils.getName(new URI(url).toURL().getPath());
        } catch (final MalformedURLException | URISyntaxException | IllegalArgumentException e) {
            throw new CoreValidInvalidDataException(String.format("URL is invalid: %s", url), e);
        }
    }
}
