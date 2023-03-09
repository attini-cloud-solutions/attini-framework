/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;

import attini.action.CloudFormationClientFactory;
import attini.action.CloudFormationClientFactory.GetCloudFormationClientRequest;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateTerminationProtectionRequest;

public class CfnStackFacade {

    private static final Logger logger = Logger.getLogger(CfnStackFacade.class);

    private static final Set<String> CAPABILITIES = Set.of(
            "CAPABILITY_AUTO_EXPAND",
            "CAPABILITY_NAMED_IAM",
            "CAPABILITY_IAM");

    private static final Set<String> RETRY_CFN_STATUS = Set.of(
            "CREATE_IN_PROGRESS",
            "ROLLBACK_IN_PROGRESS",
            "DELETE_IN_PROGRESS",
            "UPDATE_IN_PROGRESS",
            "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS",
            "UPDATE_ROLLBACK_IN_PROGRESS",
            "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS",
            "REVIEW_IN_PROGRESS",
            "IMPORT_IN_PROGRESS",
            "IMPORT_ROLLBACK_IN_PROGRESS",
            "IMPORT_ROLLBACK_FAILED");


    private final EnvironmentVariables environmentVariables;
    private final CloudFormationClientFactory cloudFormationClientFactory;

    public CfnStackFacade(CloudFormationClientFactory cloudFormationClientFactory,
                          EnvironmentVariables environmentVariables) {
        this.cloudFormationClientFactory = requireNonNull(cloudFormationClientFactory, "cloudFormationClientFactory");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public void deleteStack(StackData stackData) {

        try {
            StackConfiguration stackConfiguration = stackData.getStackConfiguration();
            DeleteStackRequest deleteStackRequest = deleteStackBuilder(stackData)
                    .build();
            getClient(stackConfiguration).deleteStack(deleteStackRequest);

        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorCode().equals("ValidationError") && !e.getMessage()
                                                                               .contains("TerminationProtection")) {
                logger.info("Stack " + stackData.getStackConfiguration().getStackName() + " is already deleted");
            } else {
                throw e;
            }
        }
    }

    public String createCfnStack(StackData stackData) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();
        logger.info(String.format("Creating cloudformation stack: %s", stackConfiguration.getStackName()));
        logger.info("using callback strategy for deploying stack");

