Feature: User report component test

  Background:
    Given integration user-report-system should be running
    And URL: http://user-report-system.camel-k-event-streaming.svc.cluster.local
    And variable user is "user1"
    And integration user-report-system should print started and consuming from: http://0.0.0.0:8080/report/new

  Scenario: Crime report is send to crime-data topic
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
    Then integration gate-keeper should be running
    And integration gate-keeper should print "type": "crime"