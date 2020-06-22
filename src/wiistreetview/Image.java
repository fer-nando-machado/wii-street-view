/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package wiistreetview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author fernando
 */
public class Image {
    
    String filename;
    
    public Image(String filename){
        this.filename = filename;
    }

    public BufferedImage buffer(){
        try {
            return ImageIO.read(this.getClass().getClassLoader().getResource(filename));
        } 
        catch (IOException ex) {
            Logger.getLogger(Image.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
    
}
