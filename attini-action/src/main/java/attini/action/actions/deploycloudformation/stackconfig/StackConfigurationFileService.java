/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static attini.action.domain.ConfigurationPropertyValue.Type.STRING;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import attini.action.StackConfigException;
import attini.action.domain.ConfigurationPropertyValue;
import attini.action.domain.FileStackConfiguration;
import attini.action.facades.S3Facade;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.sts.StsClient;

public class StackConfigurationFileService {

    private static final Logger logger = Logger.getLogger(StackConfigurationFileService.class);
    private static final String FALLBACK_PROPERTY = "fallback";
    private static final String CONFIG_TYPE_PROPERTY = "type";
    private static final String VALUE_PROPERTY = "value";
    private static final String PARAMETERS_PROPERTY = "parameters";
    private static final String DEFAULT_VALUE_PROPERTY = "default";
    private static final String TAGS_PROPERTY = "tags";
    private static final String STACK_ROLE_ARN_PROPERTY = "stackRoleArn";
    private static final String STACK_NAME_PROPERTY = "stackName";
    private static final String REGION_PROPERTY = "region";
    private static final String EXECUTION_ROLE_PROPERTY = "executionRoleArn";
    private static final String OUTPUT_PATH_PROPERTY = "outputPath";
    private static final String ACTION = "action";
    private static final String ON_FAILURE = "onFailure";
    private static final String ENABLE_TERMINATION_PROTECTION = "enableTerminationProtection";


    private static final String TEMPLATE_PATH_PROPERTY = "template";
    public static final String EXTENDS_PROPERTY = "extends";
    private final StsClient stsClient;
    private final S3Facade s3Facade;
    private final EnvironmentVariables environmentVariables;


    public StackConfigurationFileService(StsClient stsClient,
                                         S3Facade s3Facade,
                                         EnvironmentVariables environmentVariables) {
        this.stsClient = requireNonNull(stsClient, "stsClient");
        this.s3Facade = requireNonNull(s3Facade, "s3Facade");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public FileStackConfiguration getConfiguration(GetConfigFileRequest request) {

        Set<String> configExtensions = new LinkedHashSet<>();

        Map<String, Object> map = getConfigFile(request, configExtensions);

        Map<String, ConfigurationPropertyValue> tagsConfig = getTags(map)
                .entrySet()
                .stream()
                .map(toConfigProperty())
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          Map.Entry::getValue));

