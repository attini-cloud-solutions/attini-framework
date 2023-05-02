/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static attini.step.guard.EventConverter.createInitDeployInput;
import static attini.step.guard.EventConverter.createManualApprovalEvent;
import static attini.step.guard.EventConverter.createManualInitDeployEvent;
import static attini.step.guard.EventConverter.createManualTriggerInput;
import static attini.step.guard.EventConverter.createSnsEvent;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import attini.step.guard.cloudformation.CfnEventHandler;
import attini.step.guard.cloudformation.InitDeployEventHandler;
import attini.step.guard.manualapproval.ContinueExecutionService;


@Named("app")
public class App implements RequestHandler<Map<String, Object>, Object> {
    private static final Logger logger = Logger.getLogger(App.class);

    private final CfnEventHandler cfnEventHandler;
    private final ContinueExecutionService continueExecutionService;
    private final EventTypeResolver eventTypeResolver;
    private final InitDeployEventHandler initDeployEventHandler;
    private final ObjectMapper mapper;

    @Inject
    public App(CfnEventHandler cfnEventHandler,
               ContinueExecutionService continueExecutionService,
               EventTypeResolver eventTypeResolver,
               InitDeployEventHandler initDeployEventHandler,
               ObjectMapper mapper) {
        this.cfnEventHandler = requireNonNull(cfnEventHandler, "respondToCfnEvent");
        this.continueExecutionService = requireNonNull(continueExecutionService, "continueExecutionService");
        this.eventTypeResolver = requireNonNull(eventTypeResolver, "eventTypeResolver");
        this.initDeployEventHandler = requireNonNull(initDeployEventHandler, "initDeployEventHandler");
        this.mapper = requireNonNull(mapper, "mapper");
    }

    @Override
    public Object handleRequest(Map<String, Object> input, Context context) {


        JsonNode jsonNode = mapper.valueToTree(input);
        logger.info("Got event: " + jsonNode.toString());
        EventType eventType = eventTypeResolver.resolveEventType(jsonNode);
        switch (eventType) {
            case MANUAL_APPROVAL -> continueExecutionService.continueExecution(createManualApprovalEvent(jsonNode));
            case CFN_SNS -> cfnEventHandler.respondToCloudFormationSnsEvent(createSnsEvent(jsonNode));
            case INIT_DEPLOY_CFN -> initDeployEventHandler.respondToInitDeployCfnEvent(createInitDeployInput(jsonNode));
            case CFN_MANUAL -> cfnEventHandler.respondToManualCfnEvent(createManualTriggerInput(jsonNode));
            case INIT_DEPLOY_MANUAL_TRIGGER -> initDeployEventHandler.respondToManualInitDeployEvent(
                    createManualInitDeployEvent(jsonNode));
        }

        return "Success";
    }


}
