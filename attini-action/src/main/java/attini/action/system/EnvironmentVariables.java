/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package attini.action.system;

import java.util.Optional;

public class EnvironmentVariables {

    public String getSnsNotificationArn(){
        return System.getenv(
                "ATTINI_RESPOND_TO_CFN_EVENT");
    }

    public String getDeployOriginTableName(){
       return System.getenv(
                "ATTINI_DEPLOYMENT_ORIGIN_TABLE");
    }

    public String getAttiniArtifactBucket(){
        return System.getenv("ATTINI_ARTIFACT_BUCKET");
    }

    public String getResourceStatesTableName(){
       return System.getenv("ATTINI_RESOURCE_STATES_TABLE");
    }

    public String getRegion(){
        return System.getenv("AWS_REGION");
    }

    public String getEnvironmentParameterName(){
        return System.getenv("ENVIRONMENT_PARAMETER_NAME");
    }

    public String getAccountId(){
        return System.getenv("AWS_ACCOUNT_ID");
    }

    public String getAttiniVersion(){
       return System.getenv("ATTINI_VERSION");
    }

    public Optional<String> getCompanyContactEmail(){
        return Optional.ofNullable(System.getenv("EMAIL"));
    }

    public String getLicenceToken(){
        return System.getenv("LICENCE_TOKEN");
    }

    public String getDeploymentStatusTopic(){
        return System.getenv("DEPLOYMENT_STATUS_TOPIC");
    }
}
