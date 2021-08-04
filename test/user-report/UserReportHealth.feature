Feature: Health user report and gate-keeper component test

  Background:
    Given URL: http://user-report-system.${YAKS_NAMESPACE}.svc.cluster.local
    Given HTTP request timeout is 60000 ms
    Given Kafka connection
      | url           | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
      | topic         | health-data |
      | consumerGroup | health      |

  Scenario: Run UserReportSystem Camel-K integration
    Given Camel-K integration property file application-test.properties
    When load Camel-K integration UserReportSystem.java with configuration
      | traits | knative-service.min-scale=1 |
    Then Camel-K integration user-report-system should be running

  Scenario: Run GateKeeper Camel-K integration
    Given Camel-K integration property file application-test.properties
    When load Camel-K integration GateKeeper.java with configuration
      | traits | knative-service.min-scale=1 |
    Given Camel-K integration gate-keeper should be running

  Scenario: Health report is send to health-data topic
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
          "type": "health",
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
          "type": "health",
          "alert": "@ignore@",
          "measurement": "@ignore@",
          "location": "${location}"
        }
      }
    """
    And Camel-K integration gate-keeper should print "location": "${location}"
    And receive Kafka message on topic health-data

