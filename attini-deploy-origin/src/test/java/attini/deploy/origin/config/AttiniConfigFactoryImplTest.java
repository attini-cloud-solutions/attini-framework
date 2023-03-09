package attini.deploy.origin.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.deploy.origin.InitDeployEvent;
import attini.domain.ObjectIdentifier;

@ExtendWith(MockitoExtension.class)
class AttiniConfigFactoryImplTest {

    private static final String BUCKET = "testbucket";
    private static final String ENVIRONMENT = "dev";
    private static final String OBJECT_KEY = ENVIRONMENT + "/test.zip";
    private static final ObjectIdentifier SOME_OBJECT_IDENTIFIER = ObjectIdentifier.of("someObjectIdentifier");
    private static final String SOME_USER = "pelle.the.boss";

    @Mock
    ConfigFileResolver configFileResolver;

    @Mock
    InitDeployParameterService initDeployParameterService;

    AttiniConfigFactory attiniConfigFactory;

    @BeforeEach
    void setup() {
        attiniConfigFactory = new AttiniConfigFactory(configFileResolver, initDeployParameterService);
    }


    @Test
    void createAttiniConfig() {
        Path testConfigDirectoryPath = Paths.get("src", "test", "resources", "attiniconfig");
        when(initDeployParameterService.resolveParameters(any(),any(), any())).thenReturn(Collections.emptyMap());
        when(configFileResolver.getAttiniConfigFiles(testConfigDirectoryPath)).thenReturn(List.of("attiniconfig.json"));
        AttiniConfig attiniConfig = attiniConfigFactory.createAttiniConfig(testConfigDirectoryPath,
                                                                           new InitDeployEvent(BUCKET,
                                                                                               OBJECT_KEY,
                                                                                               SOME_OBJECT_IDENTIFIER,
                                                                                               SOME_USER));
        assertNotNull(attiniConfig.getAttiniDistributionId());
        assertNotNull(attiniConfig.getAttiniDistributionName());
        assertEquals(ENVIRONMENT + "-" + attiniConfig.getAttiniDistributionName().asString(),
                     attiniConfig.getAttiniInitDeployStackConfig().get().getInitDeployStackName());

    }

    @Test
    void createAttiniConfig_yaml() {
        Path testConfigDirectoryPath = Paths.get("src", "test", "resources", "attiniconfig");
        when(configFileResolver.getAttiniConfigFiles(testConfigDirectoryPath)).thenReturn(List.of("attiniconfig.json"));
        AttiniConfig attiniConfig = attiniConfigFactory.createAttiniConfig(testConfigDirectoryPath,
                                                                           new InitDeployEvent(BUCKET,
                                                                                               OBJECT_KEY,
                                                                                               SOME_OBJECT_IDENTIFIER,
                                                                                               SOME_USER));
        assertNotNull(attiniConfig.getAttiniDistributionId());
        assertNotNull(attiniConfig.getAttiniDistributionName());
        assertEquals(ENVIRONMENT + "-" + attiniConfig.getAttiniDistributionName().asString(),
                     attiniConfig.getAttiniInitDeployStackConfig().get().getInitDeployStackName());

    }

    @Test
    void createAttiniConfig_failIfMoreThenOneConfigFile() {
        Path testConfigDirectoryPath = Paths.get("src", "test", "resources", "attiniconfig");
        when(configFileResolver.getAttiniConfigFiles(testConfigDirectoryPath)).thenReturn(List.of("attini-config.json",
                                                                                                  "attini-config.yaml"));
        assertThrows(IllegalStateException.class, () -> attiniConfigFactory.createAttiniConfig(testConfigDirectoryPath,
                                                                                               new InitDeployEvent(
                                                                                                       BUCKET,
                                                                                                       OBJECT_KEY,
                                                                                                       SOME_OBJECT_IDENTIFIER,
                                                                                                       SOME_USER)));

    }

    @Test
    void createAttiniConfig_will_set_distname_if_not_present() {
        Path testConfigDirectoryPath = Paths.get("src", "test", "resources", "attiniconfig");
        when(configFileResolver.getAttiniConfigFiles(testConfigDirectoryPath)).thenReturn(List.of("attiniconfig.json"));
        AttiniConfig attiniConfig = attiniConfigFactory.createAttiniConfig(testConfigDirectoryPath,
                                                                           new InitDeployEvent(BUCKET,
                                                                                               OBJECT_KEY,
                                                                                               SOME_OBJECT_IDENTIFIER,
                                                                                               SOME_USER));
        assertNotNull(attiniConfig.getAttiniDistributionId());
        assertNotNull(attiniConfig.getAttiniDistributionName());
        assertEquals(ENVIRONMENT + "-" + attiniConfig.getAttiniDistributionName().asString(),
                     attiniConfig.getAttiniInitDeployStackConfig().get().getInitDeployStackName());

    }
}
