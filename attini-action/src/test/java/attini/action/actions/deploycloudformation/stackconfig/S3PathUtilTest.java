/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class S3PathUtilTest {

    @Test
    void resolvePath() {
        String path = S3PathUtil.resolvePath(
                "dev/infra/308bea51-524f-45b3-8df1-7b65291fee8f/distribution-origin/../config/default.json");

        assertEquals("dev/infra/308bea51-524f-45b3-8df1-7b65291fee8f/config/default.json", path);
    }

    @Test
    void resolvePath_shouldDoNothing() {
        String originalPath = "dev/infra/308bea51-524f-45b3-8df1-7b65291fee8f/distribution-origin/config/default.json";
        String path = S3PathUtil.resolvePath(originalPath);

        assertEquals(originalPath, path);
    }

    @Test
    void shouldHandleS3PrefixedPath() {
        String originalPath = "s3://attini-artifact-store-eu-central-1-855066048591/stage/hello-world/2022-05-02_13:14:42/distribution-origin/attini-config.yaml";
        String path = S3PathUtil.resolveS3PrefixedPath(originalPath);

        assertEquals("https://attini-artifact-store-eu-central-1-855066048591.s3.amazonaws.com/stage/hello-world/2022-05-02_13:14:42/distribution-origin/attini-config.yaml", path);
    }
}
