/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.fsm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.fsm.FileStorageModel;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author jbf
 */
public class TestGzSupport {
    public static void main( String[] args ) throws URISyntaxException, FileSystemOfflineException, UnknownHostException, IOException {
        //String password= JOptionPane.showInputDialog( "Password for papco@mrfrench: " );
        FileStorageModel fsm= FileStorageModel.create(
            FileSystem.create( new URI( "file:///home/jbf/temp/testgz/" ) ),
            "POLAR_H0_CEPPAD_$Y$m$d_V01.cdf" );
        DatumRange tr=  DatumRangeUtil.parseTimeRangeValid( "2000-feb-2 to 2000-feb-5" ) ;
        String[] ss= fsm.getNamesFor( tr, new NullProgressMonitor() );
        File[] ff= fsm.getFilesFor(tr);
        for ( int i=0; i<ff.length; i++ ) {
            System.err.println(ff[i]);
        }

    }

}
