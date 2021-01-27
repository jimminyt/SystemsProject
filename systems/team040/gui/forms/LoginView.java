package systems.team040.gui.forms;

import javax.swing.*;
import java.awt.*;

/**
 * This view is the login page for the application
 */
public class LoginView extends MyPanel {
   private JTextField username;
   private JPasswordField password;
   private JButton login;

    public LoginView() {
        super(false);

        username = new JTextField();
        password = new JPasswordField("password");
        login = new JButton("login");

        username.setPreferredSize(new Dimension(200, 24));
        password.setPreferredSize(new Dimension(200, 24));

        JPanel usernamePanel = new JPanel(new BorderLayout());
        JPanel passwordPanel = new JPanel(new BorderLayout());

        usernamePanel.add(new JLabel("Username"), BorderLayout.PAGE_START);
        passwordPanel.add(new JLabel("Password"), BorderLayout.PAGE_START);

        usernamePanel.add(username, BorderLayout.CENTER);
        passwordPanel.add(password, BorderLayout.CENTER);

        centerPanel.add(usernamePanel);
        centerPanel.add(passwordPanel);
    }

    public String getEnteredUsername() {
        return username.getText();
    }

    public char[] getEnteredPassword() {
        return password.getPassword();
    }
}
