/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static attini.action.domain.ConfigurationPropertyValue.Type.STRING;
import static java.util.Objects.requireNonNull;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;
import org.jboss.logging.Logger;

import attini.action.CloudFormationClientFactory;
import attini.action.CloudFormationClientFactory.GetCloudFormationClientRequest;
import attini.action.StackConfigException;
import attini.action.actions.deploycloudformation.CollectionsUtils;
import attini.action.domain.CfnStackConfig;
import attini.action.domain.ConfigurationPropertyValue;
import attini.action.domain.DesiredState;
import attini.action.domain.FileStackConfiguration;
import attini.action.domain.OnFailure;
import attini.action.facades.stackdata.InitStackDataFacade;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ParameterDeclaration;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;

public class StackConfigurationService {

    private static final Logger logger = Logger.getLogger(StackConfigurationService.class);


    private final StackConfigurationFileService stackConfigurationFileService;
    private final CloudFormationClientFactory cloudFormationClientFactory;
    private final EnvironmentVariables environmentVariables;
    private final SsmClient ssmClient;
    private final InitStackDataFacade initStackDataFacade;


    public StackConfigurationService(StackConfigurationFileService stackConfigurationFileService,
                                     CloudFormationClientFactory cloudFormationClientFactory,
                                     EnvironmentVariables environmentVariables,
                                     SsmClient ssmClient,
                                     InitStackDataFacade initStackDataFacade) {
        this.stackConfigurationFileService = requireNonNull(stackConfigurationFileService,
                                                            "stackConfigurationFileService");
        this.cloudFormationClientFactory = requireNonNull(cloudFormationClientFactory, "cloudFormationClientFactory");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.ssmClient = requireNonNull(ssmClient, "ssmClient");
        this.initStackDataFacade = requireNonNull(initStackDataFacade, "initStackDataFacade");
    }

