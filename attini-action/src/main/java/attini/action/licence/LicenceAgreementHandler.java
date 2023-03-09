package attini.action.licence;

import static attini.action.custom.resource.CustomResourceResponse.failedResponse;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.SendUsageDataFacade;
import attini.action.custom.resource.CustomResourceResponse;
import attini.action.custom.resource.CustomResourceResponseSender;

public class LicenceAgreementHandler {

    private static final Logger logger = Logger.getLogger(LicenceAgreementHandler.class);


    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final CustomResourceResponseSender licenceAgreementResponseSender;
    private final SendUsageDataFacade sendUsageDataFacade;

    public LicenceAgreementHandler(CustomResourceResponseSender licenceAgreementResponseSender,
                                   SendUsageDataFacade sendUsageDataFacade) {
        this.licenceAgreementResponseSender = requireNonNull(licenceAgreementResponseSender,
                                                             "licenceAgreementResponseSender");
        this.sendUsageDataFacade = requireNonNull(sendUsageDataFacade, "sendUsageDataFacade");
    }

    public void handleLicenceAgreement(Map<String, Object> input) {

        JsonNode inputJson = createJson(input);

        String acceptance = inputJson.get("ResourceProperties")
                                     .get("Acceptance")
                                     .asText();

        try {
            CustomResourceResponse.Builder builder =
                    CustomResourceResponse.builder()
                                          .setRequestId(inputJson.get("RequestId").asText())
                                          .setLogicalResourceId(inputJson.get("LogicalResourceId").asText())
                                          .setPhysicalResourceId(inputJson.get("LogicalResourceId").asText())
                                          .setStackId(inputJson.get("StackId").asText());


            if (!inputJson.get("RequestType").asText().equals("Delete")) {
                sendUsageDataFacade.sendAcceptedLicenceAgreement(acceptance.equals("true"));
            }

            if (acceptance.equals("true") || inputJson.get("RequestType").asText().equals("Delete")) {
                builder.setStatus("SUCCESS").setReason("All is good");
            } else {
                builder.setStatus("FAILED").setReason("You have not accepted the Attini licence agreement");
            }

            licenceAgreementResponseSender.sendResponse(inputJson.get("ResponseURL")
                                                                 .asText(), builder.build());
        } catch (Exception e) {
            licenceAgreementResponseSender.sendResponse(inputJson.get("ResponseURL").asText(),
                                                        failedResponse(inputJson).build());
        }

    }

    private JsonNode createJson(Map<String, Object> input) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(input)).deepCopy();
        } catch (JsonProcessingException e) {
            logger.fatal("Could not parse input", e);
            throw new RuntimeException(e);
        }
    }
}
