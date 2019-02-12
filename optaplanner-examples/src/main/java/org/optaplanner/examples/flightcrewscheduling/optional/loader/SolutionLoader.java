package org.optaplanner.examples.flightcrewscheduling.optional.loader;

import java.io.File;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.score.director.ScoreDirectorFactory;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewSolution;
import org.optaplanner.examples.flightcrewscheduling.persistence.FlightCrewSchedulingXlsxFileIO;

public class SolutionLoader {
    private static final String refSchedule = "data/flightcrewscheduling/unsolved/4W-flights-poc.xlsx";
    private static final String outSchedule = "data/flightcrewscheduling/solved/4W-flights-poc.xlsx";

    public static void main(String[] args) {
        File refScheduleFile = new File(refSchedule);
        
        FlightCrewSchedulingXlsxFileIO flightCrewSchedulingXlsxFileIO = new FlightCrewSchedulingXlsxFileIO();
        FlightCrewSolution workingSolution = flightCrewSchedulingXlsxFileIO.readSolved(refScheduleFile);

        SolverFactory<FlightCrewSolution> solverFactory = SolverFactory.createFromXmlResource("org/optaplanner/examples/flightcrewscheduling/solver/flightCrewSchedulingSolverConfig.xml");
        
        Solver<FlightCrewSolution> solver = solverFactory.buildSolver();
               
        ScoreDirectorFactory<FlightCrewSolution> directorFactory = solver.getScoreDirectorFactory();
        ScoreDirector<FlightCrewSolution> scoreDirector = directorFactory.buildScoreDirector();
        scoreDirector.setWorkingSolution(workingSolution);
        scoreDirector.calculateScore();
        Score score = scoreDirector.calculateScore();

        System.out.println("calculated score: " + score.toString());

     
        File outputSolutionFile = new File(outSchedule);
        flightCrewSchedulingXlsxFileIO.write(workingSolution, outputSolutionFile );
       
    }

}
