Execute the generate_use_case function in `step_workflows.generate_model_from_usecase.py`. Use the output path from the last step as the input to this one. 
After it executes, explain the model and ask the user if they would like to modify the model to better meet their requirements.
Remember the path to the model file it produces as you will need to supply it in the next step.
After it is updated (or they choose not to), suggest running the `@generate-data` prompt.