/* File: CommandAction.java
 * Copyright (C) 2002-2003 University of Iowa
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class CommandAction implements ActionListener {
    
    private CommandBlock commandBlock;
    
    public CommandAction(CommandBlock commandBlock) {
        this.commandBlock = commandBlock;
    }
    
    public void actionPerformed(ActionEvent e) {
        try {
            FormComponent x = (FormComponent)e.getSource();
            commandBlock.execute(x.getForm());
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(de);
        }
        catch (DataFormatException dfe) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dfe);
        }
        catch (ParsedExpressionException pee) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(pee);
        }
        catch (java.lang.reflect.InvocationTargetException ite) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(ite.getCause());
        }
    }
    
}

