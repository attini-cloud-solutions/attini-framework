/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.custom.resource.CustomResourceHandler;
import deployment.plan.transform.TransformDeploymentPlanCloudFormation;


@Named("app")
public class App implements RequestHandler<Map<String, Object>, Object> {
    private static final Logger logger = Logger.getLogger(App.class);
    private final TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation;
    private final CustomResourceHandler customResourceHandler;
    private final ObjectMapper mapper;


    @Inject
    public App(TransformDeploymentPlanCloudFormation transformDeploymentPlanCloudFormation,
               CustomResourceHandler customResourceHandler,
               ObjectMapper mapper) {
        this.transformDeploymentPlanCloudFormation = requireNonNull(transformDeploymentPlanCloudFormation,
                                                                    "transformDeploymentPlanCloudFormation");
        this.customResourceHandler = requireNonNull(customResourceHandler, "customResourceHandler");
        this.mapper = mapper;
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        try {
            String inputString = mapper.writeValueAsString(input);
            logger.info("Got event: " + inputString);

            if (isMacro(input)) {
                try {
                    logger.info("Transforming deployment plan");
                    Map<String, Object> stringObjectMap = transformDeploymentPlanCloudFormation.transformTemplate(input);
                    logger.info("Transformed template: " +mapper.writeValueAsString(stringObjectMap));
                    return stringObjectMap;
                } catch (Exception e) {
                    logger.fatal("Could not transform deployment plan, sending error response", e);
                    return createErrorResponse(input, e);
                }
            }
            if (isCustomResource(input)) {
                logger.info("Registering deployment plan");
                customResourceHandler
                        .handleCustomResource(input, context);
            }

        } catch (JsonProcessingException e) {
            logger.error("Could not parse input", e);
        }
        return input;
    }

    private static Map<String, Object> createErrorResponse(Map<String, Object> input, Exception e) {
        final Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("requestId", input.get("requestId"));
        responseMap.put("status", "FAILURE");
        responseMap.put("errorMessage", e.getMessage());
        return responseMap;
    }


    private boolean isCustomResource(Map<String, Object> input) {
        String resourceType = (String) input.get("ResourceType");
        return input.containsKey("ResourceType")
               && resourceType.startsWith("Custom::");
    }

    private static boolean isMacro(Map<String, Object> input) {
        return input.containsKey("fragment");
    }

}
