/*
 * SinWaveStream.java
 *
 * Created on January 5, 2004, 12:03 PM
 */

package edu.uiowa.physics.pw.das.stream.test;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.test.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.stream.*;
import java.nio.channels.*;
import java.util.*;

/**
 *
 * @author  Jeremy
 */

/* dumps out a sine wave */
public class SineWaveStream {
    
    private double frequency = 0.1;
    private boolean compress;
    
    public SineWaveStream(boolean compress) {
        this.compress = compress;
    }
    
    public void produceSineWaveStream(WritableByteChannel out) {
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            if (compress) { sd.setCompression("deflate"); }
            producer.streamDescriptor(sd);

            int nx=3600;

            StreamXDescriptor xDescriptor = new StreamXDescriptor();
            xDescriptor.setDataTransferType(DataTransferType.getByName("ascii24"));
            xDescriptor.setUnits(Units.seconds);
            xDescriptor.setBase(TimeUtil.createValid("2000-001"));
            StreamMultiYDescriptor y = new StreamMultiYDescriptor();
            y.setDataTransferType(DataTransferType.getByName("ascii10"));
            PacketDescriptor pd = new PacketDescriptor();
            pd.setXDescriptor(xDescriptor);
            pd.addYDescriptor(y);

            producer.packetDescriptor(pd);

            double[] z = new double[1];
            DatumVector[] yValues = new DatumVector[1];
            for (int i = 0; i < nx; i++) {
                Datum xTag = Datum.create((double)i, Units.seconds);
                z[0] = Math.sin(Math.PI * frequency * i );
                yValues[0] = DatumVector.newDatumVector(z, Units.dimensionless);
                producer.packet(pd, xTag, yValues);
            }

            producer.streamClosed(sd);
        }
        catch (StreamException se) {
            se.printStackTrace();
            System.exit(-1);
        }
    }
    
    public static void main(String[] args) {
        List argList = Arrays.asList(args);
        boolean c = argList.contains("-c");
        WritableByteChannel out = Channels.newChannel(System.out);
        SineWaveStream rs = new SineWaveStream(c);
        rs.produceSineWaveStream(out);
    }

}
