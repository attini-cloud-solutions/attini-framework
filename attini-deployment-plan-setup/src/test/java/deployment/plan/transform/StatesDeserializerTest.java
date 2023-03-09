package deployment.plan.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class StatesDeserializerTest {

    @Inject
    ObjectMapper objectMapper;


    String STATES = """
            {"states" : {
                "SamStrudle": {
                    "Type": "AttiniSam",
                    "Properties": {
                        "Project": {
                            "Path": "/zip-sam"
                        },
                        "StackName": "SamStuffStack"
                    },
                    "Next": "SamStrudle2"
                },
                "SamStrudle2": {
                    "Type": "AttiniSam",
                    "Properties": {
                        "Project": {
                            "Path": "/zip-sam"
                        },
                        "StackName": "SamStuffStack"
                    },
                    "End": true
                }
             }
            }
                        
            """;

    @Test
    void test() throws JsonProcessingException {

        TestRecord testRecord = objectMapper.readValue(STATES, TestRecord.class);
        assertEquals(2,testRecord.states().size());
        assertTrue(testRecord.states().containsKey("SamStrudle"));
        assertTrue(testRecord.states().containsKey("SamStrudle2"));

    }

    record TestRecord(@JsonDeserialize(using = StatesDeserializer.class)
                      Map<String, Map<String, Object>> states) {
    }

}
