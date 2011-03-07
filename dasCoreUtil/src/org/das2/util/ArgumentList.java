package org.das2.util;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Utility class for processing the String[] arguments passed into the main routine,
 * handing positional and switch parameters.  Also automatically generates the
 * usage documentation.
 *
 * Note in Autoplot's pngwalk, we add a parameter and positional argument with the
 * same name.  This should continue to be supported.
 */
public class ArgumentList {
    
    int nposition;
    
    String programName;
    
    String[] positionKeys;
    
    Map<String,String> values;
    
    Map<String,String> descriptions;
    
    Map<String,String> names;
    
    Map<String,String> reverseNames;
    
    Map<String,String> formUsed;
    
    Map<String,String> abbrevs;
    
    Map<String,String> isBoolean;
    
    ArrayList<String[]> requireOneOfList;
    
    /**
     * if false, then any unrecognized switch is an error.
     */
    boolean allowUndefinedSwitch= false;
    
    private String UNSPECIFIED = "__unspecified__";
    
    private String REFERENCEWITHOUTVALUE = "__referencewithoutvalue__";
    
    private String UNDEFINED_SWITCH = "__undefinedSwitch__";
    
    public String FALSE = "__false__";
    
    public String TRUE = "__true__";
    
    private static final Logger logger= Logger.getLogger( "das2.util" );
    
    /**
     * creates the processor for the program.  <tt>programName</tt> is provided
     * for the usage statement.  After creating the object, arguments are
     * specified one by one, and then the process method is called.
     */
    public ArgumentList(String programName) {
        this.programName= programName;
        positionKeys= new String[10];
        values= new HashMap();
        descriptions= new HashMap();
        names= new LinkedHashMap();
        reverseNames= new HashMap();
        abbrevs= new HashMap();
        formUsed= new HashMap();
        requireOneOfList= new ArrayList();
    }
    
    /**
     * get the value for this parameter
     * @return the parameter's value.
     * @throws IllegalArgumentException if the parameter name was never described.
     */
    public String getValue(String key) {
        if ( values.containsKey(key) ) {
            return (String)values.get( key );
        } else {
            throw new IllegalArgumentException( "No such key: "+key );
        }
    }
    
    /**
     * returns the options as a java.util.prefs.Preferences object, for
     * batch processes.  The idea is that a process which grabs default
     * settings from the user Preferences can instead get them from the command
     * line, to support batch processes.  See the Vg1pws app for an example of
     * how this is used.
     *
     * @return a Preferences object, loaded with the command line values.
     */
    public Preferences getPreferences() {
        return new AbstractPreferences(null,"") {
            protected void putSpi(String key, String value) {
                formUsed.put(key,value);
                values.put(key,value);
            }
            
            protected String getSpi(String key) {
                if ( formUsed.containsKey(key) ) {
                    return (String) values.get(key);
                } else {
                    return null;
                }
            }
            
            protected void removeSpi(String key) {
                // do nothing
            }
            
            protected void removeNodeSpi() throws BackingStoreException {
                // do nothing
            }
            
            protected String[] keysSpi() throws BackingStoreException {
                return (String[])values.keySet().toArray(new String[values.size()]);
            }
            
            protected String[] childrenNamesSpi() throws BackingStoreException {
                return new String[0];
            }
            
            protected AbstractPreferences childSpi(String name) {
                return null;
            }
            
            protected void syncSpi() throws BackingStoreException {
                // do nothing
            }
            
            protected void flushSpi() throws BackingStoreException {
                // do nothing
            }
        };
    }
    
    public boolean getBooleanValue(String key) {
        return values.get( key ) .equals( this.TRUE );
    }
    
    /**
     * Specify the ith positional argument.
     *
     * @param position the position number, 0 is the first argument position after the class name.
     * @param key the internal reference name to get the value specified.
     * @param description a short (40 character) description of the argument.
     */
    public void addPositionArgument(int position, String key, String description) {
        if ( position>nposition ) {
            throw new IllegalArgumentException( "Position arguments must be specified 0,1,2,3: position="+position );
        }
        if ( position>positionKeys.length ) {
            throw new IllegalArgumentException( "Position too big: position="+position );
        }
        nposition= position+1;
        positionKeys[position]= key;
        descriptions.put(key, description);
        values.put( key, UNSPECIFIED );
    }
    
    /**
     * requires the user specify one of these values, otherwise the usage
     * statement is printed.
     * @param keyNames an array of internal key names that identify parameters.
     */
    public void requireOneOf( String[] keyNames ) {
        requireOneOfList.add(keyNames);
    }
    
