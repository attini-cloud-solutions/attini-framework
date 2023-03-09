package attini.action.actions.deploycloudformation;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import attini.action.actions.deploycloudformation.input.AttiniCfnInput;
import attini.action.actions.deploycloudformation.input.CfnConfig;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.deploycloudformation.stackconfig.StackConfigurationService;
import attini.action.domain.CfnStackConfig;
import attini.action.domain.ConfigurationPropertyValue;
import attini.action.domain.DesiredState;
import attini.action.system.EnvironmentVariables;
import attini.domain.DeployOriginData;

public class CfnHandler {

    private static final Logger logger = Logger.getLogger(CfnHandler.class);


    private final DeployCfnService deployCfnService;
    private final DeployCfnCrossRegionService deployCfnCrossRegionService;
    private final EnvironmentVariables environmentVariables;
    private final CfnStackFacade cfnStackFacade;
    private final StackConfigurationService stackConfigurationService;

    public CfnHandler(DeployCfnService deployCfnService,
                      DeployCfnCrossRegionService deployCfnCrossRegionService,
                      EnvironmentVariables environmentVariables,
                      CfnStackFacade cfnStackFacade,
                      StackConfigurationService stackConfigurationService) {
        this.deployCfnService = requireNonNull(deployCfnService, "deployCfnService");
        this.deployCfnCrossRegionService = requireNonNull(deployCfnCrossRegionService, "deployCfnCrossRegionService");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.cfnStackFacade = requireNonNull(cfnStackFacade, "cfnStackFacade");
        this.stackConfigurationService = requireNonNull(stackConfigurationService, "stackConfigurationService");
    }

    public void deployCfn(AttiniCfnInput attiniCfnInput) {

        StackData stackData = StackData.builder()
                                       .setDistributionId(attiniCfnInput.deploymentOriginData()
                                                                        .getDistributionId())
                                       .setEnvironment(attiniCfnInput.deploymentOriginData()
                                                                     .getEnvironment())
                                       .setObjectIdentifier(attiniCfnInput.deploymentOriginData()
                                                                          .getObjectIdentifier())
                                       .setDistributionName(attiniCfnInput.deploymentOriginData()
                                                                          .getDistributionName())
                                       .setStackConfiguration(stackConfigurationService.getStackConfig(
                                               createCfnStackConfig(attiniCfnInput)))
                                       .setDeploymentPlanExecutionMetadata(attiniCfnInput.deploymentPlanExecutionMetadata())
                                       .build();

        if (isDifferentRegion(stackData.getStackConfiguration()) || isDifferentAccount(stackData.getStackConfiguration())) {
            logger.info("Is cross region/account deployment. Will use polling pattern for deploying the stack");
            deployCfnCrossRegionService.deployWithPolling(stackData);
            return;
        }
        if (shouldDeleteStackWithoutNotificationArn(stackData)){
            logger.info("Is delete stack request for a stack without a notification arn. Will use polling pattern for deploying the stack");
            deployCfnCrossRegionService.deployWithPolling(stackData);
            return;
        }

        logger.info("Is same account/region deployment. Will use a callback pattern for deploying the stack");
        deployCfnService.deployStack(stackData);

    }

    private boolean shouldDeleteStackWithoutNotificationArn(StackData stackData) {
        return stackData.getStackConfiguration()
                        .getDesiredState() == DesiredState.DELETED && !cfnStackFacade.stackHasNotificationArn(stackData);
    }

    private boolean isDifferentRegion(StackConfiguration stackConfiguration) {
        return stackConfiguration.getRegion().isPresent() && !stackConfiguration.getRegion()
                                                                                .get()
                                                                                .equals(environmentVariables.getRegion());
    }

    private boolean isDifferentAccount(StackConfiguration stackConfiguration) {
        Optional<String> account = stackConfiguration.getExecutionRole()
                                                     .map(s -> s.split(":")[4]);
        return account.isPresent() && !account
                .get()
                .equals(environmentVariables.getAccountId());
    }


    private CfnStackConfig createCfnStackConfig(AttiniCfnInput attiniCfnInput) {

        DeployOriginData deployOriginData = attiniCfnInput.deploymentOriginData();
        CfnConfig cfnConfig = attiniCfnInput.cfnConfig();

        String s3Prefix = String.format("https://s3.%s.amazonaws.com/%s/%s",
                                        environmentVariables.getRegion(),
                                        deployOriginData.getDeploySource().getAttiniDeploySourceBucket(),
                                        deployOriginData.getDeploySource().getAttiniDeploySourcePrefix());

        CfnStackConfig.Builder builder = CfnStackConfig.builder()
                                                       .setDistributionName(deployOriginData.getDistributionName())
                                                       .setEnvironment(deployOriginData.getEnvironment())
                                                       .setDistributionId(deployOriginData.getDistributionId())
                                                       .setObjectIdentifier(deployOriginData.getObjectIdentifier())
                                                       .setInitStackName(deployOriginData.getStackName())
                                                       .setStackName(cfnConfig.stackName())
                                                       .setTemplate(cfnConfig.template())
                                                       .setTemplateUrlPrefix(s3Prefix)
                                                       .setStackRole(cfnConfig.stackRoleArn())
                                                       .setConfigPath(cfnConfig.configFile())
                                                       .setRegion(cfnConfig.region())
                                                       .setExecutionRole(cfnConfig.executionRoleArn())
                                                       .setOutputPath(cfnConfig.outputPath())
                                                       .setAction(cfnConfig.action())
                                                       .setOnFailure(cfnConfig.onFailure())
                                                       .setEnableTerminationProtection(cfnConfig.enableTerminationProtection())
                                                       .setVariables(cfnConfig.variables());


        if (cfnConfig.parameters() != null) {
            logger.info("Setting parameters to cfn config");
            Map<String, ConfigurationPropertyValue> parameters = cfnConfig.parameters().entrySet()
                                                                          .stream()
                                                                          .collect(toMap(Map.Entry::getKey,
                                                                                         entry -> ConfigurationPropertyValue
                                                                                                 .create(entry.getValue(),
                                                                                                         false)));
            builder.setParameters(parameters);
        }

        return builder.build();
    }


}
