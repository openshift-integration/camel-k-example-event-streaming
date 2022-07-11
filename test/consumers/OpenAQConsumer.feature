@require('com.consol.citrus:citrus-validation-hamcrest:@citrus.version@')
@require('org.hamcrest:hamcrest:2.2')
Feature: OpenAQ consumer test

  Background:
    Given load variables application-test.properties
    Given variables
      | kafka.topic   | pm-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | pm      |

  Scenario: Create Kafka topic
    Given load Kubernetes custom resource kafka-topic.yaml in kafkatopics.kafka.strimzi.io

  Scenario: Run OpenAQConsumer Camel K integration
    Given Camel K integration property file application-test.properties
    Then load Camel K integration OpenAQConsumer.java

  Scenario: OpenAQConsumer pulls from OpenAQ API and pushes events to Kafka
    Given Camel K integration open-aqconsumer is running
    Then Camel K integration open-aqconsumer should print Received message from OpenAQ
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

  Scenario: Remove Camel K integrations
    Given delete Camel K integration open-aqconsumer
