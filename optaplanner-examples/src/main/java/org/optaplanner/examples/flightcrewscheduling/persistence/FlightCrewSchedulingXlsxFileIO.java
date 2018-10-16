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

package org.optaplanner.examples.flightcrewscheduling.persistence;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization.EMPLOYEE_UNAVAILABILITY;
import static org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization.FLIGHT_CONFLICT;
import static org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization.LOAD_BALANCE_FLIGHT_DURATION_TOTAL_PER_EMPLOYEE;
import static org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization.REQUIRED_SKILL;
import static org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization.TRANSFER_BETWEEN_TWO_FLIGHTS;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.examples.common.persistence.AbstractXlsxSolutionFileIO;
import org.optaplanner.examples.flightcrewscheduling.app.FlightCrewSchedulingApp;
import org.optaplanner.examples.flightcrewscheduling.domain.Airport;
import org.optaplanner.examples.flightcrewscheduling.domain.Duty;
import org.optaplanner.examples.flightcrewscheduling.domain.Employee;
import org.optaplanner.examples.flightcrewscheduling.domain.Flight;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightAssignment;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewParametrization;
import org.optaplanner.examples.flightcrewscheduling.domain.FlightCrewSolution;
import org.optaplanner.examples.flightcrewscheduling.domain.IataFlight;
import org.optaplanner.examples.flightcrewscheduling.domain.Skill;
import org.optaplanner.swing.impl.TangoColorFactory;

public class FlightCrewSchedulingXlsxFileIO extends AbstractXlsxSolutionFileIO<FlightCrewSolution> {

    private static final String DAY_OFF_MATCH = "OUV|UW|W|V|C";
    private static final String PRE_ASSIGNED_DUTY_MATCH = "GND|S1E|LSE|ESE";
    private static final String COMMA_SPLIT = ",\\s+|,";

