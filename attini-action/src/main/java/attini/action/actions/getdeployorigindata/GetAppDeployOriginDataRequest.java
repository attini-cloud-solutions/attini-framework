/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.actions.getdeployorigindata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import attini.domain.DistributionName;
import attini.domain.ObjectIdentifier;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class GetAppDeployOriginDataRequest {
    private final String sfnArn;
    private final String executionArn;
    private final DistributionName distributionName;
    private final ObjectIdentifier objectIdentifier;
    private final ObjectIdentifier platformDistributionIdentifier;
    private final DistributionName platformDistributionName;

    @JsonCreator
    public GetAppDeployOriginDataRequest(@JsonProperty("sfnArn") String sfnArn,
                                         @JsonProperty("executionArn") String executionArn,
                                         @JsonProperty("distributionName") DistributionName distributionName,
                                         @JsonProperty("objectIdentifier") ObjectIdentifier objectIdentifier,
                                         @JsonProperty("platformDistributionIdentifier") ObjectIdentifier platformDistributionIdentifier,
                                         @JsonProperty("platformDistributionName") DistributionName platformDistributionName) {
        this.sfnArn = sfnArn;
        this.executionArn = executionArn;
        this.distributionName = distributionName;
        this.objectIdentifier = objectIdentifier;

        this.platformDistributionIdentifier = platformDistributionIdentifier;
        this.platformDistributionName = platformDistributionName;
    }

    public String getSfnArn() {
        return sfnArn;
    }

    public String getExecutionArn() {
        return executionArn;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public ObjectIdentifier getPlatformDistributionIdentifier() {
        return platformDistributionIdentifier;
    }

    public DistributionName getPlatformDistributionName() {
        return platformDistributionName;
    }
}
