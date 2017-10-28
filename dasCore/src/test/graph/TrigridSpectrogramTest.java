/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.graph;

import javax.swing.JFrame;
import org.das2.graph.SpectrogramRenderer;

/**
 *
 * @author jbf
 */
public class TrigridSpectrogramTest extends SpectrogramRendererDemo {
    
    public static void main(String[] args ) {
        JFrame frame= new SpectrogramRendererDemo().showFrame();
        frame.setSize( 800,800 );
    }
}
