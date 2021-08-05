#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

function waitFor() {
  for i in {1..30}; do
    sleep 5
    ("$@") && return
    echo "$i Waiting for exit code of command \"$@\"."
  done
  exit 1
}

SOURCE=$(dirname "${BASH_SOURCE[0]}")
INFRA="${SOURCE}"/../../infra
ACTIVEMQ_VERSION=v0.19.3
URL="https://raw.githubusercontent.com/artemiscloud/activemq-artemis-operator/${ACTIVEMQ_VERSION}"

BROKER=$(kubectl get activemqartemis/broker -n ${YAKS_NAMESPACE} || echo "ERROR: failed to find AMQ Broker instance")

#check for existing amq-broker instance
if [ "${BROKER//ERROR/}" != "${BROKER}" ]; then

  # Install AMQ Artemis
  kubectl create -f "${URL}"/deploy/service_account.yaml
  kubectl create -f "${URL}"/deploy/role.yaml
  kubectl create -f "${URL}"/deploy/role_binding.yaml

  kubectl create -f "${URL}"/deploy/crds/broker_activemqartemis_crd.yaml
  kubectl create -f "${URL}"/deploy/crds/broker_activemqartemisaddress_crd.yaml
  kubectl create -f "${URL}"/deploy/crds/broker_activemqartemisscaledown_crd.yaml

  kubectl create -f "${URL}"/deploy/operator.yaml

  # wait for operator to start
  waitFor kubectl wait pod -l name=activemq-artemis-operator --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}

  # Create AMQ broker
  kubectl create -f "${INFRA}"/messaging/broker/instances/amq-broker-instance.yaml -n ${YAKS_NAMESPACE}

  # wait for broker to start
  waitFor kubectl wait pod -l ActiveMQArtemis=broker --for condition=Ready --timeout=60s -n ${YAKS_NAMESPACE}

else
  echo "AMQ Broker instance already exists"
fi
