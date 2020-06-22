package wiistreetview;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.*;
import wiiremotej.*;
import wiiremotej.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;


public class WiiStreetView extends BalanceBoardAdapter
{   
    private static BalanceBoard board;
    private static JFrame graphFrame;
    private static JPanel graph;
    private static double massX;
    private static double massY;
    private static double standingMass = 0;
    
    private static int calibration = 0;
    private static final int CALIBRATION_TIMEOUT = 250;
    private static final double CENTER_RANGE = 0.125;
    
    private static final double STEP_DIFF = 20;  
    
    private static final int STEP_NONE = 0;
    private static final int STEP_LEFT = 1;
    private static final int STEP_RIGHT = 2;

    private static int step = STEP_NONE;
    private static int steps = 0;
    private static int stepTime = 0;
    private static final int STEP_TIMEOUT = 150;
    private static final int STEP_MAX = 15;
   
    private static int cameraX = 0;
    private static final int CAMERAX_TIMEOUT = 75;
    private static final double CAMERAX_RANGE = 0.25;
    
    private static int cameraY = 0;
    private static final int CAMERAY_TIMEOUT = 50;
    private static final double CAMERAY_RANGE = 0.3;

    private static double calories = 0;
    
    private static boolean shutdown = false;
    private static int shutdownTimer = 0;   
    private static final int SHUTDOWN_TIMEOUT = 150;

    private static final Keyboard KEYBOARD = new Keyboard();
    
    private static final Random RANDOM = new Random();

    private static final int WIDTH_SIZE = 640;
    private static final int HEIGHT_SIZE = 360;
    private static final int CIRCLE = 20;
    
    private static BufferedImage background;
        
