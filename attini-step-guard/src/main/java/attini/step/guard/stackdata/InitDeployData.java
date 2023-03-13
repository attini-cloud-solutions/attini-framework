/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.List;

import attini.domain.DistributionContext;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class InitDeployData implements DistributionContext {

    private final List<String> sfnArns;
    private final ObjectIdentifier objectIdentifier;
    private final Environment environment;
    private final DistributionName distributionName;
    private final List<String> runners;
    private final DistributionId distributionId;


    public InitDeployData(List<String> sfnArns,
                          ObjectIdentifier objectIdentifier,
                          Environment environment,
                          DistributionName distributionName,
                          List<String> runners, DistributionId distributionId) {
        this.sfnArns = requireNonNull(sfnArns, "sfnArns");
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.environment = requireNonNull(environment, "environment");
        this.distributionName = requireNonNull(distributionName, "distributionName");
        this.runners = requireNonNull(runners, "runners");
        this.distributionId = requireNonNull(distributionId, "distributionId");
    }

    public List<String> getSfnArns() {
        return sfnArns;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public DistributionName getDistributionName() {
        return distributionName;
    }

    @Override
    public DistributionId getDistributionId() {
        return distributionId;
    }

    public List<String> getRunners() {
        return runners;
    }
}