    /**
     * Specify the ith positional argument, which may be left unspecified by
     * the user.  Note that all positional arguments after this one must also be
     * optional.
     *
     * @param position the position number, 0 is the first argument position after the class name.
     * @param key the internal reference name to get the value specified.
     * @param defaultValue the value that is returned if a value is not provided by the user.
     * @param description a short (40 character) description of the argument.
     */
    public void addOptionalPositionArgument(int position, String key, String defaultValue, String description) {
        if ( key==null ) throw new IllegalArgumentException("null key");
        addPositionArgument( position, key, description );
        values.put(key,defaultValue);
        if ( defaultValue==null ) {
            if ( false ) System.err.println("breakpoint");
        }
    }
    
    /**
     * specify a named switch argument that must be specified by the user.  For example, --level=3 or -l=3
     * @param name the long parameter name, which the user may enter. e.g. "level"
     * @param abbrev short (one letter) parameter version.  e.g. "l" for -l=3
     * @param key the internal reference name to get the value specified, not necessarily but often the same as name.
     * @param description a short (40 character) description of the argument.
     */
    public void addSwitchArgument(String name, String abbrev, String key, String description) {
        if ( abbrev==null && name==null ) {
            throw new IllegalArgumentException( "both abbrev and name are null, one must be specified" );
        }
        if ( key==null ) throw new IllegalArgumentException("null key");
        descriptions.put( key,  description );
        if ( abbrev!=null ) {
            if ( abbrevs.containsKey(abbrev) ) {
                throw new IllegalArgumentException( "abbrev already used: "+abbrev );
            }
            abbrevs.put( abbrev, key );
        }
        if ( name!=null ) {
            names.put( name, key );
            reverseNames.put( key, name );
        }
        values.put( key, UNSPECIFIED );
    }

    /**
     * specify a named switch argument that may be specified by the user.  For example, --level=3 or -l=3
     * @param name the long parameter name, which the user may enter. e.g. "level"
     * @param abbrev short (one letter) parameter version.  e.g. "l" for -l=3
     * @param defaultValue value to return if the user doesn't specify.  If TRUE or FALSE is used, then the
     *     user may use a number of different inputs such as "T" or "true", and getBooleanValue can be used to read the value
     * @param key the internal reference name to get the value specified, not necessarily but often the same as name.
     * @param description a short (40 character) description of the argument.
     */
    public void addOptionalSwitchArgument(String name, String abbrev, String key, String defaultValue, String description) {
        addSwitchArgument( name, abbrev, key, description );
        if ( key==null ) throw new IllegalArgumentException("null key");
        values.put( key, defaultValue );
        if ( defaultValue==null ) {
            if ( false ) System.err.println("breakpoint");
        }
    }
    
    /**
     * specify a named switch argument that is named, and we only care whether it was used or not.  e.g. --debug
     * @param name the long parameter name, which the user may enter. e.g. "level"
     * @param abbrev short (one letter) parameter version.  e.g. "l" for -l=3
     * @param key the internal reference name to get the value specified, not necessarily but often the same as name.
     * @param description a short (40 character) description of the argument.
     */
    public void addBooleanSwitchArgument(String name, String abbrev, String key, String description) {
        if ( key.equals("commandLinePrefs") ) allowUndefinedSwitch=true;
        addOptionalSwitchArgument( name, abbrev, key, FALSE, description );
    }

