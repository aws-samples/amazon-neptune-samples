"""Configuration management for NACL."""
import os
from typing import Dict
from dotenv import load_dotenv

class Config:
    """Configuration manager that reads from environment variables."""
    
    def __init__(self, env_file: str = ".env"):
        """Initialize configuration."""
        load_dotenv(env_file)
        
    def get_neptune_config(self) -> Dict[str, str]:
        """Get Neptune configuration."""
        return {
            "writer_endpoint": self._get_config_value("NEPTUNE_WRITER_ENDPOINT")
        }
    
    def get_llm_config(self) -> Dict[str, str]:
        """Get LLM configuration."""
        return {
            "model": self._get_config_value("LLM_MODEL"),
            "region": self._get_config_value("AWS_REGION", default="us-east-1")
        }
    
    def _get_config_value(self, env_key: str, default: str = None) -> str:
        """Get configuration value from environment."""
        value = os.getenv(env_key)
        if value:
            return value
        if default is not None:
            return default
        raise ValueError(f"Configuration value not found for {env_key}")
