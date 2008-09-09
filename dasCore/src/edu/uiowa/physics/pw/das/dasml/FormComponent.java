/* File: FormComponent.java
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

package edu.uiowa.physics.pw.das.dasml;

import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasNameException;
import java.awt.event.MouseEvent;
import edu.uiowa.physics.pw.das.*;
import org.das2.util.DnDSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author  eew
 */
public interface FormComponent {
    
    Element getDOMElement(Document document);
    
    FormBase getForm();
    
    boolean getEditingMode();
    
    void setEditingMode(boolean b);
    
    DnDSupport getDnDSupport();
    
    boolean startDrag(int x, int y, int action, MouseEvent evt);
    
    String getDasName();
    
    void setDasName(String name) throws DasNameException;
    
    DasApplication getDasApplication();
    
    void registerComponent() throws DasException;
    
    void deregisterComponent();
    
}
