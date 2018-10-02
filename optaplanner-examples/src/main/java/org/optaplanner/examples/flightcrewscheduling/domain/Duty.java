package org.optaplanner.examples.flightcrewscheduling.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.examples.common.domain.AbstractPersistable;

@PlanningEntity
public class Duty extends AbstractPersistable {

    private static final long serialVersionUID = 71L;

    private String code;

    private LocalDate date;

    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(entityClass = Employee.class, variableName = "duties"))
    private NavigableSet<FlightAssignment> flightAssignments;

    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(entityClass = Employee.class, variableName = "duties"))
    private LocalDateTime start;

    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(entityClass = Employee.class, variableName = "duties"))
    private LocalDateTime end;

    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(entityClass = Employee.class, variableName = "duties"))
    private LocalDateTime lastFlightArrival;

    public Duty() {
        flightAssignments = new TreeSet<FlightAssignment>(FlightAssignment.DATE_TIME_COMPARATOR);
    }

    public void addFlightAssignment(FlightAssignment flightAssignment) {
        flightAssignments.add(flightAssignment);
    }

    public void removeFlightAssignment(FlightAssignment flightAssignment) {
        flightAssignments.remove(flightAssignment);
    }

    public void updateStart() {
        // TODO: taxi time
        Flight firstFlight = flightAssignments.first().getFlight();
        start = firstFlight.getDepartureUTCDateTime().minus(firstFlight.getSignInDuration());
    }

    public void updateEnd() {
        // TODO: taxi time
        Flight lastFlight = flightAssignments.last().getFlight();
        end = lastFlight.getArrivalUTCDateTime().plus(lastFlight.getSignOffDuration());
    }

    public void updateLastFlightArrival() {
        lastFlightArrival = flightAssignments.last().getFlight().getArrivalUTCDateTime();
    }

    public Optional<Duration> getFlightDutyPeriod() {
        try {
            return Optional.of(Duration.between(start, lastFlightArrival));
        } catch (NullPointerException e) {
            return Optional.empty();
        }
    }

    public long getFlightDutyPeriodMin() {
        try {
            return Duration.between(start, lastFlightArrival).toMinutes();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("Duty [code=%s, date=%s, flightAssignments=%s, FDP=%d]", code, date, flightAssignments, getFlightDutyPeriod().orElse(Duration.ZERO)
                                                                                                                                          .toMinutes());
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public LocalDateTime getLastFlightArrival() {
        return lastFlightArrival;
    }

    public void setLastFlightArrival(LocalDateTime lastFlightArrival) {
        this.lastFlightArrival = lastFlightArrival;
    }

    public NavigableSet<FlightAssignment> getFlightAssignments() {
        return flightAssignments;
    }

}
