/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.CloudFormationClientFactory;
import attini.action.builders.TestBuilders;
import attini.action.domain.CfnStackConfig;
import attini.action.domain.ConfigurationPropertyValue;
import attini.action.domain.FileStackConfiguration;
import attini.action.facades.stackdata.InitStackDataFacade;
import attini.action.system.EnvironmentVariables;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CloudFormationException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ParameterDeclaration;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.ssm.SsmClient;

@ExtendWith(MockitoExtension.class)
class StackConfigurationServiceTest {

    @Mock
    StackConfigurationFileService stackConfigurationFileService;

    @Mock
    CloudFormationClient cloudFormationClient;

    @Mock
    CloudFormationClientFactory cloudFormationClientFactory;

    @Mock
    EnvironmentVariables environmentVariables;

    @Mock
    SsmClient ssmClient;

    @Mock
    InitStackDataFacade initStackDataFacade;

    StackConfigurationService stackConfigurationService;

    @BeforeEach
    void setUp() {
        when(cloudFormationClientFactory.getClient(any())).thenReturn(cloudFormationClient);
        when(initStackDataFacade.getInitConfigVariables(anyString())).thenReturn(Collections.emptyMap());
        stackConfigurationService = new StackConfigurationService(stackConfigurationFileService,
                                                                  cloudFormationClientFactory,
                                                                  environmentVariables,
                                                                  ssmClient,
                                                                  initStackDataFacade);
    }

    @Test
    void shouldCreateStack_shouldUseCurrentParamOrTagIfFallback() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .build();

