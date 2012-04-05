/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import javax.swing.DefaultBoundedRangeModel;

/**
 *
 * @author jbf
 */
public class ArrayDataSetBeanInfo extends SimpleBeanInfo {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            return new PropertyDescriptor[] {
                new PropertyDescriptor( "rank", QDataSet.class, "rank", null ),
                new PropertyDescriptor( "length", QDataSet.class, "length", null ),
            };
        } catch ( IntrospectionException ex ) {
            return new PropertyDescriptor[0];
        }
    }

}
