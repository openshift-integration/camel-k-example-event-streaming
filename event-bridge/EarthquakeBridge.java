// camel-k: language=java

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EarthquakeBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeBridge.class);

    @PropertyInject("messaging.broker.url")
    String messagingBrokerUrl;

    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";

        Sjms2Component sjms2Component = new Sjms2Component();
        sjms2Component.setConnectionFactory(new ActiveMQConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);

        JacksonDataFormat dataFormat  = new JacksonDataFormat();
        dataFormat.setUnmarshalType(Feature.class);


        from("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}")
                .unmarshal(dataFormat)
                .process(exchange -> {
                    Feature feature = exchange.getMessage().getBody(Feature.class);

                    String alert = feature.getProperties().getAlert();
                    double magnitude = feature.getProperties().getMag();
                    int tsunami = feature.getProperties().getTsunami();



                    if (alert != null || magnitude > 4.0 || tsunami != 0) {
                        Alert alertMessage = new Alert();
                        alertMessage.setText("Critical geological event: " + feature.getProperties().getTitle());

                        exchange.getMessage().setHeader(unsafeHeader, true);

                        String text = feature.getProperties().getTitle();

                        if (tsunami != 0) {
                            text = text + " with possibility of tsunami";
                        }

                        alertMessage.setSeverity("red");
                        alertMessage.setText(text);

                        ObjectMapper mapper = new ObjectMapper();
                        String body = mapper.writeValueAsString(alertMessage);
                        exchange.getMessage().setBody(body);
                    }
                    else {
                        LOG.debug("Non-critical geological event: {}", feature.getProperties().getTitle());
                    }
                })
                .streamCaching()
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .wireTap("direct:timeline")
                        .to("sjms2://queue:alarms?ttl={{messaging.ttl.alarms}}", "sjms2://queue:notifications?ttl={{messaging.ttl.notifications}}");

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");
    }
}
