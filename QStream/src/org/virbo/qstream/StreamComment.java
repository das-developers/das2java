/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.qstream;

import org.w3c.dom.Element;

/**
 * StreamComment allows comments to be put onto the stream.
 * TODO: consider that comments should be sourced by the stream producer, but what about comments from filters?
 *   That is, if I'm a filter and I produce comments, should I throw out the comments I receive?
 * @author jbf
 */
public class StreamComment implements Descriptor {

    /**
     * task progress comments are either of the form:
     *   [xx]000000<comment type='taskProgress' message='0 of 100'>   or
     *   [xx]000000<comment type='taskProgress' message='0 of -1'> for indeterminate
     * These are currently unimplemented!
     */
    public final String TYPE_TASK_PROGRESS="taskProgress";

    /**
     * log comments are of the form:
     *   [xx]000000<comment type='log:FINE' message='calc fine process'>   or
     *   [xx]000000<comment type='log:INFO' message='reading calibration'>
     * Note the log level should be requested by the client.
     */
    public final String TYPE_LOG="log:(.*)";

    private Element element;
    
    StreamComment( Element element ) {
        this.type= element.getAttribute("type");
        this.message= element.getAttribute("message");
        this.element= element;
    }

    public Element getDomElement() {
        return element;
    }

    private String type;

    String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String message;

    String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
