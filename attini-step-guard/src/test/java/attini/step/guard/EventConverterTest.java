package attini.step.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.step.guard.cloudformation.CloudFormationManualTriggerEvent;
import attini.step.guard.cloudformation.CloudFormationSnsEventImpl;
import attini.step.guard.manualapproval.ManualApprovalEvent;

class EventConverterTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createManualTriggerSnsEvent() throws JsonProcessingException {
        String event = """
                {
                    "payload": {
                        "stackName": "demo-database",
                        "resourceStatus": "UPDATE_COMPLETE",
                        "logicalResourceId": "demo-database",
                        "resourceType": "AWS::CloudFormation::Stack",
                        "region": null,
                        "executionRoleArn": null,
                        "stackId": null,
                        "sfnResponseToken": "AQCwAAAAKgAAAAMAAAAAAAAAAVVf1HAobaCSbwN3dih4BdI8bQXidWMsZyEFd4a+9vnRqivGR/cQ1eg9v721mZ0liCinAL9664sSKLK56UmSWZQwj0RIvI0q/NBKZB2Edyuue5BqD32YEWBw+l6JIEfOlpFS+RJMzEYWJd+dgb7j58Zw9A==tBwilWO27ke7OrrR+dydmivcQaWx8LyFM/yYuPgJGTtyjJjBrmwP0DnaMARsvp8RjD7u3T4E7apdytvZavx0/2z+DtJPK9CMbMolT+tuAgTFXg9qkJ4bDjId5tAgclfLy9YrvRnRJQaphTI2QhAB7XBAHF6y6nU0wZQK7Bk+sJo6yCB9e/TAXJTuici2H9L56UGv5+liKAP5rWMs80TP7kIwI73MFMeDK3DuX2ZEhRBgQK2h87P1/fAKr4wf15nYc7ItbM3d1zs5qUqK31vMqg2MjPngW2ZF+fcKgBVYUBAluF/aCMCXUwum/e477GgnpESIfbttPTvvxu3m8BSUAONXwJzKuQ46L2Ey6NlCjaKJOu8wYLCC2cm6Tvwf5GgwrgnB/xnBhvCuYgps4X2xOYWEKIfjFZg4NrRMrI6tH/1nmGEOE8gz6XtYBb9D/8aziergMc4lJ4Zj7Ek3PkqlPlkjSoCMKtF9KBeU+/iDmt1I8tQ5RXQqa48F639302m/Ns2h+n9ZYCwbPUzFlAbm",
                        "stepName": "deployDatabase",
                        "outputPath": null,
                        "environment": "dev",
                        "distributionId": "cf007f98-69e3-4689-87d5-1d7cc04bec83",
                        "distributionName": "cdk-demo",
                        "objectIdentifier": "dev/cdk-demo/cdk-demo.zip#kD6pD1px5vO5vXzayc_2TgliOmX0yjV9",
                        "clientRequestToken": "b9a3a4fc-5d4c-4d54-9d65-01a3a34e8fef1676991754048",
                        "desiredState": "DEPLOYED"
                    },
                    "requestType": "manualTrigger"
                }
                """;

        CloudFormationManualTriggerEvent manualTriggerInput = EventConverter.createManualTriggerInput(objectMapper.readTree(
                event));

