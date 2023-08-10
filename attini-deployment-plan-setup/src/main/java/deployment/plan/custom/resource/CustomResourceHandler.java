/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.custom.resource.service.AppDeploymentService;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataRequest;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;
import deployment.plan.transform.Runner;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

public class CustomResourceHandler {

    private static final Logger logger = Logger.getLogger(CustomResourceHandler.class);
    private static final String RESPONSE_URL = "ResponseURL";
    private static final String STACK_NAME = "StackName";
    private static final String STACK_ID = "StackId";
    private static final String LOGICAL_RESOURCE_ID = "LogicalResourceId";
    private static final String REQUEST_ID = "RequestId";
    private static final String REQUEST_TYPE = "RequestType";
    private static final String RESOURCE_TYPE = "ResourceType";
    private static final String PHYSICAL_RESOURCE_ID = "DeploymentPlan";
    private final RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService;
    private final RegisterDeployOriginDataService registerDeployOriginDataService;
    private final CfnResponseSender responseSender;
    private final ObjectMapper objectMapper;
    private final AppDeploymentService appDeploymentService;


    public CustomResourceHandler(RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService,
                                 RegisterDeployOriginDataService registerDeployOriginDataService,
                                 CfnResponseSender responseSender,
                                 ObjectMapper objectMapper, AppDeploymentService appDeploymentService) {
        this.registerDeploymentPlanTriggerService = requireNonNull(registerDeploymentPlanTriggerService,
                                                                   "registerDeploymentPlanTrigger");
        this.registerDeployOriginDataService = requireNonNull(registerDeployOriginDataService,
                                                              "registerDeployOriginData");
        this.responseSender = requireNonNull(responseSender, "responseSender");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.appDeploymentService = requireNonNull(appDeploymentService, "appDeploymentService");
    }

