package org.das2.datum;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Finally introduce SI Units library, which preserves units through
 * multiplication and division.
 *
 * @author jbf
 */
public class SIUnits extends NumberUnits {

    private static final Logger logger = LoggerManager.getLogger("das2.datum.siunits");
    Ratio m;  // meter
    Ratio kg; // kilogram
    Ratio s;  // second
    Ratio A;  // ampere
    Ratio K;  // Kelvin

    RationalNumber multiplier;

    private SIUnits(String id, String desc, Ratio m, Ratio kg, Ratio s, Ratio A, Ratio K, RationalNumber multiplier) {
        super(id, desc);
        this.m = m;
        this.kg = kg;
        this.s = s;
        this.A = A;
        this.K = K;
        this.multiplier = multiplier;
    }

    private static final Map<String, SIUnits> instances = new HashMap();

    public static SIUnits create( String id ) {
        return (SIUnits) getByName(id);
    }
            
    public static SIUnits create(String id, String desc, Ratio m, Ratio kg, Ratio s, Ratio A, Ratio K, RationalNumber multiplier) {
        String tag = String.format("%s*%sm%skg%ss%sA%sK", multiplier.doubleValue(), m, kg, s, A, K);
        SIUnits u;
        synchronized (instances) {
            u = instances.get(tag);
            if (u != null) {
                return u;
            }
            try {
                Units u2 = Units.getByName(id);
                if (u2 != null && u2 instanceof SIUnits) {
                    return (SIUnits) u2;
                } else {
                    if (u2 != null) {
                        logger.warning("units were not SIUnits, this may cause problems");
                    }
                    SIUnits siu = new SIUnits(id, desc, m, kg, s, A, K, multiplier);
                    instances.put(tag, siu);
                    return siu;
                }
            } catch (IllegalArgumentException ex) {
                return new SIUnits(id, desc, m, kg, s, A, K, multiplier);
            }
        }
    }

    public static SIUnits create(String id, String desc, int m, int kg, int s, double multiplier) {
        return create(id, desc, new Ratio(m), new Ratio(kg), new Ratio(s), Ratio.one, Ratio.one, new RationalNumber(multiplier));
    }

    public static SIUnits create(String id, String desc, int m, int kg, int s) {
        return create(id, desc, new Ratio(m), new Ratio(kg), new Ratio(s), Ratio.one, Ratio.one, new RationalNumber(1));
    }

    public static SIUnits create(String id, String desc, int m, int kg, int s, int A, int K) {
        return create(id, desc, new Ratio(m), new Ratio(kg), new Ratio(s), new Ratio(A), new Ratio(K), new RationalNumber(1));
    }

    public static SIUnits create(String id, String desc, Ratio m, Ratio k, Ratio s, RationalNumber multiplier) {
        return create(id, desc, m, k, s, Ratio.one, Ratio.one, multiplier);
    }

    public static SIUnits multiply(SIUnits s1, SIUnits s2) {
        return create(s1.getId() + "*" + s2.getId(), s1.getId() + "*" + s2.getId(),
                s1.m.add(s2.m),
                s1.kg.add(s2.kg),
                s1.s.add(s2.s),
                s1.A.add(s2.A),
                s1.K.add(s2.K),
                s1.multiplier.multiply(s2.multiplier));
    }

    
    public static SIUnits multiply( String id, String desc, SIUnits s1, RationalNumber s ) {
        return create( id, desc,
                s1.m,
                s1.kg,
                s1.s,
                s1.A,
                s1.K,
                s1.multiplier.multiply(s) );
    }
    
    /**
     * apply the power to the exponent.  For example
     * pow(Units.kg,2)&rarr; kg**2
     * pow(Units.cm,2)&rarr; cm**2 == .01^2 * m^2.
     * Units.kg
     * @param s1 the unit, e.g. SIUnits.si_Hz
     * @param pow the exponent e.g. -1
     * @return SI unit, e.g. SIUnits.si_s
     */
    public static SIUnits pow( SIUnits s1, int pow ) {
        if ( pow==1 ) {
            return s1;
        } else {
            Ratio rpow= new Ratio(pow);
            return create( s1.getId() + "^" + pow, s1.getId() + "^" + pow,
                s1.m.multiply( rpow ),
                s1.kg.multiply( rpow ),
                s1.s.multiply( rpow ),
                s1.A.multiply( rpow ),
                s1.K.multiply( rpow ),
                s1.multiplier.pow( new Ratio( pow ) ) ); //TODO I dont think this is right...
        }
    }
    /**
     * code borrowed from QSAS or qunit library.
     */
    // register the units in the QSas library
    private static final SIUnits si_m = SIUnits.create("m", "meter", 1, 0, 0);
    private static final SIUnits si_kg = SIUnits.create("kg", "kilogram", 0, 1, 0);
    private static final SIUnits si_s = SIUnits.create("s", "second", 0, 0, 1);
    private static final SIUnits si_A = SIUnits.create("A", "ampere", 0, 0, 0, 1, 0);
    private static final SIUnits si_K = SIUnits.create("K", "kelvin", 0, 0, 0, 0, 1);

    private static final SIUnits Hz = SIUnits.create("Hz", "Hertz", 0, 0, -1);

    private static final SIUnits N = SIUnits.create("N", "newton", 1, 1, -2);
    private static final SIUnits Pa = SIUnits.create("Pa", "pascal", -1, 1, -2);
    private static final SIUnits J = SIUnits.create("J", "joule", 2, 1, -2);

    private static final SIUnits W = SIUnits.create("W", "watt", 2, 1, -3);
    private static final SIUnits C = SIUnits.create("C", "coulomb", 0, 0, 1, 1, 0);

    private static final SIUnits V = SIUnits.create("V", "volt", 2, 1, -3, -1, 0);

