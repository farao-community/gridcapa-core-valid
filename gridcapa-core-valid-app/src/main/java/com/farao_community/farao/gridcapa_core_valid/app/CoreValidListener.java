/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_core_valid.api.JsonApiConverter;
import com.farao_community.farao.gridcapa_core_valid.api.exception.AbstractCoreValidException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class CoreValidListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidListener.class);
    private static final String TASK_STATUS_UPDATE = "task-status-update";

    private final JsonApiConverter jsonApiConverter;
    private final CoreValidHandler coreValidHandler;
    private final StreamBridge streamBridge;

    public CoreValidListener(final CoreValidHandler coreValidHandler,
                             final StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
        this.jsonApiConverter = new JsonApiConverter();
        this.coreValidHandler = coreValidHandler;
    }

    @Bean
    public Consumer<Flux<byte[]>> request() {
        return flux -> flux
                .doOnNext(this::onMessage)
                .subscribe();
    }

    public void onMessage(final byte[] req) {
        try {
            final CoreValidRequest coreValidRequest = jsonApiConverter.fromJsonMessage(req, CoreValidRequest.class);
            runCoreValidRequest(coreValidRequest);
        } catch (final RuntimeException e) {
            LOGGER.error("Core valid exception occurred", e);
        }
    }

    private void runCoreValidRequest(final CoreValidRequest coreValidRequest) {
        try {
            LOGGER.info("Core valid request received: {}", coreValidRequest);
            streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(coreValidRequest.getId()), TaskStatus.RUNNING));
            final String coreValidResponseId = coreValidHandler.handleCoreValidRequest(coreValidRequest);
            updateTaskStatus(coreValidResponseId, coreValidRequest.getTimestamp(), TaskStatus.SUCCESS);
        } catch (final AbstractCoreValidException e) {
            LOGGER.error("Core valid exception occurred", e);
            updateTaskStatus(coreValidRequest.getId(), coreValidRequest.getTimestamp(), TaskStatus.ERROR);
        } catch (final RuntimeException e) {
            LOGGER.error("Unknown exception occurred", e);
            updateTaskStatus(coreValidRequest.getId(), coreValidRequest.getTimestamp(), TaskStatus.ERROR);
        }
    }

    private void updateTaskStatus(final String requestId,
                                  final OffsetDateTime timestamp,
                                  final TaskStatus targetStatus) {
        streamBridge.send(TASK_STATUS_UPDATE, new TaskStatusUpdate(UUID.fromString(requestId), targetStatus));
        LOGGER.info("Updating task status to {} for timestamp {}", targetStatus, timestamp);
    }

}
