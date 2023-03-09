/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.services.cloudformation.model.Parameter;

class ParametersUtilTest {


    @Test
    void removeUnusedParams() {
        List<Parameter> allParams = List.of(createParam("key1", "value1"),
                                            createParam("key2", "value2"));

        Set<String> neededParams = Set.of("key1");
        List<Parameter> attiniFrameworkParameters = List.of(Parameter.builder()
                                                                     .parameterKey("AttiniEnvName")
                                                                     .parameterValue("Dev")
                                                                     .build());
        List<Parameter> parameters = ParametersUtil.removeUnusedParams(neededParams, allParams, attiniFrameworkParameters);
        assertEquals(parameters.get(0).parameterValue(), "value1");
        assertEquals(parameters.size(), 1);


    }

    private static Parameter createParam(String key, String value) {
        return Parameter.builder().parameterKey(key).parameterValue(value).build();
    }
}
