/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.deploy.origin.deploystack.InitDeployError;
import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.AttiniConfigTestBuilder;
import attini.deploy.origin.deploystack.DeployDataFacade;
import attini.deploy.origin.deploystack.DeployInitStackException;
import attini.deploy.origin.deploystack.DeployInitStackService;
import attini.deploy.origin.deploystack.DynamoInitDeployStackFacade;
import attini.deploy.origin.lifecycle.LifeCycleService;
import attini.deploy.origin.stepguard.StepGuardFacade;
import attini.domain.DistributionContext;
import attini.domain.DistributionContextImpl;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;

@ExtendWith(MockitoExtension.class)
class InitDeployServiceTest {

    private static final InitDeployEvent EVENT = new InitDeployEvent("myBucket",
                                                                     "dev/myFile.zip",
                                                                     ObjectIdentifier.of("someObjectIdentifier"),
                                                                     "pelle.the.boss");
    private static final String BUCKET_PATH = "my/path";
    private static final String TEMPlATE_MD5_HEX = "123232323";
    private static final long DEPLOY_TIME = 1000L;

    private final static DistributionContext DISTRIBUTION_CONTEXT = DistributionContextImpl.builder()
                                                                                           .distributionName(
                                                                                                   DistributionName.of(
                                                                                                           "my-dist"))
                                                                                           .objectIdentifier(
                                                                                                   ObjectIdentifier.of(
                                                                                                           "someObjectIdentifier"))
                                                                                           .distributionId(
                                                                                                   DistributionId.of(
                                                                                                           "dist-id"))
                                                                                           .environment(Environment.of(
                                                                                                   "dev"))
                                                                                           .build();


    @Mock
    PublishArtifactService publishArtifactService;
    @Mock
    DeployInitStackService deployInitStackService;
    @Mock
    DeployDataFacade deployDataFacade;
    @Mock
    PutLatestDistributionReferenceParameter putLatestDistributionReferenceParameter;
    @Mock
    DynamoInitDeployStackFacade dynamoInitDeployStackFacade;
    @Mock
    LifeCycleService lifeCycleService;
    @Mock
    MonitoringFacade monitoringFacade;
    @Mock
    SystemClockFacade systemClockFacade;
    @Mock
    StepGuardFacade stepGuardFacade;

    @Mock
    DistributionDataFacade distributionDataFacade;

    InitDeployService initDeployService;

    @BeforeEach
    void setUp() {
        when(systemClockFacade.getCurrentTime()).thenReturn(DEPLOY_TIME);

        initDeployService = new InitDeployService(publishArtifactService,
                                                  deployInitStackService,
                                                  deployDataFacade,
                                                  putLatestDistributionReferenceParameter,
                                                  dynamoInitDeployStackFacade,
                                                  lifeCycleService,
                                                  monitoringFacade,
                                                  systemClockFacade,
                                                  stepGuardFacade,
                                                  distributionDataFacade);
    }

    @Test
    void shouldDeployInitStack() {
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig()
                                                           .distributionId(DISTRIBUTION_CONTEXT.getDistributionId())
                                                           .distributionName(DISTRIBUTION_CONTEXT.getDistributionName())
                                                           .build();
        DistributionData distributionData = new DistributionData(attiniConfig,
                                                                 BUCKET_PATH, TEMPlATE_MD5_HEX);
        when(publishArtifactService.publishDistribution(EVENT)).thenReturn(distributionData);

        when(lifeCycleService.cleanup(any(DistributionName.class), any(Environment.class), anyString())).thenReturn(
                CompletableFuture.allOf());
        initDeployService.initDeploy(EVENT);


        verify(publishArtifactService).publishDistribution(EVENT);
        verify(putLatestDistributionReferenceParameter).putParameter(attiniConfig, EVENT.getEnvironmentName());
        verify(deployInitStackService).deployInitStack(DISTRIBUTION_CONTEXT,
                                                       attiniConfig.getAttiniInitDeployStackConfig().get(),
                                                       BUCKET_PATH,
                                                       attiniConfig.getAttiniDistributionTags());
        verify(deployDataFacade).save(DeployDataFacade.SaveDeploymentDataRequest.builder()
                                                                                .deployTime(DEPLOY_TIME)
                                                                                .distributionContext(
                                                                                        DISTRIBUTION_CONTEXT)
                                                                                .distributionData(distributionData)
                                                                                .isUnchanged(false)
                                                                                .build());
    }

