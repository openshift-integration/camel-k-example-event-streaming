@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Pollution bridge test

  Background:
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap.event-streaming-kafka-cluster:9092 |
        | topic     | pm-data |
    And JMS connection factory
        | type      | org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory |
        | brokerUrl | tcp://broker-hdls-svc.event-streaming-messaging-broker:61616     |

  Scenario: Short term alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable uniqueCity is "citrus:randomString(10)"
    And jms selector: city='${uniqueCity}'
    And integration pollution-bridge is running
    When send message to Kafka with body
    """
    {
      "location": "",
      "parameter": "pm10",
      "date": {
        "utc": 1,
        "local": 1
      },
      "value": 85,
      "unit": "?g/m?",
      "coordinates": {
        "longitude": 1,
        "latitude": 3
      },
      "country": "CN",
      "city": "${uniqueCity}"
    }
  """
    Then expect message in JMS broker with body
    """
    {
      "text": "City ${uniqueCity} exceeds the maximum safe levels for PM 10 exposure: 85.000000.",
      "severity": "red"
    }
    """

  Scenario: Long term alerts ends in JMS queue:notifications
    Given jms destination: notifications
    And variable uniqueCity is "citrus:randomString(10)"
    And jms selector: city='${uniqueCity}'
    And integration pollution-bridge is running
    When send message to Kafka with body
    """
    {
      "location": "",
      "parameter": "pm25",
      "date": {
        "utc": 1,
        "local": 1
      },
      "value": 10,
      "unit": "?g/m?",
      "coordinates": {
        "longitude": 1,
        "latitude": 3
      },
      "country": "CN",
      "city": "${uniqueCity}"
    }
    """
    Then expect message in JMS broker with body
    """
    {
      "text": "City ${uniqueCity} exceeds the maximum safe levels for PM 10 exposure: 10.000000.",
      "severity": "yellow"
    }
    """