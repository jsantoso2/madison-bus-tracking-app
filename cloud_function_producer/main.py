## Get Data Imports
from google.transit import gtfs_realtime_pb2
from google.protobuf.json_format import MessageToDict
import requests

## Kafka Imports
import json
from json import dumps
from kafka import KafkaProducer
from time import sleep

def getVehiclePositions(event, context):
  """
  Args:
    event (dict): Event Payload
    context (google.cloud.function.Context): Metadata for event
  """

  print("event: ", event)
  print("context: ", context)
  

  ## Global Variables
  URL_VEHICLES = 'http://transitdata.cityofmadison.com/Vehicle/VehiclePositions.pb'
  URL_TRIPS = 'http://transitdata.cityofmadison.com/TripUpdate/TripUpdates.pb'
  KAFKA_ADDRESS = '10.128.0.3' ## Internal IP of KafkaVM
  KAFKA_TOPIC_VEHICLES = 'VehiclePositions'
  KAFKA_TOPIC_TRIPS = 'TripUpdates'

  ## Kafka Producer
  producer = KafkaProducer(bootstrap_servers = [KAFKA_ADDRESS + ":9092"],
                          sasl_mechanism = 'PLAIN',
                          sasl_plain_username = 'admin',
                          sasl_plain_password = '2d15dc73ddb202b5bfa5f451bd684ffc25bf651a987cbe6a0f541239e94377db',
                          security_protocol = 'SASL_PLAINTEXT',
                          api_version=(0,11,5),
                          value_serializer = lambda x: dumps(x).encode('utf-8'),
                          acks = 1, ## Default -> Broker Recieve Ack
                          batch_size = 16384, ## Default Value
                          compression_type = 'gzip')
  
  ######################## Vehicle Messages #####################################
  ## Get Feed from URL
  FEED = gtfs_realtime_pb2.FeedMessage()
  RESPONSE = requests.get(URL_VEHICLES)
  
  ## Parse Response String
  FEED.ParseFromString(RESPONSE.content)

  ## Send to Kafka Topic
  for idx, elem in enumerate(FEED.entity):
      print("Vehicle Messages sending index:", idx)
      producer.send(KAFKA_TOPIC_VEHICLES, value=MessageToDict(elem)).get(timeout=30)
  
  ######################## Trips Messages #####################################
  ## Get Feed from URL
  FEED = gtfs_realtime_pb2.FeedMessage()
  RESPONSE = requests.get(URL_TRIPS)
  
  ## Parse Response String
  FEED.ParseFromString(RESPONSE.content)

  ## Send to Kafka Topic
  for idx, elem in enumerate(FEED.entity):
      print("Trips Messages sending index:", idx)
      producer.send(KAFKA_TOPIC_TRIPS, value=MessageToDict(elem)).get(timeout=30)
