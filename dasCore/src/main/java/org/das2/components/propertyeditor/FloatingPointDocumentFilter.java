package org.das2.components.propertyeditor;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;

class FloatingPointDocumentFilter extends DocumentFilter {
    
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet atts) throws BadLocationException {
        if (text.length() == 1) {
            if (text.charAt(0) == '-') {
                String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                int eIndex = Math.max(content.indexOf('e'), content.indexOf('E'));
                if (content.length() == 0) {
                    super.insertString(fb, 0, text, atts);
                }
                else if (eIndex < 0 || offset <= eIndex) {
                    if (content.charAt(0) == '-') {
                        super.remove(fb, 0, 1);
                    }
                    else {
                        super.insertString(fb, 0, text, atts);
                    }
                }
                else {
                    if (content.length() == eIndex+1) {
                        super.insertString(fb, eIndex+1, text, atts);
                    }
                    else if (content.charAt(eIndex+1) == '-') {
                        super.remove(fb, eIndex+1, 1);
                    }
                    else {
                        super.insertString(fb, eIndex+1, text, atts);
                    }
                }
            }
            else if (text.charAt(0) == '.') {
                String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                int dotIndex = content.indexOf('.');
                if (offset <= dotIndex) {
                    super.replace(fb, offset, dotIndex-offset+1, text, atts);
                }
                else if (dotIndex < 0) {
                    super.insertString(fb, offset, text, atts);
                }
            }
            else if (text.charAt(0) == 'e' || text.charAt(0) == 'E') {
                String content = fb.getDocument().getText(0, fb.getDocument().getLength());
                int eIndex = Math.max(content.indexOf('e'), content.indexOf('E'));
                if (offset <= eIndex) {
                    super.replace(fb, offset, eIndex-offset+1, text, atts);
                }
                else if (eIndex < 0) {
                    super.insertString(fb, offset, text, atts);
                }
            }
            else if (Character.isDigit(text.charAt(0))) {
                super.insertString(fb, offset, text, atts);
            }
        }
    }
    
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet atts) throws BadLocationException {
        remove(fb, offset, length);
        insertString(fb, offset, text, atts);
    }
    
}

