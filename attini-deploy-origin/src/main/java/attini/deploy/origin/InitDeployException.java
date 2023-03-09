/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

public class InitDeployException extends RuntimeException {

    public InitDeployException(String message) {
        super(message);
    }

    public InitDeployException(String message, Throwable cause) {
        super(message, cause);
    }
}