        assertEquals("demo-database", manualTriggerInput.getStackName());
        assertEquals("demo-database", manualTriggerInput.getLogicalResourceId());
        assertEquals("UPDATE_COMPLETE", manualTriggerInput.getResourceStatus());
        assertEquals("deployDatabase", manualTriggerInput.getStepName());


    }


    @Test
    public void manualApprovalEvent() throws JsonProcessingException {
        String event = """
                {
                    "Records": [
                        {
                            "EventSource": "aws:sns",
                            "EventVersion": "1.0",
                            "EventSubscriptionArn": "arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event:8bbca047-0ded-4d99-9ac0-10a20796427d",
                            "Sns": {
                                "Type": "Notification",
                                "MessageId": "17988c72-cb30-55c4-bf36-d0f17b9fa7f8",
                                "TopicArn": "arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event",
                                "Subject": null,
                                "Message": "{\\"environment\\":\\"dev\\",\\"distributionName\\":\\"attini-manual-approval-demo\\",\\"stepName\\":\\"ManualApproval\\",\\"abort\\":false,\\"sfnToken\\":\\"AQCoAAAAKgAAAAMAAAAAAAAAAW2P3a354VPrSg9mIdeJnCpdwHP/BweTQbMHUqqObHsTK0JEZUaNC/+vNXE3B6gMt+aSio1QLwEP1iNgavYVUhZ74W0jBEPZ015KDAw8NGU+/jhM46jvCrDmXWgP2zH8qAVKfbTLMxag9zGg0GoTxzTXESIPISeWCsLdifftnXKzKKYtCoF15Z2eN2IUGq6T6yv7K/V+cLbSs/ep06mGZamKdBXxzEcsvmnEpQePZDdtMm6lziMSgCFvUr+Yq1HD+iQE94a52i5i9Wy4mEOPgSsOvWUhU7k8d4Vqi+objUFTJF5DnKmzIjackBmW1ga3mo7uBc31KISf75wU5hilL7llm9kcUX7haYdQs+5k1lcx486F53NU9x++MYvmS8/7gHsHhXwVcXGehO84Kra3WksdGVC4Xweei9fVA5ai7/xLP8hv9RO5fl9AC6EZL6E7nutaJryJUSLLY25MoSY8flNsLGRE4y1iVq96gXLU2Ns+3tTxfVdfwNlnDyTsG/IktkS3GIruAWSCsyP8TfKN5VqQsu0t0NNYnVPnrGaPyS0zUF4tD2xHZLiOWIlfF6yxXqJW6cuVdDboJs6y9VxCpNqfrmm9VtXu4R0o6REfJ7uCBQ3EnSwtBsmLMTEs/yd2EfK3SxCjj+8Wdv3gcFT2ptI5EG9biW85yDp6O8Z+\\"}",
                                "Timestamp": "2023-03-09T10:01:19.606Z",
                                "SignatureVersion": "1",
                                "Signature": "fQ4MjDWuPGdU4gp34/k9XEhFQbbIBj11Y+/WAeh33SnofcNFQzR8dW75mDbl959wk+CEZYAdxnnqbtwke0YlDSMTgh6SoHiNLwd+Y/h/SUJdad/Th931sKzZgi2VXOqhAJKV6FaBgtOJgWQ15+iVMMSfNBNJlsuJUPeG0cxXWY7rN9ngc6dTcnmZiteweXC5uFE5HlLdGkhXCVeD7dgnecTFowQWaHtwBR9S4ptPS7yCQTIQpo7q0/93U4vjj4rj4SEVltkRw8+MWxj0EIrxUt85p7hQWz+H7XLcGCgyZ5idXSGSA14uZHFqEFO0Rw9os8AF4pLZlhqosyvuYHAoWw==",
                                "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
                                "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event:8bbca047-0ded-4d99-9ac0-10a20796427d",
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

        ManualApprovalEvent manualApprovalEvent = EventConverter.createManualApprovalEvent(objectMapper.readTree(event));

        assertEquals("attini-manual-approval-demo",manualApprovalEvent.getDistributionName().asString());
    }

    @Test
    void createCfnSnsEvent() throws JsonProcessingException {
            String event = """
                    {
                        "Records": [
                            {
                                "EventSource": "aws:sns",
                                "EventVersion": "1.0",
                                "EventSubscriptionArn": "arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event:8bbca047-0ded-4d99-9ac0-10a20796427d",
                                "Sns": {
                                    "Type": "Notification",
                                    "MessageId": "8b747db3-f80b-5b3b-8828-21dd3b8fbcd4",
                                    "TopicArn": "arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event",
                                    "Subject": "AWS CloudFormation Notification",
                                    "Message": "StackId='arn:aws:cloudformation:eu-west-1:855066048591:stack/demo-database/2d3488a0-b1e8-11ed-8baf-06885360a369'\\nTimestamp='2023-02-21T13:04:04.398Z'\\nEventId='37bdd470-b1e8-11ed-9a9a-066cfd01808d'\\nLogicalResourceId='demo-database'\\nNamespace='855066048591'\\nPhysicalResourceId='arn:aws:cloudformation:eu-west-1:855066048591:stack/demo-database/2d3488a0-b1e8-11ed-8baf-06885360a369'\\nPrincipalId='AROA4OFPIPBH2YIOGW7TU:attini-action'\\nResourceProperties='null'\\nResourceStatus='CREATE_COMPLETE'\\nResourceStatusReason=''\\nResourceType='AWS::CloudFormation::Stack'\\nStackName='demo-database'\\nClientRequestToken='10b76aa9-15c1-4ddd-8fe9-2c08bc06f9421676984626637'\\n",
                                    "Timestamp": "2023-02-21T13:04:04.437Z",
                                    "SignatureVersion": "1",
                                    "Signature": "SYOjg6+G6kHKsoXYJ/eGNK8LLAeifCHhnSautOMgRxEB3RqStwmumm7DaD/C5l9jZ3QGI4+NuAJnlUSXl1NTJHAKP+04pFXDbyLc3C7P6NvDg1+Hy5nCbHwhcgNJa1xcIz4yIKibXy09BiLnKRXpM9CtM0qB3iJftZRUfXddB0B5+a9MoCEIB/D5Eg/rNiGhrrfnEmtZGuHuGIJYaOWgOUl1W7LBWh81WEG91egGCFFscX3Cvf9SEEPwF3U/r5L+j53aCqbKOMN54SuOCa7n8jNF9MYSmizfIweWdsPW5yp2v/5ZCEQE7PzUBvE0foyNdvg1+9U2QRJhEgjtkDHjuA==",
                                    "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-56e67fcb41f6fec09b0196692625d385.pem",
                                    "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:855066048591:attini-respond-to-cfn-event:8bbca047-0ded-4d99-9ac0-10a20796427d",
                                    "MessageAttributes": {}
                                }
                            }
                        ]
                    }
                    """;

        CloudFormationSnsEventImpl snsEvent = EventConverter.createSnsEvent(objectMapper.readTree(event));

        assertEquals("demo-database", snsEvent.getLogicalResourceId());
        assertEquals("demo-database", snsEvent.getStackName());

    }
}
