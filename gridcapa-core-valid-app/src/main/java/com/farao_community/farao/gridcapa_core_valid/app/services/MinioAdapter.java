/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import com.farao_community.farao.core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.app.configuration.MinioConfiguration;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class MinioAdapter {
    private static final int DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS = 7;
    private static final Logger LOGGER = LoggerFactory.getLogger(MinioAdapter.class);
    public static final String FORMAT_URL = "%s/%s";

    private final MinioClient client;
    private final String bucket;
    private final String basePath;

    public MinioAdapter(MinioConfiguration minioConfiguration, MinioClient minioClient) {
        this.client = minioClient;
        this.bucket = minioConfiguration.getBucket();
        this.basePath = minioConfiguration.getBasePath();
    }

    public String getBasePath() {
        return basePath;
    }

    public void uploadFile(String filePath, InputStream sourceInputStream) {
        String fullPath = String.format(FORMAT_URL, basePath, filePath);
        try {
            createBucketIfDoesNotExist(bucket);
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(fullPath).stream(sourceInputStream, -1, 50000000).build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new CoreValidInternalException(String.format("Exception occurred while uploading file: %s, to minio server", filePath));
        }
    }

    public void uploadFile(String filePath, ByteArrayOutputStream baos) {
        byte[] mainByteArray = baos.toByteArray();
        InputStream is = new ByteArrayInputStream(mainByteArray);
        uploadFile(filePath, is);
    }

    public String generatePreSignedUrl(String filePath) {
        String fullPath = String.format(FORMAT_URL, basePath, filePath);
        try {
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket).object(fullPath)
                            .expiry(DEFAULT_DOWNLOAD_LINK_EXPIRY_IN_DAYS, TimeUnit.DAYS)
                            .method(Method.GET)
                            .build()
            );
        } catch (Exception e) {
            throw new CoreValidInternalException("Exception in MinIO connection.", e);
        }
    }

    public void deleteCgmBeforeRao(String prefix) {
        Iterable<Result<Item>> files = listArtifacts(prefix);

        for (Result<Item> result : files) {
            try {
                String objectName = result.get().objectName();
                client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
                LOGGER.info("File {} deleted from Minio", objectName);
            } catch (Exception e) {
                LOGGER.error(String.format("Can not delete artifact starting with %s.", prefix));
            }
        }
    }

    private Iterable<Result<Item>> listArtifacts(String prefix) {
        return client.listObjects(ListObjectsArgs.builder()
                .bucket(bucket)
                .prefix(basePath + "artifacts/" + prefix)
                .recursive(true)
                .build());
    }

    public void deleteCgmAfterRao() {
        Iterable<Result<Item>> files = listArtifacts("RAO");

        for (Result<Item> result : files) {
            try {
                String objectName = result.get().objectName();
                if (objectName.contains("networkWithPRA.xiidm")) {
                    client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(result.get().objectName()).build());
                    LOGGER.info("File {} deleted from Minio", objectName);
                }
            } catch (Exception e) {
                LOGGER.error("Can not delete artifact CGM after RAO");
            }
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
