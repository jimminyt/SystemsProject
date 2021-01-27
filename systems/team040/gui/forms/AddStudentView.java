package systems.team040.gui.forms;

import com.mysql.cj.xdevapi.SqlDataResult;
import systems.team040.functions.SQLFunctions;
import systems.team040.gui.components.MyTextField;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.PasswordView;
import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class AddStudentView extends InputPanel {
    private JPasswordField passwordField;

    public AddStudentView() {
        super(true);

        ArrayList<String> possibleTitles = new ArrayList<>(Arrays.asList("Mr", "Mrs"));

        try {
            ArrayList<String> degrees = SQLFunctions.queryToList(
                    "SELECT DegreeCode FROM Degree ORDER BY DegreeCode;",
                    rs -> rs.getString(1)
            );
            ArrayList<String> startDates = SQLFunctions.queryToList(
                    "SELECT StartDate FROM TermDates ORDER BY StartDate;",
                    rs -> rs.getString(1)
            );

            addComboBox("Start Date", "startdate", startDates);
            addComboBox("Degree", "degree", degrees);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Error fetching from DB: " + e.getCause().getMessage()
            );
        }

        addComboBox("Title", "title", possibleTitles);
        addStringInput("Forename", "forename", new MyTextField(".+"), JTextComponent::getText);
        addStringInput("Surname", "surname", new MyTextField(".+"), JTextComponent::getText);
        addStringInput("Tutor", "tutor", new MyTextField(".+"), JTextComponent::getText);

        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordField = new JPasswordField();

        passwordPanel.add(new JLabel("Password (leave blank to generate randomly)"), BorderLayout.PAGE_START);
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        centerPanel.add(passwordPanel);
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }
}