    public StackConfiguration getStackConfig(CfnStackConfig cfnStackConfig) {
        HashMap<String, String> variables = new HashMap<>(initStackDataFacade.getInitConfigVariables(
                cfnStackConfig.getInitStackName()));
        variables.putAll(cfnStackConfig.getVariables());
        Optional<FileStackConfiguration> configurationOptional =
                cfnStackConfig.getConfigPath()
                              .map(s -> GetConfigFileRequest.builder().setConfigPath(s)
                                                            .setDistributionId(cfnStackConfig.getDistributionId())
                                                            .setDistributionName(cfnStackConfig.getDistributionName())
                                                            .setEnvironment(cfnStackConfig.getEnvironment())
                                                            .setVariables(variables).build())
                              .map(stackConfigurationFileService::getConfiguration);


        configurationOptional.ifPresentOrElse(fileStackConfiguration -> logger.info("got configuration =" + fileStackConfiguration),
                                              () -> logger.info("no file config found"));

        if (cfnStackConfig.getTemplateUrl()
                          .isEmpty() && configurationOptional.map(FileStackConfiguration::getTemplatePath)
                                                             .isEmpty()) {
            throw new StackConfigException("no template path is specified");
        }

        if (cfnStackConfig.getStackName()
                          .isEmpty() && configurationOptional.map(FileStackConfiguration::getStackName)
                                                             .isEmpty()) {
            throw new StackConfigException("no stack name is specified");
        }
        StringSubstitutor stringSubstitutor = new StringSubstitutor(variables);


        String templatePath = cfnStackConfig.getTemplateUrl()
                                            .orElseGet(() -> configurationOptional.get().getTemplatePath());
        String templateUrl = TemplatePathUtil.getTemplatePath(stringSubstitutor.replace(templatePath),
                                                              cfnStackConfig.getTemplateUrlPrefix());

        String stackName = stringSubstitutor.replace(cfnStackConfig.getStackName()
                                                                   .orElseGet(() -> configurationOptional.get()
                                                                                                         .getStackName()));

        Optional<String> region = cfnStackConfig.getRegion()
                                                .or(() -> configurationOptional.flatMap(FileStackConfiguration::getRegion))
                                                .map(stringSubstitutor::replace);

        Optional<String> executionRole = cfnStackConfig.getExecutionRole()
                                                       .or(() -> configurationOptional.flatMap(FileStackConfiguration::getExecutionRole))
                                                       .map(stringSubstitutor::replace);

        Optional<String> stackRole = cfnStackConfig.getStackRole()
                                                   .or(() -> configurationOptional.flatMap(FileStackConfiguration::getStackRoleArn))
                                                   .map(stringSubstitutor::replace);

        Optional<String> outputPath = cfnStackConfig.getOutputPath()
                                                    .or(() -> configurationOptional.flatMap(FileStackConfiguration::getOutputPath))
                                                    .map(stringSubstitutor::replace);

        Optional<OnFailure> onFailure = cfnStackConfig.getOnFailure()
                                                      .or(() -> configurationOptional.flatMap(FileStackConfiguration::getOnFailure))
                                                      .map(stringSubstitutor::replace)
                                                      .map(StackConfigurationService::toOnFailure);


        List<Parameter> attiniFrameworkParameters = getAttiniFrameworkParameters(cfnStackConfig);

        GetCloudFormationClientRequest.Builder cfnClientRequest = GetCloudFormationClientRequest.builder();
        region.ifPresent(cfnClientRequest::setRegion);
        executionRole.ifPresent(cfnClientRequest::setExecutionRoleArn);

        CloudFormationClient client = cloudFormationClientFactory.getClient(cfnClientRequest.build());


        Optional<Stack> stackDescription = getStackDescription(stackName, client);


        Optional<Boolean> enableTerminationProtection =
                cfnStackConfig.getEnableTerminationProtection()
                              .or(() -> configurationOptional.flatMap(
                                      FileStackConfiguration::getEnableTerminationProtection))
                              .map(stringSubstitutor::replace)
                              .map(s -> {
                                  if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                                      return Boolean.parseBoolean(s);
                                  }
                                  throw new StackConfigException(
                                          "Invalid value for property \"enableTerminationProtection\", current value = " + s + ", allowed values are \"true\" or \"false\"");
                              }).map(aBoolean -> {
                                  if (!stackDescription.map(Stack::enableTerminationProtection).orElse(false).equals(aBoolean)) {
                                      return aBoolean;
                                  }
                                  return null;
                              }).or(() -> {
                                  if (stackDescription.map(Stack::enableTerminationProtection).orElse(false).equals(true)) {
                                      return Optional.of(false);
                                  }
                                  return Optional.empty();
                              });

        Map<String, String> existingParameters = stackDescription.map(stack -> stack.parameters()
                                                                                    .stream()
                                                                                    .collect(Collectors.toMap(Parameter::parameterKey,
                                                                                                              Parameter::parameterValue)))
                                                                 .orElse(Collections.emptyMap());
        Set<Parameter> parameters = configurationOptional
                .map(configuration -> combineMaps(cfnStackConfig.getParameters(), configuration.getParameters()))
                .map(map -> replaceFallBackValues(existingParameters, map))
                .map((Map<String, ConfigurationPropertyValue> parameters2) -> replaceParamStoreConfig(parameters2,
                                                                                                      stringSubstitutor))
                .map(this::createParametersList)
                .map(parameters1 -> CollectionsUtils.combineCollections(parameters1, attiniFrameworkParameters))
                .map(parameters1 -> filterNeededParams(templateUrl, parameters1, attiniFrameworkParameters, client))
                .orElseGet(() -> filterNeededParams(templateUrl,
                                                    CollectionsUtils.combineCollections(createParametersList(cfnStackConfig.getParameters()),
                                                                                        getAttiniFrameworkParameters(cfnStackConfig)),
                                                    attiniFrameworkParameters, client))
                .stream()
                .map(parameter -> parameter.toBuilder()
                                           .parameterValue(stringSubstitutor.replace(parameter.parameterValue()))
                                           .build())
                .collect(Collectors.toSet());


