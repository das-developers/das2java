/* File: HorizontalSlicerMouseModule.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.event;

import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.DataSetConsumer;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.Renderer;
/**
 *
 * @author  jbf
 */
public class AngleSlicerMouseModule extends MouseModule {
    
    private DasAxis xaxis;
    private DasAxis yaxis;
    
    private TableDataSetConsumer dataSetConsumer;
        
    private DataPointSelectionEvent de;
    
    private javax.swing.event.EventListenerList listenerList =  null;
    
    public AngleSlicerMouseModule( DasPlot parent, TableDataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        this( parent, (DataSetConsumer)dataSetConsumer, xaxis, yaxis );
    }
    
    protected AngleSlicerMouseModule(DasPlot parent, DataSetConsumer dataSetConsumer, DasAxis xaxis, DasAxis yaxis) {
        super( parent, new AngleSelectionDragRenderer(), "Angle Slice" );
        
        if (!(dataSetConsumer instanceof TableDataSetConsumer)) {
            throw new IllegalArgumentException("dataSetConsumer must be an XTaggedYScanDataSetConsumer");
        }
        this.dataSetConsumer= ( TableDataSetConsumer)dataSetConsumer;
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.de= new DataPointSelectionEvent(this,null,null);
        
    }
    
//    public static AngleSlicerMouseModule create(DasPlot parent) {
//        DasAxis xaxis= parent.getXAxis();
//        DasAxis yaxis= parent.getYAxis();
//        return new AngleSlicerMouseModule(parent,parent,xaxis,yaxis);
//    }
    
    public static AngleSlicerMouseModule create(Renderer renderer)
    {
        DasPlot parent= renderer.getParent();
        DasAxis xaxis= parent.getXAxis();
        DasAxis yaxis= parent.getYAxis();
        return new AngleSlicerMouseModule(parent,renderer,xaxis,yaxis);
    }

    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {
        MouseBoxEvent e= (MouseBoxEvent)e0;
        
    }
    
}
