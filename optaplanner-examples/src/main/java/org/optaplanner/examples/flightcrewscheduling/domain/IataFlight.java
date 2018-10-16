package org.optaplanner.examples.flightcrewscheduling.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.optaplanner.examples.common.domain.AbstractPersistable;

public class IataFlight extends AbstractPersistable {
    private static final long serialVersionUID = 71L;

    private Airport departureAirport;
    private LocalTime departureUTCTime;
    private Airport arrivalAirport;
    private LocalTime arrivalUTCTime;
    private Set<DayOfWeek> daysOfWeek = new HashSet<>();
    
    public void setDaysOfWeek(String stringCellValue) {
        for (char c : stringCellValue.toCharArray()) {
            switch (c) {
            case '1':
                daysOfWeek.add(DayOfWeek.MONDAY);
                break;
            case '2':
                daysOfWeek.add(DayOfWeek.TUESDAY);
                break;
            case '3':
                daysOfWeek.add(DayOfWeek.WEDNESDAY);
                break;
            case '4':
                daysOfWeek.add(DayOfWeek.THURSDAY);
                break;
            case '5':
                daysOfWeek.add(DayOfWeek.FRIDAY);
                break;
            case '6':
                daysOfWeek.add(DayOfWeek.SATURDAY);
                break;
            case '7':
                daysOfWeek.add(DayOfWeek.SUNDAY);
                break;
            }
        }
    }
    
    public boolean isAvailable(LocalDate date) {
        return getDaysOfWeek().contains(date.getDayOfWeek());
    }
    
    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public Airport getDepartureAirport() {
        return departureAirport;
    }
    public void setDepartureAirport(Airport departureAirport) {
        this.departureAirport = departureAirport;
    }
    public LocalTime getDepartureUTCTime() {
        return departureUTCTime;
    }
    public void setDepartureUTCTime(LocalTime departureUTCTime) {
        this.departureUTCTime = departureUTCTime;
    }
    public Airport getArrivalAirport() {
        return arrivalAirport;
    }
    public void setArrivalAirport(Airport arrivalAirport) {
        this.arrivalAirport = arrivalAirport;
    }
    public LocalTime getArrivalUTCTime() {
        return arrivalUTCTime;
    }
    public void setArrivalUTCTime(LocalTime arrivalUTCTime) {
        this.arrivalUTCTime = arrivalUTCTime;
    }
    public Set<DayOfWeek> getDaysOfWeek() {
        return daysOfWeek;
    }
    public void setDaysOfWeek(Set<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }
}
