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
        //int echoExpTime = 4;
        String imageCode = "G3034\r";
        String imgLocation = "./image.jpg";
        //modem.setTimeout(2000);
        openModem(modem,modemName);
        getImage(modem,imageCode,imgLocation);
        //List<Long> times = echoPacketResponseTime(modem,echoCode,echoExpTime);
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
    /* Method that requests a single echo packet from the ithaki server
    * Input arguments:
    *   modem: A modem class
    *   echoCode: String, the echo code for the particular date and time provided by ithaki lab
    */

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
        int characterReceived,counter=0;
        boolean startCorrect;
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
     * Method that requests an image (Error free or with Errors) from the ithaki server
     * @param modem a modem class
     * @param imageCode the image code for the particular date and time provided by ithaki lab
     * @param imgLocation the location to store the image
     * @throws IOException throw exception if there is an error writing the image
     */
    public static void getImage(Modem modem,String imageCode,String imgLocation) throws IOException {
        File image = new File(imgLocation);
        FileOutputStream fos = new FileOutputStream(image);
        int characterReceived,counter = 0;
        int[] endSequence = {255,217};

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
                if (characterReceived == -1) throw new customExceptionMessage("Modem disconnected during image request");
                if (characterReceived == endSequence[counter]) counter += 1;
                else counter = 0;
                System.out.println(characterReceived);

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
}

/**
 * Custom class to throw custom exceptions
 */
class customExceptionMessage extends Exception {
    public customExceptionMessage(String message){
        super(message);
    }
}
