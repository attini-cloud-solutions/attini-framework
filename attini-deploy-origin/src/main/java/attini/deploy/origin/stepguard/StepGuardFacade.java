package attini.deploy.origin.stepguard;

import static java.util.Objects.requireNonNull;

import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.deploy.origin.system.EnvironmentVariables;
import io.quarkus.runtime.annotations.RegisterForReflection;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

public class StepGuardFacade {

    private final LambdaClient lambdaClient;
    private final EnvironmentVariables environmentVariables;
    private final ObjectMapper objectMapper;

    public StepGuardFacade(LambdaClient lambdaClient,
                           EnvironmentVariables environmentVariables,
                           ObjectMapper objectMapper) {
        this.lambdaClient = requireNonNull(lambdaClient, "lambdaClient");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    public void respondToStepGuard(String stackName) {

        lambdaClient.invoke(InvokeRequest.builder()
                                         .functionName(environmentVariables.getStepGuardName())
                                         .payload(SdkBytes.fromUtf8String(createRequestString(stackName)))
                                         .build());

    }

    private String createRequestString(String stackName) {
        try {
            return objectMapper.writeValueAsString(new StepGuardRequest(stackName, "init-deploy-manual-trigger"));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @RegisterForReflection
    public record StepGuardRequest(String stackName, String requestType) {
    }
}
