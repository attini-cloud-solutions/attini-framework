/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.step.guard;

import static java.util.Objects.requireNonNull;

import org.jboss.logging.Logger;

import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.SendTaskFailureRequest;
import software.amazon.awssdk.services.sfn.model.SendTaskSuccessRequest;

public class SfnResponseSender {
    private static final Logger logger = Logger.getLogger(SfnResponseSender.class);
    private final SfnClient sfnClient;

    public SfnResponseSender(SfnClient sfnClient) {
        this.sfnClient = requireNonNull(sfnClient, "sfnClient");
    }
    public void sendTaskSuccess(String sfnToken, String output){
        logger.info("Sending task success with token" + sfnToken);
        SendTaskSuccessRequest successRequest = SendTaskSuccessRequest.builder()
                .taskToken(sfnToken)
                .output(output)
                .build();
        sfnClient.sendTaskSuccess(successRequest);
    }


    public void sendTaskFailure(String sfnToken, String cause, String error){
        logger.info("Sending task failed with token" + sfnToken);
        SendTaskFailureRequest failureRequest = SendTaskFailureRequest.builder()
                .taskToken(sfnToken)
                .error(error)
                .cause(cause)
                .build();
        sfnClient.sendTaskFailure(failureRequest);
    }


}
