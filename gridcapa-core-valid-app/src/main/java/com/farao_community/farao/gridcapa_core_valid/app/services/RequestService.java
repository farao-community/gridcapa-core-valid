/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_core_valid.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_core_valid.api.exception.AbstractCoreValidException;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidResponse;
import com.farao_community.farao.gridcapa_core_valid.api.resource.ThreadLauncherResult;
import com.farao_community.farao.gridcapa_core_valid.app.CoreValidHandler;
import com.farao_community.farao.gridcapa_core_valid.app.util.GenericThreadLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Theo Pascoli {@literal <theo.pascoli at rte-france.com>}
 */
@Service
public class RequestService {
    private static final String TASK_STATUS_UPDATE = "task-status-update";
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);
    private final CoreValidHandler coreValidHandler;
    private final Logger businessLogger;
    private final JsonApiConverter jsonApiConverter = new JsonApiConverter();
    private final StreamBridge streamBridge;

    public RequestService(CoreValidHandler coreValidHandler, Logger businessLogger, StreamBridge streamBridge) {
        this.coreValidHandler = coreValidHandler;
        this.businessLogger = businessLogger;
        this.streamBridge = streamBridge;
    }

    public byte[] launchCoreValidRequest(byte[] req) {
        byte[] result;
        CoreValidRequest coreValidRequest = jsonApiConverter.fromJsonMessage(req, CoreValidRequest.class);
        // propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", coreValidRequest.getId());
        try {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidRequest.getId()), TaskStatus.RUNNING));
            LOGGER.info("Cse request received : {}", coreValidRequest);
            GenericThreadLauncher<CoreValidHandler, CoreValidResponse> launcher = new GenericThreadLauncher<>(coreValidHandler, coreValidRequest.getId(), coreValidRequest);
            launcher.start();
            ThreadLauncherResult<CoreValidResponse> coreValidResponse = launcher.getResult();
            if (coreValidResponse.hasError() && coreValidResponse.getException() != null) {
                throw coreValidResponse.getException();
            }
            Optional<CoreValidResponse> resp = coreValidResponse.getResult();
            if (resp.isPresent() && !coreValidResponse.hasError()) {
                result = sendCoreValidResponse(resp.get());
                LOGGER.info("Core Valid response sent: {}", resp.get());
            } else {
                businessLogger.info("Core Valid run has been interrupted");
                result = sendCoreValidResponse(new CoreValidResponse(coreValidRequest.getId(), null, null, null, null, null));
            }
        } catch (Exception e) {
            result = handleError(e, coreValidRequest.getId());
        }
        return result;
    }

    private byte[] sendCoreValidResponse(CoreValidResponse coreValidResponse) {
        if (coreValidResponse.getMainResultFileUrl() == null && coreValidResponse.getRexResultFileUrl() == null &&
                coreValidResponse.getRemedialActionsFileUrl() == null) {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidResponse.getId()), TaskStatus.INTERRUPTED));
        } else {
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidResponse.getId()), TaskStatus.SUCCESS));
        }
        return jsonApiConverter.toJsonMessage(coreValidResponse, CoreValidResponse.class);
    }

    private byte[] handleError(Exception e, String requestId) {
        AbstractCoreValidException coreValidException = new CoreValidInternalException("Core Valid run failed", e);
        LOGGER.error(coreValidException.getDetails(), coreValidException);
        businessLogger.error(coreValidException.getDetails());
        return sendErrorResponse(requestId, coreValidException);
    }

    private byte[] sendErrorResponse(String requestId, AbstractCoreValidException exception) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), TaskStatus.ERROR));
        return exceptionToJsonMessage(exception);
    }

    private byte[] exceptionToJsonMessage(AbstractCoreValidException e) {
        return jsonApiConverter.toJsonMessage(e);
    }

}
