@require('com.consol.citrus:citrus-validation-hamcrest:@citrus.version@')
@require('org.hamcrest:hamcrest:2.2')
Feature: OpenAQ consumer test

  Background:
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
        | topic     | pm-data |

  Scenario: OpenAQConsumer pulls from OpenAQ API and pushes events to Kafka
    Given Camel-K integration open-aq-consumer is running
    Then Camel-K integration open-aq-consumer should print Received message from OpenAQ
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
