package attini.deploy.origin;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

@Named("app")
public class App implements RequestHandler<Map<String, Object>, String> {

    private static final Logger logger = Logger.getLogger(App.class);
    private final InitDeployService initDeployService;
    private final InitDeployEventFactory initDeployEventFactory;

    @Inject
    public App(InitDeployService initDeployService, InitDeployEventFactory initDeployEventFactory) {
        this.initDeployService = requireNonNull(initDeployService, "initDeployService");
        this.initDeployEventFactory = requireNonNull(initDeployEventFactory, "initDeployEventFactory");
    }


    @Override
    public String handleRequest(Map<String, Object> input, Context context) {

        logger.info("Starting to handle event");
        InitDeployEvent initDeployEvent = initDeployEventFactory.create(input);

        initDeployService.initDeploy(initDeployEvent);

        return "Success";
    }
}
