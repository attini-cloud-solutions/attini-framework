/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import deployment.plan.transform.AttiniStep;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder(builderClassName = "Builder")
public class DeploymentPlanResourceState {
    private final String attiniEnvironmentName;
    private final String attiniDistributionName;
    private final String attiniDistributionId;
    private final String attiniObjectIdentifier;
    private final String sfnArn;
    private final String stackName;
    private final List<AttiniStep> attiniSteps;

    private final Map<String, Object> payloadDefaults;



    public static Builder builder() {
        return new Builder();
    }


    public String getAttiniEnvironmentName() {
        return attiniEnvironmentName;
    }

    public String getAttiniDistributionName() {
        return attiniDistributionName;
    }

    public String getAttiniObjectIdentifier() {
        return attiniObjectIdentifier;
    }

    public String getSfnArn() {
        return sfnArn;
    }

    public String getStackName() {
        return stackName;
    }

    public String getAttiniDistributionId() {
        return attiniDistributionId;
    }

    public List<AttiniStep> getAttiniSteps() {
        return attiniSteps.isEmpty() ? Collections.emptyList() : attiniSteps;
    }

    public Map<String, Object> getPayloadDefaults() {
        return payloadDefaults.isEmpty() ? Collections.emptyMap() : payloadDefaults;
    }
}
