/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.facades.deployorigin;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.system.EnvironmentVariables;
import attini.domain.ObjectIdentifier;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@ExtendWith(MockitoExtension.class)
class DeployOriginFacadeTest {

    DeployOriginFacade deployOriginFacade;

    @Mock
    DynamoDbClient dynamoDbClient;

    @Mock
    EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
        deployOriginFacade = new DeployOriginFacade(dynamoDbClient,environmentVariables);
        when(environmentVariables.getDeployOriginTableName()).thenReturn("AttiniDeployData");
    }

    @Disabled
    @Test
    public void setSfnExecutionArn(){
        deployOriginFacade.setSfnExecutionArn("my.arn", ObjectIdentifier.of("my.arn.123232323"), "My tag");
    }
}