    public static void main(String args[])
    {
        
        System.setProperty("bluecove.jsr82.psm_minimum_off", "true");
        //System.setProperty("bluecove.stack", "bluesoleil");
        
        //basic console logging options...
        WiiRemoteJ.setConsoleLoggingAll();
        //WiiRemoteJ.setConsoleLoggingOff();
        
        background = new Image("media/board.jpg").buffer();
        
        try
        {
            graphFrame = new JFrame();
            graphFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            graphFrame.setTitle("Wii Street View");
            graphFrame.setSize( WIDTH_SIZE+2, HEIGHT_SIZE+29);
            graphFrame.setResizable(false);
            
            graph = new JPanel()
            {
                @Override
                public void paintComponent(Graphics graphics)
                {
                    graphics.setColor(Color.BLACK);
                    graphics.clearRect(0, 0, WIDTH_SIZE, HEIGHT_SIZE);
                    graphics.fillRect(0, 0, WIDTH_SIZE, HEIGHT_SIZE);
                    
                    
                    if(board != null){
                        graphics.drawImage(background, 0, 0, WIDTH_SIZE, HEIGHT_SIZE, this);
                    }
                        
                    graphics.setColor(Color.WHITE);
                    if(board == null){
                        graphics.drawString("PRESS <SYNC> ON THE WII BALANCE BOARD.", 0, HEIGHT_SIZE);
                    }
                    else{
                        if(calibration == 0){
                            graphics.drawString("PLEASE CALIBRATE.", 0, HEIGHT_SIZE);
                        }
                        else if(calibration < CALIBRATION_TIMEOUT){
                            graphics.drawString("CALIBRATING... ("+ (int) (calibration*100/CALIBRATION_TIMEOUT) +"%)", 0, HEIGHT_SIZE);     
                        }
                        else{
                            graphics.drawString("WEIGHT: " + new DecimalFormat("#.##").format(standingMass) + "kg | CALORIES: " + new DecimalFormat("#.##").format(calories) + "cal", 0, HEIGHT_SIZE);  

                        }
                        
                        if(step == STEP_LEFT){
                            graphics.setColor(Color.RED);
                            graphics.drawRoundRect((int) (WIDTH_SIZE/(9.7d)), (int) (HEIGHT_SIZE/(7.6d)), (int) (WIDTH_SIZE/(3.7d)), (int) (HEIGHT_SIZE/(1.4d)), 60, 60);
                            graphics.fillRoundRect((int) (WIDTH_SIZE/(9.7d)), (int) (HEIGHT_SIZE/(7.6d)), (int) (WIDTH_SIZE/(3.7d)), (int) (HEIGHT_SIZE/(1.4d)), 60, 60);
                        }
                        else if(step == STEP_RIGHT){
                            graphics.setColor(Color.RED);
                            graphics.drawRoundRect((int) (6.2d * WIDTH_SIZE/(9.7d)), (int) (HEIGHT_SIZE/(7.6d)), (int) (WIDTH_SIZE/(3.7d)), (int) (HEIGHT_SIZE/(1.4d)), 60, 60);
                            graphics.fillRoundRect((int) (6.2d * WIDTH_SIZE/(9.7d)), (int) (HEIGHT_SIZE/(7.6d)), (int) (WIDTH_SIZE/(3.7d)), (int) (HEIGHT_SIZE/(1.4d)), 60, 60);
                        }
                        
                        graphics.setColor(Color.WHITE);
                        graphics.drawLine(0, HEIGHT_SIZE/2, WIDTH_SIZE, HEIGHT_SIZE/2);
                        graphics.drawLine(WIDTH_SIZE/2, 0, WIDTH_SIZE/2, HEIGHT_SIZE);
                        
                        graphics.setColor(Color.BLUE);
                        graphics.fillOval( (int)(massX * WIDTH_SIZE+(WIDTH_SIZE/2)-(CIRCLE/2)), (int)(massY * HEIGHT_SIZE+(HEIGHT_SIZE/2)-(CIRCLE/2)), CIRCLE, CIRCLE );
                    }
                    
                }
            };

            graph.setDoubleBuffered(true);           
            graphFrame.add(graph);
            graphFrame.setLocationRelativeTo(null);
            graphFrame.setVisible(true);

            //Find and connect to a Balance Board
            BalanceBoard board = null;
            
            while (board == null) {
                try {
                    board = WiiRemoteJ.findBalanceBoard();
                }
                catch(IOException | IllegalStateException | InterruptedException e) {
                    board = null;
                    e.printStackTrace();
                    System.out.println("Failed to connect board. Trying again.");
                }
            }
            
            board.addBalanceBoardListener(new WiiStreetView(board));
            board.setLEDIlluminated(true);
            
            new Sound("media/sync.wav").start();

            final BalanceBoard boardF = board;
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){boardF.disconnect();}}));
        }
        catch(HeadlessException | IllegalArgumentException | IOException e){
            e.printStackTrace();
        }
    }
    
    public WiiStreetView(BalanceBoard board)
    {
        this.board = board;
    }
    
    @Override
    public void disconnected()
    {
        System.out.println("Board disconnected... Please Wii again.");
        System.exit(0);
    }

    
    @Override
    public void massInputReceived(BBMassEvent evt) {
    	
        if(shutdown){
            shutdownTimer++;
        }

        
    	if (evt.getTotalMass() > 1) {
            double massTL = evt.getMass(MassConstants.TOP, MassConstants.LEFT);
            double massTR = evt.getMass(MassConstants.TOP, MassConstants.RIGHT);
            double massBL = evt.getMass(MassConstants.BOTTOM, MassConstants.LEFT);
            double massBR = evt.getMass(MassConstants.BOTTOM, MassConstants.RIGHT);

            double topMass = massTL + massTR;
            double bottomMass = massBL + massBR;
            double leftMass = massTL + massBL;
            double rightMass = massTR + massBR;

            double vertRange = topMass + bottomMass;
            double horizRange = rightMass + leftMass;
            
            massX = (rightMass-leftMass)/horizRange;
            massY = -(topMass-bottomMass)/vertRange;
            
            if(calibration < CALIBRATION_TIMEOUT){
                if((Math.abs(massX) < CENTER_RANGE)&&(Math.abs(massY) < CENTER_RANGE)){
                    standingMass += evt.getTotalMass();
                    calibration++;
                    
                    if(calibration == CALIBRATION_TIMEOUT){
                        standingMass = standingMass/CALIBRATION_TIMEOUT;
                        new Sound("media/start.wav").start();
                    }
                }
                else{
                    calibration = 0;
                    standingMass = 0;
                }
            }
            else{
                
                if(evt.getTotalMass() > standingMass + (standingMass*STEP_DIFF/100)){
                    if(leftMass > rightMass){
                        switchStep(STEP_LEFT);
                    }
                    else{
                        switchStep(STEP_RIGHT);
                    }
                    
                }
                else{                   
                    stepTime++;
                    if(stepTime == STEP_TIMEOUT){
                        step = STEP_NONE;
                        stepTime = 0;
                        steps = 0;
                    }
                }
                
                if(massX < -CAMERAX_RANGE){
                    cameraX++;
                    if((cameraX >= CAMERAX_TIMEOUT)&&(cameraX % 5 == 0)){
                        KEYBOARD.press('a');
                    }
                }     
                else if(massX > CAMERAX_RANGE){
                    cameraX++;
                    if((cameraX >= CAMERAX_TIMEOUT)&&(cameraX % 5 == 0)){
                        KEYBOARD.press('d');
                    }
                }
                else{
                    cameraX = 0;
                }
                
                if(massY < -CAMERAY_RANGE){
                    cameraY++;
                    if((cameraY >= CAMERAY_TIMEOUT)&&(cameraY % 5 == 0)){
                        KEYBOARD.press('s');
                    }
                }     
                else if(massY > CAMERAY_RANGE){
                    cameraY++;
                    if((cameraY >= CAMERAY_TIMEOUT)&&(cameraY % 5 == 0)){
                        KEYBOARD.press('w');
                    }
                }
                else{
                    cameraY = 0;
                }
                
                System.out.println("Camera X " + cameraX);
                System.out.println("Camera Y " + cameraY);
                                
            }


    	}
    	else {
            massX = 0;
            massY = 0;
            calibration = 0;
            calories = 0;
            standingMass = 0;
            step = STEP_NONE;
            stepTime = 0;
            steps = 0;
    	}
        
        graph.repaint();
    }
    
    private void switchStep(int s){
        if(steps == STEP_MAX){
            KEYBOARD.press(KeyEvent.VK_UP);
            step = STEP_NONE;
            stepTime = 0;
            steps = 0;
        }
        else if( (step == STEP_NONE) || ((step == STEP_LEFT)&&(s == STEP_RIGHT)) || ((step == STEP_RIGHT)&&(s == STEP_LEFT))){
            stepTime = 0;
            step = s;
            steps++;

            new Sound("media/step.wav").start();
            
            KEYBOARD.press("ol",100);
            calories = calories + 0.3d + (RANDOM.nextDouble()/10);
        }
        
    }

 
    
    @Override
    public void buttonInputReceived(BBButtonEvent evt) {
        if (evt.wasPressed()) {
            shutdown = true;
    	}
        if(evt.wasReleased()){
            shutdown = false;
            
            cameraY = -CAMERAY_TIMEOUT;
            cameraX = -CAMERAX_TIMEOUT;
            
            step = STEP_NONE;
            stepTime = 0;
            steps = 0;
            
            shutdownTimer = 0;
            if(calibration >= CALIBRATION_TIMEOUT){
                new Sound("media/step.wav").start();
                KEYBOARD.press('x');
            }
        }
        if((evt.isPressed())&&(shutdownTimer == SHUTDOWN_TIMEOUT)){
            new Sound("media/out.wav").run();
            System.exit(0);
        }
    }
    
    
}