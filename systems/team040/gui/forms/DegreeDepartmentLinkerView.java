package systems.team040.gui.forms;

import jdk.internal.util.xml.impl.Input;
import systems.team040.functions.SQLFunctions;
import systems.team040.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class DegreeDepartmentLinkerView extends MyPanel {
    private final String dept;
    private JComboBox<String> degreeSelector;

    public DegreeDepartmentLinkerView(String dept) {
        super(true);
        this.dept = dept;

        centerPanel.setLayout(new BorderLayout());

        try {
            String query = "" +
                    "SELECT Degree.* " +
                    "  FROM Degree " +
                    "  JOIN DegreeDepartments " +
                    "    ON Degree.DegreeCode = DegreeDepartments.DegreeCode " +
                    " WHERE Dept = ?;";

            centerPanel.add(new JScrollPane(new JTable(GUI.queryToTable(query, s -> s.setString(1, dept)))));

            query = "SELECT DegreeCode FROM Degree ORDER BY DegreeCode;";
            ArrayList<String> degrees = SQLFunctions.queryToList(query, rs -> rs.getString("DegreeCode"));

            degreeSelector = new JComboBox<>();
            for(String degree : degrees) {
                degreeSelector.addItem(degree);
            }

            centerPanel.add(degreeSelector, BorderLayout.PAGE_END);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getDept() {
        return dept;
    }

    public String getSelectedDegree() {
        return (String)degreeSelector.getSelectedItem();
    }
}
