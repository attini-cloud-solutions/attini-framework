/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;
import deployment.plan.transform.AttiniStep;
import lombok.Builder;
import lombok.ToString;

@ToString
@Builder(builderClassName = "Builder")
public class DeploymentPlanResourceState {
    private final Environment environment;
    private final DistributionName distributionName;
    private final DistributionId distributionId;
    private final ObjectIdentifier objectIdentifier;
    private final String sfnArn;
    private final String stackName;
    private final List<AttiniStep> attiniSteps;

    private final Map<String, Object> payloadDefaults;


    public Environment getEnvironment() {
        return environment;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public String getSfnArn() {
        return sfnArn;
    }

    public String getStackName() {
        return stackName;
    }

    public DistributionId getDistributionId() {
        return distributionId;
    }

    public List<AttiniStep> getAttiniSteps() {
        return attiniSteps.isEmpty() ? Collections.emptyList() : attiniSteps;
    }

    public Map<String, Object> getPayloadDefaults() {
        return payloadDefaults.isEmpty() ? Collections.emptyMap() : payloadDefaults;
    }
}
