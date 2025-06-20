#!/bin/env bash
set -euox pipefail

msg() {
    echo >&2 -e "${1-}"
}

die() {
    local msg=$1
    local code=${2-1} # default exit status 1
    msg "$msg"
    exit "$code"
}

parse_params() {
    # default values of variables set from params
    ACCOUNT=$(aws sts get-caller-identity --query Account --output text || true)
    REGION=$(aws configure get region || true)
    REGION=${REGION:-"us-east-1"}
    PIPELINE_NAME=""
    USE_GRAPHBOLT=false
    USE_GSPROCESSING=0
    UPDATE_PIPELINE=0

    while :; do
        case "${1-}" in
        -h | --help) usage ;;
        -x | --verbose) set -x ;;
        -i | --input-s3)
            # Path to raw graph input data
            INPUT_S3="${2-}"
            shift
            ;;
        -c | --config-filename)
            # Name of the GCOnstruct file
            GCONSTRUCT_CONFIG="${2-}"
            shift
            ;;
        -r | --execution-role)
            # SageMaker execution role
            ROLE_ARN="${2-}"
            shift
            ;;
        -a | --account)
            # AWS Account ID.
            ACCOUNT="${2-}"
            shift
            ;;
        -b | --bucket-name)
            # AWS Bucket to use for output
            BUCKET_NAME="${2-}"
            shift
            ;;
        -n | --pipeline-name)
            # Name for the SageMaker Pipeline
            PIPELINE_NAME="${2-}"
            shift
            ;;
        -g | --graphstorm-location)
            # Path to the GraphStorm repository
            GRAPHSTORM_LOCATION="${2-}"
            shift
            ;;
        --train-yaml-s3)
            # S3 URI for the train YAML configuration
            TRAIN_YAML_S3="${2-}"
            shift
            ;;
        --inference-yaml-s3)
            # S3 URI for the train YAML configuration
            INFERENCE_YAML_S3="${2-}"
            shift
            ;;
        --instance-type)
            # S3 URI for the train YAML configuration
            INSTANCE_TYPE="${2-}"
            shift
            ;;
        --use-gsprocessing)
            # Whether to use GSProcessing for graph processing
            USE_GSPROCESSING=1
            ;;
        --use-graphbolt)
            # Whether to use GraphBolt for training/inference
            USE_GRAPHBOLT=true
            ;;
        --update)
            # When set, will try to update an existing pipeline instead of creating a new one.
            UPDATE_PIPELINE=1
            ;;
        -?*) die "Unknown option: $1" ;;
        *) break ;;
        esac
        shift
    done

    # check required params and arguments
    [[ -z "${INPUT_S3-}" ]] && die "Missing required parameter: -i/--input-s3 <s3-input-path>"
    [[ -z "${TRAIN_YAML_S3-}" ]] && die "Missing required parameter: --train-yaml-s3 <train-s3-yaml-path>"
    [[ -z "${INFERENCE_YAML_S3-}" ]] && die "Missing required parameter: --inference-yaml-s3 <train-s3-yaml-path>"
    [[ -z "${ACCOUNT-}" ]] && die "Missing required parameter: -a/--account <aws-account-id>"
    [[ -z "${BUCKET_NAME-}" ]] && die "Missing required parameter: -b/--bucket-name <s3-bucket>"
    [[ -z "${ROLE_ARN-}" ]] && die "Missing required parameter: -r/--execution-role <execution-role-arn>"
    [[ -z "${GRAPHSTORM_LOCATION-}" ]] && die "Missing required parameter: -g/--graphstorm-location <path-to-graphstorm-root>"
    [[ -z "${GCONSTRUCT_CONFIG-}" ]] && die "Missing required parameter: -c/--config-filename <gconstruct-filename>"

    return 0
}

cleanup() {
    trap - SIGINT SIGTERM ERR EXIT
    # script cleanup here
}

parse_params "$@"

DATASET_S3_PATH="$INPUT_S3"
OUTPUT_PATH="s3://${BUCKET_NAME}/pipelines-output"
GRAPH_NAME="ieee-ics-fraud-detection"
# Number of partitions/instances to use during training
INSTANCE_COUNT="2"
# Number of training processes to use on each instance.
# For GPU training, set this to the number of GPUs on each instance
NUM_TRAINERS=4

# GSProcessing parameters
GCONSTRUCT_INSTANCE=${INSTANCE_TYPE:-"ml.m5.4xlarge"}
GSPROCESSING_IMAGE_URI=${ACCOUNT}.dkr.ecr.$REGION.amazonaws.com/graphstorm-processing-sagemaker:latest-x86_64

