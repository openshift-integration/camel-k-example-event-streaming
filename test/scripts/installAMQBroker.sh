#!/bin/bash

function waitFor() {
  for i in {1..20}; do
    sleep 5
    "$@" &> /dev/null && return
  done
}

SOURCE=$( dirname "${BASH_SOURCE[0]}")
INFRA="${SOURCE}"/../../infra

#create OperatorGroup
sed "s/YAKS_NAMESPACE/${YAKS_NAMESPACE}/" "${SOURCE}"/resources/operatorGroup.yaml | oc create -f - 2> /dev/null || echo "OperatorGroup already exists"

#install AMQ broker using OLM
oc create -f "${SOURCE}"/resources/amq-broker-subscription.yaml -n ${YAKS_NAMESPACE}

#ensure operator pod is deployed and Ready
waitFor oc get pod -l name=amq-broker-operator -n ${YAKS_NAMESPACE}
oc wait pod -l name=amq-broker-operator --for condition=Ready -n ${YAKS_NAMESPACE}

#create broker
oc create -f "${INFRA}"/messaging/broker/instances/amq-broker-instance.yaml -n ${YAKS_NAMESPACE}
waitFor oc get pod -l ActiveMQArtemis=broker -n ${YAKS_NAMESPACE}
oc wait pod -l ActiveMQArtemis=broker --for condition=Ready --timeout=600s -n ${YAKS_NAMESPACE}

waitFor [[ `oc get ActiveMQArtemis broker -o=jsonpath='{.status.podStatus.ready}' -n ${YAKS_NAMESPACE}` !=  "" ]]

sleep 30
#install addresses
oc apply -f "${INFRA}"/messaging/broker/instances/addresses -n ${YAKS_NAMESPACE}


