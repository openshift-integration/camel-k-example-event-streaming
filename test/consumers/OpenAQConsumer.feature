Feature: OpenAQ consumer test

  Background:
    Given Kafka connection
        | url       | event-streaming-kafka-cluster-kafka-brokers:9094 |
        | topic     | pm-data |

  Scenario: OpenAQConsumer pulls from OpenAQ API and pushes events to Kafka
    Given integration open-aq-consumer is running
    Then integration open-aq-consumer should print Received message from OpenAQ
    And expect message in Kafka with body
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