Feature: User report and gate-keeper component test

  Background:
    Given integration user-report-system should be running
    And integration gate-keeper should be running
    Given HTTP request timeout is 60000 ms
    Given URL: http://user-report-system.${YAKS_NAMESPACE}.svc.cluster.local
    Given HTTP request header Content-Type is "application/json"
    Given variable user is "user1"

  Scenario: Crime report is send to crime-data topic
    Given Kafka connection
      | url           | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
      | topic         | crime-data |
      | consumerGroup | crime      |
    And variable location is "citrus:randomString(10)"
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
    And verify message in Kafka with body
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
    Then integration gate-keeper should be running
    And integration gate-keeper should print "location": "${location}"

  Scenario: Health report is send to health-data topic
    Given Kafka connection
      | url           | event-streaming-kafka-cluster-kafka-bootstrap:9092 |
      | topic         | health-data |
      | consumerGroup | health      |
    And variable location is "citrus:randomString(10)"
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
    Then integration user-report-system should be running
    And receive HTTP 200
    And expect HTTP response body: OK
    And verify message in Kafka with body
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
    And integration gate-keeper should print "location": "${location}"