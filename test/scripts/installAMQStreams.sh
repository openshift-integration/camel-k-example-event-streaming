#!/bin/bash

function waitFor() {
  for i in {1..30}; do
    sleep 5
    ("$@") && return
    echo "$i Waiting for exit code of command \"$@\"."
  done
  exit 1
}

TIMEOUT=${TIMEOUT:-30}
SOURCE=$(dirname "${BASH_SOURCE[0]}")
INFRA="${SOURCE}"/../../infra

#create OperatorGroup
sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

#install amq streams using OLM
oc create -f "${SOURCE}"/resources/amq-streams-subscription.yaml -n ${YAKS_NAMESPACE}

#ensure operator pod is deployed and Ready
waitFor oc wait pod -l name=amq-streams-cluster-operator --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}

#create Kafka
oc create -f "${INFRA}"/kafka/clusters/event-streaming-cluster.yaml -n ${YAKS_NAMESPACE}
oc wait kafka/event-streaming-kafka-cluster --for=condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

#install topics
sleep $TIMEOUT
oc apply -f "${INFRA}"/kafka/clusters/topics -n ${YAKS_NAMESPACE}

# wait for topics initialization
sleep $TIMEOUT
