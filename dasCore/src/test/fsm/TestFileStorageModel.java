/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.fsm;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.das2.components.DasProgressPanel;
import org.das2.datum.DatumRangeUtil;
import org.das2.fsm.FileStorageModel;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public class TestFileStorageModel {
    public static void main( String[] args ) throws IOException, URISyntaxException {
        FileStorageModel fsm= FileStorageModel.create( FileSystem.create( new URI( "file:///opt/project/archive/analog/DE1/" ) ), "%Y/%j/%H/DE1_10_%Y_%j_%H%M_0.DAT" );
        System.err.println( fsm.getParent() );
        ProgressMonitor pm= DasProgressPanel.createFramed("HA HA HA");
        File[] ff= fsm.getBestFilesFor( DatumRangeUtil.parseTimeRangeValid("1984-082"), pm );
        for ( File f: ff ) {
            System.err.println( f );
        }
        System.err.println( fsm.getRepresentativeFile( new NullProgressMonitor() ) );


    }
}
