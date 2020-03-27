@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Pollution bridge test

  Background:
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap.event-streaming-kafka-cluster:9092 |
        | topic     | crime-data |
    And JMS connection factory
        | type      | org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory |
        | brokerUrl | tcp://broker-hdls-svc.event-streaming-messaging-broker:61616     |

  Scenario: Alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable location is "citrus:randomString(10)"
    And jms selector: location='${location}'
    And integration crime-bridge is running
    When send message to Kafka with body
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
    Then expect message in JMS broker with body
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
    And integration crime-bridge is running
    When send message to Kafka with body
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
    Then expect message in JMS broker with body
    """
    {
      "text": "There is a crime incident on ${location}",
      "severity": "yellow"
    }
    """