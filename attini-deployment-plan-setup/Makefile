DOCKER_BUILD    			?=true
QUARKUS_BUILD_IMAGE			?=quay.io/quarkus/ubi-quarkus-native-image:22.3.0-java17
FUNCTION_NAME   			:=attini-deployment-plan-setup
AWS_REGION					?=eu-west-1

build-and-sync: build sync

build:
	@./mvnw clean package -Pnative -Dquarkus.native.container-build="$(DOCKER_BUILD)" -Dquarkus.native.builder-image=$(QUARKUS_BUILD_IMAGE); \
	cd target/classes/templates; \
	zip -yr --symlinks ../../function.zip .;


sync:
	@aws lambda update-function-code --function-name $(FUNCTION_NAME) --zip-file fileb://target/function.zip --region $(AWS_REGION)
