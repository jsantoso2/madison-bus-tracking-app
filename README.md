# madison-bus-tracking-app
- Create Cloud Scheduler job to pull data from Madison Transit Website and publish to Kafka Topic
- Process Kafka Messages using Flink Streaming Job and Publish back to Kafka Topic - VM / GKE [+ Prometheus/Grafana Monitoring] 
- Read from Kafka Topic to publish messages to Websocket
- Connect Websocket to React App for Visualization
- All services are hosted on GCP

### Website Link (Valid until Dec 1st 2023): [https://learning-gcp-405504.uc.r.appspot.com](https://learning-gcp-405504.uc.r.appspot.com/)
  
### Purpose + Goal:
- Learn technologies (Terraform, Kafka, Flink, Networking in GCP, GKE, Websockets, React), NOT producing the best/optimal architecture

### Architecture Diagram:
<p align="center"> <img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/ArchitectureDiagram.png height="400"></p>
<p>NOTE: Pull message every 2 minutes + Data pulls suspended from 12AM - 5AM CT (to reduce strain on Madison Transit Server) </p>
<p>NOTE: All GCP resources only have Private IP </p>

#### Data Sources:
- Madison Transit Data: https://www.cityofmadison.com/metro/business/information-for-developers
  - GTFS-RT Trip Updates = Live feed for trip updates and delay
  - GTFS-RT Vehicle Positions = Live feed of Vehicle Positions
  - GTFS Schedule data = Static dataset for stops/schedule
- GTFS Schema: https://github.com/MobilityData/gtfs-realtime-bindings/tree/master/python

#### Sample Data:
<table>
  <tr>
    <td>Input Msg VehiclePositions</td>
    <td>Input Msg Trips</td>
  </tr>
  <tr>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/InputMsgVehicle.png height="250"></td>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/InputMsgTrip.png height="250"></td>
  </tr>
  <tr>
    <td>Output Msg Vehicle</td>
    <td>Output Msg Stop</td>
  </tr>
  <tr>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/OutputMsgVehicle.png height="200"></td>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/OutputMsgStops.png height="200"></td>
  </tr>
</table>

#### Flink
<table>
  <tr>
    <td>Flink UI</td>
    <td>Flink Job</td>
  </tr>
  <tr>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/FlinkUI.png height="200"></td>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/FlinkJob.png height="200"></td>
  </tr>
  <tr>
    <td>Grafana on GKE only</td>
    <td>Prometheus on GKE only</td>
  </tr>
  <tr>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/SampleGrafanaDash.png height="200"></td>
    <td valign="top"><img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/SamplePrometheus.png height="125"></td>
  </tr>
</table>

#### Application UI:
<p>1. Waiting For Messages: </p>
<img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/AppStart.png height="375">
<p>2. Messages Available: </p>
<img src=https://github.com/jsantoso2/madison-tracking-app/blob/main/images/AppMain.png height="375">

### Tools/Framework Used:
- Non-GCP Services  
  - Terraform: To create and standardize GCP resources (ex: VPC Netowork, Subnets, storage bucket, VM, etc.)
  - Kafka: Message broker to handle stream data
  - Flink: To process messages and perform transformation in streaming mode
  - Helm: Pre-built charts for deployment of Flink/Grafana/Prometheus on Kubernetes
  - Prometheus: To monitor Flink Job (if running on GKE)
  - Grafana: Display Prometheus Metrics on Flink Job (if running on GKE)
  - Websockets: To connect Kafka to React (built in Python)
  - React: To display Front End Application
- GCP Services
  - VPC Network: Private Network setup in GCP
  - Cloud NAT: Allows Private IP VMs to connect to outside internet
  - Pub/Sub: Recieve trigger from Cloud Scheduler to run Cloud Function
  - Cloud Function: Receive Trigger from Pub/Sub and writes each event to Kafka Topic
  - Cloud Scheduler: Triggers Pub/Sub and later cloud function to get data from Transit Website
  - Compute Engine: Host Bitnami Image for Kafka + Host Flink Cluster (if running on VM)
  - Cloud Storage: Storage location for static lookup files
  - GKE: Host Flink Cluster (if running on Kubernetes)
  - Cloud Run: To host deployment of WebSocket application
  - App Engine: To host deployment of React App
  - Artifact Registry: Hold pre-built container (for Flink Job + WebSockets)

