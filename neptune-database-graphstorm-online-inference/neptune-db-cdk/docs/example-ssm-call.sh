aws ssm start-session \
  --target i-064b7add22a5c5f98 \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{
    "portNumber":["8182"],
    "localPortNumber":["8182"],
    "host":["neptunedbcluster-0fbrg4b3wuzy.cluster-cu2i55qhflpm.us-east-1.neptune.amazonaws.com"]
  }'
