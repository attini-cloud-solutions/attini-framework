package attini.step.guard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.step.guard.manualapproval.ContinueExecutionService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

@ExtendWith(MockitoExtension.class)
class ContinueExecutionServiceTest {

    @Mock
    private DynamoDbClient dynamoDbClient;
    @Mock
    private StepFunctionFacade stepFunctionFacade;
    @Mock
    private EnvironmentVariables environmentVariables;

    ContinueExecutionService continueExecutionService;

    @BeforeEach
    void setUp() {
        continueExecutionService = new ContinueExecutionService(dynamoDbClient,
                                                                stepFunctionFacade,
                                                                environmentVariables);
    }

    @Test
    void shouldContinue() {

        String token = "some-token";
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder()
                                           .item(Map.of("sfnToken",
                                                        AttributeValue.builder()
                                                                      .s(token)
                                                                      .build()))
                                           .build());

        continueExecutionService.continueExecution(StepGuardInputBuilder.aManualApprovalEvent()
                                                                        .abort(false)
                                                                        .sfnToken(token)
                                                                        .build());

        verify(stepFunctionFacade).sendTaskSuccess(token,"{}");

    }

    @Test
    void shouldAbort() {

        String token = "some-token";
        when(dynamoDbClient.getItem(any(GetItemRequest.class)))
                .thenReturn(GetItemResponse.builder()
                                           .item(Map.of("sfnToken",
                                                        AttributeValue.builder()
                                                                      .s(token)
                                                                      .build()))
                                           .build());

        continueExecutionService.continueExecution(StepGuardInputBuilder.aManualApprovalEvent()
                                                                        .abort(true)
                                                                        .sfnToken(token)
                                                                        .build());

        verify(stepFunctionFacade).sendTaskFailure(token,"ManuallyAborted","aborted by user");

    }

}
