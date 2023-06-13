/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import deployment.plan.custom.resource.service.AppDeploymentService;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataRequest;
import deployment.plan.custom.resource.service.RegisterDeployOriginDataService;
import deployment.plan.custom.resource.service.RegisterDeploymentPlanTriggerService;

@ExtendWith(MockitoExtension.class)
class CustomResourceHandlerTest {

    @Mock
    RegisterDeploymentPlanTriggerService registerDeploymentPlanTriggerService;

    @Mock
    RegisterDeployOriginDataService registerDeployOriginDataService;

    @Mock
    CfnResponseSender responseSender;

    @Mock
    Context context;

    @Mock
    AppDeploymentService appDeploymentService;


    CustomResourceHandler customResourceHandler;

    @BeforeEach
    void setUp() {
        customResourceHandler = new CustomResourceHandler(registerDeploymentPlanTriggerService,
                                                          registerDeployOriginDataService,
                                                          responseSender, new ObjectMapper(),
                                                          appDeploymentService);
    }

    @Test
    public void handleCustomResource_shouldSendSuccessIfDeleteUnknownResource() throws IOException {
        JsonNode input = getInput("delete-resource.json");
        customResourceHandler.handleCustomResource(input, context);
        ArgumentCaptor<CfnResponse> cfnResponseArgumentCaptor = ArgumentCaptor.forClass(CfnResponse.class);
        verify(responseSender).sendResponse(anyString(), cfnResponseArgumentCaptor.capture());
        String status = cfnResponseArgumentCaptor.getValue().getStatus();
        assertEquals("SUCCESS", status);
    }

    @Test
    public void handleCustomResource_shouldSendFailedIfUpdateUnknownResource() throws IOException {
        JsonNode input = getInput("update-unknown-resource.json");
        customResourceHandler.handleCustomResource(input, context);
        ArgumentCaptor<CfnResponse> cfnResponseArgumentCaptor = ArgumentCaptor.forClass(CfnResponse.class);
        verify(responseSender).sendResponse(anyString(), cfnResponseArgumentCaptor.capture());
        String status = cfnResponseArgumentCaptor.getValue().getStatus();
        assertEquals("FAILED", status);
    }

    @Test
    public void handleCustomResource_shouldRegisterDeployPlan() throws IOException {
        JsonNode input = getInput("create-deploy-plan-resource.json");
        customResourceHandler.handleCustomResource(input, context);
        ArgumentCaptor<CfnResponse> cfnResponseArgumentCaptor = ArgumentCaptor.forClass(CfnResponse.class);
        verify(registerDeploymentPlanTriggerService).registerDeploymentPlanTrigger(input, CfnRequestType.CREATE);
        verify(registerDeployOriginDataService).registerDeployOriginData(any(RegisterDeployOriginDataRequest.class));
        verify(responseSender).sendResponse(anyString(), cfnResponseArgumentCaptor.capture());
        String status = cfnResponseArgumentCaptor.getValue().getStatus();
        assertEquals("SUCCESS", status);
    }

    @Test
    public void handleCustomResource_shouldRegisterDeployPlanOnUpdate() throws IOException {
        JsonNode input = getInput("update-deploy-plan-resource.json");
        customResourceHandler.handleCustomResource(input, context);
        ArgumentCaptor<CfnResponse> cfnResponseArgumentCaptor = ArgumentCaptor.forClass(CfnResponse.class);
        verify(registerDeploymentPlanTriggerService).registerDeploymentPlanTrigger(input, CfnRequestType.UPDATE);
        verify(registerDeployOriginDataService).registerDeployOriginData(any(RegisterDeployOriginDataRequest.class));
        verify(responseSender).sendResponse(anyString(), cfnResponseArgumentCaptor.capture());
        String status = cfnResponseArgumentCaptor.getValue().getStatus();
        assertEquals("SUCCESS", status);
    }

    private JsonNode getInput(String fileName) throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", "custom-resource", fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(inputFilePath.toFile());
    }
}
