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

CLUSTER=$(kubectl get kafka/my-cluster -n ${YAKS_NAMESPACE} || echo "ERROR: failed to find AMQ Streams Kafka instance")

#check for existing amq-streams kafka instance
if [ "${CLUSTER//ERROR/}" != "${CLUSTER}" ]; then

  # Install Kafka
  kubectl create -f https://strimzi.io/install/latest?namespace=${YAKS_NAMESPACE}

  # Apply the `Kafka` Cluster CR file
  kubectl apply -f https://strimzi.io/examples/latest/kafka/kafka-ephemeral-single.yaml

  # wait for everything to start
  kubectl wait kafka/my-cluster --for=condition=Ready --timeout=300s -n ${YAKS_NAMESPACE}

else
  echo "AMQ Streams Kafka instance already exists"
fi
