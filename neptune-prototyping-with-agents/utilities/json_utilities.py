import json
import re
from jsonschema import validate, ValidationError

# Common schemas for validation
SCHEMAS = {
    "model": {
        "type": "object",
        "properties": {
            "nodes": {"type": "array"},
            "edges": {"type": "array"}
        },
        "required": ["nodes", "edges"]
    },
    "queries": {
        "type": "object",
        "properties": {
            "queries": {"type": "array"}
        },
        "required": ["queries"]
    }
}

def extract_json(text, schema_name=None):
    """
    Extract and validate JSON from text.
    
    Args:
        text: Text containing JSON
        schema_name: Optional schema name to validate against ('model' or 'queries')
    
    Returns:
        Parsed and validated JSON object
        
    Raises:
        json.JSONDecodeError: If JSON parsing fails
        ValidationError: If schema validation fails
    """
    # Find JSON between code blocks or standalone (using non-greedy matching)
    json_match = re.search(r'```(?:json)?\s*(\{.*?\})\s*```', text, re.DOTALL)
    if json_match:
        json_str = json_match.group(1)
    else:
        # Try to find JSON without code blocks (non-greedy)
        json_match = re.search(r'(\{.*?\})', text, re.DOTALL)
        json_str = json_match.group(1) if json_match else text
    
    try:
        parsed_json = json.loads(json_str.strip())
    except json.JSONDecodeError:
        # Try to fix newlines in query values by processing the entire string
        pattern = r'("query"\s*:\s*")(.*?)("(?=\s*[,}]))'
        def replace_newlines(match):
            query_start = match.group(1)
            query_content = match.group(2)
            query_end = match.group(3)
            # Replace newlines and extra spaces with a single space
            fixed_content = ' '.join(query_content.split())
            return query_start + fixed_content + query_end
            
        fixed_str = re.sub(pattern, replace_newlines, json_str, flags=re.DOTALL)
        parsed_json = json.loads(fixed_str.strip())
    
    # Validate against schema if provided
    if schema_name and schema_name in SCHEMAS:
        validate(instance=parsed_json, schema=SCHEMAS[schema_name])
    
    return parsed_json
