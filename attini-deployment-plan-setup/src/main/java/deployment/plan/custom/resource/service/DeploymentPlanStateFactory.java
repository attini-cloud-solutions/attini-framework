package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import deployment.plan.custom.resource.StackType;
import deployment.plan.transform.AttiniStep;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceResponse;

public class DeploymentPlanStateFactory {


    private static final Logger logger = Logger.getLogger(DeploymentPlanStateFactory.class);


    private final CloudFormationClient cloudFormationClient;
    private final DeployStatesFacade deployStatesFacade;

    public DeploymentPlanStateFactory(CloudFormationClient cloudFormationClient,
                                      DeployStatesFacade deployStatesFacade) {
        this.cloudFormationClient = requireNonNull(cloudFormationClient, "cloudFormationClient");
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
    }

    public DeploymentPlanResourceState create(RegisterDeployOriginDataRequest request, StackType stackType) {

        StackResourceState stackResourceState = getStackResourceState(stackType, request);

        return DeploymentPlanResourceState.builder()
                                          .distributionName(stackResourceState.getDistributionName())
                                          .environment(stackResourceState.getEnvironment())
                                          .objectIdentifier(stackResourceState.getObjectIdentifier())
                                          .distributionId(stackResourceState.getDistributionId())
                                          .sfnArn(request.getNewSfnArn())
                                          .stackName(request.getStackName())
                                          .attiniSteps(getAttiniSteps(request))
                                          .payloadDefaults(request.getPayloadDefaults())
                                          .build();
    }

    private StackResourceState getStackResourceState(StackType stackType, RegisterDeployOriginDataRequest request) {
        if (stackType == StackType.INFRA) {
            return deployStatesFacade.getInitStackState(
                    request.getStackName());
        }
        return deployStatesFacade.getStackState(request.getStackName());
    }

    private List<AttiniStep> getAttiniSteps(RegisterDeployOriginDataRequest request) {
        DescribeStackResourceResponse response = cloudFormationClient.describeStackResource(DescribeStackResourceRequest.builder()
                                                                                                                        .stackName(
                                                                                                                                request.getStackName())
                                                                                                                        .logicalResourceId(
                                                                                                                                request.getStepFunctionLogicalId())
                                                                                                                        .build());


        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(response.stackResourceDetail().metadata());
            JsonNode attiniSteps = jsonNode.path("AttiniSteps");
            if (attiniSteps.isMissingNode()) {
                return Collections.emptyList();
            }
            ObjectReader reader = objectMapper.readerFor(new TypeReference<List<AttiniStep>>() {
            });
            return reader.readValue(attiniSteps);
        } catch (IOException e) {
            logger.error("Could not parse resource metadata", e);
            throw new RuntimeException("Could not parse resource metadata", e);
        }
    }

}
