import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author gmdnko003
 */
public class Client implements Runnable{
    static Socket MyClient = null; //stream socket (TCP)
    static String machineName; //specifies machine name in IP address form
    static String userName; //specifies client chosen user-name
    static int portNumber, portNumber2; //server open port used to send requests to server < 1023 < 65536
    static DataInputStream input; //stores server responses
    static PrintStream output = null; //stores message to be sent to server
    static Scanner sc;
    public final static int FILE_SIZE = 6022386; // max file size temporarily hard coded
    //submitted Client methods
    //created input stream to client 
    private static BufferedReader serverInput = null;
    private static BufferedReader clientInput = null;
    private static boolean socketClosed = false;

    public static void main(String[] args) throws IOException{
        String host = "localhost";
        int portNumber = 4444;
        String response;
        String portNum = "";
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        sc = new Scanner(System.in);
        while(true){
            System.out.println(endOfLine+"Currently using default settings: host = " +host+", port number = " +portNumber +"\nEnter 'yes' to change default settings or enter 'no' to continue with default settings:");
            response = sc.nextLine();
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
                new Thread(new Client()).start();
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
        String serverMessage;
        int i = 0;
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        String fileDir = "";
        int fileSize1;
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
    
    //the methods below are from the original Client...
    //initializing...
    public static void setup(){
        try{
            MyClient = new Socket(machineName, portNumber);
            System.out.println("Client socket setup complete!");
        }
        catch (IOException e){
            System.out.println("ERROR: Client setup method says: "+e);
        }    
    }

    //method to make program run via command line until exit command is supplied...
    public static void run2() throws IOException{
        //initializing server and client sockets input and output streams respectively...   
        DataInputStream clientInputStream = dataInputStream(); //messages sent to client (from server)
        PrintStream clientOutputStream = dataOutputStream(); //messages sent from client (to server)
        String fileDir;
        Scanner clientMsgIn = new Scanner(clientInputStream); //used to store incoming messages from server
        //ArrayList<String> log = new ArrayList();
        String command = userName+" has entered the conversation"; //app prompts client to enter a command
        clientOutputStream.println(command); //send entry message to server
        //System.out.println(clientMsgIn.nextLine()); //prints to console message sent from server to client
        formatLogEntry(clientMsgIn.nextLine()); 
        //sc.nextLine(); //to get rid of the blank 1st message
        while (!command.equals(":exit")){
            if (command.equals(":sendfile")){
                command = "";
                System.out.println("Please ensure that the file is in the Client directory and enter the name of the file: ");
                System.out.print(">>>");
                try{                  
                    fileDir = sc.nextLine(); //app prompts user to enter a image name (redundant at this point)
                    clientOutputStream.println(fileDir); //sends server file name/ directory :OUTPUT
                    //
                    System.out.println("Sending file: "+fileDir+"...");
                    clientFileSend(fileDir); //sends file to server
                    //           
                }
                catch(Exception e){
                    System.out.println("Client run method said "+e);
                }  
            }
            else if (command.equals(":getfile")){
                command = "";
                System.out.println("Please enter the name of the file you would like to get: ");
                System.out.print(">>>");
                fileDir = sc.nextLine(); //app prompts user to enter a file name (redundant at this point)
                System.out.println("Receiving file: "+fileDir+"...");  
                //
                clientOutputStream.println(fileDir); //send the server the name of the file you wish to receive
                formatLogEntry(clientMsgIn.nextLine()); //input
                try{
                    clientFileReceive(fileDir);
                    System.out.println("Client has recieved file: "+fileDir+"!!! ");
                }
                catch(Exception e){
                    System.out.println("Client get file method said "+e);
                }  
            }
            else{
                // now we will continuously send messages to the server and print out the servers response...
                System.out.print(">>>");
                command = sc.nextLine(); //app prompts user to enter a command
                clientOutputStream.println("("+System.currentTimeMillis()+") "+userName+": "+command); //output
                //while (clientMsgIn.hasNextLine()){
                    //System.out.println(clientMsgIn.nextLine()); //prints to console message sent from server to client
                    formatLogEntry(clientMsgIn.nextLine()); //input
                //}         
            }
            //ensure input/output streams are kept constant
        }
        command = userName+" has left the conversation"; //app prompts client to enter a command
        clientOutputStream.println(command); //send entry message to server
        if (clientMsgIn.hasNextLine()){
            //System.out.println(clientMsgIn.nextLine()); //prints to console message sent from server to client
            formatLogEntry(clientMsgIn.nextLine()); 
        } 
        System.out.println("");
    }
    
    public static synchronized void clientFileSend(String fileToSend) throws FileNotFoundException, IOException, InterruptedException{
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        Socket sock = null;
              
        System.out.println("Waiting for server to create connection...");
        try {
            TimeUnit.SECONDS.sleep(5);
            sock = new Socket(machineName, portNumber2);
            System.out.println("Accepted connection : " + sock);
            // send file
            File myFile = new File (fileToSend);
            byte [] mybytearray  = new byte [(int)myFile.length()];
            fis = new FileInputStream(myFile);
            bis = new BufferedInputStream(fis);
            bis.read(mybytearray,0,mybytearray.length);
            os = sock.getOutputStream();
            System.out.println("Sending " + fileToSend + "(" + mybytearray.length + " bytes)");
            os.write(mybytearray,0,mybytearray.length);
            os.flush();
            System.out.println("Done.");
        }
        finally {
            if (bis != null) bis.close();
            if (os != null) os.close();
            if (sock!=null) sock.close();
        }        
    }
    
    public static synchronized void clientFileReceive(String fileToReceive) throws FileNotFoundException, IOException, InterruptedException {
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        Socket sock = null;
        System.out.println("Waiting for server to create connection...");
        try {     
            TimeUnit.SECONDS.sleep(5);
            sock = new Socket(machineName, portNumber2);
            System.out.println("Connecting...");
            // receive file
            byte [] mybytearray  = new byte [FILE_SIZE];
            InputStream is = sock.getInputStream();
            fos = new FileOutputStream(fileToReceive);
            bos = new BufferedOutputStream(fos);
            bytesRead = is.read(mybytearray,0,mybytearray.length);
            current = bytesRead;
            do {
               bytesRead =
                  is.read(mybytearray, current, (mybytearray.length-current));
               if(bytesRead >= 0) current += bytesRead;
            } while(bytesRead > -1);

            bos.write(mybytearray, 0 , current);
            bos.flush();
            System.out.println("File " + fileToReceive
                + " downloaded (" + current + " bytes read)");
        }
        finally {
            if (fos != null) fos.close();
            if (bos != null) bos.close();
            if (sock != null) sock.close();
        }
    }
    
    //adequately prints/ displays to returned server log to console 
    public static void formatLogEntry(String inMsg){
        String out = inMsg.substring(1, inMsg.length()-1); //remove brackets
        //System.out.println("out = "+out);
        String[] out2 = out.split(", ");
        for (String s: out2){
            System.out.println(s);
        }     
    }
    
    //client processing responses from the server...
    public static DataInputStream dataInputStream(){
        try{
            input = new DataInputStream(MyClient.getInputStream());
            //System.out.println("Client dataInputStream method says: input = "+input);
        }
        catch(Exception e){
            System.out.println("ERROR: Client dataInputStream method says: "+e);
        }
        return input;
    }
    
    //sets and gets client output stream to send data to the server
    public static PrintStream dataOutputStream(){
        try{
            output = new PrintStream(MyClient.getOutputStream());
            //System.out.println("Client dataOutputStream method says: output = "+output);
        }
        catch(Exception e){
            System.out.println("ERROR: Client dataOutputStream method says: "+e);
        }
        return output;
    }
    
    //closing server sockets
    public static void closeSockets(){
        try{
            output.close();
            input.close();
            MyClient.close();
            System.out.println("Client closeSockets method says: All sockets closed successfully!");
        }
        catch(IOException e){
            System.out.println("ERROR: Client closeSockets method says: "+e);
        }
    }   
    
    public static String getClientIP(){
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    //String fullCredentials = iface.getDisplayName() + " " + ip;
                    System.out.println(ip);
                }
            }
        } 
        catch (SocketException e) {
            throw new RuntimeException(e);
        }        
        return ip;
    }
    
    public static void exit(){
        System.out.println("*** Thank you for using Chat APP. Goodbye! ***");
    }
    
}