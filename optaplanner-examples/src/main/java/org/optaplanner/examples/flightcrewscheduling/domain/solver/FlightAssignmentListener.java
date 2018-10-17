package org.optaplanner.examples.flightcrewscheduling.domain.solver;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

import org.optaplanner.core.impl.domain.variable.listener.VariableListener;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.flightcrewscheduling.domain.Airport;
import org.optaplanner.examples.flightcrewscheduling.domain.Duty;
import org.optaplanner.examples.flightcrewscheduling.domain.Employee;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightAssignment;
import org.optaplanner.examples.flightcrewscheduling.domain.IataFlight;

/**
 * Listen for addition/removal of an <b>employee</b> to a
 * {@link FlightAssignment} This listener updated <b>duties</b> in
 * {@link Employee}
 * 
 * @author donato
 *
 */
@SuppressWarnings("rawtypes")
public class FlightAssignmentListener implements VariableListener<FlightAssignment> {

    @Override
    public void beforeEntityAdded(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        // nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        // nothing
    }

    @Override
    public void beforeVariableChanged(ScoreDirector scoreDirector,
            FlightAssignment flightAssignment) {
        retractDuty(scoreDirector, flightAssignment);

        // if this flight assignment has a iata flight remove it
        // check the previous flight assignment and the following one, 
        // if the connection among the two is not feasible by taxi try to fill the gap woth a iata flight

    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector,
            FlightAssignment flightAssignment) {
        insertDuty(scoreDirector, flightAssignment);
        
        // check the previous flight assignment and the following one, 
        // if the connection is not feasible by taxi try to fill the gap woth a iata flight
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector scoreDirector,
            FlightAssignment flightAssignment) {
        // nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        // nothing
    }

    // ***********************************************
    // Update logic
    // ***********************************************

    private void insertDuty(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        LocalDate date = flightAssignment.getFlight().getDepartureUTCDate();

        if (flightAssignment.getEmployee() != null) {
            Duty duty = flightAssignment.getEmployee().getDutyByDate(date);

            scoreDirector.beforeVariableChanged(duty, "flightAssignments");
            duty.addFlightAssignment(flightAssignment);
            scoreDirector.afterVariableChanged(duty, "flightAssignments");
        }
    }

    private void retractDuty(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        LocalDate date = flightAssignment.getFlight().getDepartureUTCDate();

        if (flightAssignment.getEmployee() != null) {
            Duty duty = flightAssignment.getEmployee().getDutyByDate(date);

            scoreDirector.beforeVariableChanged(duty, "flightAssignments");
            duty.removeFlightAssignment(flightAssignment);
            scoreDirector.afterVariableChanged(duty, "flightAssignments");
        }
    }

    private void insertIataFlight(ScoreDirector scoreDirector, FlightAssignment flightAssignment, Duty duty) {
        // TODO Auto-generated method stub
        LocalDate departureUTCDate = flightAssignment.getFlight().getDepartureUTCDate();
        
        NavigableSet<FlightAssignment> flightAssignmentSet = flightAssignment.getEmployee().getFlightAssignmentSet();
        
        FlightAssignment nextFlightAssignment = flightAssignmentSet.higher(flightAssignment);
        LocalDate nextDepartureUTCDate = nextFlightAssignment.getFlight().getDepartureUTCDate();
        
        Airport arrivalAirport = flightAssignment.getFlight().getArrivalAirport();
        Airport departureAirport = nextFlightAssignment.getFlight().getDepartureAirport();
        
        // if next airport is different and not reachable by taxi
        if (nextDepartureUTCDate.isAfter(departureUTCDate) && arrivalAirport!= departureAirport && arrivalAirport.getTaxiTimeInMinutesTo(departureAirport) == null) {
            ArrayList<IataFlight> iataFlightsList = arrivalAirport.getIataFlightsFor(departureAirport);
            
            boolean iataAssigned = false;
            // try to assign to this duty
            for (IataFlight iataFlight : iataFlightsList) {
                LocalTime arrivalUTCTime = flightAssignment.getFlight().getArrivalUTCTime();
                LocalDate arrivalDate = flightAssignment.getFlight().getArrivalUTCDate();
                // if the iataFlight is after the arrival and it's available
                // (at least one hour after)
                if (iataFlight.getDepartureUTCTime().isAfter(arrivalUTCTime.plusHours(1)) && iataFlight.isAvailable(arrivalDate)) {
                    scoreDirector.beforeVariableChanged(duty, "iataFlightHolder");
                    duty.setIataFlightHolder(duty);
                    scoreDirector.afterVariableChanged(duty, "iataFlightHolder");
                    scoreDirector.beforeVariableChanged(duty, "iataFlight");
                    duty.setIataFlight(iataFlight);
                    scoreDirector.afterVariableChanged(duty, "iataFlight");
                    iataAssigned = true;
                }
            }
            
            // if it's not already assigned try to assign to next duty
            if (!iataAssigned) {
                //flightAssignment.getEmployee().
            }
            
        }
            
        
        //flightAssignmentSet.
        
    }
}
