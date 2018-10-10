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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;

import org.optaplanner.examples.common.domain.AbstractPersistable;

public class Flight extends AbstractPersistable {

    private String flightNumber;
    private Airport departureAirport;
    private LocalDateTime departureUTCDateTime;
    private Airport arrivalAirport;
    private LocalDateTime arrivalUTCDateTime;
    private String aircraftRegistration;    
    private String aircraftType;

    public Flight() {
    }

    public long getDurationInMinutes() {
        return ChronoUnit.MINUTES.between(departureUTCDateTime, arrivalUTCDateTime);
    }

    public LocalDate getDepartureUTCDate() {
        return departureUTCDateTime.toLocalDate();
    }
    public LocalTime getDepartureUTCTime() {
        return departureUTCDateTime.toLocalTime();
    }

    public LocalDate getArrivalUTCDate() {
        return arrivalUTCDateTime.toLocalDate();
    }
    public LocalTime getArrivalUTCTime() {
        return arrivalUTCDateTime.toLocalTime();
    }

    public long overlaps(Flight other) {
        //disregard sigin - sigoff. Multiple legs does not suffer of such problem 
        LocalDateTime startA = departureUTCDateTime;
        LocalDateTime endA = arrivalUTCDateTime;
        LocalDateTime startB = other.departureUTCDateTime;
        LocalDateTime endB = other.arrivalUTCDateTime;

        Duration between = null;

        // if there is an overlap
        if (startA.isBefore(endB) && startB.isBefore(endA)) {
            if (startA.isBefore(startB))
                between = Duration.between(startB, endA);
            else
                between = Duration.between(startA, endB);
            return between.toMinutes();
        } else
            return 0;
    }

    public Duration getSignInDuration() {
        return Duration.ofMinutes(30);
    }

    public Duration getSignOffDuration() {
        return Duration.ofMinutes(30);
    }

    @Override
    public String toString() {
        return flightNumber + "@" + departureUTCDateTime.toLocalDate();
    }

    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public Airport getDepartureAirport() {
        return departureAirport;
    }

    public void setDepartureAirport(Airport departureAirport) {
        this.departureAirport = departureAirport;
    }

    public LocalDateTime getDepartureUTCDateTime() {
        return departureUTCDateTime;
    }

    public void setDepartureUTCDateTime(LocalDateTime departureUTCDateTime) {
        this.departureUTCDateTime = departureUTCDateTime;
    }

    public Airport getArrivalAirport() {
        return arrivalAirport;
    }

    public void setArrivalAirport(Airport arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }

    public LocalDateTime getArrivalUTCDateTime() {
        return arrivalUTCDateTime;
    }

    public void setArrivalUTCDateTime(LocalDateTime arrivalUTCDateTime) {
        this.arrivalUTCDateTime = arrivalUTCDateTime;
    }

    public String getAircraftRegistration() {
        return aircraftRegistration;
    }

    public void setAircraftRegistration(String aircraftRegistration) {
        this.aircraftRegistration = aircraftRegistration;
    }

    public String getAircraftType() {
        return aircraftType;
    }

    public void setAircraftType(String aircraftType) {
        this.aircraftType = aircraftType;
    }

}
