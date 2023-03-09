package attini.action.actions.deploycloudformation.input;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomBooleanDeserializer;
import attini.domain.deserializers.CustomStringDeserializer;
import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CfnConfig(@JsonProperty("Variables")  @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> variables,
                        @JsonProperty("Parameters") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> parameters,
                        @JsonProperty("ConfigFile") @JsonDeserialize(using = CustomStringDeserializer.class) String configFile,
                        @JsonProperty("Action")  @JsonDeserialize(using = CustomStringDeserializer.class) String action,
                        @JsonProperty("Template") @JsonDeserialize(using = CustomStringDeserializer.class) String template,
                        @JsonProperty("StackName") @JsonDeserialize(using = CustomStringDeserializer.class) String stackName,
                        @JsonProperty("StackRoleArn") @JsonDeserialize(using = CustomStringDeserializer.class) String stackRoleArn,
                        @JsonProperty("ExecutionRoleArn") @JsonDeserialize(using = CustomStringDeserializer.class) String executionRoleArn,
                        @JsonProperty("Region") @JsonDeserialize(using = CustomStringDeserializer.class) String region,
                        @JsonProperty("OutputPath") @JsonDeserialize(using = CustomStringDeserializer.class) String outputPath,
                        @JsonProperty("OnFailure") @JsonDeserialize(using = CustomStringDeserializer.class) String onFailure,
                        @JsonProperty("EnableTerminationProtection")  @JsonDeserialize(using = CustomBooleanDeserializer.class) String enableTerminationProtection) {
}
