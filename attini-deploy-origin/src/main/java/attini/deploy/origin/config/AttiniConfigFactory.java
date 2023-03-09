package attini.deploy.origin.config;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.text.StringSubstitutor;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import attini.deploy.origin.AttiniConfigException;
import attini.deploy.origin.InitDeployEvent;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.Version;

public class AttiniConfigFactory {
    private static final Logger logger = Logger.getLogger(AttiniConfigFactory.class);
    public static final String ATTINI_DISTRIBUTION_NAME_KEY = "distributionName";
    public static final String ATTINI_DISTRIBUTION_ID_KEY = "distributionId";

    public static final String ATTINI_VERSION_KEY = "version";

    private static final String DISTRIBUTION_TAGS_KEY = "distributionTags";

    private static final String DEPENDENCIES = "dependencies";


    private final ConfigFileResolver configFileResolver;
    private final InitDeployParameterService initDeployParameterService;

    private final ObjectMapper mapper;

    public AttiniConfigFactory(ConfigFileResolver configFileResolver,
                               InitDeployParameterService initDeployParameterService) {
        this.configFileResolver = requireNonNull(configFileResolver, "configFileResolver");
        this.initDeployParameterService = requireNonNull(initDeployParameterService, "initDeployParameterService");
        this.mapper = new ObjectMapper(new YAMLFactory());
    }

    public AttiniConfig createAttiniConfig(Path zipDir, InitDeployEvent event) {


        List<String> configFiles = configFileResolver.getAttiniConfigFiles(zipDir);

        Map<String, Object> configMap;
        if (configFiles.size() > 1) {
            throw new IllegalStateException("More then one attini-config files detected");
        }
        if (configFiles.size() == 0) {
            logger.info("No attini-config file present in distribution, will create default");
            configMap = new HashMap<>();
        } else {
            configMap = getConfigMap(zipDir, configFiles);
        }

        DistributionId distributionId = createDistributionId(configMap);

        DistributionName distributionName = createDistributionName(configMap, event.getFolderName());

        configMap = applyVariables(configMap, event.getEnvironmentName(), distributionId, distributionName);

        logger.info("AttiniConfig after variable substitution: " + configMap);

        List<DistributionDependency> dependencies = createDependencies(configMap, distributionName, distributionId);


        return AttiniConfigImpl.builder()
                               .attiniInitDeployStackConfig(extractAttiniInitDeployStackConfig(configMap,
                                                                                               event.getEnvironmentName(),
                                                                                               dependencies).orElse(
                                       null))
                               .dependencies(dependencies)
                               .version(createVersion(configMap))
                               .distributionId(distributionId)
                               .distributionName(distributionName)
                               .distributionTags(createAttiniDistributionTags(configMap,
                                                                              distributionName,
                                                                              distributionId))
                               .build();
    }

    private DistributionId createDistributionId(Map<String, Object> configMap) {
        if (configMap.get(ATTINI_DISTRIBUTION_ID_KEY) == null) {
            String id = UUID.randomUUID().toString();
            logger.info("distributionId is missing, setting id: " + id);
            return DistributionId.of(id);
        }

        return DistributionId.of((String) configMap.get(ATTINI_DISTRIBUTION_ID_KEY));
    }

    private Version createVersion(Map<String, Object> configMap) {
        if (configMap.get(ATTINI_VERSION_KEY) == null) {
            return null;
        }

        return Version.of((String) configMap.get(ATTINI_VERSION_KEY));
    }

    private DistributionName createDistributionName(Map<String, Object> configMap, String defaultValue) {
        if (configMap.get(ATTINI_DISTRIBUTION_NAME_KEY) == null) {
            logger.info("distributionName is missing, setting name: " + defaultValue);
            return DistributionName.of(defaultValue);
        }
        return DistributionName.of((String) configMap.get(ATTINI_DISTRIBUTION_NAME_KEY));
    }

    private Map<String, String> createAttiniDistributionTags(Map<String, Object> attiniConfigFile,
                                                             DistributionName distributionName,
                                                             DistributionId distributionId) {
        try {
            return mapper.convertValue(attiniConfigFile.get(DISTRIBUTION_TAGS_KEY),
                                       new TypeReference<>() {
                                       });
        } catch (IllegalArgumentException e) {
            String message = "Could not add attini distribution tags, this is most likely due to an invalid format in the attini-config file. " +
                             "Distribution tags only support key value pairs with string keys and string values";
            logger.error(message, e);
            throw new AttiniConfigException(AttiniConfigException.ErrorCode.INVALID_FORMAT,
                                            message,
                                            distributionName,
                                            distributionId);
        }
    }

