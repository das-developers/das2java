package edu.uiowa.physics.pw.das.util;

import java.util.*;

public class ArgumentList {
    
    int nposition;
    
    String programName;
    
    String[] positionKeys;
    
    HashMap values;
    
    HashMap descriptions;
    
    HashMap names;
    
    HashMap reverseNames;
    
    HashMap formUsed;
    
    HashMap abbrevs;
    
    HashMap isBoolean;
    
    ArrayList requireOneOfList;
    
    private String UNSPECIFIED = new String("__unspecified__");
    
    private String REFERENCEWITHOUTVALUE = new String( "__referencewithoutvalue__" );
    
    private String UNDEFINED_SWITCH = new String( "__undefinedSwitch__" );
    
    private String FALSE = new String("__false__");
    
    private String TRUE = new String("__true__");
    
    public ArgumentList(String programName) {
        this.programName= programName;
        positionKeys= new String[10];
        values= new HashMap();
        descriptions= new HashMap();
        names= new HashMap();
        reverseNames= new HashMap();
        abbrevs= new HashMap();
        formUsed= new HashMap();
        requireOneOfList= new ArrayList();
    }
    
    public String getValue(String key) {
        if ( values.containsKey(key) ) {
            return (String)values.get( key );
        } else {
            throw new IllegalArgumentException( "No such key: "+key );
        }
    }
    
    public boolean getBooleanValue(String key) {
        return values.get( key ) == this.TRUE;
    }
    
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
    
    public void requireOneOf( String[] keyNames ) {
        requireOneOfList.add(keyNames);
    }
    
    public void addOptionalPositionArgument(int position, String key, String defaultValue, String description) {
        addPositionArgument( position, key, description );
        values.put(key,defaultValue);
    }
    
    public void addSwitchArgument(String name, String abbrev, String key, String description) {
        if ( abbrev!=null && abbrev.length()>1 ) {
            throw new IllegalArgumentException( "abbrev can only be one character for "+key+": "+abbrev );
        }
        if ( abbrev==null && name==null ) {
            throw new IllegalArgumentException( "both abbrev and name are null, one must be specified" );
        }
        descriptions.put( key,  description );
        if ( abbrev!=null ) abbrevs.put( abbrev, key );
        if ( name!=null ) {
            names.put( name, key );
            reverseNames.put( key, name );
        }
        values.put( key, UNSPECIFIED );
    }
    
    public void addOptionalSwitchArgument(String name, String abbrev, String key, String defaultValue, String description) {
        addSwitchArgument( name, abbrev, key, description );
        values.put( key, defaultValue );
    }
    
    public void addBooleanSwitchArgument(String name, String abbrev, String key, String description) {
        addOptionalSwitchArgument( name, abbrev, key, FALSE, description );
    }
    
    public void printUsage() {
        String s;
        s= "Usage: "+this.programName+" ";
        for ( int i=0; i<this.nposition; i++ ) {
            Object key= positionKeys[i];
            if ( values.get(key)!=this.UNSPECIFIED ) {
                s+= "["+descriptions.get(key)+"] ";
            } else {
                s+= "<"+descriptions.get(key)+"> ";
            }
        }
        
        System.err.println(s);
        
        Set set= names.keySet();
        Iterator i= set.iterator();
        
        while ( i.hasNext() ) {
            Object name= i.next();
            Object key= names.get(name);
            s= "  ";
            Object description= descriptions.get(key);
            if ( values.get(key)!=this.UNSPECIFIED ) {
                if ( values.get(key)==this.FALSE || values.get(key)==this.TRUE ) {
                    s+= "--"+name+"   "+description;
                } else {
                    s+= "--"+name+"="+description+" ";
                }
            } else {
                s+= "--"+name+"="+description+" (required)";
            }
            System.err.println(s);
        }
        
        set= abbrevs.keySet();
        i= set.iterator();
        
        while ( i.hasNext() ) {
            Object abbrev= i.next();
            Object key= abbrevs.get(abbrev);
            s= "  ";
            Object description= descriptions.get(key);
            if ( values.get(key)!=this.UNSPECIFIED ) {
                if ( values.get(key)==this.FALSE || values.get(key)==this.TRUE ) {
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
    
    private void checkArgs() {
        boolean error= false;
        java.util.List errorList= new java.util.ArrayList(); // add strings to here
        for ( int i=0; !error & i<nposition; i++ ) {
            if ( values.get( positionKeys[i] ) == this.UNSPECIFIED ) {
                errorList.add( "Expected more positional arguments, only got "+i );
                error= true;
            }
        }
        if ( !error ) {
            Iterator i= values.keySet().iterator();
            while ( i.hasNext() ) {
                Object key= i.next();
                if ( key.equals("help") ) {
                    printUsage();
                    System.exit(-1);
                }
                if ( values.get( key )==this.UNSPECIFIED ) {
                    errorList.add( "Argument needed: --" + reverseNames.get( key ) );
                }
                if ( values.get( key )== this.REFERENCEWITHOUTVALUE ) {
                    errorList.add( "Switch requires argument: "+formUsed.get(key));
                }
                if ( values.get( key ) == this.UNDEFINED_SWITCH ) {
                    errorList.add( "Not a valid switch: "+formUsed.get(key) );
                }
            }
        }
        
        if ( !error ) {
            for ( int i=0; i<requireOneOfList.size(); i++ ) {
                String[] keys= (String[])requireOneOfList.get(i);
                boolean haveValue=false;
                for ( i=0;i<keys.length;i++ ) {
                    if ( !values.get(keys[i]).equals(UNSPECIFIED) &
                        !values.get(keys[i]).equals(UNDEFINED_SWITCH) &
                        !values.get(keys[i]).equals(REFERENCEWITHOUTVALUE) ) haveValue=true;
                }                
                if ( !haveValue ) {
                    StringBuffer list= new StringBuffer( (String)reverseNames.get( keys[0] ) );
                    for ( i=1;i<keys.length;i++ ) list.append(", "+(String)reverseNames.get( keys[0] ) );      
                    errorList.add("One of the following needs to be specified: "+list.toString());
                }
            }
        }
        
        if ( errorList.size()>0 ) {
            printUsage();
            for ( int ii=0; ii<errorList.size(); ii++ ) {
                System.err.println( errorList.get(ii) );
            }
            System.exit(-1);
        }
        
    }
    
    public void process(String[] args) {
        int iposition=0;
        String value;
        for ( int i=0; i<args.length; i++ ) {
            String key;
            if ( args[i].startsWith("-") ) {
                if ( args[i].startsWith("--") ) {
                    String name= args[i].substring(2);
                    if ( name.indexOf('=') != -1 ) {
                        name= name.substring(0,name.indexOf('='));
                    }
                    key= (String)names.get(name);
                } else {
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
                } else {
                    formUsed.put( key,args[i] );
                    if ( values.get(key) == this.FALSE || values.get(key) == this.TRUE ) { // is boolean
                        values.put( key, TRUE );
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
                        values.put( key, value );
                    }
                }
            } else {
                key= this.positionKeys[iposition];
                values.put( key, args[i] );
                iposition= iposition+1;
                formUsed.put( key, args[i] );
            }
        }
        checkArgs();
    }
    
}

