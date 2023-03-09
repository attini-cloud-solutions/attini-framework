/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.domain;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DeployOriginData {

    private final DistributionName distributionName;
    private final long deployTimeInEpoch;
    private final DeploySource deploySource;
    private final Environment environment;
    private final DistributionId distributionId;
    private final String deployName;
    private final ObjectIdentifier objectIdentifier;
    private final String stackName;
    private final Map<String, String> distributionTags;
    private final Version version;
    private final boolean samPackaged;


    @JsonCreator
    public DeployOriginData(@JsonProperty("distributionName") DistributionName distributionName,
                            @JsonProperty("deploymentTime") long deployTimeInEpoch,
                            @JsonProperty("deploymentSource") DeploySource deploySource,
                            @JsonProperty("environment") Environment environment,
                            @JsonProperty("distributionId") DistributionId distributionId,
                            @JsonProperty("deploymentName") String deployName,
                            @JsonProperty("objectIdentifier") ObjectIdentifier objectIdentifier,
                            @JsonProperty("stackName") String stackName,
                            @JsonProperty("distributionTags") Map<String, String> distributionTags,
                            @JsonProperty("version") Version version,
                            @JsonProperty("samPackaged") boolean samPackaged) {

        this.distributionName = distributionName;
        this.deployTimeInEpoch = deployTimeInEpoch;
        this.deploySource = deploySource;
        this.environment = environment;
        this.distributionId = distributionId;
        this.deployName = deployName;
        this.objectIdentifier = objectIdentifier;
        this.stackName = stackName;
        this.distributionTags = distributionTags;
        this.version = version;
        this.samPackaged = samPackaged;
    }

    private DeployOriginData(Builder builder) {
        distributionName = builder.distributionName;
        deployTimeInEpoch = builder.deployTimeInEpoch;
        deploySource = builder.deploySource;
        environment = builder.environment;
        distributionId = builder.distributionId;
        deployName = builder.deployName;
        objectIdentifier = builder.objectIdentifier;
        stackName = builder.stackName;
        distributionTags = builder.distributionTags;
        version = builder.version;
        samPackaged = builder.samPackaged;
    }

    public static Builder builder() {
        return new Builder();
    }


    @JsonProperty("objectIdentifier")
    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    @JsonProperty("distributionName")
    public DistributionName getDistributionName() {
        return distributionName;
    }

    @JsonProperty("deploymentTime")
    public long getDeployTimeInEpoch() {
        return deployTimeInEpoch;
    }

    @JsonProperty("deploymentSource")
    public DeploySource getDeploySource() {
        return deploySource;
    }

    @JsonProperty("environment")
    public Environment getEnvironment() {
        return environment;
    }

    @JsonProperty("distributionId")
    public DistributionId getDistributionId() {
        return distributionId;
    }

    @JsonProperty("deploymentName")
    public String getDeployName() {
        return deployName;
    }


    @JsonProperty("stackName")
    public String getStackName() {
        return stackName;
    }

    @JsonProperty("distributionTags")
    public Map<String, String> getDistributionTags() {
        return distributionTags;
    }

    @JsonProperty("version")
    public Optional<Version> getVersion() {
        return Optional.ofNullable(version);
    }

    @JsonProperty("samPackaged")
    public boolean isSamPackaged() {
        return samPackaged;
    }


    @Override
    public String toString() {
        return "DeployOriginData{" +
               "distributionName=" + distributionName +
               ", deployTimeInEpoch=" + deployTimeInEpoch +
               ", deploySource=" + deploySource +
               ", environment=" + environment +
               ", distributionId=" + distributionId +
               ", deployName='" + deployName + '\'' +
               ", objectIdentifier=" + objectIdentifier +
               ", stackName='" + stackName + '\'' +
               ", distributionTags=" + distributionTags +
               ", version=" + version +
               ", samPackaged=" + samPackaged +
               '}';
    }

    @RegisterForReflection
    public static class
    DeploySource {
        private final String attiniDeploySourcePrefix;
        private final String attiniDeploySourceBucket;


        @JsonCreator
        public DeploySource( @JsonProperty("deploymentSourcePrefix") String attiniDeploySourcePrefix,
                             @JsonProperty("deploymentSourceBucket") String attiniDeploySourceBucket) {
            this.attiniDeploySourcePrefix = attiniDeploySourcePrefix;
            this.attiniDeploySourceBucket = attiniDeploySourceBucket;
        }

        @JsonProperty("deploymentSourcePrefix")
        public String getAttiniDeploySourcePrefix() {
            return attiniDeploySourcePrefix;
        }

        @JsonProperty("deploymentSourceBucket")
        public String getAttiniDeploySourceBucket() {
            return attiniDeploySourceBucket;
        }


        @Override
        public String toString() {
            return "DeploySource{" +
                   "attiniDeploySourcePrefix='" + attiniDeploySourcePrefix + '\'' +
                   ", attiniDeploySourceBucket='" + attiniDeploySourceBucket + '\'' +
                   '}';
        }
    }


    public static final class Builder {
        private DistributionName distributionName;
        private long deployTimeInEpoch;
        private DeploySource deploySource;
        private Environment environment;
        private DistributionId distributionId;
        private String deployName;
        private ObjectIdentifier objectIdentifier;
        private String stackName;
        private Map<String, String> distributionTags;
        private Version version;

        private boolean samPackaged;

        private Builder() {
        }

        public Builder distributionName(DistributionName val) {
            distributionName = val;
            return this;
        }

        public Builder deployTimeInEpoch(long val) {
            deployTimeInEpoch = val;
            return this;
        }

        public Builder deploySource(DeploySource val) {
            deploySource = val;
            return this;
        }

        public Builder environment(Environment val) {
            environment = val;
            return this;
        }

        public Builder distributionId(DistributionId val) {
            distributionId = val;
            return this;
        }

        public Builder deployName(String val) {
            deployName = val;
            return this;
        }

        public Builder objectIdentifier(ObjectIdentifier val) {
            objectIdentifier = val;
            return this;
        }

        public Builder stackName(String val) {
            stackName = val;
            return this;
        }

        public Builder distributionTags(Map<String, String> val) {
            distributionTags = val;
            return this;
        }

        public Builder version(Version val) {
            version = val;
            return this;
        }

        public Builder samPackaged(boolean val) {
            samPackaged = val;
            return this;
        }


        public DeployOriginData build() {
            return new DeployOriginData(this);
        }
    }
}

