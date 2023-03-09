/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.facades;

import static java.util.Objects.requireNonNull;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3Facade {

    private final S3Client s3Client;

    public S3Facade(S3Client s3Client) {
        this.s3Client = requireNonNull(s3Client, "s3Client");
    }

    public byte[] getObject(String bucketName, String key) {
        return s3Client.getObject(GetObjectRequest.builder()
                                                  .bucket(bucketName)
                                                  .key(key)
                                                  .build(), ResponseTransformer.toBytes()).asByteArray();
    }

    public void saveObject(String bucketName,String key, byte[] file) {
        s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                           RequestBody.fromBytes(file));
    }

}
