/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard.cloudformation;

import java.util.Objects;

public class StackError {
    private final String message;
    private final String resourceId;
    private final String errorStatus;

    private StackError(Builder builder) {
        this.message = builder.message;
        this.resourceId = builder.resourceId;
        this.errorStatus = builder.errorStatus;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getMessage() {
        return message;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getErrorStatus() {
        return errorStatus;
    }

    public static StackError defaultError() {
        return StackError.builder()
                         .setMessage("could not resolve error, check cloudformation logs for more info")
                         .setResourceId("Unknown")
                         .setErrorStatus("Unknown")
                         .build();
    }

    public static class Builder {
        private String message;
        private String resourceId;
        private String errorStatus;

        private Builder() {
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setResourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder setErrorStatus(String errorStatus) {
            this.errorStatus = errorStatus;
            return this;
        }

        public Builder of(StackError stackError) {
            this.message = stackError.message;
            this.resourceId = stackError.resourceId;
            this.errorStatus = stackError.errorStatus;
            return this;
        }

        public StackError build() {
            return new StackError(this);
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackError that = (StackError) o;
        return Objects.equals(message, that.message) && Objects.equals(resourceId,
                                                                       that.resourceId) && Objects.equals(
                errorStatus,
                that.errorStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, resourceId, errorStatus);
    }

    @Override
    public String toString() {
        return "StackError{" +
               "message='" + message + '\'' +
               ", resourceId='" + resourceId + '\'' +
               ", errorStatus='" + errorStatus + '\'' +
               '}';
    }
}
