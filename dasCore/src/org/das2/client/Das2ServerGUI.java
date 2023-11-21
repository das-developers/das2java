package org.das2.client;

import java.lang.StringBuilder;

import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Component;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JCheckBox;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ButtonGroup;
import javax.swing.JButton;

import java.util.LinkedHashMap;
import java.util.ArrayList;

import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.StringReader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.das2.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author jbf
 */
public class Das2ServerGUI {

    private Object readXML(String xmlsrc) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        // String _ = "Read the Das2Server container for DSDF";
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputSource inputsrc = new InputSource(new StringReader(xmlsrc));
        Document doc = builder.parse(inputsrc);
        XPath xp = XPathFactory.newInstance().newXPath();
        Node nn = (Node) xp.evaluate("//properties", doc, XPathConstants.NODE);
        LinkedHashMap mm = new LinkedHashMap();
        ArrayList ll = new ArrayList(100);
        for (int i = 0; i < 100; i++) {
            ll.add(i, "");
        }

        NamedNodeMap aa = nn.getAttributes();
        for (int i = 0; i < aa.getLength(); i++) {
            Node itm = aa.item(i);
            if (itm.getNodeName().startsWith("param_")) {
                int idx = Integer.parseInt(itm.getNodeName().substring(6));
                ll.set(idx, itm.getNodeValue());
            }
        }

        return ll;
    }

    private ArrayList readDsdf(String dsdfsrc) {
        // String _ = "Read the DSDF source file";
        ArrayList ll = new ArrayList(100);
        for (int i = 0; i < 100; i++) {
            ll.add(i, "");
        }

        for (String line : dsdfsrc.split("\n")) {
            if (line.startsWith("param_")) {
                int ieq = line.indexOf("=");
                int idx = Integer.parseInt(line.substring(0, ieq).substring(6));
                String vv = line.substring(ieq + 1).trim();
                if ((vv.charAt(0) == '\'') && (vv.charAt(vv.length() - 1) == '\'')) {
                    vv = vv.substring(1, -1);
                }
                ll.set(idx, vv);
            }
        }

        return ll;
    }

    private String checkMatch(String template, String sval) {
        // String _ = "if template=xxx@bb and sval=xxxJbb then return J otherwise return empty string";
        template = template.trim();
        sval = sval.trim();
        if (template.indexOf("@") > -1) {
            int idx = template.indexOf("@");
            int i = sval.length() - template.length() - idx - 1;
            if (template.substring(0, idx).equals(sval.substring(0, idx)) && template.substring(idx + 1).equals(sval.substring(i))) {
                return sval.substring(idx, (idx + sval.length() - template.length() + 1));
            } else {
                return "";
            }
        } else {
            if (template.equals(sval)) {
                return sval;
            } else {
                return "";
            }
        }
    }

    private int index(String[] arr, String search) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(search)) {
                return i;
            }
        }
        return -1;
    }

    private static String[] removeArrayElement(String[] oddArray, int index) {
        //array is empty or index is beyond array bounds
        if (oddArray == null || index < 0 || index >= oddArray.length) {
            return oddArray;
        }
        String[] result = new String[oddArray.length - 1];
        System.arraycopy(oddArray, 0, result, 0, index);
        System.arraycopy(oddArray, index + 1, result, index, result.length - index);
        return result;
    }

    private String findParamValue(String[] paramsArr, String ss0, String template) {
        // String _ = "return the value from paramsArr or '' (empty string).  template can be a template or a regex";
        try {
            if (template == null) {
                int idx = index(paramsArr, ss0);
                paramsArr = removeArrayElement(paramsArr, idx);
                return ss0;
            } else {
                if (template.charAt(0) == '^') {
                    String regex = template;
                    Pattern pattern = Pattern.compile(regex);
                    for (String item : paramsArr) {
                        Matcher match = pattern.matcher(item);
                        if (match != null) {
                            return item;
                        }
                    }
                    return "";
                } else {
                    String[] templates = template.split("\\s+");
                    int i = 0;
                    int ipa = 0;
                    System.out.println("" + templates + "" + templates.length);
                    while (i < templates.length) {
                        while ((ipa < paramsArr.length) && (i < templates.length)) {
                            String item = paramsArr[ipa].trim();
                            String match = checkMatch(templates[i], item);
                            if (match.length()==0 ) {
                                return "";
                            } else {
                                if (templates[i].contains("@")) {
                                    return match;
                                } else {
                                    ipa = ipa + 1;
                                    i = i + 1;
                                }
                            }
                        }

                    }

                    return "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("param not found: " + ss0);
            return "";
        }
    }

    private void setSelectedListItems(Component jcomponent, String[] allItems, String[] items) {
        if (jcomponent instanceof JList) {
            JList jList = (JList) jcomponent;
            jList.clearSelection();
            for (String item : items) {
                try {
                    int idx = index(allItems, item);
                    if (idx > -1) {
                        jList.addSelectionInterval(idx, idx);
                    }
                } catch (Exception e) {
                    continue;
                }
            }

        } else {
            JPanel jList = (JPanel) jcomponent;
            for (Component b : jList.getComponents()) {
                JCheckBox tp = (JCheckBox) b;
                String item = tp.getName();
                try {
                    int idx = index(items, item);
                    if (idx > -1) {
                        tp.setSelected(true);
                    }
                } catch (Exception e) {
                    continue;
                }
            }

        }
    }

    private Object getSelectedListItems(Component jcomponent) {
        if (jcomponent instanceof JList) {
            return ((JList) jcomponent).getSelectedValuesList();
        } else {
            ArrayList resultList = new ArrayList();
            JPanel jList = (JPanel) jcomponent;
            for (Component c : jList.getComponents()) {
                JCheckBox b = (JCheckBox) c;
                if (b.isSelected()) {
                    resultList.add(b.getText());
                }
            }

            return resultList;
        }
    }

    private String getRegex(String t) {
        // "Return the regular expression for the template.  The first field will be the @ symbol value";
        return t.replaceAll("@", "(\\S+)");
    }

}
