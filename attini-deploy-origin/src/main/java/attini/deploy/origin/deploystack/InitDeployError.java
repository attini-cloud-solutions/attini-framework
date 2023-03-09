/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.deploy.origin.deploystack;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

public class InitDeployError {

    private final String errorCode;
    private final String errorMessage;

    public InitDeployError(String errorCode, String errorMessage) {
        this.errorCode = requireNonNull(errorCode, "errorCode");
        this.errorMessage = requireNonNull(errorMessage, "errorMessage");
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InitDeployError that = (InitDeployError) o;
        return Objects.equals(errorCode, that.errorCode) && Objects.equals(errorMessage,
                                                                           that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, errorMessage);
    }
}
