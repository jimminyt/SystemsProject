package systems.team040.gui.forms;

import systems.team040.functions.AccountType;
import systems.team040.gui.components.MyTextField;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;

public class AddUserView extends InputPanel {
    private JComboBox accountTypeCB;
    private JPasswordField passwordField;

    public int getAccountType() {
        return accountTypeCB.getSelectedIndex() + 1;
    }

    public AddUserView(boolean hasBackButton) {
        super(hasBackButton);
        ArrayList<String> accountTypes = new ArrayList<>();
        for(AccountType at : AccountType.values()) {
            accountTypes.add(at.toString());
        }

        addStringInput("Username", "username", new MyTextField(".+"), JTextComponent::getText);
        accountTypeCB = addComboBox("Account Type", "accounttype", accountTypes);

        passwordField = new JPasswordField();
        JPanel passwordPanel = new JPanel(new BorderLayout());
        passwordPanel.add(new JLabel("Password"), BorderLayout.PAGE_START);
        passwordPanel.add(passwordField, BorderLayout.CENTER);

        centerPanel.add(passwordPanel);
    }

    public char[] getPassword() {
        return passwordField.getPassword();
    }

    @Override
    public boolean isOkay() {
        boolean okay = super.isOkay();
        if(getPassword().length < 8) {
            if(okay) {
                errorMessage = "Errors found:\n";
            }

            errorMessage += "Password must be 8 characters or more\n";

            okay = false;
        }

        return okay;
    }
}
