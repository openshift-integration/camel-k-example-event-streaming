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

AMQ_BROKER_VERSION="v7.8.2-opr-1"
TIMEOUT=${TIMEOUT:-30}
SOURCE=$(dirname "$0")
INFRA="${SOURCE}"/../../infra

CSV=$(oc get csv amq-broker-operator.${AMQ_BROKER_VERSION} -n ${YAKS_NAMESPACE} || echo "")
#check for existing amq-broker subscription
if [ -z "$CSV" ]; then
  echo "Create AMQ Broker subscription"

  #create OperatorGroup
  sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

  #install AMQ broker using OLM
  oc create -f "${SOURCE}"/resources/amq-broker-subscription.yaml -n ${YAKS_NAMESPACE}

  #ensure operator pod is deployed and Ready
  waitFor oc wait pod -l name=amq-broker-operator --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}
else
  echo "AMQ Broker subscription already exists"
fi

#create AMQ broker
oc create -f "${INFRA}"/messaging/broker/instances/amq-broker-instance.yaml -n ${YAKS_NAMESPACE}
waitFor oc wait pod -l ActiveMQArtemis=broker --for condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

#install addresses
sleep $TIMEOUT
oc apply -f "${INFRA}"/messaging/broker/instances/addresses -n ${YAKS_NAMESPACE}
