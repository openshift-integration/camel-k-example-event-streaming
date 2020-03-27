import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Data {
    
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