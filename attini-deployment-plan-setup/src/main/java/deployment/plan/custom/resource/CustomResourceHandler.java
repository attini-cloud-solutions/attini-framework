/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource;

import static java.util.Objects.requireNonNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import deployment.plan.custom.resource.service.RegisterDeployOriginDataRequest;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;
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


    public CustomResourceHandler(RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService,
                                 RegisterDeployOriginDataService registerDeployOriginDataService,
                                 CfnResponseSender responseSender,
                                 ObjectMapper objectMapper) {
        this.registerDeploymentPlanTriggerService = requireNonNull(registerDeploymentPlanTriggerService,
                                                                   "registerDeploymentPlanTrigger");
        this.registerDeployOriginDataService = requireNonNull(registerDeployOriginDataService,
                                                              "registerDeployOriginData");
        this.responseSender = requireNonNull(responseSender, "responseSender");
        this.objectMapper = objectMapper;
    }

    public void handleCustomResource(Map<String, Object> input, Context context) {


        try {
            String inputString = objectMapper.writeValueAsString(input);
            ObjectNode inputJson = objectMapper.readTree(inputString).deepCopy();

            if (isDeploymentPlanCustomResource(input)) {
                registerDeploymentPlan(input, context, inputJson);
            } else if (isDeleteCustomResource(input)) {
                responseSender
                        .sendResponse(input.get(RESPONSE_URL).toString(),
                                      createSuccessResponse(inputJson));
            } else {
                responseSender
                        .sendResponse(input.get(RESPONSE_URL).toString(),
                                      createFailResponse(inputJson,
                                                         String.format(
                                                                 "Could not process request to unknown custom resource = %s, sending failed response",
                                                                 input.get(RESOURCE_TYPE))));

            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse input string", e);
        }


    }

    private void registerDeploymentPlan(Map<String, Object> input,
                                        Context context,
                                        ObjectNode inputJson) {


        CfnRequestType requestType = getRequestType(inputJson);
        try {
            validateInput(inputJson);
            registerDeploymentPlanTriggerService.registerDeploymentPlanTrigger(input, requestType);
            registerDeployOriginDataService
                    .registerDeployOriginData(toRegisterDeployOriginRequest(input, requestType));

            responseSender.sendResponse(inputJson.get(RESPONSE_URL).asText(),
                                        createSuccessResponse(inputJson));
        }catch (IllegalArgumentException e){
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

    @SuppressWarnings("unchecked")
    private RegisterDeployOriginDataRequest toRegisterDeployOriginRequest(Map<String, Object> input,
                                                                                 CfnRequestType requestType) {
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");


        RegisterDeployOriginDataRequest.Builder builder = RegisterDeployOriginDataRequest.builder();

        if (resourceProperties.containsKey("Runners")){
            builder.runners(objectMapper.convertValue(resourceProperties.get("Runners"), new TypeReference<>() {
            }));

        }
        if (resourceProperties.containsKey("Parameters")){
            builder.parameters(objectMapper.convertValue(resourceProperties.get("Parameters"), new TypeReference<>() {
            }));
        }

        if (resourceProperties.containsKey("PayloadDefaults")){
            builder.payloadDefaults(objectMapper.convertValue(resourceProperties.get("PayloadDefaults"), new TypeReference<>() {
            }));
        }

        if (requestType == CfnRequestType.UPDATE) {
            Map<String, Object> oldResourceProperties = (Map<String, Object>) input.get("OldResourceProperties");
            builder.oldSfnArn((String) oldResourceProperties.get("SfnArn"));
        }

        return builder.stackName((String) resourceProperties.get(STACK_NAME))
                      .stepFunctionLogicalId((String) resourceProperties.get("DeploymentPlanLogicalName"))
                      .cfnRequestType(requestType)
                      .newSfnArn((String) resourceProperties.get("SfnArn"))
                      .build();
    }


    private static CfnResponse createSuccessResponse(ObjectNode input) {
        return CfnResponse.builder()
                          .setStatus("SUCCESS")
                          .setStackId(input.get(STACK_ID).asText())
                          .setLogicalResourceId(input.get(LOGICAL_RESOURCE_ID).asText())
                          .setRequestId(input.get(REQUEST_ID).asText())
                          .setPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                          .setReason("All is good")
                          .build();
    }

    private static CfnResponse createFailResponse(ObjectNode input, String reason) {
        return CfnResponse.builder()
                          .setStatus("FAILED")
                          .setStackId(input.get(STACK_ID).asText())
                          .setLogicalResourceId(input.get(LOGICAL_RESOURCE_ID).asText())
                          .setRequestId(input.get(REQUEST_ID).asText())
                          .setPhysicalResourceId(PHYSICAL_RESOURCE_ID)
                          .setReason(reason)
                          .build();
    }


    private static void validateInput(ObjectNode inputJson) {
        try {
            new URL(inputJson.get(RESPONSE_URL).asText());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "the response URL is in an invalid format. This is a critical error, response URL =" + inputJson.get(
                            "ResponseURL").asText());
        }
    }

    private static CfnRequestType getRequestType(ObjectNode input) {
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


    private static boolean isDeleteCustomResource(Map<String, Object> input) {
        return input.containsKey(REQUEST_TYPE) && input.get(REQUEST_TYPE)
                                                       .equals("Delete");
    }


    private static boolean isDeploymentPlanCustomResource(Map<String, Object> input) {
        return input.containsKey(RESOURCE_TYPE) && input.get(RESOURCE_TYPE)
                                                        .equals("Custom::AttiniDeploymentPlanTrigger");
    }

}
