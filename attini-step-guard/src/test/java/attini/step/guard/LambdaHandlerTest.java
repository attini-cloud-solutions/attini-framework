/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LambdaHandlerTest {

    private static RespondToCfnEvent mock;

    @BeforeAll
    public static void setUp() {
        mock = Mockito.mock(RespondToCfnEvent.class);
        QuarkusMock.installMockForType(mock, RespondToCfnEvent.class);
    }

    @Test
    public void testSimpleLambdaSuccess() {
      given()
              .contentType("application/json")
                .accept("application/json")
                .body(input())
                .when()
                .post()
                .then()
                .statusCode(200)
                .body(containsString("Success"));
        verify(mock).respondToInitDeployCfnEvent(any(InitDeploySnsEvent.class));
    }

    private Map<String, Object> input(){
        Map<String, Object>  input =  new HashMap<>();
        Map<String, Object>  record =  new HashMap<>();
        Map<String, Object>  sns =  new HashMap<>();
        sns.put("TopicArn", "arn:aws:sns:eu-west-1:655047308345:attini-respond-to-init-deploy-cfn-event");
        sns.put("Message", "StackId='test'\nLogicalResourceId='MyFirstLambda'\nResourceType='AWS::CloudFormation::Stack'\nStackName='MyFirstLambda'\nResourceStatus='UPDATE_COMPLETE'\nClientRequestToken='test'");
        record.put("Sns", sns);
        List<Map<String, Object>> records = Arrays.asList(record);
        input.put("Records", records);
        return input;
    }

}
