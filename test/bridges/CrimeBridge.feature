@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Crime bridge test

  Background:
    Given load variables application-test.properties
    Given variables
      | kafka.topic      | crime-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | crime-bridge |
    And JMS connection factory
      | type      | org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory |
      | brokerUrl | ${messaging.broker.url} |

  Scenario: Create Kafka topic
    Given load Kubernetes custom resource kafka-topic.yaml in kafkatopics.kafka.strimzi.io

  Scenario: Create ActiveMQ address
    Given variable activemq.address is "alarms"
    Given load Kubernetes custom resource activemq-address.yaml in activemqartemisaddresses.broker.amq.io
    Given variable activemq.address is "notifications"
    Given load Kubernetes custom resource activemq-address.yaml in activemqartemisaddresses.broker.amq.io

  Scenario: Run CrimeBridge Camel K integration
    Given Camel K integration property file application-test.properties
    Then load Camel K integration CrimeBridge.java

  Scenario: Alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And Camel K integration crime-bridge is running
    When send Kafka message with body
    """
      {
        "user": {
          "name": "user"
        },
        "report": {
          "type": "crime",
          "alert": "true",
          "measurement": "crime",
          "location": "${location}"
        }
      }
  """
    Then expect JMS message with body
    """
    {
      "text": "There is a crime incident on ${location}",
      "severity": "red"
    }
    """

   Scenario: Non-alert messages ends in JMS queue:notifications
    Given jms destination: notifications
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And Camel K integration crime-bridge is running
    When send Kafka message with body
    """
      {
        "user": {
          "name": "user"
        },
        "report": {
          "type": "crime",
          "alert": "false",
          "measurement": "crime",
          "location": "${location}"
        }
      }
  """
    Then expect JMS message with body
    """
    {
      "text": "There is a crime incident on ${location}",
      "severity": "yellow"
    }
    """

  Scenario: Remove Camel K integrations
    Given delete Camel K integration crime-bridge