### Procedure/General Setup
- Pre-Setup
  - Pull all files from Github to Local
  - Go to GCP Account -> Create New Project
  - Enable APIs (Pub/Sub, Compute Engine, Cloud Scheduler, GKE, Cloud Storage, Serverless VPC, Cloud Functions)
  - Generate Service Accounts
    - Terraform Service Account (“terraform-serv-acct”) with “Editor” Permission
    - Flink Service Account (“flink-serv-acct”) with “Editor” + “Storage Admin” Permission
    - Artifact Registry Service Account (“pushtoar-serv-acct”) with “Editor” + “Storage Admin” + “Artifact Registry Admin”
  - In local terminal -> gcloud auth login -> gcloud configure set project PROJECTID
  - VSCode SSH connection to flink-vm (recommended – see notes above)
- Terraform
  - Launch Command Shell -> Open Editor -> Load Service Account Key + Terraform Files + main_requirements.zip (from cloud_function_producer folder) into root folder
  - Launch terraform init -> terraform plan -> terraform apply
  - Launch “Apache Kafka Image by bitnami” from marketplace
    - Machine = n2-standard-2
    - Name = “kafka” || Zone= “us-central1-c”
    - Network = “test-network” || Subnetwork = “us-central1-subnet” || External IP = None 
- Kafka
  - Create 4 Topics (VehiclePositions, TripUpdates, OutputTopicVehicles, OutputTopicStop) in kafka-vm
    - OutputTopic are Logcompacted
    - export KAFKA_OPTS="-Djava.security.auth.login.config=/opt/bitnami/kafka/config/kafka_jaas.conf"
    - /opt/bitnami/kafka/bin/kafka-topics.sh --create --command-config /opt/bitnami/kafka/config/producer.properties --bootstrap-server localhost:9092 --topic TOPICNAME
    - /opt/bitnami/kafka/bin/kafka-topics.sh --create --command-config /opt/bitnami/kafka/config/producer.properties --bootstrap-server localhost:9092 --topic LOG_COMPACTED_TOPICNAME --config cleanup.policy=compact min.cleanable.dirty.ratio=0.01 delete.retention.ms=100 segment.ms=100 retention.ms=1440000
  - Go to /opt/binami/kafka/config/server.properties -> Edit
  - Edit advertised.listeners in kafka config/server.properties to include Internal VM IP
  - Set advertised.listeners=INTERNAL://:9092 -> INTERNAL://GCP VM INTERNAL IP:9092
  - Restart Kafka Service= sudo service bitnami stop -> sudo service bitnami restart
- Cloud Function
  - Edit KAFKA_ADDRESS = GCP VM INTERNAL IP of “kafka-vm”
  - Edit sasl_plain_password = password value in /opt/bitnami/kafka/config/kafka_jaas.conf
- Cloud Storage
  - Upload “datafiles/stops.txt” and “datafiles/trips.txt” to “flink-bucket” in GCS
- Flink
  - In local terminal -> gcloud auth login -> gcloud config set project
  - OpenSSH in VSCode and SSH into “flinkvm”
  - Install Maven = sudo apt-get install maven
  - Install Flink locally
    - curl -o flink-1.17.1-bin-scala_2.12.tgz https://dlcdn.apache.org/flink/flink-1.17.1/flink-1.17.1-bin-scala_2.12.tgz
    - tar -xzf flink-*.tgz
  - Upload “testpipeline” to root folder
  - Update
    - KAFKA_ADDRESS = GCP Internal VM
    - KAFKA_PASSWORD = password value in /opt/bitnami/kafka/config/kafka_jaas.conf
    - STATIC_STOPS_FILEPATH + STATIC_TRIPS_FILEPATH = “gs://PROJECTID-flink-bucket/filename.txt” 
  - Add below section to flink-1.17.1/conf/flink-conf.yaml
    - gs.project.id:  learning-gcp-383300
    - gs.auth.type: SERVICE_ACCOUNT_JSON_KEYFILE
    - gs.auth.service.account.json.keyfile: /home/jonat/flink-serv-acct-keys.json
  - Install “flink-gs-fs-hadoop” dependency as plugins (Inside of the flink-1.17.1 folder)
    - mkdir flink-1.17.1/plugins/gs-fs-hadoop
    - cp flink-1.17.1/opt/flink-gs-fs-hadoop-1.17.1.jar flink-1.17.1/plugins/gs-fs-hadoop/
  - Run Job in local cluster
    - Package Java files = cd to “testpipeline” -> mvn package
    - Start Local Flink Cluster = flink-1.17.1/bin/start-cluster.sh
    - Submit JAR Files = flink-1.17.1/bin/flink run testpipeline/target/testpipeline-0.1.jar
    - Stop Local Flink Cluster = flink-1.17.1/bin/stop-cluster.sh
  - Port Forward 8081 in VSCode -> Go to Localhost:8081 in web browser
