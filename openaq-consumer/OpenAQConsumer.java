// camel-k: language=java

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAQConsumer extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAQConsumer.class);

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

    public static class OpenAQData {
        @JsonIgnore
        private Object meta;

        private List<PollutionData> results = new LinkedList<>();

        public Object getMeta() {
            return meta;
        }

        public void setMeta(Object meta) {
            this.meta = meta;
        }

        public List<PollutionData> getResults() {
            return results;
        }

        public void setResults(List<PollutionData> results) {
            this.results = results;
        }
    }

    public static class MySplitter {
        public List<String> splitBody(OpenAQData data) {
            List<String> ret = new ArrayList<>(data.results.size());

            ObjectMapper mapper = new ObjectMapper();
            for (PollutionData pollutionData : data.results) {
                if (pollutionData.getParameter().equals("pm25") || pollutionData.getParameter().equals("pm10")) {
                    try {
                        ret.add(mapper.writeValueAsString(pollutionData));
                    } catch (JsonProcessingException e) {
                        LOG.error("Unable to serialize record: {}", pollutionData);
                    }
                }

            }

            return ret;
        }
    }

    public void configure() throws Exception {
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();

        jacksonDataFormat.setUnmarshalType(OpenAQData.class);


        /*
         Read the data at a fixed interval of 1 second between each request, logging the execution of the
         route, setting up the HTTP method to GET and hitting the OpenAQ measurement API.
         */
        from("timer:refresh?period=600000&fixedRate=true")
                .log("OpenAQ route running")
                .setHeader(Exchange.HTTP_METHOD).constant("GET")
                .to("https://api.openaq.org/v1/measurements?limit=10000")
                .unmarshal(jacksonDataFormat)

                /*
                In this example we are only interested on the measurement data ... and we want to sent each
                measurement separately. To do, we use a splitter to split the results array and send to Kafka
                only the results data and nothing more.
                */
                .split().method(MySplitter.class, "splitBody")
                /*
                 Then setup a wireTap route to log the data before sending it to our Kafka instance.
                 */

                .wireTap("direct:tap")
                .to("kafka:pm-data?brokers={{kafka.bootstrap.address}}");

        from("direct:tap")
                .to("log:info");
    }
}
