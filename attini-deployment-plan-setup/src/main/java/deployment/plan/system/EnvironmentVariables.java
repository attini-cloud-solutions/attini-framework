/*
 * Copyright (c) 2020 Attini Cloud Solutions AB.
 * All Rights Reserved
 */

package deployment.plan.system;

public class EnvironmentVariables {

    public String getDefaultRole(){
        return System.getenv("ATTINI_DEPLOYMENT_PLAN_DEFAULT_ROLE");
    }

    public String getRegion(){
        return System.getenv("AWS_REGION");
    }

    public String getAccount(){
        return System.getenv("AWS_ACCOUNT_ID");
    }

    public String getResourceStatesTableName(){
       return System.getenv("ATTINI_RESOURCE_STATES_TABLE");
    }


    public String getAttiniVersion(){
        return System.getenv("ATTINI_VERSION");
    }

}
