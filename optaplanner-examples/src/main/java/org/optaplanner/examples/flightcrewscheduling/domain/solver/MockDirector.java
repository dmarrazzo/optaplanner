package org.optaplanner.examples.flightcrewscheduling.domain.solver;

import java.util.Collection;
import java.util.Map;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.core.impl.domain.variable.descriptor.VariableDescriptor;
import org.optaplanner.core.impl.score.director.ScoreDirector;

@SuppressWarnings("rawtypes")
public class MockDirector<Solution_> implements ScoreDirector<Solution_> {

    @Override
    public Solution_ getWorkingSolution() {

        return null;
    }

    @Override
    public void setWorkingSolution(Solution_ workingSolution) {

    }

    @Override
    public Score calculateScore() {

        return null;
    }

    @Override
    public boolean isConstraintMatchEnabled() {

        return false;
    }

    @Override
    public Collection<ConstraintMatchTotal> getConstraintMatchTotals() {

        return null;
    }

    @Override
    public Map<Object, Indictment> getIndictmentMap() {

        return null;
    }

    @Override
    public String explainScore() {

        return null;
    }

    @Override
    public void beforeEntityAdded(Object entity) {

    }

    @Override
    public void afterEntityAdded(Object entity) {

    }

    @Override
    public void beforeVariableChanged(Object entity, String variableName) {

    }

    @Override
    public void afterVariableChanged(Object entity, String variableName) {

    }

    @Override
    public void beforeVariableChanged(VariableDescriptor variableDescriptor, Object entity) {

    }

    @Override
    public void afterVariableChanged(VariableDescriptor variableDescriptor, Object entity) {

    }

    @Override
    public void changeVariableFacade(VariableDescriptor variableDescriptor, Object entity,
            Object newValue) {

    }

    @Override
    public void triggerVariableListeners() {

    }

    @Override
    public void beforeEntityRemoved(Object entity) {

    }

    @Override
    public void afterEntityRemoved(Object entity) {

    }

    @Override
    public void beforeProblemFactAdded(Object problemFact) {

    }

    @Override
    public void afterProblemFactAdded(Object problemFact) {

    }

    @Override
    public void beforeProblemPropertyChanged(Object problemFactOrEntity) {

    }

    @Override
    public void afterProblemPropertyChanged(Object problemFactOrEntity) {

    }

    @Override
    public void beforeProblemFactRemoved(Object problemFact) {

    }

    @Override
    public void afterProblemFactRemoved(Object problemFact) {

    }

    @Override
    public <E> E lookUpWorkingObject(E externalObject) {
        return null;
    }

    @Override
    public void close() {
    }

}
