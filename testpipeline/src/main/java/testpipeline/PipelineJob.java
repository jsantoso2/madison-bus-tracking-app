package testpipeline;

import datamodel.TripUpdateEvent;
import datamodel.TripUpdateEventDeserializer;
import datamodel.VehiclePositionEvent;
import datamodel.VehiclePositionEventDeserializer;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.StatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.TableConfig;

import java.time.ZoneId;

public class PipelineJob {
    public static void main( String[] args ) throws Exception {

        System.out.println( "Hello World!" );

        // Define Global Variables
        String KAFKA_ADDRESS = "10.128.0.4";
        String KAFKA_INPUT_TOPIC_VEHICLE = "VehiclePositions"; 
        String KAFKA_INPUT_TOPIC_TRIP = "TripUpdates";
        String KAFKA_OUTPUT_TOPIC_VEHICLE = "OutputTopicVehicle";
        String KAFKA_OUTPUT_TOPIC_STOP = "OutputTopicStop";
        String KAFKA_USERNAME = "admin";
        String KAFKA_PASSWORD = "b0da8178c6b593ac597945659c80fae291e6879c892eb29c26202820b276e63d";
        String KAFKA_GROUPID_CONSUMER = "flink_consumer";
        String KAFKA_GROUPID_PRODUCER = "flink_producer";

        // ##################################################################################################################################
        // Configuration for static files in local OR gs bucket
        // ##################################################################################################################################
        // Cluster Run: "gs://learning-gcp-392602-flink-bucket/stops.txt";
        // Local Run:  PipelineJob.class.getResource("/data/stops.txt").getPath(); //(inside src/main/java/resource)
        String STATIC_STOPS_FILEPATH = "gs://learning-gcp-392602-flink-bucket/stops.txt";
        String STATIC_TRIPS_FILEPATH = "gs://learning-gcp-392602-flink-bucket/trips.txt";

        // ##################################################################################################################################
        // Define Flink Configuration
        // ##################################################################################################################################
        // Define Execution environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.STREAMING); //STREAMING OR BATCH
        env.enableCheckpointing(100000);
        //env.getCheckpointConfig().setCheckpointStorage("gs://learning-gcp-383300-flink-bucket/checkpoints"); //Only for cluster deployment
        //env.setParallelism(1);
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);
        TableConfig config = tEnv.getConfig();
        config.setLocalTimeZone(ZoneId.of("UTC"));
        
        // ##################################################################################################################################
        // Define Kafka Inputs
        // ##################################################################################################################################
        