    /**
     * print the usage statement out to stderr.
     */
    public void printUsage() {
        String s;
        s= "Usage: "+this.programName+" ";
        for ( int i=0; i<this.nposition; i++ ) {
            Object key= positionKeys[i];
            if ( !this.UNSPECIFIED.equals(values.get(key)) ) {
                s+= "["+descriptions.get(key)+"] ";
            } else {
                s+= "<"+descriptions.get(key)+"> ";
            }
        }
        
        System.err.println(s);
        
        Set set= names.keySet();
        Iterator<String> i= set.iterator();

        Map<String,String> abbrevsCopy= new HashMap(abbrevs);

        while ( i.hasNext() ) {
            String name= i.next();
            String key= names.get(name);
            String abbrev=null;
            for ( Entry<String,String> se: abbrevsCopy.entrySet() ) {
                if ( se.getValue().equals(name) ) {
                    abbrev= se.getKey();
                    break;
                }
            }
            if ( abbrev!=null ) {
                abbrevsCopy.remove(abbrev);
            }
            
            s= "  ";
            String description= descriptions.get(key);
            String value= values.get(key);

            if ( abbrev==null ) {
                if ( !this.UNSPECIFIED.equals(value) ) {
                    if ( this.FALSE.equals(value) || this.TRUE.equals(value) ) {
                        s+= "--"+name+"  \t"+description;
                    } else {
                        s+= "--"+name+"= \t"+description+" ";
                    }
                } else {
                    s+= "--"+name+"= \t"+description+" (required)";
                }
            } else {
                if ( !this.UNSPECIFIED.equals(value) ) {
                    if ( this.FALSE.equals(value) || this.TRUE.equals(value) ) {
                        s+= "-"+abbrev+ ", --"+name+"  \t"+description;
                    } else {
                        s+= "-"+abbrev+ ", --"+name+"= \t"+description+" ";
                    }
                } else {
                    s+= "-"+abbrev+ ", --"+name+"= \t"+description+" (required)";
                }
            }
            System.err.println(s);
        }
        
        set= abbrevsCopy.keySet();
        i= set.iterator();
        
        while ( i.hasNext() ) {
            String abbrev= i.next();
            String key= abbrevs.get(abbrev);
            s= "  ";
            String description= descriptions.get(key);
            if ( !this.UNSPECIFIED.equals(values.get(key) ) ) {
                if ( this.FALSE.equals(values.get(key)) || this.TRUE.equals(values.get(key)) ) {
                    s+= "-"+abbrev+"   \t"+description;
                } else {
                    s+= "-"+abbrev+"="+description+" ";
                }
            } else {
                s+= "-"+abbrev+"="+description+" (required)";
            }
            System.err.println(s);
        }
    }
    
    /**
     * check that the user's specified arguments are valid.
     */
    private void checkArgs() {
        boolean error= false;
        java.util.List errorList= new java.util.ArrayList(); // add strings to here
        for ( int i=0; !error & i<nposition; i++ ) {
            if ( values.get( positionKeys[i] ) == this.UNSPECIFIED ) {
                errorList.add( "Expected more positional arguments, only got "+i );
                error= true;
            }
        }
        //TODO: check for too many arguments provided by enduser!
        if ( !error ) {
            Iterator i= values.keySet().iterator();
            while ( i.hasNext() ) {
                Object key= i.next();
                if ( key==null ) {
                    System.err.println("TODO: handle this case whereever it's coming from: key==null");
                    continue;
                }
                if ( key.equals("help") || key.equals("--help" ) ) { // kludge
                    printUsage();
                    System.exit(-1);
                }
                if ( values.get( key )==this.UNSPECIFIED ) {
                    errorList.add( "Argument needed: --" + reverseNames.get( key ) );
                }
                if ( values.get( key )== this.REFERENCEWITHOUTVALUE ) {
                    errorList.add( "Switch requires argument: "+formUsed.get(key));
                }
                if ( values.get( key ) == this.UNDEFINED_SWITCH && !allowUndefinedSwitch ) {
                    errorList.add( "Not a valid switch: "+formUsed.get(key) );
                }
            }
        }
        
        if ( !error ) {
            for ( int i=0; i<requireOneOfList.size(); i++ ) {
                String[] keys= (String[])requireOneOfList.get(i);
                boolean haveValue=false;
                for ( int j=0;j<keys.length;j++ ) {
                    if ( !values.get(keys[j]).equals(UNSPECIFIED) &
                            !values.get(keys[j]).equals(UNDEFINED_SWITCH) &
                            !values.get(keys[j]).equals(REFERENCEWITHOUTVALUE) ) haveValue=true;
                }
                if ( !haveValue ) {
                    StringBuffer list= new StringBuffer( (String)reverseNames.get( keys[0] ) );
                    for ( int j=1;j<keys.length;j++ ) list.append(", "+(String)reverseNames.get( keys[j] ) );
                    errorList.add("One of the following needs to be specified: "+list.toString());
                }
            }
        }
        
        if ( errorList.size()>0 ) {
            printUsage();
            System.err.println( "" );
            for ( int ii=0; ii<errorList.size(); ii++ ) {
                System.err.println( errorList.get(ii) );
            }
            System.exit(-1);
        }
        
    }
    
    /**
     * return a Map of all the specified values.  The keys are all the internal
     * String keys, and the values are all Strings.
     * @return a Map of the specified values, including defaults.
     */
    public Map getMap() {
        return new HashMap( values );
    }
    
    /**
     * returns a Map of optional arguments that were specified, so you can see
     * exactly what was specified.
     * @return a Map of the specified values, without defaults.
     */
    public Map getOptions() {
        HashMap result= new HashMap();
        List exclude= Arrays.asList( positionKeys );
        for ( Iterator i=values.keySet().iterator(); i.hasNext(); ) {
            Object key= (String)i.next();
            if( !exclude.contains(key) && formUsed.containsKey(key) ) {
                result.put( key, values.get(key) );
            }
        }
        return result;
    }
    
