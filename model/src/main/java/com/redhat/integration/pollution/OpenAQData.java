package com.redhat.integration.pollution;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OpenAQData {
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