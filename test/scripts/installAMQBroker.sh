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
SOURCE=$( dirname "${BASH_SOURCE[0]}")
INFRA="${SOURCE}"/../../infra

#create OperatorGroup
sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

#install AMQ broker using OLM
oc create -f "${SOURCE}"/resources/amq-broker-subscription.yaml -n ${YAKS_NAMESPACE}

#ensure operator pod is deployed and Ready
waitFor oc wait pod -l name=amq-broker-operator --for condition=Ready -n ${YAKS_NAMESPACE}

#create broker
oc create -f "${INFRA}"/messaging/broker/instances/amq-broker-instance.yaml -n ${YAKS_NAMESPACE}
waitFor oc wait pod -l ActiveMQArtemis=broker --for condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

sleep $TIMEOUT
#install addresses
oc apply -f "${INFRA}"/messaging/broker/instances/addresses -n ${YAKS_NAMESPACE}