        GetConfigFileRequest getConfigFileRequest = toConfigFileRequest(stackConfig);

        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder()
                                                  .stacks(Stack.builder()
                                                               .parameters(Parameter.builder()
                                                                                    .parameterKey("Ram")
                                                                                    .parameterValue("1024")
                                                                                    .build())
                                                               .tags(Tag.builder().key("Author").value("Pelle").build())
                                                               .build())
                                                  .build());
        when(stackConfigurationFileService.getConfiguration(getConfigFileRequest))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", true)))
                                    .setTags(Map.of("Author", ConfigurationPropertyValue.create("Oscar", true)))
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        ArrayList<Tag> tags = new ArrayList<>(stackConfiguration.getTags());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("1024", parameters.get(0).parameterValue());
        assertEquals("Author", tags.get(0).key());
        assertEquals("Pelle", tags.get(0).value());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertFalse(stackConfiguration.getRegion().isPresent());
    }

    private GetConfigFileRequest toConfigFileRequest(CfnStackConfig stackConfig) {
        return GetConfigFileRequest.builder()
                                   .setConfigPath(stackConfig.getConfigPath().get())
                                   .setDistributionId(stackConfig.getDistributionId())
                                   .setDistributionName(stackConfig.getDistributionName())
                                   .setEnvironment(stackConfig.getEnvironment())
                                   .setVariables(stackConfig.getVariables()).build();
    }

    @Test
    void shouldCreateStack_prioritizeNoneConfigFileParams() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertFalse(stackConfiguration.getRegion().isPresent());
    }


    @Test
    void shouldCreateStack_ShouldIncludeRegion() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setRegion("eu-west-1")
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertTrue(stackConfiguration.getRegion().isPresent());
        assertEquals("eu-west-1", stackConfiguration.getRegion().get());

    }

    @Test
    void shouldCreateStack_ShouldIncludeRegionIfOnlyPresentInTemplate() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setRegion("eu-west-1")
                                                 .build();
        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertTrue(stackConfiguration.getRegion().isPresent());
        assertEquals("eu-west-1", stackConfiguration.getRegion().get());

    }

    @Test
    void shouldCreateStack_ShouldIncludeStackRoleIfOnlyPresentInTemplate() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setStackRole("my-template-role")
                                                 .setRegion("eu-west-1")
                                                 .build();
        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));


        assertEquals("my-template-role", stackConfiguration.getStackRole().get());

    }

    @Test
    void shouldCreateStack_ShouldIncludeStackRoleIfOnlyPresentInConfigFile() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setRegion("eu-west-1")
                                                 .setConfigPath("/a/path/config.json")
                                                 .setStackRole(null)
                                                 .build();
        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(emptyMap())
                                    .setTags(emptyMap())
                                    .setRegion("eu-west-1")
                                    .setStackRoleArn("my-stack-role")
                                    .build());


        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));


        assertEquals("my-stack-role", stackConfiguration.getStackRole().get());

    }

    @Test
    void shouldCreateStack_ShouldIncludeStackRoleFromTemplate() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setRegion("eu-west-1")
                                                 .setConfigPath("/a/path/config.json")
                                                 .setStackRole("template-stack-role")
                                                 .build();
        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(emptyMap())
                                    .setTags(emptyMap())
                                    .setRegion("eu-west-1")
                                    .setStackRoleArn("my-stack-role")
                                    .build());


        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));


        assertEquals("template-stack-role", stackConfiguration.getStackRole().get());

    }


    @Test
    void shouldCreateStack_ShouldIncludeRegionIfOnlyPresentInFile() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .setRegion("eu-west-1")
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertTrue(stackConfiguration.getRegion().isPresent());
        assertEquals("eu-west-1", stackConfiguration.getRegion().get());

    }

    @Test
    void shouldCreateStack_ShouldIncludeRegionFromTemplateConfig() {
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setRegion("eu-central-1")
                                                 .build();
        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .setRegion("eu-west-1")
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
        assertTrue(stackConfiguration.getRegion().isPresent());
        assertEquals("eu-central-1", stackConfiguration.getRegion().get());

    }

    @Test
    void shouldCreateStack_shouldSubstituteVariables() {

        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setStackName("${Variable}-my-stack")
                                                 .setVariables(Map.of("Variable", "someValue"))
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals("someValue-my-stack", stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
    }

    @Test
    void shouldCreateStack_shouldSubstituteVariablesFromAttiniConfig() {

        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setStackName("${Variable}-my-stack")
                                                 .setVariables(Map.of("Variable", "someValue"))
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(initStackDataFacade.getInitConfigVariables(stackConfig.getInitStackName())).thenReturn(Map.of("Variable",
                                                                                                           "someOtherValue"));
        when(stackConfigurationFileService.getConfiguration(any(GetConfigFileRequest.class)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals("someValue-my-stack", stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
    }

    @Test
    void shouldCreateStack_shouldSubstituteVariables_shouldOverrideAttiniConfig() {

        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .setStackName("${Variable}-my-stack")
                                                 .build();
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(initStackDataFacade.getInitConfigVariables(stackConfig.getInitStackName())).thenReturn(Map.of("Variable",
                                                                                                           "someValue"));
        when(stackConfigurationFileService.getConfiguration(any(GetConfigFileRequest.class)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(stackConfig.getTemplate().get(),
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());
        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals("someValue-my-stack", stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
    }


    @Test
    void shouldCreateStack_shouldGetTemplatePathFromConfigIfMissing() {
        CfnStackConfig stackConfig = TestBuilders.aCfnStackConfig()
                                                 .setConfigPath("/a/path/config.json")
                                                 .setTemplate(null)
                                                 .setParameters(Map.of("Ram",
                                                                       ConfigurationPropertyValue.create("512", false)))
                                                 .build();
        String templatePath = "/my/template/path.yaml";
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(CloudFormationException.class);

        when(stackConfigurationFileService.getConfiguration(toConfigFileRequest(stackConfig)))
                .thenReturn(FileStackConfiguration
                                    .builder()
                                    .setTemplatePath(templatePath)
                                    .setParameters(Map.of("Ram", ConfigurationPropertyValue.create("256", false)))
                                    .setTags(emptyMap())
                                    .build());

        when(environmentVariables.getEnvironmentParameterName()).thenReturn("my-environment-parameter");

        String expectedTemplatePath = TemplatePathUtil.getTemplatePath(templatePath,
                                                                       stackConfig.getTemplateUrlPrefix());

        when(cloudFormationClient.getTemplateSummary(GetTemplateSummaryRequest.builder()
                                                                              .templateURL(expectedTemplatePath)
                                                                              .build()))
                .thenReturn(GetTemplateSummaryResponse.builder()
                                                      .parameters(List.of(ParameterDeclaration.builder()
                                                                                              .parameterKey("Ram")
                                                                                              .build()))
                                                      .build());


        StackConfiguration stackConfiguration = stackConfigurationService.getStackConfig(stackConfig);
        verify(cloudFormationClient, times(1)).getTemplateSummary(any(GetTemplateSummaryRequest.class));

        ArrayList<Parameter> parameters = new ArrayList<>(stackConfiguration.getParameters());
        assertEquals(1, parameters.size());
        assertEquals("Ram", parameters.get(0).parameterKey());
        assertEquals("512", parameters.get(0).parameterValue());
        assertEquals(stackConfig.getStackName().get(), stackConfiguration.getStackName());
        assertEquals(expectedTemplatePath, stackConfiguration.getTemplate());
    }

}
