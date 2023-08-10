/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.data.crac_api.NetworkElement;
import com.farao_community.farao.data.crac_creation.creator.api.parameters.CracCreationParameters;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.FbConstraint;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCracCreator;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.crac_creator.FbConstraintCreationContext;
import com.farao_community.farao.data.crac_creation.creator.fb_constraint.importer.FbConstraintImporter;
import com.farao_community.farao.data.refprog.reference_program.ReferenceProgram;
import com.farao_community.farao.data.refprog.refprog_xml_importer.RefProgImporter;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidRaoException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileExporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.FileImporter;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetPositionsHandler;
import com.farao_community.farao.gridcapa_core_valid.app.services.NetworkHandler;
import com.farao_community.farao.gridcapa_core_valid.app.services.results_export.ResultType;
import com.farao_community.farao.gridcapa_core_valid.app.study_point.*;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.rao_runner.api.resource.RaoRequest;
import com.farao_community.farao.rao_runner.api.resource.RaoResponse;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService.DEFAULT_PMAX;
import static com.farao_community.farao.gridcapa_core_valid.app.study_point.StudyPointService.DEFAULT_PMIN;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class CoreValidHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidHandler.class);
    private static final DateTimeFormatter ARTIFACTS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm");

    private final Logger eventsLogger;
    private final FileExporter fileExporter;
    private final FileImporter fileImporter;
    private final MinioAdapter minioAdapter;
    private final StudyPointService studyPointService;

    public CoreValidHandler(StudyPointService studyPointService, FileImporter fileImporter, FileExporter fileExporter, MinioAdapter minioAdapter, Logger eventsLogger) {
        this.studyPointService = studyPointService;
        this.fileImporter = fileImporter;
        this.fileExporter = fileExporter;
        this.minioAdapter = minioAdapter;
        this.eventsLogger = eventsLogger;
    }

    public CoreValidResponse handleCoreValidRequest(CoreValidRequest coreValidRequest) {
        final String formattedTimestamp = setUpEventLogging(coreValidRequest);

        try {

            InputStream networkStream = null;
            try {
                networkStream = new FileInputStream(coreValidRequest.getCgm().getUrl().substring(5));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Network network = NetworkHandler.loadNetwork(coreValidRequest.getCgm().getFilename(), networkStream);

            CracCreationParameters cracCreationParameters = new CracCreationParameters();
            cracCreationParameters.setDefaultMonitoredLineSide(CracCreationParameters.MonitoredLineSide.MONITOR_LINES_ON_LEFT_SIDE);
            InputStream cracStream = null;
            try {
                cracStream = new FileInputStream(coreValidRequest.getCbcora().getUrl().substring(5));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            FbConstraint nativeCrac = new FbConstraintImporter().importNativeCrac(cracStream);
            FbConstraintCreationContext cracCreationContext = new FbConstraintCracCreator().createCrac(nativeCrac, network, coreValidRequest.getTimestamp(), cracCreationParameters);

            Instant computationStartInstant = Instant.now();
            List<StudyPointResult> studyPointResults = computeStudyPoints(coreValidRequest, network, cracCreationContext, formattedTimestamp);
            Instant computationEndInstant = Instant.now();

            Map<ResultType, String> resultFileUrls = postTreatment(studyPointResults, coreValidRequest, cracCreationContext, formattedTimestamp);

            return new CoreValidResponse(coreValidRequest.getId(), resultFileUrls.get(ResultType.MAIN_RESULT), resultFileUrls.get(ResultType.REX_RESULT), resultFileUrls.get(ResultType.REMEDIAL_ACTIONS_RESULT), computationStartInstant, computationEndInstant);
        } catch (InterruptedException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            Thread.currentThread().interrupt();
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        } catch (ExecutionException e) {
            eventsLogger.error("Error during core request running for timestamp {}.", formattedTimestamp);
            throw new CoreValidInternalException(String.format("Error during core request running for timestamp '%s'", coreValidRequest.getTimestamp()), e);
        }
    }

    private static String setUpEventLogging(CoreValidRequest coreValidRequest) {
        MDC.put("gridcapa-task-id", coreValidRequest.getId());
        return TIMESTAMP_FORMATTER.format(coreValidRequest.getTimestamp());
    }

    private List<StudyPointResult> computeStudyPoints(CoreValidRequest coreValidRequest, Network network, FbConstraintCreationContext cracCreationContext, String formattedTimestamp) throws InterruptedException, ExecutionException {
        Map<StudyPoint, RaoRequest> studyPointRaoRequests = new HashMap<>();
        Map<StudyPoint, CompletableFuture<RaoResponse>> studyPointCompletableFutures = new HashMap<>();
        List<StudyPointResult> studyPointResults = new ArrayList<>();

        InputStream studyPointStream = null;
        try {
            studyPointStream = new FileInputStream(coreValidRequest.getStudyPoints().getUrl().substring(5));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<StudyPoint> studyPoints = StudyPointsImporter.importStudyPoints(studyPointStream, coreValidRequest.getTimestamp());
        if (!studyPoints.isEmpty()) {
            StudyPointData studyPointData = fillStudyPointData(coreValidRequest, network, cracCreationContext);
            //computeStudyPointShift
            StudyPoint studyPoint0 = studyPoints.get(0);
            LOGGER.info("Running computation for study point {} ", studyPoint0.getVerticeId());
            network.getTieLine(cracCreationContext.getCrac().getContingency("BE-FR_CO_00008").getNetworkElements().iterator().next().getId()).getTerminal1().disconnect();
            network.getTieLine(cracCreationContext.getCrac().getContingency("BE-FR_CO_00008").getNetworkElements().iterator().next().getId()).getTerminal2().disconnect();
            ZonalData<Scalable> scalableZonalData = studyPointData.getScalableZonalData();
            Map<String, Double> coreNetPositions = studyPointData.getCoreNetPositions();
            String jsonCracUrl = studyPointData.getJsonCracUrl();
            String raoParametersUrl = studyPointData.getRaoParametersUrl();
            RaoRequest raoRequest = null;
            String initialVariant = network.getVariantManager().getWorkingVariantId();
            String newVariant = initialVariant + "_" + studyPoint0.getVerticeId();
            network.getVariantManager().cloneVariant(initialVariant, newVariant);
            network.getVariantManager().setWorkingVariant(newVariant);
            try {
                Map<String, InitGenerator> initGenerators = setPminPmaxToDefaultValue(network, scalableZonalData);
                NetPositionsHandler.shiftNetPositionToStudyPoint(network, studyPoint0, scalableZonalData, coreNetPositions);
                LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
                loadFlowParameters.setDc(true);
                loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD);
                String countries = "AT,BA,BE,BG,CH,CZ,DE,ES,FR,GR,HR,HU,IT,ME,MK,NL,PL,PT,RO,RS,SI,SK,TR,UA";
                Set<Country> countrySet = new HashSet<>();
                for (String countryCode : countries.split(",")) {
                    countrySet.add(Country.valueOf(countryCode));
                }
                loadFlowParameters.setCountriesToBalance(countrySet);
                LoadFlow.find("OpenLoadFlow").run(network, loadFlowParameters);
                NetworkElement networkElement = (NetworkElement) cracCreationContext.getCrac().getCnec("FR_CBCO_00123 - curative").getNetworkElements().iterator().next();
                Identifiable line = network.getIdentifiable(networkElement.getId());
                resetInitialPminPmax(network, scalableZonalData, initGenerators);
                String shiftedCgmUrl = fileExporter.saveShiftedCgm(network, studyPoint0);
                studyPoint0.getStudyPointResult().setShiftedCgmUrl(shiftedCgmUrl);
                String raoDirPath = String.format("%s/artifacts/RAO-%s-%s/", minioAdapter.getProperties().getBasePath(), coreValidRequest.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'_'HH-mm")), studyPoint0.getVerticeId());
                // For rao logs dispatcher, the rao request should correspond to the core valid request
                raoRequest = new RaoRequest(coreValidRequest.getId(), shiftedCgmUrl, jsonCracUrl, raoParametersUrl, raoDirPath);
            } catch (Exception e) {
                LOGGER.error("Error during study point {} computation", studyPoint0.getVerticeId(), e);
                studyPoint0.getStudyPointResult().setStatus(StudyPointResult.Status.ERROR);
            } finally {
                network.getVariantManager().setWorkingVariant(initialVariant);
                network.getVariantManager().removeVariant(newVariant);
            }

            studyPointService.computeStudyPointShift(studyPoints.get(0), studyPointData, coreValidRequest.getTimestamp(), coreValidRequest.getId());
            studyPoints.forEach(studyPoint -> studyPointRaoRequests.put(studyPoint, studyPointService.computeStudyPointShift(studyPoint, studyPointData, coreValidRequest.getTimestamp(), coreValidRequest.getId())));
            eventsLogger.info("All studypoints shifts are done for timestamp {}", formattedTimestamp);
            runRaoForEachStudyPoint(studyPointRaoRequests, studyPointCompletableFutures);
            studyPointResults = fillResultsForEachStudyPoint(studyPointData, studyPointCompletableFutures);
        }
        return studyPointResults;
    }

    private StudyPointData fillStudyPointData(CoreValidRequest coreValidRequest, Network network, FbConstraintCreationContext cracCreationContext) {
        InputStream refProgStream = null;
        try {
            refProgStream = new FileInputStream(coreValidRequest.getRefProg().getUrl().substring(5));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ReferenceProgram referenceProgram = RefProgImporter.importRefProg(refProgStream, coreValidRequest.getTimestamp());
        Map<String, Double> coreNetPositions = NetPositionsHandler.computeCoreReferenceNetPositions(referenceProgram);

        InputStream glskStream = null;
        try {
            glskStream = new FileInputStream(coreValidRequest.getGlsk().getUrl().substring(5));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(glskStream);
        ZonalData<Scalable> scalableZonalData = glskDocument.getZonalScalable(network, coreValidRequest.getTimestamp().toInstant());

        String jsonCracUrl = fileExporter.saveCracInJsonFormat(cracCreationContext.getCrac(), coreValidRequest.getTimestamp());
        RaoParameters raoParameters = RaoParameters.load();
        String raoParametersUrl = fileExporter.saveRaoParametersAndGetUrl(raoParameters);
        return new StudyPointData(network, coreNetPositions, scalableZonalData, cracCreationContext, jsonCracUrl, raoParametersUrl);
    }

    private void runRaoForEachStudyPoint(Map<StudyPoint, RaoRequest> studyPointRaoRequests, Map<StudyPoint, CompletableFuture<RaoResponse>> studyPointCompletableFutures) throws ExecutionException, InterruptedException {
        studyPointRaoRequests.forEach((studyPoint, raoRequest) -> {
            CompletableFuture<RaoResponse> raoResponse = studyPointService.computeStudyPointRao(studyPoint, raoRequest);
            studyPointCompletableFutures.put(studyPoint, raoResponse);
            raoResponse.thenApply(raoResponse1 -> {
                LOGGER.info("End of RAO for studypoint {} ...", studyPoint.getVerticeId());
                return null;
            }).exceptionally(exception -> {
                studyPoint.getStudyPointResult().setStatusToError();
                eventsLogger.error("Error during RAO computation for studypoint {}.", studyPoint.getVerticeId());
                throw new CoreValidRaoException(String.format("Error during RAO computation for studypoint %s .", studyPoint.getVerticeId()));
            });
        });
        CompletableFuture.allOf(studyPointCompletableFutures.values().toArray(new CompletableFuture[0])).get();
    }

    private List<StudyPointResult> fillResultsForEachStudyPoint(StudyPointData studyPointData, Map<StudyPoint, CompletableFuture<RaoResponse>> studyPointCompletableFutures) throws InterruptedException, ExecutionException {
        List<StudyPointResult> studyPointResults = new ArrayList<>();
        for (Map.Entry<StudyPoint, CompletableFuture<RaoResponse>> entry : studyPointCompletableFutures.entrySet()) {
            StudyPoint studyPoint = entry.getKey();
            RaoResponse raoResponse = entry.getValue().get();
            Network networkWithPra = fileImporter.importNetworkFromUrl(raoResponse.getNetworkWithPraFileUrl());
            String fileName = networkWithPra.getNameOrId() + "_" + studyPoint.getVerticeId() + "_withPra.uct";
            fileExporter.saveShiftedCgmWithPra(networkWithPra, fileName);
            studyPointResults.add(studyPointService.postTreatRaoResult(studyPoint, studyPointData, raoResponse));
        }
        return studyPointResults;
    }

    private Map<ResultType, String> postTreatment(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest, FbConstraintCreationContext cracCreationContext, String formattedTimestamp) {
        Map<ResultType, String> resultFileUrls = saveProcessOutputs(studyPointResults, coreValidRequest, cracCreationContext);
        if (coreValidRequest.getLaunchedAutomatically()) {
            deleteArtifacts(coreValidRequest);
        }
        eventsLogger.info("Process done for timestamp {}.", formattedTimestamp);
        return resultFileUrls;
    }

    private Map<ResultType, String> saveProcessOutputs(List<StudyPointResult> studyPointResults, CoreValidRequest coreValidRequest, FbConstraintCreationContext cracCreationContext) {
        return fileExporter.exportStudyPointResult(studyPointResults, coreValidRequest, cracCreationContext);
    }

    private void deleteArtifacts(CoreValidRequest coreValidRequest) {
        deleteCgmBeforeRao(ARTIFACTS_FORMATTER.format(coreValidRequest.getTimestamp().atZoneSameInstant(ZoneId.of("Europe/Paris"))));
    }

    private void deleteCgmBeforeRao(String prefix) {
        List<String> results = minioAdapter.listFiles("artifacts/" + prefix);
        minioAdapter.deleteFiles(results);
    }

    private static class InitGenerator {
        double pMin;
        double pMax;

        public double getpMin() {
            return pMin;
        }

        public void setpMin(double pMin) {
            this.pMin = pMin;
        }

        public double getpMax() {
            return pMax;
        }

        public void setpMax(double pMax) {
            this.pMax = pMax;
        }
    }

    private Map<String, InitGenerator> setPminPmaxToDefaultValue(Network network, ZonalData<Scalable> scalableZonalData) {
        Map<String, InitGenerator> initGenerators = new HashMap<>();
        CoreAreasId.getCountriesId().stream().map(zone -> new CountryEICode(Country.valueOf(zone)).getCode()).map(scalableZonalData::getData).filter(Objects::nonNull).forEach(scalable -> {
            scalable.filterInjections(network).forEach(injection -> {
                if (!injection.getTerminal().isConnected()) {
                    LOGGER.warn("reconnecting {}", injection.getId());
                    injection.getTerminal().connect();
                }
            });
        });
        CoreAreasId.getCountriesId().stream().map(zone -> new CountryEICode(Country.valueOf(zone)).getCode()).map(scalableZonalData::getData).filter(Objects::nonNull).map(scalable -> scalable.filterInjections(network).stream()
            .filter(Generator.class::isInstance)
            .map(Generator.class::cast)
            .collect(Collectors.toList())).forEach(generators -> generators.forEach(generator -> {
            if (Double.isNaN(generator.getTargetP())) {
                generator.setTargetP(0.);
            }
            InitGenerator initGenerator = new InitGenerator();
            initGenerator.setpMin(generator.getMinP());
            initGenerator.setpMax(generator.getMaxP());
            initGenerators.put(generator.getId(), initGenerator);
            generator.setMinP(DEFAULT_PMIN);
            generator.setMaxP(DEFAULT_PMAX);
        }));
        LOGGER.info("Pmax and Pmin are set to default values for network {}", network.getNameOrId());
        return initGenerators;
    }

    private void resetInitialPminPmax(Network network, ZonalData<Scalable> scalableZonalData, Map<String, InitGenerator> initGenerators) {
        CoreAreasId.getCountriesId().forEach(zone -> {
            String zoneEiCode = new CountryEICode(Country.valueOf(zone)).getCode();
            Scalable scalable = scalableZonalData.getData(zoneEiCode);
            if (scalable != null) {
                List<Generator> generators = scalable.filterInjections(network).stream()
                    .filter(Generator.class::isInstance)
                    .map(Generator.class::cast)
                    .collect(Collectors.toList());

                generators.forEach(generator -> {
                    generator.setMaxP(Math.max(generator.getTargetP(), initGenerators.get(generator.getId()).getpMax()));
                    generator.setMinP(Math.min(generator.getTargetP(), initGenerators.get(generator.getId()).getpMin()));
                });
            }
        });
        LOGGER.info("Pmax and Pmin are reset to initial values for network {}", network.getNameOrId());
    }
}
