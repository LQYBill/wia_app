package org.jeecg.modules.business.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class Period {
    @JsonProperty
    private final Instant start;
    @JsonProperty
    private final Instant end;

    public Period(Instant start, Instant end) {
        this.start = start;
        this.end = end;
    }

    public Instant start() {
        return start;
    }

    public Instant end() {
        return end;
    }
}
