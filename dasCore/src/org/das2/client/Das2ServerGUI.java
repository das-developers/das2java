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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Code for editing a Das2Server parameters argument.  This is a somewhat 
 * arbitary control, but the DSDF may try to describe a GUI, using the 
 * spec at https://github.com/das-developers/das2docs/wiki/Structured-Sub%E2%80%90values-for-Params
 * 
 * <tt>
 * Das2ServerGUI x = new Das2ServerGUI();
 * String dsdf = "param_01 = '1.5V_REF | Simulate +1.8 monitor'\n"
 *               + "param_02 = '1.5V_WvFE'\n"
 *               + "param_03 = '1.5V_Y180'\n"
 *               + "param_04 = '1.8U | Power Supply'\n"
 *               + "param_05 = '1.8V_MEM'";
 *       x.setSpecification(dsdf);
 *       x.setParameters("1.5V_REF 1.5V_REF 1.8V_MEM");
 *       if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, x.panel)) {
 *           System.err.println(x.getParameters());
 *       }
 * </tt>
 * @author jbf
 */
public class Das2ServerGUI {

    /**
     * Read the Das2Server container for DSDF
     *
     * @param xmlsrc
     * @return string array
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private String[] readXML(String xmlsrc) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
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

        return (String[]) ll.toArray(new String[ll.size()]);
    }

    /**
     * parse integer which may start with one 0.
     *
     * @param s
     * @return
     */
    private static int parseInt(String s) {
        s = s.trim();
        if (s.startsWith("0")) {
            return Integer.parseInt(s.substring(1));
        } else {
            return Integer.parseInt(s);
        }
    }

    /**
     * Read the DSDF source file
     */
    private String[] readDsdf(String dsdfsrc) {
        ArrayList ll = new ArrayList(100);
        for (int i = 0; i < 100; i++) {
            ll.add(i, "");
        }

        for (String line : dsdfsrc.split("\n")) {
            if (line.startsWith("param_")) {
                int ieq = line.indexOf("=");
                int idx = parseInt(line.substring(0, ieq).substring(6));
                String vv = line.substring(ieq + 1).trim();
                if ((vv.charAt(0) == '\'') && (vv.charAt(vv.length() - 1) == '\'')) {
                    vv = vv.substring(1, vv.length() - 1);
                }
                ll.set(idx, vv);
            }
        }

        return (String[]) ll.toArray(new String[ll.size()]);
    }

