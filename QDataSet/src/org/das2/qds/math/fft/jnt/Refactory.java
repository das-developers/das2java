/*
 * Refactory.java
 *
 * Created on November 29, 2004, 10:58 PM
 */

package org.das2.qds.math.fft.jnt;

import java.io.*;
import java.util.regex.*;

/**
 *
 * @author  Jeremy
 */
public class Refactory {
     
    // these are the variable names that will be affected
    //static String vars="(in|out)"; // used for Complex
    static String vars="(newdata|data)"; // used for Real
    
    /*
     * replaces  "double data[]" with "ComplexArray.Double data"
     */
    public static void replaceComplexArrayDoubleDeclaration( String filename, String outfilename ) throws IOException {
        BufferedReader in= new BufferedReader( new FileReader(filename) );
        BufferedWriter out= new BufferedWriter( new FileWriter(outfilename) );
        String line;

        Pattern setPattern= Pattern.compile("(.*)(double\\s*)"+vars+"(\\[\\])"+"(.*)");
        Matcher m;
        int iline=0;
        while (( line=in.readLine())!=null ) {
            iline++;
            if ((m= setPattern.matcher(line)).matches() ) {
                String newLine= m.group(1)+"ComplexArray.Double "+m.group(3)+m.group(5);
                out.write(newLine);
                System.err.println(newLine);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();

    }
    
    public static void setArrayImag( String filename, String outfilename ) throws IOException {
        BufferedReader in= new BufferedReader( new FileReader(filename) );
        BufferedWriter out= new BufferedWriter( new FileWriter(outfilename) );
        String line;
        Pattern setPattern= Pattern.compile("(\\s*)"+vars+"\\[(.*)\\s*\\+\\s*1\\s*\\]\\s*=\\s*(.*)\\s*;(.*)");
        Matcher m;
        int iline=0;
        while (( line=in.readLine())!=null ) {
            iline++;
            //if ( iline>836 ) {
            //    System.out.println(line);
            //}
            if ((m= setPattern.matcher(line)).matches() ) {
                String newLine= m.group(1)+m.group(2)+".setImag("+m.group(3)+","+m.group(4)+");"+m.group(5);
                out.write(newLine);
                System.err.println(newLine);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();
    }
    
     public static void setArrayReal( String filename, String outfilename ) throws IOException {
        BufferedReader in= new BufferedReader( new FileReader(filename) );
        BufferedWriter out= new BufferedWriter( new FileWriter(outfilename) );
        String line;
        Pattern setPattern= Pattern.compile("(\\s*)"+vars+"\\[(.*)\\s*\\]\\s*=\\s*(.*)\\s*;(.*)");
        Matcher m;
        while (( line=in.readLine())!=null ) {
            if ((m= setPattern.matcher(line)).matches() ) {
                String newLine= m.group(1)+m.group(2)+".setReal("+m.group(3)+","+m.group(4)+");"+m.group(5);
                out.write(newLine);
                System.err.println(newLine);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();
    }
     
     
     public static void getArrayImag( String filename, String outfilename ) throws IOException {
        BufferedReader in= new BufferedReader( new FileReader(filename) );
        BufferedWriter out= new BufferedWriter( new FileWriter(outfilename) );
        String line;
        Pattern setPattern= Pattern.compile("(.*)"+vars+"\\[(.*)\\s*\\+\\s*1\\s*\\](.*)");
        Matcher m;
        while (( line=in.readLine())!=null ) {
            if ((m= setPattern.matcher(line)).matches() ) {
                String newLine= m.group(1)+m.group(2)+".getImag("+m.group(3)+")"+m.group(4);
                out.write(newLine);
                System.err.println(newLine);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();
    }
     
     public static void getArrayReal( String filename, String outfilename ) throws IOException {
        BufferedReader in= new BufferedReader( new FileReader(filename) );
        BufferedWriter out= new BufferedWriter( new FileWriter(outfilename) );
        String line;
        Pattern setPattern= Pattern.compile("(.*)"+vars+"\\[(.*)\\s*\\](.*)");
        Matcher m;
        while (( line=in.readLine())!=null ) {
            if ((m= setPattern.matcher(line)).matches() ) {                
                String newLine= m.group(1)+m.group(2)+".getReal("+m.group(3)+")"+m.group(4);
                out.write(newLine);
                System.err.println(newLine);
                out.newLine();
            } else {
                out.write(line);
                out.newLine();
            }
        }
        out.close();
    }
     
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        //String filename=Refactory.class.getResource("RealDoubleFFT.java").getFile();
        String filename="C:\\Documents and Settings\\Jeremy\\Desktop\\das2Brief\\working\\edu\\uiowa\\physics\\pw\\das\\math\\fft\\jnt\\RealDoubleFFT_Even.java";
        String outfilename1= filename+".1.java";
        String outfilename2= filename+".2.java";
        replaceComplexArrayDoubleDeclaration(filename,outfilename1);
        replaceComplexArrayDoubleDeclaration(outfilename1,outfilename2);
        setArrayImag(outfilename2,outfilename1);
        setArrayImag(outfilename1,outfilename2);
        setArrayImag(outfilename2,outfilename1);
        setArrayReal(outfilename1,outfilename2);
        setArrayReal(outfilename2,outfilename1);
        getArrayImag(outfilename2,outfilename1);
        getArrayImag(outfilename1,outfilename2);
        getArrayReal(outfilename1,outfilename2);
        getArrayReal(outfilename2,outfilename1);
        getArrayReal(outfilename1,filename+".java");
        System.out.println("result is in "+filename+".java");
        System.out.flush();
        
    }
    
}
