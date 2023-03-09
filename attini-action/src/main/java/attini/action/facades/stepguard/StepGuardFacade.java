/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.facades.stepguard;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.App;
import attini.action.actions.deploycloudformation.stackconfig.StackConfiguration;
import attini.action.actions.deploycloudformation.StackData;
import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

public class StepGuardFacade {

    private static final Logger logger = Logger.getLogger(App.class);


    private final LambdaClient lambdaClient;

    public StepGuardFacade(LambdaClient lambdaClient) {
        this.lambdaClient = requireNonNull(lambdaClient, "lambdaClient");
    }

    public void notifyStepCompleted(StackData stackData, String stackStatus) {

        StepGuardRequest stepGuardRequest = stepGuardRequest(stackData, stackStatus, null);

        logger.info("Notifying step guard to continue, " + stepGuardRequest);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                                                   .functionName(System.getenv("ATTINI_STEP_GUARD"))
                                                   .payload(SdkBytes.fromUtf8String(createPayloadString(stepGuardRequest)))
                                                   .build();
        lambdaClient.invoke(invokeRequest);
    }

    public void notifyStepCompleted(StackData stackData, String stackStatus, String stackId) {

        StepGuardRequest stepGuardRequest = stepGuardRequest(stackData, stackStatus, stackId);

        logger.info("Notifying step guard to continue, " + stepGuardRequest);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                                                   .functionName(System.getenv("ATTINI_STEP_GUARD"))
                                                   .payload(SdkBytes.fromUtf8String(createPayloadString(stepGuardRequest)))
                                                   .build();
        lambdaClient.invoke(invokeRequest);
    }

    private static String createPayloadString(StepGuardRequest stepGuardRequest) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(new PayloadTemplate(stepGuardRequest));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @RegisterForReflection
    private static class PayloadTemplate {
        public final StepGuardRequest payload;
        public final String requestType = "manualTrigger";

        public PayloadTemplate(StepGuardRequest payload) {
            this.payload = payload;
        }
    }

    private static StepGuardRequest stepGuardRequest(StackData stackData, String stackStatus,  String stackId) {
        StackConfiguration stackConfiguration = stackData.getStackConfiguration();

        StepGuardRequest.Builder builder = StepGuardRequest.builder()
                                                           .setStepName(stackData.getDeploymentPlanExecutionMetadata()
                                                                                 .stepName())
                                                           .setStackName(stackConfiguration.getStackName())
                                                           .setResourceType("AWS::CloudFormation::Stack")
                                                           .setLogicalResourceId(stackConfiguration.getStackName())
                                                           .setSfnResponseToken(stackData.getDeploymentPlanExecutionMetadata()
                                                                                         .sfnToken())
                                                           .setStackId(stackId)
                                                           .setResourceStatus(stackStatus)
                                                           .setEnvironment(stackData.getEnvironment())
                                                           .setDistributionName(stackData.getDistributionName())
                                                           .setObjectIdentifier(stackData.getObjectIdentifier())
                                                           .setDistributionId(stackData.getDistributionId())
                                                           .setClientRequestToken(stackData.getClientRequestToken()
                                                                                           .asString())
                                                           .setDesiredState(stackConfiguration.getDesiredState());

        stackConfiguration.getRegion().ifPresent(builder::setRegion);
        stackConfiguration.getExecutionRole().ifPresent(builder::setExecutionRoleArn);
        stackConfiguration.getOutputPath().ifPresent(builder::setOutputPath);
        return builder.build();
    }
}
