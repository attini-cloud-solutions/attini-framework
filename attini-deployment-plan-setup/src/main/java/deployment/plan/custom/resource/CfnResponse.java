/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CfnResponse {
    private final String stackId;
    private final String requestId;
    private final String logicalResourceId;
    private final String physicalResourceId;
    private final String reason;
    private final String status;

    private CfnResponse(Builder builder) {
        this.stackId = requireNonNull(builder.stackId, "stackId");
        this.requestId = requireNonNull(builder.requestId, "requestId");
        this.logicalResourceId = requireNonNull(builder.logicalResourceId, "logicalResourceId");
        this.physicalResourceId = requireNonNull(builder.physicalResourceId, "physicalResourceId");
        this.status = requireNonNull(builder.status, "status");
        this.reason = builder.reason;
    }

    public static Builder builder() {
        return new Builder();
    }


    public String getStackId() {
        return stackId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getLogicalResourceId() {
        return logicalResourceId;
    }

    public String getPhysicalResourceId() {
        return physicalResourceId;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String toJsonString(){
        final ObjectNode jsonResponse = new ObjectMapper().createObjectNode();
        jsonResponse.put("Status", requireNonNull(this.status, "stackId"));
        jsonResponse.put("StackId", requireNonNull(this.stackId, "stackId"));
        jsonResponse.put("RequestId", requireNonNull(this.requestId, "requestId"));
        jsonResponse.put("LogicalResourceId", requireNonNull(this.logicalResourceId, "logicalResourceId"));
        jsonResponse.put("PhysicalResourceId", requireNonNull(this.physicalResourceId, "physicalResourceId"));
        jsonResponse.put("Reason", this.reason);
        return jsonResponse.toPrettyString();
    }

    public static class Builder {
        private String stackId;
        private String requestId;
        private String logicalResourceId;
        private String physicalResourceId;
        private String reason;
        private String status;

        private Builder() {
        }

        public Builder setStackId(String stackId) {
            this.stackId = stackId;
            return this;
        }

        public Builder setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder setLogicalResourceId(String logicalResourceId) {
            this.logicalResourceId = logicalResourceId;
            return this;
        }

        public Builder setPhysicalResourceId(String physicalResourceId) {
            this.physicalResourceId = physicalResourceId;
            return this;
        }

        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder of(CfnResponse cfnResponse) {
            this.stackId = cfnResponse.stackId;
            this.requestId = cfnResponse.requestId;
            this.logicalResourceId = cfnResponse.logicalResourceId;
            this.physicalResourceId = cfnResponse.physicalResourceId;
            this.reason = cfnResponse.reason;
            this.status = cfnResponse.status;
            return this;
        }

        public CfnResponse build() {
            return new CfnResponse(this);
        }
    }
}
