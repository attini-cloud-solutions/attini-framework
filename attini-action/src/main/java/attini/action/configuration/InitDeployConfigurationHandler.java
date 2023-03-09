package attini.action.configuration;

import static attini.action.custom.resource.CustomResourceResponse.failedResponse;
import static attini.action.custom.resource.CustomResourceResponse.successResponse;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.custom.resource.CustomResourceResponseSender;

public class InitDeployConfigurationHandler {

    private static final Logger logger = Logger.getLogger(InitDeployConfigurationHandler.class);
    private final CustomResourceResponseSender customResourceResponseSender;
    private final ObjectMapper objectMapper;

    public InitDeployConfigurationHandler(CustomResourceResponseSender customResourceResponseSender,
                                          ObjectMapper objectMapper) {
        this.customResourceResponseSender = requireNonNull(customResourceResponseSender,
                                                           "customResourceResponseSender");
        this.objectMapper = objectMapper;
    }

    public void handleConfig(Map<String, Object> input) {
        JsonNode inputJson = createJson(input);
        String arn = inputJson.get("ResourceProperties")
                              .get("InitDeployRoleArn")
                              .asText();

        String responseURL = inputJson.get("ResponseURL")
                                      .asText();

        try {
            if (arn != null && !arn.isEmpty()) {
                String[] split = arn.split("/");
                String name = split[split.length - 1];
                logger.info("Setting InitDeployRoleName=" + name + " to response");

                customResourceResponseSender.sendResponse(responseURL,
                                                          successResponse(inputJson)
                                                                  .addData("InitDeployRoleName", name)
                                                                  .build());
            } else {
                customResourceResponseSender.sendResponse(responseURL, successResponse(inputJson).build());
            }

        } catch (Exception e) {
            logger.error("Error when responding to config mutation custom resource", e);
            customResourceResponseSender.sendResponse(responseURL,
                                                      failedResponse(inputJson)
                                                              .setReason("Failed to mutate config")
                                                              .build());
        }


    }

    private JsonNode createJson(Map<String, Object> input) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(input)).deepCopy();
        } catch (JsonProcessingException e) {
            logger.fatal("Could not parse input", e);
            throw new RuntimeException(e);
        }
    }
}
