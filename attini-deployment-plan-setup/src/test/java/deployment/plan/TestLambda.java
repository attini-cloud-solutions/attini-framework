package deployment.plan;

import java.util.Map;
import jakarta.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@Named("test")
public class TestLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> stringObjectMap, Context context) {
        return null;
    }
}
