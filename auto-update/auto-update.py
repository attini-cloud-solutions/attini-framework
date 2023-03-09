import boto3
import os
import logging
import json
from botocore.config import Config

config = Config(
    retries={
        'max_attempts': 10,
        'mode': 'standard'
    }
)

logger = logging.getLogger()
logger.setLevel(logging.INFO)

cfn_client = boto3.client('cloudformation', config=config)


def get_template_parameters(template_url):
    return cfn_client.get_template_summary(
        TemplateURL=template_url
    )["Parameters"]


def remove_unused_parameters(template_parameters, stack_parameters):
    final_parameters = []

    for sp in stack_parameters:
        if list(filter(lambda tp: tp["ParameterKey"] == sp["ParameterKey"], template_parameters)):
            final_parameters.append(sp)

    return final_parameters


def get_stack_parameters(template_url):
    current_parameters = cfn_client.describe_stacks(
        StackName=os.environ["STACK_NAME"]
    )["Stacks"][0]["Parameters"]

    stack_parameters = []

    for param in current_parameters:
        param["UsePreviousValue"] = True
        del param["ParameterValue"]
        stack_parameters.append(param)

    template_parameters = get_template_parameters(template_url)

    return remove_unused_parameters(template_parameters, stack_parameters)


def update_stack(parameters, template_url):
    cfn_client.update_stack(
        StackName=os.environ["STACK_NAME"],
        TemplateURL=template_url,
        Capabilities=["CAPABILITY_AUTO_EXPAND", "CAPABILITY_NAMED_IAM"],
        Parameters=parameters
    )
    logger.info(f"Updated {os.environ['STACK_NAME']} cloudformation stack")


def lambda_handler(event, context):
    logger.info(f"Got event {json.dumps(event)}")

    if "TemplateUrl" not in event:
        template_url = os.environ["TEMPLATE_URL"]
    else:
        template_url = event["TemplateUrl"]

    logger.info(f"Using template: {template_url}")

    if "TemplateVersion" in event:
        template_url = template_url.replace("latest", event["TemplateVersion"])

    logger.info(f"Using template: {template_url}")

    cfn_parameters = get_stack_parameters(template_url)

    update_stack(cfn_parameters, template_url)

    return event
