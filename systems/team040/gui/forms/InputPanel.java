package systems.team040.gui.forms;

import systems.team040.gui.components.MyTextField;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Kinda janky but gives a way to generate inputforms easily enough
 */
public class InputPanel extends MyPanel {
    private HashMap<String, Supplier<String>> stringGetters;
    private HashMap<String, Supplier<Integer>> integerGetters;
    protected String errorMessage;
    private ArrayList<MyTextField> needValidating;

    public InputPanel(boolean hasBackButton) {
        super(hasBackButton);
        stringGetters = new HashMap<>();
        integerGetters = new HashMap<>();
        needValidating = new ArrayList<>();
    }

    public <T extends JComponent> void addStringInput(
            String label, String key, T component, Function<T, String> func) {

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel(label), BorderLayout.PAGE_START);
        inputPanel.add(component, BorderLayout.CENTER);

        centerPanel.add(inputPanel);

        stringGetters.put(key, () -> func.apply(component));
    }

    public void addStringInput(
            String label, String key, MyTextField component, Function<MyTextField, String> func) {

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel(label), BorderLayout.PAGE_START);
        inputPanel.add(component, BorderLayout.CENTER);

        centerPanel.add(inputPanel);

        stringGetters.put(key, () -> func.apply(component));
        needValidating.add(component);
    }

    public <T extends JComponent> void addNumericInput(
            String label, String key, T component, Function<T, Integer> func) {

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel(label), BorderLayout.PAGE_START);
        inputPanel.add(component, BorderLayout.CENTER);

        centerPanel.add(inputPanel);
        integerGetters.put(key, () -> func.apply(component));
    }

    public JComboBox<String> addComboBox(String label, String key, ArrayList<String> list) {
        JComboBox<String> cbb = new JComboBox<>(list.toArray(new String[list.size()]));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel(label), BorderLayout.PAGE_START);
        inputPanel.add(cbb, BorderLayout.CENTER);

        centerPanel.add(inputPanel);
        stringGetters.put(key, () -> Objects.toString(cbb.getSelectedItem()));

        return cbb;
    }

    public String getString(String key) {
        return stringGetters.get(key).get();
    }

    public int getInteger(String key) {
        return integerGetters.get(key).get();
    }

    boolean isOkay() {
        boolean okay = true;
        StringBuilder sb = new StringBuilder();

        for(MyTextField textField : needValidating) {
            if(!textField.isOkay()) {
                if(okay) {
                    sb.append("Errors found:\n");
                }
                okay = false;
                if(textField.getText().isEmpty()) {
                    sb.append("Input cannot be empty");
                } else {
                    sb.append('"');
                    sb.append(textField.getText());
                    sb.append("\" is not valid input");
                }
            }
        }

        return okay;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
