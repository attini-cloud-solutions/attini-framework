package attini.domain;

public class DeployOriginDataTestBuilder {


    public static DeployOriginData.Builder aDeployOriginData() {
        return DeployOriginData.builder()
                        .environment(Environment.of("dev"))
                        .deploySource(new DeployOriginData.DeploySource("some/prefix", "some-bucket"))
                        .deployName("dev-infra")
                        .distributionId(DistributionId.of("some-dist-id"))
                        .distributionName(DistributionName.of("infra"))
                        .objectIdentifier(ObjectIdentifier.of("some-object-id"))
                        .deployTimeInEpoch(1666777560935L)
                        .stackName("some-init-stack");
    }
}
