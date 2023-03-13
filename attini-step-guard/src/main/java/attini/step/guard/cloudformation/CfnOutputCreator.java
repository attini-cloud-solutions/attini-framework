/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.cloudformation;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.step.guard.stackdata.DesiredState;
import attini.step.guard.stackdata.StackData;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;

public class CfnOutputCreator {

    private static final Logger logger = Logger.getLogger(CfnOutputCreator.class);

    private final CloudFormationClientFactory cloudFormationClientFactory;
    private final ObjectMapper objectMapper;


    public CfnOutputCreator(CloudFormationClientFactory cloudFormationClientFactory,
                            ObjectMapper objectMapper) {
        this.cloudFormationClientFactory = requireNonNull(cloudFormationClientFactory, "cloudFormationClientFactory");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public String createCfnOutput(StackData stackData, CloudFormationEvent cloudFormationEvent) {
        return createCfnOutput(cloudFormationEvent,
                               stackData.getOutputPath().orElse(null),
                               stackData.getDesiredState());

    }

    public String createCfnOutput(CloudFormationManualTriggerEvent cloudFormationManualTriggerEvent) {
        return createCfnOutput(cloudFormationManualTriggerEvent,
                               cloudFormationManualTriggerEvent.getOutputPath().orElse(null),
                               cloudFormationManualTriggerEvent.getDesiredState());

    }

    private String createCfnOutput(CloudFormationEvent cloudFormationEvent,
                                   String outputPath,
                                   DesiredState desiredState) {

        if (desiredState != DesiredState.DELETED) {

            DescribeStacksResponse response =
                    cloudFormationClientFactory.getClient(cloudFormationEvent)
                                               .describeStacks(DescribeStacksRequest.builder()
                                                                                    .stackName(
                                                                                            cloudFormationEvent
                                                                                                    .getStackId()
                                                                                                    .orElseGet(cloudFormationEvent::getStackName))
                                                                                    .build());

            logger.info("Found " + response.stacks()
                                           .size() + " stacks for stack name " + cloudFormationEvent.getStackName());


            Map<String, ?> stackOutput = outputPath == null ? createStackOutput(response) : createStackOutput(response,
                                                                                                              outputPath);

            return writeToJsonString(stackOutput);
        }


        return "{}";
    }

    private Map<String, String> createStackOutput(DescribeStacksResponse response) {

        HashMap<String, String> stackOutput = new HashMap<>();
        response.stacks()
                .get(0)
                .outputs()
                .forEach(output -> stackOutput.put(output.outputKey(), output.outputValue()));
        return stackOutput;
    }

    private Map<String, Map<String, String>> createStackOutput(DescribeStacksResponse response, String outputPath) {
        Map<String, String> stackOutput = createStackOutput(response);
        return Map.of(outputPath, stackOutput);
    }


    private String writeToJsonString(Map<String, ?> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not parse stack output", e);
        }
    }

}
