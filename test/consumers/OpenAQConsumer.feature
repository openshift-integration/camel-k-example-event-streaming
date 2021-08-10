@require('com.consol.citrus:citrus-validation-hamcrest:@citrus.version@')
@require('org.hamcrest:hamcrest:2.2')
Feature: OpenAQ consumer test

  Background:
    Given Disable auto removal of Camel-K resources
    Given Disable variable support in Camel-K sources
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
        | topic     | pm-data |

  Scenario: Run OpenAQConsumer Camel-K integration
    Given Camel-K integration property file application-test.properties
    Then load Camel-K integration OpenAQConsumer.java

  Scenario: OpenAQConsumer pulls from OpenAQ API and pushes events to Kafka
    Given Camel-K integration open-aqconsumer is running
    Then Camel-K integration open-aqconsumer should print Received message from OpenAQ
    And expect Kafka message with body
    """
    {
      "location": "@assertThat(notNullValue())@",
      "parameter": "@assertThat(notNullValue())@",
      "date": "@assertThat(notNullValue())@",
      "value": "@assertThat(notNullValue())@",
      "unit": "@assertThat(notNullValue())@",
      "coordinates": "@assertThat(notNullValue())@",
      "country": "@assertThat(notNullValue())@",
      "city": "@assertThat(notNullValue())@"
    }
    """
