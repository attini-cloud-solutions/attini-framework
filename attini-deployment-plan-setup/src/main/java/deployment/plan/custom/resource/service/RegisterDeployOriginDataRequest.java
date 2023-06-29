/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import deployment.plan.custom.resource.CfnRequestType;
import deployment.plan.transform.Runner;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Builder(builderClassName = "Builder")
public class RegisterDeployOriginDataRequest {
    private final String newSfnArn;
    private final String oldSfnArn;
    private final String stackName;
    private final CfnRequestType cfnRequestType;
    private final String stepFunctionLogicalId;
    private final List<Runner> runners;

    private final Map<String, String> parameters;

    private final Map<String, Object> payloadDefaults;


    public String getNewSfnArn() {
        return newSfnArn;
    }

    public String getOldSfnArn() {
        return oldSfnArn;
    }

    public String getStackName() {
        return stackName;
    }

    public CfnRequestType getCfnRequestType() {
        return cfnRequestType;
    }

    public String getStepFunctionLogicalId() {
        return stepFunctionLogicalId;
    }

    public List<Runner> getRunners() {
        return runners == null ? Collections.emptyList() : runners;
    }

    public Map<String, String> getParameters() {
        return parameters == null ? Collections.emptyMap() : parameters;
    }

    public Map<String, Object> getPayloadDefaults() {
        return payloadDefaults;
    }

}
