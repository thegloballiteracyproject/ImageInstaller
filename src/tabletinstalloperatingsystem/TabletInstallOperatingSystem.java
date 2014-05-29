/*
 * The MIT License (MIT)

Copyright (c) 2014 The Dalai Lama Center

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */

package tabletinstalloperatingsystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class TabletInstallOperatingSystem {
    private static String username, password, ssid, networkPassword;

    public static void main(String[] args) {
        
        //Check credentials before doing any processing
        ServerConnect server = new ServerConnect();
        while(true)
        {
            System.out.println("Please enter your username");
            username = readUserInput();
            System.out.println("Please Enter your password");
            password = readUserInput();
            
            System.out.println("Checking...");
            if(!server.checkCredentials(username, password))
                System.out.println("Your credentials don't match.\nPlease try again.");
            else
            {
                System.out.println("Your credentials match!");
                break;
            }
        }
        
        //Get network infrmation that will be used for every tablet
        System.out.println("Please enter the SSID of the default network connection.\n"
                + "(Leave blank if there will not be a default connection)");
        ssid = readUserInput();
        if(ssid.isEmpty())
        {
            System.out.println("There will not be a default network.");
            networkPassword = ssid = null;
        }
        else
        {
            while(true)
            {
                System.out.println("Please enter the network password");
                networkPassword = readUserInput();
                System.out.println("Please enter the password again");
                if(!readUserInput().equals(networkPassword))
                    System.out.println("The two passwords do not match");
                else
                {
                    System.out.println("Network credentials saved!");
                    break;
                }
            }
        }
        
        File serialLabelMappingFile = new File("serialToLabelMapping.txt");
        //Check if there is a file in our directory for versioning information
        determineConfigFile();
        
        //Get the serial number to label mapping
        String[] serialToLabel = serialToLabelMapping(serialLabelMappingFile);
        
        //Parse the array into a hash table 
        HashMap<String, String> serialToLabelHashMap = mapIt(serialToLabel);
        
        //start processing tablets
        //Continue processing until the program closed
        while(true)
        {
            try 
            {
                processTablets(serialToLabelHashMap);
                Thread.sleep(4000);
            }
            catch(InterruptedException e) {}
        }
    }
    
    private static void processTablets(HashMap<String, String> serialToLabelMapping)
    {
        int numberOfDevices = 0;
        TabletInformation tabletInfo = TabletInformation.getInstance();
        List<String> devices = new LinkedList<>();
        try 
            { 
            Process p=Runtime.getRuntime().exec("cmd /c adb devices"); 
            p.waitFor(); 
            BufferedReader reader=new BufferedReader(
                new InputStreamReader(p.getInputStream())
            ); 
            String line; 
            //Skip the first line
            String temp = reader.readLine();
            while((line = reader.readLine()) != null) 
            { 
                if(line.length() > 0)
                {
                    numberOfDevices++;
                    devices.add(line);
                    System.out.println(line);
                }
            }
            //remove the empty line from the count
            numberOfDevices--;
        }
        catch(IOException | InterruptedException e1) 
        {
            System.out.println("Exception: " + e1);
        } 
        System.out.println("Number of devices: " + numberOfDevices +1);

        //Loop over all discovered devices
        for(int i = 0; i < devices.size(); i++)
        {
            if(tabletInfo.checkIfPresent(devices.get(i).split("\t")[0]))
                continue;
            //Code below is to pull the label from a pre-defined array
            String label = null;
            System.out.println(devices.get(i).split("\t")[0]);
            
            //Generate the label
            label = tabletInfo.getTabletLabel() + tabletInfo.getSequenceNumber();
            //increment the sequence by one for the next iteration
            tabletInfo.incrementSequenceNumber();
            
            insertLabelInfoFile(label);

            //Get the device label
            String deviceSerialId = devices.get(i).split("\t")[0];

            String cmd, adb, root, push, reboot, line, serverStop, serverStart;
            cmd = "cmd /c ";
            adb = " adb -s ";
            serverStop = cmd + adb + " adb kill-server ";
            serverStart = cmd + adb + " adb start-server";
            root = cmd + adb + deviceSerialId + " root";
            push = cmd + adb + deviceSerialId + " push openrecoveryscript /cache/recovery";
            reboot = cmd + adb + deviceSerialId + " reboot recovery";

            //Kick off the install process
            try 
            {
                //Stop and start the server
                Process p = Runtime.getRuntime().exec(serverStop);
                p.waitFor();
                p = Runtime.getRuntime().exec(serverStart);
                p.waitFor();

                //start the install process
                p = Runtime.getRuntime().exec(root); 
                p.waitFor();
                BufferedReader reader=new BufferedReader(
                    new InputStreamReader(p.getInputStream())
                );
                while((line = reader.readLine()) != null) System.out.println(line);
                Thread.sleep(2000);
                p = Runtime.getRuntime().exec(push); 
                p.waitFor();
                reader=new BufferedReader(
                    new InputStreamReader(p.getInputStream())
                );
                while((line = reader.readLine()) != null) System.out.println(line);
                Thread.sleep(2000);
                p = Runtime.getRuntime().exec(reboot); 
                p.waitFor();
                reader=new BufferedReader(
                    new InputStreamReader(p.getInputStream())
                );
                while((line = reader.readLine()) != null) System.out.println(line);
                Thread.sleep(2000);
            }
            catch(IOException | InterruptedException e1) {} 
            tabletInfo.addDevice(devices.get(i).split("\t")[0]);
            System.out.println(label + "  has been completed.");
        }
    }
    
    private static void determineConfigFile()
    {
        TabletInformation tabletInfo = TabletInformation.getInstance();
        //Get the info from the user   
        System.out.println("Please enter the generic Tablet Label with no "
            + "spaces and no numeric identifier (i.e. ERL-GSU-)");
        tabletInfo.setTabletLabel(readUserInput());

        System.out.println("Please enter the starting sequence number.  "
                + "Leave blank for default of \"01\"");
        String tempUserInput = readUserInput();

        //If no input, set to default
        tabletInfo.setSequenceNumber(
        tempUserInput.isEmpty() ? 1 : Integer.parseInt(tempUserInput));
        

    }
    
    private static void insertLabelInfoFile(String label)
    {
        String labelDirectory = "/data/media/0/label.txt";
        File file = new File("openrecoveryscript.unnamed");
        if(!file.exists()) 
        {
            System.out.println("openrecoveryscript.unnamed doesn't exist!");
        }
        String line;
        
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("openrecoveryscript")));
            //Clear out the file
            bw.write("");
            while((line = br.readLine()) != null)
            {
                if(line.contains("--"))
                {
                    line = "cmd echo \"" + label + "\" > " + labelDirectory;
                }
                bw.append(line + "\n");
            }
            bw.flush();
        }
        catch (IOException e) {}
        
    }
    
    private static String readUserInput()
    {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
        catch (IOException e){}
        
        //If try fails
        return "-1";
    }
    
    private static String[] serialToLabelMapping(File file)
    {
        String line;
        List<String> serialToLabel = new LinkedList();
        
        try (BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            //Clear out the file
            while((line = br.readLine()) != null)
            {
                serialToLabel.add(line);
            }
        }
        catch (IOException e) {}
        return serialToLabel.toArray(new String[serialToLabel.size()]);
    }
    
    private static HashMap<String, String> mapIt(String[] arrayToMap)
    {
        String[] tempArray;
        HashMap<String, String> tempHashMap = new HashMap();
        for(int i = 0; i < arrayToMap.length; i++)
        {
            tempArray = arrayToMap[i].split(",");
            tempHashMap.put(tempArray[1].trim(), tempArray[0].trim());
        }
        return tempHashMap;
    }
}