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
        if (flightAssignment.getEmployee() != null) {
            retractDuty(scoreDirector, flightAssignment);

            retractIataFlight(scoreDirector, flightAssignment);
        }
    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector,
            FlightAssignment flightAssignment) {
        if (flightAssignment.getEmployee() != null) {
            insertDuty(scoreDirector, flightAssignment);

            insertIataFlight(scoreDirector, flightAssignment);
        }
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

        Duty duty = flightAssignment.getEmployee().getDutyByDate(date);

        scoreDirector.beforeVariableChanged(duty, "flightAssignments");
        duty.addFlightAssignment(flightAssignment);
        scoreDirector.afterVariableChanged(duty, "flightAssignments");
    }

    private void retractDuty(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        LocalDate date = flightAssignment.getFlight().getDepartureUTCDate();

        Duty duty = flightAssignment.getEmployee().getDutyByDate(date);

        scoreDirector.beforeVariableChanged(duty, "flightAssignments");
        duty.removeFlightAssignment(flightAssignment);
        scoreDirector.afterVariableChanged(duty, "flightAssignments");
    }

    private void insertIataFlight(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        // check the previous flight assignment and the following one,
        // if the connection is not feasible by taxi try to fill the gap woth a iata
        // flight
        // remove previous flight iata if there is one

        // simplification the iata flight is assigned always to the next duty (a rule
        // has to enforce no flight assignment when iata flight

        // TODO A more sophisticated version should find the best option and if it can
        // fit in the current duty
        // TODO: manage iata + taxi

        NavigableSet<FlightAssignment> flightAssignmentSet = flightAssignment.getEmployee()
                                                                             .getFlightAssignmentSet();

        FlightAssignment prevFlightAssignment = flightAssignmentSet.lower(flightAssignment);
        FlightAssignment nextFlightAssignment = flightAssignmentSet.higher(flightAssignment);

        cleanIataFlight(scoreDirector, prevFlightAssignment);
        fitIataFlight(scoreDirector, flightAssignment, nextFlightAssignment);
        fitIataFlight(scoreDirector, prevFlightAssignment, flightAssignment);
    }

    private void retractIataFlight(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        // if this flight assignment has a iata flight remove it
        // check the previous flight assignment and the following one,
        // if the connection among the two is not feasible by taxi try to fill the gap
        // woth a iata flight
        cleanIataFlight(scoreDirector, flightAssignment);

        NavigableSet<FlightAssignment> flightAssignmentSet = flightAssignment.getEmployee()
                                                                             .getFlightAssignmentSet();

        FlightAssignment prevFlightAssignment = flightAssignmentSet.lower(flightAssignment);
        FlightAssignment nextFlightAssignment = flightAssignmentSet.higher(flightAssignment);

        fitIataFlight(scoreDirector, prevFlightAssignment, nextFlightAssignment);
    }

    private void cleanIataFlight(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        if (flightAssignment != null && flightAssignment.getIataFlightHoder() != null) {
            Duty duty = flightAssignment.getIataFlightHoder();
            scoreDirector.beforeVariableChanged(duty, "iataFlight");
            duty.setIataFlight(null);
            scoreDirector.afterVariableChanged(duty, "iataFlight");
            scoreDirector.beforeVariableChanged(flightAssignment, "iataFlightHoder");
            flightAssignment.setIataFlightHoder(null);
            scoreDirector.afterVariableChanged(flightAssignment, "iataFlightHoder");
        }
    }

    private void fitIataFlight(ScoreDirector scoreDirector, FlightAssignment flightAssignment,
            FlightAssignment nextFlightAssignment) {
        if (flightAssignment == null || nextFlightAssignment == null)
            return;

        Airport arrivalAirport = flightAssignment.getFlight().getArrivalAirport();
        LocalDate departureUTCDate = flightAssignment.getFlight().getDepartureUTCDate();

        LocalDate nextDepartureUTCDate = nextFlightAssignment.getFlight().getDepartureUTCDate();
        Airport nextDepartureAirport = nextFlightAssignment.getFlight().getDepartureAirport();

        // if next flight departure date is after the current flight and airport is
        // different and not reachable by taxi
        if (nextDepartureUTCDate.isAfter(departureUTCDate) && arrivalAirport != nextDepartureAirport
                && arrivalAirport.getTaxiTimeInMinutesTo(nextDepartureAirport) == null) {
            ArrayList<IataFlight> iataFlightsList = arrivalAirport.getIataFlightsFor(nextDepartureAirport);

            // assign the first available flight to the duty on day after
            if (iataFlightsList != null && iataFlightsList.isEmpty() == false) {
                Duty dayAfterDuty = flightAssignment.getEmployee()
                                                    .getDutyByDate(departureUTCDate.plusDays(1));
                scoreDirector.beforeVariableChanged(dayAfterDuty, "iataFlight");
                dayAfterDuty.setIataFlight(iataFlightsList.get(0));
                scoreDirector.afterVariableChanged(dayAfterDuty, "iataFlight");
                scoreDirector.beforeVariableChanged(flightAssignment, "iataFlightHoder");
                flightAssignment.setIataFlightHoder(dayAfterDuty);
                scoreDirector.afterVariableChanged(flightAssignment, "iataFlightHoder");
            }
        }
    }
}
