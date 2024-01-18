
package org.das2.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.Units;
import static org.das2.graph.Renderer.CONTROL_KEY_COLOR;
import static org.das2.graph.Renderer.CONTROL_KEY_DRAW_ERROR;
import static org.das2.graph.Renderer.CONTROL_KEY_FILL_COLOR;
import static org.das2.graph.Renderer.CONTROL_KEY_FILL_DIRECTION;
import static org.das2.graph.Renderer.CONTROL_KEY_FILL_TEXTURE;
import static org.das2.graph.Renderer.CONTROL_KEY_LINE_THICK;
import static org.das2.graph.Renderer.CONTROL_KEY_MODULO_Y;
import static org.das2.graph.Renderer.CONTROL_KEY_SPECIAL_COLORS;
import static org.das2.graph.Renderer.CONTROL_KEY_SYMBOL;
import static org.das2.graph.Renderer.CONTROL_KEY_SYMBOL_SIZE;
import static org.das2.graph.Renderer.decodeDatum;
import static org.das2.graph.Renderer.decodeFillStyle;
import static org.das2.graph.Renderer.decodePlotSymbolControl;
import static org.das2.graph.Renderer.encodeBooleanControl;
import static org.das2.graph.Renderer.encodeColorControl;
import static org.das2.graph.Renderer.encodeDatum;
import static org.das2.graph.Renderer.encodeFillStyle;
import static org.das2.graph.Renderer.encodePlotSymbolControl;
import static org.das2.graph.Renderer.formatControl;
import static org.das2.graph.SeriesRenderer.CONTROL_KEY_BACKGROUND_THICK;
import static org.das2.graph.SeriesRenderer.CONTROL_KEY_FILL_STYLE;
import org.das2.qds.DataSetUtil;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.util.GrannyTextRenderer;

/**
 * render a 2-D poly (typically triangle) mesh.  This was introduced just 
 * to identify this schema.  This
 * renderer needs rank 3 data with 2 bundled datasets, the
 * points in X-Y data space and the triangles (or polys) which connect them.  
 * When a rank 1 dataset with a triangleMesh as DEPEND_0 is passed in, then 
 * colors or text will be drawn.
 * 
 * Note, QDataSet should not support a join of non-qube datasets, so all 
 * polygons should have the same length (all triangles or all four-sided polys).
 * Most codes will work fine if this is not the case.
 * 
 * Applications for this include marking data and drawing tesselations.
 * 
 * 
 * 
 * @author jbf
 */
public class PolyMeshRenderer extends Renderer {
    
    /**
     * return true if the renderer accepts data in this form.  
     * @param ds
     * @return 
     */
    public static boolean acceptsData( QDataSet ds ) {
        if ( ds.rank()==1 ) {
            QDataSet tris= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( tris==null ) {
                return false;
            } else {
                return Schemes.isPolyMesh(tris);
            }
        } else {
            return Schemes.isPolyMesh(ds);
        }
    }

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param ds the dataset to autorange.
     * @return a bounds dataset, where result[0,:] is the xrange and result[1,:] is the yrange.
     */
    public static QDataSet doAutorange( QDataSet ds ) {
        QDataSet tris;
        if ( ds.rank()==1 ) {
            tris= (QDataSet) ds.property(QDataSet.DEPEND_0);
        } else {
            tris= ds;
        }
        QDataSet xy= tris.slice(0);
        
        QDataSet xrange= Ops.extent(Ops.slice1( xy,0 ));
        QDataSet yrange= Ops.extent(Ops.slice1( xy,1 ));
        
        xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
        yrange= Ops.rescaleRangeLogLin( yrange, -0.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }
    
    private Color color= Color.LIGHT_GRAY;
    private double lineThick= 1.;
    private boolean fill = true;
    private String fillTexture= "";
    private PsymConnector lineStyle= PsymConnector.SOLID;
        
    @Override
    public void setControl(String s) {
        super.setControl(s);
        color= getColorControl( CONTROL_KEY_COLOR, color );
        lineThick= getDoubleControl( CONTROL_KEY_LINE_THICK, lineThick );
        fill= getBooleanControl( "fill", fill );
        fillTexture= getControl( CONTROL_KEY_FILL_TEXTURE, fillTexture );
        lineStyle= decodePlotSymbolConnectorControl( getControl( CONTROL_KEY_LINE_STYLE, encodePlotSymbolConnectorControl(lineStyle) ), lineStyle );
    }

    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( CONTROL_KEY_COLOR, encodeColorControl(color) );
        controls.put( CONTROL_KEY_LINE_THICK, String.valueOf(lineThick) );
        controls.put( "fill", encodeBooleanControl(fill) );
        controls.put( CONTROL_KEY_FILL_TEXTURE, fillTexture );
        controls.put( CONTROL_KEY_LINE_STYLE,  encodePlotSymbolConnectorControl(lineStyle) );
        return formatControl(controls);
    }  
    