# DistPartition parameters
PARTITION_OUTPUT_JSON="metadata.json"

# Train/inference parameters
TRAIN_CPU_INSTANCE=${INSTANCE_TYPE:-"ml.m5.4xlarge"}
TRAIN_GPU_INSTANCE=${INSTANCE_TYPE:-"ml.g5.4xlarge"}
TASK_TYPE="node_classification"
# Train/inference images
GSF_CPU_IMAGE_URI=${ACCOUNT}.dkr.ecr.$REGION.amazonaws.com/graphstorm:sagemaker-cpu
GSF_GPU_IMAGE_URI=${ACCOUNT}.dkr.ecr.$REGION.amazonaws.com/graphstorm:sagemaker-gpu
# Size of disk on each instance
VOLUME_SIZE=50

# Which model epoch to use for inference
INFERENCE_MODEL_SNAPSHOT="epoch-19"

# Ensure TRAIN_YAML_S3 exists
if aws s3 ls "$TRAIN_YAML_S3" 2>&1 | grep -q 'NoSuchKey'; then
    die "YAML file does not exist at $TRAIN_YAML_S3"
fi


if [[ -z "${PIPELINE_NAME-}" ]]; then
    if [[ $USE_GRAPHBOLT == "true" ]]; then
        PIPELINE_NAME="$GRAPH_NAME-gsp-graphbolt-pipeline"
    else
        PIPELINE_NAME="$GRAPH_NAME-gsp-pipeline"
    fi
fi


if [[ $USE_GSPROCESSING == 0 ]]; then
    PARTITION_ALGORITHM="metis"
    GRAPH_CONSTRUCTION_ARGS="--num-processes 8 --add-reverse-edges"
    JOBS_TO_RUN="gconstruct train inference"
else
    PARTITION_ALGORITHM="random"
    GRAPH_CONSTRUCTION_ARGS="--add-reverse-edges True"
    JOBS_TO_RUN="gsprocessing dist_part train inference"
    if [[ $USE_GRAPHBOLT == "true" ]]; then
        JOBS_TO_RUN="gsprocessing dist_part gb_convert train inference"
    fi
fi

# Build the base command to launch pipeline with required arguments
PYTHON_CMD="python3 $GRAPHSTORM_LOCATION/sagemaker/pipeline/create_sm_pipeline.py \
    --cpu-instance-type ${TRAIN_CPU_INSTANCE} \
    --execution-role \"${ROLE_ARN}\" \
    --gpu-instance-type ${TRAIN_GPU_INSTANCE} \
    --graph-construction-args \"${GRAPH_CONSTRUCTION_ARGS}\" \
    --graph-construction-config-filename ${GCONSTRUCT_CONFIG} \
    --graph-construction-instance-type ${GCONSTRUCT_INSTANCE} \
    --graph-name ${GRAPH_NAME} \
    --graphstorm-pytorch-cpu-image-uri \"${GSF_CPU_IMAGE_URI}\" \
    --graphstorm-pytorch-gpu-image-uri \"${GSF_GPU_IMAGE_URI}\" \
    --gsprocessing-pyspark-image-uri \"${GSPROCESSING_IMAGE_URI}\" \
    --inference-model-snapshot \"${INFERENCE_MODEL_SNAPSHOT}\" \
    --inference-yaml-s3 \"${INFERENCE_YAML_S3}\" \
    --input-data-s3 \"${DATASET_S3_PATH}\" \
    --instance-count ${INSTANCE_COUNT} \
    --jobs-to-run ${JOBS_TO_RUN} \
    --num-trainers ${NUM_TRAINERS} \
    --output-prefix-s3 \"${OUTPUT_PATH}\" \
    --partition-algorithm ${PARTITION_ALGORITHM} \
    --partition-output-json ${PARTITION_OUTPUT_JSON} \
    --pipeline-name \"${PIPELINE_NAME}\" \
    --region ${REGION} \
    --save-embeddings \
    --save-predictions \
    --train-inference-task ${TASK_TYPE} \
    --train-on-cpu \
    --train-yaml-s3 \"${TRAIN_YAML_S3}\" \
    --use-graphbolt ${USE_GRAPHBOLT} \
    --volume-size-gb ${VOLUME_SIZE}"

# Conditionally add the --update-pipeline flag
if [ "$UPDATE_PIPELINE" = "1" ]; then
    PYTHON_CMD="$PYTHON_CMD --update-pipeline"
fi

# Execute the command
eval "$PYTHON_CMD"
