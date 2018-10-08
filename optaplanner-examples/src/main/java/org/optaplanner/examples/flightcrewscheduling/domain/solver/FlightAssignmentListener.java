package org.optaplanner.examples.flightcrewscheduling.domain.solver;

import java.time.LocalDate;

import org.optaplanner.core.impl.domain.variable.listener.VariableListener;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.examples.flightcrewscheduling.domain.Duty;
import org.optaplanner.examples.flightcrewscheduling.domain.Employee;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightAssignment;

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
    }

    @Override
    public void afterVariableChanged(ScoreDirector scoreDirector,
            FlightAssignment flightAssignment) {
        insertDuty(scoreDirector, flightAssignment);
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

            updateDuty(scoreDirector, duty, flightAssignment.getEmployee());
        }
    }

    private void retractDuty(ScoreDirector scoreDirector, FlightAssignment flightAssignment) {
        LocalDate date = flightAssignment.getFlight().getDepartureUTCDate();

        if (flightAssignment.getEmployee() != null) {
            Duty duty = flightAssignment.getEmployee().getDutyByDate(date);

            scoreDirector.beforeVariableChanged(duty, "flightAssignments");
            duty.removeFlightAssignment(flightAssignment);
            scoreDirector.afterVariableChanged(duty, "flightAssignments");

            updateDuty(scoreDirector, duty, flightAssignment.getEmployee());
        }
    }

    private void updateDuty(ScoreDirector scoreDirector, Duty duty, Employee employee) {
        if (duty.getFlightAssignments().isEmpty()) {
            scoreDirector.beforeVariableChanged(duty, "employee");
            duty.setEmployee(null);
            scoreDirector.afterVariableChanged(duty, "employee");

            scoreDirector.beforeVariableChanged(duty, "code");
            duty.setCode(null);
            scoreDirector.afterVariableChanged(duty, "code");

            scoreDirector.beforeVariableChanged(duty, "start");
            duty.setStart(null);
            scoreDirector.afterVariableChanged(duty, "start");

            scoreDirector.beforeVariableChanged(duty, "end");
            duty.setEnd(null);
            scoreDirector.afterVariableChanged(duty, "end");

            scoreDirector.beforeVariableChanged(duty, "lastFlightArrival");
            duty.setLastFlightArrival(null);
            scoreDirector.afterVariableChanged(duty, "lastFlightArrival");
        } else {
            scoreDirector.beforeVariableChanged(duty, "employee");
            duty.setEmployee(employee);
            scoreDirector.afterVariableChanged(duty, "employee");

            // Conventionally FD stand for Flight Duty
            scoreDirector.beforeVariableChanged(duty, "code");
            duty.setCode("FD");
            scoreDirector.afterVariableChanged(duty, "code");

            scoreDirector.beforeVariableChanged(duty, "start");
            duty.updateStart();
            scoreDirector.afterVariableChanged(duty, "start");

            scoreDirector.beforeVariableChanged(duty, "end");
            duty.updateEnd();
            scoreDirector.afterVariableChanged(duty, "end");

            scoreDirector.beforeVariableChanged(duty, "lastFlightArrival");
            duty.updateLastFlightArrival();
            scoreDirector.afterVariableChanged(duty, "lastFlightArrival");
        }
    }
}