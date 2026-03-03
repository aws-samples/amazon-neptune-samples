Execute the generate_use_case function in `step_workflows.generate_data_from_model.py`. Use the input path from the @generate-model step as the first parameter and the output from the @generate-model step as the second parameter. 
If you set up a timeout for the process, make the timeout at least 60 minutes as this process can take a significant amount of time.
Warn the user that this may take a while as it is generating data for the entire graph. Give the user occassional updates to the status so they don't think it is stuck.
Do not suggest to the user to continue to the next step until after this completes, as it requires all of the data to be created in order for the schema to be complete for query generation.
After it executes, tell the user they can use Neptune to view the data directly. Do not mention specific query languages.
After it is updated (or they choose not to), suggest running the `@generate-queries` prompt.