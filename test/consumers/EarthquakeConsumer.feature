@require('com.consol.citrus:citrus-validation-hamcrest:@citrus.version@')
@require('org.hamcrest:hamcrest:2.2')
Feature: Earthquake consumer test

  Background:
    Given load variables application-test.properties
    Given variables
      | kafka.topic   | earthquake-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | earthquake      |

  Scenario: Create Kafka topic
    Given load Kubernetes custom resource kafka-topic.yaml in kafkatopics.kafka.strimzi.io

  Scenario: Run EarthquakeConsumer Camel-K integration
    Given Camel-K integration property file application-test.properties
    Then load Camel-K integration EarthquakeConsumer.java

  Scenario: EarthquakeConsumer pulls from USGS Earthquake API and pushes events to Kafka
    Given Camel-K integration earthquake-consumer is running
    Then Camel-K integration earthquake-consumer should print Received message from USGS Earthquake Alert System
    And expect Kafka message with body
    """
    {
      "type": "@assertThat(notNullValue())@",
      "properties": {
        "mag": "@assertThat(notNullValue())@",
        "place": "@assertThat(notNullValue())@",
        "time": "@assertThat(notNullValue())@",
        "updated": "@assertThat(notNullValue())@",
        "tz": "@assertThat(notNullValue())@",
        "url": "@assertThat(notNullValue())@",
        "detail": "@assertThat(notNullValue())@",
        "felt": "@assertThat(notNullValue())@",
        "cdi": "@assertThat(notNullValue())@",
        "mmi": "@assertThat(notNullValue())@",
        "alert": "@ignore@",
        "status": "@assertThat(notNullValue())@",
        "tsunami": "@assertThat(notNullValue())@",
        "sig": "@assertThat(notNullValue())@",
        "net": "@assertThat(notNullValue())@",
        "code": "@assertThat(notNullValue())@",
        "ids": "@assertThat(notNullValue())@",
        "sources": "@assertThat(notNullValue())@",
        "types": "@assertThat(notNullValue())@",
        "nst": "@ignore@",
        "rms": "@assertThat(notNullValue())@",
        "gap": "@ignore@",
        "magType": "@assertThat(notNullValue())@",
        "type": "@assertThat(notNullValue())@",
        "title": "@assertThat(notNullValue())@"
      },
      "geometry": {
        "type": "@assertThat(notNullValue())@",
        "coordinates": "@assertThat(notNullValue())@"
      },
      "id": "@assertThat(notNullValue())@"
    }
    """

  Scenario: Remove Camel-K integrations
    Given delete Camel-K integration earthquake-consumer