    @Test
    void shouldOnlyPublishArtifact() {
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig()
                                                           .distributionName(DISTRIBUTION_CONTEXT.getDistributionName())
                                                           .distributionId(DISTRIBUTION_CONTEXT.getDistributionId())
                                                           .attiniInitDeployStackConfig(null)
                                                           .build();
        DistributionData distributionData = new DistributionData(attiniConfig,
                                                                 BUCKET_PATH, TEMPlATE_MD5_HEX);
        when(publishArtifactService.publishDistribution(EVENT)).thenReturn(distributionData);
        when(lifeCycleService.cleanup(any(DistributionName.class), any(Environment.class), anyString())).thenReturn(
                CompletableFuture.allOf());

        initDeployService.initDeploy(EVENT);

        verify(publishArtifactService).publishDistribution(EVENT);
        verify(putLatestDistributionReferenceParameter).putParameter(attiniConfig, EVENT.getEnvironmentName());
        verify(deployInitStackService, never()).deployInitStack(DISTRIBUTION_CONTEXT,
                                                                attiniConfig.getAttiniInitDeployStackConfig()
                                                                            .orElse(null),
                                                                BUCKET_PATH,
                                                                attiniConfig.getAttiniDistributionTags());
        verify(deployDataFacade).save(DeployDataFacade.SaveDeploymentDataRequest.builder()
                                                                                .deployTime(DEPLOY_TIME)
                                                                                .distributionContext(
                                                                                        DISTRIBUTION_CONTEXT)
                                                                                .distributionData(distributionData)
                                                                                .isUnchanged(false)
                                                                                .build());
    }

    @Test
    void shouldSaveErrorIfDeployStackFails() {
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig()
                                                           .distributionId(DISTRIBUTION_CONTEXT.getDistributionId())
                                                           .distributionName(DISTRIBUTION_CONTEXT.getDistributionName())
                                                           .build();
        DistributionData distributionData = new DistributionData(attiniConfig,
                                                                 BUCKET_PATH, TEMPlATE_MD5_HEX);
        when(publishArtifactService.publishDistribution(EVENT)).thenReturn(distributionData);

        DeployInitStackException stackException =
                new DeployInitStackException(CloudFormationException.builder()
                                                                    .awsErrorDetails(
                                                                            AwsErrorDetails
                                                                                    .builder()
                                                                                    .errorCode("ErrorCode")
                                                                                    .errorMessage("ErrorMessage")
                                                                                    .build())
                                                                    .build());
        doThrow(stackException)
                .when(deployInitStackService)
                .deployInitStack(DISTRIBUTION_CONTEXT,
                                 attiniConfig.getAttiniInitDeployStackConfig().get(),
                                 BUCKET_PATH,
                                 attiniConfig.getAttiniDistributionTags());

        assertThrows(InitDeployException.class, () -> initDeployService.initDeploy(EVENT));

        verify(publishArtifactService).publishDistribution(EVENT);
        verify(putLatestDistributionReferenceParameter).putParameter(attiniConfig, EVENT.getEnvironmentName());
        verify(deployInitStackService).deployInitStack(DISTRIBUTION_CONTEXT,
                                                       attiniConfig.getAttiniInitDeployStackConfig().get(),
                                                       BUCKET_PATH,
                                                       attiniConfig.getAttiniDistributionTags());
        verify(deployDataFacade).save(DeployDataFacade.SaveDeploymentDataRequest.builder()
                                                                                .deployTime(DEPLOY_TIME)
                                                                                .distributionContext(
                                                                                        DISTRIBUTION_CONTEXT)
                                                                                .distributionData(distributionData)
                                                                                .isUnchanged(false).error(new InitDeployError(stackException.getAwsErrorCode(),
                                                                                                                              stackException.getAwsErrorMessage()))
                                                                                .build());
    }

    @Test
    void shouldSaveErrorIfPublishArtifactFails() {
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig().attiniInitDeployStackConfig(null).build();

        doThrow(new PublishDistributionException(DistributionName.of("my-dist"),
                                                 DistributionId.of("dist-id"),
                                                 new RuntimeException()))
                .when(publishArtifactService)
                .publishDistribution(EVENT);
        assertThrows(InitDeployException.class, () -> initDeployService.initDeploy(EVENT));

        verify(publishArtifactService).publishDistribution(EVENT);
        verify(putLatestDistributionReferenceParameter, never()).putParameter(attiniConfig, EVENT.getEnvironmentName());
        verify(deployInitStackService, never()).deployInitStack(DISTRIBUTION_CONTEXT,
                                                                attiniConfig.getAttiniInitDeployStackConfig()
                                                                            .orElse(null),
                                                                BUCKET_PATH,
                                                                attiniConfig.getAttiniDistributionTags());
        verify(deployDataFacade).save(any(DeployDataFacade.SaveDeploymentDataRequest.class));
    }
}
