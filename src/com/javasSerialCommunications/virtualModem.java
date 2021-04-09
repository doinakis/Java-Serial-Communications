/*
 * Doinakis Michail 9292
 * e-mail: doinakis@eceauth.gr
 */
package com.javasSerialCommunications;

import ithakimodem.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class virtualModem {
    /**
     * Static variables for handling the modem
     */
    static Modem modem = new Modem();
    static int modemSpeed = 1000;
    static int modemSpeedImage = 80000;
    static String modemName = "ithaki";
    static int timeout = 2000;
    static int expTime = 4;
    static String folderLocation = "./session/";

    /**
     * Experiment Codes
     */
    static String echoCode = "E7745";

    static String imageCode = "M7525";
    static String cam = "FIX"; // or CAM = "FIX" or "PTZ" or ""
    // Only for PTZ type of img.If CAM = "FIX" they are ignored
    static String dir = "L";
    static String size = "S";

    static String imageCodeErrors = "G2202";
    static String camErrors = "PTZ";
    // Only for PTZ type of img.If CAM = "FIX" they are ignored
    static String dirErrors = "L";
    static String sizeErrors = "S";

    static String gpsCode = "P1108";
    static String gpsRoute = "1015099";
    static int numberOfMarks = 9;
    static int timeBetweenMarks = 10;

    static String ackCode = "Q8694";
    static String nackCode = "R8666";

    public static void main(String[] args) throws IOException {
        (new virtualModem()).demo();
    }

    /**
     * Performs all the experiments for the given assignment
     * @throws IOException throws IO exception if there is an error creating the file
     */
    public void demo() throws IOException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date date;
        modem.setTimeout(timeout);
        openModem(modem,modemName);

        /*
         * Echo packet response times experiment
         */
        date = new Date(System.currentTimeMillis());
        System.out.println("Echo Packet experiment started: " + formatter.format(date));

        echoPacketResponseTime(modem,echoCode + "\r",expTime);

        date = new Date(System.currentTimeMillis());
        System.out.println("Echo Packet experiment ended: " + formatter.format(date));

        /*
         * Image request experiment
         */
        // Error free
        modem.setSpeed(modemSpeedImage);
        String imgLocation = folderLocation + imageCode + ".jpg";
        date = new Date(System.currentTimeMillis());
        System.out.println("Requesting error free image: " + formatter.format(date));
        getImage(modem,imageCode,cam,dir,size,imgLocation);
        date = new Date(System.currentTimeMillis());
        System.out.println("Image  received: " + formatter.format(date));

        // With errors
        imgLocation = folderLocation + imageCodeErrors + ".jpg";
        date = new Date(System.currentTimeMillis());
        System.out.println("Requesting image with errors: " + formatter.format(date));
        getImage(modem,imageCodeErrors,camErrors,dirErrors,sizeErrors,imgLocation);
        date = new Date(System.currentTimeMillis());
        System.out.println("Image  received: " + formatter.format(date));

        /*
         * Gps request experiment
         */
        List<String> R = new ArrayList<>();
        R.add(gpsRoute);
        imgLocation = folderLocation + "gpsImage.jpg";
        date = new Date(System.currentTimeMillis());
        System.out.println("Requesting GPS route image: " + formatter.format(date));
        getGPSMark(modem,gpsCode,R,imgLocation,numberOfMarks,timeBetweenMarks);
        date = new Date(System.currentTimeMillis());
        System.out.println("Image  received: " + formatter.format(date));

        /*
         * Automatic repeat request
         */
        modem.setSpeed(modemSpeed);
        date = new Date(System.currentTimeMillis());
        System.out.println("Automatic Repeat experiment started: " + formatter.format(date));
        arqPacketExperiment(modem,ackCode + "\r",nackCode + "\r",expTime);
        date = new Date(System.currentTimeMillis());
        System.out.println("Automatic Repeat Request experiment ended: " + formatter.format(date));

        modem.close();
    }
    /**
     * Method that initializes a connection with the virtual modem
     * @param modem a modem class
     * @param modemName the name of the modem to connect to (In this case ithaki)
     */
    static void openModem(Modem modem,String modemName) {
        try{
            if(!modem.open(modemName)) throw new CustomExceptionMessage("Could not open to modem.");
            printHelloMessage(modem);
        }catch(Exception e){
            System.out.println(e);
        }
    }

    /**
     * Method that prints the Greetings message that ithaki modems send at first connection with it
     * @param modem a modem class
     */
    static void printHelloMessage(Modem modem) {
        int characterReceived,counter=0;
        char[] endSequence = {'\r','\n','\n','\n'};
        do {
            try{
                characterReceived = modem.read();

                if ((char)characterReceived == endSequence[counter]) counter += 1;
                else counter = 0;

                if(characterReceived == -1) throw new CustomExceptionMessage("Modem disconnected");
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
     */
    static void echoPacketResponseTime(Modem modem,String echoCode,int time) {
        List<Long> responseTimes = new ArrayList<>();
        long timeElapsed,totalTime=0L,experimentTime=(long)time*60000;
        while(totalTime < experimentTime) {
            timeElapsed = System.currentTimeMillis();
            responseTimes.add(getEchoPacket(modem,echoCode));
            timeElapsed = System.currentTimeMillis() - timeElapsed;
            totalTime += timeElapsed;
        }
        StringBuilder toWriteEchoResponseTimes = new StringBuilder();
        for (Long responseTime : responseTimes) {
            toWriteEchoResponseTimes.append(responseTime).append(",");
        }
        try {
            File myFile1 = new File(folderLocation + "echoExperiment.csv");
            Writer writer = new PrintWriter(myFile1);
            writer.write(toWriteEchoResponseTimes.toString());
            writer.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Method that requests a single echo packet from the ithaki server
     * @param modem a modem class
     * @param echoCode the echo code for the particular date and time provided by ithaki lab
     * @return the response time of a single packet
     */
    static long getEchoPacket(Modem modem, String echoCode) {

        long responseTime=0L;
        char[] startSequence = "PSTART".toCharArray();
        char[] stopSequence = "PSTOP".toCharArray();
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if(!modem.write(echoCode.getBytes()))
                throw new CustomExceptionMessage("Could not request packet from server.");
            responseTime = System.currentTimeMillis();

        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new CustomExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                if (iterationCounter < startSequence.length){
                    if (characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if (!startCorrect) throw new CustomExceptionMessage("Unexpected packet format");
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
    static void requestImage(Modem modem, String imgCode, String imgLocation) throws IOException {
        boolean startCorrect=true;
        int characterReceived,stopCounter=0,iterationCounter=0;
        int[] startSequence = {255,216};
        int[] endSequence = {255,217};
        File image = new File(imgLocation);
        FileOutputStream fos = new FileOutputStream(image);
        try{
            if (!modem.write(imgCode.getBytes()))
                throw new CustomExceptionMessage("Could not request image from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new CustomExceptionMessage("Modem disconnected during image request");
                if (characterReceived == endSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                fos.write((byte) characterReceived);
                if(iterationCounter < startSequence.length){
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new CustomExceptionMessage("Unexpected image format");
                    iterationCounter++;
                }
            }catch (Exception e){
                System.out.println(e);
                System.exit(1);
            }
            if (stopCounter == endSequence.length) {
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
    static void getImage(Modem modem,String imageCode,String cam,String dir,String size,String imgLocation) throws IOException {

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
    static String constructImageCode(String imageCode,String cam,String dir,String size){
        boolean bool = dir.equals("L") || dir.equals("U") || dir.equals("R") || dir.equals("D");
        switch(cam) {
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
    static void getGPSMark(Modem modem,String gpsCode,List<String> R,String imgLocation,int numberOfMarks,int timeBetweenMarks) throws IOException {

        char[] startSequence = "START ITHAKI GPS TRACKING\r\n".toCharArray();
        char[] stopSequence = "STOP ITHAKI GPS TRACKING\r\n".toCharArray();
        String gpsMarkCode = constructGPSCode(gpsCode,R,true);
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        try{
            if (!modem.write(gpsMarkCode.getBytes()))
                throw new CustomExceptionMessage("Could not request packet from server.");
        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        String gpsMark = "";
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new CustomExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                gpsMark += (char) characterReceived;
                if(iterationCounter < startSequence.length) {
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new CustomExceptionMessage("Unexpected packet format");
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
        int secondsLat,secondsLon;
        int k = gpsMark.split("\r\n").length;
        int i = 0;
        double prevTime = 0.0;
        double currTime;
        double time;
        String[] markSplit;
        String test;
        for(int c = 0;c < k;c++) {
            markSplit = gpsMark.split("\r\n")[c].split(",");
            currTime = Double.parseDouble(markSplit[1].substring(0,2)) * 3600 + Double.parseDouble(markSplit[1].substring(2,4))* 60 + Double.parseDouble(markSplit[1].substring(4));
            time = currTime - prevTime;
            if(time >= timeBetweenMarks && i < numberOfMarks) {
                latitude.add(markSplit[2]);
                longitude.add(markSplit[4]);
                secondsLat = (int)Math.round(Double.parseDouble(latitude.get(i).substring(4)) * 60);
                secondsLon = (int)Math.round(Double.parseDouble(longitude.get(i).substring(5)) * 60);
                test = longitude.get(i).substring(1,5) + secondsLon + latitude.get(i).substring(0,4) + secondsLat;
                if(!T.contains(test)) {
                    T.add(test);
                    i++;
                }else {
                    latitude.remove(i);
                    longitude.remove(i);
                }
                prevTime = currTime;
            }
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
    static String constructGPSCode(String gpsCode,List<String> R,boolean type) {
        if(type) {
            if (!R.isEmpty()) {
                gpsCode = gpsCode + "R=" + R.get(0);
            }
        }else {
            if (!R.isEmpty()) {
                for (String s : R) {
                    gpsCode = gpsCode + "T=" + s;
                }
            }
        }
        gpsCode = gpsCode + "\r";
        return gpsCode;
    }

    /**
     * Method that performs the ARQ packet experiment
     * @param modem     a modem class
     * @param ackCode   request code that indicates that the packets arrived correctly
     * @param nackCode  request code that indicates that the packets arrived incorrectly
     * @param time      the time the experiment will tun
     */
    static void arqPacketExperiment(Modem modem,String ackCode,String nackCode,int time) {
        List<Integer> numberOfNack = new ArrayList<>();
        List<Long> packetResponseTime = new ArrayList<>();
        long timeElapsed,totalTime=0L,experimentTime=(long)time*60000;

        while(totalTime < experimentTime) {
            timeElapsed = System.currentTimeMillis();
            numberOfNack.add(getCorrectPacket(modem,ackCode,nackCode));
            timeElapsed = System.currentTimeMillis() - timeElapsed;
            packetResponseTime.add(timeElapsed);
            totalTime += timeElapsed;
        }
        String toWriteARQTimes="";
        StringBuilder toWriteNumberOfARQ = new StringBuilder();
        for (Long aLong : packetResponseTime) {
            toWriteARQTimes += aLong + ",";
        }
        for (Integer integer : numberOfNack) {
            toWriteNumberOfARQ.append(integer).append(",");
        }
        try {
            File myFile1 = new File(folderLocation + "ArqResponseTimes.csv");
            File myFile2= new File(folderLocation + "ArqNumberOfNack.csv");
            Writer writer1 = new PrintWriter(myFile1);
            Writer writer2 = new PrintWriter(myFile2);
            writer1.write(toWriteARQTimes);
            writer2.write(toWriteNumberOfARQ.toString());
            writer1.close();
            writer2.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Method that counts how many times a specific packet is requested
     * @param modem     a modem class
     * @param ackCode   the code that requests the next packet if the received packet is correct
     * @param nackCode  the code that requests the same packet if its received incorrectly
     * @return  returns the number of times a packet its requested
     */
    static int getCorrectPacket(Modem modem,String ackCode,String nackCode) {
        int numberOfNack=0;
        if(!requestARQCode(modem,ackCode)) {
            numberOfNack++;
            while (!requestARQCode(modem,nackCode)) {
                numberOfNack++;
            }
        }
        return numberOfNack;
    }

    /**
     * Method that requests a packet from the server
     * @param modem     a modem class
     * @param arqCode   the request code (either ACK or Nack)
     * @return returns true if the requested packet arrives correctly, false otherwise
     */
    static boolean requestARQCode(Modem modem,String arqCode) {
        char[] startSequence = "PSTART".toCharArray();
        char[] stopSequence = "PSTOP".toCharArray();
        int characterReceived,stopCounter=0,iterationCounter=0;
        boolean startCorrect=true;
        String arqResponse = "";
        try{
            if (!modem.write(arqCode.getBytes()))
                throw new CustomExceptionMessage("Could not request packet from server.");

        }catch (Exception e){
            System.out.println(e);
            System.exit(1);
        }
        do{
            try{
                characterReceived = modem.read();
                if (characterReceived == -1) throw new CustomExceptionMessage("Modem disconnected during packet request");
                if ((char) characterReceived == stopSequence[stopCounter]) stopCounter += 1;
                else stopCounter = 0;
                arqResponse += (char)characterReceived;
                if(iterationCounter < startSequence.length) {
                    if(characterReceived != startSequence[iterationCounter]) startCorrect = false;
                    if(!startCorrect) throw new CustomExceptionMessage("Unexpected packet format");
                    iterationCounter++;
                }
            }catch (Exception e) {

                System.out.println(e);
                System.exit(1);
            }
        }while(stopCounter != stopSequence.length);

        char[] coded = arqResponse.split(" ")[4].substring(1,17).toCharArray();
        int fcs = Integer.parseInt(arqResponse.split(" ")[5]);
        int codedFCS = 0;
        for (char c : coded) {
            codedFCS = codedFCS ^ (int) c;
        }

        return (codedFCS == fcs);
    }
}

/**
 * Custom class to throw custom exceptions
 */
class CustomExceptionMessage extends Exception {
    public CustomExceptionMessage(String message) {
        super(message);
    }
}
