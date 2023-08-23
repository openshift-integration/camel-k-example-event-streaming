#!/bin/bash

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

ACTIVEMQ_VERSION=v1.0.14
URL="https://raw.githubusercontent.com/artemiscloud/activemq-artemis-operator/${ACTIVEMQ_VERSION}"

BROKER=$(kubectl get activemqartemis/artemis-broker -n ${YAKS_NAMESPACE} || echo "")

#check for existing amq-broker instance
if [ -z "$BROKER" ]; then
  echo "No AMQ Broker instance found"
else
  # Uninstall AMQ Artemis
  kubectl delete -f "${URL}"/deploy/crds/broker_activemqartemis_crd.yaml
  kubectl delete -f "${URL}"/deploy/crds/broker_activemqartemisaddress_crd.yaml
  kubectl delete -f "${URL}"/deploy/crds/broker_activemqartemisscaledown_crd.yaml
  kubectl delete -f "${URL}"/deploy/crds/broker_activemqartemissecurity_crd.yaml

  kubectl delete -f "${URL}"/deploy/service_account.yaml
  kubectl delete -f "${URL}"/deploy/role.yaml
  kubectl delete -f "${URL}"/deploy/role_binding.yaml
  kubectl delete -f "${URL}"/deploy/election_role.yaml
  kubectl delete -f "${URL}"/deploy/election_role_binding.yaml

  kubectl delete -f "${URL}"/deploy/operator_config.yaml
  kubectl delete -f "${URL}"/deploy/operator.yaml
fi
