package attini.step.guard;

import com.fasterxml.jackson.databind.JsonNode;

public class EventTypeResolver {

    public EventType resolveEventType(JsonNode input) {
        if (isManualTrigger(input)) {
            return EventType.CFN_MANUAL;
        }
        if (isInitDeployManualTrigger(input)) {
            return EventType.INIT_DEPLOY_MANUAL_TRIGGER;
        }
        if (isManualApproval(input)) {
            return EventType.MANUAL_APPROVAL;
        }
        if (isInitDeployCfnEvent(input)) {
            return EventType.INIT_DEPLOY_CFN;
        }

        if (isCdkManualTrigger(input)){
            return EventType.CDK_REGISTER_STACKS;
        }
        return EventType.CFN_SNS;
    }

    private static boolean isInitDeployCfnEvent(JsonNode input) {
        return input.path("Records")
                    .path(0)
                    .path("Sns")
                    .path("TopicArn")
                    .asText()
                    .contains("attini-respond-to-init-deploy-cfn-event");
    }

    private static boolean isManualApproval(JsonNode input) {
        return input.path("Records")
                    .path(0)
                    .path("Sns")
                    .path("MessageAttributes")
                    .has("type");
    }

    private static boolean isManualTrigger(JsonNode input) {
        return !input.path("requestType").isMissingNode() &&
               input.path("requestType")
                    .asText()
                    .equals("manualTrigger");
    }

    private static boolean isInitDeployManualTrigger(JsonNode input) {
        return !input.path("requestType").isMissingNode() && input.path("requestType")
                                                                  .asText()
                                                                  .equals("init-deploy-manual-trigger");
    }

    private static boolean isCdkManualTrigger(JsonNode input) {
        return !input.path("requestType").isMissingNode() && input.path("requestType")
                                                                  .asText()
                                                                  .equals("register-cdk-stacks");
    }
}