        Map<String, String> existingTags = stackDescription.map(stack -> stack.tags()
                                                                              .stream()
                                                                              .collect(Collectors.toMap(Tag::key,
                                                                                                        Tag::value)))
                                                           .orElse(Collections.emptyMap());

        Set<Tag> tags = configurationOptional
                .map(stackConfiguration -> combineMaps(cfnStackConfig.getTags(), stackConfiguration.getTags()))
                .map(map -> replaceFallBackValues(existingTags, map))
                .map((Map<String, ConfigurationPropertyValue> parameters1) -> replaceParamStoreConfig(parameters1,
                                                                                                      stringSubstitutor))
                .map(this::createTagsList)
                .orElse(createTagsList(cfnStackConfig.getTags()))
                .stream()
                .map(tag -> tag.toBuilder().value(stringSubstitutor.replace(tag.value())).build())
                .collect(Collectors.toSet());

        String action = stringSubstitutor.replace(cfnStackConfig.getAction()
                                                                .orElseGet(() -> configurationOptional.flatMap(
                                                                                                              FileStackConfiguration::getAction)
                                                                                                      .orElse("Deploy")));
        StackConfiguration.Builder builder = StackConfiguration.builder()
                                                               .setStackName(stackName)
                                                               .setParameters(parameters)
                                                               .setTags(tags)
                                                               .setTemplate(stringSubstitutor.replace(templateUrl))
                                                               .setDesiredState(toDesiredState(action));

        onFailure.ifPresent(builder::setOnFailure);
        enableTerminationProtection.ifPresent(builder::setEnableTerminationProtection);
        region.ifPresent(builder::setRegion);
        executionRole.ifPresent(builder::setExecutionRole);
        stackRole.ifPresent(builder::setStackRole);
        outputPath.map(stringSubstitutor::replace).ifPresent(builder::setOutputPath);

