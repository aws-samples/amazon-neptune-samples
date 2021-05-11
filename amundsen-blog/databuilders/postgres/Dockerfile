FROM python:alpine3.7
COPY . /app
WORKDIR /app
RUN apk add --no-cache --update \
    python3 python3-dev gcc \
    gfortran musl-dev postgresql-dev \
    build-base alpine-sdk
RUN pip install --upgrade pip
RUN pip install -r requirements.txt
CMD python3 ./postgres_extract_neptune_publish.py
