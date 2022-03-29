/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa_core_valid.app.services;

import io.minio.MinioClient;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.*;
import io.minio.messages.Item;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class MinioAdapterTest {

    @Autowired
    private MinioAdapter minioAdapter;

    @MockBean
    private MinioClient minioClient;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(minioClient);
    }

    @Test
    void uploadFileTest() throws Exception {
        minioAdapter.uploadFile("file/path", new ByteArrayInputStream("File content".getBytes()));
        Mockito.verify(minioClient, Mockito.times(1)).putObject(Mockito.any());
    }

    @Test
    void generatePreSignedUrlTest() throws Exception {
        Mockito.when(minioClient.getPresignedObjectUrl(Mockito.any())).thenReturn("http://url");
        String url = minioAdapter.generatePreSignedUrl("file/path");
        Mockito.verify(minioClient, Mockito.times(1)).getPresignedObjectUrl(Mockito.any());
        assertEquals("http://url", url);
    }

    @Test
    void listArtifactsTest() {
        Item item = new Item() {
            @Override
            public String objectName() {
                return "networkWithPRA.xiidm";
            }
        };
        List<Result<Item>> listRes = Collections.singletonList(new Result<>(item));
        Mockito.when(minioClient.listObjects(Mockito.any())).thenReturn(listRes);
        Iterable<Result<Item>> results = minioAdapter.listArtifacts("prefix");
        assertEquals(1, StreamSupport.stream(results.spliterator(), false).count());
        Mockito.verify(minioClient, Mockito.times(1)).listObjects(Mockito.any());
    }

    @Test
    void deleteCgmBeforeAndAfterRaoTest() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Item item = new Item() {
            @Override
            public String objectName() {
                return "networkWithPRA.xiidm";
            }
        };
        List<Result<Item>> listRes = Collections.singletonList(new Result<>(item));
        minioAdapter.deleteObjects(listRes);
        minioAdapter.deleteObjectsContainingString(listRes, "network");
        Mockito.verify(minioClient, Mockito.times(2)).removeObject(Mockito.any());
    }

    @Test
    void fileDoesNotExistOnMinio() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        Mockito.when(minioClient.statObject(Mockito.any())).thenReturn(null);
        minioAdapter.exists("filepath");
        Mockito.verify(minioClient, Mockito.times(1)).statObject(Mockito.any());
    }

    @Test
    void fileExistOnMinio() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        StatObjectResponse objectStat = Mockito.mock(StatObjectResponse.class);
        Mockito.when(minioClient.statObject(Mockito.any())).thenReturn(objectStat);
        minioAdapter.exists("filepath");
        Mockito.verify(minioClient, Mockito.times(1)).statObject(Mockito.any());
    }
}
