package wiistreetview;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;




public class Keyboard {
    
     Robot robot;
     
     public Keyboard(){
         try {
             if(robot == null){
                robot = new Robot();
             }
         } catch (AWTException e) {
             Logger.getLogger(Keyboard.class.getName()).log(Level.SEVERE, null, e);
         }
     }
     
     
     
     public void press(int code){
         robot.keyPress(code);
         robot.keyRelease(code);
     }

     public void press(char c){
         press(KeyEvent.getExtendedKeyCodeForChar(Integer.valueOf(c)));
     }
     
    public void press(String s, int delay){
        char[] cs = s.toCharArray();

        for(char c: cs){  
            press(c);
            robot.delay(delay);
        }  
    }


            

    
    
    
}
