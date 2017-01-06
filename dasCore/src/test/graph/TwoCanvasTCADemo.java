/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.das2.DasNameException;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasColumn;
import org.das2.graph.DasPlot;
import org.das2.graph.DasRow;
import org.das2.graph.GraphUtil;
import org.das2.graph.Renderer;
import org.das2.graph.SeriesRenderer;
import org.virbo.dataset.AbstractQFunction;
import org.virbo.dataset.BundleDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DRank0DataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class TwoCanvasTCADemo {

    private static final Logger logger= Logger.getLogger("TwoCanvasTCADemo");
    
    public static void main( String[] args ) throws ParseException {
                
        
        int width = 500;
        int height = 400;

        JPanel panel= new JPanel();
        panel.setLayout( new BoxLayout( panel, BoxLayout.X_AXIS ) );

        final DasCanvas canvas1 = new DasCanvas(width, height);
        try {
            canvas1.setDasName("canvas1");
        } catch (DasNameException ex) {
            logger.log(Level.SEVERE, null, ex);  // Grr--Why is this a checked exception???
        }
        canvas1.setAntiAlias(true);
        panel.add(canvas1 );

        final DasCanvas canvas2 = new DasCanvas(width, height);
        try {
            canvas2.setDasName("canvas2");
        } catch (DasNameException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        canvas2.setAntiAlias(true);
        panel.add(canvas2 );
        
        // read data
        QDataSet yds = Ops.sin( Ops.linspace(0,10,1000) );
        QDataSet tds= Ops.timegen( "2010-01-01T00:00", "1 s", 1000 );

        QDataSet ds= Ops.link( tds, yds );

        // here's some old das2 autoranging, works for this case
        DasAxis xaxis = GraphUtil.guessXAxis(ds);
        DasAxis yaxis = GraphUtil.guessYAxis(ds);

        final DasPlot plot = new DasPlot( xaxis, yaxis );

        JPanel p= new JPanel();
        p.setLayout( new BoxLayout( p, BoxLayout.Y_AXIS ) );
        
        JButton b= new JButton("Move Plot");
        b.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DasCanvas c= plot.getCanvas();
                DasCanvas nc; // new canvas
                if ( c==canvas1 ) {
                    nc= canvas2;
                } else {
                    nc= canvas1;
                }
                c.remove(plot);
                nc.add(plot,plot.getRow(),plot.getColumn());
            }
        });
        p.add( b );

        JButton bpng= new JButton("Write PNG");
        bpng.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable run= new Runnable() {
                    @Override
                    public void run() {
                        try {
                            plot.getCanvas().writeToPng("/tmp/TwoCanvasTCADemo.png");
                        } catch (IOException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                };
                new Thread(run,"createPng").start();
            }
        });
        p.add( bpng );

        JButton bstat= new JButton("Dump Stats");
        bstat.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Runnable run= new Runnable() {
                    @Override
                    public void run() {
                        System.err.println("canvas1: " + canvas1.isPendingChanges() );
                        System.err.println("canvas2: " + canvas2.isPendingChanges() );
                    }
                };
                new Thread(run,"createPng").start();
            }
        });
        p.add( bstat );
        
        panel.add(p);
        
        // here's autoplot as of 2005
        Renderer r= GraphUtil.guessRenderer(ds);
        plot.addRenderer( r );

        // ugh.  I need to make antialiased the default.  Right now it reads the property from $HOME/.dasrc
        if ( r instanceof SeriesRenderer ) {
            ((SeriesRenderer)r).setAntiAliased(true);
        }

        xaxis.setTcaFunction( new AbstractQFunction() {

            @Override
            public QDataSet value(QDataSet parm) {

                if ( parm.rank()==1 ) {
                    parm= new JoinDataSet(parm);
                } else {
                    throw new IllegalArgumentException("rank should be 1");
                }

                QDataSet result= values( parm );

                return result.slice(0);
            }

            @Override
            public QDataSet values(QDataSet parms) {

                BundleDataSet outbds1= BundleDataSet.createRank1Bundle();

                //TODO: this example should use bundle descriptor, and then just set BUNDLE_1 property.

                DDataSet sec= DDataSet.createRank1(parms.length());
                sec.putProperty( QDataSet.LABEL, "Sec" );
                DDataSet rand1= DDataSet.createRank1(parms.length());
                rand1.putProperty( QDataSet.LABEL, "Rand" );
                rand1.putProperty( QDataSet.FORMAT, "%5.2f" );
                DDataSet rand2= DDataSet.createRank1(parms.length());
                rand2.putProperty( QDataSet.LABEL, "Rand2" );
                rand2.putProperty( QDataSet.FORMAT, "%5.3f" );

                for ( int i=0; i<parms.length(); i++ ) {
                    QDataSet parm= parms.slice(i);

                    QDataSet time= parm.slice(0);

                    sec.putValue( i, Ops.mod( time, DataSetUtil.asDataSet(3600,Units.seconds) ).value() );
                    rand1.putValue(i, Ops.randu(1).slice(0).value() );
                    rand2.putValue( i,Ops.randu(1).slice(0).value() );

                }

                outbds1.bundle(sec);
                outbds1.bundle(rand1);
                outbds1.bundle(rand2);

                return outbds1;
            }

            @Override
            public QDataSet exampleInput() {
                BundleDataSet inbds= BundleDataSet.createRank0Bundle();
                DRank0DataSet dd= DataSetUtil.asDataSet(0,Units.t2000);
                dd.putProperty(QDataSet.LABEL, "Time") ;
                inbds.bundle( dd );
                return inbds;
            }

        } );

        xaxis.setDrawTca(true);

        canvas1.add( plot, DasRow.create( canvas1, null, "0%+2em", "100%-4em" ),
                DasColumn.create( canvas1, null, "0%+5em", "100%-7em" ) );

        JFrame frame= new JFrame();
        frame.getContentPane().add( panel );
        frame.pack();

        frame.setVisible(true);

    }
}