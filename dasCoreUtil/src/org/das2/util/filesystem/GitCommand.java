
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
    
    Logger logger = LoggerManager.getLogger("das2.filesystem.git");

    File pwd= null;
    
    /**
     * contains the exit code and messages.
     */
    public static class GitResponse {
        int exitCode;
        public int getExitCode() {
            return exitCode;
        }
        String response;
        public String getResponse() {
            return response;
        }
        String errorResponse;
        public String getErrorResponse() {
            return errorResponse;
        }
    }
    
    public GitCommand(File pwd) {
        this.pwd= pwd;
    }
    
    /**
     * pull from the upstream repository
     * @return 0 for success.
     * @throws IOException
     * @throws InterruptedException 
     */
    public GitResponse pull() throws IOException, InterruptedException {
        
        String exe = "git pull";

        logger.log(Level.FINE, "running command {0}", exe);
        
        ProcessBuilder pb = new ProcessBuilder(exe.split(" "));
        pb.directory(pwd);

        File log = File.createTempFile( "editor.pull.", ".txt" );

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));

        Process p = pb.start();
        p.waitFor();

        String msg = FileUtil.readFileToString(log);
        
        System.err.println(msg);
        
        GitResponse result= new GitResponse( );
        result.exitCode= p.exitValue();
        result.response= msg;
        result.errorResponse= null;
        
        return result;
    }
    
    /**
     * query for the differences in the file.
     * @param f the file.
     * @return the difference formatted as unified diff.
     * @throws IOException
     * @throws InterruptedException 
     * @see UnifiedDiffUtils.parseUnifiedDiff in QDataSet
     */
    public GitResponse diff( File f ) throws IOException, InterruptedException {

        String exe = "git diff "+f.getPath() ;

        logger.log(Level.FINE, "running command {0}", exe);
        
        ProcessBuilder pb = new ProcessBuilder(exe.split(" "));
        pb.directory(pwd);

        File log = File.createTempFile( "editor.diff.", ".txt" );

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));

        Process p = pb.start();
        p.waitFor();

        String msg = FileUtil.readFileToString(log);
        
        GitResponse response= new GitResponse();
        response.exitCode= p.exitValue();
        if ( response.exitCode==0 ) {
            response.response= msg;
        } else {
            response.errorResponse= msg;
        }
        
        return response;
    }
    
    
    public GitResponse commit( File script, String msg ) throws IOException, InterruptedException {
        
        String name = script.getName();

        logger.log(Level.INFO, "pwd: {0}", script.getParentFile());

        ProcessBuilder pb = new ProcessBuilder( "git","commit","-m",msg,name );
        pb.directory( script.getParentFile() );

        logger.log(Level.FINE, "running command {0}", String.join( " ", pb.command() ) );

        pb.directory(pwd);

        File log = File.createTempFile( "editor.commit.", ".txt" );

        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.to(log));

        Process p = pb.start();
        p.waitFor();

        String logmsg = FileUtil.readFileToString(log);

        logger.info(logmsg);

        GitResponse response= new GitResponse();
        response.exitCode= p.exitValue();
        if ( response.exitCode==0 ) {
            response.response= logmsg;
        } else {
            response.errorResponse= logmsg;
        }
        
        return response;
        
    }


}
