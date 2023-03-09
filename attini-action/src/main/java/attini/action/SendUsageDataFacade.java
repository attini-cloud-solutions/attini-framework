package attini.action;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.facades.deployorigin.DeployOriginFacade;
import attini.action.facades.stackdata.DeploymentPlanDataFacade;
import attini.action.facades.stepfunction.ExecutionSummery;
import attini.action.facades.stepfunction.StepFunctionFacade;
import attini.action.system.EnvironmentVariables;
import attini.domain.DeployOriginData;
import software.amazon.awssdk.services.organizations.OrganizationsClient;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sts.StsAsyncClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Tag;

public class SendUsageDataFacade {

    private static final String DISTRIBUTION_NAME = "distributionName";
    private static final String ENVIRONMENT = "environment";
    private static final String DEPLOY_TYPE = "deployType";
    private static final String EXECUTION_ARN = "executionArn";
    private static final String AWS_ORG = "awsOrg";
    private static final String ATTINI_VERSION = "attiniVersion";
    private static final String DEPLOYMENT_PLAN_STEPS_COUNT = "deploymentPlanStepsCount";
    private static final String REGION = "region";
    private static final String EMAIL = "email";
    private static final String LICENCE_TOKEN = "licenceToken";
    private static final String ACCEPTED_LICENCE_AGREEMENT = "acceptedLicenceAgreement";
    private static final String DEPLOYMENT_PLAN_STATUS = "status";

    private final StsAsyncClient stsClient;
    private final OrganizationsClient organizationsClient;
    private final EnvironmentVariables environmentVariables;
    private final StepFunctionFacade stepFunctionFacade;
    private final SnsAsyncClient snsClient;
    private final DeploymentPlanDataFacade deploymentPlanDataFacade;
    private final DeployOriginFacade deployOriginFacade;
    private final ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(SendUsageDataFacade.class);

    private String orgId;

