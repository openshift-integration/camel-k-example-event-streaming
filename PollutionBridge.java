// camel-k: language=java property=file:application.properties
// camel-k: dependency=mvn:org.amqphub.quarkus:quarkus-qpid-jms
// camel-k: dependency=github:openshift-integration:camel-k-example-event-streaming:1.8.x-SNAPSHOT

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

import com.redhat.integration.common.Alert;
import com.redhat.integration.pollution.PollutionData;

public class PollutionBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PollutionBridge.class);

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String unsafeTypeHeader = "unsafe-type";
        final String cityHeader = "city";
        final String SHORT_TERM = "short term";
        final String LONG_TERM = "long term";

        from("kafka:pm-data?brokers={{kafka.bootstrap.address}}&groupId=pmbrige&autoOffsetReset=earliest")
                .unmarshal().json(JsonLibrary.Jackson, PollutionData.class)
                .process(exchange -> {
                    final String TEXT_FORMAT =
                            "City %s exceeds the maximum safe levels for %s exposure: %f.";
                    String text = null;

                    PollutionData pollutionData = exchange.getMessage().getBody(PollutionData.class);
                    LOG.info("Processing pollution data for city {} ", pollutionData.getCity());

                    Alert alert = new Alert();

                    if (pollutionData.getParameter().equals("pm10")) {
                        if (pollutionData.getValue() > 25.0) {
                            LOG.info("City {} exceeds the maximum safe levels for PM 10 exposure",
                                    pollutionData.getCity());
                            exchange.getMessage().setHeader(unsafeHeader, true);

                            if (pollutionData.getValue() > 50.0) {
                                exchange.getMessage().setHeader(unsafeTypeHeader, SHORT_TERM);
                                alert.setSeverity("red");
                            } else {
                                exchange.getMessage().setHeader(unsafeTypeHeader, LONG_TERM);
                                alert.setSeverity("yellow");
                            }
                        }

                        text = String.format(TEXT_FORMAT, pollutionData.getCity(), "PM 10",
                                pollutionData.getValue());

                    }

                    if (pollutionData.getParameter().equals("pm25")) {
                        if (pollutionData.getValue() > 8.0) {
                            LOG.info("City {} exceeds the maximum safe levels for PM 2.5 exposure",
                                    pollutionData.getCity());
                            exchange.getMessage().setHeader(unsafeHeader, true);

                            if (pollutionData.getValue() > 25.0) {
                                exchange.getMessage().setHeader(unsafeTypeHeader, SHORT_TERM);
                                alert.setSeverity("red");
                            } else {
                                exchange.getMessage().setHeader(unsafeTypeHeader, LONG_TERM);
                                alert.setSeverity("yellow");
                            }
                        }

                        text = String.format(TEXT_FORMAT, pollutionData.getCity(), "PM 10",
                                pollutionData.getValue());
                    }

                    alert.setText(text);

                    exchange.getMessage().setBody(alert);
                    exchange.getMessage().setHeader(cityHeader, pollutionData.getCity());
                })
                .marshal().json()
                .convertBodyTo(String.class)
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .wireTap("direct:timeline")
                        .choice()
                            .when(header(unsafeTypeHeader).isEqualTo(SHORT_TERM))
                                .to("jms://queue:alarms?timeToLive={{messaging.ttl.alarms}}")
                            .when(header(unsafeTypeHeader).isEqualTo(LONG_TERM))
                                .to("jms://queue:notifications?timeToLive={{messaging.ttl.notifications}}")
                            .otherwise()
                                .log("Unexpected data: ${body}")
                            .endChoice()
                        .endChoice();

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");

    }
}
