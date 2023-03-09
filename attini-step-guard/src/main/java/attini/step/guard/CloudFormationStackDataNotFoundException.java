/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

public class CloudFormationStackDataNotFoundException extends RuntimeException {
    public CloudFormationStackDataNotFoundException(String msg) {
        super(msg);
    }
}