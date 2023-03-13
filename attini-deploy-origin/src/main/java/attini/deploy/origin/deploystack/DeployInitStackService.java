package attini.deploy.origin.deploystack;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;

import attini.deploy.origin.config.InitDeployStackConfig;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionContext;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.AlreadyExistsException;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ParameterDeclaration;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

public class DeployInitStackService {
    private final static Logger logger = Logger.getLogger(DeployInitStackService.class);
    private static final Set<String> CAPABILITIES = Set.of("CAPABILITY_AUTO_EXPAND",
                                                           "CAPABILITY_NAMED_IAM",
                                                           "CAPABILITY_IAM");
    private static final String ATTINI_DISTRIBUTION_NAME_KEY = "AttiniDistributionName";
    private static final String ATTINI_ENVIRONMENT_NAME_KEY = "AttiniEnvironmentName";
    private static final String ATTINI_DISTRIBUTION_ID_KEY = "AttiniDistributionId";
    private static final String ATTINI_RANDOM_STRING_KEY = "AttiniRandomString";
    private static final String ATTINI_RESOURCE_TYPE = "AttiniResourceType";
    private final CloudFormationClient cloudFormationClient;
    private final EnvironmentVariables environmentVariables;


    public DeployInitStackService(CloudFormationClient cloudFormationClient,
                                  EnvironmentVariables environmentVariables) {
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public void deployInitStack(DistributionContext distributionContext,
                                InitDeployStackConfig attiniInitDeployStackConfig,
                                String deployPrefix,
                                Map<String, String> distributionTags) {


        String templateURL = createTemplateUrl(deployPrefix, attiniInitDeployStackConfig);

        logger.info(templateURL);

        Set<Tag> tags = createTags(distributionContext, distributionTags, attiniInitDeployStackConfig);

        Set<Parameter> parameters = createParameters(distributionContext, attiniInitDeployStackConfig, templateURL);

        deployCfnStack(attiniInitDeployStackConfig.getInitDeployStackName(),
                       templateURL,
                       tags,
                       parameters, distributionContext.getObjectIdentifier().asString());

    }

    private static Set<Tag> createTags(DistributionContext distributionContext, Map<String,String> tags,
                                       InitDeployStackConfig initDeployStackConfig) {


        Map<String, String> finalTags = new HashMap<>(tags);
        finalTags.putAll(initDeployStackConfig.getTags(distributionContext.getEnvironment()));



        finalTags.put(ATTINI_DISTRIBUTION_NAME_KEY, distributionContext.getDistributionName().asString());

        finalTags.put(ATTINI_ENVIRONMENT_NAME_KEY, distributionContext.getEnvironment().asString());

        finalTags.put(ATTINI_RESOURCE_TYPE, "init-deploy");

        return finalTags.entrySet()
                        .stream()
                        .map(stringStringEntry -> Tag.builder()
                                                     .key(stringStringEntry.getKey())
                                                     .value(stringStringEntry.getValue())
                                                     .build())
                        .collect(toSet());

    }

    private void deployCfnStack(String stackName,
                                String templateUrl,
                                Set<Tag> tags,
                                Set<Parameter> parameters,
                                String objectIdentifier) {
        logger.info("Deploying init stack");
        try {
            createCfnStack(stackName, templateUrl, tags, parameters, objectIdentifier);
            logger.info("Created init stack");
        } catch (AlreadyExistsException e) {
            logger.info("init stack exists, updating");
            updateCfnStack(stackName, templateUrl, tags, parameters, objectIdentifier);
        } catch (CloudFormationException e) {
            logger.error(e.awsErrorDetails().errorCode());
            logger.error(e.awsErrorDetails().errorMessage());
            throw new DeployInitStackException(e);
        }
    }


    private void reCreateCfnStack(String stackName,
                                  String templateUrl,
                                  Set<Tag> tags,
                                  Set<Parameter> parameters,
                                  String objectIdentifier) {
        logger.info(String.format(
                "Cloudformation stack %s already exists but is in status ROLLBACK_COMPLETE or ROLLBACK_FAILED so it will be re created",
                stackName));
        DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder()
                                                                  .stackName(stackName)
                                                                  .clientRequestToken("recreate-call-" + UUID.randomUUID())
                                                                  .build();
        cloudFormationClient.deleteStack(deleteStackRequest);

        for (int i = 1; i < 21; ++i) {
            waitBeforeRetry();
            try {
                logger.info("Trying to re-create the stack");
                createCfnStack(stackName, templateUrl, tags, parameters, objectIdentifier);
                break;
            } catch (CloudFormationException e) {
                if (e.awsErrorDetails().errorMessage().contains("ROLLBACK_COMPLETE ") ||
                    e.awsErrorDetails().errorMessage().contains("DELETE_IN_PROGRESS")) {
                    logger.info(String.format(
                            "Tried to recreate the stack after %s seconds but the deletion was not finished",
                            i));
                }
            }
        }
    }

    private void createCfnStack(String stackName,
                                String templateUrl,
                                Set<Tag> tags,
                                Set<Parameter> parameters,
                                String objectIdentifier) {
        try {
            logger.info(String.format("Creating cloudformation stack: %s", stackName));
            CreateStackRequest createStackRequest = CreateStackRequest.builder()
                                                                      .capabilitiesWithStrings(CAPABILITIES)
                                                                      .clientRequestToken(DigestUtils.md5Hex(
                                                                              objectIdentifier).toUpperCase())
                                                                      .notificationARNs(environmentVariables.getInitStackNotificationArn())
                                                                      .stackName(stackName)
                                                                      .templateURL(templateUrl)
                                                                      .parameters(parameters)
                                                                      .tags(tags)
                                                                      .build();
            cloudFormationClient.createStack(createStackRequest);
        } catch (AlreadyExistsException e) {
            logger.info("Stack already exists");
            throw e;
        } catch (CloudFormationException e) {
            logger.error("Could not create cloudformation stack", e);
            throw new DeployInitStackException(e);
        }

    }


    private void updateCfnStack(String stackName,
                                String templateUrl,
                                Set<Tag> tags,
                                Set<Parameter> parameters,
                                String objectIdentifier) {
        logger.info(String.format("Updating cloudformation stack: %s", stackName));
        try {


            UpdateStackRequest updateStackRequest = UpdateStackRequest.builder()
                                                                      .capabilitiesWithStrings(CAPABILITIES)
                                                                      .notificationARNs(environmentVariables.getInitStackNotificationArn())
                                                                      .stackName(stackName)
                                                                      .clientRequestToken(DigestUtils.md5Hex(
                                                                              objectIdentifier).toUpperCase())
                                                                      .templateURL(templateUrl)
                                                                      .parameters(parameters)
                                                                      .tags(tags)
                                                                      .build();
            cloudFormationClient.updateStack(updateStackRequest);

        } catch (CloudFormationException e) {
            if (e.awsErrorDetails().errorMessage().contentEquals("No updates are to be performed.")) {
                logger.info(String.format("No updates are to be performed to stack %s", stackName));
            } else if (e.awsErrorDetails()
                        .errorMessage()
                        .endsWith("is in ROLLBACK_COMPLETE state and can not be updated.")) {

                reCreateCfnStack(stackName, templateUrl, tags, parameters, objectIdentifier);
                logger.info("Recreated init stack");
            } else {
                logger.error(e.awsErrorDetails().errorCode());
                logger.error(e.awsErrorDetails().errorMessage());
                throw new DeployInitStackException(e);
            }
        }
    }

    private GetTemplateSummaryResponse getTemplateSummaryResult(String templateUrl) {
        try {
            GetTemplateSummaryRequest getTemplateSummaryRequest = GetTemplateSummaryRequest.builder()
                                                                                           .templateURL(templateUrl)
                                                                                           .build();
            return cloudFormationClient.getTemplateSummary(getTemplateSummaryRequest);
        } catch (AwsServiceException e) {
            logger.error(e.awsErrorDetails().errorCode());
            logger.error(e.awsErrorDetails().errorMessage());
            throw new DeployInitStackException(e);
        }
    }


    public Set<Parameter> createParameters(DistributionContext distributionContext, InitDeployStackConfig attiniInitDeployStackConfig, String templateUrl) {



        HashMap<String, String> finalParameters = new HashMap<>(attiniInitDeployStackConfig.getParameters());


        finalParameters.put(ATTINI_DISTRIBUTION_NAME_KEY, distributionContext.getDistributionName().asString());
        finalParameters.put(environmentVariables.getEnvironmentParameterName(), distributionContext.getEnvironment().asString());
        finalParameters.put(ATTINI_DISTRIBUTION_ID_KEY, distributionContext.getDistributionId().asString());
        finalParameters.put(ATTINI_RANDOM_STRING_KEY, UUID.randomUUID().toString());


        List<String> cfnParametersKeys = getTemplateSummaryResult(templateUrl).parameters()
                                                                              .stream()
                                                                              .map(ParameterDeclaration::parameterKey)
                                                                              .collect(toList());


        return finalParameters.entrySet()
                              .stream()
                              .filter(isPresentInList(cfnParametersKeys))
                              .map(entry -> Parameter.builder()
                                                     .parameterKey(entry.getKey())
                                                     .parameterValue(entry.getValue())
                                                     .build())
                              .collect(toSet());


    }


    private static Predicate<Map.Entry<String, String>> isPresentInList(List<String> cfnParametersKeys) {
        return entry -> {
            boolean isPresent = cfnParametersKeys.contains(entry.getKey());
            if (!isPresent) {
                logger.info(String.format("Removing parameter [%s] because its missing in template", entry.getKey()));
            }
            return isPresent;
        };
    }

    private String createTemplateUrl(String deployPrefix, InitDeployStackConfig initDeployStackConfig) {
        return String.format("https://s3.%s.amazonaws.com/%s/%s/%s",
                             environmentVariables.getAwsRegion(),
                             environmentVariables.getArtifactBucket(),
                             deployPrefix,
                             initDeployStackConfig.getAttiniInitDeployTemplatePath());
    }

    private static void waitBeforeRetry() {
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

