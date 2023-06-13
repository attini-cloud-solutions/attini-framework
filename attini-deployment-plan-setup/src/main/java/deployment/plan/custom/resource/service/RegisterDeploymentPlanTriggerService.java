/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import deployment.plan.custom.resource.CfnRequestType;

public class RegisterDeploymentPlanTriggerService {
    private static final Logger logger = Logger.getLogger(RegisterDeploymentPlanTriggerService.class);
    private final DeployStatesFacade deployStatesFacade;

    public RegisterDeploymentPlanTriggerService(DeployStatesFacade deployStatesFacade) {
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
    }


    public void registerDeploymentPlanTrigger(JsonNode input, CfnRequestType requestType) {
        JsonNode resourceProperties = input.get("ResourceProperties");
        SfnProperties sfnProperties = createSfnProperties(resourceProperties);
        deployStatesFacade.removeErrors(sfnProperties.stackName);
        switch (requestType) {
            case CREATE -> {
                logger.info("Register Trigger");
                deployStatesFacade.saveSfnTrigger(sfnProperties.sfnArn, sfnProperties.stackName);
            }
            case UPDATE -> {
                JsonNode oldResourceProperties = input.path("OldResourceProperties");
                SfnProperties oldSfnProperties = createSfnProperties(oldResourceProperties);
                updateTrigger(sfnProperties, oldSfnProperties);
            }
            case DELETE -> {
                logger.info("Deleting Trigger");
                deployStatesFacade.deleteSfnTrigger(sfnProperties.sfnArn, sfnProperties.stackName);
            }
            default -> throw new IllegalStateException("Invalid request type = " + requestType);
        }
    }

    private static SfnProperties createSfnProperties(JsonNode resourceProperties) {
        return new SfnProperties(
                resourceProperties.path("SfnArn").asText(),
                resourceProperties.path("SfnName").asText(),
                resourceProperties.path("StackName").asText()
        );
    }

    private void updateTrigger(SfnProperties newSfnProperties, SfnProperties oldSfnProperties) {
        if (!newSfnProperties.sfnArn.equals(oldSfnProperties.sfnArn)) {
            logger.info("Updating Trigger");
            deployStatesFacade.deleteSfnTrigger(oldSfnProperties.sfnArn, oldSfnProperties.stackName);
        }
        deployStatesFacade.saveSfnTrigger(newSfnProperties.sfnArn, newSfnProperties.stackName);

    }


}
