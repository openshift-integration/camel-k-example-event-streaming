#! /bin/bash
oc new-project event-streaming-kafka-cluster

oc apply -f infra/kafka/amq-streams-subscription.yaml --wait

oc create -f infra/kafka/clusters/event-streaming-cluster.yaml
oc wait kafka/event-streaming-kafka-cluster --for=condition=Ready --timeout=600s
oc apply -f infra/kafka/clusters/topics/

#------------
oc new-project event-streaming-messaging-broker

oc apply -f infra/messaging/broker/amq-broker-subscription.yaml


oc create -f infra/messaging/broker/instances/amq-broker-instance.yaml


oc apply -f infra/messaging/broker/instances/addresses

 #------
oc new-project camel-k-event-streaming

oc create secret generic example-event-streaming-user-reporting --from-file config/application.properties
#oc apply -f infra/knative/channels/audit-channel.yaml

# check env before test
#oc wait InMemoryChannel/audit --for=condition=Ready --timeout=600s
#oc wait KafkaTopic --all --for=condition=Ready --timeout=600s -n event-streaming-kafka-cluster
 
 kamel install --wait
 yaks install

 #kamel run openaq-consumer/OpenAQConsumer.java
 #kamel run event-bridge/PollutionBridge.java
 #kamel run event-bridge/CrimeBridge.java
 #kamel run user-report-system/UserReportSystem.java -t knative-service.min-scale=1

 #yaks test test -e YAKS_CAMELK_MAX_ATTEMPTS=300 -e YAKS_CAMELK_DELAY_BETWEEN_ATTEMPTS=8000