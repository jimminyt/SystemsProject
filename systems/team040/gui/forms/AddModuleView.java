package systems.team040.gui.forms;

import systems.team040.functions.SQLFunctions;
import systems.team040.gui.components.MyTextField;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.sql.SQLException;
import java.util.ArrayList;

public class AddModuleView extends InputPanel {
    public AddModuleView(boolean hasBackButton) {
        super(hasBackButton);
        ArrayList<String> departments;
        ArrayList<String> timePeriods;

        try {
            departments = SQLFunctions.queryToList(
                    "SELECT Dept FROM Department ORDER BY Dept;", rs -> rs.getString(1)
            );
            timePeriods = SQLFunctions.queryToList(
                    "SELECT TimePeriod from TimePeriods ORDER BY TimePeriod;", rs -> rs.getString(1)
            );

            addComboBox("Department", "dept", departments);
            addComboBox("Time Period", "timeperiod", timePeriods);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null, "DB Access error: " + e.getCause().getMessage()
            );

            return;
        }

        addStringInput(
                "Module Number Code",
                "moduleid",
                new MyTextField("\\d{4}"),
                JTextComponent::getText
        );

        MyTextField credits = new MyTextField("\\d{1,3}");
        addNumericInput("Credits", "credits", credits, mtf -> Integer.parseInt(mtf.getText()));

        addStringInput("Module Title", "moduletitle", new MyTextField(".+"), JTextComponent::getText);
    }
}
