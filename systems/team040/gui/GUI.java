package systems.team040.gui;

import systems.team040.functions.CheckedConsumer;
import systems.team040.functions.SQLFunctions;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.xml.transform.Result;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

/**
 * Holds the general purpose functions/constants for the GUI
 */
public class GUI {
    // screensize and default input/button sizes
    public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    public static final Dimension inputSize = new Dimension(100, 24);
    public static final Dimension buttonSize = new Dimension(100, 24);

    // Converts a query and paramters to a JTable
    @SafeVarargs
    public static TableModel queryToTable(
            String query, CheckedConsumer<PreparedStatement, SQLException>... parameters
    ) throws SQLException {

        Vector<String> columnNames = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();

        try(Connection conn = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = conn.prepareStatement(query)) {

            for(CheckedConsumer<PreparedStatement, SQLException> param : parameters) {
                param.accept(pstmt);
            }

            System.out.println(pstmt);

            try(ResultSet rs = pstmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columns = metaData.getColumnCount();
                for(int i = 1; i <= columns; ++i) {
                    columnNames.add(metaData.getColumnName(i));
                }

                while(rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for(int i = 1; i <= columns; ++i) {
                        row.add(rs.getObject(i));
                    }

                    data.add(row);
                }
            }
        }

        return new DefaultTableModel(data, columnNames);
    }
}
