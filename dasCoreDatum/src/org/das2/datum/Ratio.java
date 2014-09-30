package org.das2.datum;

/**
 * Represent the ratio of two integers.
 * @author jbf
 */
public class Ratio {
    int numerator;
    int denominator;
    
    public Ratio( int i ) {
        this.numerator= i;
        this.denominator= 1;
    }
    
    public static Ratio one= new Ratio(1);
    
    public static Ratio zero= new Ratio(0);
    
    public Ratio( int n, int d ) {
        if ( n % d == 0 ) {
            n= n/d;
            d=1;
        }
        //TODO: GCD
        this.numerator= n;
        this.denominator= d;
    }
    
    
       
    /**
     * return the Greatest Common Divisor (GCD) of the two numbers.
     * @param a the first number
     * @param d the second number, slightly more efficient if this is smaller.
     * @return the divisor.
     */
    public static long gcd( long a, long d ) {
        if ( a<d ) {
            long t= a;  a= d;  d= t;
        }
        if ( d==0 ) {
            return a;
        }
        long r= a % d;                
        while ( r>0 ) {
            d= r;
            r= a % d;
        }    
        return d;
    }
    
    /**
     * parse aaa.bbbbEcc into Ratio.
     * @param s
     * @return 
     */
    public static Ratio create( String s ) {
        s= s.toLowerCase();
        int ipt= s.indexOf(".");
        int ien= s.length();
        int ie= s.lastIndexOf("e");
        int exp=0;
        if ( ie>-1 ) {
            exp= Integer.parseInt(s.substring(ie+1) );
            if ( ipt==-1 ) ipt= ie;
        } else {
            if ( ipt==-1 ) ipt= s.length();
        }
        int n;
        if ( ipt>0 ) {
            n= Integer.parseInt(s.substring(0,ipt) );
        } else {
            n= 0;
        }
        if ( ipt==ie ) {
            if ( exp>0 ) {
                n=n*(int)Math.pow(10,exp);
            } else if ( exp<0 ) {
                throw new IllegalArgumentException("exponent must be positive, use rational Number");
            }
            return new Ratio(n); 
        } else {
            int d;
            int np;
            if ( ipt+1<ien ) {
                d= Integer.parseInt( s.substring(ipt+1,ien) );
            } else {
                d= 0;
            }
            if ( d==0 ) {
                np= 0;
            } else {
                np= (int)Math.log10(d)+1;
            }
            n= n*(int)Math.pow(10,np) + d;
            d= (int)Math.pow(10,np);
            if ( exp>0 ) {
                n=n*10^exp;
                return new Ratio( n, d );
            } else {
                return new Ratio( n, d );
            }
        }
    }
    
    public static Ratio create( double number ) {
        
        if ( number==0 ) {
            return Ratio.zero;
        }
        if ( Double.isNaN(number) ) {
            throw new IllegalArgumentException("NaN is not supported");
        }
        
// Code below doesn't work for 0 and NaN - just check before

        long bits = Double.doubleToLongBits(number);

        long sign = bits >>> 63;
        long exponent = ((bits >>> 52) ^ (sign << 11)) - 1023;
        long fraction = bits << 12; // bits are "reversed" but that's not a problem

        long a = 1L;
        long b = 1L;

        for (int i = 63; i >= 33; i--) {
            a = a * 2 + ((fraction >>> i) & 1);
            b *= 2;
        }

        if (exponent > 0) {
            a *= 1 << exponent;
        } else {
            b *= 1 << -exponent;
        }

        if (sign == 1) {
            a *= -1;
        }
        
        long gcd= gcd( a,b );
        if ( gcd>1 ) {
            a= a/gcd;
            b= b/gcd;
        }
        
        if ( a<Integer.MIN_VALUE || a>Integer.MAX_VALUE ) {
            return create( Math.round( number/1000 ) * 1000 ); // kludge
        } else if ( b<Integer.MIN_VALUE || b>Integer.MAX_VALUE ) {
            return create( Math.round( number/1000 ) * 1000 ); // kludge
        }
        return new Ratio( (int)a,(int)b );
    }
    
    public Ratio pow( Ratio r ) {
        if ( r.denominator==1 ) {
            return new Ratio( (int)Math.pow( this.numerator, r.numerator ), (int)Math.pow( this.denominator, r.numerator ) );
        } else {
            return Ratio.create( Math.pow( this.numerator, r.doubleValue() ) / Math.pow( this.denominator, r.doubleValue() ) );
        }
    }
    
    public Ratio sqrt( ) {
        return Ratio.create( Math.sqrt(numerator)/Math.sqrt(denominator) );
    }
    
    public Ratio multiply( Ratio r ) {
        return new Ratio( this.numerator * r.numerator, this.denominator * r.denominator );
    }
    
    public Ratio divide( Ratio r ) {
        return new Ratio( this.numerator / r.numerator, this.denominator / r.denominator );        //TODO: check this
    }
    
    public Ratio add( Ratio r ) {
        return new Ratio( this.numerator * r.denominator + r.numerator * this.denominator, this.denominator * r.denominator );
    }
    
    public Ratio subtract( Ratio r ) {
        return new Ratio( this.numerator * r.denominator - r.numerator * this.denominator, this.denominator * r.denominator );    
    }
    
    public boolean isZero() {
        return this.numerator==0;
    }
    
    public boolean isOne() {
        return this.numerator==this.denominator;
    }
    
    @Override
    public boolean equals( Object o ) {
        if ( o instanceof Ratio ) {
            Ratio r= (Ratio)o;
            return r.numerator * this.denominator == r.denominator * this.numerator;
        } else if ( o instanceof Number ) {
            return ((Number)o).doubleValue() * this.denominator == this.numerator;
        } else {
            return super.equals(o);
        }
    }
    @Override
    public String toString() {
        if ( denominator==1 ) {
            return String.format( "%d", numerator );
        } else if ( denominator<100 ) {
            return String.format( "%d/%d", numerator, denominator );
        } else {
            return String.valueOf( numerator/(double)denominator );
        }
    }

    /**
     * return the double value closest to this.
     * @return the double value closest to this.
     */
    public double doubleValue() {
        return numerator / (double)denominator; 
    }
    
    public static void main( String[] args ) {
        System.err.println( Ratio.create("1.3") );
        System.err.println( Ratio.create(".13") );
        System.err.println( Ratio.create("13.") );
        System.err.println( Ratio.create("13.00") );
        System.err.println( new Ratio(4,1).sqrt() );
    }
}
