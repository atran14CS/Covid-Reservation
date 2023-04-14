package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

public class Scheduler {
    static int appointmentId = 1;
    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
        }
        String username = tokens[1];
        String password = tokens[2];
        //password strength test
        if (!strongPassword(password)) {
            System.out.println("password is not strong enough");
            return;
        }
        if(usernameExistsPatient(username)) {
            System.out.println("Username taken try, again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patient WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        //password strength test
        if (!strongPassword(password)) {
            System.out.println("password is not strong enough");
            return;
        }
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
         // login_Patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String time = tokens[1];
        String checkAvailability =  "SELECT a.Username as caregiver, v.Name as VaccineName, v.Doses as DoseAvailable " +
                                    " FROM Availabilities as a, Vaccines as v "+
                                    " WHERE a.Time = ? " +
                                    " ORDER BY a.Username ";
        try {
            PreparedStatement caregiverSchedule = con.prepareStatement(checkAvailability); //Look at checkAvailability sql query
            Date date = Date.valueOf(time); //convert time string type into date type
            caregiverSchedule.setDate(1, date); //place the date into the '?'
            ResultSet rs = caregiverSchedule.executeQuery(); //executeQuery
            while(rs.next()) {
                System.out.println("Caregiver Username " + rs.getString(1) + " Vaccine Name " +
                                    rs.getString(2) + " Number doses " + rs.getInt(3));
            }
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }


    private static void reserve(String[] tokens) {
        // TODO: Part 2
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        } else if (currentCaregiver != null && currentPatient == null) {
            System.out.println("patient performs this operation");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String time = tokens[1];
        String vaccineName = tokens[2];
        String availableCaregiver = "SELECT Username  " +
                                    " FROM Availabilities  " +
                                    " WHERE Time = ? " +
                                    " ORDER BY Username ";

        String dosageCheckage = "SELECT doses " +
                                " FROM Vaccines  " +
                                " WHERE Name = ? ";
        try {
            PreparedStatement caregiverSchedule = con.prepareStatement(availableCaregiver); //sql query for availableCaregiver
            Date date = Date.valueOf(time); //turn time string time into date date
            caregiverSchedule.setDate(1, date); //plug in the date for ?
            ResultSet rs = caregiverSchedule.executeQuery(); //run the query
            if (!rs.next()) { //checks if no Caregiver is available!
                System.out.println("No Caregiver available!");
                return;
            }
            //Check for dosage.
            PreparedStatement dosage = con.prepareStatement(dosageCheckage);//sql query for dosage
            dosage.setString(1, vaccineName); //name of vaccine
            ResultSet amountDosage = dosage.executeQuery(); //execute the query
            while(amountDosage.next()) {
                if (amountDosage.getInt(1) == 0) { //checks if the dosage is empty
                    System.out.println("Not Enough available doses!");
                    return;
                }
            }
//          decrease vaccine dosage
//          Getting the reservation done and updating the Availabilities
            String CareGiverName = rs.getString(1); //grabs the first caregiver username
            System.out.println(CareGiverName);
            //select availablity * print all result expected table
            String removeCaregiver = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?"; //Sql query to remove the Caregiver
            PreparedStatement reservation = con.prepareStatement(removeCaregiver); //loaded up the query
            reservation.setString(1, CareGiverName); //plug in the caregiver name
            reservation.setDate(2, date); //plug in the date
            reservation.executeUpdate(); //execute the query
            //put reservation into appointment
            String reserveQuery = "Insert Into Appointment (AppID, PUsername, CUsername, Name, Time) " +
                                    " Values( ?, ?, ?, ?, ? ) ";
            PreparedStatement queryReservation = con.prepareStatement(reserveQuery);
            queryReservation.setInt(1, appointmentId);
            queryReservation.setString(2, currentPatient.Username());
            queryReservation.setString(3, CareGiverName);
            queryReservation.setString(4, vaccineName);
            queryReservation.setDate(5, date);
            queryReservation.executeUpdate();
            System.out.println("Appointment Made! -> Appointment ID: " + appointmentId + " Caregiver: " + CareGiverName);
            appointmentId++;
            Vaccine dosesVaccine = new Vaccine.VaccineGetter(vaccineName).get();
            dosesVaccine.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Please try again!");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static boolean strongPassword(String password) {
        if (password.length() < 8) {
            System.out.println("Password length must be greater than 8");
            return false;
        } else if (!password.matches(".*[a-z].*")) {
            System.out.println("Needs at least 1 lower case letter");
            return false;
        } else if (!password.matches(".*[A-Z].*")) {
            System.out.println("Needs at least 1 upper case letter");
            return false;
        } else if (!password.matches(".*\\d.*")) {
            System.out.println("Needs at least 1 number");
            return false;
        } else if (!password.matches(".*[!@#?].*")) {
            System.out.println("Needs at least 1 special character '!' '@' '#' '?' ");
            return false;
        }
        return true;
    }

//    private static void cancel(String[] tokens) {
//        // TODO: Extra credit
//        ConnectionManager cm = new ConnectionManager();
//        Connection con = cm.createConnection();
//        if (currentCaregiver == null && currentPatient == null) {
//            System.out.println("Please login in first!");
//            return;
//        }
//        if (tokens.length != 2) {
//            System.out.println("Please try again!");
//            return;
//        }
//        String id = tokens[1];
//        int appId = Integer.parseInt(id);
//        try {
//
//            String cancellation = "DELETE FROM Appointment WHERE AppID = ? ";
//            PreparedStatement statement = con.prepareStatement(cancellation);
//            statement.setInt(1, appId);
//            statement.executeUpdate();
//
//        } catch (SQLException e) {
//            System.out.println("Please try again!");
//            e.printStackTrace();
//        } finally {
//            cm.closeConnection();
//        }
//    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login in first!");
            return;
        } else if (currentCaregiver != null || currentPatient == null) {
            String appointments = ("SELECT a.AppID, a.Name, a.Time, a.PUsername " +
                                    " FROM Appointment as a " +
                                    " WHERE a.CUsername = ? " +
                                    " ORDER BY a.AppID ");
            try {
                PreparedStatement allAppointments = con.prepareStatement(appointments);
                allAppointments.setString(1, currentCaregiver.getUsername());
                ResultSet rs = allAppointments.executeQuery();
                while(rs.next()) {
                    System.out.println("AppointmentID: " + rs.getInt(1) + " Vaccine Name: " + rs.getString(2) +
                                        " Date: " + rs.getDate(3) + " Patient Username " + rs.getString(4));
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            }
        } else {
            String appointments = (" SELECT a.AppID, a.Name, a.Time, a.CUsername " +
                    " FROM Appointment as a " +
                    " WHERE a.PUsername = ? " +
                    " ORDER BY a.AppID ");
            try {
                PreparedStatement allAppointments = con.prepareStatement(appointments);
                allAppointments.setString(1, currentPatient.Username());
                ResultSet rs = allAppointments.executeQuery();
                while(rs.next()) {
                    System.out.println("AppointmentID: " + rs.getInt(1) + " Vaccine Name: " + rs.getString(2) +
                            " Date: " + rs.getDate(3) + " Caregiver Username " + rs.getString(4));
                }
            } catch(SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            }
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if(currentCaregiver == null && currentPatient == null) {
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
        } else if (currentCaregiver != null && currentPatient == null || currentCaregiver == null && currentPatient != null) {
            currentPatient = null;
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
        } else {
            System.out.println("Please try again!");
        }
    }
}
