/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import attini.action.actions.merge.MergeUtil;

public class MergeUtilTest {


    @Test
    void merge() {
        Map<String, Object> map1 = Map.of("StringValue",
                                          "SomeValue",
                                          "StringList", List.of("listItem2"),
                                          "MapValue",
                                          Map.of("String1", "String1", "AnotherMap", Map.of("String4", "String4")));
        Map<String, Object> map2 = Map.of("StringValue",
                                          "SomeValue",
                                          "StringList", List.of("listItem1"),
                                          "MapValue",
                                          Map.of("String2",
                                                 "String2",
                                                 "String1",
                                                 "String1",
                                                 "AnotherMap",
                                                 Map.of("String3", "String3")));

        Map<String, Object> expectedResult = Map.of("StringValue",
                                                    "SomeValue",
                                                    "StringList", List.of("listItem1", "listItem2"),
                                                    "MapValue",
                                                    Map.of("String2",
                                                           "String2",
                                                           "String1",
                                                           "String1",
                                                           "AnotherMap",
                                                           Map.of("String3", "String3", "String4", "String4")));
        Map<String, Object> merge = MergeUtil.merge(Arrays.asList(map1, map2));

        assertEquals(expectedResult, merge);


    }

    @Test
    void merge_shouldNotDuplicateListItems() {
        Map<String, Object> map1 = Map.of("StringValue",
                                          "SomeValue",
                                          "StringList", List.of("listItem2"),
                                          "MapValue",
                                          Map.of("String1", "String1", "AnotherMap", Map.of("String4", "String4")));
        Map<String, Object> map2 = Map.of("StringValue",
                                          "SomeValue",
                                          "StringList", List.of("listItem1", "listItem2"),
                                          "MapValue",
                                          Map.of("String2",
                                                 "String2",
                                                 "String1",
                                                 "String1",
                                                 "AnotherMap",
                                                 Map.of("String3", "String3")));

        Map<String, Object> expectedResult = Map.of("StringValue",
                                                    "SomeValue",
                                                    "StringList", List.of("listItem1", "listItem2"),
                                                    "MapValue",
                                                    Map.of("String2",
                                                           "String2",
                                                           "String1",
                                                           "String1",
                                                           "AnotherMap",
                                                           Map.of("String3", "String3", "String4", "String4")));
        Map<String, Object> merge = MergeUtil.merge(Arrays.asList(map1, map2));

        assertEquals(expectedResult, merge);


    }
}
