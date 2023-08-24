Feature: User report and gate-keeper component test

  Background:
    Given URL: http://user-report-system.${YAKS_NAMESPACE}
    Given HTTP request timeout is 60000 ms
    Given load variables application-test.properties
    Given variables
      | kafka.topic   | crime-data |
    Given Kafka topic: ${kafka.topic}
    Given Kafka connection
      | url           | ${kafka.bootstrap.server.host}.${YAKS_NAMESPACE}:${kafka.bootstrap.server.port} |
      | consumerGroup | crime      |

  Scenario: Create Kafka topic
    Given load Kubernetes custom resource kafka-topic.yaml in kafkatopics.kafka.strimzi.io

  Scenario: Run UserReportSystem Camel K integration
    Given Camel K integration property file application-test.properties
    When load Camel K integration UserReportSystem.java with configuration
      | traits | knative-service.min-scale=1 |
    Then Camel K integration user-report-system should be running
    Then Camel K integration user-report-system should print Listening on

  Scenario: Run GateKeeper Camel K integration
    Given Camel K integration property file application-test.properties
    When load Camel K integration GateKeeper.java with configuration
      | traits | knative-service.min-scale=1 |
    Given Camel K integration gate-keeper should be running
    Then Camel K integration gate-keeper should print Listening on

  Scenario: Crime report is send to crime-data topic
    Given variable user is "user1"
    And variable location is "citrus:randomString(10)"
    And HTTP request header Content-Type is "application/json"
    And HTTP request body
    """
      {
        "user": {
          "name": "${user}"
        },
        "report": {
          "type": "crime",
          "alert": "true",
          "measurement": "g",
          "location": "${location}"
        }
      }
    """
    When send PUT /report/new
    And receive HTTP 200
    And expect HTTP response body: OK
    And verify Kafka message body
    """
      {
        "user": {
          "name": "${user}"
        },
        "report": {
          "type": "crime",
          "alert": "@ignore@",
          "measurement": "@ignore@",
          "location": "${location}"
        }
      }
    """
    Then receive Kafka message on topic crime-data
    And Camel K integration gate-keeper should print "location": "${location}"

  Scenario: Remove Camel K integrations
    Given delete Camel K integration user-report-system
    Given delete Camel K integration gate-keeper
