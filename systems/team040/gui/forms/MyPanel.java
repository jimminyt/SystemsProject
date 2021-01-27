package systems.team040.gui.forms;

import javax.swing.*;
import java.awt.*;

public class MyPanel extends JPanel {
    protected JPanel centerPanel;
    protected JPanel buttonsPanel;
    protected JButton backButton;

    public MyPanel(boolean hasBackButton) {
        super(new BorderLayout());
        centerPanel = new JPanel(new FlowLayout());
        buttonsPanel = new JPanel(new FlowLayout());

        add(centerPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.PAGE_END);

        if(hasBackButton) {
            backButton = new JButton("Back");
            buttonsPanel.add(backButton);
        }

        buttonsPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    }

    public JButton getBackButton() {
        return backButton;
    }

    public JPanel getButtonsPanel() {
        return buttonsPanel;
    }

    public JPanel getCenterPanel() {
        return centerPanel;
    }

    public JButton addButton(String text) {
        JButton button = new JButton(text);
        buttonsPanel.add(button);
        return button;
    }
}
