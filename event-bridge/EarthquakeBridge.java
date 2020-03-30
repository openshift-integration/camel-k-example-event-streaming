// camel-k: language=java

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.model.dataformat.JsonLibrary;


public class EarthquakeBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeBridge.class);

    @PropertyInject("messaging.broker.url")
    String messagingBrokerUrl;

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String titleHeader = "title";

        Sjms2Component sjms2Component = new Sjms2Component();
        sjms2Component.setConnectionFactory(new ActiveMQConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);

        from("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}")
                .unmarshal().json(JsonLibrary.Jackson, Feature.class)
                .process(exchange -> {
                    Feature feature = exchange.getMessage().getBody(Feature.class);

                    String alert = feature.getProperties().getAlert();
                    double magnitude = feature.getProperties().getMag();
                    int tsunami = feature.getProperties().getTsunami();

                    if (alert != null || magnitude > 4.0 || tsunami != 0) {
                        exchange.getMessage().setHeader(unsafeHeader, true);

                        Alert alertMessage = new Alert();
                        String text = "Critical geological event: " + feature.getProperties().getTitle();

                        if (tsunami != 0) {
                            text = text + " with possibility of tsunami";
                        }

                        alertMessage.setSeverity("red");
                        alertMessage.setText(text);

                        exchange.getMessage().setBody(alertMessage);
                    }
                    else {
                        LOG.debug("Non-critical geological event: {}", feature.getProperties().getTitle());
                    }
                    exchange.getMessage().setHeader(titleHeader ,feature.getProperties().getTitle());
                })
                .marshal().json()
                .convertBodyTo(String.class)
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .wireTap("direct:timeline")
                        .to("sjms2://queue:alarms?ttl={{messaging.ttl.alarms}}", "sjms2://queue:notifications?ttl={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");
    }
}
