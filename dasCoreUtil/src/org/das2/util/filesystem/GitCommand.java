/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.FileUtil;
import org.das2.util.LoggerManager;

/**
 * Wrapper for git commands.  There's a Java-based Git command, but for
 * now we will just spawn commands and get output.
 * @author jbf
 */
public class GitCommand {
    File pwd= null;
    
    public GitCommand(File pwd) {
        this.pwd= pwd;
    }
    
    /**
     * pull from the upstream repository
     * @return 0 for success.
     * @throws IOException
     * @throws InterruptedException 
     */
    public int pull() throws IOException, InterruptedException {
        
        Logger logger = LoggerManager.getLogger("jython.actions");

        String exe = "git pull";

        logger.log(Level.FINE, "running command {0}", exe);
        
        ProcessBuilder pb = new ProcessBuilder(exe.split(" "));
        pb.directory(pwd);

        File log = new File("/tmp/editor.pull.txt");

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));

        Process p = pb.start();
        p.waitFor();

        String msg = FileUtil.readFileToString(log);
        
        System.err.println(msg);
        
        return 0;
    }
    
    /**
     * 
     * @param f
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    public String diff( File f ) throws IOException, InterruptedException {
        Logger logger = LoggerManager.getLogger("jython.actions");

        String exe = "git diff "+f.getPath() ;

        logger.log(Level.FINE, "running command {0}", exe);
        
        ProcessBuilder pb = new ProcessBuilder(exe.split(" "));
        pb.directory(pwd);

        File log = new File("/tmp/editor.diff.txt"); //TODO: this will not scale as multiple people use this.

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));

        Process p = pb.start();
        p.waitFor();

        String msg = FileUtil.readFileToString(log);
        
        if ( p.exitValue()==0 ) {
            System.err.println(msg);
            return msg;
        } else {
            throw new IllegalArgumentException("git command failed: "+msg);
        }
    }
}
