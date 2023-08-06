## Pull Flink Image from DockerHub
FROM flink:1.17.1-scala_2.12

## Define environmental variable
ENV FLINK_HOME=/opt/flink
ENV GCP_PROJECT_ID=learning-gcp-383300
ENV FLINK_SERVACCT_KEYS=flink-serv-acct-keys.json

## Copy jar file to docker container
COPY testpipeline-0.1.jar $FLINK_HOME/examples/streaming/testpipeline-0.1.jar
COPY $FLINK_SERVACCT_KEYS ./$FLINK_SERVACCT_KEYS

## Create Directory for plugins and copy jar file
RUN mkdir $FLINK_HOME/plugins/gs-fs-hadoop
RUN cp $FLINK_HOME/opt/flink-gs-fs-hadoop-1.17.1.jar $FLINK_HOME/plugins/gs-fs-hadoop/

## Add configuration to flink-conf.yaml for gs bucket
RUN echo "gs.project.id: $GCP_PROJECT_ID" >> $FLINK_HOME/conf/flink-conf.yaml
RUN echo "gs.auth.type: SERVICE_ACCOUNT_JSON_KEYFILE" >> $FLINK_HOME/conf/flink-conf.yaml
RUN echo "gs.auth.service.account.json.keyfile: ./$FLINK_SERVACCT_KEYS" >> $FLINK_HOME/conf/flink-conf.yaml

USER flink
EXPOSE 8081 6123
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["--help"]

