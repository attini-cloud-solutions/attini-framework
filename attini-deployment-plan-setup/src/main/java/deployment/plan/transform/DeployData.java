/*
 * Copyright (c) 2023 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.transform;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DeployData {
    private static final Logger logger = Logger.getLogger(DeployData.class);
    private final TemplateFileLoader templateFileLoader;


    public DeployData(TemplateFileLoader templateFileLoader) {
        this.templateFileLoader = templateFileLoader;
    }

    public JsonNode getDeployData(String nextStep) {

       return getNode(nextStep, templateFileLoader.getDeployStateTemplate());
    }

    public JsonNode getAppDeployData(String nextStep) {

        return getNode(nextStep,templateFileLoader.getAppDeployStateTemplate());
    }

    private ObjectNode getNode(String nextStep, File file){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ObjectNode node = (ObjectNode) objectMapper.readTree(file);
            node.put("Next", nextStep);
            return node;
        } catch (IOException e) {
            logger.error("could not parse deploy data json");
            throw new UncheckedIOException(e);
        }
    }
}

