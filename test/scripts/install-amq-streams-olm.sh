#!/bin/bash

set -e

function waitFor() {
  for i in {1..30}; do
    sleep 5
    ("$@") && return
    echo "$i Waiting for exit code of command \"$@\"."
  done
  exit 1
}

AMQ_STREAMS_VERSION="v1.7.2"
TIMEOUT=${TIMEOUT:-30}
SOURCE=$(dirname "$0")
INFRA="${SOURCE}"/../../infra

CSV=$(oc get csv amqstreams.${AMQ_STREAMS_VERSION} -n ${YAKS_NAMESPACE} || echo "")
#check for existing amq-streams subscription
if [ -z "$CSV" ]; then
  echo "Create AMQ Streams subscription"

  #create OperatorGroup
  sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

  #install AMQ streams using OLM
  oc create -f "${SOURCE}"/resources/amq-streams-subscription.yaml -n ${YAKS_NAMESPACE}

  #ensure operator pod is deployed and Ready
  waitFor oc wait pod -l name=amq-streams-cluster-operator --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}
else
  echo "AMQ Streams subscription already exists"
fi

#create Kafka cluster
oc create -f "${INFRA}"/kafka/clusters/event-streaming-cluster.yaml -n ${YAKS_NAMESPACE}
oc wait kafka/event-streaming-kafka-cluster --for=condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

#install topics
sleep $TIMEOUT
oc apply -f "${INFRA}"/kafka/clusters/topics -n ${YAKS_NAMESPACE}

# wait for topics initialization
sleep $TIMEOUT
