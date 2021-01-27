package systems.team040.application;

import systems.team040.functions.*;
import systems.team040.gui.GUI;
import systems.team040.gui.components.MyTextField;
import systems.team040.gui.forms.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.function.Supplier;

import static systems.team040.functions.AccountType.Admin;

/**
 * Was just supposed to contain the base application logic but now contains a stupid amount of code
 * which should almost definitely be split into many many different files
 */
public class AppController {
    private JFrame frame;
    private Container contentPane;

    private AppController() {
        EventQueue.invokeLater(() -> {
            frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            contentPane = frame.getContentPane();

            frame.setResizable(false);
            frame.setSize(1000, 600);
            frame.setLocation(
                    GUI.screenSize.width / 2 - 500,
                    GUI.screenSize.height / 2 - 300
            );

            frame.setVisible(true);

            changeView(createLoginScreen());
        });
    }

    /**
     * Clears the frame and puts the new jpanel on it
     */
    private void changeView(JPanel newPanel) {
        contentPane.removeAll();
        contentPane.add(newPanel);
        frame.revalidate();
        frame.repaint();
    }

    /**
     * Creates the login screen
     */
    private JPanel createLoginScreen() {
        LoginView view = new LoginView();

        view.addButton("Login").addActionListener(e -> {
            JPanel nextPage = tryLogin(view.getEnteredUsername(), view.getEnteredPassword());
            if(nextPage == null) {
                JOptionPane.showMessageDialog(null, "Invalid credentials input");
            } else {
                changeView(nextPage);
            }
        });

        return view;
    }

    /**
     * Debug function that lets us login to different account types
     * by providing the appropriate username, ignoring password.
     * delegates to actual login function if not used
     */
    private JPanel tryLogin(String username, char[] password) {
        String query = "SELECT Password, AccountType FROM UserAccount WHERE Username = ?;";

        try(Connection con = SQLFunctions.connectToDatabase();
            PreparedStatement pstmt = con.prepareStatement(query)) {

            pstmt.setString(1, username);
            try(ResultSet rs = pstmt.executeQuery()) {
                // no password found, username invalid, do nothing
                if(!rs.next()) {
                    return null;
                }

                String stored = rs.getString("Password");

                if(Hasher.validatePassword(password, stored)) {
                    System.out.println("Real account entered");
                    AccountType at = AccountType.fromInt(rs.getInt("AccountType"));

                    LoggedInUser.login(username, at);

                    switch(at) {
                        case Registrar:
                            return registrarHome();
                        case Teacher:
                            return createTeacherView();
                        case Student:
                            String studentID = StudentFunctions.getIDFromUsername(username);
                            return viewStudent(studentID);
                        case Admin:
                            return createAdminSwitchboard();
                    }
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Something went wrong"
            );
            e.printStackTrace();
        }

        return null;
    }

    /**
     * logs out and removes loggedinuser variable
     */
    private void logout() {
        LoggedInUser.logout();
        changeView(createLoginScreen());
    }

    private JPanel createTeacherView() {
        String query = "SELECT * FROM Student";
        MyPanel view = createInfoPanel(query, false);

        view.addButton("Log out").addActionListener(e -> logout());
        view.addButton("Select Student").addActionListener(e -> changeView(selectStudent()));

        return view;
    }

    /**
     * creates the switchboard for the admin
     */
    private JPanel createAdminSwitchboard() {
        return createGenericSwitchboard(
                new Pair<>("View/Edit users", this::viewUsers),
                new Pair<>("Add Student", this::addStudent),
                new Pair<>("View/Edit modules", this::viewModules),
                new Pair<>("View/Edit degrees", this::viewDegrees),
                new Pair<>("View/Edit departments", this::viewDepartments),
                new Pair<>("Link modules", this::linkModules),
                new Pair<>("Link degrees/departments", this::selectDegree)
        );
    }

    private JPanel selectDegree() {
        ArrayList<String> depts = null;
        try {
            depts = SQLFunctions.queryToList("SELECT Dept FROM Department;", rs -> rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));

        view.addComboBox("Department", "dept", depts);
        view.addButton("Select Degree").addActionListener(e -> changeView(linkDegrees(view.getString("dept"))));

        return view;
    }

    private JPanel linkDegrees(String dept) {
        DegreeDepartmentLinkerView view = new DegreeDepartmentLinkerView(dept);

        view.addButton("Link").addActionListener(e -> {
            String degreeCode = view.getSelectedDegree();
            String query = "INSERT INTO DegreeDepartments VALUES(?, ?, 0); ";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, degreeCode);
                pstmt.setString(2, dept);

                pstmt.executeUpdate();

            } catch (SQLIntegrityConstraintViolationException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Degree is already linked with this module"
                );
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong"
                );
            }

