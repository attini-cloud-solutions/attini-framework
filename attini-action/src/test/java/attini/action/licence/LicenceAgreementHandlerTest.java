package attini.action.licence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.action.SendUsageDataFacade;
import attini.action.custom.resource.CustomResourceResponse;
import attini.action.custom.resource.CustomResourceResponseSender;


@ExtendWith(MockitoExtension.class)
class LicenceAgreementHandlerTest {

    @Mock
    CustomResourceResponseSender sender;

    @Mock
    SendUsageDataFacade sendUsageDataFacade;

    LicenceAgreementHandler licenceAgreementHandler;

    @BeforeEach
    void setUp() {
        licenceAgreementHandler = new LicenceAgreementHandler(sender, sendUsageDataFacade);
    }

    @Test
    void createRequest_shouldAnswerSuccess() throws IOException {
        Map<String, Object> input = getInput("licence-agreement-request-create.json");

        ArgumentCaptor<CustomResourceResponse> responseCaptor = ArgumentCaptor.forClass(CustomResourceResponse.class);


        licenceAgreementHandler.handleLicenceAgreement(input);

        verify(sender).sendResponse(anyString(), responseCaptor.capture());
        verify(sendUsageDataFacade).sendAcceptedLicenceAgreement(true);
        CustomResourceResponse value = responseCaptor.getValue();
        assertEquals("SUCCESS",value.getStatus());

    }

    @Test
    void deleteRequest_shouldAnswerSuccessAlways() throws IOException {
        Map<String, Object> input = getInput("licence-agreement-request-delete.json");

        ArgumentCaptor<CustomResourceResponse> responseCaptor = ArgumentCaptor.forClass(CustomResourceResponse.class);


        licenceAgreementHandler.handleLicenceAgreement(input);
        verify(sender).sendResponse(anyString(), responseCaptor.capture());
        verify(sendUsageDataFacade, never()).sendAcceptedLicenceAgreement(anyBoolean());
        CustomResourceResponse value = responseCaptor.getValue();
        assertEquals("SUCCESS",value.getStatus());

    }

    @Test
    void createRequest_shouldAnswerFailed() throws IOException {
        Map<String, Object> input = getInput("licence-agreement-request-create-not-accepted.json");

        ArgumentCaptor<CustomResourceResponse> responseCaptor = ArgumentCaptor.forClass(CustomResourceResponse.class);


        licenceAgreementHandler.handleLicenceAgreement(input);
        verify(sender).sendResponse(anyString(), responseCaptor.capture());
        verify(sendUsageDataFacade).sendAcceptedLicenceAgreement(false);

        CustomResourceResponse value = responseCaptor.getValue();
        assertEquals("FAILED",value.getStatus());

    }

    private static Map<String, Object> getInput(String fileName) throws IOException {
        Path inputFilePath = Paths.get("src", "test", "resources", fileName);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(inputFilePath.toFile(), new TypeReference<>() {
        });
    }

}
