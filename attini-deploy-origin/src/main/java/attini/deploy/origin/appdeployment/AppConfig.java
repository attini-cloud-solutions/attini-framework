package attini.deploy.origin.appdeployment;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.domain.Environment;

public class AppConfig {


    private final String appDeploymentPlan;

    private final JsonNode config;

    public AppConfig(JsonNode jsonNode,
                     ObjectMapper objectMapper,
                     String appDeploymentPlan,
                     Environment environment) {
        this.appDeploymentPlan = requireNonNull(appDeploymentPlan, "appDeploymentPlan");

        if (jsonNode != null) {
            jsonNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isValueNode()){
                    throw new IllegalArgumentException("Configuration for an environments need to be an object");
                }
            });
            JsonNode defaultNode = jsonNode.path("default").deepCopy();
            if (jsonNode.path(environment.asString()).isMissingNode()) {
                this.config = defaultNode;
            } else {
                try {
                    this.config = objectMapper.readerForUpdating(defaultNode)
                                         .readValue(jsonNode.path(environment.asString()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

        } else {
            config = objectMapper.missingNode();
        }

    }

    public String getAppDeploymentPlan() {
        return appDeploymentPlan;
    }

    public JsonNode getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "AppConfig{" +
               "appDeploymentPlan='" + appDeploymentPlan + '\'' +
               ", config=" + config +
               '}';
    }
}
