package attini.deploy.origin.appdeployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import attini.domain.Environment;

class AppConfigTest {


    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void shouldMergeConfig() {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ObjectNode defaultNode = objectMapper.createObjectNode();
        ObjectNode devNode = objectMapper.createObjectNode();
        ObjectNode configObject = objectMapper.createObjectNode();

        configObject.put("constantValue", "shouldStaySame");
        configObject.put("testValue", "defaultValue");
        defaultNode.set("configObject", configObject.deepCopy());

        configObject.put("testValue", "devValue");
        devNode.set("configObject", configObject.deepCopy());

        rootNode.set("default", defaultNode);
        rootNode.set("dev", devNode);


        AppConfig appConfig = new AppConfig(rootNode, objectMapper, "TestAppPlan", Environment.of("dev"));

        JsonNode config = appConfig.getConfig();

        assertEquals("shouldStaySame", config.path("configObject").path("constantValue").asText());
        assertEquals("devValue", config.path("configObject").path("testValue").asText());


    }

    @Test
    public void shouldHandleEmptyDefault() {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ObjectNode devNode = objectMapper.createObjectNode();
        ObjectNode configObject = objectMapper.createObjectNode();

        configObject.put("constantValue", "shouldStaySame");
        configObject.put("testValue", "defaultValue");

        configObject.put("testValue", "devValue");
        devNode.set("configObject", configObject.deepCopy());

        rootNode.set("dev", devNode);


        AppConfig appConfig = new AppConfig(rootNode, objectMapper, "TestAppPlan", Environment.of("dev"));
        JsonNode config = appConfig.getConfig();

        assertEquals("shouldStaySame", config.path("configObject").path("constantValue").asText());
        assertEquals("devValue", config.path("configObject").path("testValue").asText());


    }

    @Test
    public void shouldThrowExceptionIfInvalidFormat() {
        ObjectNode rootNode = objectMapper.createObjectNode();

        rootNode.put("dev","test");


        assertThrows(IllegalArgumentException.class, () -> new AppConfig(rootNode, objectMapper, "TestAppPlan", Environment.of("dev")));



    }

    @Test
    public void shouldHandleOnlyDefault() {
        ObjectNode rootNode = objectMapper.createObjectNode();
        ObjectNode defaultNode = objectMapper.createObjectNode();

        ObjectNode configObject = objectMapper.createObjectNode();

        configObject.put("constantValue", "shouldStaySame");
        configObject.put("testValue", "defaultValue");
        defaultNode.set("configObject", configObject.deepCopy());

        configObject.put("testValue", "devValue");

        rootNode.set("default", defaultNode);
        AppConfig appConfig = new AppConfig(rootNode, objectMapper, "TestAppPlan", Environment.of("dev"));
        JsonNode config = appConfig.getConfig();

        assertEquals("shouldStaySame", config.path("configObject").path("constantValue").asText());
        assertEquals("defaultValue", config.path("configObject").path("testValue").asText());


    }

    @Test
    public void shouldHandleConfig() {

        AppConfig appConfig = new AppConfig(null, objectMapper, "TestAppPlan", Environment.of("dev"));

        assertEquals("TestAppPlan", appConfig.getAppDeploymentPlan());


    }


}
