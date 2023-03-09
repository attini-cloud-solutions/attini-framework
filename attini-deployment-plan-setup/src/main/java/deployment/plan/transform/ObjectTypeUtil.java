/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import java.util.Map;

public class ObjectTypeUtil {

    public static boolean isMap(Object object){
        return object instanceof Map;
    }

}
