package attini.step.guard.stackdata;

import attini.domain.DistributionName;
import attini.domain.Environment;
import attini.domain.ObjectIdentifier;

public interface ResourceState {


    ObjectIdentifier getObjectIdentifier();
    Environment getEnvironment();
    DistributionName getDistributionName();
}