    private int processSwitch( String[] args, int i ) {
        String key;
        if ( args[i].startsWith("--") ) {
            String name= args[i].substring(2);
            if ( name.indexOf('=') != -1 ) {
                name= name.substring(0,name.indexOf('='));
            }
            key= (String)names.get(name);
        } else {
            // TODO: should support several abbrevs: e.g.,  -xvf (Throw exception if ambiguous)
            String abbrev= args[i].substring(1);
            if ( abbrev.indexOf('=') != -1 ) {
                abbrev= abbrev.substring(0,abbrev.indexOf('='));
            }
            key= (String)abbrevs.get(abbrev);
        }
        
        if ( key==null ) {
            key= args[i];
            values.put( key, this.UNDEFINED_SWITCH );
            formUsed.put( key, args[i] );
            logger.finer("undefined switch: "+key);
        } else {
            String value;
            formUsed.put( key,args[i] );
            if ( values.get(key) == this.FALSE || values.get(key) == this.TRUE ) { // is boolean
                value= TRUE;
                if ( args[i].indexOf('=')!= -1 ) {
                    value= args[i].substring( args[i].indexOf('=')+1 );
                    if ( value.equals("t") || value.equals("true") || value.equals("y") || value.equals("yes") ) {
                        value= TRUE;
                    } else if ( value.equals("f") || value.equals("false") || value.equals("n") || value.equals("no") ) {
                        value= FALSE;
                    }
                }
                values.put( key, value );
            } else {
                if ( args[i].indexOf('=') != -1 ) {
                    value= args[i].substring( args[i].indexOf('=')+1 );
                } else {
                    if ( i+1 < args.length && ( ! args[i+1].startsWith("-") ) ) {
                        value= args[i+1];
                        i= i+1;
                    } else {
                        value= this.REFERENCEWITHOUTVALUE;
                    }
                }
                if ( value.startsWith("\"") ) {
                    value= value.substring(1,value.length()-2);
                }
                logger.finer("switch key: "+key+"="+value);
                values.put( key, value );
            }
        }
        return i;
    }
    
    /**
     * given the specification, process the argument list.  If the list is in error, the
     * usage statement is generated, and the System.exit is called (sorry!).  Otherwise
     * the method returns and getValue() may be used to retrieve the values.
     *
     * Again, note that System.exit may be called.  This is probably a bad idea and another
     * method will probably be added that would return true if processing was successful.
     *
     * @param args as in public static void main( String[] args ).
     */
    public void process(String[] args) {
        
        StringBuffer sb= new StringBuffer();
        for ( int i=0; i<args.length; i++ ) {
            sb.append(args[i]);
            sb.append(" ");
        }
        logger.finer("args: "+sb.toString());
        int iposition=0;
        
        for ( int i=0; i<args.length; i++ ) {
            if ( args[i].startsWith("-") ) {
                i= processSwitch( args, i );
            } else {
                String key;
                String value;
                key= this.positionKeys[iposition];
                if ( key==null ) {
                    System.err.println( "\nWarning: position value found when position value was not expected: "+ args[i] + "\n" );
                }
                logger.finer("position key: "+key+"="+args[i]);
                String vv= args[i];
                if ( vv.startsWith("\"") ) {
                    vv= vv.substring(1,vv.length()-2);
                }
                values.put( key, args[i] );
                iposition= iposition+1;
                formUsed.put( key, args[i] );
            }
        }
        checkArgs();
    }
    
    /**
     * see Vg1pws app for example use.
     */
    public void printPrefsSettings() {
        String s;
        s= "Explicit Settings: \n";
        s+= this.programName+" ";
        
        for ( int i=0; i<this.nposition; i++ ) {
            Object key= positionKeys[i];
            if ( formUsed.get(key)!=null ) {
                s+= formUsed.get(key);
            }
        }
        
        Set set= names.keySet();
        Iterator i= set.iterator();
        
        while ( i.hasNext() ) {
            Object name= i.next();
            Object key= names.get(name);
            String value= (String)formUsed.get(key);
            if ( value !=null ) {
                if ( value==this.TRUE ) {
                    s+= "--"+name;
                } if ( value==this.FALSE ) {
                    // do nothing
                } else {
                    s+= "--"+name+"="+value+" ";
                }
            }
        }
        
        System.err.println(s);
    }
    
}

