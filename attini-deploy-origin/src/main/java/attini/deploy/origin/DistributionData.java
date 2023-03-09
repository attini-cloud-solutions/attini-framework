/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;

import attini.deploy.origin.config.AttiniConfig;

public class DistributionData {

    private final AttiniConfig attiniConfig;
    private final String artifactPath;
    private final String templateMd5Hex;

    public DistributionData(AttiniConfig attiniConfig,
                            String artifactPath,
                            String templateMd5Hex) {
        this.attiniConfig = requireNonNull(attiniConfig, "attiniConfig");
        this.artifactPath = requireNonNull(artifactPath, "artifactPath");
        this.templateMd5Hex = templateMd5Hex;
    }

    public AttiniConfig getAttiniConfig() {
        return attiniConfig;
    }

    public String getArtifactPath() {
        return artifactPath;
    }

    public Optional<String> getTemplateMd5Hex() {
        return Optional.ofNullable(templateMd5Hex);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DistributionData that = (DistributionData) o;
        return Objects.equals(attiniConfig, that.attiniConfig) && Objects.equals(artifactPath,
                                                                                 that.artifactPath) && Objects.equals(
                templateMd5Hex,
                that.templateMd5Hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attiniConfig, artifactPath, templateMd5Hex);
    }

    @Override
    public String toString() {
        return "DistributionData{" +
               "attiniConfig=" + attiniConfig +
               ", artifactPath='" + artifactPath + '\'' +
               ", templateMd5Hex='" + templateMd5Hex + '\'' +
               '}';
    }
}
