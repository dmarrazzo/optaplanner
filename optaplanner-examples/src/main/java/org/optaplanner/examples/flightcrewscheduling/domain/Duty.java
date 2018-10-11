package org.optaplanner.examples.flightcrewscheduling.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.flightcrewscheduling.domain.solver.FlightAssignmentListener;

@PlanningEntity
public class Duty extends AbstractPersistable {

    private static final long serialVersionUID = 71L;

    private static final ZoneId GMT = ZoneId.of("GMT");
    private static final ZoneId GMTp2 = ZoneId.of("GMT+2");
    
    public static MaxFDP[] maxFDPList;

    //Employees, date, code cannot be changed
    private LocalDate date;

    private Employee employee;

    // code is not null for preassigned duties 
    private String code;

    private LocalDateTime preAssignedDutyStart;
    private LocalDateTime preAssignedDutyEnd;

    @CustomShadowVariable(variableListenerClass=FlightAssignmentListener.class, sources = @PlanningVariableReference(entityClass=FlightAssignment.class, variableName="employee") )
    private NavigableSet<FlightAssignment> flightAssignments;

    public Duty() {
        flightAssignments = new TreeSet<FlightAssignment>(FlightAssignment.DATE_TIME_COMPARATOR);
    }

    public boolean notEmpty() {
        return code != null && flightAssignments.size()>0;
    }

    public boolean isFlightDuty() {
        return flightAssignments.size()>0;
    }

    public void addFlightAssignment(FlightAssignment flightAssignment) {
        flightAssignments.add(flightAssignment);
    }

    public void removeFlightAssignment(FlightAssignment flightAssignment) {
        flightAssignments.remove(flightAssignment);
    }

    public LocalDateTime getStart() {
        // TODO: taxi time ??
        if(isFlightDuty()) {
            LocalDateTime departureUTCDateTime = flightAssignments.first().getFlight().getDepartureUTCDateTime();
            Duration signInDuration = flightAssignments.first().getFlight().getSignInDuration();

            if (preAssignedDutyStart == null || departureUTCDateTime.isBefore(preAssignedDutyStart))
                return departureUTCDateTime.minus(signInDuration);
        }
        return preAssignedDutyStart;
    }

    public LocalDateTime getEnd() {
        // TODO: taxi time ??
        if(isFlightDuty()) {
            LocalDateTime arrivalUTCDateTime = flightAssignments.last().getFlight().getArrivalUTCDateTime();
            Duration signOffDuration = flightAssignments.last().getFlight().getSignOffDuration();

            if (preAssignedDutyStart == null || arrivalUTCDateTime.isAfter(preAssignedDutyStart))
                return arrivalUTCDateTime.plus(signOffDuration);
        }
        return preAssignedDutyEnd;
    }
    
    public LocalDateTime getFlightStart() {
        // TODO: taxi time ??
        return flightAssignments.first().getFlight().getDepartureUTCDateTime();
    }

    public LocalDateTime getFlightEnd() {
        // TODO: taxi time ??
        return flightAssignments.last().getFlight().getArrivalUTCDateTime();
    }
    