            changeView(linkDegrees(dept));
        });

        view.addButton("Unlink").addActionListener(e -> {
            String query = "" +
                    "DELETE FROM DegreeDepartments WHERE DegreeCode = ? AND Dept = ? AND NOT LeadDepartment;";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, view.getSelectedDegree());
                pstmt.setString(2, dept);

                pstmt.executeUpdate();
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null, "Something went wrong"
                );
            }

            changeView(linkDegrees(dept));
        });

        view.getBackButton().addActionListener(e -> changeView(selectDegree()));

        return view;
    }

    private MyPanel viewUsers() {
        String query = "SELECT * FROM UserAccount;";
        MyPanel view = createInfoPanel(query, true);

        view.addButton("Add User").addActionListener(e -> changeView(addUser()));
        view.addButton("Delete User").addActionListener(e -> changeView(deleteUser()));
        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));

        return view;
    }

    private void displayErrors(InputPanel panel) {
        JOptionPane.showMessageDialog(null, panel.getErrorMessage());
    }

    private MyPanel addUser() {
        AddUserView view = new AddUserView(true);
        view.getBackButton().addActionListener(e -> changeView(viewUsers()));
        view.addButton("Add").addActionListener(e -> {
            if(!view.isOkay()) {
                displayErrors(view);
                return;
            }

            if(view.getAccountType() == 3) {
                JOptionPane.showMessageDialog(
                        null,
                        "Sorry, can't create students from this page, please use " +
                                "create students page"
                );

                return;
            }

            try {
                AdminFunctions.createAccount(
                        view.getString("username"),
                        view.getPassword(),
                        view.getAccountType()
                );
                changeView(viewUsers());
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't create account: " + e1.getCause().getMessage()
                );
            }

        });
        return view;
    }

    private MyPanel deleteUser() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewUsers()));

        try {
            ArrayList<String> usernames = SQLFunctions.queryToList(
                    "SELECT Username FROM UserAccount ORDER BY Username; ",
                    rs -> rs.getString(1)
            );
            view.addComboBox("Username", "username", usernames);
        } catch (SQLException e) {

            JOptionPane.showMessageDialog(null,
                    "Couldn't fetch usernames: " + e.getCause().getMessage()
            );
        }


        view.addButton("Delete").addActionListener(e -> {
            String userToDelete = view.getString("username");

            if (userToDelete.equals(LoggedInUser.getInstance().getUsername())) {
                JOptionPane.showMessageDialog(
                        null,
                        "Can't delete your own user."
                );
                return;
            }

            try {
                AdminFunctions.removeUser(view.getString("username"));
                changeView(viewUsers());
            } catch (SQLException e1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't remove user: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }

    private MyPanel viewModules() {
        String query = "SELECT * FROM Module;";
        MyPanel view = createInfoPanel(query, true);

        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));
        view.addButton("Add module").addActionListener(e -> changeView(addModule()));
        view.addButton("Delete module").addActionListener(e -> changeView(deleteModule()));

        return view;
    }

    private MyPanel addModule() {
        AddModuleView view = new AddModuleView(true);
        view.getBackButton().addActionListener(e -> changeView(viewModules()));
        view.addButton("Add").addActionListener(
                e -> {
                    try {
                        AdminFunctions.addModule(
                                view.getString("dept") + view.getString("moduleid"),
                                view.getString("dept"),
                                view.getInteger("credits"),
                                view.getString("timeperiod"),
                                view.getString("moduletitle")
                        );

                        changeView(viewModules());
                    } catch (SQLIntegrityConstraintViolationException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                                null,
                                view.getString("dept") + view.getString("moduleid")
                                + " is already a module on our system. Please choose another number."
                        );

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                                null,
                                "Error saving to DB: " + ex.getCause().getMessage()
                        );
                    }
                }
        );
        return view;
    }

    private MyPanel deleteModule() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewModules()));

        try {
            ArrayList<String> moduleIDs = SQLFunctions.queryToList(
                    "SELECT ModuleID FROM Module ORDER BY ModuleID;",
                    rs -> rs.getString("ModuleID")
            );

            view.addComboBox("Module ID", "moduleid", moduleIDs);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't get module IDs: " + e.getCause().getMessage()
            );
            e.printStackTrace();
        }

        view.addButton("Delete").addActionListener(e -> {
            try {
                AdminFunctions.removeModule(view.getString("moduleid"));
                changeView(viewModules());
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Error deleting module: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }

    private MyPanel viewDegrees() {
        String query = "SELECT * FROM Degree;";
        MyPanel view = createInfoPanel(query, true);

        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));
        view.addButton("Add degree").addActionListener(e -> changeView(addDegree()));
        view.addButton("Delete degree").addActionListener(e -> changeView(deleteDegree()));
        return view;
    }

    private MyPanel addDegree() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewDegrees()));

        ArrayList<String> departments = null;
        try {
            departments = SQLFunctions.queryToList("SELECT Dept FROM Department ORDER BY Dept;", rs -> rs.getString(1));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null, "Something went wrong"
            );
            e.printStackTrace();
        }
        view.addComboBox("Department", "dept", departments);


        ArrayList<String> bools = new ArrayList<>();
        bools.add("False");
        bools.add("True");

        view.addComboBox("Has Placement?", "placement", bools);

        view.addStringInput("Name", "name", new MyTextField(".+"), JTextField::getText);

        view.addStringInput(
                "Numeric Code",
                "numcode",
                new MyTextField("\\d{2}"),
                JTextField::getText
        );


        view.addNumericInput(
                "Degree Length (1/3/4), excludes year in industry if selected",
                "dlen",
                new MyTextField("[134]"),
                mtf -> Integer.parseInt(mtf.getText())
        );

        view.addButton("Add").addActionListener(e -> {
            String numCode = view.getString("numcode");
            boolean hasPlacement = view.getString("placement").equals("True");
            String dept = view.getString("dept");
            String name = view.getString("name");
            int dlen = view.getInteger("dlen");

            String degreeCode = dept + (dlen == 1 ? "P" : "U") + numCode;

            if(dlen == 1 && hasPlacement) {
                JOptionPane.showMessageDialog(
                        null, "Can't have a placement year on a 1 year degree!"
                );
            }

            try {
                AdminFunctions.addDegree(degreeCode, name, dlen, hasPlacement, dept);
                changeView(viewDegrees());
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't save degree: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }

    private MyPanel deleteDegree() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewDegrees()));

        try {
            ArrayList<String> degreeCodes =
                    SQLFunctions.queryToList(
                            "SELECT DegreeCode FROM Degree ORDER BY DegreeCode;",
                            rs -> rs.getString(1)
                    );

            view.addComboBox("Degree Code", "dcode", degreeCodes);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't get degree codes: " + e.getCause().getMessage()
            );
        }

        view.addButton("Delete").addActionListener(e -> {
            String dcode = view.getString("dcode");
            try {
                AdminFunctions.removeDegree(dcode);
                changeView(viewDegrees());
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't delete module: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }
    private MyPanel viewDepartments() {
        String query = "SELECT * FROM Department;";
        MyPanel view = createInfoPanel(query, true);

        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));
        view.addButton("Add department").addActionListener(e -> changeView(addDepartment()));
        view.addButton("Delete department").addActionListener(e -> changeView(deleteDepartment()));
        return view;
    }

    private MyPanel addDepartment() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewDepartments()));

        view.addStringInput(
                "Dept Code", "dcode",
                new MyTextField("[A-Z]{3}"), JTextComponent::getText
        );

        view.addStringInput(
                "Dept Name", "dname",
                new MyTextField(".+"), JTextComponent::getText
        );

        view.addButton("Add").addActionListener(e -> {
            String dcode = view.getString("dcode");
            String dname = view.getString("dname");
            try {
                AdminFunctions.addDepartment(dcode, dname);
                changeView(viewDepartments());
            } catch (SQLIntegrityConstraintViolationException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Department already exists with code \"" + dcode + "\""
                );
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't create department: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }

    private MyPanel deleteDepartment() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(viewDepartments()));

        try {
            ArrayList<String> departments = SQLFunctions.queryToList(
                    "SELECT Dept FROM Department ORDER BY Dept;",
                    rs -> rs.getString(1)
            );

            view.addComboBox("Department Code", "dept", departments);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't get departments: " + e.getCause().getMessage()
            );
        }


        view.addButton("Delete").addActionListener(e -> {
            String dept = view.getString("dept");
            try {
                AdminFunctions.removeDepartment(dept);
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't delete department: " + e1.getCause().getMessage()
                );
            }
        });
        return view;
    }
    private MyPanel linkModules() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(createAdminSwitchboard()));

        try {
            String query = "SELECT DegreeLevel FROM DegreeLevel ORDER BY SUBSTRING(2, DegreeLevel);";
            ArrayList<String> degrees = SQLFunctions.queryToList(query, rs -> rs.getString("DegreeLevel"));
            view.addComboBox("Degree", "degree", degrees);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Something went wrong getting degrees"
            );
            e.printStackTrace();
        }

        view.addButton("Go to DegreeLevel").addActionListener(e -> changeView(addModulesToDegree(view.getString("degree"))));
        return view;
    }

    private JPanel addModulesToDegree(String degree) {
        LinkModuleView view = new LinkModuleView(degree);

        view.getBackButton().addActionListener(e -> changeView(linkModules()));

        view.addButton("Add Module").addActionListener(e -> {
            String q = "SELECT Credits FROM Module WHERE ModuleID = ?; ";
            int credits = 180;

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(q)) {

                pstmt.setString(1, view.getSelectedModule());
                try(ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    credits = rs.getInt(1);
                }

            } catch (SQLException e1) {
                e1.printStackTrace();
            }

            boolean isCore;

            if(credits > view.creditsAvailable) {
                JOptionPane.showMessageDialog(
                        null,
                        "Too many core credits, adding as optional module"
                );

                isCore = false;
            } else {
                isCore = JOptionPane.showConfirmDialog(
                        null,
                        "Should this be a core module?"
                ) == JOptionPane.YES_OPTION;
            }

            String query = "INSERT INTO DegreeModule VALUES (?, ?, ?);";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, view.getSelectedModule());
                pstmt.setString(2, view.getDegreeLevel());
                pstmt.setInt(3, isCore ? 1 : 0);

                pstmt.executeUpdate();
                changeView(addModulesToDegree(view.getDegreeLevel()));
            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong"
                );
            }
        });

        view.addButton("Unlink Module").addActionListener(e -> {
            String query = "DELETE FROM DegreeModule WHERE DegreeLevel = ? AND ModuleID = ?;";
            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, view.getDegreeLevel());
                pstmt.setString(2, view.getSelectedModule());

                pstmt.executeUpdate();

                changeView(addModulesToDegree(view.getDegreeLevel()));

            } catch (SQLException e1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong"
                );
                e1.printStackTrace();
            }

        });

        return view;
    }

    private MyPanel registrarHome() {
        String query = "SELECT * FROM Student;";
        MyPanel view = createInfoPanel(query, false);
        view.addButton("Log out").addActionListener(e -> logout());
        view.addButton("View individual student").addActionListener(
                e -> changeView(selectStudent())
        );
        view.addButton("Add student").addActionListener(e -> changeView(addStudent()));
        view.addButton("Delete student").addActionListener(e -> changeView(deleteStudent()));

        return view;
    }

    private MyPanel selectStudent() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(registrarHome()));

        try {
            ArrayList<String> studentIDs = SQLFunctions.queryToList(
                    "SELECT StudentID FROM Student ORDER BY StudentID;", rs -> rs.getString("StudentID")
            );
            view.addComboBox("Student ID", "studentid", studentIDs);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't fetch student ID's: " + e.getCause().getMessage()
            );
            e.printStackTrace();
        }

        view.addButton("Select").addActionListener(e -> {
            String studentID = view.getString("studentid");
            changeView(gradeStudent(studentID));
        });

        return view;
    }

    private MyPanel gradeStudent(String studentID) {
        GradeStudentView view = new GradeStudentView(studentID);


        if(LoggedInUser.getInstance().getAccountType().equals(AccountType.Teacher)) {
            view.getBackButton().addActionListener(e -> changeView(createTeacherView()));
            view.addButton("Grade Student").addActionListener(e -> {
                DefaultTableModel model = view.getModel();

                try(Connection con = SQLFunctions.connectToDatabase()) {
                    con.setAutoCommit(false);
                    String query = "" +
                            "UPDATE Grades " +
                            "   SET Grade = ? " +
                            "     , Resit = ? " +
                            " WHERE ModuleID = ? " +
                            "       AND StudentPeriod = ?;";

                    int rows = model.getRowCount();


                    try(PreparedStatement pstmt = con.prepareStatement(query)) {
                        for(int i = 0; i < rows; ++i) {
                            String moduleID = (String) model.getValueAt(i, 0);
                            String studentPeriod = (String) model.getValueAt(i, 3);
                            Object grade = null, resit;

                            if(!view.IS_RESIT) {
                                grade = model.getValueAt(i, view.GRADE_COLUMN);

                            }
                            resit = model.getValueAt(i, view.RESIT_COLUMN);

                            if(grade == null) {
                                pstmt.setNull(1, Types.INTEGER);
                            } else if(grade instanceof String) {
                                if(((String) grade).isEmpty()) {
                                    pstmt.setNull(1, Types.INTEGER);
                                } else {
                                    pstmt.setInt(1, Integer.parseInt((String) grade));
                                }
                            } else if(grade instanceof Integer) {
                                pstmt.setInt(1, (int) grade);
                            }

                            if(resit == null) {
                                pstmt.setNull(2, Types.INTEGER);
                            } else if(resit instanceof String){
                                if(((String) resit).isEmpty()) {
                                    pstmt.setNull(2, Types.INTEGER);
                                } else {
                                    pstmt.setInt(2, Integer.parseInt((String) resit));
                                }
                            } else if(resit instanceof Integer) {
                                pstmt.setInt(2, (int) resit);
                            }

                            pstmt.setString(3, moduleID);
                            pstmt.setString(4, studentPeriod);

                            System.out.println(pstmt);

                            System.out.println(pstmt.toString());
                            pstmt.executeUpdate();
                        }
                    }

                    con.commit();
                    JOptionPane.showMessageDialog(
                            null,
                            "Graded student!"
                    );
                    changeView(gradeStudent(studentID));
                } catch (SQLException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null,
                            "Something went wrong"
                    );
                }

            });
            view.addButton("Progress Student").addActionListener(e -> {
                // first we find out what the students latest study period is
                String query = "SELECT MAX(StudentPeriod) FROM StudentPeriod WHERE StudentID = ?;";
                String studentPeriod;
                Grade g = null;

                try(Connection con = SQLFunctions.connectToDatabase();
                    PreparedStatement pstmt = con.prepareStatement(query)) {

                    pstmt.setString(1, studentID);
                    try(ResultSet rs = pstmt.executeQuery()) {
                        rs.next();
                        studentPeriod = rs.getString(1);
                    }


                    TeacherFunctions.ProgressReturn ret = TeacherFunctions.progressToNextPeriod(studentPeriod);
                    System.out.println(ret);

                    switch (ret) {
                        case NotGraded:
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Student still has ungraded modules, please fix"
                            );
                            return;
                        case Progressed:
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Student has progressed!"
                            );
                            changeView(createTeacherView());
                            return;
                        case Failed:
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Student failed and is resitting"
                            );
                            changeView(createTeacherView());
                            return;
                        case NotEnoughCredits:
                            JOptionPane.showMessageDialog(
                                    null,
                                    "Student is not signed up for correct number of credits, please remedy"
                            );
                            changeView(createTeacherView());
                            return;
                        case PassedAndFinished:
                            g = TeacherFunctions.gradeDegree(studentID, true);
                            break;
                        case FailedAndFinished:
                            g = TeacherFunctions.gradeDegree(studentID, false);
                    }
                    String message = "Degree has finished, student attained:\n";

                    switch(g) {
                        case Pass:
                            message += "a pass";
                            break;
                        case Merit:
                            message += "a merit";
                            break;
                        case FirstClass:
                            message += "a first class honours";
                            break;
                        case UpperSecond:
                            message += "an upper second";
                            break;
                        case LowerSecond:
                            message += "a lower second";
                            break;
                        case ThirdClass:
                            message += "a third class";
                            break;
                        case Distinction:
                            message += "a distinction";
                            break;
                        case Fail:
                            message += "an overall fail";
                            break;
                        case EquivBSC:
                            message += "a fail at the fourth level of their masters, achieving the corresponding" +
                                    "bsc.";
                            break;
                    }
                    JOptionPane.showMessageDialog(null, message);

                } catch (SQLException e1) {
                    e1.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null,
                            "Something went wrong"
                    );
                }

            });
        } else if(LoggedInUser.getInstance().getAccountType().equals(AccountType.Registrar)) {
            view.addButton("Register for Optionals").addActionListener(e -> changeView(moduleRegisterer(studentID)));
            view.getBackButton().addActionListener(e -> changeView(registrarHome()));
        }

        return view;
    }

    private JPanel moduleRegisterer(String studentID) {
        ModuleRegisterView view = new ModuleRegisterView(studentID);

        view.getBackButton().addActionListener(e -> changeView(registrarHome()));

        view.addButton("Register").addActionListener(e -> {
            String moduleID = view.getSelectedModule();

            String query = "INSERT INTO Grades(ModuleID, StudentPeriod) VALUES (?, ?); ";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, moduleID);
                pstmt.setString(2, view.getLatestStudentPeriod());

                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(
                        null,
                        "Module registered!"
                );

                changeView(moduleRegisterer(studentID));


            } catch (SQLException e1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong."
                );
                e1.printStackTrace();
            }

        });

        view.addButton("Remove Module").addActionListener(e -> {
            String query = "DELETE FROM Grades WHERE ModuleID = ? AND StudentPeriod = ?;";

            try(Connection con = SQLFunctions.connectToDatabase();
                PreparedStatement pstmt = con.prepareStatement(query)) {

                pstmt.setString(1, view.getSelectedModule());
                pstmt.setString(2, view.getLatestStudentPeriod());

                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(
                        null,
                        "Student de-registered for module"
                );

                changeView(moduleRegisterer(studentID));

            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Something went wrong"
                );
            }
        });

        return view;
    }

    private MyPanel addStudent() {
        AddStudentView view = new AddStudentView();
        if(LoggedInUser.getInstance().getAccountType().equals(AccountType.Registrar)) {
            view.getBackButton().addActionListener(evt -> changeView(registrarHome()));
        } else if (LoggedInUser.getInstance().getAccountType().equals(AccountType.Admin)) {
            view.getBackButton().addActionListener(evt -> changeView(createAdminSwitchboard()));
        }
        view.addButton("Add").addActionListener(evt -> {
            try {
                char[] pw = view.getPassword().length == 0 ? StudentFunctions.generateRandomPassword() : view.getPassword();
                RegistrarFunctions.addStudent(
                        view.getString("title"),
                        view.getString("forename"),
                        view.getString("surname"),
                        view.getString("tutor"),
                        view.getString("degree"),
                        view.getString("startdate"),
                        pw
                );

                JOptionPane.showMessageDialog(null, "PASSWORD GIVEN IS \"" + new String(pw) + "\"");

                if(LoggedInUser.getInstance().getAccountType().equals(Admin)) {
                    changeView(createAdminSwitchboard());
                } else {
                    changeView(registrarHome());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error: User not created");
            }
        });
        return view;
    }

    private MyPanel deleteStudent() {
        InputPanel view = new InputPanel(true);
        view.getBackButton().addActionListener(e -> changeView(registrarHome()));

        try {
            ArrayList<String> usernames = SQLFunctions.queryToList(
                    "SELECT Username FROM Student ORDER BY Username;",
                    rs -> rs.getString(1)
            );

            view.addComboBox("Username", "username", usernames);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    "Couldn't get usernames: " + e.getCause().getMessage()
            );
        }
        view.addButton("Delete").addActionListener(e -> {
            try {
                RegistrarFunctions.removeStudent(view.getString("username"));

            } catch (SQLException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(
                        null, "Something went wrong"
                );
                return;
            }
            JOptionPane.showMessageDialog(null, "Deleted!");
            changeView(registrarHome());
        });

        return view;
    }

    @SafeVarargs
    private final MyPanel createInfoPanel(
            String query, boolean hasBackButton, CheckedConsumer<PreparedStatement, SQLException>... params
    ) {
        MyPanel view = new MyPanel(hasBackButton);
        JScrollPane scrollPane;
        try {
            scrollPane = new JScrollPane(new JTable(GUI.queryToTable(query, params)));
            view.add(scrollPane, BorderLayout.CENTER);

        } catch (SQLException e) {
            e.printStackTrace();
            JTextArea errorLabel = new JTextArea(e.getCause().getMessage());
            errorLabel.setLineWrap(true);
            errorLabel.setOpaque(false);
            errorLabel.setWrapStyleWord(true);

            view.add(errorLabel, BorderLayout.CENTER);
        }


        return view;
    }

    private JPanel viewStudent(String studentID) {
        String query = "" +
                "SELECT StudentPeriod.StudentPeriod " +
                "     , ModuleID " +
                "     , COALESCE(Grade, 'Not Taken') as 'Initial Grade' " +
                "     , COALESCE(Resit, 'Not Taken') as 'Resit Grade' " +
                "  FROM Grades " +
                "  JOIN StudentPeriod " +
                "       ON StudentPeriod.StudentPeriod = Grades.StudentPeriod " +
                " WHERE StudentID = ?;";

        MyPanel view = createInfoPanel(query, false, s -> s.setString(1, studentID));


        view.addButton("Log out").addActionListener(e -> logout());
        return view;
    }

    /**
     * Generic function to create a switchboard that just has a list
     * of buttons that take you to various JPanels, provided as suppliers in the
     * function
     *
     * We can give every switchboard a logout button at the bottom because
     * we know that they only show up directly after logging in
     *
     * (at first it seemed like every user type would need a switchboard but it looks like
     * having a generic function here isn't necessary since we use it one time but hey)
     */
    @SafeVarargs
    private final JPanel createGenericSwitchboard(Pair<String, Supplier<JPanel>>... pairs) {
        // Switchboard is the whole thing, buttonpanel is the panel
        // with the switch buttons on
        JPanel switchboard = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());

        // Create each button using the titles and link them to an action that
        // creates the view
        for(Pair<String, Supplier<JPanel>> pair : pairs) {
            JButton button = new JButton(pair.first);
            button.addActionListener(e -> changeView(pair.second.get()));
            buttonPanel.add(button);
        }

        switchboard.add(buttonPanel, BorderLayout.CENTER);

        JPanel logoutPanel = new JPanel(new FlowLayout());
        JButton logoutButton = new JButton("Log out");

        logoutButton.addActionListener(e -> changeView(createLoginScreen()));
        logoutPanel.add(logoutButton);
        switchboard.add(logoutPanel, BorderLayout.PAGE_END);

        buttonPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        return switchboard;
    }

    public static void main(String[] args) {
        new AppController();
    }
}
