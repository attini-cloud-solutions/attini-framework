/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.step.guard;

public class EnvironmentVariables {

    public String getResourceStatesTableName(){
        return System.getenv("ATTINI_RESOURCE_STATES_TABLE");
    }

    public String getRegion(){
        return System.getenv("AWS_REGION");
    }

    public String getAccountId(){
        return System.getenv("AWS_ACCOUNT_ID");
    }

    public String getDeploymentStatusTopic(){
        return System.getenv("DEPLOYMENT_STATUS_TOPIC");
    }

    public String getDeployOriginTableName(){
        return System.getenv(
                "ATTINI_DEPLOYMENT_ORIGIN_TABLE");
    }


}