    private List<DistributionDependency> createDependencies(Map<String, Object> attiniConfigFile,
                                                            DistributionName distributionName,
                                                            DistributionId distributionId) {
        try {
            Object object = attiniConfigFile.get(DEPENDENCIES);
            if (object == null) {
                return Collections.emptyList();
            }
            return mapper.convertValue(object, new TypeReference<>() {
            });
        } catch (IllegalArgumentException e) {
            String message = "Invalid format for dependencies";
            logger.error(message, e);
            throw new AttiniConfigException(AttiniConfigException.ErrorCode.INVALID_FORMAT,
                                            message,
                                            distributionName,
                                            distributionId);
        }
    }

    private Map<String, Object> getConfigMap(Path zipDir, List<String> configFiles) {
        try {

            Path configPath = Path.of(String.format("%s/%s",
                                                    zipDir.toAbsolutePath(),
                                                    configFiles.get(0)));
            return mapper.readValue(configPath.toFile(), new TypeReference<>() {
            });
        } catch (IOException e) {
            logger.warn("Could not extract Config file, setting defaults");
            return new HashMap<>();
        }
    }

    private static Map<String, Object> applyVariables(Map<String, Object> config,
                                                      Environment environmentName,
                                                      DistributionId distributionId,
                                                      DistributionName distributionName) {

        try {
            Map<String, String> variableMap = new HashMap<>();
            variableMap.put("environment", environmentName.asString());
            variableMap.put(ATTINI_DISTRIBUTION_NAME_KEY,
                            distributionName.asString());
            variableMap.put(ATTINI_DISTRIBUTION_ID_KEY,
                            distributionId.asString());

            StringSubstitutor sub = new StringSubstitutor(variableMap);
            ObjectMapper objectMapper = new ObjectMapper();

            String configString = sub.replace(objectMapper.writeValueAsString(config));
            return objectMapper.readValue(configString, new TypeReference<>() {
            });


        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid attini-config format");
        }
    }

    private Optional<InitDeployStackConfig> extractAttiniInitDeployStackConfig(Map<String, Object> attiniConfigFile,
                                                                               Environment environment,
                                                                               List<DistributionDependency> dependencies) {

        if (attiniConfigFile.containsKey("initDeployConfig")) {

            logger.info("initDeployConfig present in attini-config file");
            Map<String, Object> rawAttiniInitDeployStackConfig = mapper.convertValue(attiniConfigFile.get(
                    "initDeployConfig"), new TypeReference<>() {
            });
            InitDeployStackConfig.Builder builder = new InitDeployStackConfig.Builder();

            if (rawAttiniInitDeployStackConfig.containsKey("template")) {
                String template = rawAttiniInitDeployStackConfig.get("template").toString();
                builder.attiniInitDeployTemplatePath(template.startsWith("/") ? template.substring(1) : template);
            }

            if (rawAttiniInitDeployStackConfig.containsKey("stackName")) {
                builder.attiniInitDeployStackName(rawAttiniInitDeployStackConfig.get("stackName").toString());
            }

            if (rawAttiniInitDeployStackConfig.containsKey("parameters")) {

                try {
                    builder.parameters(initDeployParameterService.resolveParameters(mapper.valueToTree(
                                                                                            rawAttiniInitDeployStackConfig.get("parameters")),
                                                                                    environment,
                                                                                    dependencies.stream()
                                                                                                .map(DistributionDependency::distributionName)
                                                                                                .collect(Collectors.toSet())));
                } catch (IllegalArgumentException e) {
                    throw new AttiniConfigException(AttiniConfigException.ErrorCode.INVALID_PARAMETER_CONFIG,
                                                    e.getMessage(),
                                                    DistributionName.of((String) attiniConfigFile.get(
                                                            ATTINI_DISTRIBUTION_NAME_KEY)),
                                                    (DistributionId.of((String) attiniConfigFile.get(
                                                            ATTINI_DISTRIBUTION_ID_KEY))));
                }

            }
            if (rawAttiniInitDeployStackConfig.containsKey("tags")) {
                builder.tags(mapper.convertValue(rawAttiniInitDeployStackConfig.get("tags"), new TypeReference<>() {
                }));
            }

            if (rawAttiniInitDeployStackConfig.containsKey("variables")) {
                builder.variables(mapper.convertValue(rawAttiniInitDeployStackConfig.get("variables"),
                                                      new TypeReference<>() {
                                                      }));
            }

            if (rawAttiniInitDeployStackConfig.containsKey("forceUpdate")) {
                builder.forceUpdate(Boolean.parseBoolean(rawAttiniInitDeployStackConfig.get("forceUpdate").toString()));
            }
            return Optional.of(builder.build());
        }


        return Optional.empty();

    }
}
