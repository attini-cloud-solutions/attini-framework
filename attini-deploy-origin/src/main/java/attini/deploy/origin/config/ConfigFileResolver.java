/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin.config;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import attini.deploy.origin.system.EnvironmentVariables;

public class ConfigFileResolver {

    private static final Logger logger = Logger.getLogger(ConfigFileResolver.class);

    private final EnvironmentVariables environmentVariables;

    public ConfigFileResolver(EnvironmentVariables environmentVariables) {
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public List<String> getAttiniConfigFiles(Path zipDir) {
        String attiniConfigFile = environmentVariables.getAttiniConfigFile();
        Set<String> validConfigNames = Set.of(attiniConfigFile + ".yml",
                                              attiniConfigFile + ".yaml",
                                              attiniConfigFile + ".json");
        return Arrays.stream(requireNonNull(zipDir.toFile().list()))
                     .filter(validConfigNames::contains)
                     .peek(s -> logger.info("found attini-config file: " + s))
                     .collect(Collectors.toList());
    }
}
