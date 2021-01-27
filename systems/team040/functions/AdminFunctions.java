/*
 * AdminFunctions.java
 * @author Matt Prestwich
 * @author James Taylor
 */

/**
 * A class containing all the functions available to admin accounts.
 */
package systems.team040.functions;

import java.sql.*;

public class AdminFunctions {
	/**
	 * Function employed to create user accounts and set their privileges.
	 */
	public static void createAccount(String username, char[] password, int level) throws SQLException {
		String digest = Hasher.generateDigest(password);
		String query = "INSERT INTO UserAccount VALUES (?, ?, ?);";

		try(Connection con = SQLFunctions.connectToDatabase();
		    PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, digest);
            pstmt.setInt(3, level);
            pstmt.executeUpdate();
		}
	}

	/**
	 * Function employed to remove a user account from the user account table given the username.
	 */
	public static void removeUser(String username) throws SQLException {
	    String query = "DELETE FROM UserAccount WHERE username = ?;";

		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query)){

            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
	}

	/**
	 * Function employed to add departments. Adds a department to the department table give a 3 letter code and a full name.
	 */
	public static void addDepartment(String deptCode, String deptName) throws SQLException {
		String query = "INSERT INTO Department VALUES (?, ?);";

		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query)) {

			pstmt.setString(1, deptCode);
			pstmt.setString(2, deptName);
			pstmt.executeUpdate();
		}
	}
	
	/**
	 * Function employed to remove departments. Department is deleted from the department table. This deletes any connected modules and links to degrees.
	 * Any degrees where the deleted module is the primary department are deleted.
	 */
	public static void removeDepartment(String deptCode) throws SQLException {
		String query = "DELETE FROM Department WHERE Dept = ?;";
		String query2 = "DELETE Degree FROM Degree JOIN DegreeDepartments ON Degree.DegreeCode = DegreeDepartments.DegreeCode WHERE DegreeDepartments.Dept = ? and LeadDepartment = true;";

		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query);
			PreparedStatement pstmt2 = con.prepareStatement(query2)) {

			pstmt.setString(1, deptCode);
			pstmt.executeUpdate();
			pstmt2.setString(1, deptCode);
			pstmt2.executeUpdate();
		}
	}
	
	/**
	 * Function employed to add degree courses. Adds to the degree table and then creates items in the degreeLevel table based on the number of years and if there is a year in industry.
	 * Finally creates a link the primary module of the department.
	 */
	public static void addDegree(String degreeCode, String degreeName, int degreeLength, boolean hasIndustryYear, String pDept) throws SQLException {
		String degreeQuery = "INSERT INTO Degree VALUES (?, ?);";
		String degreeLevelsQuery = "INSERT INTO DegreeLevel VALUES (?, ?, ?, ?);";
		String degreeDepartment = "INSERT INTO DegreeDepartments VALUES (?,?, true);";


		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt1 = con.prepareStatement(degreeQuery);
			PreparedStatement pstmt2 = con.prepareStatement(degreeLevelsQuery);
			PreparedStatement pstmt3 = con.prepareStatement(degreeDepartment)){

			
			int actualLength = degreeLength;
			String actualDegreeCode = degreeCode;
			if (hasIndustryYear) {
				actualLength += 1;
				actualDegreeCode =  degreeCode + "P";
			}
			
		    con.setAutoCommit(false);
			pstmt1.setString(1, actualDegreeCode);
			pstmt1.setString(2, degreeName);
			pstmt1.executeUpdate();
			
			
			for(int i = 1; i <= actualLength; i++) {
				
				pstmt2.setString(2, actualDegreeCode);
				if (i == (actualLength - 1) && (hasIndustryYear)) {
					pstmt2.setString(3, "P");
					pstmt2.setString(1, "P" + actualDegreeCode);
				} else if ((i == actualLength) && (hasIndustryYear)){
					pstmt2.setString(3, Integer.toString(i-1));
					pstmt2.setString(1, i + actualDegreeCode);
				} else {
					pstmt2.setString(3, Integer.toString(i));
					pstmt2.setString(1, i + actualDegreeCode);
				}
				if (degreeLength == 1) {
					pstmt2.setInt(4, 4);
				} else {
					pstmt2.setInt(4, i);
				}
				pstmt2.executeUpdate();
			}

			pstmt3.setString(1, actualDegreeCode);
			pstmt3.setString(2, pDept);
			pstmt3.executeUpdate();
			
			con.commit();
		}
	}

	/**
	 * Function employed to remove degree courses.
	 */
	public static void removeDegree(String degreeCode) throws SQLException {
		String query = "DELETE FROM Degree WHERE DegreeCode = ?";

		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query)) {

			pstmt.setString(1, degreeCode);
			pstmt.executeUpdate();
		}
	}
	
	/**
	 * Function employed to assign a department or departments to a degree course.
	 */
	public static void assignDegreeDepartment(String DegreeCode, String Dept, int isPrimary) throws SQLException {
		String query = "INSERT INTO DegreeDepartments VALUES (?, ?, ?)";

		try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = con.prepareStatement(query)) {

			pstmt.setString(1, DegreeCode);
			pstmt.setString(2, Dept);
			pstmt.setInt(3, isPrimary);
			pstmt.executeUpdate();
		}
	}
	
	/**
	 * Function employed to add modules.
	 */
	public static void addModule(
			String moduleID, String dept, int credits, String timePeriod, String moduleTitle
	) throws SQLException {
		String query = "INSERT INTO Module VALUES (?, ?, ?, ?, ?);";
		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query)) {

			pstmt.setString(1, moduleID);
			pstmt.setString(2, dept);
			pstmt.setInt(3, credits);
			pstmt.setString(4, timePeriod); //Time period is the CHAR A/S/U/Y (Autumn,Spring,Summer,Year)
			pstmt.setString(5, moduleTitle);
			pstmt.executeUpdate();
		}
	}	
	
	/**
	 * Function employed to remove modules. This will delete all stored student grades for that module so must be used with caution
	 */
	public static void removeModule(String ModuleID) throws SQLException {
		String query = "DELETE FROM Module WHERE ModuleID = ?";
		try(Connection con = SQLFunctions.connectToDatabase();
			PreparedStatement pstmt = con.prepareStatement(query)) {

			pstmt.setString(1, ModuleID);
			pstmt.executeUpdate();
		}
	}
}

