import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EarthquakeConsumer extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(EarthquakeConsumer.class);

    public static class Data {
        public static class Feature {
            public static class Properties {
                private double mag;
                private String place;
                private Date time;
                private Date updated;
                private int tz;
                private String url;
                private String detail;
                private int felt;
                private double cdi;
                private double mmi;
                private String alert;
                private String status;
                private int tsunami;
                private int sig;
                private String net;
                private String code;
                private String ids;
                private String sources;
                private String types;
                private String nst;
                @JsonIgnore
                private double dmin;
                private double rms;
                private String gap;
                private String magType;
                private String type;
                private String title;

                public double getMag() {
                    return mag;
                }

                public void setMag(double mag) {
                    this.mag = mag;
                }

                public String getPlace() {
                    return place;
                }

                public void setPlace(String place) {
                    this.place = place;
                }

                public Date getTime() {
                    return time;
                }

                public void setTime(Date time) {
                    this.time = time;
                }

                public Date getUpdated() {
                    return updated;
                }

                public void setUpdated(Date updated) {
                    this.updated = updated;
                }

                public int getTz() {
                    return tz;
                }

                public void setTz(int tz) {
                    this.tz = tz;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public String getDetail() {
                    return detail;
                }

                public void setDetail(String detail) {
                    this.detail = detail;
                }

                public int getFelt() {
                    return felt;
                }

                public void setFelt(int felt) {
                    this.felt = felt;
                }

                public double getCdi() {
                    return cdi;
                }

                public void setCdi(double cdi) {
                    this.cdi = cdi;
                }

                public double getMmi() {
                    return mmi;
                }

                public void setMmi(double mmi) {
                    this.mmi = mmi;
                }

                public String getAlert() {
                    return alert;
                }

                public void setAlert(String alert) {
                    this.alert = alert;
                }

                public String getStatus() {
                    return status;
                }

                public void setStatus(String status) {
                    this.status = status;
                }

                public int getTsunami() {
                    return tsunami;
                }

                public void setTsunami(int tsunami) {
                    this.tsunami = tsunami;
                }

                public int getSig() {
                    return sig;
                }

                public void setSig(int sig) {
                    this.sig = sig;
                }

                public String getNet() {
                    return net;
                }

                public void setNet(String net) {
                    this.net = net;
                }

                public String getCode() {
                    return code;
                }

                public void setCode(String code) {
                    this.code = code;
                }

                public String getIds() {
                    return ids;
                }

                public void setIds(String ids) {
                    this.ids = ids;
                }

                public String getSources() {
                    return sources;
                }

                public void setSources(String sources) {
                    this.sources = sources;
                }

                public String getTypes() {
                    return types;
                }

                public void setTypes(String types) {
                    this.types = types;
                }

                public String getNst() {
                    return nst;
                }

                public void setNst(String nst) {
                    this.nst = nst;
                }

                public double getDmin() {
                    return dmin;
                }

                public void setDmin(double dmin) {
                    this.dmin = dmin;
                }

                public double getRms() {
                    return rms;
                }

                public void setRms(double rms) {
                    this.rms = rms;
                }

                public String getGap() {
                    return gap;
                }

                public void setGap(String gap) {
                    this.gap = gap;
                }

                public String getMagType() {
                    return magType;
                }

                public void setMagType(String magType) {
                    this.magType = magType;
                }

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String getTitle() {
                    return title;
                }

                public void setTitle(String title) {
                    this.title = title;
                }
            }

            public static class Geometry {
                private String type;
                private List<Double> coordinates = new ArrayList<Double>();

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public List<Double> getCoordinates() {
                    return coordinates;
                }

                public void setCoordinates(List<Double> coordinates) {
                    this.coordinates = coordinates;
                }
            }

            private String type;
            private Properties properties;
            private Geometry geometry;

            private String id;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public Properties getProperties() {
                return properties;
            }

            public void setProperties(Properties properties) {
                this.properties = properties;
            }

            public Geometry getGeometry() {
                return geometry;
            }

            public void setGeometry(Geometry geometry) {
                this.geometry = geometry;
            }

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }
        }

        private String type;
        @JsonIgnore
        private Object metadata;
        private List<Feature> features = new ArrayList<Feature>();
        @JsonIgnore
        private Object bbox;


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }

        public List<Feature> getFeatures() {
            return features;
        }

        public void setFeatures(List<Feature> features) {
            this.features = features;
        }

        public Object getBbox() {
            return bbox;
        }

        public void setBbox(Object bbox) {
            this.bbox = bbox;
        }
    }

    public static class MySplitter {
        public List<String> splitBody(Data data) {
            List<String> ret = new ArrayList<>(data.features.size());

            ObjectMapper mapper = new ObjectMapper();
            for (Data.Feature feature: data.features) {

                try {
                    ret.add(mapper.writeValueAsString(feature));
                } catch (JsonProcessingException e) {
                    LOG.error("Unable to serialize record: {}", feature);
                }
            }

            return ret;
        }
    }

    public void configure() throws Exception {
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();

        jacksonDataFormat.setUnmarshalType(Data.class);
/*
         Read the data at a fixed interval of 1 second between each request, logging the execution of the
         route, setting up the HTTP method to GET and hitting the OpenAQ measurement API.
         */
        from("timer:refresh?period=60000&fixedRate=true")
                .log("USGS Earthquake route running")
                .setHeader(Exchange.HTTP_METHOD).constant("GET")
                .to("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson")
                .streamCaching()
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
                .to("kafka:earthquake-data?brokers={{kafka.bootstrap.address}}");

        from("direct:tap")
                .to("log:info");
    }
}
