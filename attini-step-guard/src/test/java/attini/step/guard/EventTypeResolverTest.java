package attini.step.guard;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class EventTypeResolverTest {

    private final EventTypeResolver eventTypeResolver = new EventTypeResolver();

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void shouldResolveToInitDeployManualEvent() throws JsonProcessingException {
        String event = """
                {
                    "stackName": "acc-website-init-deploy",
                    "requestType": "init-deploy-manual-trigger"
                }
                """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.INIT_DEPLOY_MANUAL_TRIGGER, eventType);
    }

    @Test
    void shouldResolveToCfnManualEvent() throws JsonProcessingException {
        String event = """
                {
                    "payload": {
                        "stackName": "acc-public-website",
                        "resourceStatus": "UPDATE_COMPLETE",
                        "logicalResourceId": "acc-public-website",
                        "resourceType": "AWS::CloudFormation::Stack",
                        "region": null,
                        "executionRoleArn": null,
                        "stackId": null,
                        "sfnResponseToken": "Some token",
                        "stepName": "StaticWebsite",
                        "outputPath": null,
                        "environment": "acc",
                        "distributionId": "96173d81e34a314006e281952cb8d46e38cd0d2b",
                        "distributionName": "website",
                        "objectIdentifier": "acc/website/website.zip#5c3favoo.8xQy3d9KvNCUAowtFG5kqD5",
                        "clientRequestToken": "5f17e50f-27a8-4b09-b4d6-fbd3c66f0ecc1673866193017",
                        "desiredState": "DEPLOYED"
                    },
                    "requestType": "manualTrigger"
                }
                                """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.CFN_MANUAL, eventType);
    }

    @Test
    void shouldResolveToCfnSnsEvent() throws JsonProcessingException {
        String event = """
                {
                    "Records": [
                        {
                            "EventSource": "aws:sns",
                            "EventVersion": "1.0",
                            "EventSubscriptionArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event:b0acf5e9-6060-40f1-af13-410e2ec4d30c",
                            "Sns": {
                                "Type": "Notification",
                                "MessageId": "f8a47081-c80c-5e21-9317-2cbee5e6a1b7",
                                "TopicArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event",
                                "Subject": "AWS CloudFormation Notification",
                                "Message": "StackId='arn:aws:cloudformation:eu-west-1:316360690372:stack/acc-public-website-backend/fd194e20-b613-11eb-8511-024e297a3331'\\nTimestamp='2023-01-16T10:49:14.590Z'\\nEventId='6af8dac0-958b-11ed-922e-06854a680515'\\nLogicalResourceId='acc-public-website-backend'\\nNamespace='316360690372'\\nPhysicalResourceId='arn:aws:cloudformation:eu-west-1:316360690372:stack/acc-public-website-backend/fd194e20-b613-11eb-8511-024e297a3331'\\nPrincipalId='AROAUTKERBLCJWHDBFTAT:attini-action'\\nResourceProperties='null'\\nResourceStatus='UPDATE_IN_PROGRESS'\\nResourceStatusReason='User Initiated'\\nResourceType='AWS::CloudFormation::Stack'\\nStackName='acc-public-website-backend'\\nClientRequestToken='5f17e50f-27a8-4b09-b4d6-fbd3c66f0ecc1673866154213'\\n",
                                "Timestamp": "2023-01-16T10:49:14.671Z",
                                "SignatureVersion": "1",
                                "Signature": "JPUnYOXqUMODdKdeqqfuq68rKd5+ONgK7S+PDU65/Qhc8WteELw83cdsBt0ttXo8oAnBcxrjLQaW1+gpyhLFjhIjgSA/pMGlQvP5ttBzQ9ta6UrSoqS8KjmC9F3RmZKzoxH/UTVSX7pf3bczp4vEatEeeQd1yyJaT7466RlFxB0utq3ho+QonBPcFABYwtbFlWxjNV4pi+sa557X1HavKzc+XWDyTn+uPtvnw8AnYG4C/O0NI5Nb1Euev33sTEIxDbo8lg6E3lLQ8bvEM70aungCDovc6n+vTUaX+dkq5CMylr3MWeSWCnVY91qYpHeIDODE8FyfwEO1ooMGpAWWRg==",
                                "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
                                "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event:b0acf5e9-6060-40f1-af13-410e2ec4d30c",
                                "MessageAttributes": {}
                            }
                        }
                    ]
                }
                """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.CFN_SNS, eventType);
    }


    @Test
    void shouldResolveToInitCfnSnsEvent() throws JsonProcessingException {
        String event = """
                {
                    "Records": [
                        {
                            "EventSource": "aws:sns",
                            "EventVersion": "1.0",
                            "EventSubscriptionArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-init-deploy-cfn-event:0f45a6d2-c807-4463-beb3-9e4968122023",
                            "Sns": {
                                "Type": "Notification",
                                "MessageId": "e28ed23e-c096-5784-bc78-8e7f6fceac9c",
                                "TopicArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-init-deploy-cfn-event",
                                "Subject": "AWS CloudFormation Notification",
                                "Message": "StackId='arn:aws:cloudformation:eu-west-1:316360690372:stack/acc-attini-runner-demo-deployment-plan/7ef87080-8cdf-11ed-9e95-0218d747b32d'\\nTimestamp='2023-01-18T08:26:00.701Z'\\nEventId='AttiniRunnerQueueCustomRunner-CREATE_IN_PROGRESS-2023-01-18T08:26:00.701Z'\\nLogicalResourceId='AttiniRunnerQueueCustomRunner'\\nNamespace='316360690372'\\nResourceProperties='{\\"ReceiveMessageWaitTimeSeconds\\":\\"20\\",\\"FifoQueue\\":\\"True\\",\\"VisibilityTimeout\\":\\"15\\",\\"KmsDataKeyReusePeriodSeconds\\":\\"86400\\"}'\\nResourceStatus='CREATE_IN_PROGRESS'\\nResourceStatusReason=''\\nResourceType='AWS::SQS::Queue'\\nStackName='acc-attini-runner-demo-deployment-plan'\\nClientRequestToken='E56CCAF2B11315DC7C142F6EC1260145'\\n",
                                "Timestamp": "2023-01-18T08:26:00.800Z",
                                "SignatureVersion": "1",
                                "Signature": "xEqKPEA51uVRWIoADgDmLzc1427fO7FsgNU11qTbDlSd9WiBRyYM3scIsTPf0LRyUytLi93LMQusNH2GNnVBANOz/lxjQItJP6QNxjAJhTXfrhLazrJ9RjhR5NnXCp0dGatBdVtcmJZxI28uLYh8vO5458SDki3kcQGHrICZ3MYd/rBf0guWP0Ef7VCHcQdwiozc5d1MI1Uk135f3Ezm3LQu6xMpjw32hi4g1v+Eln5z4ZzsgAwTy5rLkc306pBmA8XOqZbb0MvvqqokWyFOBFcNn20k93ZymOXFhEHtrToRrb9Y1I7UsUJWqR3Hpp8mnRPBkWLjRMVyHo2llYy0Gg==",
                                "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
                                "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:316360690372:attini-respond-to-init-deploy-cfn-event:0f45a6d2-c807-4463-beb3-9e4968122023",
                                "MessageAttributes": {}
                            }
                        }
                    ]
                }
                     """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.INIT_DEPLOY_CFN, eventType);
    }

    @Test
    void shouldResolveToManualApproval() throws JsonProcessingException {
        String event = """
                {
                    "Records": [
                        {
                            "EventSource": "aws:sns",
                            "EventVersion": "1.0",
                            "EventSubscriptionArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event:b0acf5e9-6060-40f1-af13-410e2ec4d30c",
                            "Sns": {
                                "Type": "Notification",
                                "MessageId": "56955871-93b0-51f9-9339-0435ff062944",
                                "TopicArn": "arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event",
                                "Subject": null,
                                "Message": "Message",
                                "Timestamp": "2023-01-20T10:18:30.425Z",
                                "SignatureVersion": "1",
                                "Signature": "kyvtmNPfm32GysDkgqiubKo5hYPv+vgoTbrF5kg8cWrBO90AfPAcpw2/uU+x46s0YvHq7yGENx1dNrNXBDg3V950URwQfJlfF8BeSXRGh+2eX67un8FfRZU5tSkAPFImVDhAgwN8nwhyNnBaGYCHCgr+MqfGmWbzyubNkPa0QWa0RwI5YR9Ubo+DkYAWG4qIWlpv2mF5+l3/T3b2pDQ0+Vi0/0UwDHiZOre+Zde03UYIHhFFYFMGvfV+rG5AnHxxQkk6H44j9p7WjGAx7Iih0pP85Vz2anly5yfCHZ442tF97ewpfpcfS1lcj7RVk/S5Gmz4RkiSvUsqYNLED+FXtQ==",
                                "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
                                "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:316360690372:attini-respond-to-cfn-event:b0acf5e9-6060-40f1-af13-410e2ec4d30c",
                                "MessageAttributes": {
                                    "type": {
                                        "Type": "String",
                                        "Value": "manualApprovalEvent"
                                    }
                                }
                            }
                        }
                    ]
                }
                      """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.MANUAL_APPROVAL, eventType);
    }

    @Test
    void shouldResolveToCdkStack() throws JsonProcessingException {
        String event = """
                {
                    "requestType": "register-cdk-stacks",
                    "objectIdentifier": "",
                    "distributionId": "2023-02-15_14:52:53",
                    "distributionName": "runner-test-1",
                    "environment": "dev",
                    "stepName": "CdkExample",
                    "stacks": [
                        {
                            "id": "CdkTestStack",
                            "name": "CdkTestStack",
                            "environment": {
                                "account": "unknown-account",
                                "region": "unknown-region",
                                "name": "aws://unknown-account/unknown-region"
                            }
                        }
                    ]
                }
                """;
        EventType eventType = eventTypeResolver.resolveEventType(objectMapper.readTree(event));

        assertEquals(EventType.CDK_REGISTER_STACKS, eventType);
    }

}
