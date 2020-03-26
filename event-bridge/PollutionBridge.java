// camel-k: language=java

import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.camel.PropertyInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PollutionBridge extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PollutionBridge.class);

    @PropertyInject("messaging.broker.url.amqp")
    private String messagingBrokerUrl;

    public static class PollutionData {
        public static class DateInfo {
            private Date utc;
            private Date local;

            public Date getUtc() {
                return utc;
            }

            public void setUtc(Date utc) {
                this.utc = utc;
            }

            public Date getLocal() {
                return local;
            }

            public void setLocal(Date local) {
                this.local = local;
            }
        }

        public static class Coordinates {
            private double longitude;
            private double latitude;

            public double getLongitude() {
                return longitude;
            }

            public void setLongitude(double longitude) {
                this.longitude = longitude;
            }

            public double getLatitude() {
                return latitude;
            }

            public void setLatitude(double latitude) {
                this.latitude = latitude;
            }
        }

        private String location;
        private String parameter;
        private DateInfo date;
        private double value;
        private String unit;
        private Coordinates coordinates;
        private String country;
        private String city;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public DateInfo getDate() {
            return date;
        }

        public void setDate(DateInfo date) {
            this.date = date;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Coordinates getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(Coordinates coordinates) {
            this.coordinates = coordinates;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class Alert {
        private String text;
        private String severity;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }
    }


    public void configure() throws Exception {
        final String unsafeHeader = "unsafe";
        final String unsafeTypeHeader = "unsafe-type";
        final String SHORT_TERM = "short term";
        final String LONG_TERM = "long term";

        Sjms2Component sjms2Component = new Sjms2Component();
        // Note that this component is using AMQP instead of Core protocol like the others
        sjms2Component.setConnectionFactory(new JmsConnectionFactory(messagingBrokerUrl));
        getContext().addComponent("sjms2", sjms2Component);


        JacksonDataFormat dataFormat  = new JacksonDataFormat();
        dataFormat.setUnmarshalType(PollutionData.class);

        from("kafka:pm-data?brokers={{kafka.bootstrap.address}}")
                .streamCaching()
                .unmarshal(dataFormat)
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

                    ObjectMapper mapper = new ObjectMapper();

                    String body = mapper.writeValueAsString(alert);
                    exchange.getMessage().setBody(body);
                })
                .choice()
                    .when(header(unsafeHeader).isEqualTo(true))
                        .wireTap("direct:timeline")
                        .choice()
                            .when(header(unsafeTypeHeader).isEqualTo(SHORT_TERM))
                                .to("sjms2://queue:alarms")
                            .when(header(unsafeTypeHeader).isEqualTo(LONG_TERM))
                                .to("sjms2://queue:notifications")
                            .otherwise()
                                .log("Unexpected data: ${body}")
                            .endChoice()
                        .endChoice();

        from("direct:timeline")
                .log("${body}")
                .to("kafka:timeline-data?brokers={{kafka.bootstrap.address}}");

    }
}
