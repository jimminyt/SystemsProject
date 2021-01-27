package systems.team040.gui.components;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;

public class MyTextField extends JTextField {
    private String validationPattern;
    private boolean valid;
    private boolean patternProvided;
    private Color validColor = new Color(102, 255, 51);
    private Color invalidColor = new Color(255, 51, 51);

    public boolean isOkay() {
        return valid;
    }

    private MyTextField(String regex, boolean patternProvided) {
        this.patternProvided = patternProvided;
        validationPattern = regex;
        valid = "".matches(regex);
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setDocument(new MyDocument());
        setPreferredSize(new Dimension(150, 27));

        if(patternProvided) {
            addPropertyChangeListener("text", evt -> {
                valid = ((String)evt.getNewValue()).matches(validationPattern);
                if(valid) {
                    setBorder(BorderFactory.createLineBorder(validColor));
                } else {
                    setBorder(BorderFactory.createLineBorder(invalidColor));
                }
            });
        }
    }

    public MyTextField(String regex) { this(regex, true); }
    public MyTextField() { this("", false); }

    class MyDocument extends PlainDocument {
        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String oldVal, newVal;
            oldVal = getVal();
            super.replace(offset, length, text, attrs);
            newVal = getVal();

            if(!oldVal.equals(newVal)) {
                MyTextField.this.firePropertyChange("text", oldVal, newVal);
            }
        }

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            String oldVal, newVal;
            oldVal = getVal();
            super.remove(offs, len);
            newVal = getVal();

            if(!oldVal.equals(newVal)) {
                MyTextField.this.firePropertyChange("text", oldVal, newVal);
            }
        }

        private String getVal() throws BadLocationException {
            return getText(0, getLength());
        }
    }
}
