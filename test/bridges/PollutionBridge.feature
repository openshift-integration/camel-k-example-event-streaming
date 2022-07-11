@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Pollution bridge test

  Background:
    Given load variables application-test.properties
    Given variables
      | kafka.topic      | pm-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | pm-bridge |
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

  Scenario: Run PollutionBridge Camel K integration
    Given Camel K integration property file application-test.properties
    Then load Camel K integration PollutionBridge.java

  Scenario: Short term alerts ends in JMS queue:alarms
    Given jms destination: alarms
    And variable uniqueCity is "citrus:randomString(10)"
    And jms selector: city='${uniqueCity}'
    And Camel K integration pollution-bridge is running
    When send Kafka message with body
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
    Then expect JMS message with body
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
    And Camel K integration pollution-bridge is running
    When send Kafka message with body
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
    Then expect JMS message with body
    """
    {
      "text": "City ${uniqueCity} exceeds the maximum safe levels for PM 10 exposure: 10.000000.",
      "severity": "yellow"
    }
    """

  Scenario: Remove Camel K integrations
    Given delete Camel K integration pollution-bridge
