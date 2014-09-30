/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.datum;

/**
 * represent a rational number
 *
 * @author jbf
 */
public class RationalNumber {

    Ratio n;
    Ratio exp;

    /**
     * create a rationalNumber that is very close to d.
     *
     * @param d
     */
    public RationalNumber(double d) {
        int iexp= (int)Math.log10( Math.abs(d) );
        exp= new Ratio( iexp );
        d= d / Math.pow( 10, iexp );
        n= Ratio.create(d);
    }

    /**
     * create a rationalNumber with n * 10^exp
     *
     * @param n
     * @param exp
     */
    public RationalNumber(Ratio n, Ratio exp) {
        this.exp = exp;
        this.n = n;
    }

    /**
     * create a rationalNumber with n * 10^exp
     *
     * @param n
     */
    public RationalNumber(Ratio n) {
        this.exp = Ratio.zero;
        this.n = n;
    }

    /**
     * create a rationalNumber with n * 10^exp
     *
     * @param n
     */
    public RationalNumber(int n) {
        this.exp = Ratio.zero;
        this.n = new Ratio(n);
    }

    /**
     * create a new rational number with numerator n and denominator d. The
     * result will be n/d*10^0
     *
     * @param n the numerator
     * @param d the denominator.
     */
    public RationalNumber(int n, int d) {
        this.exp = Ratio.zero;
        this.n = new Ratio(n, d);
    }

    /**
     * return a close representation using double.
     *
     * @return
     */
    public double doubleValue() {
        return n.numerator * Math.pow(10, exp.numerator / (double) exp.denominator) / n.denominator;
    }

    public String toString() {
        return String.valueOf(n.doubleValue() * Math.pow(10, exp.doubleValue()));
    }

    /**
     * parse the string to get the RationalNumber. This simply calls
     * Double.parseDouble, but a future implementation could do a better job of
     * this.
     *
     * @param s the string.
     * @return the RationalNumber
     * @throws NumberFormatException
     */
    public static RationalNumber parse(String s) throws NumberFormatException {
        return new RationalNumber(Ratio.create(s));
    }

    /**
     * divide by the number.
     *
     * @param number
     * @return
     */
    public RationalNumber divide(RationalNumber number) {
        return new RationalNumber(this.n.divide(number.n), this.exp.subtract(number.exp));
    }

    /**
     * multiply by the number
     *
     * @param number
     * @return
     */
    public RationalNumber multiply(RationalNumber number) {
        return new RationalNumber(this.n.multiply(number.n), this.exp.add(number.exp));
    }

    public RationalNumber pow(Ratio r) {
        return new RationalNumber(this.n.pow(r), exp.multiply(r));
    }

    /**
     * returns the sqrt of the number, e.g. 4 * 10^
     *
     * @return
     */
    public RationalNumber sqrt() {
        return new RationalNumber(n.sqrt(), exp.divide(new Ratio(2)));
    }

    /**
     * return true if the number is 1.
     *
     * @return true if the number is 1.
     */
    public boolean isOne() {
        return this.n.isOne() && this.exp.isZero();
    }

    /**
     * return true if the number is 0.
     *
     * @return true if the number is 0.
     */
    public boolean isZero() {
        return this.n.isZero();
    }
}
