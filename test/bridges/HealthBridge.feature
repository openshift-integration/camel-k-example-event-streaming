@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Health bridge test

  Background:
    Given load variables application-test.properties
    Given variables
      | kafka.topic      | health-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | health-bridge |
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

  Scenario: Run HealthBridge Camel K integration
    Given Camel K integration property file application-test.properties
    Then load Camel K integration HealthBridge.java

  Scenario: Alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And Camel K integration health-bridge is running
    When send Kafka message with body
    """
      {
        "user": {
          "name": "user"
        },
        "report": {
          "type": "health",
          "alert": "true",
          "measurement": "health",
          "location": "${location}"
        }
      }
  """
    Then expect JMS message with body
    """
    {
      "text": "There is a health incident on ${location}",
      "severity": "red"
    }
    """

   Scenario: Non-alert messages ends in JMS queue:notifications
    Given jms destination: notifications
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And Camel K integration health-bridge is running
    When send Kafka message with body
    """
      {
        "user": {
          "name": "user"
        },
        "report": {
          "type": "health",
          "alert": "false",
          "measurement": "health",
          "location": "${location}"
        }
      }
  """
    Then expect JMS message with body
    """
    {
      "text": "There is a health incident on ${location}",
      "severity": "yellow"
    }
    """

  Scenario: Remove Camel K integrations
    Given delete Camel K integration health-bridge
