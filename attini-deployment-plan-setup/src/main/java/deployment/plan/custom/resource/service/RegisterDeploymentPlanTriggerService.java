/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;

import deployment.plan.custom.resource.CfnRequestType;

public class RegisterDeploymentPlanTriggerService {
    private static final Logger logger = Logger.getLogger(RegisterDeploymentPlanTriggerService.class);
    private final DeployStatesFacade deployStatesFacade;

    public RegisterDeploymentPlanTriggerService(DeployStatesFacade deployStatesFacade) {
        this.deployStatesFacade = requireNonNull(deployStatesFacade, "deployStatesFacade");
    }

    @SuppressWarnings("unchecked")
    public void registerDeploymentPlanTrigger(Map<String, Object> input, CfnRequestType requestType){
        Map<String, Object> resourceProperties = (Map<String, Object>) input.get("ResourceProperties");
        SfnProperties sfnProperties = createSfnProperties(resourceProperties);
        deployStatesFacade.removeErrors(sfnProperties.stackName);
        switch (requestType) {
            case CREATE -> {
                logger.info("Register Trigger");
                deployStatesFacade.saveSfnTrigger(sfnProperties.sfnArn, sfnProperties.stackName);
            }
            case UPDATE -> {
                Map<String, Object> oldResourceProperties = (Map<String, Object>) input.get("OldResourceProperties");
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

    private static SfnProperties createSfnProperties(Map<String, Object> resourceProperties) {
        return new SfnProperties(
                (String) resourceProperties.get("SfnArn"),
                (String) resourceProperties.get("SfnName"),
                (String) resourceProperties.get("StackName")
        );
    }

    private void updateTrigger(SfnProperties newSfnProperties,  SfnProperties oldSfnProperties){
        if (!newSfnProperties.sfnArn.equals(oldSfnProperties.sfnArn)){
            logger.info("Updating Trigger");
            deployStatesFacade.deleteSfnTrigger(oldSfnProperties.sfnArn, oldSfnProperties.stackName);
        }
        deployStatesFacade.saveSfnTrigger(newSfnProperties.sfnArn, newSfnProperties.stackName);

    }



}
