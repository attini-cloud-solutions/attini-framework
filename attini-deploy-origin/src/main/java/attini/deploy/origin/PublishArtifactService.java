/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.AttiniConfigFactory;
import attini.deploy.origin.config.InitDeployStackConfig;
import attini.deploy.origin.s3.S3Facade;
import attini.deploy.origin.s3.TagOriginObjectService;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.deploy.origin.zip.ZipUtil;
import attini.domain.DistributionId;
import attini.domain.DistributionName;

public class PublishArtifactService {

    private static final Logger logger = Logger.getLogger(InitDeployService.class);


    private final S3Facade s3Facade;
    private final AttiniConfigFactory attiniConfigFactory;
    private final EnvironmentVariables environmentVariables;
    private final TagOriginObjectService tagOriginObjectService;

    public PublishArtifactService(S3Facade s3Facade,
                                  AttiniConfigFactory attiniConfigFactory,
                                  EnvironmentVariables environmentVariables,
                                  TagOriginObjectService tagOriginObjectService) {
        this.s3Facade = requireNonNull(s3Facade, "s3Facade");
        this.attiniConfigFactory = requireNonNull(attiniConfigFactory, "attiniConfigFactory");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.tagOriginObjectService = requireNonNull(tagOriginObjectService, "tagOriginObjectService");
    }

    public DistributionData publishDistribution(InitDeployEvent initDeployEvent) {

        byte[] file = s3Facade.downloadS3File(initDeployEvent.getS3Bucket(), initDeployEvent.getS3Key());

        Path zipDir = ZipUtil.unzip(file);
        try {

            AttiniConfig attiniConfig = attiniConfigFactory.createAttiniConfig(zipDir, initDeployEvent);

            logger.info("Attini config successfully created");
            logger.info(attiniConfig);

            tagOriginObjectService.tagOriginObject(initDeployEvent, attiniConfig);

            String artifactPath = MessageFormat.format("{0}/{1}/{2}/{3}",
                                                       initDeployEvent.getEnvironmentName().asString(),
                                                       attiniConfig.getAttiniDistributionName().asString(),
                                                       attiniConfig.getAttiniDistributionId().asString(),
                                                       "distribution-origin");
            logger.info("Beginning to upload distribution files to artifact store");
            s3Facade.uploadDirectory(
                    zipDir,
                    environmentVariables.getArtifactBucket(),
                    artifactPath, attiniConfig.getAttiniDistributionName(), attiniConfig.getAttiniDistributionId());

            logger.info("Done uploading distribution files to artifact store");
            logger.info("Beginning to upload distribution zip to artifact store");

            s3Facade.uploadFile(file,
                                environmentVariables.getArtifactBucket(),
                                artifactPath,
                                initDeployEvent.getFileName());

            logger.info("Done uploading distribution zip to artifact store");

            return new DistributionData(attiniConfig,
                                        artifactPath,
                                        attiniConfig.getAttiniInitDeployStackConfig()
                                                    .map(createMd5Hex(initDeployEvent, zipDir))
                                                    .orElse(null));

        } catch (AttiniConfigException e) {
            throw new PublishDistributionException(e.getDistributionName(), e.getDistributionId(), e);
        } catch (Exception e) {
            throw new PublishDistributionException(DistributionName.of(initDeployEvent.getFolderName()),
                                                   DistributionId.of("undefined"),
                                                   e);
        } finally {
            cleanUp(zipDir);
        }
    }

    private Function<InitDeployStackConfig, String> createMd5Hex(InitDeployEvent initDeployEvent, Path zipDir) {
        return attiniInitDeployStackConfig -> {

            String params = attiniInitDeployStackConfig
                    .getParameters()
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + entry.getValue()).sorted()
                    .collect(Collectors.joining());

            String tags = attiniInitDeployStackConfig
                    .getTags(initDeployEvent.getEnvironmentName())
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + entry.getValue()).sorted()
                    .collect(Collectors.joining());

            String variables = attiniInitDeployStackConfig
                    .getVariables(initDeployEvent.getEnvironmentName())
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + entry.getValue()).sorted()
                    .collect(Collectors.joining());

            String templateMd5Hex = createTemplateMd5Hex(zipDir,
                                                         attiniInitDeployStackConfig);

            return DigestUtils.md5Hex(params + tags + variables + environmentVariables.getAttiniVersion()) + templateMd5Hex;

        };
    }

    private String createTemplateMd5Hex(Path zipDir, InitDeployStackConfig initDeployStackConfig) {
        try {
            Path of = createTemplatePath(zipDir, initDeployStackConfig);

            return DigestUtils.md5Hex(new FileInputStream(of.toFile()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path createTemplatePath(Path zipDir, InitDeployStackConfig initDeployStackConfig) {
        return Path.of(zipDir + "/" + initDeployStackConfig
                .getAttiniInitDeployTemplatePath());
    }


    private static void cleanUp(Path zipDir) {
        try {
            FileUtils.deleteDirectory(zipDir.toFile());
            logger.info("cleaned upp distribution files after uploading to the artifact store.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
