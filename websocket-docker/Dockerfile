## Pull Python Image from DockerHub
FROM python:3.9.17-slim-bullseye

## Allow statements and log messages to immediately appear in the Knative logs
ENV PYTHONBUFFERED True

## Copy file
COPY requirements.txt ./requirements.txt

## Install Required Packages
RUN pip install -r requirements.txt

## Copy all files to directory
COPY VehiclesWebSocket.py ./VehiclesWebSocket.py

## Expose Port
EXPOSE 8080

## Run Websocket
CMD python VehiclesWebSocket.py

