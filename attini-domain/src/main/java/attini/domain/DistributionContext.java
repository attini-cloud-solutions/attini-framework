package attini.domain;

public interface DistributionContext {

    ObjectIdentifier getObjectIdentifier();
    Environment getEnvironment();
    DistributionName getDistributionName();
    DistributionId getDistributionId();

}