- Docker Flink (Optional)
  - Go to the “testpipeline-docker” folder -> Place both “flink-serv-acct-keys.json” and “pushtoar-serv-acct-keys.json” into that folder
  - Build docker image locally = docker build -t testpipeline-docker:v0.1 .
  - Configure GCP settings on local and upload image
    - gcloud auth login
    - gcloud auth activate-service-account pushtoar-serv-acct@learning-gcp-392602.iam.gserviceaccount.com --key-file=pushtoar-serv-acct-keys.json
    - gcloud auth configure-docker us-central1-docker.pkg.dev
    - docker login -u _json_key_base64 --password-stdin https://us-central-docker.pkg.dev < pushtoar-serv-acct-keys.json
    - docker tag testpipeline-docker:v0.1 us-central1-docker.pkg.dev/learning-gcp-392602/my-repo/testpipeline-docker:v0.1
    - docker push us-central1-docker.pkg.dev/learning-gcp-392602/my-repo/testpipeline-docker:v0.1
- GKE (Optional)
  - Inside of VM from same subnetwork - Connect to GKE cluster command presented in “:” icon
  - Copy kubernetes folder is inside of VM
  - Update kubernetes/basic-job.yaml “image” attribute to Artifact Registry path
  - Install kubectl dependencies on VM = sudo apt-get install kubectl, sudo apt-get install google-cloud-sdk-gke-gcloud-auth-plugin
  - Install Helm on VM (https://helm.sh/docs/intro/install/)
  - Install helm charts:
    - helm install flink-kubernetes-operator flink-operator-repo/flink-kubernetes-operator -f values.yaml --set webhook.create=false --set metrics.port=9249
    - helm install prometheus prometheus-community/kube-prometheus-stack
  - Deploy Flink Job into GKE cluster
    - Flink Job = kubectl create -f kubernetes/basic-job.yaml
    - Pod Monitor = kubectl create -f kubernetes/pod-monitor.yaml
  - Port Forward for UI
    - Flink UI = kubectl port-forward svc/basic-example-rest 8081:8081
    - Prometheus = kubectl port-forward svc/prometheus-kube-prometheus-prometheus 9090:9090
    - Grafana = kubectl port-forward svc/prometheus-grafana 8000:80
    - Login (username = admin, password = prom-operator)
  - Teardown instructions
    - helm uninstall prometheus
    - helm uninstall flink-kubernetes-operator
    - kubectl delete -f kubernetes/basic-job.yaml
    - kubectl delete -f kubernetes/pod-monitor.yaml
- Python Websockets
  - Change Kafka_Address inside VehiclesWebSocket.py
  - Build Docker Container + Push to Artifact Registry (same instructions as above)
    - docker build -t websocket-docker:v0.1 .
    - docker tag websocket-docker:v0.1 us-central1-docker.pkg.dev/learning-gcp-392602/my-repo/websocket-docker:v0.1
    - docker push us-central1-docker.pkg.dev/learning-gcp-392602/my-repo/websocket-docker:v0.1
  - Deploy docker container on Cloud Run service (websocket = wss://URL_IN_CLOUD_RUN)
- React Website
  - Create React App
  - Inside app directory -> Build App (npm run build)
  - Inside app directory -> Add app.yaml (use node16)
  - Deploy to Google app engine: gcloud app deploy 
- Teardown instructions
  - Inside of cloud shell editor -> terraform destroy 

### References:
- GCP Private IP Infra: https://cloud.google.com/architecture/building-internet-connectivity-for-private-vms
- Cloud Scheduler: https://towardsdatascience.com/how-to-schedule-a-serverless-google-cloud-function-to-run-periodically-249acf3a652e
- Bitnami Kafka: https://docs.bitnami.com/aws/infrastructure/kafka/administration/run-producer-consumer/
- Kafka Streaming: https://medium.com/swlh/data-streaming-with-apache-kafka-e1676dc5e975
- Flink SQL: https://github.com/twalthr/flink-api-examples
- Flink SQL + Kafka: https://www.youtube.com/watch?v=vLLn5PxF2Lw
- Combine Flink, Kafka, React: https://www.youtube.com/watch?v=wirjC_Zp2ZY
- Apache Flink: https://nightlies.apache.org/flink/flink-docs-stable/
- Flink Kubernetes Operator: https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/ 
