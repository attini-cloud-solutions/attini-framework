/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard.stackdata;

public class StackDataNotFoundException extends RuntimeException {
    public StackDataNotFoundException(String msg) {
        super(msg);
    }
}
