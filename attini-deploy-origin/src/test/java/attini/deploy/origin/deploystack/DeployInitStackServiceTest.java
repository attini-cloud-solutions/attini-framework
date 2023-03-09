package attini.deploy.origin.deploystack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.deploy.origin.config.AttiniConfig;
import attini.deploy.origin.config.AttiniConfigTestBuilder;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionContext;
import attini.domain.DistributionContextImpl;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

@ExtendWith(MockitoExtension.class)
class DeployInitStackServiceTest {

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
    CloudFormationClient cloudFormationClient;

    @Mock
    EnvironmentVariables environmentVariables;

    DeployInitStackService deployInitStackService;

    @BeforeEach
    void setUp() {
        deployInitStackService = new DeployInitStackService(cloudFormationClient, environmentVariables);
    }

    @Test
    void updateInitStack() {
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(GetTemplateSummaryResponse.builder().build());
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig().build();
        String deployPrefix = "dev/platform";
        when(cloudFormationClient.createStack(any(CreateStackRequest.class))).thenThrow(AlreadyExistsException.class);
        deployInitStackService.deployInitStack(DISTRIBUTION_CONTEXT, attiniConfig.getAttiniInitDeployStackConfig().get(), deployPrefix, attiniConfig.getAttiniDistributionTags());
        verify(cloudFormationClient).updateStack(any(UpdateStackRequest.class));
    }

    @Test
    void deployInitStack_shouldCreateNewStack() {
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(GetTemplateSummaryResponse.builder().build());
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig().build();
        String deployPrefix = "dev/platform";
        deployInitStackService.deployInitStack(DISTRIBUTION_CONTEXT, attiniConfig.getAttiniInitDeployStackConfig().get(), deployPrefix, attiniConfig.getAttiniDistributionTags());
        verify(cloudFormationClient).createStack(any(CreateStackRequest.class));
        verify(cloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));

    }

    @Test()
    void deployInitStack_awsFail() {
        when(cloudFormationClient.getTemplateSummary(any(GetTemplateSummaryRequest.class)))
                .thenReturn(GetTemplateSummaryResponse.builder().build());
        AwsServiceException awsServiceException = CloudFormationException.builder()
                                                                         .awsErrorDetails(AwsErrorDetails.builder()
                                                                                                         .errorMessage(
                                                                                                                 "it failed")
                                                                                                         .build())
                                                                         .build();

        when(cloudFormationClient.createStack(any(CreateStackRequest.class))).thenThrow(AlreadyExistsException.class);
        when(cloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenThrow(awsServiceException);
        AttiniConfig attiniConfig = AttiniConfigTestBuilder.aConfig().build();
        String deployPrefix = "dev/platform";

        Assertions.assertThrows(DeployInitStackException.class,
                                () -> deployInitStackService.deployInitStack(DISTRIBUTION_CONTEXT, attiniConfig.getAttiniInitDeployStackConfig().get(), deployPrefix, attiniConfig.getAttiniDistributionTags()));
        verify(cloudFormationClient).updateStack(any(UpdateStackRequest.class));

    }

}
