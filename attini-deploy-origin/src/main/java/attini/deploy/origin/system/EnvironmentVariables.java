package attini.deploy.origin.system;

import org.jboss.logging.Logger;

public class EnvironmentVariables {

    private final static Logger logger = Logger.getLogger(EnvironmentVariables.class);


    public String getAttiniConfigFile() {
        return System.getenv("ATTINI_CONFIG_FILE");
    }

    public String getAwsRegion() {
        return System.getenv("AWS_REGION");
    }

    public String getArtifactBucket() {
        return System.getenv("ATTINI_ARTIFACT_BUCKET");
    }

    public String getEnvironmentParameterName(){
        return System.getenv("ENVIRONMENT_PARAMETER_NAME");
    }

    public String getInitStackNotificationArn(){
        return System.getenv("ATTINI_RESPOND_TO_INIT_DEPLOY_CFN_EVENT");
    }

    public String getResourceStatesTableName(){
        return System.getenv("ATTINI_RESOURCE_STATES_TABLE");
    }

    public String getStepGuardName(){
        return System.getenv("ATTINI_STEP_GUARD");
    }

    public String getAttiniVersion(){
       return System.getenv("ATTINI_VERSION");
    }

    public int getRetainDistributionDays(){
        try{
            int retainDistributionDays = Integer.parseInt(System.getenv("RETAIN_DISTRIBUTION_DAYS"));
            if (retainDistributionDays < 0){
                logger.error("RETAIN_DISTRIBUTION_DAYS environment variable must be a number greater then zero");
                throw new IllegalEnvironmentVariableException("RETAIN_DISTRIBUTION_DAYS value is invalid");
            }
            return retainDistributionDays;
        }catch (NumberFormatException e){
            logger.error("RETAIN_DISTRIBUTION_DAYS environment variable must be a number",e );
            throw new IllegalEnvironmentVariableException("RETAIN_DISTRIBUTION_DAYS value is invalid");
        }
    }

    public int getRetainDistributionVersions(){
        try{

            int retainDistributionVersions = Integer.parseInt(System.getenv("RETAIN_DISTRIBUTION_VERSIONS"));
            if (retainDistributionVersions < 0){
                logger.error("RETAIN_DISTRIBUTION_VERSIONS environment variable must be a number greater then zero");
                throw new IllegalEnvironmentVariableException("RETAIN_DISTRIBUTION_VERSIONS value is invalid");
            }
            return retainDistributionVersions;

        }catch (NumberFormatException e){
            logger.error("RETAIN_DISTRIBUTION_VERSIONS environment variable must be a number",e );
            throw new IllegalEnvironmentVariableException("RETAIN_DISTRIBUTION_VERSIONS value is invalid");
        }

    }

    public String getDeployDataTableName(){
        return System.getenv("ATTINI_DEPLOYMENT_ORIGIN_TABLE");
    }

    public String getDeploymentStatusTopic(){
        return System.getenv("DEPLOYMENT_STATUS_TOPIC");
    }

}
