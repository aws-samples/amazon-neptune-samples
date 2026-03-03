Check if a `.env` file exists.  If it does not, create it and prompt the user for the 3 parameters required:
- Neptune Writer Endpoint: The writer endpoint of the Neptune cluster they wish to use.  If they do not know it, use the AWS CLI to give them the list of choices.
- LLM Model: Use `global.anthropic.claude-sonnet-4-5-20250929-v1:0` by default but let them choose another if they desire.
- AWS Region: Let them choose the proper region. Default to us-east-1.
After configuration is complete, suggest they run the @generate-usecase prompt to get started.