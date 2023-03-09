/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.cloudformation.model.Parameter;

public class ParametersUtil {

    private static final Logger logger = Logger.getLogger(ParametersUtil.class);

    public static List<Parameter> removeUnusedParams(Set<String> neededParams,
                                                     Collection<Parameter> incomingParameters,
                                                     List<Parameter> attiniFrameworkParameters) {
        return incomingParameters.stream()
                                 .filter(isNeeded(neededParams, attiniFrameworkParameters))
                                 .collect(Collectors.toList());


    }

    public static Parameter toParameter(String key, String value) {
        return Parameter.builder().parameterKey(key).parameterValue(value).build();
    }


    private static Predicate<Parameter> isNeeded(Set<String> neededParams,
                                                 List<Parameter> attiniFrameworkParameters) {
        return parameter -> {
            boolean isPresent = neededParams.contains(parameter.parameterKey());
            if (!isPresent && !attiniFrameworkParameters.stream()
                                                         .map(Parameter::parameterKey)
                                                         .collect(Collectors.toSet())
                                                         .contains(parameter.parameterKey())) {
                logger.info(String.format("Removing parameter [%s] because its missing in template",
                                          parameter.parameterKey()));
            }
            return isPresent;
        };
    }
}