        // Define KafkaSource for VehiclePositionEvent
        KafkaSource<VehiclePositionEvent> vehiclesource = KafkaSource.<VehiclePositionEvent>builder()
                                        .setProperty("security.protocol", "SASL_PLAINTEXT")
                                        .setProperty("sasl.mechanism", "PLAIN")
                                        .setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + KAFKA_USERNAME + "\" password=\"" + KAFKA_PASSWORD + "\";")
                                        .setBootstrapServers(KAFKA_ADDRESS + ":9092")
                                        .setTopics(KAFKA_INPUT_TOPIC_VEHICLE)
                                        .setGroupId(KAFKA_GROUPID_CONSUMER)
                                        .setStartingOffsets(OffsetsInitializer.latest())
                                        .setValueOnlyDeserializer(new VehiclePositionEventDeserializer())
                                        // .setBounded(OffsetsInitializer.latest())
                                        .build();

        // Define Datastream from KafkaSource
        DataStream<VehiclePositionEvent> vehiclestream = env.fromSource(vehiclesource, WatermarkStrategy.noWatermarks(), "Kafka Vehicle Source");
        
        // Create Temporary View
        tEnv.createTemporaryView("VehiclePositionEvent", vehiclestream,         
                                    Schema.newBuilder()   
                                    .primaryKey("id")                                 
                                    .columnByExpression("rowtime", "TO_TIMESTAMP_LTZ(vehicle.`timestamp`, 0)")
                                    .watermark("rowtime", "rowtime - INTERVAL '1' MINUTES")
                                    .build());
        
        // tEnv.executeSql("SELECT * FROM VehiclePositionEvent").print();

        // Define KafkaSource for TripUpdateEvent
        KafkaSource<TripUpdateEvent> tripsource = KafkaSource.<TripUpdateEvent>builder()
                                        .setProperty("security.protocol", "SASL_PLAINTEXT")
                                        .setProperty("sasl.mechanism", "PLAIN")
                                        .setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + KAFKA_USERNAME + "\" password=\"" + KAFKA_PASSWORD + "\";")
                                        .setBootstrapServers(KAFKA_ADDRESS + ":9092")
                                        .setTopics(KAFKA_INPUT_TOPIC_TRIP)
                                        .setGroupId(KAFKA_GROUPID_CONSUMER)
                                        .setStartingOffsets(OffsetsInitializer.latest())
                                        .setValueOnlyDeserializer(new TripUpdateEventDeserializer())
                                        // .setBounded(OffsetsInitializer.latest())
                                        .build();

        // Define Datastream from KafkaSource
        DataStream<TripUpdateEvent> tripstream = env.fromSource(tripsource, WatermarkStrategy.noWatermarks(), "Kafka Vehicle Source");

        // Create Temporary View
        tEnv.createTemporaryView("TripUpdateEvent", tripstream,         
                    Schema.newBuilder()        
                    .primaryKey("id")                            
                    .columnByExpression("rowtime", "TO_TIMESTAMP_LTZ(tripUpdate.`timestamp`,0)")
                    .watermark("rowtime", "rowtime - INTERVAL '1' MINUTES")
                    .build());
        
        // tEnv.executeSql("SELECT * FROM TripUpdateEvent").print();

        // ##################################################################################################################################
        // Define CSV Inputs
        // ##################################################################################################################################
        // CSV Source
        tEnv.executeSql(String.join(
                    "\n",
                    "CREATE TABLE TripLookupTable ( ",
                    "    `route_id` STRING, ", 
                    "    `route_short_name` STRING, ",
                    "    `service_id` STRING, ", 
                    "    `trip_id` STRING, ",
                    "    `trip_headsign` STRING, ",
                    "    `direction_id` STRING, ", 
                    "    `trip_direction_name` STRING, ", 
                    "    `block_id` STRING, ", 
                    "    `shape_id` STRING, ", 
                    "    `shape_code` STRING, ",
                    "    `trip_type` STRING, ", 
                    "    `trip_sort` STRING, ", 
                    "    `trip_short_name` STRING, ",
                    "    `block_transfer_id` STRING, ", 
                    "    `wheelchair_accessible` INTEGER, ",
                    "    `bikes_allowed` INTEGER",
                    "    ) WITH ( ",
                    "    'connector' = 'filesystem', ",
                    "    'format' = 'csv', ",
                    "    'path' = '" + STATIC_TRIPS_FILEPATH + "' ", 
                    ");"));

        // tEnv.executeSql("SELECT * FROM TripLookupTable").print();

        tEnv.executeSql(String.join(
                    "\n",
                    "CREATE TABLE StopLookupTable ( ",
                    "    `stop_id` STRING, ", 
                    "    `stop_code` STRING, ",
                    "    `stop_name` STRING, ", 
                    "    `stop_desc` STRING, ",
                    "    `stop_lat` STRING, ",
                    "    `stop_lon` STRING, ", 
                    "    `agency_id` STRING, ", 
                    "    `jurisdiction_id` STRING, ", 
                    "    `location_type` STRING, ", 
                    "    `parent_station` STRING, ",
                    "    `relative_position` STRING, ", 
                    "    `cardinal_direction` STRING, ", 
                    "    `wheelchair_boarding` STRING, ",
                    "    `primary_street` STRING, ", 
                    "    `address_range` STRING, ",
                    "    `cross_location` STRING",
                    "    ) WITH ( ",
                    "    'connector' = 'filesystem', ",
                    "    'format' = 'csv', ",
                    "    'path' = '" + STATIC_STOPS_FILEPATH + "' ", 
                    ");"));

        // tEnv.executeSql("SELECT * FROM StopLookupTable").print();

        // ##################################################################################################################################
        // Kafka Sink
        // ##################################################################################################################################        
        // Upsert to Kafka Topic
        tEnv.executeSql(String.join(
                        "\n",
                        "CREATE TABLE OutputTableVehicle ( ",
                        "    id STRING, ",
                        "    tripId STRING, ",
                        "    shapeId STRING, ",
                        "    routeName STRING, ",
                        "    tripHeadSign STRING, ", 
                        "    startDate STRING, ",
                        "    startTime STRING, ", 
                        "    latitude FLOAT, ",
                        "    longitude FLOAT, ",
                        "    updateTime STRING, ",
                        "    nextStopSequence STRING, ",
                        "    nextStopId STRING, ",
                        "    nextStopName STRING, ", 
                        "    nextStopApproachSec BIGINT, ", 
                        "    nextStopDelay STRING, ",
                        "    ingestionTime STRING, ", 
                        "    PRIMARY KEY (id) NOT ENFORCED",
                        "    ) WITH ( ",
                        "    'connector' = 'upsert-kafka', ",
                        "    'topic' = '" + KAFKA_OUTPUT_TOPIC_VEHICLE + "', ",
                        "    'properties.bootstrap.servers' = '" + KAFKA_ADDRESS + ":9092', ",
                        "    'properties.group.id' = '" + KAFKA_GROUPID_PRODUCER + "', ", 
                        "    'key.format' = 'raw',",
                        "    'value.format' = 'json', ",
                        "    'properties.security.protocol' = 'SASL_PLAINTEXT', ",
                        "    'properties.sasl.mechanism' = 'PLAIN', ",
                        "    'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + KAFKA_USERNAME + "\" password=\"" + KAFKA_PASSWORD + "\";' ",
                        ");"));

        // Upsert to Kafka Topic
        tEnv.executeSql(String.join(
                        "\n",
                        "CREATE TABLE OutputTableStop ( ",
                        "    stopId STRING, ",
                        "    stopName STRING, ",
                        "    stopLat STRING, ",
                        "    stopLon STRING, ", 
                        "    info STRING, ",
                        "    PRIMARY KEY (stopId) NOT ENFORCED",
                        "    ) WITH ( ",
                        "    'connector' = 'upsert-kafka', ",
                        "    'topic' = '" + KAFKA_OUTPUT_TOPIC_STOP + "', ",
                        "    'properties.bootstrap.servers' = '" + KAFKA_ADDRESS + ":9092', ",
                        "    'properties.group.id' = '" + KAFKA_GROUPID_PRODUCER + "', ", 
                        "    'key.format' = 'raw',",
                        "    'value.format' = 'json', ",
                        "    'properties.security.protocol' = 'SASL_PLAINTEXT', ",
                        "    'properties.sasl.mechanism' = 'PLAIN', ",
                        "    'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + KAFKA_USERNAME + "\" password=\"" + KAFKA_PASSWORD + "\";' ",
                        ");"));

    
        String sql1 = String.join("\n",
                           "INSERT INTO OutputTableVehicle", 
                           "SELECT id, tripId, shapeId, routeName, tripHeadSign, startDate, startTime, latitude, longitude, updateTime, nextStopSequence, nextStopId, nextStopName, nextStopApproachSec, nextStopDelay, ingestionTime",
                           "FROM (",
                           "SELECT a.id, ", 
                           "       a.vehicle.trip.tripId AS tripId, ",
                           "       c.shape_id AS shapeId, ",
                           "       c.route_short_name AS routeName, ",
                           "       c.trip_headsign AS tripHeadSign, ",
                           "       CAST(TO_DATE(a.vehicle.trip.startDate, 'yyyyMMdd') AS STRING) AS `startDate`, ",
                           "       b.tripUpdate.trip.startTime AS startTime, ",
                           "       a.vehicle.`position`.latitude, ",
                           "       a.vehicle.`position`.longitude, ",
                           "       CONVERT_TZ(FROM_UNIXTIME(a.vehicle.`timestamp`), 'UTC', 'America/Chicago') AS `updateTime`,  ",
                           "       CASE WHEN (CAST(b.tripUpdate.stopTimeUpdate[1].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[1].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[2].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[2].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[2].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[3].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[3].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[3].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[4].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[4].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[4].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[5].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[5].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[5].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[6].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[6].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[6].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[7].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[7].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[7].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[8].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[8].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[8].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[9].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[9].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[9].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[10].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[10].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[10].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[11].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[11].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[11].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[12].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[12].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[12].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[13].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[13].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[13].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[14].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[14].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[14].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[15].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[15].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[15].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[16].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[16].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[16].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[17].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[17].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[17].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[18].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[18].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[18].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[19].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[19].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[19].stopSequence",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[20].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[20].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[20].stopSequence",
                           "            ELSE CAST(0 AS STRING) END AS nextStopSequence, ",
                           "       CASE WHEN (CAST(b.tripUpdate.stopTimeUpdate[1].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[1].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[2].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[2].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[2].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[3].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[3].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[3].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[4].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[4].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[4].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[5].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[5].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[5].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[6].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[6].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[6].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[7].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[7].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[7].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[8].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[8].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[8].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[9].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[9].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[9].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[10].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[10].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[10].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[11].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[11].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[11].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[12].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[12].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[12].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[13].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[13].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[13].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[14].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[14].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[14].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[15].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[15].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[15].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[16].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[16].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[16].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[17].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[17].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[17].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[18].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[18].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[18].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[19].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[19].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[19].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[20].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[20].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[20].stopId",
                           "            ELSE CAST(0 AS STRING) END AS nextStopId, ",
                           "       d.stop_name AS nextStopName, ",
                           "       CASE WHEN (CAST(b.tripUpdate.stopTimeUpdate[1].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[1].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[2].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[2].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[2].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[2].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[3].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[3].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[3].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[3].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[4].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[4].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[4].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[4].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[5].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[5].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[5].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[5].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[6].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[6].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[6].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[6].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[7].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[7].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[7].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[7].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[8].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[8].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[8].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[8].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[9].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[9].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[9].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[9].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[10].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[10].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[10].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[10].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[11].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[11].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[11].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[11].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[12].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[12].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[12].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[12].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[13].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[13].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[13].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[13].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[14].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[14].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[14].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[14].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[15].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[15].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[15].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[15].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[16].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[16].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[16].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[16].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[17].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[17].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[17].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[17].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[18].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[18].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[18].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[18].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[19].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[19].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[19].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[19].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[20].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[20].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN CAST(b.tripUpdate.stopTimeUpdate[20].departure.`time` AS BIGINT) + CAST(b.tripUpdate.stopTimeUpdate[20].departure.`delay` AS BIGINT) - b.`timestamp`",
                           "            ELSE CAST(0 AS BIGINT) END AS nextStopApproachSec, ",
                           "       b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS nextStopDelay, ",
                           "       CONVERT_TZ(CAST(CURRENT_TIMESTAMP AS STRING), 'UTC', 'America/Chicago') AS `ingestionTime` ", 
                           "FROM VehiclePositionEvent a",
                           "LEFT JOIN TripUpdateEvent b ON a.vehicle.trip.tripId = b.id",
                           "LEFT JOIN TripLookupTable c ON a.vehicle.trip.tripId = c.trip_id",
                           "LEFT JOIN StopLookupTable d ON",
                           "       CASE WHEN (CAST(b.tripUpdate.stopTimeUpdate[1].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[1].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[1].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[2].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[2].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[2].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[3].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[3].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[3].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[4].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[4].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[4].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[5].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[5].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[5].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[6].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[6].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[6].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[7].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[7].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[7].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[8].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[8].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[8].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[9].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[9].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[9].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[10].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[10].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[10].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[11].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[11].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[11].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[12].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[12].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[12].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[13].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[13].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[13].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[14].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[14].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[14].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[15].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[15].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[15].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[16].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[16].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[16].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[17].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[17].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[17].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[18].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[18].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[18].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[19].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[19].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[19].stopId",
                           "            WHEN (CAST(b.tripUpdate.stopTimeUpdate[20].departure.`time` AS BIGINT) + COALESCE(CAST(b.tripUpdate.stopTimeUpdate[20].departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= b.`timestamp`) THEN b.tripUpdate.stopTimeUpdate[20].stopId",
                           "            ELSE CAST(0 AS STRING) END = d.stop_id",
                          ")"
                           );

        // tEnv.executeSql(sql1).print();
        
        String sql2 = String.join("\n",
                    "INSERT INTO OutputTableStop", 
                    "SELECT stopId, stopName, stopLat, stopLon, CAST(COLLECT((id, routeName, tripHeadSign, CONVERT_TZ(FROM_UNIXTIME(CAST(arrivalTime AS INTEGER)), 'UTC', 'America/Chicago'), delay)) AS STRING) AS info",
                    "FROM (", 
                        "SELECT a.id, a.routeName, a.tripHeadSign, b.stopId, c.stop_name AS stopName, c.stop_lat AS stopLat, c.stop_lon AS stopLon, COALESCE(b.departure.`time`, b.arrival.`time`) AS arrivalTime, COALESCE(b.departure.`delay`, b.arrival.`delay`) AS delay",
                        "FROM (", 
                            "SELECT a.id, ", 
                            "       a.tripUpdate.stopTimeUpdate, ",
                            "       a.`timestamp`, ", 
                            "       b.route_short_name AS routeName, ",
                            "       b.trip_headsign AS tripHeadSign, ", 
                            "       CONVERT_TZ(CAST(CURRENT_TIMESTAMP AS STRING), 'UTC', 'America/Chicago') AS `ingestionTime`", 
                            "FROM TripUpdateEvent a",
                            "JOIN TripLookupTable b ON a.id = b.trip_id",
                        ") a",
                        "CROSS JOIN UNNEST(a.stopTimeUpdate) b",
                        "LEFT JOIN StopLookupTable c ON b.stopId = c.stop_id",
                        "WHERE (CAST(b.departure.`time` AS BIGINT) + COALESCE(CAST(b.departure.`delay` AS BIGINT), CAST(0 AS BIGINT)) >= a.`timestamp`)",
                    ")",
                    "GROUP BY stopId, stopName, stopLat, stopLon;"
                    );
            
        // Execute Jobs together through StatementSet
        StatementSet statementset = tEnv.createStatementSet();

        // only single INSERT query can be accepted by `add_insert_sql` method
        statementset.addInsertSql(sql1);
        statementset.addInsertSql(sql2);
        statementset.execute();
    }
}






        // import org.apache.flink.connector.jdbc.catalog.JdbcCatalog;
        // <dependency>
        //   <groupId>mysql</groupId>
        //   <artifactId>mysql-connector-java</artifactId>
        //   <version>8.0.30</version>
        // </dependency>
        // // ##################################################################################################################################
        // // MySQL SINK
        // // ##################################################################################################################################
        // // Create MySQL Database Connection
        // Class.forName("com.mysql.cj.jdbc.Driver"); 
        // String name            = "my_catalog";
        // String defaultDatabase = "testDB";
        // String username        = "root";
        // String password        = "admin";
        // String baseUrl         = "jdbc:mysql://10.116.112.9:3306";

        // // Register MySQL Catalog
        // JdbcCatalog catalog = new JdbcCatalog(name, defaultDatabase, username, password, baseUrl);
        // tEnv.registerCatalog("my_catalog", catalog);

        // // set the JdbcCatalog as the current catalog of the session
        // tEnv.useCatalog("my_catalog");
        // System.out.println("connected to db!");

        // // Create Temporary View
        // tEnv.createTemporaryView("VehiclePositionEvent", teststream,         
        //                             Schema.newBuilder()                                    
        //                             .columnByExpression("rowtime", "TO_TIMESTAMP_LTZ(vehicle.`timestamp`, 0)")
        //                             .watermark("rowtime", "rowtime - INTERVAL '1' HOURS")
        //                             .build());

        // // Insert into MySQL Table for Analytics
        // tEnv.executeSql(String.join(
        //                             "\n",
        //                             "INSERT INTO VehiclePositionsDataTable ",
        //                             "SELECT id, tripId, startDate, routeId, latitude, longitude, bearing, updateTime, vehicleLabel, occupancyStatus, occupancyPercentage ",
        //                             "FROM (",
        //                                 "SELECT ",
        //                                     "id, ", 
        //                                     "vehicle.trip.tripId, ",
        //                                     "CAST(TO_DATE(vehicle.trip.startDate, 'yyyyMMdd') AS STRING) AS `startDate`, ",
        //                                     "vehicle.trip.routeId, ",
        //                                     "vehicle.`position`.latitude, ",
        //                                     "vehicle.`position`.longitude, ",
        //                                     "vehicle.`position`.bearing, ",
        //                                     "CONVERT_TZ(FROM_UNIXTIME(vehicle.`timestamp`), 'UTC', 'America/Chicago') AS `updateTime`, ",
        //                                     "vehicle.vehicle.id AS `vehicleLabel`, ",
        //                                     "vehicle.occupancyStatus, ",
        //                                     "vehicle.occupancyPercentage ",
        //                                 "FROM VehiclePositionEvent",
        //                             ");"));


        // // Read Directly from Kafka struggle to get nested JSON structure
        // // Kafka Topic Source
        // tEnv.executeSql(String.join(
        //             "\n",
        //             "CREATE TABLE OutputTable ( ",
        //             "    `id` STRING, ",
        //             "    `vehicle` ROW(`trip` STRING, `position` STRING, `timestamp` STRING, `vehicle` ROW(`id` STRING, `label` STRING))",
        //             "    ) WITH ( ",
        //             "    'connector' = 'kafka', ",
        //             "    'topic' = '" + KAFKA_INPUT_TOPIC + "', ",
        //             "    'properties.bootstrap.servers' = '" + KAFKA_ADDRESS + ":9092', ",
        //             "    'properties.group.id' = 'flink-producer', ", 
        //             "    'key.fields' = 'id', ",
        //             "    'key.format' = 'raw',",
        //             "    'value.format' = 'json', ",
        //             "    'scan.startup.mode' = 'earliest-offset', ",
        //             "    'properties.security.protocol' = 'SASL_PLAINTEXT', ",
        //             "    'properties.sasl.mechanism' = 'PLAIN', ",
        //             "    'properties.sasl.jaas.config' = 'org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + KAFKA_USERNAME + "\" password=\"" + KAFKA_PASSWORD + "\";' ",
        //             ");"));
