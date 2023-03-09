/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.custom.resource;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CustomResourceResponse {
    private final String stackId;
    private final String requestId;
    private final String logicalResourceId;
    private final String physicalResourceId;
    private final String reason;
    private final String status;
    private final Map<String, String> data;

    private CustomResourceResponse(Builder builder) {
        this.stackId = requireNonNull(builder.stackId, "stackId");
        this.requestId = requireNonNull(builder.requestId, "requestId");
        this.logicalResourceId = requireNonNull(builder.logicalResourceId, "logicalResourceId");
        this.physicalResourceId = requireNonNull(builder.physicalResourceId, "physicalResourceId");
        this.status = requireNonNull(builder.status, "status");
        this.reason = builder.reason;
        this.data = builder.data;
    }

    public static Builder builder() {
        return new Builder();
    }



    public String getStatus() {
        return status;
    }

    public String toJsonString(){
        ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode jsonResponse = objectMapper.createObjectNode();
        jsonResponse.put("Status", requireNonNull(this.status, "status"));
        jsonResponse.put("StackId", requireNonNull(this.stackId, "stackId"));
        jsonResponse.put("RequestId", requireNonNull(this.requestId, "requestId"));
        jsonResponse.put("LogicalResourceId", requireNonNull(this.logicalResourceId, "logicalResourceId"));
        jsonResponse.put("PhysicalResourceId", requireNonNull(this.physicalResourceId, "physicalResourceId"));
        jsonResponse.put("Reason", this.reason);
        if (data != null && !data.isEmpty()){
            jsonResponse.set("Data", objectMapper.convertValue(this.data, JsonNode.class));
        }

        return jsonResponse.toPrettyString();
    }

    public static CustomResourceResponse.Builder failedResponse(JsonNode inputJson){
        return CustomResourceResponse.builder()
                                      .setRequestId(inputJson.get("RequestId").asText())
                                      .setLogicalResourceId(inputJson.get("LogicalResourceId").asText())
                                      .setPhysicalResourceId(inputJson.get("LogicalResourceId").asText())
                                      .setStackId(inputJson.get("StackId").asText())
                                      .setStatus("FAILED");
    }

    public static CustomResourceResponse.Builder successResponse(JsonNode inputJson){
        return CustomResourceResponse.builder()
                                     .setRequestId(inputJson.get("RequestId").asText())
                                     .setLogicalResourceId(inputJson.get("LogicalResourceId").asText())
                                     .setPhysicalResourceId(inputJson.get("LogicalResourceId").asText())
                                     .setStackId(inputJson.get("StackId").asText())
                                     .setStatus("SUCCESS")
                                     .setReason("SUCCESS");
    }

    public static class Builder {
        private String stackId;
        private String requestId;
        private String logicalResourceId;
        private String physicalResourceId;
        private String reason;
        private String status;
        private Map<String, String> data;

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


        public Builder addData(String key, String value){
            if (data == null){
                data = new HashMap<>();
            }
            data.put(key, value);
            return this;
        }

        public CustomResourceResponse build() {
            return new CustomResourceResponse(this);
        }
    }
}
