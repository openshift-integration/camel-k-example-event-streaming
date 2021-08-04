@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Health bridge test

  Background:
    Given Disable auto removal of Camel-K resources
    Given Disable variable support in Camel-K sources
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
        | topic     | health-data |
    And JMS connection factory
        | type      | org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory |
        | brokerUrl | tcp://broker-hdls-svc:61616 |

  Scenario: Run HealthBridge Camel-K integration
    Given Camel-K integration property file application-test.properties
    Then load Camel-K integration HealthBridge.java

  Scenario: Alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And Camel-K integration health-bridge is running
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
    And Camel-K integration health-bridge is running
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
