package systems.team040.functions;
/*
 * RegistrarFunctions.java
 * @author Matt Prestwich
 * @author Byron Slater
 * @author James Taylor
 */


import java.sql.*;
/**
 * A class containing all the functions available to registrar accounts.
 */
public class RegistrarFunctions {
    /**
     * Gets the next highest registration number available on the db
     */
    private static String getNextRegNo() throws SQLException {
        int i = 1;
        String query = "SELECT MAX(StudentID) FROM Student;";
        try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = con.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery()) {

            if(rs.next()) {
                i = rs.getInt(1) + 1;
            } else {

                i = 1;
            }
        }

        // regno is in '000000001' format
        return String.format("%09d", i);
    }

    /**
     * get the next highest free username on the database
     * should hopefully never ever return a username that already exists
     */
    private static String getNextUsername(String username) throws SQLException {
        int usernameLength = username.length();
        int highestSoFar = 0;
        String query = "SELECT SUBSTRING(Username, ?) FROM UserAccount WHERE Username LIKE ?;";

        try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setInt(1, usernameLength + 1);
            pstmt.setString(2, username + "%");

            try(ResultSet rs = pstmt.executeQuery()) {

                while(rs.next()) {
                    String number = rs.getString(1);
                    int parsed;

                    try {
                        parsed = Integer.parseInt(number);
                        highestSoFar = (parsed > highestSoFar) ? parsed : highestSoFar;
                    } catch (NumberFormatException e) {
                        // Just catches strings we can't parse so we can skip
                    }
                }
            }
        }

        return username + (highestSoFar + 1);
    }

    /**
     * Tells us if a studentperiod is a resit, i.e. it is not the unique attempt of a degreelevel
     */
    public static boolean isRetake(Connection con, String studentPeriod) throws SQLException {
        String query = "SELECT DegreeLevel, StudentID FROM StudentPeriod WHERE StudentPeriod = ?; ";
        String degreeLevel, studentID;

        try(PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, studentPeriod);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                degreeLevel = rs.getString("DegreeLevel");
                studentID = rs.getString("studentID");
            }

        }
        query = "SELECT COUNT(*) FROM StudentPeriod WHERE StudentID = ? AND DegreeLevel = ?; ";
        try(PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, studentID);
            pstmt.setString(2, degreeLevel);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 1;
            }
        }
    }

    /**
     * Function employed to add student a student to the Student table with given details. Also takes a degree and add registers them for that degree.
     * Will generate a unique Userid and Username.
     *
     * returns the password given to the student
     */
    public static char[] addStudent(
            String title, String forenames, String surname, String tutor, String degree, String startDate, char[] password
    ) throws SQLException {

        // if surname has any spaces, we'll just take the first string before a space
        surname = surname.split("\\s+")[0];


        // Username is first letter of first name capitalized then surname in title case
        StringBuilder sb = new StringBuilder();
        sb.append(forenames.toUpperCase().charAt(0));
        sb.append(surname.toUpperCase().charAt(0));
        sb.append(surname.toLowerCase().substring(1));

        // get the next available username in usernameXX format
        String username = getNextUsername(sb.toString());


        // this kinda introduces a race condition because if another user calls this function between us getting this
        // number and us putting it on the db then we'll have an issue but it's a primary key so we crash in that case
        String studentID = getNextRegNo();


        String userQuery = "INSERT INTO UserAccount(Username, Password, AccountType) VALUES(?, ?, 3);";
        String studentQuery = "INSERT INTO Student VALUES (?, ?, ?, ?, ?, ?, ?);";
        String modulesQuery = "INSERT INTO Grades(StudentPeriod, ModuleID)" +
                "SELECT ?, ModuleID " +
                "FROM DegreeModule " +
                "WHERE DegreeLevel = ? AND isCore = 1;";

        String studentPeriodQuery = "INSERT INTO StudentPeriod VALUES (?, ?, ?, ?, ?);";

        try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement userPstmt = con.prepareStatement(userQuery);
            PreparedStatement studentPstmt = con.prepareStatement(studentQuery);
            PreparedStatement modulesPstmt = con.prepareStatement(modulesQuery);
            PreparedStatement studentPeriodPstmt = con.prepareStatement(studentPeriodQuery)) {

            // emailToStudent(studentsPersonalEmail, password);
            System.out.println("PASSWORD GIVEN TO STUDENT:");
            System.out.println(new String(password));

            // We want the whole transaction to either go through or fail we don't want students with no
            // degreelevel or student accounts with no student information
            con.setAutoCommit(false);

            userPstmt.setString(1, username);
            userPstmt.setString(2, Hasher.generateDigest(password));
            userPstmt.executeUpdate();

            studentPstmt.setString(1, studentID);
            studentPstmt.setString(2, title);
            studentPstmt.setString(3, forenames);
            studentPstmt.setString(4, surname);
            studentPstmt.setString(5, username.toLowerCase() + "@sheffield.ac.uk");
            studentPstmt.setString(6, tutor);
            studentPstmt.setString(7, username);
            studentPstmt.executeUpdate();

            // Gives the student a starting student study period with value A
            studentPeriodPstmt.setString(1, 'A' + studentID);
            studentPeriodPstmt.setString(2, "A");
            studentPeriodPstmt.setString(3, '1' + degree);
            studentPeriodPstmt.setString(4, studentID);
            studentPeriodPstmt.setString(5, startDate);
            studentPeriodPstmt.executeUpdate();

            // Gives the student a grade for each module
            modulesPstmt.setString(1, 'A' + studentID);
            modulesPstmt.setString(2, '1' + degree);
            modulesPstmt.executeUpdate();

            con.commit();
        }
        catch (SQLException ex) {
            System.err.println("Couldn't create new student!");
            throw ex;
        }

        return password;
    }

    /**
     * Function employed to remove student details. Takes the student username and deletes them for the UserAccount table.
     * This cascades and deletes the user from the system.
     */
    public static void removeStudent(String username) throws SQLException {
        Connection con = null;
        PreparedStatement pstmt = null;

        try {
            String query = "DELETE FROM UserAccount WHERE Username = ?;";
            con = SQLFunctions.connectToDatabase();
            pstmt = con.prepareStatement(query);
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
        finally {
            SQLFunctions.closeAll(con, pstmt);
        }
    }
}

