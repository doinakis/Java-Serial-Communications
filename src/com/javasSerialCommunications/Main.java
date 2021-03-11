package com.javasSerialCommunications;

import ithakimodem.*;

import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        int character;
        char[] sequence = {'\r','\n','\n','\n'};
        int counter = 0;
        String echo_code = "E6462\r";
        System.out.println("Hello World!!");
        Modem modem = new Modem(8000);
        modem.setTimeout(2000);
        modem.open("ithaki");
        for(;;){
            try{
                character = modem.read();

                if ((char)character == sequence[counter]) counter += 1;
                else counter = 0;

                if(character == -1) break;
                System.out.print((char)character);
            }catch (Exception x) {
                break;
            }
            if (counter == 4) break;
        }
        if(!modem.write(echo_code.getBytes())) System.out.println("kappa");
        for(;;){
            try{
                character = modem.read();
                if(character == -1) break;
                System.out.print((char)character);
            }catch (Exception x) {
                break;
            }
        }

        modem.close();
    }
}
