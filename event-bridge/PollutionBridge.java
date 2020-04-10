// camel-k: language=java

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;

public class PollutionBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PollutionBridge.class);

    @PropertyInject("messaging.broker.url.amqp")
    private String messagingBrokerUrl;

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String unsafeTypeHeader = "unsafe-type";
        final String cityHeader = "city";
        final String SHORT_TERM = "short term";
        final String LONG_TERM = "long term";

        Sjms2Component sjms2Component = new Sjms2Component();
        // Note that this component is using AMQP instead of Core protocol like the others
        sjms2Component.setConnectionFactory(new JmsConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);

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
                                .to("sjms2://queue:alarms?ttl={{messaging.ttl.alarms}}")
                            .when(header(unsafeTypeHeader).isEqualTo(LONG_TERM))
                                .to("sjms2://queue:notifications?ttl={{messaging.ttl.notifications}}")
                            .otherwise()
                                .log("Unexpected data: ${body}")
                            .endChoice()
                        .endChoice();

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");

    }
}
