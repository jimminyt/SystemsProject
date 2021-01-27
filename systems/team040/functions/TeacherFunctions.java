/*
 * TeacherFunctions.java
 * @author Matt Prestwich
 * @author Byron Slater
 * @author James Taylor
 */

/**
 * A class containing all the functions available to teacher accounts.
 */
package systems.team040.functions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class TeacherFunctions {
	/**
	 * Function employed to calculate students' weighted mean grades for a specific study period.
	 */
	public static float calculateWeightedMeanGrade(Connection con, String studentPeriod, char level) throws SQLException {
	    float weightedMean;
	    String query;
	    
        // this query sums up all the best of their grades and multiplies each of them by the credit
        // value of the module they scored that grade in, so when we divide through by the total credits
        // available we get the % they achieved
        query = "SELECT SUM(GREATEST(grade, resit) * credits)" +
                "  FROM (" +
                "		SELECT COALESCE(Grade, 0) as grade, LEAST(COALESCE(Resit, 0), 40) as resit, Credits" +
                "  		  FROM Grades" +
                "  	      JOIN Module" +
                "              ON Grades.ModuleID = Module.ModuleID" +
                "        WHERE StudentPeriod = ?" +
                ") as t1;";

        try(PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, studentPeriod);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                weightedMean = level == '4'
                        ? (float)rs.getInt(1) / 18000
                        : (float)rs.getInt(1) / 12000;

            }
        }
		return weightedMean;
	}

	/**
	 * Returns how many non-placement years there are in the degree of a given student
	 */
	private static int getDegreeLength(Connection con, String studentID) throws SQLException {
		String query = "" +
				"SELECT COUNT(*)" +
				"  FROM StudentPeriod " +
				"  JOIN DegreeLevel " +
				"    ON DegreeLevel.DegreeLevel = StudentPeriod.DegreeLevel " +
				" WHERE DegreeLevel.Level <> 'P'" +
				"       AND StudentID = ?; ";

		try(PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, studentID);

			try(ResultSet rs = pstmt.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}

		}
	}

	/**
	 * Calculates a grade for a student based on their entire performance at university
	 */
	public static Grade gradeDegree(String studentID, boolean passedLast) throws SQLException {
	    try(Connection con = SQLFunctions.connectToDatabase()) {
	    	int lengthOfDegree;
	    	float weightedMean;
	    	boolean isPostGrad;

	    	lengthOfDegree = getDegreeLength(con, studentID);

	    	switch(lengthOfDegree) {
				// one year masters
	    		case 1:
					weightedMean = 100 * calculateWeightedMeanGrade(con, "A" + studentID, '4');
					if(weightedMean < 49.5) {
						return Grade.Fail;
					} else if(weightedMean < 59.5) {
						return Grade.Pass;
					} else if(weightedMean < 69.5) {
						return Grade.Merit;
					} else {
						return Grade.Distinction;
					}
				// bsc
				case 3:
				    // if we didnt pass the last module then we just fail
				    if(!passedLast) {
				    	return Grade.Fail;
					}

				    String query = "" +
							"SELECT StudentPeriod " +
							"  FROM StudentPeriod " +
							"  JOIN DegreeLevel " +
							"       ON StudentPeriod.DegreeLevel = DegreeLevel.DegreeLevel " +
							" WHERE DegreeLevel.Level <> 'p' " +
							"       AND StudentID = ? " +
							" ORDER BY YearTaken; ";

					ArrayList<String> studentPeriods = SQLFunctions.queryToList(
							con, query, rs -> rs.getString(1), s -> s.setString(1, studentID)
					);

					float totalMean = 0;

					for(int i = 1; i < 3; ++i) {
						String studentPeriod = studentPeriods.get(i);
						totalMean += (Math.min(i, 2) * calculateWeightedMeanGrade(con, studentPeriod, '2'));
					}
					weightedMean = 100 * totalMean / 3;

					// if last year was a retake we can only try for a pass
					if(RegistrarFunctions.isRetake(con, studentPeriods.get(2))) {
						if(weightedMean >= 44.5) {
							return Grade.Pass;
						} else {
							return Grade.Fail;
						}
					}


					if (weightedMean < 39.5) {
						return Grade.Fail;
					} else if (weightedMean < 44.5) {
						return Grade.Pass;
					} else if (weightedMean < 49.5) {
						return Grade.ThirdClass;
					} else if (weightedMean < 59.5) {
						return Grade.LowerSecond;
					} else if (weightedMean < 69.5) {
						return Grade.UpperSecond;
					} else {
						return Grade.FirstClass;
					}

				case 4:
					// if we didn't pass the last grade then we get the equiv bsc
					if(!passedLast) {
						return Grade.EquivBSC;
					}

					query = "" +
							"SELECT StudentPeriod " +
							"  FROM StudentPeriod " +
							"  JOIN DegreeLevel " +
							"       ON StudentPeriod.DegreeLevel = DegreeLevel.DegreeLevel " +
							" WHERE DegreeLevel.Level <> 'p' " +
							"       AND StudentID = ? " +
							" ORDER BY YearTaken; ";

					studentPeriods = SQLFunctions.queryToList(
							con, query, rs -> rs.getString(1), s -> s.setString(1, studentID)
					);

					totalMean = 0;

					for(int i = 1; i < 4; ++i) {
						String studentPeriod = studentPeriods.get(i);
						totalMean += (Math.min(i, 2) * calculateWeightedMeanGrade(con, studentPeriod, i == 3 ? '4' : '2'));
					}
					weightedMean = 100 * totalMean / 5;

					if(weightedMean < 49.5) {
						return Grade.Fail;
					} else if(weightedMean < 59.5) {
						return Grade.LowerSecond;
					} else if(weightedMean < 69.5) {
						return Grade.UpperSecond;
					} else {
						return Grade.FirstClass;
					}
					default:
						throw new IllegalStateException("Wrong number of degree levels found");
			}
		}
	}

	/**
	 * Gets the character level for a given studentperiod
	 */
	public static char getLevel(Connection con, String studentPeriod) throws SQLException {
		String query = "" +
				"SELECT Level" +
				"  FROM DegreeLevel" +
				"  JOIN StudentPeriod" +
				"       ON DegreeLevel.DegreeLevel = StudentPeriod.DegreeLevel" +
				" WHERE StudentPeriod = ?";

		try(PreparedStatement pstmt = con.prepareStatement(query)) {
			pstmt.setString(1, studentPeriod);
			try(ResultSet rs = pstmt.executeQuery()) {
				rs.next();
				return rs.getString("Level").charAt(0);
			}
		}
	}

	public static boolean calculateIfPassed(String studentPeriod) throws SQLException {
	    try(Connection con = SQLFunctions.connectToDatabase()) {
	    	return calculateIfPassed(con, studentPeriod);
		}
	}

	/**
	 * Function employed to calculate whether a student has passed their period of study.
	 * @throws SQLException
	 */
	public static boolean calculateIfPassed(Connection con, String studentPeriod) throws SQLException {
	    String query;
	    int passPercent, level, failableCredits, creditsTaken;

        level = getLevel(con, studentPeriod);

        // set constants based on level of study
        if(level == 4) {
            passPercent = 50;
            failableCredits = 15;
            creditsTaken = 180;
        } else {
            passPercent = 40;
            failableCredits = 20;
            creditsTaken = 120;
        }

        // This nifty little query calculates how many modules have been failed, with any that hold more than
        // the maximum number of failable credits counting as 2 (and thus failing the degree) and, at the same time
        // calculates the weighted mean.
        query = "" +
                "SELECT SUM(BestGrade * Credits) / ? AS WeightedMean											" +
                "     , SUM(FailedCost * IF(BestGrade < ?, IF(BestGrade < ?, 2, 1), 0)) AS FailedModules		" +
                "  FROM (																						" +
                "		SELECT GREATEST(COALESCE(Grade, 0)														" +
                " 		     , LEAST(COALESCE(Resit, 0), ?)) as BestGrade										" +
                "		     , Credits																			" +
                "		     , IF(Credits > ?, 2, 1) as FailedCost												" +
                "		  FROM Grades																			" +
                "		  JOIN Module																			" +
                " 		       ON Grades.ModuleID = Module.ModuleID												" +
                "		 WHERE Grades.StudentPeriod = ?															" +
                ") as t1;																						";


        try(PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, creditsTaken);
            pstmt.setInt(2, passPercent);
            pstmt.setInt(3, passPercent - 10);
            pstmt.setInt(4, passPercent);
            pstmt.setInt(5, failableCredits);
            pstmt.setString(6, studentPeriod);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                float weightedMean = rs.getFloat("WeightedMean");
                int failedModules = rs.getInt("FailedModules");

                return !(weightedMean < passPercent) && failedModules <= 1;
            }
		}
	}

	/**
	 * Simple enum to show possible return values for the progress function
	 */
	public enum ProgressReturn {
		Failed, Progressed, NotGraded, NotEnoughCredits, PassedAndFinished, FailedAndFinished;
	}

	/**
	 * Function employed to register from, graduate from, or fail a period of study.
     *
	 * Could/should potentially be split into more modular functions, e.g. one for registering a student
	 * for a new year and one for registering a student for resitting a year but we don't need to use the code
	 * elsewhere so it's all in this big beast
	 * @throws SQLException
	 */
	public static ProgressReturn progressToNextPeriod(String studentPeriod) throws SQLException {
		char currentPeriodID, nextPeriodID;
		String degreeCode, degreeLevel;
		int creditsBeingTaken;
		int currentYearTaken;
		String query;
		String currentDate, nextDate;
		String studentID;
		String nextDegreeLevel;
		char level;


		try(Connection con = SQLFunctions.connectToDatabase()) {
		    con.setAutoCommit(false);

		    // First we'll check if there are any modules they haven't received a grade for and quit out
			query = "" +
					"SELECT COUNT(*)" +
					"  FROM Grades " +
					"  JOIN Module" +
					"    ON Grades.ModuleID = Module.ModuleID " +
					" WHERE StudentPeriod = ? " +
					"       AND Grade IS NULL " +
					"       AND Resit IS NULL; ";

			try(PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, studentPeriod);
				try(ResultSet rs = pstmt.executeQuery()) {
					rs.next();
					if(rs.getInt(1) > 0) {
						return ProgressReturn.NotGraded;
					}
				}
			}

			// Next we'll get the total credits this person is taking
			query = "" +
					"SELECT SUM(Credits)" +
					"  FROM Grades" +
					"  JOIN Module" +
					"       ON Module.ModuleID = Grades.ModuleID " +
					" WHERE Grades.StudentPeriod = ?; ";

			try(PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, studentPeriod);
				try(ResultSet rs = pstmt.executeQuery()) {
					rs.next();
					creditsBeingTaken = rs.getInt(1);
				}
			}

			// get information on next studentperiod etc.
			query = "" +
					"SELECT *" +
					"  FROM StudentPeriod" +
					"  JOIN DegreeLevel" +
					"       ON StudentPeriod.DegreeLevel = DegreeLevel.DegreeLevel" +
					" WHERE StudentPeriod = ?;";
			try(PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, studentPeriod);

				// get all the information about the current degree level
				try (ResultSet rs = pstmt.executeQuery()) {
					rs.next();

					level = rs.getString("Level").charAt(0);
					currentPeriodID = rs.getString("PeriodID").charAt(0);
					nextPeriodID = (char) (currentPeriodID + 1);
					currentDate = rs.getString("StartDate");
					degreeCode = rs.getString("DegreeCode");
					studentID = rs.getString("StudentID");
					degreeLevel = rs.getString("DegreeLevel");
					currentYearTaken = rs.getInt("YearTaken");

					if(level == '4' && creditsBeingTaken != 180 || creditsBeingTaken != 120) {
						return ProgressReturn.NotEnoughCredits;
					}
				}
			}

			// get the next startdate
			query = "SELECT StartDate FROM TermDates WHERE StartDate > ? ORDER BY StartDate;";
			try(PreparedStatement pstmt = con.prepareStatement(query)) {
				pstmt.setString(1, currentDate);

				try(ResultSet rs = pstmt.executeQuery()) {
					rs.next();

					nextDate = rs.getString("StartDate");
				}
			}

			// if we passed we set up the next year
			if(calculateIfPassed(con, studentPeriod)) {

				// get the next degreeLevel
				query = "SELECT DegreeLevel FROM DegreeLevel WHERE YearTaken = ? AND DegreeCode = ?";
				try(PreparedStatement pstmt = con.prepareStatement(query)) {
					pstmt.setInt(1, currentYearTaken + 1);
					pstmt.setString(2, degreeCode);
					try(ResultSet rs = pstmt.executeQuery()) {
					    if(!rs.next()) {
					    	return ProgressReturn.PassedAndFinished;
						}
                        nextDegreeLevel = rs.getString("DegreeLevel");

						System.out.println(nextDegreeLevel);
					}
				}


				// create the new studentperiod
				query = "INSERT INTO StudentPeriod(StudentPeriod, PeriodID, DegreeLevel, StudentID, StartDate) " +
						"VALUES (?, ?, ?, ?, ?);";
				try(PreparedStatement pstmt = con.prepareStatement(query)) {
					pstmt.setString(1, nextPeriodID + studentID);
					pstmt.setString(2, String.valueOf(nextPeriodID));
					pstmt.setString(3, nextDegreeLevel);
					pstmt.setString(4, studentID);
					pstmt.setString(5, nextDate);

					pstmt.executeUpdate();
				}

				// add the new grades
				query = "" +
						"INSERT INTO Grades (StudentPeriod, ModuleID)" +
                        "SELECT ?, ModuleID " +
                        "  FROM DegreeModule " +
                        " WHERE DegreeLevel = ? AND isCore = 1;";

				try(PreparedStatement pstmt = con.prepareStatement(query)) {
					pstmt.setString(1, nextPeriodID + studentID);
					pstmt.setString(2, nextDegreeLevel);
					pstmt.executeUpdate();
				}

				con.commit();
				return ProgressReturn.Progressed;

			} else {
				if(RegistrarFunctions.isRetake(con, studentPeriod)) {
					return ProgressReturn.FailedAndFinished;
				}

				// we didn't pass so we add a new studentperiod at the same level and carry forward grades
				// create new studentPeriod at same level
				query = "" +
						"INSERT INTO StudentPeriod(StudentPeriod, PeriodID, DegreeLevel, StudentID, StartDate)" +
						"VALUES (?, ?, ?, ?, ?);";

				try(PreparedStatement pstmt = con.prepareStatement(query)) {
					pstmt.setString(1, nextPeriodID + studentID);
					pstmt.setString(2, String.valueOf(nextPeriodID));
					pstmt.setString(3, degreeLevel);
					pstmt.setString(4, studentID);
					pstmt.setString(5, nextDate);

					pstmt.executeUpdate();
				}

				int passGrade = level == 4 ? 50 : 40;

				// insert old grade if it passed or null if it didn't
				query = "" +
						"INSERT INTO Grades (StudentPeriod, Grade, ModuleID) " +
						"SELECT StudentPeriod, IF(BestGrade >= ?, BestGrade, NULL), ModuleID " +
						"  FROM (" +
                        "		SELECT ? AS StudentPeriod " +
						"		     , GREATEST(COALESCE(Grade, 0),LEAST(COALESCE(Resit, 0), ?)) AS BestGrade " +
						"		     , ModuleID AS ModuleID" +
						"		  FROM Grades " +
						"		 WHERE StudentPeriod = ? " +
						") as t1;";

				try(PreparedStatement pstmt = con.prepareStatement(query)) {
					pstmt.setInt(1, passGrade);
					pstmt.setString(2, nextPeriodID + studentID);
					pstmt.setInt(3, passGrade);
					pstmt.setString(4, studentPeriod);

					pstmt.executeUpdate();
				}

				con.commit();
				return ProgressReturn.Failed;
			}
		}
	}
}
