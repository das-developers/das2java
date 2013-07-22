/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.Scrollable;

/**
 *
 * @author cwp
 */
public class QueryEditor extends JComponent implements Scrollable {

	@Override
	public Dimension getPreferredScrollableViewportSize(){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean getScrollableTracksViewportWidth(){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean getScrollableTracksViewportHeight(){
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getQuery(){
		throw new UnsupportedOperationException("Not supported yet.");

	}

	public void setQuery(String sQuery){

	}

}
