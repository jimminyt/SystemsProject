package systems.team040.gui.forms;

import systems.team040.functions.RegistrarFunctions;
import systems.team040.functions.SQLFunctions;
import systems.team040.gui.GUI;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GradeStudentView extends MyPanel {
    private DefaultTableModel model;
    private JTable table;
    private String studentID;
    private String latestStudentPeriod;
    private String query;
    public final int GRADE_COLUMN;
    public final int RESIT_COLUMN;
    public final boolean IS_RESIT;

    public GradeStudentView(String studentID) {
        super(true);
        boolean IS_RESIT1;

        query = "" +
                "SELECT MAX(StudentPeriod) FROM StudentPeriod WHERE StudentID = ?; ";

        try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setString(1, studentID);

            try(ResultSet rs = pstmt.executeQuery()) {
                rs.next();
                latestStudentPeriod = rs.getString(1);
            }

            IS_RESIT1 = RegistrarFunctions.isRetake(con, latestStudentPeriod);

        } catch (SQLException e) {
            e.printStackTrace();
            IS_RESIT1 = false;
        }

        IS_RESIT = IS_RESIT1;
        this.studentID = studentID;


        if(!IS_RESIT) {
            query = "" +
                    "SELECT Module.ModuleID AS 'Module' " +
                    "     , ModuleTitle as 'Name' " +
                    "     , Credits as 'Credits' " +
                    "     , Grades.StudentPeriod AS Period " +
                    "     , Grade AS 'Initial Grade' " +
                    "     , Resit AS 'Resit Grade' " +
                    "  FROM Grades " +
                    "  JOIN StudentPeriod " +
                    "       ON StudentPeriod.StudentPeriod = Grades.StudentPeriod " +
                    "  JOIN Module " +
                    "       ON Module.ModuleID = Grades.ModuleID " +
                    " WHERE StudentPeriod.StudentPeriod = ?;";

            GRADE_COLUMN = 4;
            RESIT_COLUMN = 5;
        } else {
            query = "" +
                    "SELECT Module.ModuleID AS 'Module' " +
                    "     , ModuleTitle as 'Name' " +
                    "     , Credits as 'Credits' " +
                    "     , Grades.StudentPeriod AS Period " +
                    "     , Resit AS 'Grade' " +
                    "  FROM Grades " +
                    "  JOIN StudentPeriod " +
                    "       ON StudentPeriod.StudentPeriod = Grades.StudentPeriod " +
                    "  JOIN Module " +
                    "       ON Module.ModuleID = Grades.ModuleID " +
                    " WHERE StudentPeriod.StudentPeriod = ?" +
                    "       AND Grade IS NULL; ";
            RESIT_COLUMN = 4;
            GRADE_COLUMN = -1;

        }

        try {
            model = (DefaultTableModel) GUI.queryToTable(query, s -> s.setString(1, latestStudentPeriod));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't get grades: " + e.getMessage()
            );
            e.printStackTrace();
        }

        table = new JTable(model);


        JScrollPane scrollPane = new JScrollPane(table);
        centerPanel.add(scrollPane);
    }

    public DefaultTableModel getModel() {
        model.fireTableDataChanged();
        return model;
    }
}
