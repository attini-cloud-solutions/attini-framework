package deployment.plan.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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


        assertEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniCfn() {
        AbstractRequestResponse requestResponse = new CfnRequestResponse();
        JsonNode result = attiniStepLoader.getAttiniCfn(requestResponse.request(),
                                                        "DeploySocksDB");

        assertEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniLambdaInvoke() {
        AbstractRequestResponse requestResponse = new LambdaInvokeRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniLambdaInvoke(requestResponse.request(),
                                                                 "InvokeMyLambda");

        assertEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniRunner() {
        AbstractRequestResponse requestResponse = new RunnerRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniRunner(requestResponse.request(), "Step1b1");
        assertEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniCdk() {
        AbstractRequestResponse requestResponse = new CdkRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniCdk(requestResponse.request(), "Step1b1");

        assertEquals(requestResponse.expectedResponse(), result);

    }

    @Test
    void getAttiniImport() {

        AbstractRequestResponse requestResponse = new ImportRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniImport(requestResponse.request(), "GetSocksDbArn");

        assertEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniManualApproval() {

        AbstractRequestResponse requestResponse = new ManualApprovalRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniManualApproval(requestResponse.request(), "GetSocksDbArn");

        assertEquals(requestResponse.expectedResponse(), result);
    }

    @Test
    void getAttiniMergeOutput() {

        AbstractRequestResponse requestResponse = new MergeOutputRequestResponse();

        JsonNode result = attiniStepLoader.getAttiniMergeOutput(requestResponse.request());

        assertEquals(requestResponse.expectedResponse(), result);
    }
}
