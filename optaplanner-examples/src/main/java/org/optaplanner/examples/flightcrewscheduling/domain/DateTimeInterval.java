package org.optaplanner.examples.flightcrewscheduling.domain;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DateTimeInterval implements Serializable {

    private static final long serialVersionUID = 7L;
    
    private LocalDateTime start;
    private LocalDateTime end;

    public DateTimeInterval(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

}