    public SendUsageDataFacade(StsAsyncClient stsClient,
                               OrganizationsClient organizationsClient,
                               EnvironmentVariables environmentVariables,
                               StepFunctionFacade stepFunctionFacade,
                               SnsAsyncClient snsClient,
                               DeploymentPlanDataFacade deploymentPlanDataFacade,
                               DeployOriginFacade deployOriginFacade,
                               ObjectMapper objectMapper) {
        this.stsClient = requireNonNull(stsClient, "stsClient");
        this.organizationsClient = requireNonNull(organizationsClient, "organizationsClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "stepFunctionFacade");
        this.snsClient = requireNonNull(snsClient, "snsClient");
        this.deploymentPlanDataFacade = requireNonNull(deploymentPlanDataFacade, "deploymentPlanDataFacade");
        this.deployOriginFacade = deployOriginFacade;
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public void sendStartUsage(DeployOriginData deployOriginData, String executionArn) {
        logger.info("Starting to send user data");

        CompletableFuture<AssumeRoleResponse> assumeRoleFeature = stsClient.assumeRole(
                AssumeRoleRequest.builder()
                                 .roleArn("arn:aws:iam::338009130405:role/attini-usage-statistics")
                                 .roleSessionName("send-usage-statistics")
                                 .tags(toTag(DISTRIBUTION_NAME,
                                             deployOriginData.getDistributionName().asString()),
                                       toTag(ENVIRONMENT, deployOriginData.getEnvironment().asString()),
                                       toTag(DEPLOY_TYPE, "deployStart"),
                                       toTag(EXECUTION_ARN, executionArn),
                                       toTag(AWS_ORG, getOrgId()),
                                       toTag(REGION, environmentVariables.getRegion()),
                                       toTag(EMAIL,
                                             environmentVariables.getCompanyContactEmail()
                                                                 .orElse("not-specified")),
                                       toTag(ATTINI_VERSION, environmentVariables.getAttiniVersion()),
                                       toTag(LICENCE_TOKEN, environmentVariables.getLicenceToken()))
                                 .build());


        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("deploymentOriginData", deployOriginData);
        jsonObject.put(EXECUTION_ARN, executionArn);
        jsonObject.put("type", "DeploymentPlanStart");
        jsonObject.put("region", environmentVariables.getRegion());
        jsonObject.put("eventId", UUID.randomUUID().toString());
        jsonObject.put("distributionId", deployOriginData.getDistributionId().asString());
        jsonObject.put(DISTRIBUTION_NAME, deployOriginData.getDistributionName().asString());
        jsonObject.put(ENVIRONMENT, deployOriginData.getEnvironment().asString());
        jsonObject.put("status", "SUCCEEDED");
        CompletableFuture<PublishResponse> publishFeature =
                snsClient.publish(PublishRequest.builder()
                                                .topicArn(environmentVariables.getDeploymentStatusTopic())
                                                .messageAttributes(Map.of("status",
                                                                          MessageAttributeValue.builder()
                                                                                               .dataType("String")
                                                                                               .stringValue("SUCCEEDED")
                                                                                               .build(),
                                                                          "type",
                                                                          MessageAttributeValue.builder()
                                                                                               .dataType("String")
                                                                                               .stringValue(
                                                                                                       "DeploymentPlanStart")
                                                                                               .build(),
                                                                          "environment",
                                                                          MessageAttributeValue.builder()
                                                                                               .dataType("String")
                                                                                               .stringValue(
                                                                                                       deployOriginData.getEnvironment()
                                                                                                                       .asString())
                                                                                               .build(),
                                                                          "distributionName",
                                                                          MessageAttributeValue.builder()
                                                                                               .dataType("String")
                                                                                               .stringValue(
                                                                                                       deployOriginData.getDistributionName()
                                                                                                                       .asString())
                                                                                               .build()))
                                                .message(asJsonString(jsonObject))
                                                .build());

        waitForCompleted(assumeRoleFeature, publishFeature);

    }

    private String asJsonString(Map<String, Object> jsonObject) {
        try {
            return objectMapper.writeValueAsString(jsonObject);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void sendAcceptedLicenceAgreement(boolean acceptedLicenceAgreement) {
        logger.info("Starting to send licence agreement data");

        CompletableFuture<AssumeRoleResponse> acceptLicenceAgreementFeature =
                stsClient.assumeRole(AssumeRoleRequest.builder()
                                                      .roleArn(
                                                              "arn:aws:iam::338009130405:role/attini-usage-statistics")
                                                      .roleSessionName(
                                                              "send-usage-statistics")
                                                      .tags(toTag(
                                                                    DEPLOY_TYPE,
                                                                    "acceptedLicenceAgreement"),
                                                            toTag(REGION,
                                                                  environmentVariables.getRegion()),
                                                            toTag(ACCEPTED_LICENCE_AGREEMENT,
                                                                  acceptedLicenceAgreement ? "true" : "false"))
                                                      .build());

        waitForCompleted(acceptLicenceAgreementFeature);

    }

    public void sendEndUsage(String executionArn, String status, String sfnArn, Instant startTime) {
        logger.info("Sending end usage with execution arn: " + executionArn);

        ExecutionSummery executionSummery = stepFunctionFacade.getExecutionSummery(executionArn);

        logger.info("executionSummery: " + executionSummery);
        CompletableFuture<AssumeRoleResponse> assumeRoleFeature =
                stsClient.assumeRole(AssumeRoleRequest.builder()
                                                      .roleArn(
                                                              "arn:aws:iam::338009130405:role/attini-usage-statistics")
                                                      .roleSessionName(
                                                              "send-usage-statistics")
                                                      .tags(toTag(DISTRIBUTION_NAME,
                                                                  executionSummery.getDistributionName()),
                                                            toTag(ENVIRONMENT,
                                                                  executionSummery.getEnvironment()),
                                                            toTag(DEPLOY_TYPE,
                                                                  "deployEnd"),
                                                            toTag(EXECUTION_ARN,
                                                                  executionArn),
                                                            toTag(AWS_ORG,
                                                                  getOrgId()),
                                                            toTag(REGION,
                                                                  environmentVariables.getRegion()),
                                                            toTag(ATTINI_VERSION,
                                                                  environmentVariables.getAttiniVersion()),
                                                            toTag(EMAIL,
                                                                  environmentVariables.getCompanyContactEmail()
                                                                                      .orElse("not-specified")),
                                                            toTag(DEPLOYMENT_PLAN_STEPS_COUNT,
                                                                  String.valueOf(
                                                                          executionSummery.getNrOfSteps())),
                                                            toTag(LICENCE_TOKEN,
                                                                  environmentVariables.getLicenceToken()),
                                                            toTag(DEPLOYMENT_PLAN_STATUS,
                                                                  status))
                                                      .build());


        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put(EXECUTION_ARN, executionArn);
        jsonObject.put("type", "DeploymentPlanEnd");
        jsonObject.put("region", environmentVariables.getRegion());
        jsonObject.put(DEPLOYMENT_PLAN_STATUS, status);
        jsonObject.put(DEPLOYMENT_PLAN_STEPS_COUNT, String.valueOf(executionSummery.getNrOfSteps()));
        jsonObject.put(ENVIRONMENT, executionSummery.getEnvironment());
        jsonObject.put(DISTRIBUTION_NAME, executionSummery.getDistributionName());
        jsonObject.put("distributionId", executionSummery.getDistributionId());
        jsonObject.put("eventId", UUID.randomUUID().toString());


        CompletableFuture<PublishResponse> publishFeature =
                snsClient.publish(PublishRequest.builder()
                                                .topicArn(
                                                        environmentVariables.getDeploymentStatusTopic())
                                                .message(asJsonString(jsonObject))
                                                .messageAttributes(Map.of(
                                                        "status",
                                                        MessageAttributeValue.builder()
                                                                             .dataType(
                                                                                     "String")
                                                                             .stringValue(
                                                                                     status)
                                                                             .build(),
                                                        "type",
                                                        MessageAttributeValue.builder()
                                                                             .dataType(
                                                                                     "String")
                                                                             .stringValue(
                                                                                     "DeploymentPlanEnd")
                                                                             .build(),
                                                        "environment",
                                                        MessageAttributeValue.builder()
                                                                             .dataType(
                                                                                     "String")
                                                                             .stringValue(
                                                                                     executionSummery.getEnvironment())
                                                                             .build(),
                                                        "distributionName",
                                                        MessageAttributeValue.builder()
                                                                             .dataType(
                                                                                     "String")
                                                                             .stringValue(
                                                                                     executionSummery.getDistributionName())
                                                                             .build()))
                                                .build());

        waitForCompleted(assumeRoleFeature, publishFeature);

        deploymentPlanDataFacade.saveFinalStatus(sfnArn, status, startTime);
        deployOriginFacade.setDeploymentPlanStatus(executionSummery.getEnvironment() + "-" + executionSummery.getDistributionName(),
                                                   executionSummery.getObjectIdentifier(),
                                                   status);
    }

    private String getOrgId() {
        try {
            if (orgId == null) {
                orgId = organizationsClient.describeOrganization().organization().id();
            }
        } catch (Exception e) {
            logger.warn("Could not get org id", e);
        }

        return orgId == null ? "no-org-id-specified" : orgId;

    }


    private Tag toTag(String key, String value) {
        return Tag.builder()
                  .key(key)
                  .value(value)
                  .build();
    }

    private void waitForCompleted(CompletableFuture<?>... features) {
        try {
            CompletableFuture.allOf(features).get();
            logger.info("Completed publishing usage data");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Interrupted while waiting for usage data to be sent", e);
        }
    }
}