    /**
     * if template=xxx@bb and sval=xxxJbb then return J otherwise return empty
     * string
     *
     * @param template
     * @param sval
     * @return
     */
    private String checkMatch(String template, String sval) {
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

    /**
     * return the value from paramsArr or '' (empty string). template can be a
     * template or a regex
     *
     * @param paramsArr
     * @param ss0
     * @param template
     * @return
     */
    private String findParamValue(String[] paramsArr, String ss0, String template) {
        try {
            if (template == null) {
                int idx = index(paramsArr, ss0);
                if (idx == -1) {
                    return "";
                } else {
                    paramsArr = removeArrayElement(paramsArr, idx);
                    return ss0;
                }
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
                            if (match.length() == 0) {
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
        // JList handling code removed...
        JPanel jPanel = (JPanel) jcomponent;
        for (Component b : jPanel.getComponents()) {
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

    private List<String> getSelectedListItems(Component jcomponent) {
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

    private String[] ll;
    private String[] tt;  // types
    private String[] ff;  // formats (--dftlen @)
    private String[] ss;  // special arguments for the GUI type, like sep
    private JComponent[] cc;
    private JPanel panel;

    /**
     * set the DSDF specification for the parameters.  This can be an XML document
     * or a list of IDL name/value pairs.
     * @param sss 
     */
    public void setSpecification(String sss) {
        if ( sss.length()<4 ) {
            throw new IllegalArgumentException("control string is too short");
        }
        if ( sss.substring(0, 4).equals("[00]") ){
            sss = sss.substring(10);
        }
        if ( sss.subSequence(0,8).equals("<stream>") ) {
            try {
                ll = readXML(sss);
            } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException ex) {
                Logger.getLogger("das2").log(Level.SEVERE, null, ex);
            }
        } else {
            ll = readDsdf(sss);
        }

        tt = new String[101];
        ff = new String[101];
        ss = new String[101];
        cc = new JComponent[101];

    }

    public void setParameters(String paramz) {

        String[] paramsArr = paramz.split("\\s+"); //TODO: delim
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        boolean extra = false;
        for (int i = 0; i < 100; i++) {
            String itm = ll[i];
            if (itm.length() > 0) {
                itm = itm.trim();
                ss = itm.split("\\|", -2);
                int narg = ss.length;
                JComponent c2 = null;
                if (narg == 1) {
                    ss[0] = ss[0].trim();
                    JCheckBox c = new JCheckBox(ss[0]);
                    String vv = findParamValue(paramsArr, ss[0], null);
                    if (vv.length() > 0) {
                        c.setSelected(true);
                    }
                    tt[i] = "JCheckBox";
                    panel.add(c);
                    c2 = c;
                } else if (narg == 2) {
                    ss[1] = ss[1].trim();
                    ss[0] = ss[0].trim();
                    JCheckBox c = new JCheckBox(ss[0] + ": " + ss[1]);
                    String vv = findParamValue(paramsArr, ss[0], null);
                    if (vv.length() > 0) {
                        c.setSelected(true);
                    }
                    tt[i] = "JCheckBox";
                    panel.add(c);
                    c2 = c;
                } else if (narg == 3) {
                    ss[0] = ss[0].trim();
                    ss[1] = ss[1].trim();
                    ss[2] = ss[2].trim();
                    panel.add(new JLabel(ss[0] + ": " + ss[1]));
                    JTextField c = new JTextField("");
                    c.setMaximumSize(new Dimension(8000, c.getPreferredSize().height));
                    String vv = findParamValue(paramsArr, ss[0], null);
                    ff[i] = ss[2];
                    vv = findParamValue(paramsArr, ss[0], ss[2]);
                    if (vv.length() > 0) {
                        c.setText(vv);
                    }
                    tt[i] = "JTextField";
                    panel.add(c);
                    c2 = c;
                } else if (narg == 4) {
                    ss[0] = ss[0].trim();
                    ss[1] = ss[1].trim();
                    ss[2] = ss[2].trim();
                    ss[3] = ss[3].trim();
                    if (ss[3].startsWith("set:")) {
                        panel.add(new JLabel(ss[0] + ": " + ss[1]));
                        String[] sepAllItems = ss[3].substring(4).split("\\s+");
                        String[] allItems = Arrays.copyOfRange(sepAllItems, 1, sepAllItems.length);
                        String sep = sepAllItems[0];
                        ss[i] = sep;

                        JPanel panel1 = new JPanel();
                        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
                        for (String item : allItems) {
                            JCheckBox b1 = new JCheckBox(item);
                            b1.setSelected(false);
                            panel1.add(b1);
                        }

                        c2 = panel1;

                        String itemMatch = String.join("|", allItems);
                        String regex = String.format("^(%s)(?:%s(%s))*$", itemMatch, sep, itemMatch);

                        String vv = findParamValue(paramsArr, ss[0], regex);
                        if (vv.length() > 0) {
                            String[] items = vv.split(sep);
                            setSelectedListItems(c2, allItems, items);
                        }
                        tt[i] = "JList";

                    } else {
                        panel.add(new JLabel(ss[0] + ": " + ss[1] + " (" + ss[3] + ")"));
                        JTextField c = new JTextField("");
                        c2 = c;
                        c.setMaximumSize(new Dimension(8000, c.getPreferredSize().height));
                        String vv = findParamValue(paramsArr, ss[0], null);
                        ff[i] = ss[2];
                        vv = findParamValue(paramsArr, ss[0], ss[2]);
                        if (vv.length() > 0) {
                            c.setText(vv);
                        }
                        tt[i] = "JTextField";
                    }

                    panel.add(c2);

                } else {
                    extra = true;
                }

                if (c2 != null) {
                    panel.add(c2);
                    cc[i] = c2;
                    c2.setAlignmentX(Component.LEFT_ALIGNMENT);
                }
            }
        }

        if (extra) {
            panel.add(new JLabel("Text stuff"));
            JTextArea c = new JTextArea();
            c.setText(""); //TODO: unhandled parameters
            c.setRows(4);
            panel.add(c);
            cc[100] = c;
        }

    }

    public String getParameters() {
        String delim = " ";
        StringBuilder parametersBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            if (cc[i] == null) {
            } else if (tt[i].equals("JCheckBox")) {
                if (((JCheckBox) cc[i]).isSelected()) {
                    String txt = ((JCheckBox) cc[i]).getText();
                    int i2 = txt.indexOf(": ");
                    if (i2 > -1) {
                        txt = txt.substring(0, i2);
                    }
                    parametersBuilder.append(txt);
                    parametersBuilder.append(delim);
                }
            } else if (tt[i].equals("JTextField")) {
                String s = ((JTextField) cc[i]).getText();
                if (s.length() > 0) {
                    s = ff[i].replace("@", s);
                    parametersBuilder.append(s);
                    parametersBuilder.append(delim);
                }
            } else if (tt[i].equals("JList")) {
                List<String> selectedItems = getSelectedListItems(cc[i]);
                String sep = ss[i];
                parametersBuilder.append(String.join(sep, selectedItems));
                parametersBuilder.append(delim);
            }
        }
        if (cc[100] != null) {
            parametersBuilder.append(delim);
            String txt = ((JTextField) cc[100]).getText().replace("\n", delim);
            txt = txt.trim();
            parametersBuilder.append(txt);
        }

        return parametersBuilder.toString();
    }
    
    /**
     * return the panel.  See the javadoc for how this is to be called.
     * @return 
     */
    public JPanel getPanel() {
        return panel;
    }
    
    public static void main(String[] args) {
        Das2ServerGUI x = new Das2ServerGUI();
        String dsdf = "param_01 = '1.5V_REF | Simulate +1.8 monitor'\n"
                + "param_02 = '1.5V_WvFE'\n"
                + "param_03 = '1.5V_Y180'\n"
                + "param_04 = '1.8U | Power Supply'\n"
                + "param_05 = '1.8V_MEM'";
        x.setSpecification(dsdf);
        x.setParameters("1.5V_REF 1.5V_REF 1.8V_MEM");

        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, x.panel)) {
            System.err.println(x.getParameters());
        }

    }
}
