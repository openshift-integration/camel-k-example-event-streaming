@require('org.apache.activemq:artemis-jms-client:2.11.0')
Feature: Earthquake bridge test

  Background:
    Given Disable auto removal of Camel-K resources
    Given Disable variable support in Camel-K sources
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
        | topic     | earthquake-data |
    And JMS connection factory
        | type      | org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory |
        | brokerUrl | tcp://broker-hdls-svc:61616 |

  Scenario: Run EarthquakeBridge Camel-K integration
    Given Camel-K integration property file application-test.properties
    Then load Camel-K integration EarthquakeBridge.java

  Scenario: Alerts ends in JMS queue:alarms and queue:notifications
    Given variable title is "citrus:randomString(10)"
    And jms selector: title='${title}'
    And Camel-K integration earthquake-bridge is running
    When send Kafka message with body
    """
    {
      "type": "citrus:randomString(10)",
      "properties": {
        "title": "${title}",
        "alert": "true",
        "mag": "1.0",
        "tsunami": "0",
        "place": "citrus:randomString(10)",
        "time": "citrus:currentDate(yyyy-MM-dd)",
        "updated": "citrus:currentDate(yyyy-MM-dd)",
        "tz": "citrus:randomNumber(2)",
        "url": "citrus:randomString(10)",
        "detail": "citrus:randomString(10)",
        "felt": "citrus:randomNumber(2)",
        "cdi": "citrus:randomNumber(1)",
        "mmi": "citrus:randomNumber(1)",
        "status": "citrus:randomString(10)",
        "sig": "citrus:randomNumber(2)",
        "net": "citrus:randomString(10)",
        "code": "citrus:randomString(10)",
        "ids": "citrus:randomString(10)",
        "sources": "citrus:randomString(10)",
        "types": "citrus:randomString(10)",
        "nst": "citrus:randomString(10)",
        "rms": "citrus:randomNumber(1)",
        "gap": "citrus:randomString(10)",
        "magType": "citrus:randomString(10)",
        "type": "citrus:randomString(10)"
      },
      "geometry": {
        "type": "citrus:randomString(10)",
        "coordinates": ["citrus:randomNumber(1)"]
      },
      "id": "citrus:randomString(10)"
    }
    """
    And jms destination: alarms
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title}",
      "severity": "red"
    }
    """
    And jms destination: notifications
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title}",
      "severity": "red"
    }
    """

Scenario: Non-alert message with magnitude > 4.0 ends in JMS queue:alarms and queue:notifications
    Given variable title is "citrus:randomString(10)"
    And jms selector: title='${title}'
    And Camel-K integration earthquake-bridge is running
    When send Kafka message with body
    """
    {
      "type": "citrus:randomString(10)",
      "properties": {
        "title": "${title}",
        "alert": "false",
        "mag": "5",
        "tsunami": "0",
        "place": "citrus:randomString(10)",
        "time": "citrus:currentDate(yyyy-MM-dd)",
        "updated": "citrus:currentDate(yyyy-MM-dd)",
        "tz": "citrus:randomNumber(2)",
        "url": "citrus:randomString(10)",
        "detail": "citrus:randomString(10)",
        "felt": "citrus:randomNumber(2)",
        "cdi": "citrus:randomNumber(1)",
        "mmi": "citrus:randomNumber(1)",
        "status": "citrus:randomString(10)",
        "sig": "citrus:randomNumber(2)",
        "net": "citrus:randomString(10)",
        "code": "citrus:randomString(10)",
        "ids": "citrus:randomString(10)",
        "sources": "citrus:randomString(10)",
        "types": "citrus:randomString(10)",
        "nst": "citrus:randomString(10)",
        "rms": "citrus:randomNumber(1)",
        "gap": "citrus:randomString(10)",
        "magType": "citrus:randomString(10)",
        "type": "citrus:randomString(10)"
      },
      "geometry": {
        "type": "citrus:randomString(10)",
        "coordinates": ["citrus:randomNumber(1)"]
      },
      "id": "citrus:randomString(10)"
    }
    """
    And jms destination: alarms
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title}",
      "severity": "red"
    }
    """
    And jms destination: notifications
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title}",
      "severity": "red"
    }
    """

Scenario: Non-alert message with tsunami warning ends in JMS queue:alarms and queue:notifications
    Given variable title is "citrus:randomString(10)"
    And jms selector: title='${title}'
    And Camel-K integration earthquake-bridge is running
    When send Kafka message with body
    """
    {
      "type": "citrus:randomString(10)",
      "properties": {
        "title": "${title}",
        "alert": "false",
        "mag": "1",
        "tsunami": "1",
        "place": "citrus:randomString(10)",
        "time": "citrus:currentDate(yyyy-MM-dd)",
        "updated": "citrus:currentDate(yyyy-MM-dd)",
        "tz": "citrus:randomNumber(2)",
        "url": "citrus:randomString(10)",
        "detail": "citrus:randomString(10)",
        "felt": "citrus:randomNumber(2)",
        "cdi": "citrus:randomNumber(1)",
        "mmi": "citrus:randomNumber(1)",
        "status": "citrus:randomString(10)",
        "sig": "citrus:randomNumber(2)",
        "net": "citrus:randomString(10)",
        "code": "citrus:randomString(10)",
        "ids": "citrus:randomString(10)",
        "sources": "citrus:randomString(10)",
        "types": "citrus:randomString(10)",
        "nst": "citrus:randomString(10)",
        "rms": "citrus:randomNumber(1)",
        "gap": "citrus:randomString(10)",
        "magType": "citrus:randomString(10)",
        "type": "citrus:randomString(10)"
      },
      "geometry": {
        "type": "citrus:randomString(10)",
        "coordinates": ["citrus:randomNumber(1)"]
      },
      "id": "citrus:randomString(10)"
    }
    """
    And jms destination: alarms
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title} with possibility of tsunami",
      "severity": "red"
    }
    """
    And jms destination: notifications
    Then expect JMS message with body
    """
    {
      "text": "Critical geological event: ${title} with possibility of tsunami",
      "severity": "red"
    }
    """

