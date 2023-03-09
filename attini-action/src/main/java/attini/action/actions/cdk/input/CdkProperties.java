package attini.action.actions.cdk.input;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import attini.domain.deserializers.CustomBooleanDeserializer;
import attini.domain.deserializers.CustomStringDeserializer;
import attini.domain.deserializers.CustomStringListDeserializer;
import attini.domain.deserializers.CustomStringMapDeserializer;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CdkProperties(@JsonProperty("Path") @JsonDeserialize(using = CustomStringDeserializer.class) String path,
                            @JsonProperty("App") @JsonDeserialize(using = CustomStringDeserializer.class) String app,
                            @JsonProperty("Stacks") @JsonDeserialize(using = CustomStringListDeserializer.class) List<String> stacks,
                            @JsonProperty("Context") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String, String> context,
                            @JsonProperty("StackConfiguration") List<StackConfiguration> stackConfiguration,
                            @JsonProperty("Runner") String runner,
                            @JsonProperty("Build") @JsonDeserialize(using = CustomStringDeserializer.class) String build,
                            @JsonProperty("Plugins") @JsonDeserialize(using = CustomStringListDeserializer.class) List<String> plugins,
                            @JsonProperty("BuildExclude") @JsonDeserialize(using = CustomStringListDeserializer.class) List<String> buildExcludes,
                            @JsonProperty("NotificationArns") @JsonDeserialize(using = CustomStringListDeserializer.class) List<String> notificationArns,
                            @JsonProperty("Force") @JsonDeserialize(using = CustomBooleanDeserializer.class) String force,
                            @JsonProperty("Environment") @JsonDeserialize(using = CustomStringMapDeserializer.class) Map<String,String> environment,
                            @JsonProperty("RoleArn") @JsonDeserialize(using = CustomStringDeserializer.class) String roleArn) {
}