    public static final DateTimeFormatter MILITARY_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm", Locale.ENGLISH);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    @Override
    public FlightCrewSolution read(File inputSolutionFile) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(inputSolutionFile))) {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            return new FlightCrewSchedulingXlsxReader(workbook).read();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "Failed reading inputSolutionFile (" + inputSolutionFile + ").", e);
        }
    }

    public FlightCrewSolution readSolved(File inputSolutionFile) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(inputSolutionFile))) {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            FlightCrewSchedulingXlsxReader xlsxReader = new FlightCrewSchedulingXlsxReader(workbook);
            xlsxReader.setReadSolved(true);
            return xlsxReader.read();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                    "Failed reading inputSolutionFile (" + inputSolutionFile + ").", e);
        }
    }

    private static class FlightCrewSchedulingXlsxReader
            extends AbstractXlsxReader<FlightCrewSolution> {

        private Map<String, Skill> skillMap;
        private Map<String, Employee> nameToEmployeeMap;
        private Map<String, Airport> airportMap;
        private Map<String, FlightAssignment> mapFlightDateSkillToFlightAssignment;
        private Map<String, String> mapEmployeeDateToDutyCode;
        private HashMap<String, String[]> qualificationAircraftTypeMap;
        private boolean readSolved;
        
        public FlightCrewSchedulingXlsxReader(XSSFWorkbook workbook) {
            super(workbook);
        }

        public void setReadSolved(boolean readSolved) {
            this.readSolved = readSolved; 
        }

        @Override
        public FlightCrewSolution read() {
            solution = new FlightCrewSolution();
            readConfiguration();
            readSkillList();
            readAirportList();
            readTaxiTimeMaps();
            readIataFlights();
            readQualifications();
            readMaxFDP();
            readFlightListAndFlightAssignmentList();
            readEmployeeList();
            readPreAssignedDuties();
            return solution;
        }

        private void readPreAssignedDuties() {
            nextSheet("CP duties");
            nextRow(false);
            readHeaderCell("Crewmember");
            
            readPreAssignedDutiesSheet();

            nextSheet("FO duties");
            nextRow(false);
            readHeaderCell("Crewmember");

            readPreAssignedDutiesSheet();
        }

        private void readPreAssignedDutiesSheet() {
            while ( nextRow() ) {
                String employeeName = nextStringCell().getStringCellValue();
                String startDateString = nextStringCell().getStringCellValue();
                String endDateString = nextStringCell().getStringCellValue();
                double flyingHours = nextNumericCell().getNumericCellValue();
                if(flyingHours == 0) {
                    Employee employee = nameToEmployeeMap.get(employeeName);
                    LocalDateTime dutyStart = LocalDateTime.parse(startDateString, DATE_TIME_FORMATTER);
                    LocalDateTime dutyEnd = LocalDateTime.parse(endDateString, DATE_TIME_FORMATTER);
                    LocalDate dutyDate = dutyStart.toLocalDate();
                    Duty duty = employee.getDutyByDate(dutyDate);
                    
                    // TODO: new model to manage multiple activity for a duty
                    String dutyCode = mapEmployeeDateToDutyCode.get(employeeName+"-"+dutyDate);
                    if (duty == null && dutyCode != null) {
                        // Create a new duty. Retrieve duty code from map emp-date/dutyCode (from employee list)
                        duty = new Duty();
                        duty.setCode(dutyCode);
                        duty.setDate(dutyDate);
                        duty.setEmployee(employee);
                        duty.setPreAssignedDutyStart(dutyStart);
                        duty.setPreAssignedDutyEnd(dutyEnd);
                        employee.setDutyByDate(dutyDate, duty);
                    }                    
                }
            }
        }

        private void readQualifications() {
            nextSheet("Qualifications");
            nextRow(false);
            readHeaderCell("Description");
            readHeaderCell("Qualification");
            readHeaderCell("Aircraft Type");

            qualificationAircraftTypeMap = new HashMap<>();
            
            while (nextRow()) {
                //skip description
                nextCell();
                //qualification
                String qualification = nextStringCell().getStringCellValue();
                String aircraftTypeValue = nextStringCell().getStringCellValue();
                String[] aircraftTypeArray = aircraftTypeValue.split(COMMA_SPLIT);
                qualificationAircraftTypeMap.put(qualification, aircraftTypeArray);
            }
        }

        @SuppressWarnings("deprecation")
        private void readMaxFDP() {
            nextSheet("FDP");
            nextRow(false);
            readHeaderCell("Basic Maximum daily FDP");
            nextRow(false);
            // skip second header
            nextRow(false);
            
            readHeaderCell("min");
            readHeaderCell("max");
            
            // initialize the segment from the header
            int segmentColumns = currentRow.getLastCellNum()-2;
            int[] segmentNum = new int[segmentColumns];
            for (int i = 0; i< segmentNum.length; i++) {
                segmentNum[i] = (int) nextNumericCell().getNumericCellValue(); 
            }
            
            int maxFDPListLength = currentSheet.getLastRowNum() - currentRowNumber;
            Duty.maxFDPList = new Duty.MaxFDP[maxFDPListLength];

            // the following instance is temporary
            Duty dutyInstance = new Duty();
            
            // for each maxFDP row
            for (int countMaxFDP = 0; countMaxFDP < maxFDPListLength; countMaxFDP++) {
                nextRow();
                Duty.MaxFDP maxFDPRange = dutyInstance.new MaxFDP();
                Duty.maxFDPList[countMaxFDP] = maxFDPRange;
                Date startTime = nextNumericCell().getDateCellValue();
                Date endTime = nextNumericCell().getDateCellValue();
                maxFDPRange.setStart(LocalDateTime.ofInstant(startTime.toInstant(), ZoneOffset.systemDefault()).toLocalTime());
                maxFDPRange.setEnd(LocalDateTime.ofInstant(endTime.toInstant(), ZoneOffset.systemDefault()).toLocalTime());
                for (int i = 0; i< segmentNum.length; i++) {
                    Date date = nextNumericCell().getDateCellValue();
                    maxFDPRange.setMaxFDPBySegment(segmentNum[i], Duration.ofMinutes(date.getHours()*60+date.getMinutes()));
                }
            }                        
        }

        private void readConfiguration() {
            nextSheet("Configuration");
            nextRow(false);
            readHeaderCell("Schedule start UTC Date");
            solution.setScheduleFirstUTCDate(LocalDate.parse(nextStringCell().getStringCellValue(), DAY_FORMATTER));
            nextRow(false);
            readHeaderCell("Schedule end UTC Date");
            solution.setScheduleLastUTCDate(LocalDate.parse(nextStringCell().getStringCellValue(), DAY_FORMATTER));
            nextRow(false);
            nextRow(false);
            readHeaderCell("Constraint");
            readHeaderCell("Weight");
            readHeaderCell("Description");
            FlightCrewParametrization parametrization = new FlightCrewParametrization();
            parametrization.setId(0L);
            readLongConstraintLine(LOAD_BALANCE_FLIGHT_DURATION_TOTAL_PER_EMPLOYEE, parametrization::setLoadBalanceFlightDurationTotalPerEmployee, "Soft penalty per 0.001 minute difference with the average flight duration total per employee.");
            readIntConstraintLine(REQUIRED_SKILL, null, "Hard penalty per missing required skill for a flight assignment");
            readIntConstraintLine(FLIGHT_CONFLICT, null, "Hard penalty per 2 flights of an employee that directly overlap");
            readIntConstraintLine(TRANSFER_BETWEEN_TWO_FLIGHTS, null, "Hard penalty per 2 sequential flights of an employee with no viable transfer from the arrival airport to the departure airport");
            readIntConstraintLine(EMPLOYEE_UNAVAILABILITY, null, "Hard penalty per flight assignment to an employee that is unavailable");
            solution.setParametrization(parametrization);
        }

        private void readSkillList() {
            nextSheet("Skills");
            nextRow(false);
            readHeaderCell("Name");
            List<Skill> skillList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            skillMap = new HashMap<>(currentSheet.getLastRowNum() - 1);
            long id = 0L;
            while (nextRow()) {
                Skill skill = new Skill();
                skill.setId(id++);
                skill.setName(nextStringCell().getStringCellValue());
                skillMap.put(skill.getName(), skill);
                skillList.add(skill);
            }
            solution.setSkillList(skillList);
        }

        private void readAirportList() {
            nextSheet("Airports");
            nextRow(false);
            // City Country IATA Latitude Longitude Altitude Timezone DST
            readHeaderCell("City");
            readHeaderCell("Country");
            readHeaderCell("IATA");
            readHeaderCell("Latitude");
            readHeaderCell("Longitude");
            readHeaderCell("Altitude");
            readHeaderCell("Timezone");
            readHeaderCell("DST");

            List<Airport> airportList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            airportMap = new HashMap<>(currentSheet.getLastRowNum() - 1);
            long id = 0L;
            while (nextRow()) {
                Airport airport = new Airport();
                airport.setId(id++);
                airport.setName(nextStringCell().getStringCellValue());
                nextCell();
                airport.setCode(nextStringCell().getStringCellValue());
                airport.setLatitude(nextNumericCell().getNumericCellValue());
                airport.setLongitude(nextNumericCell().getNumericCellValue());
                airport.setTaxiTimeInMinutesMap(new LinkedHashMap<>(currentSheet.getLastRowNum()));
                airportMap.put(airport.getCode(), airport);
                airportList.add(airport);
            }
            solution.setAirportList(airportList);
        }

        private void readTaxiTimeMaps() {
            nextSheet("Taxi time");
            nextRow();
            readHeaderCell("Driving time in minutes by taxi between two nearby airports to allow employees to start from a different airport.");
            nextRow();
            readHeaderCell("Airport code");

            // prepare destination airport list
            String destinationCode;
            List<String> destinationList = new ArrayList<>(100);
            while ((destinationCode = nextStringCell().getStringCellValue()) != null
                    && !destinationCode.isEmpty()) {
                destinationList.add(destinationCode);
            }

            // each row is a taxi departing location
            while (nextRow()) {
                String departingCode = nextStringCell().getStringCellValue();
                for (String destination : destinationList) {
                    Airport destinationAirport = airportMap.get(destination);
                    XSSFCell taxiTimeCell = nextNumericCellOrBlank();
                    if (taxiTimeCell != null) {
                        if (airportMap.get(departingCode) == null)
                            System.out.println(departingCode);
                        Map<Airport, Long> taxiTimeInMinutesMap = airportMap.get(departingCode)
                                                                            .getTaxiTimeInMinutesMap();
                        taxiTimeInMinutesMap.put(destinationAirport, (long) taxiTimeCell.getNumericCellValue());
                    }
                }

            }
        }

        private void readIataFlights() {
            nextSheet("Iata Flights");
            nextRow();
            readHeaderCell("Departure station");
            nextRow();
            // Header: DEP  DEST    Departure time  Arrival time    DAY_OF_WEEK
            readHeaderCell("DEP");
            
            long id = 0;
            
            // each row is a iata flight
            while (nextRow()) {
                IataFlight iataFlight = new IataFlight();
                iataFlight.setId(id++);
                
                String departureCode = nextStringCell().getStringCellValue();
                Airport departureAirport = airportMap.get(departureCode);

                iataFlight.setDepartureAirport(departureAirport);
                String arrivalCode = nextStringCell().getStringCellValue();
                iataFlight.setArrivalAirport(airportMap.get(arrivalCode));
                iataFlight.setDepartureUTCTime(LocalDateTime.ofInstant(nextNumericCell().getDateCellValue().toInstant(), ZoneId.systemDefault()).toLocalTime());
                iataFlight.setArrivalUTCTime(LocalDateTime.ofInstant(nextNumericCell().getDateCellValue().toInstant(), ZoneId.systemDefault()).toLocalTime());
                iataFlight.setDaysOfWeek(nextCell().getRawValue());
                
                departureAirport.addIataFlight(iataFlight);
            }
        }

        private void readEmployeeList() {
            List<Employee> employeeList = new ArrayList<>();
            nameToEmployeeMap = new HashMap<>();
            mapEmployeeDateToDutyCode = new HashMap<>();

            // CP Capitans ------
            nextSheet("CP");
            nextRow(false);
            readHeaderCell("unique ID Crewmember");
            nextRow(false);

            long id = 0L;
            while (nextRow()) {
                if (readEmployee(employeeList,"CP", id))
                    id++;
            }
            // FO First Officer ------
            nextSheet("FO");
            nextRow(false);
            readHeaderCell("unique ID Crewmember");
            nextRow(false);

            while (nextRow()) {
                if (readEmployee(employeeList, "FO", id))
                    id++;
            }

            solution.setEmployeeList(employeeList);
        }

        /**
         * 
         * @param employeeList
         * @param skill 
         * @param id keep track of new instances of Employee added
         * @return true if new employee added
         */
        private boolean readEmployee(List<Employee> employeeList, String skill, long id) {
            String employeeName = nextStringCell().getStringCellValue();
            Employee employee = nameToEmployeeMap.get(employeeName);
            boolean newEmp = false;
            if (employee == null) {
                newEmp = true;
                employee = new Employee();
                employee.setId(id);
                employee.setName(employeeName);
                HashSet<Skill> skillSet = new HashSet<>();
                skillSet.add(skillMap.get(skill));
                employee.setSkillSet(skillSet);
                employee.setFlightAssignmentSet(new TreeSet<>(
                        FlightAssignment.DATE_TIME_COMPARATOR));
                Set<LocalDate> unavailableDaySet = new LinkedHashSet<>();
                employee.setUnavailableDaySet(unavailableDaySet);

                nameToEmployeeMap.put(employee.getName(), employee);
                employeeList.add(employee);
                
                // aircraft type qualification
                String aircraftTypeQualificationsValue = nextStringCell().getStringCellValue();
                String[] aircraftTypeQualificationsArray = aircraftTypeQualificationsValue.split(COMMA_SPLIT);
                Set<String> aircraftTypeQualifications = new HashSet<>();
                for (String aircraftTypeQualification : aircraftTypeQualificationsArray) {
                    String[] qualificationAircraftTypeArray = qualificationAircraftTypeMap.get(aircraftTypeQualification);
                    if (qualificationAircraftTypeArray!= null)
                        aircraftTypeQualifications.addAll(Arrays.asList(qualificationAircraftTypeArray));
                }
                employee.setAircraftTypeQualifications(aircraftTypeQualifications);

                // Home base
                String homeAirportCode = nextStringCell().getStringCellValue();
                Airport homeAirport = airportMap.get(homeAirportCode);
                if (homeAirport == null) {
                    throw new IllegalStateException(currentPosition() + ": The employee ("
                            + employee.getName() + ")'s homeAirport (" + homeAirportCode
                            + ") does not exist in the airports (" + airportMap.keySet()
                            + ") of the other sheet (Airports).");
                }
                employee.setHomeAirport(homeAirport);
                // special qualification
                String specialQualificationsValue = nextStringCell().getStringCellValue();
                String[] specialQualificationsArray = specialQualificationsValue.split(COMMA_SPLIT);
                Set<String> specialQualifications = new HashSet<>();
                specialQualifications.addAll(Arrays.asList(specialQualificationsArray));
                employee.setSpecialQualifications(specialQualifications);
            } else {
                // skip repeated information
                // aircraft type qualification
                nextCell();
                // Home base
                nextCell();
                // Special qualification
                nextCell();                    
            }
            // Duty date
            String dutyDateStr = nextStringCell().getStringCellValue();
            // Duty code
            String[] dutyCodes = nextStringCell().getStringCellValue().split(COMMA_SPLIT);

            LocalDate dutyDate = LocalDate.parse(dutyDateStr, DATE_FORMATTER);
            
            for (String dutyCode : dutyCodes) {
                if (dutyCode.matches(DAY_OFF_MATCH)) {
                    employee.getUnavailableDaySet().add(dutyDate);
                } else if (dutyCode.matches(PRE_ASSIGNED_DUTY_MATCH)) {
                    // No new duty, add the code in map emp-date/dutyCode, it will be managed later by the preassigned duties
                    mapEmployeeDateToDutyCode.put(employeeName+"-"+dutyDate, dutyCode);
                } else if (readSolved){
                    FlightAssignment flightAssignment = mapFlightDateSkillToFlightAssignment.get(dutyCode+"@"+dutyDateStr+"#"+skill);

                    if (flightAssignment != null) {
                        flightAssignment.setEmployee(employee);

                        Duty duty = employee.getDutyByDate(dutyDate);

                        if (duty != null) {
                            duty.addFlightAssignment(flightAssignment);
                        } else {
                            duty = new Duty();
                            duty.setEmployee(employee);
                            duty.setDate(dutyDate);
                            duty.addFlightAssignment(flightAssignment);
                            employee.setDutyByDate(dutyDate, duty);
                        }
                    }
                }
            }

            return newEmp;
        }

        private void readFlightListAndFlightAssignmentList() {
            nextSheet("Flights");
            nextRow(false);
            // AF_DAT   AF_AL   AF_NR   AF_DEP  AF_DEST AF_AB_S AF_AN_S AC_REG  AF_FT_KZ

            readHeaderCell("AF_DAT");
            readHeaderCell("AF_AL");
            readHeaderCell("AF_NR");
            readHeaderCell("AF_DEP");
            readHeaderCell("AF_DEST");
            readHeaderCell("AF_AB_S");
            readHeaderCell("AF_AN_S");
            readHeaderCell("AC_REG");
            readHeaderCell("AF_FT_KZ");
            
            List<Flight> flightList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            List<FlightAssignment> flightAssignmentList = new ArrayList<>(
                    (currentSheet.getLastRowNum() - 1) * 5);
            mapFlightDateSkillToFlightAssignment = new HashMap<>();
            
            long id = 0L;
            long flightAssignmentId = 0L;
            while (nextRow()) {
                Flight flight = new Flight();
                flight.setId(id++);
                // skip planned date
                nextCell();
                // AF_AL   AF_NR
                flight.setFlightNumber(nextStringCell().getStringCellValue() + nextStringCell().getStringCellValue());
                // AF_DEP
                String departureAirportCode = nextStringCell().getStringCellValue();
                Airport departureAirport = airportMap.get(departureAirportCode);
                if (departureAirport == null) {
                    throw new IllegalStateException(currentPosition() + ": The flight ("
                            + flight.getFlightNumber() + ")'s departureAirport ("
                            + departureAirportCode + ") does not exist in the airports ("
                            + airportMap.keySet() + ") of the other sheet (Airports).");
                }
                flight.setDepartureAirport(departureAirport);
                // AF_DEST
                String arrivalAirportCode = nextStringCell().getStringCellValue();
                Airport arrivalAirport = airportMap.get(arrivalAirportCode);
                if (arrivalAirport == null) {
                    throw new IllegalStateException(currentPosition() + ": The flight ("
                            + flight.getFlightNumber() + ")'s arrivalAirport (" + arrivalAirportCode
                            + ") does not exist in the airports (" + airportMap.keySet()
                            + ") of the other sheet (Airports).");
                }
                flight.setArrivalAirport(arrivalAirport);

                // AF_AB_S
                flight.setDepartureUTCDateTime(LocalDateTime.parse(nextStringCell().getStringCellValue(), DATE_TIME_FORMATTER));
                // AF_AN_S
                flight.setArrivalUTCDateTime(LocalDateTime.parse(nextStringCell().getStringCellValue(), DATE_TIME_FORMATTER));
                // AC_REG
                flight.setAircraftRegistration(nextStringCell().getStringCellValue());
                // AF_FT_KZ
                flight.setAircraftType(nextStringCell().getStringCellValue());

                // By default just CP and FO
                String[] skillNames = {"CP", "FO" };
                String[] employeeNames = nextStringCell().getStringCellValue().split(", ");
                for (int i = 0; i < skillNames.length; i++) {
                    FlightAssignment flightAssignment = new FlightAssignment();
                    flightAssignment.setId(flightAssignmentId++);
                    flightAssignment.setFlight(flight);
                    flightAssignment.setIndexInFlight(i);
                    Skill requiredSkill = skillMap.get(skillNames[i]);
                    if (requiredSkill == null) {
                        throw new IllegalStateException(currentPosition() + ": The flight ("
                                + flight.getFlightNumber() + ")'s requiredSkill (" + requiredSkill
                                + ") does not exist in the skills (" + skillMap.keySet()
                                + ") of the other sheet (Skills).");
                    }
                    flightAssignment.setRequiredSkill(requiredSkill);
                    flightAssignmentList.add(flightAssignment);
                    //map used to pre-assign employee
                    mapFlightDateSkillToFlightAssignment.put(flight.getFlightNumber()+"@"+DATE_FORMATTER.format(flight.getDepartureUTCDate())+"#"+skillNames[i], flightAssignment);                    
                }
                flightList.add(flight);
            }
            solution.setFlightList(flightList);
            solution.setFlightAssignmentList(flightAssignmentList);
        }

    }

    @Override
    public void write(FlightCrewSolution solution, File outputSolutionFile) {
        try (FileOutputStream out = new FileOutputStream(outputSolutionFile)) {
            Workbook workbook = new FlightCrewSchedulingXlsxWriter(solution).write();
            workbook.write(out);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed writing outputSolutionFile ("
                    + outputSolutionFile + ") for solution (" + solution + ").", e);
        }
    }

    private static class FlightCrewSchedulingXlsxWriter
            extends AbstractXlsxWriter<FlightCrewSolution> {
        protected static final XSSFColor FILLED_COLOR = new XSSFColor(TangoColorFactory.ORANGE_1);

        public FlightCrewSchedulingXlsxWriter(FlightCrewSolution solution) {
            super(solution, FlightCrewSchedulingApp.SOLVER_CONFIG);
        }

        @Override
        public Workbook write() {
            writeSetup();
            writeConfiguration();
            writeSkillList();
            writeAirportList();
            writeTaxiTimeMaps();
            writeEmployeeList();
            writeFlightListAndFlightAssignmentList();
            writeEmployeesView();
            writeScoreView();
            return workbook;
        }

        private void writeConfiguration() {
            nextSheet("Configuration", 1, 4, false);
            nextRow();
            nextHeaderCell("Schedule start UTC Date");
            nextCell().setCellValue(DAY_FORMATTER.format(solution.getScheduleFirstUTCDate()));
            nextRow();
            nextHeaderCell("Schedule end UTC Date");
            nextCell().setCellValue(DAY_FORMATTER.format(solution.getScheduleLastUTCDate()));
            nextRow();
            nextRow();
            nextHeaderCell("Constraint");
            nextHeaderCell("Weight");
            nextHeaderCell("Description");
            FlightCrewParametrization parametrization = solution.getParametrization();

            writeLongConstraintLine(LOAD_BALANCE_FLIGHT_DURATION_TOTAL_PER_EMPLOYEE, parametrization::getLoadBalanceFlightDurationTotalPerEmployee, "Soft penalty per 0.001 minute difference with the average flight duration total per employee.");
            nextRow();
            writeIntConstraintLine(REQUIRED_SKILL, null, "Hard penalty per missing required skill for a flight assignment");
            writeIntConstraintLine(FLIGHT_CONFLICT, null, "Hard penalty per 2 flights of an employee that directly overlap");
            writeIntConstraintLine(TRANSFER_BETWEEN_TWO_FLIGHTS, null, "Hard penalty per 2 sequential flights of an employee with no viable transfer from the arrival airport to the departure airport");
            writeIntConstraintLine(EMPLOYEE_UNAVAILABILITY, null, "Hard penalty per flight assignment to an employee that is unavailable");
            autoSizeColumnsWithHeader();
        }

        private void writeSkillList() {
            nextSheet("Skills", 1, 1, false);
            nextRow();
            nextHeaderCell("Name");
            for (Skill skill : solution.getSkillList()) {
                nextRow();
                nextCell().setCellValue(skill.getName());
            }
            autoSizeColumnsWithHeader();
        }

        private void writeAirportList() {
            nextSheet("Airports", 1, 1, false);
            nextRow();
            nextHeaderCell("Code");
            nextHeaderCell("Name");
            nextHeaderCell("Latitude");
            nextHeaderCell("Longitude");
            for (Airport airport : solution.getAirportList()) {
                nextRow();
                nextCell().setCellValue(airport.getCode());
                nextCell().setCellValue(airport.getName());
                nextCell().setCellValue(airport.getLatitude());
                nextCell().setCellValue(airport.getLongitude());
            }
            autoSizeColumnsWithHeader();
        }

        private void writeTaxiTimeMaps() {
            nextSheet("Taxi time", 1, 1, false);
            nextRow();
            nextHeaderCell("Driving time in minutes by taxi between two nearby airports to allow employees to start from a different airport.");
            currentSheet.addMergedRegion(new CellRangeAddress(currentRowNumber, currentRowNumber,
                    currentColumnNumber, currentColumnNumber + 20));
            List<Airport> airportList = solution.getAirportList();
            nextRow();
            nextHeaderCell("Airport code");
            for (Airport airport : airportList) {
                nextHeaderCell(airport.getCode());
            }
            for (Airport a : airportList) {
                nextRow();
                nextHeaderCell(a.getCode());
                for (Airport b : airportList) {
                    Long taxiTime = a.getTaxiTimeInMinutesTo(b);
                    if (taxiTime == null) {
                        nextCell();
                    } else {
                        nextCell().setCellValue(taxiTime);
                    }
                }
            }
            autoSizeColumnsWithHeader();
        }

        private void writeEmployeeList() {
            nextSheet("Employees", 1, 2, false);
            nextRow();
            nextHeaderCell("");
            nextHeaderCell("");
            nextHeaderCell("");
            nextHeaderCell("Unavailability");
            nextRow();
            nextHeaderCell("Name");
            nextHeaderCell("Home airport");
            nextHeaderCell("Skills");
            LocalDate firstDate = solution.getScheduleFirstUTCDate();
            LocalDate lastDate = solution.getScheduleLastUTCDate();
            for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                nextHeaderCell(DAY_FORMATTER.format(date));
            }
            for (Employee employee : solution.getEmployeeList()) {
                nextRow();
                nextCell().setCellValue(employee.getName());
                nextCell().setCellValue(employee.getHomeAirport().getCode());
                nextCell().setCellValue(String.join(", ", employee.getSkillSet().stream()
                                                                  .map(Skill::getName)
                                                                  .collect(toList())));
                for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                    nextCell(employee.getUnavailableDaySet().contains(date) ? unavailableStyle
                            : defaultStyle).setCellValue("");
                }
            }
            autoSizeColumnsWithHeader();
        }

        private void writeFlightListAndFlightAssignmentList() {
            nextSheet("Flights", 1, 1, false);
            nextRow();
            nextHeaderCell("Flight number");
            nextHeaderCell("Departure airport code");
            nextHeaderCell("Departure UTC date time");
            nextHeaderCell("Arrival airport code");
            nextHeaderCell("Arrival UTC date time");
            nextHeaderCell("Employee skill requirements");
            nextHeaderCell("Employee assignments");
            Map<Flight, List<FlightAssignment>> flightToFlightAssignmentMap = solution.getFlightAssignmentList()
                                                                                      .stream()
                                                                                      .collect(groupingBy(FlightAssignment::getFlight, toList()));
            for (Flight flight : solution.getFlightList()) {
                nextRow();
                nextCell().setCellValue(flight.getFlightNumber());
                nextCell().setCellValue(flight.getDepartureAirport().getCode());
                nextCell().setCellValue(DATE_TIME_FORMATTER.format(flight.getDepartureUTCDateTime()));
                nextCell().setCellValue(flight.getArrivalAirport().getCode());
                nextCell().setCellValue(DATE_TIME_FORMATTER.format(flight.getArrivalUTCDateTime()));

                List<FlightAssignment> flightAssignmentList = flightToFlightAssignmentMap.get(flight);
                nextCell().setCellValue(flightAssignmentList.stream()
                                                            .map(FlightAssignment::getRequiredSkill)
                                                            .map(Skill::getName)
                                                            .collect(joining(", ")));
                nextCell().setCellValue(flightAssignmentList.stream()
                                                            .map(FlightAssignment::getEmployee)
                                                            .map(employee -> employee == null ? ""
                                                                    : employee.getName())
                                                            .collect(joining(", ")));
            }
            autoSizeColumnsWithHeader();
        }

        private void writeEmployeesView() {
            XSSFCellStyle missionStyle = createStyle(FILLED_COLOR);
            XSSFCellStyle homeStyle = createStyle((byte)0x6E,(byte)0xff,(byte)0xC8);
            
            nextSheet("Employees view", 2, 2, true);
            int minimumHour = solution.getFlightList().stream().map(Flight::getDepartureUTCTime)
                                      .map(LocalTime::getHour).min(Comparator.naturalOrder())
                                      .orElse(9);
            int maximumHour = solution.getFlightList().stream().map(Flight::getArrivalUTCTime)
                                      .map(LocalTime::getHour).max(Comparator.naturalOrder())
                                      .orElse(17);
            nextRow();
            nextHeaderCell("");
            nextHeaderCell("");
            LocalDate firstDate = solution.getScheduleFirstUTCDate();
            LocalDate lastDate = solution.getScheduleLastUTCDate();
            
            // Print the header with dates
            for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                nextHeaderCell(DAY_FORMATTER.format(date));
                currentSheet.addMergedRegion(new CellRangeAddress(currentRowNumber,
                        currentRowNumber, currentColumnNumber,
                        currentColumnNumber + (maximumHour - minimumHour)));
                currentColumnNumber += (maximumHour - minimumHour);
            }
            nextRow();
            nextHeaderCell("Employee name");
            nextHeaderCell("Home airport");
            
            // Print the header with time slots
            for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                for (int hour = minimumHour; hour <= maximumHour; hour++) {
                    nextHeaderCell(TIME_FORMATTER.format(LocalTime.of(hour, 0)));
                }
            }
            Map<Employee, List<FlightAssignment>> employeeToFlightAssignmentMap = solution.getFlightAssignmentList()
                                                                                          .stream()
                                                                                          .filter(flightAssignment -> flightAssignment.getEmployee() != null)
                                                                                          .collect(groupingBy(FlightAssignment::getEmployee, toList()));
            
            // Print the schedule for all the emplooyees
            for (Employee employee : solution.getEmployeeList()) {
                nextRow();
                nextHeaderCell(employee.getName());
                nextHeaderCell(employee.getHomeAirport().getCode());

                // for each day in the schedule
                for (LocalDate date = firstDate; date.compareTo(lastDate) <= 0; date = date.plusDays(1)) {
                    boolean unavailable = employee.getUnavailableDaySet().contains(date);

                    Duty duty = employee.getDutyByDate(date);

                    // for each time slot
                    for (int departureHour = minimumHour; departureHour <= maximumHour; departureHour++) {
                        // get the flight assignment list for a departing hour

                        // Flight shot
                        StringBuffer flightSlot = null;
                        LocalDateTime lastArrival = null;
                        LocalDateTime firstDeparture = null;

                        // if flight duty
                        if (duty.isFlightDuty()) {
                            for (FlightAssignment flightAssignment : duty.getFlightAssignments()) {
                                Flight flight = flightAssignment.getFlight();

                                // if the flight depart at hour
                                if (flight.getDepartureUTCTime().getHour() == departureHour) {
                                    // First flight in the time slot
                                    if (flightSlot == null) {
                                        firstDeparture = flight.getDepartureUTCDateTime();
                                        flightSlot = new StringBuffer();
                                    } else
                                        flightSlot.append(", ");

                                    flightSlot.append(flight.getFlightNumber() + "-");
                                    flightSlot.append(flight.getDepartureAirport().getCode());
                                    flightSlot.append(MILITARY_TIME_FORMATTER.format(flight.getDepartureUTCTime()));
                                    flightSlot.append("→" + flight.getArrivalAirport().getCode());
                                    flightSlot.append(MILITARY_TIME_FORMATTER.format(flight.getArrivalUTCTime()));
                                    lastArrival = flight.getArrivalUTCDateTime();
                                }
                            }

                            if (flightSlot != null) {
                                XSSFCellStyle style;
                                if (duty.getClosingInconvenience() == 10)
                                    style = missionStyle;
                                else
                                    style = homeStyle;
                                
                                nextCell(style).setCellValue(flightSlot.toString());

                                int stretch = (int) Duration.between(firstDeparture, lastArrival)
                                                            .toHours();
                                try {
                                    if (stretch > 0)
                                        currentSheet.addMergedRegion(new CellRangeAddress(
                                                currentRowNumber, currentRowNumber,
                                                currentColumnNumber,
                                                currentColumnNumber + stretch));
                                } catch (Exception e) {
                                    System.err.println(e.getMessage());
                                }
                                currentRow.setHeightInPoints(30);
                                currentColumnNumber += stretch - 1;
                                departureHour += stretch;
                            }
                        }

                        try {
                            if (unavailable) {
                                nextCell(unavailableStyle);
                            } else if (duty.getCode().matches(PRE_ASSIGNED_DUTY_MATCH)
                                    && departureHour == duty.getPreAssignedDutyStart().getHour()) {
                                // workaround: remove 1 minutes for duties finishing at 00, this avoid filling the whole next hour
                                int stretch = (int) Duration.between(duty.getPreAssignedDutyStart(), duty.getPreAssignedDutyEnd().minusMinutes(1))
                                                            .abs().toHours();
                                nextCell(unavailableStyle).setCellValue(duty.getCode());
                                try {
                                    if (stretch > 0)
                                        currentSheet.addMergedRegion(new CellRangeAddress(
                                                currentRowNumber, currentRowNumber, currentColumnNumber,
                                                currentColumnNumber + stretch));
                                    
                                } catch (Exception e) {
                                    System.err.println(e.getMessage());
                                }
                            } else {
                                nextCell(defaultStyle);
                            }
                        } catch (NullPointerException e) {
                            nextCell(defaultStyle);
                        }
                    }
                    // end for each time slot
                }
            }
            setSizeColumnsWithHeader(1500);
            currentSheet.autoSizeColumn(0);
            currentSheet.autoSizeColumn(1);
        }

        private void writeScoreView() {
            nextSheet("Score view", 1, 3, true);
            nextRow();
            nextHeaderCell("Score");
            nextCell().setCellValue(solution.getScore() == null ? "Not yet solved"
                    : solution.getScore().toShortString());
            nextRow();
            nextRow();
            nextHeaderCell("Constraint match");
            nextHeaderCell("Match score");
            nextHeaderCell("Total score");
            for (ConstraintMatchTotal constraintMatchTotal : constraintMatchTotalList) {
                nextRow();
                nextHeaderCell(constraintMatchTotal.getConstraintName());
                nextCell();
                nextCell().setCellValue(constraintMatchTotal.getScore().toShortString());
                List<ConstraintMatch> constraintMatchList = new ArrayList<>(
                        constraintMatchTotal.getConstraintMatchSet());
                constraintMatchList.sort(Comparator.comparing(ConstraintMatch::getScore));
                for (ConstraintMatch constraintMatch : constraintMatchList) {
                    Number[] levelNumbers = constraintMatch.getScore().toLevelNumbers();
                    boolean notZeroScore = false;
                    for (Number number : levelNumbers) {
                        notZeroScore = number.longValue() != 0;
                        if (notZeroScore) 
                            break;
                    }
                    if (notZeroScore) {
                        nextRow();
                        StringBuffer objectMatching = new StringBuffer();
                        for (Object object : constraintMatch.getJustificationList()) {
                            if (object instanceof FlightAssignment) {
                                FlightAssignment flightAssignment = (FlightAssignment) object;
                                objectMatching.append(" - "
                                        + flightAssignment.getFlight().toString());
                            } else
                                objectMatching.append(" - " + object.toString());
                        }
                        nextCell().setCellValue(objectMatching.toString());
                        nextCell().setCellValue(constraintMatch.getScore().toShortString());
                    }
                }
            }
            autoSizeColumnsWithHeader();
        }

    }

}
