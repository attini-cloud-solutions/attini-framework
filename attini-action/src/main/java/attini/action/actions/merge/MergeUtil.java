/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.merge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MergeUtil {

    public static Map<String, Object> merge(List<Map<String, Object>> maps) {
        return maps.stream()
                   .reduce(MergeUtil::merge)
                   .orElse(Collections.emptyMap());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> merge(Map<String, Object> map1, Map<String, Object> map2) {
        HashMap<String, Object> resultMap = new HashMap<>(map2);
        map1.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .forEach((entry) -> resultMap.merge(entry.getKey(), entry.getValue(), (o1, o2) -> {
                if (o1 instanceof Map && o2 instanceof Map) {
                    return merge((Map<String, Object>) o1, (Map<String, Object>) o2);
                } else if (o1 instanceof List && o2 instanceof List) {
                    return Stream.of((List<Object>) o1, (List<Object>) o2)
                                 .flatMap(Collection::stream)
                                 .distinct()
                                 .collect(Collectors.toList());
                } else {
                    return o1;
                }
            }));
        return resultMap;
    }


}
