# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: MIT-0
import os
import json

with open(f'{os.getenv("FRONTEND_PATH")}/src/config.json', "w") as f:
    f.writelines(json.dumps(
        {"SERVER_URL": os.getenv("API_GATEWAY_INVOKE_URL")}, indent=2))

with open(f'{os.getenv("FRONTEND_PATH")}/src/aws-exports.js', "w") as f:
    f.write('const awsmobile = {\n')
    f.write(f'\taws_project_region: "{os.getenv("REGION")}",\n')
    f.write(
        f'\taws_cognito_identity_pool_id: "{os.getenv("IDENTITY_POOL_ID")}",\n')
    f.write(f'\taws_cognito_region: "{os.getenv("REGION")}",\n')
    f.write("\toauth: {},\n")
    f.write('\taws_bots: "enable",\n')
    f.write('\taws_bots_config: [{\n')
    f.write('\t\tname: "blog_chatbot",\n')
    f.write('\t\talias: "$LATEST",\n')
    f.write(f'\t\tregion: "{os.getenv("REGION")}"\n')
    f.write('}]};\n')
    f.write('export default awsmobile;\n')