        Map<String, ConfigurationPropertyValue> parametersConfig = getParameters(map)
                .entrySet()
                .stream()
                .map(toConfigProperty())
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          Map.Entry::getValue));


        return FileStackConfiguration.builder()
                                     .setParameters(parametersConfig)
                                     .setTags(tagsConfig)
                                     .setStackName((String) map.get(STACK_NAME_PROPERTY))
                                     .setTemplatePath((String) map.get(TEMPLATE_PATH_PROPERTY))
                                     .setRegion((String) map.get(REGION_PROPERTY))
                                     .setExecutionRole((String) map.get(EXECUTION_ROLE_PROPERTY))
                                     .setStackRoleArn((String) map.get(STACK_ROLE_ARN_PROPERTY))
                                     .setOutputPath((String) map.get(OUTPUT_PATH_PROPERTY))
                                     .setAction((String) map.get(ACTION))
                                     .setOnFailure((String) map.get(ON_FAILURE))
                                     .setEnableTerminationProtection(map.get(ENABLE_TERMINATION_PROTECTION) == null ? null : String.valueOf(
                                             map.get(ENABLE_TERMINATION_PROTECTION)))
                                     .build();
    }

    private Map<String, Object> getConfigFile(GetConfigFileRequest request, Set<String> configExtensions) {
        StringSubstitutor stringSubstitutor = new StringSubstitutor(request.getVariables());


        String configFile = stringSubstitutor.replace(request.getConfigPath());

        configExtensions.add(configFile);

        if (!configFile.startsWith("/")) {
            throw new StackConfigException(
                    "Relative file paths for stack configs are not supported at this time, config file=" + configFile);
        }


        try {
            String key = S3PathUtil.resolvePath(request.getEnvironment().asString() + "/"
                                                + request.getDistributionName().asString() + "/"
                                                + request.getDistributionId().asString()
                                                + "/distribution-origin"
                                                + configFile);

            logger.info("Getting config: " + key);
            String bucketName = String.format("attini-artifact-store-%s-%s",
                                              environmentVariables.getRegion(),
                                              stsClient.getCallerIdentity()
                                                       .account());

            ObjectMapper objectMapper = getObjectMapper(configFile);

            byte[] bytes = getConfigFile(configFile, key, bucketName);

            Map<String, Object> configMap = objectMapper.readValue(bytes, new TypeReference<>() {
            });

            if (configMap.containsKey(EXTENDS_PROPERTY)) {
                if (!(configMap.get(EXTENDS_PROPERTY) instanceof String)) {
                    throw new StackConfigException("Error in config file " + configFile + ", extends has to be a string");
                }


                String parentPath = stringSubstitutor.replace((String) configMap.get(EXTENDS_PROPERTY));

                if (configExtensions.contains(parentPath)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    configExtensions.forEach(s -> stringBuilder.append(s).append(" -> "));
                    stringBuilder.append(parentPath);

                    throw new StackConfigException("Recursive config file extension detected, config file = "
                                                   + parentPath +
                                                   " extends a configuration that has already been inherited. Extensions = " + stringBuilder);
                }

                GetConfigFileRequest getParentRequest = GetConfigFileRequest.builder()
                                                                            .of(request)
                                                                            .setConfigPath(parentPath)
                                                                            .build();

                Map<String, Object> parentConfigMap = getConfigFile(getParentRequest, configExtensions);

                Map<String, Object> tags = getTags(parentConfigMap);
                tags.putAll(getTags(configMap));
                Map<String, Object> parameters = getParameters(parentConfigMap);
                parameters.putAll(getParameters(configMap));

                Object templatePath = configMap.containsKey(TEMPLATE_PATH_PROPERTY) ? configMap.get(
                        TEMPLATE_PATH_PROPERTY) : parentConfigMap
                                              .get(TEMPLATE_PATH_PROPERTY);

                Object stackName = configMap.containsKey(STACK_NAME_PROPERTY) ? configMap.get(STACK_NAME_PROPERTY) : parentConfigMap
                        .get(STACK_NAME_PROPERTY);

                Object region = configMap.containsKey(REGION_PROPERTY) ? configMap.get(REGION_PROPERTY) : parentConfigMap
                        .get(REGION_PROPERTY);

                Object executionRole = configMap.containsKey(EXECUTION_ROLE_PROPERTY) ? configMap.get(
                        EXECUTION_ROLE_PROPERTY) : parentConfigMap
                                               .get(EXECUTION_ROLE_PROPERTY);


                Object outputPath = configMap.containsKey(OUTPUT_PATH_PROPERTY) ? configMap.get(
                        OUTPUT_PATH_PROPERTY) : parentConfigMap
                                            .get(OUTPUT_PATH_PROPERTY);

                Object action = configMap.containsKey(ACTION) ? configMap.get(
                        ACTION) : parentConfigMap
                                        .get(ACTION);
                Object onFailure = configMap.containsKey(ON_FAILURE) ? configMap.get(
                        ON_FAILURE) : parentConfigMap
                                           .get(ON_FAILURE);

                Object enableTerminationProtection = configMap.containsKey(ENABLE_TERMINATION_PROTECTION) ? configMap.get(
                        ENABLE_TERMINATION_PROTECTION) : parentConfigMap
                                                             .get(ENABLE_TERMINATION_PROTECTION);

                HashMap<String, Object> responseMap = new HashMap<>();
                responseMap.put(TAGS_PROPERTY, tags);
                responseMap.put(PARAMETERS_PROPERTY, parameters);

                if (templatePath != null) {
                    responseMap.put(TEMPLATE_PATH_PROPERTY,
                                    templatePath);
                }

                if (stackName != null) {
                    responseMap.put(STACK_NAME_PROPERTY,
                                    stackName);
                }

                if (region != null) {
                    responseMap.put(REGION_PROPERTY,
                                    region);
                }

                if (executionRole != null) {
                    responseMap.put(EXECUTION_ROLE_PROPERTY,
                                    executionRole);
                }

                if (outputPath != null) {
                    responseMap.put(OUTPUT_PATH_PROPERTY, outputPath);
                }

                if (action != null) {
                    responseMap.put(ACTION, action);
                }

                if (onFailure != null) {
                    responseMap.put(ON_FAILURE, onFailure);
                }

                if (enableTerminationProtection != null) {
                    responseMap.put(ENABLE_TERMINATION_PROTECTION, enableTerminationProtection);
                }

                return responseMap;
            }

            if (configExtensions.size() > 1) {
                logger.info("Done building config file, inheritance structure= " + createConfigTreeString(
                        configExtensions));
            }
            return configMap;
        } catch (JsonParseException e) {
            throw new StackConfigException("Could not parse config file = " + configFile, e);
        } catch (IOException e) {
            throw new StackConfigException("Could not get config file = " + configFile, e);
        }
    }

    private static String createConfigTreeString(Set<String> configExtensions) {
        StringBuilder stringBuilder = new StringBuilder();
        String delimiter = " -> ";
        configExtensions.forEach(s -> stringBuilder.append(s).append(delimiter));

        return stringBuilder.substring(0, stringBuilder.toString().length() - delimiter.length());
    }

    private ObjectMapper getObjectMapper(String filePath) {
        if (filePath.endsWith(".yaml") || filePath.endsWith(".yml")) {
            logger.info("using yaml file parser");

            return new ObjectMapper(new YAMLFactory());

        }
        logger.info("using json file parser");
        return new ObjectMapper();
    }

    private byte[] getConfigFile(String configFile, String key, String bucketName) {
        try {
            return s3Facade.getObject(bucketName, key);
        } catch (NoSuchKeyException e) {
            String errorMessage = "Could not find config file " + configFile + " in S3, please check config file name and path";
            logger.error(errorMessage);
            throw new StackConfigException(errorMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getTags(Map<String, Object> map) {
        return (Map<String, Object>) map.getOrDefault(TAGS_PROPERTY, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getParameters(Map<String, Object> map) {
        return (Map<String, Object>) map.getOrDefault(PARAMETERS_PROPERTY, new HashMap<>());
    }

    private Function<Map.Entry<String, Object>, Map.Entry<String, ConfigurationPropertyValue>> toConfigProperty() {
        return entry -> {
            if (entry.getValue() instanceof Map) {
                return toObjectConfigProperty(entry);
            } else if (entry.getValue() instanceof List) {
                throw new StackConfigException("Lists are currently not supported as configuration, faulty property = " + entry
                        .getKey());
            } else {
                return Map.entry(entry.getKey(), ConfigurationPropertyValue.create(entry.getValue().toString()));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Map.Entry<String, ConfigurationPropertyValue> toObjectConfigProperty(Map.Entry<String, Object> entry) {
        Map<String, Object> value = (Map<String, Object>) entry.getValue();
        if (value.get(VALUE_PROPERTY) instanceof List) {
            throw new StackConfigException("Lists are currently not supported as configuration, faulty property = " + entry
                    .getKey());
        }
        if (!value.containsKey(VALUE_PROPERTY)) {
            throw new StackConfigException(
                    "Object properties in stack configuration files need to have a value field, faulty property = " + entry
                            .getKey());

        }
        ConfigurationPropertyValue.Type type = value.containsKey(CONFIG_TYPE_PROPERTY) ? ConfigurationPropertyValue.Type.mapType(
                value.get(CONFIG_TYPE_PROPERTY).toString()) : STRING;
        String defaultValue = value.containsKey(DEFAULT_VALUE_PROPERTY) ? value.get(DEFAULT_VALUE_PROPERTY)
                                                                               .toString() : null;

        if (value.containsKey(FALLBACK_PROPERTY)) {
            if (value.get(FALLBACK_PROPERTY) instanceof Boolean) {
                return Map.entry(entry.getKey(),
                                 ConfigurationPropertyValue.create(value.get(VALUE_PROPERTY).toString(),
                                                                   (Boolean) value.get(FALLBACK_PROPERTY),
                                                                   type,
                                                                   defaultValue));
            } else {
                throw new StackConfigException("Fallback property has to be a boolean, faulty property = " + entry.getKey());
            }
        } else {
            return Map.entry(entry.getKey(),
                             ConfigurationPropertyValue.create(value.get(VALUE_PROPERTY).toString(),
                                                               false,
                                                               type,
                                                               defaultValue));
        }
    }

}