    public Optional<Duration> getFlightDutyPeriod() {
        try {
            //Flight duty period has to consider the signin but not the signoff (now we are considering even preassigned duty in getStart)
            return Optional.of(Duration.between(getStart(), getFlightEnd()));
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    public long getFlightDutyPeriodMin() {
        try {
            return Duration.between(getFlightStart(), getFlightEnd()).toMinutes();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    /**
     * 
     * @return the number of minutes exceeding the Maximum Daily FDP
     */
    public int getOverMaxFDP() {
        try {
            int segments = getFlightAssignments().size();
            
            if (segments == 0) 
                return 0;
            MaxFDP maxFDPValid = maxFDPList[0];
            
            for (MaxFDP maxFDP : maxFDPList) {
                // TODO: use employee acclimatization Time Zone
                ZonedDateTime startZ = ZonedDateTime.of(getFlightStart(), GMT);
                if (maxFDP.match(startZ.withZoneSameInstant(GMTp2).toLocalTime())) {
                    maxFDPValid = maxFDP;
                    break;
                }
            }
            Duration maxFDPDuration = maxFDPValid.getMaxFDPBySegment(segments);
            Duration difference = maxFDPDuration.minus(getFlightDutyPeriod().orElse(Duration.ZERO));
            if (difference.isNegative())
                return (int) difference.abs().toMinutes()/10;
            else
                return 0;            
        } catch (NullPointerException e) {
            return 0;
        }
    }
    
    public boolean isDayAfter(Duty otherDuty) {
        return Period.between(getDate(), otherDuty.getDate()).getDays() == 1;
    }
    
    public int getRestLack(Duty otherDuty) {
        // TODO: manage mixed duty

        // if this is not a Flight Duty or the other duty is empty
        if (!isFlightDuty() || otherDuty == null || !otherDuty.notEmpty())
            return 0;
        else {
            Duration rest = Duration.between(getEnd(), otherDuty.getStart());
            Duration dutyDuration = Duration.between(getStart(), getEnd());
            Duration minimumRest = null;

            //if the duty end in home base
            if (flightAssignments.last().getFlight().getArrivalAirport() == employee.getHomeAirport()) {
                if (dutyDuration.toHours()>12)
                    minimumRest = dutyDuration;
                else 
                    minimumRest = Duration.ofHours(12);
            } else {
                if (dutyDuration.toHours()>10)
                    minimumRest = dutyDuration;
                else 
                    minimumRest = Duration.ofHours(10);                
            }
        
            if (rest.compareTo(minimumRest)>0)
                return 0;
            else
                return (int) minimumRest.minus(rest).toMinutes();
        }
    }
    
    @Override
    public String toString() {
        return String.format("Duty [code=%s, date=%s, emp=%s, flightAssignments=%s, FDP=%d]", code, date, employee.getName(), flightAssignments, getFlightDutyPeriod().orElse(Duration.ZERO)
                                                                                                                                                                      .toMinutes());
    }
    
    // ************************************************************************
    // Inner class
    // ************************************************************************

    public class MaxFDP {
        private LocalTime start;
        private LocalTime end;
        private Duration[] maxFDPList = new Duration[10];

        boolean match(LocalTime time) {
            if (start == null || end == null)
                return false;
            return start.compareTo(time) <= 0 && end.compareTo(time) >= 0;
        }
        
        public LocalTime getStart() {
            return start;
        }
        public void setStart(LocalTime start) {
            this.start = start;
        }
        public LocalTime getEnd() {
            return end;
        }
        public void setEnd(LocalTime end) {
            this.end = end;
        }
        public Duration[] getMaxFDPList() {
            return maxFDPList;
        }

        public void setMaxFDPBySegment(int segment, Duration maxFDP) {
            int index = 0;
            if (segment <= 2)
                index = 0;
            else
                index = segment - 2;
            this.maxFDPList[index]=maxFDP;
        }
        
        public Duration getMaxFDPBySegment(int segment) {
            try {
                int index = 0;
                if (segment <= 2)
                    index = 0;
                else
                    index = segment - 2;
                return this.maxFDPList[index];                
            } catch (ArrayIndexOutOfBoundsException e) {
                return Duration.ofHours(9);
            }
        }        
    }

    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getPreAssignedDutyStart() {
        return preAssignedDutyStart;
    }

    public void setPreAssignedDutyStart(LocalDateTime start) {
        this.preAssignedDutyStart = start;
    }

    public LocalDateTime getPreAssignedDutyEnd() {
        return preAssignedDutyEnd;
    }

    public void setPreAssignedDutyEnd(LocalDateTime end) {
        this.preAssignedDutyEnd = end;
    }

    public NavigableSet<FlightAssignment> getFlightAssignments() {
        return flightAssignments;
    }
}
