from functools import partial
import asyncio
import json
import threading
import websockets
from kafka import KafkaConsumer

## Global Variables
KAFKA_ADDRESS = "10.128.0.4"
VEHICLE_KAFKA_TOPIC = "OutputTopicVehicle"
VEHICLE_WEBSOCKET_PORT = 8080
GROUP_ID = "Websocket_python_consumger_group"

## Method to run consumer groups
def run_consumer(shutdown_flag, clients, lock):
    print("Starting Kafka Consumer.")
    consumer = KafkaConsumer(VEHICLE_KAFKA_TOPIC,
                         group_id = GROUP_ID,
                         bootstrap_servers=[KAFKA_ADDRESS + ':9092'],
                         sasl_mechanism = 'PLAIN',
                         sasl_plain_username= 'admin',
                         sasl_plain_password= 'b0da8178c6b593ac597945659c80fae291e6879c892eb29c26202820b276e63d',
                         security_protocol = 'SASL_PLAINTEXT',
                         auto_offset_reset='latest',
                         enable_auto_commit=True,
                         api_version=(0,11,5))
    
    for message in consumer:
        if not shutdown_flag.done():
            if message.value is not None:
                formatted_msg = json.loads(message.value) 
                print(f"Sending {formatted_msg} to {clients}")
                
                ## Broadcast Message to Clients
                with lock:
                    websockets.broadcast(clients, message.value.decode('utf-8'))

    print("Closing Kafka Consumer")
    consumer.close()
        

## Handle connections to WebSockets
async def handle_connection(clients, lock, connection):
    with lock:
        clients.add(connection)
    await connection.wait_closed()
    with lock:
        clients.remove(connection)


async def main():
    ### Define Parameters
    ## shutdown_flag = Future that will never be fulfilled
    ## clients = set of clients to publish messages
    ## lock = to excecute in multi-thread safely
    shutdown_flag = asyncio.futures.Future()
    clients = set()
    lock = threading.Lock()

    ## Get Kafka consumer and publish message
    asyncio.events.get_event_loop().run_in_executor(None, run_consumer, shutdown_flag, clients, lock)

    ## Publish Websocker
    print("Starting WebSocket Server.")
    try:
        ## Run Forever
        async with websockets.serve(partial(handle_connection, clients, lock), "", VEHICLE_WEBSOCKET_PORT):
            await asyncio.futures.Future()
    finally:
        shutdown_flag.set_result(True)

## Run Main Program
asyncio.run(main())