@require('com.consol.citrus:citrus-validation-hamcrest:@citrus.version@')
@require('org.hamcrest:hamcrest:2.2')
Feature: Earthquake consumer test

  Background:
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
        | topic     | earthquake-data |

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
