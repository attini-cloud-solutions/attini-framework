/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.action;

public class StackConfigException extends RuntimeException{

    public StackConfigException(String message) {
        super(message);
    }

    public StackConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
