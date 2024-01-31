# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0

import json
import boto3
import pandas as pd
from langchain.llms.bedrock import Bedrock
from langchain.chains import ConversationChain
from langchain.memory import ConversationBufferMemory

class BedrockAPI:
    """
    A class for interacting with different models through the Bedrock API.

    Methods:
    - call_claude_v2_model(question: str) -> str:
        Calls the Claude-v2 model via the Bedrock API and returns the model's response for the given question.

    - call_AI21Lab_model(question: str) -> str:
        Calls the AI21Lab model via the Bedrock API and returns the model's response for the given question.
        Uses a custom prompt generated internally.

    - call_bedrock_model(question: str) -> str:
        Calls a Bedrock model (currently set to claude-v2) and returns the model's response for the given question.

    Attributes:
    - None: The class does not have any specific attributes.

    Usage:
    - Initialize an instance of BedrockAPI and use its methods to interact with different models.
    """
    def __init__(self, profile_name='default', region_name='us-east-1'):
        """
        Initializes the BedrockAPI instance with a Neptune client and graphId.

        Parameters:
        - profile_name (str): The name of the AWS profile. Defaults to "default".
        - region_name (str): The AWS region name. Defaults to "us-east-1".
        """
        self.session = boto3.Session(profile_name=profile_name)
        self.bedrock = self.session.client(service_name='bedrock-runtime', region_name=region_name)


    def call_claude_v2_model(self, question):
        """
        Calls the Claude-v2 model via the Bedrock API.

        Args:
        - question (str): The input question for the model.

        Returns:
        - str: The model's response for the given question.
        """
        llm = Bedrock(model_id="anthropic.claude-v2", region_name='us-east-1', 
                        client=self.bedrock, 
                        model_kwargs={"max_tokens_to_sample": 1000, "temperature": 0.0})
        conversation = ConversationChain(
            llm=llm, verbose=True, memory=ConversationBufferMemory()
        )
        claude_prompt = f"\n\nHuman: {question}  \n\nAssistant:"
        response = conversation.predict(input=claude_prompt)
        return response

    def call_AI21Lab_model(self, question):
        """
        Calls the AI21Lab model via the Bedrock API.

        Args:
        - question (str): The input question for the model.

        Returns:
        - str: The model's response for the given question.
        """
        prompt = self.generate_custom_prompt(question)

        bedrock_model_id = "ai21.j2-ultra-v1"  # Set the foundation model
        # bedrock_model_id = "anthropic.claude-v2"

        body = json.dumps({
            "prompt": prompt,
            "maxTokens": 1024,
            "temperature": 1e-11,
            "topP": 0.9,
            "stopSequences": [],
            "countPenalty": {"scale": 0},
            "presencePenalty": {"scale": 0},
            "frequencyPenalty": {"scale": 0}
        })  # Build the request payload

        response = self.bedrock.invoke_model(body=body, modelId=bedrock_model_id, accept='application/json', contentType='application/json')  # Send the payload to Bedrock

        response_body = json.loads(response.get('body').read())  # Read the response
        response_text = response_body.get("completions")[0].get("data").get("text")  # Extract the text from the JSON response
        return response_text

    def call_bedrock_model(self, question):
        """
        Calls a Bedrock model (currently set to claude-v2) via the Bedrock API.

        Args:
        - question (str): The input question for the model.

        Returns:
        - str: The model's response for the given question.
        """
        # Using claude-v2 model.
        # TODO: add another parameter to choose model type.
        return self.call_claude_v2_model(question)