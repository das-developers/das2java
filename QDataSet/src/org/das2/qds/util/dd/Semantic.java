package org.das2.qds.util.dd;

import java.util.*;


class Semantic {

    static void error (String s) {
	System.out.println(s);
    }
    
    static boolean checkProgram(List<Node> program) {
	
	Set<String> env = new HashSet<String>();
	boolean programFlag = true;

	for (Node stmnt : program) {
	    boolean stmntFlag = checkStmnt(stmnt, env);
	    programFlag = programFlag && stmntFlag;
	}
	
	return programFlag;

    }

    static boolean checkStmnt (Node stmnt, Set<String>env) {
	
	switch (stmnt.getId()) {
	case ASSIGN :
	    IdentifierNode var = (IdentifierNode)stmnt.getChild(0, 2);
	    Node exp = stmnt.getChild(1, 2);
	    
	    boolean flag = checkExp(exp, env);

	    env.add(var.getName());
	    
	    return flag;

	default :
	    error("Unexpected statement: "+stmnt.getId());
	    return false;
	}
    }


    static boolean checkExp (Node exp, Set<String> env) {
	
	switch (exp.getId()) {

	case DOUBLE :
	    return true;

	case IDENT :
	    String name = ((IdentifierNode)exp).getName();
	    if (env.contains(name)) return true;
	    
	    error("Variable " + name + " not defined.");
	    return false;
	    
	case PLUS :
	    boolean flag1 = checkExp(exp.getChild(0,2), env);
	    boolean flag2 = checkExp(exp.getChild(1,2), env);
	    return flag1 && flag2;

	default :
	    error("Unexpected expression: "+exp.getId());
	    return false;
	}
    }
}



