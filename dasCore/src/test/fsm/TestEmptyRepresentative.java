/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.fsm;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.das2.fsm.FileStorageModelNew;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author jbf
 */
public class TestEmptyRepresentative {

    public static void main( String[] args ) throws IOException, URISyntaxException {
        String templ= "%Y/%j/i1_av_ott_%Y%j%H%M%S_?%v.cdf";
        String base= "ftp://cdaweb.gsfc.nasa.gov/pub/istp/isis1/ott/";
        FileSystem fs= FileSystem.create(new URI(base));
        FileStorageModelNew fsm= FileStorageModelNew.create( fs, templ );
        System.err.println( fsm.getRepresentativeFile( new NullProgressMonitor() ) );
    }

}