        CreateStackResponse response = getClient(stackConfiguration)
                .createStack(createStackBuilder(stackData)
                                     .notificationARNs(environmentVariables.getSnsNotificationArn())
                                     .build());
        return response.stackId();

    }

    public boolean stackHasNotificationArn(StackData stackData) {
       return getClient(stackData.getStackConfiguration())
                .describeStacks(DescribeStacksRequest.builder()
                                                     .stackName(
                                                             stackData.getStackConfiguration()
                                                                      .getStackName())
                                                     .build())
                .stacks()
                .stream()
                .findAny()
                .map(stack -> stack.notificationARNs().stream().anyMatch(s -> environmentVariables.getSnsNotificationArn().equals(s)))
                .orElse(false);
    }

    public String updateCfnStack(StackData stackData) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();
        logger.info(String.format("Updating cloudformation stack: %s", stackConfiguration.getStackName()));
        logger.info("using callback strategy for deploying stack");


        CloudFormationClient client = getClient(stackConfiguration);

        stackData.getStackConfiguration()
                 .getEnableTerminationProtection()
                 .ifPresent(aBoolean ->
                            {
                                logger.info("Updating termination protection");
                                client.updateTerminationProtection(UpdateTerminationProtectionRequest.builder()
                                                                                                     .stackName(
                                                                                                             stackData.getStackConfiguration()
                                                                                                                      .getStackName())
                                                                                                     .enableTerminationProtection(
                                                                                                             aBoolean)
                                                                                                     .build());
                            });
        UpdateStackResponse response =
                client.updateStack(updateStackBuilder(stackData)
                                           .notificationARNs(environmentVariables.getSnsNotificationArn())
                                           .build());


        return response.stackId();


    }

    public StackStatus getStackStatus(StackData stackData) {

        DescribeStackEventsRequest request = DescribeStackEventsRequest.builder()
                                                                       .stackName(stackData.getStackConfiguration()
                                                                                           .getStackName())
                                                                       .build();

        List<StackEvent> stackEvents = getClient(stackData.getStackConfiguration()).describeStackEvents(request)
                                                                                   .stackEvents();

        return stackEvents.stream()
                          .filter(stackEvent -> stackEvent.logicalResourceId()
                                                          .equals(stackEvent.stackName()))
                          .findFirst()
                          .map(stackEvent -> {
                              if (RETRY_CFN_STATUS.contains(stackEvent.resourceStatusAsString())) {
                                  return new StackStatus(stackEvent.clientRequestToken(),
                                                         StackStatus.StackState.UPDATE_IN_PROGRESS,
                                                         stackEvent.resourceStatusAsString(),
                                                         stackEvent.stackId());
                              } else {
                                  return new StackStatus(stackEvent.clientRequestToken(),
                                                         StackStatus.StackState.COMPLETE,
                                                         stackEvent.resourceStatusAsString(),
                                                         stackEvent.stackId());

                              }
                          })
                          .orElseThrow(() -> new IllegalStateException("The stack exists but there are no events for the stack. This should be a impossible scenario, please report to Attini."));
    }


    public String createStackCrossRegion(StackData stackData) {
        return getClient(stackData.getStackConfiguration()).createStack(createStackBuilder(stackData).build())
                                                           .stackId();

    }

    public String updateStackCrossRegion(StackData stackData) {
        return getClient(stackData.getStackConfiguration()).updateStack(updateStackBuilder(stackData).build())
                                                           .stackId();

    }


    private DeleteStackRequest.Builder deleteStackBuilder(StackData stackData) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();
        return DeleteStackRequest.builder()
                                 .clientRequestToken(stackData.getClientRequestToken().asString())
                                 .roleARN(stackConfiguration.getStackRole().orElse(null))
                                 .stackName(stackConfiguration.getStackName());
    }

    private UpdateStackRequest.Builder updateStackBuilder(StackData stackData) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();
        return UpdateStackRequest.builder()
                                 .tags(getTags(stackData))
                                 .parameters(stackConfiguration.getParameters())
                                 .clientRequestToken(stackData.getClientRequestToken().asString())
                                 .capabilitiesWithStrings(CAPABILITIES)
                                 .roleARN(stackConfiguration.getStackRole().orElse(null))
                                 .stackName(stackConfiguration.getStackName())
                                 .templateURL(stackConfiguration.getTemplate());
    }

    private CreateStackRequest.Builder createStackBuilder(StackData stackData) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();

        return CreateStackRequest.builder()
                                 .enableTerminationProtection(stackConfiguration.getEnableTerminationProtection()
                                                                                .orElse(null))
                                 .onFailure(stackConfiguration.getOnFailure().map(Enum::name).orElse(null))
                                 .tags(getTags(stackData))
                                 .parameters(stackConfiguration.getParameters())
                                 .clientRequestToken(stackData.getClientRequestToken().asString())
                                 .capabilitiesWithStrings(CAPABILITIES)
                                 .roleARN(stackConfiguration.getStackRole().orElse(null))
                                 .stackName(stackConfiguration.getStackName())
                                 .templateURL(stackConfiguration.getTemplate());
    }


    private Collection<Tag> getTags(StackData stackData) {
        List<Tag> attiniTags =
                List.of(Tag.builder()
                           .key("AttiniDistributionName")
                           .value(stackData.getDistributionName().asString())
                           .build(),
                        Tag.builder()
                           .key(environmentVariables.getEnvironmentParameterName())
                           .value(stackData.getEnvironment().asString())
                           .build(),
                        Tag.builder()
                           .key("AttiniResourceType")
                           .value("cloudformation-stack")
                           .build());

        return CollectionsUtils.combineCollections(stackData.getStackConfiguration().getTags(), attiniTags);

    }

    private CloudFormationClient getClient(StackConfiguration stackConfiguration) {
        GetCloudFormationClientRequest.Builder cfnClientRequest =
                GetCloudFormationClientRequest
                        .builder();
        stackConfiguration.getRegion().ifPresent(cfnClientRequest::setRegion);
        stackConfiguration.getExecutionRole().ifPresent(cfnClientRequest::setExecutionRoleArn);

        return cloudFormationClientFactory.getClient(cfnClientRequest.build());
    }
}
