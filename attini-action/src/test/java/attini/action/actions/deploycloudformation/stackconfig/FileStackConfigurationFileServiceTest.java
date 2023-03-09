/*
 * Copyright (c) 2021 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.actions.deploycloudformation.stackconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.StackConfigException;
import attini.action.domain.FileStackConfiguration;
import attini.action.facades.S3Facade;
import attini.action.system.EnvironmentVariables;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@ExtendWith(MockitoExtension.class)
class FileStackConfigurationFileServiceTest {

    private static final String ENVIRONMENT = "dev";
    private static final String DIST_NAME = "infra";
    private static final String DIST_ID = "12312323123dsds";
    private static final String CONFIG_FILE = "/test/dev/config.json";
    private static final String CONFIG_FILE_YAML = "/test/dev/config.yaml";

    @Mock
    StsClient stsClient;

    @Mock
    S3Facade s3Facade;

    @Mock
    EnvironmentVariables environmentVariables;

    StackConfigurationFileService stackConfigurationFileService;

    @BeforeEach
    void setUp() {
        stackConfigurationFileService = new StackConfigurationFileService(stsClient, s3Facade, environmentVariables);
    }

    @Test
    void getConfiguration() throws IOException {

        byte[] bytes = getInput("simpleConfig.json");

        when(stsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().account("myAccount").build());
        when(s3Facade.getObject(anyString(), anyString())).thenReturn(bytes);
        FileStackConfiguration configuration = stackConfigurationFileService.getConfiguration(aRequest(CONFIG_FILE));


        assertEquals("128",configuration.getParameters().get("Ram").getValue());
        assertEquals("Oscar",configuration.getTags().get("Author").getValue());
        assertEquals("my/path", configuration.getTemplatePath());

    }

    @Test
    void getConfiguration_yaml() throws IOException {

        byte[] bytes = getInput("simpleConfig.yaml");

        when(stsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().account("myAccount").build());
        when(s3Facade.getObject(anyString(), anyString())).thenReturn(bytes);
        FileStackConfiguration configuration = stackConfigurationFileService.getConfiguration(aRequest(CONFIG_FILE_YAML));


        assertEquals("128",configuration.getParameters().get("Ram").getValue());
        assertEquals("Oscar",configuration.getTags().get("Author").getValue());
        assertEquals("my/path", configuration.getTemplatePath());

    }

    @Test
    void getConfiguration_extendsSelf_yaml() throws IOException {

        byte[] bytes = getInput("selfExtend.yaml");

        when(stsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().account("myAccount").build());
        when(s3Facade.getObject(anyString(), anyString())).thenReturn(bytes);
        assertThrows(StackConfigException.class, () -> stackConfigurationFileService.getConfiguration(aRequest(CONFIG_FILE_YAML)));

    }

    @Test
    void getConfiguration_extends_yaml() throws IOException {

        byte[] bytes = getInput("childConfig.yaml");
        byte[] parentBytes = getInput("parentConfig.yaml");


        when(environmentVariables.getRegion()).thenReturn("eu-west-1");
        when(stsClient.getCallerIdentity()).thenReturn(GetCallerIdentityResponse.builder().account("myAccount").build());
        when(s3Facade.getObject("attini-artifact-store-eu-west-1-myAccount", "dev/infra/12312323123dsds/distribution-origin/childConfig.yaml")).thenReturn(bytes);
        when(s3Facade.getObject("attini-artifact-store-eu-west-1-myAccount", "dev/infra/12312323123dsds/distribution-origin/parentConfig.yaml")).thenReturn(parentBytes);

        FileStackConfiguration configuration = stackConfigurationFileService.getConfiguration(aRequest("/childConfig.yaml"));


        assertEquals("128",configuration.getParameters().get("Ram").getValue());
        assertEquals("Oscar Parent",configuration.getTags().get("Author").getValue());
        assertEquals("my/path", configuration.getTemplatePath());

    }


    private static GetConfigFileRequest aRequest(String configFile){
        return GetConfigFileRequest.builder().setEnvironment(Environment.of(ENVIRONMENT)).setConfigPath(configFile).setDistributionId(
                DistributionId.of(DIST_ID)).setDistributionName(DistributionName.of(DIST_NAME)).build();
    }

    private static byte[] getInput(String fileName) throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources","config", fileName);

       return Files.readAllBytes(inputFilePath);

    }
}
