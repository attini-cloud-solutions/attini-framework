/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package attini.deploy.origin;

import attini.deploy.origin.config.AttiniConfig;

import org.jboss.logging.Logger;

import attini.domain.Environment;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.TimeUnit;

public class PutLatestDistributionReferenceParameter {
    private static final Logger logger = Logger.getLogger(PutLatestDistributionReferenceParameter.class);
    private final SsmClient ssmClient;


    public PutLatestDistributionReferenceParameter(SsmClient ssmClient) {
        this.ssmClient = requireNonNull(ssmClient, "ssmClient");
    }

    public void putParameter(AttiniConfig attiniConfig, Environment environment) {
        String name = String.format("/attini/distributions/%s/%s/latest",
                                    environment.asString(),
                                    attiniConfig.getAttiniDistributionName().asString());
        PutParameterRequest putParameterRequest = PutParameterRequest.builder()
                                                                     .type("String")
                                                                     .description(
                                                                             "Reference to the latest distribution Id")
                                                                     .name(name)
                                                                     .overwrite(true)
                                                                     .value(attiniConfig.getAttiniDistributionId().asString())
                                                                     .build();
        logger.info(String.format("Saving the latest distribution Id to SSM parameter store. Name: %s Value: %s",
                                  name,
                                  attiniConfig.getAttiniDistributionId().asString()));

        PutParameterResponse putParameterResponse = ssmClient.putParameter(putParameterRequest);

        if (putParameterResponse.version() == 1) {
            try {
                TimeUnit.MILLISECONDS.sleep(1500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        AddTagsToResourceRequest addTagsToResourceRequest = AddTagsToResourceRequest.builder()
                                                                                    .resourceType(ResourceTypeForTagging.PARAMETER)
                                                                                    .tags(Tag.builder()
                                                                                             .key("Attini")
                                                                                             .value("-")
                                                                                             .build())
                                                                                    .resourceId(name)
                                                                                    .build();
        ssmClient.addTagsToResource(addTagsToResourceRequest);
    }

}