    @Override
    public void render(Graphics2D g, DasAxis xaxis, DasAxis yaxis) {
        
        QDataSet ds= getDataSet();
        
        if ( ds==null ) return;
        
        QDataSet tri, xy;
        QDataSet dependentVariable;
        
        if ( Schemes.isPolyMesh(ds) ) {
            tri= ds.slice(1);
            xy= ds.slice(0);
            dependentVariable= null;
        } else {
            dependentVariable= ds;
            ds= (QDataSet) ds.property( QDataSet.DEPEND_0 );
            if ( ds==null || !Schemes.isPolyMesh(ds) ) {
                throw new IllegalArgumentException("expected DEPEND_0 to be a polyMesh");
            }
            tri= ds.slice(1);
            xy= ds.slice(0);
        }
        
        g.setColor( color );
               
        double[] xx= new double[xy.length()];
        double[] yy= new double[xy.length()];
        
        Units xunits= SemanticOps.getUnits(xy.slice(0).slice(0));
        Units yunits= SemanticOps.getUnits(xy.slice(0).slice(1));
        
        for ( int i=0; i<xy.length(); i++ ) {
            xx[i]= xaxis.transform( xy.value(i,0), xunits );
            yy[i]= yaxis.transform( xy.value(i,1), yunits );
        }
        
        Color[] cc= null;
        String[] ss= null;
        
        if ( dependentVariable!=null ) {
            Units zunits= SemanticOps.getUnits(dependentVariable);
            if ( zunits==Units.rgbColor ) {
                cc= new Color[tri.length()];
                for ( int i=0; i<cc.length; i++ ) {
                    cc[i]= new Color((int)dependentVariable.value(i));
                }
            } else { 
                ss= new String[tri.length()];
                for ( int i=0; i<ss.length; i++ ) {
                    ss[i]= DataSetUtil.toString(dependentVariable.slice(i));
                }
            }
        } else {
            cc= new Color[tri.length()];
            for ( int i=0; i<cc.length; i++ ) {
                cc[i]= color;
            }
        }
        
        g.setStroke( lineStyle.getStroke( (float) lineThick ) );
        
        for ( int i=0; i<tri.length(); i++ ) {
            QDataSet tri1= tri.slice(i);
            if ( cc!=null ) {
                g.setColor(cc[i]);
                GeneralPath gp= new GeneralPath();
                int k= (int)tri1.value(tri1.length()-1);
                gp.moveTo( xx[k], yy[k] );
                for ( int j=0; j<tri1.length(); j++ ) {
                    k= (int)tri1.value(j);
                    gp.lineTo( xx[k], yy[k] );
                }
                if ( fill ) {
                    if ( fillTexture.length()>0 ) {
                        GraphUtil.fillWithTexture( g, gp, cc[i], fillTexture);
                    } else {
                        g.fill(gp);
                    }
                }
                if ( lineStyle!=PsymConnector.NONE ) {
                    g.draw(gp);
                }
            } else if ( ss!=null ) {
                // calculate the midpoint
                double x= 0;
                double y= 0;
                for ( int j=0; j<tri1.length(); j++ ) {
                    int k= (int)tri1.value(j);
                    x+= xx[k];
                    y+= yy[k];
                }
                x/= tri1.length();
                y/= tri1.length();
                GrannyTextRenderer gtr= new GrannyTextRenderer();
                gtr.setString(g, ss[i]);
                gtr.draw( g, (int)(x-gtr.getWidth()/2), (int)(y+gtr.getAscent()/2) );
            }
        }

    }
}
