package test.components;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.das2.client.Das2ServerGUI;

/**
 *
 * @author jbf
 */
public class TestDas2ServerGUI {

    private static void renderGUI(File pngFile, String dsdf, String params) throws Exception {
        Das2ServerGUI x = new Das2ServerGUI();
        x.setSpecification(dsdf);
        x.setParameters(params);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    JPanel p = x.getPanel();
                    renderComponentToPng(p, pngFile);
                    System.err.println("render to " + pngFile);
                    System.err.println(x.getParameters());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void renderComponentToPng(JPanel panel, File file) throws Exception {
        Dimension size = panel.getPreferredSize();
        if (size == null || size.width <= 0 || size.height <= 0) {
            size = new Dimension(400, 300);
        }

        panel.setSize(size);
        panel.doLayout();

        layoutRecursively(panel);

        BufferedImage image = new BufferedImage(
                size.width,
                size.height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2 = image.createGraphics();
        try {
            panel.paint(g2);
        } finally {
            g2.dispose();
        }

        ImageIO.write(image, "png", file);
    }

    private static void layoutRecursively(java.awt.Container c) {
        c.doLayout();
        for (java.awt.Component child : c.getComponents()) {
            if (child instanceof java.awt.Container) {
                layoutRecursively((java.awt.Container) child);
            }
        }
    }

    public static void testCase(File outdir, int test) throws Exception {
        String dsdf;
        String params0;
        switch (test) {
            case 1:
                dsdf = "param_01 = '1.5V_REF | Simulate +1.8 monitor'\n"
                        + "param_02 = '1.5V_WvFE'\n"
                        + "param_03 = '1.5V_Y180'\n"
                        + "param_04 = '1.8U | Power Supply'\n"
                        + "param_05 = '1.8V_MEM'";
                params0 = "1.5V_REF 1.5V_WvFE 1.8V_MEM Extra-Unrecognized";
                break;
            case 2:
                dsdf = "param_01 = 'packet_ids|Output data from the following packet IDs (defaults to all)|@|set: , x242 x252'\n";
                params0 = "";
                break;
            case 3:
                dsdf = "param_01='sensor|Select the sensor to output| sensor=@ | option: Eu Ev Ew Bu Bv Bw'\n"
                        + "param_02='fftlen|Select the number of points in the DFT | fftlen=@ | range: 16 to 208896'\n"
                        + "param_03='overlap|Select the percentage overlap for each DFT | overlap=@ | range: 0 to 90'\n"
                        + "param_04='adj-overlap|Adjacent Overlap | adj-overlap=@ | option: true false'";
                params0 = "sensor=Bu overlap=50 adj-overlap=false";
                break;
            case 4:
                dsdf = "param_00  =  Keep_Negative | Negative values are used as a noise flag and are normally excluded.  Use this option to keep them. | --negative=off \n"
                        + "param_01  =  No_PLS_Filter | Turn off PLS noise filter for the 311 Hz and 562 Hz bands. | --pls=off\n"
                        + "param_02  =  No_Burst_Filter | Turn off 17-time-point noise burst filter | --noise=off\n"
                        + "param_03  =  Threshold_Filter | Filter out values above the data number thresholds given in HOUR[1/2]/FILTER.CSV | --threshold=on \n"
                        + "param_04  =  Spike_Filter | Filter interference observed in 1.0, 1.78 and 3.1 KHz channels | --v2spike=on\n"
                        + "param_08  =  Channel | Output only a single channel, by default all are sent | --chan=@ |option: 10.0Hz 17.8Hz 31.1Hz 56.2Hz 100Hz 178Hz 311Hz 562Hz 1.00kHz 1.78kHz 3.11kHz 5.62kHz 10.0kHz 17.8kHz 31.1kHz 56.2kHz\n"
                        + "param_09  =  SCLK | Output spacecraft clock values in addition to SCET times | -s\n"
                        + "param_10  =  output|Set the output units to \\nDN: Raw data numbers; \\nEF: Electric Field in V/m; \\nSD: Electric spectral density in V^2/m^2 Hz; \\nPF: Electric power spectral density in W/m^2 Hz\\n|--units=@|option: DN EF SD PF";
                params0= " --negative=off";
                break;
            default:
                throw new IllegalArgumentException("bad test number");
        }

        File f = new File(outdir, String.format("%03d.png", test));

        if (System.getProperty("java.awt.headless", "false").equals("true")) {
            renderGUI(f, dsdf, params0);
        } else {
            Das2ServerGUI x = new Das2ServerGUI();
            x.setSpecification(dsdf);
            x.setParameters(params0);

            if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null, x.getPanel(), "Edit params", JOptionPane.OK_CANCEL_OPTION)) {
                System.err.println(x.getParameters());
            }
        }

    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[]{"/tmp/ap/"};
        }
        File outdir = new File(args[0]);
        testCase(outdir, 1);
        testCase(outdir, 2);
        testCase(outdir, 3);
        testCase(outdir, 4);
    }
}
