/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class GetDeployOriginDataRequest {
    private final String sfnArn;
    private final String executionArn;

    @JsonCreator
    public GetDeployOriginDataRequest(@JsonProperty("sfnArn") String sfnArn,
                                      @JsonProperty("executionArn") String executionArn) {
        this.sfnArn = sfnArn;
        this.executionArn = executionArn;
    }

    public String getSfnArn() {
        return sfnArn;
    }

    public String getExecutionArn() {
        return executionArn;
    }
}
