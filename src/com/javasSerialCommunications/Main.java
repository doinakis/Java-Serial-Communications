package com.javasSerialCommunications;

import ithakimodem.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Main {

    public static void main(String[] args) throws IOException{

        Modem modem = new Modem(80000);
        String modemName = "ithaki";
//        int echoExpTime = 4;
        String imageCode = "M3263";
        String cam = "PTZ"; // or CAM = "FIX" or "PTZ" or ""
        String dir = "U";
        String size = "S";
        String imgLocation = "./imgPTZUL.jpg";
        constructImageCode(imageCode,cam,dir,size);
        String gpsCode = "P9819";
        //modem.setTimeout(2000);
        openModem(modem,modemName);
        getGPSMark(modem,gpsCode);
//        getImage(modem,imageCode,cam,dir,size,imgLocation);
//        //List<Long> times = echoPacketResponseTime(modem,"E7936\r",1);
        modem.close();

    }

    /**
     * Method that initializes a connection with the virtual modem
     * @param modem a modem class
     * @param modemName the name of the modem to connect to (In this case ithaki)
     */
    public static void openModem(Modem modem,String modemName){
        try{

            if(!modem.open(modemName)) throw new customExceptionMessage("Could not open to modem.");
            printHelloMessage(modem);
        }catch(Exception e){
            System.out.println(e);
        }
    }

    /**
     * Method that prints the Greetings message that ithaki modems send at first connection with it
     * @param modem a modem class
     */
    public static void printHelloMessage(Modem modem){
        int characterReceived,counter=0;
        char[] endSequence = {'\r','\n','\n','\n'};
        for(;;){
            try{
                characterReceived = modem.read();

                if ((char)characterReceived == endSequence[counter]) counter += 1;
                else counter = 0;

                if(characterReceived == -1) throw new customExceptionMessage("Modem disconnected");
                System.out.print((char)characterReceived);
            }catch (Exception e){
                System.out.println(e);
                return;
            }
            if (counter == endSequence.length) break;
        }
    }

    /**
     * Method that calculates the response times of the ithaki server in a certain period of time
     * @param modem a modem class
     * @param echoCode the echo code for the particular date and time provided by ithaki lab
     * @param time how long the experiment will continue asking ithaki server for echo packets (in minutes)
     * @return a list with the response times of all the echo packets requested, as response time is defined the time
     *          that the last character of the echo packet is received
     */
    public static List<Long> echoPacketResponseTime(Modem modem,String echoCode,int time){
        List<Long> responseTimes = new ArrayList<>();
        long timeElapsed,totalTime=0L,experimentTime=(long)time*60000;
        while(totalTime < experimentTime){
            timeElapsed = System.currentTimeMillis();
            responseTimes.add(getEchoPacket(modem,echoCode));
            timeElapsed = System.currentTimeMillis() - timeElapsed;
            totalTime += timeElapsed;
        }
        return responseTimes;
    }

    /**
     * Method that requests a single echo packet from the ithaki server
     * @param modem a modem class
     * @param echoCode the echo code for the particular date and time provided by ithaki lab
     * @return the response time of a single packet
     */
    public static long getEchoPacket(Modem modem, String echoCode){

        long responseTime=0L;
        char[] startSequence = {'P','S','T','A','R','T'};
        char[] stopSequence = {'P','S','T','O','P'};
        int characterReceived,counter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if (!modem.write(echoCode.getBytes()))
                throw new customExceptionMessage("Could not request packet from server.");
            responseTime = System.currentTimeMillis();

        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        for(;;){
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[counter]) counter += 1;
                else counter = 0;
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected packet format");
                    iterationCounter++;
                }
                if (counter == stopSequence.length){
                    responseTime = System.currentTimeMillis() - responseTime;
                }
            }catch (Exception e){

                System.out.println(e);
                System.exit(1);
            }
            if (counter == stopSequence.length) break;
        }
        return responseTime;
    }

    /**
     *
     * @param modem         a modem class
     * @param imageCode     the image code for the particular date and time provided by ithaki lab
     * @param cam           parameter for which camera to be used
     * @param dir           direction of the camera dir = "R" or "L" or "U" or "D"(right,left,up,down)(applies only for cam = "PTZ")
     * @param size          size of the requested image size = "L" or "R" (applies only for cam = "PTZ")
     * @param imgLocation   the location to store the image
     * @throws IOException  throws IO exception if there is an error creating the file
     */
    public static void getImage(Modem modem,String imageCode,String cam,String dir,String size,String imgLocation) throws IOException {

        imageCode = constructImageCode(imageCode,cam,dir,size);
        boolean startCorrect=true;
        int characterReceived,counter = 0,iterationCounter=0;
        int[] startSequence = {255,216};
        int[] endSequence = {255,217};
        File image = new File(imgLocation);
        FileOutputStream fos = new FileOutputStream(image);
        try{
            if (!modem.write(imageCode.getBytes()))
                throw new customExceptionMessage("Could not request image from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        for(;;){
            try{
                characterReceived = modem.read();
                fos.write((byte) characterReceived);
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected image format");
                    iterationCounter++;
                }
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during image request");
                if (characterReceived == endSequence[counter]) counter += 1;
                else counter = 0;
            }catch (Exception e){
                System.out.println(e);
                System.exit(1);
            }
            if (counter == endSequence.length){
                fos.close();
                break;
            }
        }
    }

    /**
     * Method that constructs an image code given the CAM,DIR,SIZE parameters
     * @param imageCode the requested image code
     * @param cam   the code of the camera
     * @param dir   the direction (L,R,U,D)
     * @param size  the desirable size of the image (S,L)
     * @return      returns a string with the code and the desirable image parameters
     */
    public static String constructImageCode(String imageCode,String cam,String dir,String size){
        boolean bool = dir.equals("L") || dir.equals("U") || dir.equals("R") || dir.equals("D");
        switch(cam){
            case "PTZ":
                cam = "CAM=PTZ";
                if(bool) dir = "DIR=" + dir;
                else dir = "";
                if(size.equals("S") || size.equals("L")) size = "SIZE=" + size;
                else size = "";
                break;
            case "FIX":
                cam = "CAM=FIX";
                dir = "";
                size = "";
                break;
            default:
                cam = "CAM=" + cam;
                if(bool) dir = "DIR=" + dir;
                else dir = "";
                if((size.equals("S") || size.equals("L"))) size = "SIZE=" + size;
                else size = "";
                break;
        }
        imageCode = imageCode + cam + dir + size + "\r";

        return imageCode;
    }

    public static void getGPSMark(Modem modem,String gpsCode){

        char[] startSequence = "START ITHAKI GPS TRACKING\r\n".toCharArray();
        char[] stopSequence = "STOP ITHAKI GPS TRACKING\r\n".toCharArray();
        List<String> yolo = new ArrayList<>();
        gpsCode = constructGPSCode(gpsCode,"1000016",yolo);
        int characterReceived,counter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if (!modem.write(gpsCode.getBytes()))
                throw new customExceptionMessage("Could not request packet from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        for(;;){
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[counter]) counter += 1;
                else counter = 0;
                System.out.print((char)characterReceived);
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected packet format");
                    iterationCounter++;
                }
            }catch (Exception e){

                System.out.println(e);
                System.exit(1);
            }
            if (counter == stopSequence.length) break;
        }
    }

    /**
     * Construct a gps code to send to the ithaki server
     * @param gpsCode   the requested gps code
     * @param R         gps marks from a certain route (e.g R="XPPPLL")
     * @param T         gps marks jpeg image           (e.g T="AABBCCDDEEZZ")
     * @return          string with the constructed code
     */
    public static String constructGPSCode(String gpsCode,String R,List<String> T){
        gpsCode = gpsCode + "R=" + R;
        if(!T.isEmpty()){
            for (int i = 0; i < T.size(); i++) {
                gpsCode = gpsCode + "T=" + T.get(i);
            }
        }
        gpsCode = gpsCode + "\r";
        return gpsCode;
    }
}

/**
 * Custom class to throw custom exceptions
 */
class customExceptionMessage extends Exception {
    public customExceptionMessage(String message){
        super(message);
    }
}
