/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.starter;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
// This config class allows the scanning of the package by Spring Boot, hence declaring CoreValidClient as a bean
@Configuration
@EnableConfigurationProperties(CoreValidClientProperties.class)
public class CoreValidAutoConfiguration {
}
