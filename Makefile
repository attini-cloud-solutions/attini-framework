DOCKER_BUILD    					?=true
QUARKUS_BUILD_IMAGE					?=quay.io/quarkus/ubi-quarkus-native-image:22.3-java17
AWS_REGION							?=eu-west-1
ENVIRONMENT_PARAMETER_NAME			?=AttiniEnvironmentName
APP_NAME   						 	=attini-setup


build:
	@./mvnw clean package -Dquarkus.native.container-build="$(DOCKER_BUILD)" -Dquarkus.native.builder-image=$(QUARKUS_BUILD_IMAGE); \
	cd attini-deployment-plan-setup/target/classes/templates; \
	zip -yr --symlinks ../../function.zip .;

build-native:
	@./mvnw clean package -Pnative -Dquarkus.native.container-build="$(DOCKER_BUILD)" -Dquarkus.native.builder-image="$(QUARKUS_BUILD_IMAGE)"; \
	cd attini-deployment-plan-setup/target/classes/templates; \
	zip -yr --symlinks ../../function.zip .;


deploy:
	@sam deploy --resolve-s3 --s3-prefix $(APP_NAME) --stack-name $(APP_NAME) --region $(AWS_REGION) --no-confirm-changeset --capabilities CAPABILITY_NAMED_IAM \
			--parameter-overrides 	  AcceptLicenseAgreement=true \
																CreateDeploymentPlanDefaultRole=false \
																CreateInitDeployDefaultRole=false \
																GiveAdminAccess=false \
																EnvironmentParameterName=$(ENVIRONMENT_PARAMETER_NAME)

build-and-deploy: build-native deploy