        StackConfiguration stackConfiguration = builder.build();
        logger.info("Final stack config =" + stackConfiguration);
        return stackConfiguration;

    }

    private static DesiredState toDesiredState(String action) {
        return switch (action.trim()) {
            case "Delete" -> DesiredState.DELETED;
            case "Deploy" -> DesiredState.DEPLOYED;
            default -> throw new StackConfigException("Invalid value for property \"Action\", current value = " + action + ", allowed values are \"Deploy\" or \"Delete\"");
        };
    }

    private static OnFailure toOnFailure(String onFailure) {
        return switch (onFailure.trim()) {
            case "DELETE" -> OnFailure.DELETE;
            case "DO_NOTHING" -> OnFailure.DO_NOTHING;
            case "ROLLBACK" -> OnFailure.ROLLBACK;
            default -> throw new StackConfigException("Invalid value for property \"onFailure\", current value = " + onFailure + ", allowed values are \"DELETE\", \"ROLLBACK\" or \"DO_NOTHING\"");
        };
    }

    public static <V> Map<String, V> combineMaps(Map<String, V> priorityMap, Map<String, V> secondaryMap) {

        Map<String, V> finalMap = new HashMap<>();

        finalMap.putAll(secondaryMap);
        finalMap.putAll(priorityMap);
        return finalMap;
    }


    private List<Tag> createTagsList(Map<String, ConfigurationPropertyValue> tags) {
        return tags.entrySet()
                   .stream()
                   .map(entry -> Tag.builder()
                                    .key(entry.getKey())
                                    .value(entry.getValue().getValue())
                                    .build()).collect(Collectors.toList());
    }


    private Map<String, ConfigurationPropertyValue> replaceParamStoreConfig(Map<String, ConfigurationPropertyValue> parameters,
                                                                            StringSubstitutor stringSubstitutor) {
        return parameters.entrySet()
                         .stream()
                         .map(entry -> {
                             ConfigurationPropertyValue configProperty = entry.getValue();
                             if (configProperty.getType() == ConfigurationPropertyValue.Type.SSM_PARAMETER) {
                                 String paramValue = getParameter(configProperty, stringSubstitutor);
                                 return new AbstractMap.SimpleEntry<>(entry.getKey(),
                                                                      ConfigurationPropertyValue.create(paramValue,
                                                                                                        configProperty.isFallback()));
                             }
                             return entry;
                         }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getParameter(ConfigurationPropertyValue configProperty, StringSubstitutor stringSubstitutor) {
        try {
            return ssmClient.getParameter(GetParameterRequest.builder()
                                                             .name(stringSubstitutor.replace(configProperty.getValue()))
                                                             .build())
                            .parameter()
                            .value();
        } catch (ParameterNotFoundException e) {
            logger.info("No parameter for value = " + configProperty.getValue() + ", using default");
            return configProperty.getDefaultValue()
                                 .orElseThrow(() -> new StackConfigException(
                                         "no parameter found in parameterStore for value = " + configProperty.getValue() + " and no default specified"));
        }
    }

    private Map<String, ConfigurationPropertyValue> replaceFallBackValues(Map<String, String> existingValues,
                                                                          Map<String, ConfigurationPropertyValue> config) {

        return config.entrySet().stream().map(entry -> {
            if (entry.getValue().isFallback() && existingValues.containsKey(entry.getKey())) {
                ConfigurationPropertyValue configurationProperty = entry.getValue();
                return new AbstractMap.SimpleEntry<>(entry.getKey(),
                                                     ConfigurationPropertyValue.create(existingValues.get(entry.getKey()),
                                                                                       configurationProperty.isFallback(),
                                                                                       STRING,
                                                                                       configurationProperty.getDefaultValue()
                                                                                                            .orElse(null)));
            }
            return entry;
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    private List<Parameter> createParametersList(Map<String, ConfigurationPropertyValue> parameters) {
        return parameters.entrySet()
                         .stream()
                         .map(entry -> Parameter.builder()
                                                .parameterKey(entry.getKey())
                                                .parameterValue(entry.getValue()
                                                                     .getValue())
                                                .build()).collect(Collectors.toList());
    }

    private Optional<Stack> getStackDescription(String stackName, CloudFormationClient client) {
        try {
            return Optional.of(client.describeStacks(DescribeStacksRequest.builder()
                                                                          .stackName(stackName)
                                                                          .build())
                                     .stacks()
                                     .get(0));
        } catch (CloudFormationException e) {
            logger.info("No cloudformation stack with name: " + stackName + " exists");
            return Optional.empty();
        }
    }


    private List<Parameter> filterNeededParams(String templateUrl,
                                               List<Parameter> parameters,
                                               List<Parameter> attiniFrameworkParams,
                                               CloudFormationClient cloudFormationClient) {
        try {
            logger.debug(String.format("Getting template summary for: %s", templateUrl));
            GetTemplateSummaryRequest getTemplateSummaryRequest = GetTemplateSummaryRequest.builder()
                                                                                           .templateURL(templateUrl)
                                                                                           .build();
            Set<String> neededParams = cloudFormationClient.getTemplateSummary(getTemplateSummaryRequest).parameters()
                                                           .stream()
                                                           .map(ParameterDeclaration::parameterKey)
                                                           .collect(Collectors.toSet());
            return ParametersUtil.removeUnusedParams(neededParams, parameters, attiniFrameworkParams);

        } catch (AwsServiceException e) {
            if (e.getMessage().contains("Access Denied")){
                throw new StackConfigException("Could not get template for url=" + templateUrl + ". This could be because of lacking permissions or because the file is missing", e);

            }
            throw new StackConfigException("Template at url=" + templateUrl + " is invalid, Error message=" + e.getMessage(), e);

        }
    }

    private List<Parameter> getAttiniFrameworkParameters(CfnStackConfig cfnStackConfig) {
        return List.of(
                ParametersUtil.toParameter("AttiniDistributionName", cfnStackConfig.getDistributionName().asString()),
                ParametersUtil.toParameter(environmentVariables.getEnvironmentParameterName(),
                                           cfnStackConfig.getEnvironment().asString()),
                ParametersUtil.toParameter("AttiniDistributionId", cfnStackConfig.getDistributionId().asString()),
                ParametersUtil.toParameter("AttiniRandomString", UUID.randomUUID().toString()));
    }
}
