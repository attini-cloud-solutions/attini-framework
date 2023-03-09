package attini.deploy.origin.config;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import attini.deploy.origin.Distribution;
import attini.deploy.origin.DistributionDataFacade;
import attini.deploy.origin.s3.S3Facade;
import attini.deploy.origin.system.EnvironmentVariables;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.json.AttiniJsonProvider;

public class InitDeployParameterService {


    private final ObjectMapper objectMapper;
    private final DistributionDataFacade distributionDataFacade;
    private final S3Facade s3Facade;
    private final EnvironmentVariables environmentVariables;

    public InitDeployParameterService(DistributionDataFacade distributionDataFacade,
                                      S3Facade s3Facade,
                                      EnvironmentVariables environmentVariables) {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.distributionDataFacade = requireNonNull(distributionDataFacade, "distributionDataFacade");
        this.s3Facade = requireNonNull(s3Facade, "s3Facade");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public Map<String, String> resolveParameters(JsonNode jsonNode, Environment environment,
                                                 Set<DistributionName> dependencies) {

        return StreamSupport.stream(toIterable(jsonNode, environment).spliterator(), false)
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                      getParameterValue(environment, dependencies)));
    }

    private Iterable<Map.Entry<String, JsonNode>> toIterable(JsonNode jsonNode, Environment environment) {
        return getConfig(jsonNode, environment)::fields;
    }

    private JsonNode getConfig(JsonNode jsonNode, Environment environment) {
        if (!jsonNode.path("default").isMissingNode() &&
            !jsonNode.path(environment.asString()).isMissingNode()) {

            ObjectNode defaultConfig = (ObjectNode) jsonNode.get("default");
            return defaultConfig.setAll((ObjectNode) jsonNode.path(environment.asString()));


        }

        if (!jsonNode.path(environment.asString()).isMissingNode()) {
            return jsonNode.path(environment.asString());
        }

        if (!jsonNode.path("default").isMissingNode()) {
            return jsonNode.path("default");
        }
        return objectMapper.createObjectNode();
    }

    private Function<Map.Entry<String, JsonNode>, String> getParameterValue(Environment environment,
                                                                            Set<DistributionName> dependencies) {
        return entry -> {
            JsonNode value = entry.getValue();
            if (value.isObject()) {
                if ("Distribution".equals(value.get("sourceType").asText())) {
                    validateDistParam(value, entry.getKey(), dependencies);
                    String distributionName = value.path("source").path("name").textValue();
                    Distribution distribution = distributionDataFacade.getDistribution(DistributionName.of(
                                                                              distributionName), environment)
                                                                      .orElseThrow(() -> new IllegalArgumentException(
                                                                              "Distribution: " + distributionName + " not found in environment"));

                    String outputUrl = distribution.getOutputUrl()
                                                   .orElseThrow(() -> new IllegalArgumentException(
                                                           "No output found for distribution: " + distributionName));

                    byte[] bytes = s3Facade.downloadS3File(environmentVariables.getArtifactBucket(), getKey(outputUrl));

                    Configuration configuration = Configuration.builder()
                                                               .jsonProvider(new AttiniJsonProvider())
                                                               .build();

                    DocumentContext context = JsonPath.using(configuration).parse(new String(bytes));

                    try {
                        return context.read(value.path("source").path("mapping").textValue());
                    } catch (PathNotFoundException e) {
                        throw new IllegalArgumentException("Could not read init parameter: " + entry.getKey()+". " + e.getMessage());
                    }
                    
                } else {
                    throw new IllegalArgumentException("Invalid type for init parameter: " + entry.getKey());
                }

            }

            return value.asText();
        };
    }

    private static void validateDistParam(JsonNode jsonNode, String key, Set<DistributionName> dependencies) {
        if (jsonNode.path("source").isMissingNode()) {
            throw new IllegalArgumentException("no source specified for init parameter: " + key);
        }
        JsonNode distributionName = jsonNode.path("source").path("name");
        if (distributionName.isMissingNode()) {
            throw new IllegalArgumentException("no name specified in source for init parameter: " + key);
        }
        if (!dependencies.contains(DistributionName.of(distributionName.asText()))) {
            throw new IllegalArgumentException("Parameter " + key + " is dependent on distribution " +distributionName + " but it is not declared as a dependency");
        }

        if (jsonNode.path("source").path("mapping").isMissingNode()) {
            throw new IllegalArgumentException("no mapping specified in source for init parameter: " + key);
        }
    }

    private static String getKey(String url) {
        try {
            return new URL(url).getPath().substring(1);
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

}
