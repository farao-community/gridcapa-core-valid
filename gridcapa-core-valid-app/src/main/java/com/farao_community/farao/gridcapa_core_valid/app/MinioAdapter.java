/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app;

import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.CoreValidServerProperties;
import io.minio.*;
import io.minio.http.Method;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class MinioAdapter {
    private static final int DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);

    private final MinioClient client;
    private final String bucket;
    private final String basePath;

    public MinioAdapter(CoreValidServerProperties serverProperties, MinioClient minioClient) {
        this.client = minioClient;
        this.bucket = serverProperties.getMinio().getBucket();
        this.basePath = serverProperties.getMinio().getBasePath();
    }

    @Bean
    public static MinioClient generateMinioClient(CoreValidServerProperties serverProperties) {
        LOGGER.info("Generates MinioClient bean");
        CoreValidServerProperties.MinioProperties minioProperties = serverProperties.getMinio();
        try {
            return MinioClient.builder().endpoint(minioProperties.getUrl()).credentials(minioProperties.getAccess().getName(), minioProperties.getAccess().getSecret()).build();
        } catch (Exception e) {
            throw new CoreValidInternalException("Exception in MinIO client generation", e);
        }
    }

    public void putFile(byte[] fileBytes, String filePath) {
        createBucketIfDoesNotExist(bucket);
        String fullFilePath = String.format("%s/%s", basePath, filePath);
        LOGGER.info("Put file '{}' in Minio bucket '{}'", fullFilePath, bucket);
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fullFilePath)
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .build());
        } catch (Exception e) {
            throw new CoreValidInternalException("Exception in MinIO connection.", e);
        }
    }

    public CoreValidFileResource generateFileResource(String filePath) {
        try {
            String fullFilePath = String.format("%s/%s", basePath, filePath);
            String filename = FilenameUtils.getName(filePath);
            LOGGER.info("Generates pre-signed URL for file '{}' in Minio bucket '{}'", fullFilePath, bucket);
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(bucket).object(fullFilePath).expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS).method(Method.GET).build());
            return new CoreValidFileResource(filename, url);
        } catch (Exception e) {
            throw new CoreValidInternalException("Exception in MinIO connection.", e);
        }
    }

    private void createBucketIfDoesNotExist(String bucket) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                LOGGER.info("Create Minio bucket '{}' that did not exist already", bucket);
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new CoreValidInternalException(String.format("Cannot create bucket '%s'", bucket), e);
        }
    }
}
