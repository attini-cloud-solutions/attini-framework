package attini.step.guard.manualapproval;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;

import attini.step.guard.EnvironmentVariables;
import attini.step.guard.StepFunctionFacade;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class ContinueExecutionService {

    private static final Logger logger = Logger.getLogger(ContinueExecutionService.class);


    private final DynamoDbClient dynamoDbClient;
    private final StepFunctionFacade stepFunctionFacade;
    private final EnvironmentVariables environmentVariables;

    public ContinueExecutionService(DynamoDbClient dynamoDbClient,
                                    StepFunctionFacade stepFunctionFacade,
                                    EnvironmentVariables environmentVariables) {
        this.dynamoDbClient = requireNonNull(dynamoDbClient, "dynamoDbClient");
        this.stepFunctionFacade = requireNonNull(stepFunctionFacade, "sfnResponseSender");
        this.environmentVariables = requireNonNull(environmentVariables, "environmentVariables");
    }

    public void continueExecution(ManualApprovalEvent event) {

        logger.info("Manual approval event triggered");

        Map<String, AttributeValue> item =
                dynamoDbClient.getItem(GetItemRequest.builder()
                                                     .tableName(environmentVariables.getResourceStatesTableName())
                                                     .key(Map.of("resourceType",
                                                                 AttributeValue.builder()
                                                                               .s("ManualApproval")
                                                                               .build(),
                                                                 "name",
                                                                 AttributeValue.builder()
                                                                               .s(createName(event))
                                                                               .build()))
                                                     .build())
                              .item();

        if (event.getSfnToken().equals(item.get("sfnToken").s())) {
            if (event.isAbort()) {
                stepFunctionFacade.sendTaskFailure(event.getSfnToken(),
                                                   "ManuallyAborted",
                                                   event.getMessage() == null ? "aborted by user" : event.getMessage());

            } else {
                stepFunctionFacade.sendTaskSuccess(event.getSfnToken(),
                                                   event.getMessage() == null ? "{}" : "{\"message\":\"" + event.getMessage() + "\"}");
            }
        } else {
            logger.warn("Different sfn token in dynamo vs request, will not continue the step function execution");
        }

    }

    private static String createName(ManualApprovalEvent event) {
        return event.getEnvironment()
                    .asString() + "-" + event.getDistributionName()
                                             .asString() + "-" + event.getStepName();
    }

}
