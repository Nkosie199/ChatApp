/**
 *
 * @author dillon
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import javax.annotation.processing.FilerException;

public class ChatAppClient implements Runnable {
    //created client socket reference
    private static Socket MyClient = null;
    //created output stream from client
    private static PrintStream output = null;
    //created input stream to client 
    private static BufferedReader serverInput = null;
    private static BufferedReader clientInput = null;
    private static boolean socketClosed = false;
    
    public static void main(String[] args) {
        //set default port number to 51352
        int portNumber = 51352;
        //set default host address to localhost and provide option to change default settings
        String host = "localhost";
        Scanner sc = new Scanner(System.in);
        String response;
	String portNum = "";
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        while(true){
            System.out.println(endOfLine+"Currently using default settings: host =" +host+", port number =" +portNumber +"\nEnter 'yes' to change default settings or enter 'no' to continue with default settings:");
            response = sc.next();
            if (response.equalsIgnoreCase("yes")){
                System.out.println(endOfLine+"Enter host to connect to: ");
                host = sc.next();
                System.out.println(endOfLine+"Enter port number to connect to: ");
                try{
                    portNum = sc.next();
                    portNumber = Integer.valueOf(portNum).intValue();
                    break;
                }
                catch (NumberFormatException e){
                    System.err.println(endOfLine+"Please enter a port number with no letters or special characters(digits only). You entered: "+portNum);
                }
            }    
            else if(response.equalsIgnoreCase("no")){
                    break;
            }
            else {
                System.out.println(endOfLine+"Please enter just 'yes' or 'no'. You entered: "+response);
            }
        }
        
        /*
        * Opening the socket on the host and port number chosen by the user.
        * Opening the input and output streams of the client to and from the server.
        */
        try{
            MyClient = new Socket(host, portNumber);
            clientInput = new BufferedReader(new InputStreamReader(System.in));
            output = new PrintStream(MyClient.getOutputStream());
            serverInput = new BufferedReader(new InputStreamReader(MyClient.getInputStream()));
        } 
        catch (UnknownHostException e){
            System.err.println(endOfLine+"This host is unknown: " +host);
        }
        catch (IOException e){
            System.err.println(endOfLine+"Unable to get Input/output connection of host: "+host);
        }
        
        /*
        * If socket, host port number, input and output streams was initialised correctly then 
        * we allow client thread to write to server and read from server simlutaneously on socket connection
        */
        if (MyClient != null && output != null && serverInput != null) {
            try{
                //The thread to read from server
                new Thread(new ChatAppClient()).start();
                
                //Loop to write to server 
                while (!socketClosed){
                    output.println(clientInput.readLine().trim());
                }
                
                //Close socket, input and output streams.
                output.close();
                serverInput.close();
                MyClient.close();
            }
            catch (IOException e){
                System.err.println(endOfLine+"IOException: " +e);
            }
        }           
    }
    
    // Run method of thread client to read from the server
    @Override    
    public void run(){
        /*
        * Loop to keep relaying messages from server to client until client 
        * logs out and server displays "You've logged out" and breaks loop. 
        */
        String serverMessage;
        int i = 0;
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        String fileDir = "";
        int fileSize1 = 16*1024;
        String fileData ="";
        try{
            while((serverMessage = serverInput.readLine()) != null){
                if (fileData.contains(serverMessage)){
                    continue;
                }
                else{
                System.out.println(serverMessage);
                if (serverMessage.contains(endOfLine+"You've logged out"+endOfLine)){
                    break;
                }
                else if(serverMessage.contains("Uploading file from: ")){
                    try{
                        fileDir = serverMessage.substring(serverMessage.lastIndexOf(":")+2);
                        File file =new File(fileDir);
                        //to create a string version of the file for blocking file data through the buffered reader input stream to block file data from printing to terminal.
                        File fileToString = file;
                        try{
                            Long fileLength = fileToString.length();
                            byte[] buffer4 = new byte[fileLength.intValue()];
                            FileInputStream inputStream = new FileInputStream(fileToString);
                            while(inputStream.read(buffer4) != -1) {
                                fileData += new String(buffer4)+ "\n";
                            }   
                            inputStream.close();
                        }
                        catch(FileNotFoundException e){System.err.println(endOfLine+"FileNotFoundException "+e);}
                        catch(IOException e){System.err.println(endOfLine+"IOException: " +e);}
                        Long fileSize = file.length();
                        FileInputStream fileInput =new FileInputStream(file);
                        byte[] buffer = new byte [fileSize.intValue()];
                        output.println("yes: "+fileSize);
                        int read;
                        while((read = fileInput.read(buffer)) !=-1){
                            output.write(buffer, 0, read);
                            output.flush();                            
                        }
                        fileInput.close();
                    }
                    catch(FileNotFoundException e){
                        System.err.println(endOfLine+"\nFile Sent Unsuccessful - Unable to open file in file directory given: " + fileDir);
                        output.println("no");
                    }
                    catch(IOException e){
                        System.err.println(endOfLine+"\nFile Sent Unsuccessful - Input/Output error when uploading file from directory given: " + fileDir);
                        output.println("no");
                    }
                    catch(IndexOutOfBoundsException e){System.err.println(endOfLine+"\nFile Sent Unsuccessful - Not the proper format from directory given: " + fileDir);
                    output.println("no");
                    }
                }
                else if(serverMessage.contains("Sending file")){
                    try{
                        String filename = serverMessage.substring(serverMessage.indexOf("(")+1,serverMessage.indexOf(")"));
                        String fileExt = filename.substring(filename.indexOf("."));
                        fileSize1 = Integer.parseInt(serverMessage.substring(serverMessage.lastIndexOf(":")+2));
                        String clientName = serverMessage.substring(serverMessage.lastIndexOf("-")+2,serverMessage.lastIndexOf(","));
                        File folder = new File(System.getProperty("user.dir")+"/"+clientName);
                        //creating new folder with receiving client's name to store file being received
                        if (!folder.exists()) {
				if (folder.mkdir()) {
					System.out.println(endOfLine+"New client folder created to store image");
				} else {
					System.out.println(endOfLine+"Failed to create folder to store file");
				}
			} else {
				System.out.println(endOfLine+"Storing file in client's existing folder");
			}
 
                        File file = new File(System.getProperty("user.dir")+"/"+clientName+"/file"+i+fileExt);
                        byte[] buffer3 = new byte [fileSize1];
                        FileOutputStream fos=new FileOutputStream(file,true);
                        long bytesRead;
                        do
                        {
                        bytesRead = MyClient.getInputStream().read(buffer3, 0, buffer3.length);
                        fos.write(buffer3,0,buffer3.length);
                        }while(!(bytesRead<fileSize1));
                        fos.close();
                        System.out.println("File received: "+filename+"\nFile size: "+(fileSize1/1000)+" kB");
                        i++;
                        }
                        catch(EOFException e){
                            System.err.println(endOfLine+"EOFException "+e);
                        }
                }
            }
            }
            socketClosed = true;
        }
        catch (IOException e){
            System.err.println(endOfLine+"IOException: " +e);
        }
    }
}

