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
        openModem(modem,modemName);
        /*
         * Echo packet response times experiment
         */
        int echoExpTime = 1;
        String echoCode = "E7349\r";
        List<Long> times = echoPacketResponseTime(modem,echoCode,echoExpTime);

        /*
         * Image request experiment
         */
        // Error free
        String imageCode = "M6915";
        String cam = ""; // or CAM = "FIX" or "PTZ" or ""
        String dir = "";
        String size = "";
        String imgLocation = "./imgFIXErrorFree.jpg";
        constructImageCode(imageCode,cam,dir,size);
        getImage(modem,imageCode,cam,dir,size,imgLocation);

        // With errors
        imageCode = "G8174";
        cam = "";
        dir = "";
        size = "";
        imgLocation = "./imgFIXErrors.jpg";
        getImage(modem,imageCode,cam,dir,size,imgLocation);

        /*
         * Gps request experiment
         */
        String gpsCode = "P6982";
        List<String> R = new ArrayList<>();
        R.add("1000080");
        imgLocation = "./gpsImage.jpg";
        getGPSMark(modem,gpsCode,R,imgLocation);

        /*
         * Automatic repeat request
         */
        String ackCode = "Q2107\r";
        String nackCode = "R3193\r";
        int number = arqPacketExperiment(modem,ackCode,nackCode,echoExpTime);

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
        do {
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
        }while(counter != endSequence.length);
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
        char[] startSequence = "PSTART".toCharArray();
        char[] stopSequence = "PSTOP".toCharArray();
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if (!modem.write(echoCode.getBytes()))
                throw new customExceptionMessage("Could not request packet from server.");
            responseTime = System.currentTimeMillis();

        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected packet format");
                    iterationCounter++;
                }
                if (stopCounter == stopSequence.length){
                    responseTime = System.currentTimeMillis() - responseTime;
                }
            }catch (Exception e){

                System.out.println(e);
                System.exit(1);
            }
        }while(stopCounter != stopSequence.length);
        return responseTime;
    }

    /**
     * Receives a requested image from the server
     * @param modem         a modem class
     * @param imgCode       the requested code
     * @param imgLocation   the location to store the image
     * @throws IOException  throws IO exception if there is an error creating the file
     */
    public static void requestImage(Modem modem, String imgCode, String imgLocation) throws IOException {
        boolean startCorrect=true;
        int characterReceived,stopCounter = 0,iterationCounter=0;
        int[] startSequence = {255,216};
        int[] endSequence = {255,217};
        File image = new File(imgLocation);
        FileOutputStream fos = new FileOutputStream(image);
        try{
            if (!modem.write(imgCode.getBytes()))
                throw new customExceptionMessage("Could not request image from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                fos.write((byte) characterReceived);
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected image format");
                    iterationCounter++;
                }
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during image request");
                if (characterReceived == endSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
            }catch (Exception e){
                System.out.println(e);
                System.exit(1);
            }
            if (stopCounter == endSequence.length){
                fos.close();
            }
        }while(stopCounter != endSequence.length);
    }

    /**
     * Method that requests and saves an image requested from the ithaki server
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
        requestImage(modem, imageCode, imgLocation);
    }

    /**
     * Method that constructs an image code given the CAM,DIR,SIZE parameters
     * @param imageCode the requested image code
     * @param cam       the code of the camera
     * @param dir       the direction (L,R,U,D)
     * @param size      the desirable size of the image (S,L)
     * @return          returns a string with the code and the desirable image parameters
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

    /**
     *
     * @param modem         a modem class
     * @param gpsCode       the requested gps code
     * @param R             route parameters
     * @throws IOException  throws IO exception if there is an error creating the file
     */
    public static void getGPSMark(Modem modem,String gpsCode,List<String> R,String imgLocation) throws IOException {

        char[] startSequence = "START ITHAKI GPS TRACKING\r\n".toCharArray();
        char[] stopSequence = "STOP ITHAKI GPS TRACKING\r\n".toCharArray();
        String gpsMarkCode = constructGPSCode(gpsCode,R,true);
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if (!modem.write(gpsMarkCode.getBytes()))
                throw new customExceptionMessage("Could not request packet from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        String gpsMark = "";
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                gpsMark += (char) characterReceived;
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
        }while(stopCounter != stopSequence.length);

        gpsMark = gpsMark.substring(startSequence.length,gpsMark.length()-stopSequence.length);
        List<String> latitude = new ArrayList<>();
        List<String> longitude = new ArrayList<>();
        List<String> T = new ArrayList<>();
        int minutesLat,minutesLon;
        int secondsLat,secondsLon;
        int k =gpsMark.split("\r\n").length;
        int i = 0;
        for(int c = 0;c < k;c=c+10){
            latitude.add(gpsMark.split("\r\n")[c].split(",")[2]);
            longitude.add(gpsMark.split("\r\n")[c].split(",")[4]);
            minutesLat = (int)Double.parseDouble(latitude.get(i).substring(2));
            secondsLat = (int)Math.round((Double.parseDouble(latitude.get(i).substring(2)) - minutesLat) * 60);
            minutesLon = (int)Double.parseDouble(longitude.get(i).substring(3));
            secondsLon = (int)Math.round((Double.parseDouble(longitude.get(i).substring(3)) - minutesLon) * 60);
            T.add(longitude.get(i).substring(1,3) + minutesLon + secondsLon + latitude.get(i).substring(0,2) + minutesLat  + secondsLat);
            i++;
        }
        String gpsImgCode = constructGPSCode(gpsCode,T,false);
        requestImage(modem,gpsImgCode,imgLocation);

    }

    /**
     * Method that constructs a gps request code
     * @param gpsCode   the requested gps code
     * @param R         gps marks from a certain route (e.g R="XPPPLL") or gps marks jpeg image (e.g T="AABBCCDDEEZZ")
     * @param type      if type is true then parameter R is included in the code, otherwise R is a list with marks for the image
     * @return          returns a gps code either requesting image with marks on it or just gps marks
     */
    public static String constructGPSCode(String gpsCode,List<String> R,boolean type){
        if(type){
            if (!R.isEmpty()) {
                gpsCode = gpsCode + "R=" + R.get(0);
            }
        }else{
            if (!R.isEmpty()) {
                for (String s : R) {
                    gpsCode = gpsCode + "T=" + s;
                }
            }
        }
        gpsCode = gpsCode + "\r";
        return gpsCode;
    }

    public static int arqPacketExperiment(Modem modem,String ackCode,String nackCode,int time){
        long timeElapsed,totalTime=0L,experimentTime=(long)time*60000;
        boolean correctPacket = requestARQCode(modem,ackCode);
        int numberOfFailed = 0;
        while(totalTime < experimentTime){
            timeElapsed = System.currentTimeMillis();
            if(correctPacket) correctPacket = requestARQCode(modem,ackCode);
            else{
                correctPacket = requestARQCode(modem,nackCode);
                numberOfFailed++;
            }
            timeElapsed = System.currentTimeMillis() - timeElapsed;
            totalTime += timeElapsed;
        }
        return numberOfFailed;
    }

    public static boolean requestARQCode(Modem modem,String arqCode){
        char[] startSequence = "PSTART".toCharArray();
        char[] stopSequence = "PSTOP".toCharArray();
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        String arqResponse = "";
        try{
            if (!modem.write(arqCode.getBytes()))
                throw new customExceptionMessage("Could not request packet from server.");

        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                arqResponse += (char)characterReceived;
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new customExceptionMessage("Unexpected packet format");
                    iterationCounter++;
                }
            }catch (Exception e){

                System.out.println(e);
                System.exit(1);
            }
        }while(stopCounter != stopSequence.length);

        char[] coded = arqResponse.split(" ")[4].substring(1,17).toCharArray();
        int fcs = Integer.parseInt(arqResponse.split(" ")[5]);
        int codedFCS = 0;
        for(int i = 0;i < coded.length;i++){
            codedFCS = codedFCS ^ (int) coded[i];
        }

        return (codedFCS == fcs);
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
