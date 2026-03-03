from strands.models import BedrockModel
from botocore.config import Config

def get_bedrock_model(model_id, region_name):

    _client_config = Config(
        region_name = region_name,
        signature_version = 'v4',
        connect_timeout=120,
        read_timeout=120,
        retries = {
            'total_max_attempts': 99,
            'mode': 'adaptive'
        }
    )
    bedrock_model = BedrockModel(
        model_id=model_id,
        region_name=region_name,
        cache_prompt="default",
        cache_tools="default",
        boto_client_config=_client_config
    )
    return bedrock_model