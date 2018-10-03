/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.flightcrewscheduling.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.InverseRelationShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.flightcrewscheduling.domain.solver.FlightAssignmentListener;

@PlanningEntity
public class Employee extends AbstractPersistable {

    private static final long serialVersionUID = 71L;
    private static final int MAX_TAXI_TIME = 240;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId GMTp2 = ZoneId.of("GMT+2");
    
    private String name;
    private Airport homeAirport;

    private Set<Skill> skillSet;
    private Set<String> aircraftTypeQualifications;
    private Set<String> specialQualifications;
    private Set<LocalDate> unavailableDaySet;

    /**
     * Sorted by {@link FlightAssignment#DATE_TIME_COMPARATOR}.
     */
    @InverseRelationShadowVariable(sourceVariableName = "employee")
    private SortedSet<FlightAssignment> flightAssignmentSet;
    
    @CustomShadowVariable(variableListenerClass=FlightAssignmentListener.class, sources = @PlanningVariableReference(entityClass=FlightAssignment.class, variableName="employee") )
    private HashMap<String, Duty> duties = new HashMap<>();

    public Employee() {
    }

    public boolean hasSkill(Skill skill) {
        return skillSet.contains(skill);
    }
    
    public boolean hasAircraftTypeQualification(String qualificationName) {
        return aircraftTypeQualifications.contains(qualificationName);
    }

    public boolean hasSpecialQualification(String qualificationName) {
        return specialQualifications.contains(qualificationName);
    }

    public boolean isAvailable(LocalDate date) {
        return !unavailableDaySet.contains(date);
    }

    public boolean isFirstAssignmentDepartingFromHome() {
        if (flightAssignmentSet.isEmpty()) {
            return true;
        }
        FlightAssignment firstAssignment = flightAssignmentSet.first();
        // TODO allow taking a taxi, but penalize it with a soft score instead
        return firstAssignment.getFlight().getDepartureAirport() == homeAirport;
    }

    public boolean isLastAssignmentArrivingAtHome() {
        if (flightAssignmentSet.isEmpty()) {
            return true;
        }
        FlightAssignment lastAssignment = flightAssignmentSet.last();
        // TODO allow taking a taxi, but penalize it with a soft score instead
        return lastAssignment.getFlight().getArrivalAirport() == homeAirport;
    }
    
    public ConnectionStatus getConnectionStatus() {
        ConnectionStatus healthCheck = new ConnectionStatus();
        FlightAssignment previousAssignment = null;

        for (FlightAssignment assignment : flightAssignmentSet) {
            if (previousAssignment != null) {
                Airport previousAirport = previousAssignment.getFlight().getArrivalAirport();
                Airport airport = assignment.getFlight().getDepartureAirport();
                if (previousAirport != airport) {
                    Long taxiTimeInMinutes = previousAirport.getTaxiTimeInMinutesTo(airport);
                    if (taxiTimeInMinutes == null || taxiTimeInMinutes > MAX_TAXI_TIME)
                        healthCheck.invalidConnection++;
                    else
                        healthCheck.taxiMinutes += taxiTimeInMinutes == null ? 0 : taxiTimeInMinutes;
                }
            }
            previousAssignment = assignment;
        }
        return healthCheck;
    }

    public long getFlightDurationTotalInMinutes() {
        long total = 0L;
        for (FlightAssignment flightAssignment : flightAssignmentSet) {
            total += flightAssignment.getFlightDurationInMinutes();
        }
        return total;
    }

    public Duty getDutyByDate(LocalDate date) {
        String dateStr = DATE_FORMATTER.format(date);
        
        return duties.get(dateStr);
    }

    public Duty setDutyByDate(LocalDate date, Duty duty) {
        String dateStr = DATE_FORMATTER.format(date);
        
        return duties.put(dateStr, duty);
    }
    
    /**
     * 
     * @param flightAssignment
     * @return minutes encroaching the day off respect
     */
    public int dayoffRespect(FlightAssignment flightAssignment) {
        LocalDateTime departureUTCDateTime = flightAssignment.getFlight().getDepartureUTCDateTime();
        ZonedDateTime departureAtAcclimitezedZone = ZonedDateTime.of(departureUTCDateTime, UTC).withZoneSameInstant(getAcclimatizedZoneId());
        
        // previous day is off
        if (!isAvailable(departureAtAcclimitezedZone.minusDays(1).toLocalDate())) {
            LocalTime departureLimit = LocalTime.of(8, 0);
            
            LocalTime departureTime = departureAtAcclimitezedZone.toLocalTime();
            if(departureLimit.isAfter(departureTime))
                return (int) Duration.between(departureTime, departureLimit).abs().toMinutes();
        }
        
        LocalDateTime arrivalUTCDateTime= flightAssignment.getFlight().getArrivalUTCDateTime();
        ZonedDateTime arrivalAtAcclimitezedZone = ZonedDateTime.of(arrivalUTCDateTime, UTC).withZoneSameInstant(getAcclimatizedZoneId());
                
        // next day is off
        if (!isAvailable(arrivalAtAcclimitezedZone.plusDays(1).toLocalDate())) {
            LocalTime arrivalLimit = LocalTime.of(22, 0);
            
            LocalTime arrivalTime = arrivalAtAcclimitezedZone.toLocalTime();
            if(arrivalTime.isAfter(arrivalLimit))
                return (int) Duration.between(arrivalLimit, arrivalTime).toMinutes();
        }
        
        // if the arrival date is in a day off then return 1440 (24h of encroaching)
        // TODO: check if it's correct to consider the UTC time
        if ( !isAvailable ( arrivalUTCDateTime.toLocalDate() ) )
            return 1440;
        
        return 0;
    }
    
    public ZoneId getAcclimatizedZoneId() {
        return GMTp2;
    }
    
    @Override
    public String toString() {
        return name;
    }

    // ************************************************************************
    // Subclasses
    // ************************************************************************

    public class ConnectionStatus {
        public long invalidConnection = 0;
        public long taxiMinutes = 0;
    }
    
    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Airport getHomeAirport() {
        return homeAirport;
    }

    public void setHomeAirport(Airport homeAirport) {
        this.homeAirport = homeAirport;
    }

    public Set<Skill> getSkillSet() {
        return skillSet;
    }

    public void setSkillSet(Set<Skill> skillSet) {
        this.skillSet = skillSet;
    }

    public Set<LocalDate> getUnavailableDaySet() {
        return unavailableDaySet;
    }

    public void setUnavailableDaySet(Set<LocalDate> unavailableDaySet) {
        this.unavailableDaySet = unavailableDaySet;
    }

    public SortedSet<FlightAssignment> getFlightAssignmentSet() {
        return flightAssignmentSet;
    }

    public void setFlightAssignmentSet(SortedSet<FlightAssignment> flightAssignmentSet) {
        this.flightAssignmentSet = flightAssignmentSet;
    }

    public Set<String> getAircraftTypeQualifications() {
        return aircraftTypeQualifications;
    }

    public void setAircraftTypeQualifications(Set<String> aircraftTypeQualifications) {
        this.aircraftTypeQualifications = aircraftTypeQualifications;
    }

    public Set<String> getSpecialQualifications() {
        return specialQualifications;
    }

    public void setSpecialQualifications(Set<String> specialQualifications) {
        this.specialQualifications = specialQualifications;
    }
    
    public HashMap<String,Duty> getDuties() {
        return duties;
    }
}