    public void handleCustomResource(JsonNode inputJson, Context context) {


        try {

            if (isDeploymentPlanCustomResource(inputJson)) {
                registerDeploymentPlan(inputJson);
            } else if (isDeleteCustomResource(inputJson)) {
                responseSender
                        .sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                      createSuccessResponse(inputJson));
            } else if (isAppDeploymentPlanCustomResource(inputJson)) {
                registerAppDeploymentPlan(inputJson);
            } else {
                responseSender
                        .sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                      createFailResponse(inputJson,
                                                         String.format(
                                                                 "Could not process request to unknown custom resource = %s, sending failed response",
                                                                 inputJson.path(RESOURCE_TYPE).asText())));

            }
        } catch (IllegalArgumentException e) {
            logger.error("Illegal argument", e);
            responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                        createFailResponse(inputJson, e.getMessage()));
        } catch (AwsServiceException e) {
            logger.error("AWS threw an exception", e);
            responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                        createFailResponse(inputJson,
                                                           e.awsErrorDetails().errorMessage()));
        } catch (Exception e) {
            logger.error("Unhandled exception occurred", e);
            String reason = String.format("Unforeseen error: %s, See more informant in Log group %s Stream %s",
                                          e.getMessage(),
                                          context.getLogGroupName(),
                                          context.getLogStreamName());
            responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                        createFailResponse(inputJson, reason));
        }

    }

    private void registerAppDeploymentPlan(JsonNode inputJson) {
        validateInput(inputJson);
        RegisterDeployOriginDataRequest registerDeployOriginRequest = toRegisterDeployOriginRequest(inputJson);
        String appPipelineName = inputJson.path("ResourceProperties").path("AppPipelineName").asText();
        appDeploymentService.saveAppDeploymentState(registerDeployOriginRequest, appPipelineName);
        responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                    createSuccessResponse(inputJson));
    }

    private void registerDeploymentPlan(JsonNode inputJson) {

        validateInput(inputJson);
        RegisterDeployOriginDataRequest registerDeployOriginRequest = toRegisterDeployOriginRequest(inputJson);
        registerDeploymentPlanTriggerService
                .registerDeploymentPlanTrigger(inputJson, registerDeployOriginRequest.getCfnRequestType());
        registerDeployOriginDataService
                .registerDeployOriginData(registerDeployOriginRequest);


        responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                    createSuccessResponse(inputJson));

    }

    private RegisterDeployOriginDataRequest toRegisterDeployOriginRequest(JsonNode input) {
        JsonNode resourceProperties = input.get("ResourceProperties");


        RegisterDeployOriginDataRequest.Builder builder = RegisterDeployOriginDataRequest.builder();

        if (!resourceProperties.path("Runners").isMissingNode()) {
            builder.runners(toRunners(resourceProperties.get("Runners")));

        }
        if (!resourceProperties.path("Parameters").isMissingNode()) {

            builder.parameters(toMap(resourceProperties.get("Parameters"), String.class));
        }

        if (!resourceProperties.path("PayloadDefaults").isMissingNode()) {
            builder.payloadDefaults(toMap(resourceProperties.get("PayloadDefaults"), Object.class));
        }

        JsonNode oldSfnArn = input.path("OldResourceProperties").path("SfnArn");
        if (!oldSfnArn.isMissingNode()) {
            builder.oldSfnArn(oldSfnArn.asText());
        }

        return builder.stackName(resourceProperties.get(STACK_NAME).textValue())
                      .stepFunctionLogicalId(resourceProperties.path("DeploymentPlanLogicalName").textValue())
                      .cfnRequestType(getRequestType(input))
                      .newSfnArn(resourceProperties.get("SfnArn").asText())
                      .build();
    }

    private List<Runner> toRunners(JsonNode node) {
        try {
            return objectMapper.treeToValue(node,
                                            objectMapper.getTypeFactory()
                                                        .constructCollectionType(ArrayList.class, Runner.class));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> Map<String, T> toMap(JsonNode node, Class<T> valueType) {
        try {
            return objectMapper.treeToValue(node,
                                            objectMapper.getTypeFactory()
                                                        .constructMapType(HashMap.class, String.class, valueType));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }


    private static CfnResponse createSuccessResponse(JsonNode input) {
        return CfnResponse.builder()
                          .setStatus("SUCCESS")
                          .setStackId(input.get(STACK_ID).asText())
                          .setLogicalResourceId(input.get(LOGICAL_RESOURCE_ID).asText())
                          .setRequestId(input.get(REQUEST_ID).asText())
                          .setPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                          .setReason("All is good")
                          .build();
    }

    private static CfnResponse createFailResponse(JsonNode input, String reason) {
        return CfnResponse.builder()
                          .setStatus("FAILED")
                          .setStackId(input.get(STACK_ID).asText())
                          .setLogicalResourceId(input.get(LOGICAL_RESOURCE_ID).asText())
                          .setRequestId(input.get(REQUEST_ID).asText())
                          .setPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                          .setReason(reason)
                          .build();
    }


    private static void validateInput(JsonNode inputJson) {
        try {
            new URL(inputJson.get(RESPONSE_URL).asText());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "the response URL is in an invalid format. This is a critical error, response URL =" + inputJson.get(
                            "ResponseURL").asText());
        }
    }

    private static CfnRequestType getRequestType(JsonNode input) {
        if (input.has(REQUEST_TYPE)) {
            return switch (input.get(REQUEST_TYPE).asText()) {
                case "Create" -> CfnRequestType.CREATE;
                case "Update" -> CfnRequestType.UPDATE;
                case "Delete" -> CfnRequestType.DELETE;
                default -> throw new IllegalStateException("Unknown request type = " + input.get(REQUEST_TYPE)
                                                                                            .asText());
            };
        } else {
            logger.fatal(
                    "The request did not have a RequestType, this is required data that should be provided by Cloudformation event");
            throw new IllegalStateException("Missing RequestType");
        }
    }


    private static boolean isDeleteCustomResource(JsonNode input) {
        return !input.path(RESOURCE_TYPE).isMissingNode() && input.get(REQUEST_TYPE).textValue()
                                                                  .equals("Delete");
    }


    private static boolean isDeploymentPlanCustomResource(JsonNode input) {
        return !input.path(RESOURCE_TYPE).isMissingNode() && input.get(RESOURCE_TYPE).textValue()
                                                                  .equals("Custom::AttiniDeploymentPlanTrigger");
    }

    private static boolean isAppDeploymentPlanCustomResource(JsonNode input) {
        return !input.path(RESOURCE_TYPE).isMissingNode() && input.get(RESOURCE_TYPE).textValue()
                                                                  .equals("Custom::AttiniAppDeploymentPlanTrigger");
    }

}
