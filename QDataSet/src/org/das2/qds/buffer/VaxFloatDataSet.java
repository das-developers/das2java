
package org.das2.qds.buffer;

import java.nio.ByteBuffer;

/**
 * reader to read Vax floats, motivated by RPWS group need to read ISEE data.
 *<blockquote><pre>
 *vap+bin:sftp://klunk.physics.uiowa.edu/opt/project/isee/archive/a1977/77295.arc?recLength=880&type=vaxfloat&recOffset=20
 *</pre></blockquote>
 * @author jbf
 */
public final class VaxFloatDataSet extends BufferDataSet {
// from http://www.codeproject.com/Articles/12363/Transform-between-IEEE-IBM-or-VAX-floating-point-n
//SEF :       S        EEEEEEEE        FFFFFFF        FFFFFFFF        FFFFFFFF
//bits :      1        2      9        10                                    32
//bytes :     byte2           byte1                   byte4           byte3
// JBF: Note the above wasn't used, but instead code from google for "vax float java nio Jenness"

    public VaxFloatDataSet(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, VAX_FLOAT, back);
        this.makeImmutable();
    }

    /**
     * convert from 4byte vax float to Java double.  See
     * ftp://202.127.24.195/pub/astrolibs/.../java/gsd/.../GSDVAXBytes.java
     * google for "vax float java nio Jenness"
     * @param buf1
     * @param offset
     * @return double value represented.
     */
    private double vaxFloatValue2(ByteBuffer buf, int offset) {

        ByteBuffer tmp = ByteBuffer.allocate(4);

        // Take this from gsd2_nativr.c as written by Horst and Remo

        // Extract the exponent
        int e = ((buf.get(1+offset) << 1) & 0xfe) | ((buf.get(0+offset) >> 7) & 0x1);

        /*    If the (biased) exponent is greater than 2, then the
         *    VAXF number can be represented in IEEE_S form as a
         *    normalised number. Decrement the exponent by 2. This
         *    allows for a difference of 1 between the exponent bias
         *    values of the two number formats and a further
         *    difference of one in the assumed position of the binary
         *    radix point.
         */

        if (e > 2) {
            e -= 2;

            /*       Construct the resulting IEEE_S number, using the
             *       appropriate bytes of the VAXF number but
             *       replacing the exponent field with its modified
             *       value.
             */
            tmp.put(0, (byte) ((buf.get(1+offset) & 0x80) | ((e >> 1) & 0x7f)));
            tmp.put(1, (byte) ((buf.get(0+offset) & 0x7f) | ((e << 7) & 0x80)));
            tmp.put(2, (byte) (buf.get(3+offset)));
            tmp.put(3, (byte) (buf.get(2+offset)));

        } else if (e == 0) {

            /*    If the (biased) VAXF exponent is zero, then the
             *    resulting IEEE_S value is zero (or we have a VAX
             *    reserved operand, but we assume that can't happen).
             */

            for (int i = 0; i < 4; i++) {
                tmp.put(i, (byte) 0);
            }

        } else {

            /*    Otherwise, if the (biased) exponent is 2 or less,
             *    then the IEEE_S equivalent will be a denormalised
             *    number, so the fraction field must be modified.
             *    Extract all the bits of the VAXF fraction field into
             *    a single integer (remember we can't assume what
             *    order the integer's bytes are stored in).  Also add
             *    the (normally omitted) leading 1.
             */

            int f =
                    buf.get(2+offset)
                    | (buf.get(3+offset) << 8)
                    | ((buf.get(0+offset) & 0x7f) << 16)
                    | (0x1 << 23);

            /*       Shift the fraction bits to account for the
             *       limited range of the exponent. Then pack the
             *       fraction field into the IEEE_S number. Retain the
             *       VAXF sign bit, but set the exponent field to zero
             *       (indicating a denormalised number).
             */

            f = f >> (3 - e);
            tmp.put(0, (byte) (buf.get(1+offset) & 0x80));
            tmp.put(1, (byte) ((f >> 16) & 0x7f));
            tmp.put(2, (byte) ((f >> 8) & 0xff));
            tmp.put(3, (byte) (f & 0xff));

        }

        /* We know we are always BIG endian so just convert the byte buffer
        to a float */
        return tmp.getFloat();
    }

    @Override
    public double value() {
        return vaxFloatValue2(back, offset());
    }

    @Override
    public double value(int i0) {
        return vaxFloatValue2(back, offset(i0));
    }

    @Override
    public double value(int i0, int i1) {
        return vaxFloatValue2(back, offset(i0, i1));
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return vaxFloatValue2(back, offset(i0, i1, i2));
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return vaxFloatValue2(back, offset(i0, i1, i2, i3));
    }

//    public static void main(String[] args) throws FileNotFoundException, IOException {
//        // /opt/project/isee/archive/a1977/77295.arc
//        // 77295    69197230       62470            first three are 4-byte floats.  I can read these.
//        // 3.12926e+07 -5.01478e+07  1.97070e+07    GSEX GSEY GSEZ
//
//        File f = new File("/opt/project/isee/archive/a1977/77295.arc");
//        FileChannel fc = new FileInputStream(f).getChannel();
//        ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10000);
//        buf.order(ByteOrder.LITTLE_ENDIAN);
//        VaxFloat vf = new VaxFloat(1, 880, 12, 10, 1, 1, 1, buf);
//        System.err.println(vf.value(0));
//        Int i = new Int(1, 880, 0, 10, 1, 1, 1, buf);
//        System.err.println(i.value(0)); // verified.
//    }

    @Override
    public void putValue(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
