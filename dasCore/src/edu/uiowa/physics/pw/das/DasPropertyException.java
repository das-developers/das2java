/* File: DasPropertyException.java
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
package edu.uiowa.physics.pw.das;

public class DasPropertyException extends edu.uiowa.physics.pw.das.DasException {
    
    public static final class MessageType {
        private final String message;
        private MessageType(String message) {
            this.message = message;
        }
        public String toString() {
            return message;
        }
    }
    
    public static final MessageType NOT_DEFINED
        = new MessageType("The property <pty-name> is undefined for <obj-name>.");
    public static final MessageType READ_ONLY
        = new MessageType("The property <pty-name> of <obj-name> is read-only.");
    public static final MessageType TYPE_MISMATCH
        = new MessageType("Type mismatch: <pty-name> of <obj-name> cannot be set to <value>");
    public static final MessageType NOT_INDEXED
        = new MessageType("The property <pty-name> of <obj-name> is not an indexed property");
    
    private String propertyName;
    private String objectName;
    private String value;
    private MessageType type;
    
    public DasPropertyException(MessageType type, String propertyName, String objectName, String value) {
        this.type = type;
	this.propertyName = propertyName;
	this.objectName = objectName;
        this.value = value;
    }
    
    public DasPropertyException(MessageType type, String propertyName, String objectName) {
        this(type, propertyName, objectName, null);
    }
    
    public DasPropertyException(MessageType type) {
        this(type, null, null);
    }

    public String getPropertyName() {
	return propertyName;
    }

    public void setPropertyName(String name) {
	propertyName = name;
    }

    public String getObjectName() {
	return objectName;
    }

    public void setObjectName(String name) {
	objectName = name;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setMessageType(MessageType type) {
        this.type = type;
    }
    
    public String getMessage() {
	String message = type.toString();
        String objStr = objectName == null ? "<unknown>" : objectName;
        String ptyStr = propertyName == null ? "<unknown>" : propertyName;
        String valueStr = value == null ? "<unknown>" : value;
        return message.replaceAll("<obj-name>", objStr)
                      .replaceAll("<pty-name>", ptyStr)
                      .replaceAll("<value>", valueStr);
    }
}
