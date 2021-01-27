/*
 * StudentFunctions.java
 * @author Matt Prestwich
 */

/**
 * A class containing all the functions available to student accounts.
 */

package systems.team040.functions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentFunctions {
    private static final int PASSWORD_LENGTH = 12;

    public static String getIDFromUsername(Connection con, String username) throws SQLException {
        String query = "SELECT StudentID FROM Student WHERE Username = ?;";

        try(PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, username);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    public static String getIDFromUsername(String username) throws SQLException {
        try(Connection con = SQLFunctions.connectToDatabase()) {
            return getIDFromUsername(con, username);
        }
    }

    /**
     * Returns a randomly generated password with PASSWORD_LENGTH random alphabetic characters and one random
     * number interspersed
     */
    public static char[] generateRandomPassword() {
        char[] availChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
        char[] password = new char[PASSWORD_LENGTH];

        for(int i = 0; i < PASSWORD_LENGTH ; i++) {
            password[i] = availChars[(int)(Math.random() * availChars.length)];
        }

        return password;
    }
}
