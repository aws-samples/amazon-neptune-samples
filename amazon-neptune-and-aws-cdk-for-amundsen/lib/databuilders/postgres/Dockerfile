FROM amazonlinux:2
COPY . /app
WORKDIR /app
RUN yum update -y
RUN yum install python3 python3-devel gcc gcc-gfortran postgresql-devel -y
RUN pip3 install --upgrade pip
RUN pip install -r requirements.txt
CMD python3 ./postgres_extract_neptune_publish.py
