package deployment.plan.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AttiniStepLoaderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateFileLoader templateFileLoader = new TemplateFileTestLoader();
    private AttiniStepLoader attiniStepLoader;

    @BeforeEach
    void setUp() {
        attiniStepLoader = new AttiniStepLoader(templateFileLoader, objectMapper);
    }

    @Test
    void getAttiniMapCfn() {
        AbstractRequestResponse requestResponse = new MapInputOutput();
        JsonNode result = attiniStepLoader.getAttiniMapCfn(requestResponse.request(),
                                                           "DeploySocksDB");


        assertJsonEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniCfn() {
        AbstractRequestResponse requestResponse = new CfnRequestResponse();
        JsonNode result = attiniStepLoader.getAttiniCfn(requestResponse.request(),
                                                        "DeploySocksDB");

        assertJsonEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniLambdaInvoke() {
        AbstractRequestResponse requestResponse = new LambdaInvokeRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniLambdaInvoke(requestResponse.request(),
                                                                 "InvokeMyLambda");

        assertJsonEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniRunner() {
        AbstractRequestResponse requestResponse = new RunnerRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniRunner(requestResponse.request(), "Step1b1");
        assertJsonEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniCdk() {
        AbstractRequestResponse requestResponse = new CdkRequestResponse();

        Map<AttiniStep, JsonNode> result = attiniStepLoader.getAttiniCdk(requestResponse.request(), "Step1b1", new HashMap<>());

        assertJsonEquals(requestResponse.expectedResponse(), result.get(new AttiniStep("Step1b1", "AttiniCdk")));

    }

    @Test
    void getAttiniCdkWithChangeSet() {
        AbstractRequestResponse requestResponse = new CdkRequestWithChangesetResponse();

        JsonNode result =
                objectMapper.createObjectNode()
                            .setAll(attiniStepLoader.getAttiniCdk(requestResponse.request(), "Step1b1", new HashMap<>())
                                                    .entrySet()
                                                    .stream()
                                                    .collect(Collectors.toMap(t -> t.getKey().name(),
                                                                              Map.Entry::getValue)));


        assertJsonEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniImport() {

        AbstractRequestResponse requestResponse = new ImportRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniImport(requestResponse.request(), "GetSocksDbArn");

        assertJsonEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniManualApproval() {

        AbstractRequestResponse requestResponse = new ManualApprovalRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniManualApproval(requestResponse.request(), "GetSocksDbArn");

        assertJsonEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniMergeOutput() {

        AbstractRequestResponse requestResponse = new MergeOutputRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniMergeOutput(requestResponse.request());

        assertJsonEquals(requestResponse.expectedResponse(), result);
    }


    /**
     * Assert equals and print result if there is an error.
     * @param expected expected json node
     * @param result actual json node
     */
    private void assertJsonEquals(JsonNode expected, JsonNode result) {
        try {
            assertEquals(expected,
                         result, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