    private static final SIUnits F = SIUnits.create("F", "farad", -2, -1, 4, 2, 0);
    private static final SIUnits ohm = SIUnits.create("ohm", "ohm", 2, 1, -3, -2, 0);

    private static final SIUnits Wb = SIUnits.create("Wb", "weber", 2, 1, -2, -1, 0);
    private static final SIUnits T = SIUnits.create("T", "tesla", 0, 1, -2, -1, 0);

    private static final SIUnits H = SIUnits.create("H", "henry", 2, 1, -2, -2, 0);

    private static final SIUnits Bg = SIUnits.create("Bq", "becquerel", 0, 0, -1);
    private static final SIUnits Gy = SIUnits.create("Gy", "gray", 2, 0, -2);

    private static final SIUnits unitless = SIUnits.create("unitless", "unitless", 0, 0, 0);

    boolean isConvertable(SIUnits u) {
        return u.kg.equals(this.kg) && u.m.equals(this.m) && u.s.equals(this.s);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        if (!this.multiplier.isOne()) {
            b.append(this.multiplier);
        }
        if (!this.kg.isZero()) {
            if (this.kg.isOne()) {
                b.append(" Kg");
            } else {
                b.append(" Kg^").append(kg);
            }
        }
        if (!this.m.isZero()) {
            if (this.m.isOne()) {
                b.append(" m");
            } else {
                b.append(" m^").append(m);
            }
        }
        if (!this.s.isZero()) {
            if (this.s.isOne()) {
                b.append(" s");
            } else {
                b.append(" s^").append(s);
            }
        }
        return b.toString();
    }

    @Override
    public Datum add(Number a, Number b, Units bUnits) {
        if (bUnits instanceof SIUnits) {
            SIUnits ub = (SIUnits) bUnits;
            if (isConvertable(ub)) {
                if (ub.multiplier == this.multiplier) {
                    return this.createDatum(a.doubleValue() + b.doubleValue());
                } else {
                    return this.createDatum(a.doubleValue() + this.multiplier.divide(ub.multiplier).doubleValue() * b.doubleValue());
                }
            } else {
                throw new InconvertibleUnitsException(bUnits, this);
            }
        } else {
            return super.add(a, b, bUnits);
        }
    }

    @Override
    public Datum subtract(Number a, Number b, Units bUnits) {
        if (bUnits instanceof SIUnits) {
            SIUnits ub = (SIUnits) bUnits;
            if (isConvertable(ub)) {
                if (ub.multiplier == this.multiplier) {
                    return this.createDatum(a.doubleValue() - b.doubleValue());
                } else {
                    return this.createDatum(a.doubleValue() - this.multiplier.divide(ub.multiplier).doubleValue() * b.doubleValue());
                }
            } else {
                throw new InconvertibleUnitsException(bUnits, this);
            }
        } else {
            return super.subtract(a, b, bUnits);
        }
    }

    @Override
    public Datum multiply(Number a, Number b, Units bUnits) {
        if (bUnits instanceof SIUnits) {
            SIUnits ub = (SIUnits) bUnits;
            String newName = this.toString() + "*" + bUnits;
            SIUnits ru = create(newName, newName, this.m.add(ub.m), this.kg.add(ub.kg), this.s.add(ub.s), Ratio.one, Ratio.one, this.multiplier.multiply(ub.multiplier));
            return ru.createDatum(a.doubleValue() * b.doubleValue());
        } else {
            return super.multiply(a, b, bUnits);
        }
    }

    @Override
    public Datum divide(Number a, Number b, Units bUnits) {
        if (bUnits instanceof SIUnits) {
            SIUnits ub = (SIUnits) bUnits;
            String newName = this.toString() + "/(" + bUnits + ")";
            SIUnits ru = create(newName, newName, this.m.subtract(ub.m), this.kg.subtract(ub.kg), this.s.subtract(ub.s), Ratio.one, Ratio.one, this.multiplier.divide(ub.multiplier));
            return ru.createDatum(a.doubleValue() / b.doubleValue());
        } else {
            return super.divide(a, b, bUnits);
        }
    }


    /**
     * Cluster CDFs had "SI_Conversion" that showed how to convert to SI units.
     * Parse this string. For example: SI_conversion=<code>{@code"1.0e-3>V m^-1"}</code>
     *
     * @param si the string.
     * @return the SIUnits
     */
    public static SIUnits fromClusterCDFSIConversion( String si, String id, String desc) {
        int i = si.indexOf(">");
        RationalNumber scale = new RationalNumber(1);
        if (i > -1) {
            scale = RationalNumber.parse( si.substring(0, i) );
        }
        String su3= si.substring(i+1);
        su3= su3.replaceAll("\\*\\*", "^");
        su3= su3.replaceAll("\\*"," ");
        Scanner s= new Scanner(su3);
        s.useDelimiter( Pattern.compile("\\s+") ) ;
        SIUnits u = (SIUnits) getByName("unitless");
        while ( s.hasNext() ) {
            String c= s.next();
            String su;
            int exp=1;
            int icarot= c.indexOf("^");
            if ( icarot>-1 ) {
                exp= Integer.parseInt( c.substring(icarot+1) );
                su= c.substring(0,icarot).trim();
            } else {
                su= c.trim();
            }
            SIUnits u2= create(su);
            u= multiply( u, pow( u2, exp ) );
            
        }
        
        return multiply( id, desc, u, scale );
        
    }
    
    public static void main( String[] args ) {
        //System.err.println( SIUnits.fromClusterCDFSIConversion( "1.3>Hz", "foo", "foo" ) );
        System.err.println( SIUnits.fromClusterCDFSIConversion( "1.3>Hz*s*kg*Hz^2", "foo", "foo" ) );
    }
}