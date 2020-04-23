#!/bin/bash

function waitFor() {
  for i in {1..20}; do
    sleep 5
    "$@" &>/dev/null && return
  done
}

SOURCE=$(dirname "${BASH_SOURCE[0]}")
INFRA="${SOURCE}"/../../infra

#create OperatorGroup
sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

#install amq streams using OLM
oc create -f "${SOURCE}"/resources/amq-streams-subscription.yaml -n ${YAKS_NAMESPACE}

#ensure operator pod is deployed and Ready
waitFor oc get pod -l name=amq-streams-cluster-operator -n ${YAKS_NAMESPACE}
oc wait pod -l name=amq-streams-cluster-operator --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}

#create Kafka
oc create -f "${INFRA}"/kafka/clusters/event-streaming-cluster.yaml -n ${YAKS_NAMESPACE}
oc wait kafka/event-streaming-kafka-cluster --for=condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

#install topics
sleep 10
oc apply -f "${INFRA}"/kafka/clusters/topics -n ${YAKS_NAMESPACE}

# wait for topics creation
sleep 30
