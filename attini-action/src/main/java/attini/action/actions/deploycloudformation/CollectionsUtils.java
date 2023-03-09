/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class CollectionsUtils {

    public static <T> List<T> combineCollections(Collection<T> list1, Collection<T> list2) {
        return Stream.concat(list1.stream(), list2.stream()).toList();
    }
}
