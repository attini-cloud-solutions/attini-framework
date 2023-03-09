/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

import static java.util.Objects.requireNonNull;

import java.util.List;

import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public class InitDeployData implements ResourceState{

    private final List<String> sfnArns;
    private final ObjectIdentifier objectIdentifier;
    private final Environment environment;
    private final DistributionName distributionName;
    private final List<String> runners;



    public InitDeployData(List<String> sfnArns,
                          ObjectIdentifier objectIdentifier,
                          Environment environment,
                          DistributionName distributionName,
                          List<String> runners) {
        this.sfnArns = requireNonNull(sfnArns, "sfnArns");
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.environment = requireNonNull(environment, "environment");
        this.distributionName = requireNonNull(distributionName, "distributionName");
        this.runners = requireNonNull(runners, "runners");
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

    public List<String> getRunners() {
        return runners;
    }
}
