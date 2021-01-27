package systems.team040.gui.forms;

import systems.team040.functions.SQLFunctions;
import systems.team040.gui.GUI;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class LinkModuleView extends MyPanel {
    private String degreeLevel;
    private JComboBox<String> moduleSelector;
    public int creditsAvailable;


    public LinkModuleView(String degreeLevel) {
        super(true);
        this.degreeLevel = degreeLevel;

        char level = degreeLevel.charAt(0);
        int creditsNeeded = level == '4' ? 180 : 120;
        int currentCredits;

        centerPanel.setLayout(new BorderLayout());


        try {
            String query = "" +
                    "SELECT SUM(Credits)" +
                    "  FROM Module" +
                    "  JOIN DegreeModule" +
                    "    ON Module.ModuleID = DegreeModule.ModuleID" +
                    " WHERE isCore " +
                    "       AND DegreeLevel = ?; ";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, degreeLevel);
                try(ResultSet rs = pstmt.executeQuery()) {

                    rs.next();
                    currentCredits = rs.getInt(1);
                }
            }

            creditsAvailable = creditsNeeded - currentCredits;


            query = "" +
                    "SELECT Module.ModuleID" +
                    "     , isCore " +
                    "     , Credits " +
                    "  FROM DegreeModule " +
                    "  JOIN Module " +
                    "    ON Module.ModuleID = DegreeModule.ModuleID " +
                    " WHERE DegreeLevel = ?;";
            centerPanel.add(new JScrollPane(new JTable(GUI.queryToTable(query, s -> s.setString(1, degreeLevel)))));

            query = "SELECT ModuleID FROM Module ORDER BY ModuleID;";
            ArrayList<String> modules = SQLFunctions.queryToList(query, rs -> rs.getString("ModuleID"));

            moduleSelector = new JComboBox<>();
            for(String module : modules) {
                moduleSelector.addItem(module);
            }

            centerPanel.add(moduleSelector, BorderLayout.PAGE_END);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getDegreeLevel() {
        return degreeLevel;
    }

    public String getSelectedModule() {
        return (String)moduleSelector.getSelectedItem();
    }
}
