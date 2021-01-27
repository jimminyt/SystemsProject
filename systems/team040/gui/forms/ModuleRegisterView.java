package systems.team040.gui.forms;

import systems.team040.functions.SQLFunctions;
import systems.team040.functions.TeacherFunctions;
import systems.team040.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class ModuleRegisterView extends MyPanel {
    private String studentID;
    private JComboBox<String> moduleSelector;
    private String latestStudentPeriod;
    private String degreeLevel;

    public ModuleRegisterView(String studentID) {
        super(true);

        centerPanel.setLayout(new BorderLayout());
        String query;

        try(Connection con = SQLFunctions.connectToDatabase()) {
            query = "SELECT StudentPeriod, DegreeLevel " +
                    "  FROM StudentPeriod " +
                    " WHERE StudentID = ?" +
                    " ORDER BY StudentPeriod DESC; ";

            try(PreparedStatement pstmt = con.prepareStatement(query)) {
                pstmt.setString(1, studentID);

                try(ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    latestStudentPeriod = rs.getString(1);
                    degreeLevel = rs.getString(2);
                }
            }

            int level = TeacherFunctions.getLevel(con, latestStudentPeriod);
            int creditsNeeded = level == 4 ? 180 : 120;
            int creditsTaken;

            query = "" +
                    "SELECT SUM(Credits) " +
                    "  FROM Grades" +
                    "  JOIN Module " +
                    "    ON Module.ModuleID = Grades.ModuleID " +
                    " WHERE Grades.StudentPeriod = ?; ";
            try(PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, latestStudentPeriod);

                try(ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    creditsTaken = rs.getInt(1);
                }
            }

            query = "" +
                    "SELECT Module.ModuleID " +
                    "  FROM DegreeModule " +
                    "  JOIN Module" +
                    "    ON DegreeModule.ModuleID = Module.ModuleID " +
                    " WHERE DegreeLevel = ?" +
                    "       AND isCore = 0" +
                    "       AND Credits <= ?; ";

            ArrayList<String> availModules = SQLFunctions.queryToList(
                    con, query,
                    rs -> rs.getString(1),
                    s -> s.setString(1, degreeLevel),
                    s -> s.setInt(2, creditsNeeded - creditsTaken)
            );

            query = "" +
                    "SELECT Grades.ModuleID " +
                    "  FROM Grades " +
                    "  JOIN DegreeModule " +
                    "    ON DegreeModule.ModuleID = Grades.ModuleID " +
                    " WHERE Grades.StudentPeriod = ? " +
                    "       AND DegreeLevel = ? " +
                    "       AND isCore = 0 " +
                    " ORDER BY Grades.ModuleID;";

            ArrayList<String> alreadyTakenModules = SQLFunctions.queryToList(
                    con, query,
                    rs -> rs.getString(1),
                    s -> s.setString(1, latestStudentPeriod),
                    s -> s.setString(2, degreeLevel)
            );

            ArrayList<String> moduleDropDown = new ArrayList<>();
            moduleDropDown.addAll(availModules);
            moduleDropDown.addAll(alreadyTakenModules);

            moduleSelector = new JComboBox<>();

            for (String availModule : moduleDropDown) {
                moduleSelector.addItem(availModule);
            }

            centerPanel.add(moduleSelector, BorderLayout.PAGE_END);


            query = "" +
                    "SELECT Module.* " +
                    "  FROM Grades " +
                    "  JOIN Module" +
                    "    ON Grades.ModuleID = Module.ModuleID" +
                    " WHERE Grades.StudentPeriod = ?; ";

            centerPanel.add(new JScrollPane(
                    new JTable(GUI.queryToTable(query, s -> s.setString(1, latestStudentPeriod)))
            ), BorderLayout.CENTER);



        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public String getSelectedModule() {
        return (String) moduleSelector.getSelectedItem();
    }

    public String getDegreeLevel() {
        return degreeLevel;
    }

    public String getStudentID() {
        return studentID;
    }

    public String getLatestStudentPeriod() {
        return latestStudentPeriod;
    }
}
