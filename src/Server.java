import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Scanner;
/**
 *
 */
public class Server{
    static long time1 = System.currentTimeMillis(); //start time
    static long time2 = System.currentTimeMillis(); //current time
    static long timeout = time2-time1;
    static int limit = 30*60000; // 30 (minutes) x 60000 (a minute in milliseconds)  
    //new variables from original server class...
    private Thread t;
    private static String threadName;
    static ServerSocket MyService = null; //stream socket to listen in for clients requests (TCP)
    static Socket clientSocket = null; //socket sent from client to server
    static int portNumber = 4444; //server will use this port number for listening for new client connections
    static int portNumber2 = 8888; //server will use this port number for listening for files and messages
    static DataInputStream input; //used to store client messages for prosessing
    static PrintStream output; //used to send messages back to client
    static Scanner sc;
    static ArrayList<String> log;
    static boolean threadSwitch = true;
    public final static int FILE_SIZE = 6022386; // file size temporary hard coded
    //new variables from the submitted server class...
    private static final int maxUsers = 20; // The server can accept up to maxUser connections at a time.
    private static final clientThreads[] threads = new clientThreads[maxUsers];
    
    Server(String name) {
        threadName = name;
        System.out.println("Creating " + threadName);
    }
    
    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        String response;
        String portNum = "";
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        while(true){
            System.out.println("Currently using default settings: port number = " +portNumber +"\nEnter 'yes' to change default settings or enter 'no' to continue with default settings:");
            response = sc.nextLine();
            if (response.equalsIgnoreCase("yes")){
                System.out.println("Enter port number to connect to:");
                try{
                    portNum = sc.nextLine();
                    portNumber = Integer.valueOf(portNum).intValue();
                    break;
                }
                catch (NumberFormatException e){
                    System.err.println("Please enter a port number with no letters or special characters(digits only). You entered: "+portNum);
                }
            }    
            else if(response.equalsIgnoreCase("no")){
                break;
            }
            else {
                System.out.println("Please enter just 'yes' or 'no'. You entered: "+response);
            }
        }
        getServerIP(); //prints out full details of inet ip address
        
        //create socket called MyService on given port number
        try {
            MyService = new ServerSocket(portNumber);
            System.out.println("Server socket setup complete!");
        } catch (IOException e) {
            System.out.println("ERROR: Server setup method says: " + e);
        }
        
        //s new client thread created for each client connected to server.
        while (true){
            try{
                clientSocket = MyService.accept();
                int i;
                for (i = 0; i< threads.length; i++){
                    if(threads.length == 0){
                      (threads[i] =  new clientThreads(clientSocket, threads)).start();
                      break;  
                    }
                    else if(threads[i] == null){
                        (threads[i] =  new clientThreads(clientSocket, threads)).start();
                        break;
                    }
                }
                
                // Once 20 clients connected to server, server prevents other potential clients until a connected client disconnects.
                if (i == maxUsers){
                    PrintStream output = new PrintStream(clientSocket.getOutputStream());
                    output.println(endOfLine+"Please try to connect again later. Server has reached its maximum number of clients.");
                    output.close();
                    clientSocket.close(); 
                }
            }
            catch (IOException e){
                System.out.println(e);
            }
        }
        //running...
        //run2();
        //exiting...
        //closeSockets();
        //System.out.println("Server closed.");
    }
    
    public static void getServerIP() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    ip = addr.getHostAddress();
                    //String fullCredentials = iface.getDisplayName() + " " + ip;
                    System.out.println(ip);
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }
    
    //none of the methods below are currently being utilized

    //method to make program run via command line until exit command is supplied...
    public static void run2() {
        DataInputStream serverInputStream = dataInputStream(); //messages sent to the server (from client)
        PrintStream serverOutputStream = dataOutputStream(); //messages sent from the server (to client)
        Scanner serverMsgIn = new Scanner(serverInputStream, "UTF-8"); //used to store incoming messages from client 
        log = new ArrayList();
        log.add("Chat started"); //add first message to server log
        String nextMsg; //buffer to store incoming messages from client
        int fileSendSwitch = 0;
        int fileReceiveSwitch = 0;
        //int extension = 1;
        while (!log.isEmpty()) { //while the log is not empty
            //perhaps don't qualify threads    
            try {
                if (!serverMsgIn.hasNextLine()) { //if scanner does not have next line                   
                    //setupClientSocket();
                }
                else { //Scanner has next line  
                    //System.out.println("Waiting for client command...");
                    nextMsg = serverMsgIn.nextLine(); //is the next message incoming from client
                    if (nextMsg.contains(":sendfile") && fileReceiveSwitch == 0) { //in the case that a client sends an image                     
                        System.out.println("Client is attempting to send file...");
                        log.add("Client is attempting to send file...");
                        //serverOutputStream.println(log.get(log.size()-1)); //sends client last message in the log, ideally the whole log
                        serverOutputStream.println(log); //sends client last message in the log, ideally the whole log: OUTPUT
                        //
                        System.out.println("Waiting for file name:");
                        String fileName = serverMsgIn.nextLine(); //INPUT
                        System.out.println("File name: "+fileName);
                        serverFileRecieve(fileName); //key method that implements, recieves file: INPUT
                        System.out.println("File has been recieved!!!");
                        while (serverMsgIn.hasNext()){
                            System.out.println(serverMsgIn.nextLine()); //CLEARING REMNANTS OF THE IMAGE
                        }
                        log.add("File "+fileName+" has successfully been recieved!!");
                        serverOutputStream.println(log); //sends client last message in the log, ideally the whole log
                        //
                        fileReceiveSwitch = 1;  
                    } 
                    else if (nextMsg.contains(":getfile") && fileSendSwitch == 0) { //in the case that a client sends an image                     
                        System.out.println("Client is attempting to get file...");
                        log.add("Client is attempting to get file...");
                        //serverOutputStream.println(log.get(log.size()-1)); //sends client last message in the log, ideally the whole log
                        serverOutputStream.println(log); //sends client last message in the log, ideally the whole log
                        //
                        System.out.println("Waiting for client to send file name...");
                        //
                        String fileName = serverMsgIn.nextLine(); //takes in name of file 
                        System.out.println("File name: "+fileName);
                        log.add("File name :"+fileName);
                        serverOutputStream.println(log); //sends client last message in the log, ideally the whole log
                        //
                        serverFileSend(fileName); //key implementing method, sends file to client
                        System.out.println("File has been sent!!!");
                        //Output stream might have been closed
                        fileSendSwitch = 1;
                    }
                    else { //in the case that a simple text message is sent
                        System.out.println(nextMsg); //print to console incoming messages from client
                        log.add(nextMsg); //add the clients message to the log
                        //serverOutputStream.println(log.get(log.size()-1)); //sends client last message in the log, ideally the whole log
                        serverOutputStream.println(log); //sends client last message in the log, ideally the whole log
                        fileSendSwitch = 0;
                        fileReceiveSwitch = 0;
                    }
                }
            } catch (Exception e) {
                System.out.println("Server run method exception says: " + e);
            }
        }
    }

    public static void serverFileRecieve(String fileToReceive) throws FileNotFoundException, IOException {
        int bytesRead;
        int current = 0;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        ServerSocket servsock = null;
        Socket sock = null;
        try {
            servsock = new ServerSocket(portNumber2);
            System.out.println("Connecting...");
            // receive file
            byte [] mybytearray  = new byte [FILE_SIZE];
            InputStream is = input;
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
            is.close();
            System.out.println("File " + fileToReceive + " downloaded (" + current + " bytes read)");
        }
        finally {
            if (fos != null) fos.close();
            if (bos != null) bos.close();
            if (servsock != null) servsock.close();
        } 
    }
    
    public static void serverFileSend(String fileToSend) throws FileNotFoundException, IOException{
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            OutputStream os = null;
            ServerSocket servsock = null;
            Socket sock = null;
            try {
                servsock = new ServerSocket(portNumber2);       
                System.out.println("Waiting...");
                try {
                    sock = servsock.accept();
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
        finally {
            if (servsock != null) servsock.close();
        }
    }

    //server processing requests from the client...
    public static DataInputStream dataInputStream() {
        try {
            input = new DataInputStream(clientSocket.getInputStream());
            //System.out.println("Server2 dataInputStream method says: input = "+input);
        } catch (IOException e) {
            System.out.println("ERROR: Server dataInputStream method says: " + e);
        }
        return input;
    }

    //client output stream to send data to the server
    public static PrintStream dataOutputStream() {
        try {
            output = new PrintStream(clientSocket.getOutputStream());
            //System.out.println("Server2 dataOutputStream method says: output = "+output);
        } catch (IOException e) {
            System.out.println("ERROR: Server dataOutputStream method says: " + e);
        }
        return output;
    }

    //closing server sockets
    public static void closeSockets() {
        try {
            output.close();
            input.close();
            clientSocket.close();
            MyService.close();
            System.out.println("All sockets closed successfully!");
            System.out.println("Server closed.");
        } catch (IOException e) {
            System.out.println(e);
        }
    }
  
}

/*
* This client thread class creates input and output streams, Displays welcoming 
* messages and facilitate communication between clients. It directs private messages 
* specific users and display's those messages in it's own terminal to indicate message was sent
* Public messages are also allowed and displays date, time, name of user and the message in both private and public messages
* Then allows user to exit chat and cleans up code of existing client thread.
*/

class clientThreads extends Thread{
    
    private  String clientName = null;
    private  BufferedReader input = null;
    private  PrintStream output = null;
    private  Socket clientSocket = null;
    private  clientThreads[] threads = null;
    private  int maxUsers;
    String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
    String help = endOfLine+"***Instructions***\n***To leave enter '/exit' in a new line.***\n***To send Private messages enter user name with '@' sign in front of name, a space and the message e.g. @Mark 'Hey Mark'***\n***To send files to a user enter  the /sendFile command, the user's name with '@' sign in front of the name and file directory of file to send - all seperated by a space e.g /sendFile @Mark /home/Pictures/image.jpg \n***To display these intructions again enter '/help' ***"+endOfLine;
    
    public clientThreads(Socket clientSocket, clientThreads[] threads){
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxUsers = threads.length;
    }
    
    @Override
    public void run(){
        int maxUsers = this.maxUsers;
        clientThreads[] threads = this.threads;
        
        try{
            // input and output streams created for each client thread
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintStream(clientSocket.getOutputStream());
            String userName;
            while (true){
                output.println("Enter your name to display in chat: ");
                userName = input.readLine().trim();
                if (!userName.contains("@")){
                    break;
                }
                else{
                    output.println("The name should not contain '@' character.");
                }
            }
            
            //Opening messages for clients.
            output.println(endOfLine+"******* Welcome to Chat APP, " +userName + "! *******\n");
            output.println("***Instructions***\n***To leave enter '/exit' in a new line.***\n***To send Private messages enter user name with '@' sign in front of name, a space and the message e.g. @Mark 'Hey Mark'***");
            output.println("\n***To send files to a user enter  the '/sendFile' command, the user's name with '@' sign in front of the name and file directory of file to send - all seperated by a space e.g /sendFile @Mark /home/Pictures/image.jpg \n***To display these intructions again enter '/help' ***"+endOfLine);
            synchronized(this){
                for (int i =0; i<threads.length;i++){
                    if (threads[i] != null && threads[i] == this){
                        clientName = "@"+userName;
                        break;
                    }
                }
                for(int i =0; i<threads.length;i++){
                    if(threads[i] != null && threads[i] != this){
                        threads[i].output.println("***New user: " +userName+" has entered the chat room!!!***");
                    }
                }                       
            }
            
            //Handling communication between clients.
            String fileData ="";
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            //Format to display the date and time e.g. 2016/11/16 12:08:43
            while(true){
                String line = input.readLine();
                Date date = new Date();
                if (line.startsWith("/exit")){
                    break;
                }
                //for sending a file
                if (line.startsWith("/sendFile")){
                    String[] fileMessage  =line.split("\\s", 3);
                    if (fileMessage.length == 3 && fileMessage[1] != null && fileMessage[2] != null){
                        fileMessage[1] = fileMessage[1].trim();
                        fileMessage[2] = fileMessage[2].trim();
                        if (!fileMessage[1].isEmpty() && !fileMessage[2].isEmpty()){
                            synchronized(this){
                                int count = 0;
                                for (int i =0; i<threads.length;i++){
                                    if (threads[i] != null && threads[i] != this && threads[i].clientName != null && threads[i].clientName.equals(fileMessage[1])){
                                        this.output.println("<"+dateFormat.format(date)+"> "+"Uploading file from: "+fileMessage[2]);
                                        String fileFound = this.input.readLine();
                                        String fileExt = ".jpg";
                                        String fileName = "";
                                        try{
                                        fileExt = fileMessage[2].substring(fileMessage[2].lastIndexOf("."));
                                        fileName = fileMessage[2].substring(fileMessage[2].lastIndexOf("/")+1);
                                        }catch(IndexOutOfBoundsException e){System.err.println("\nFile Sent Unsuccessful - Not the proper format from directory given: " + fileMessage[2]);}
                                        if (fileFound.contains("yes")){
                                            //to create a string version of the file for blocking file data through the buffered reader input stream to block file data from printing to terminal.
                                            File fileToString = new File(fileMessage[2]);
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
                                            //futher file read and store
                                            File file = new File(System.getProperty("user.dir")+"/ServerStorage/Server"+fileExt);
                                            int fileSize = Integer.parseInt(fileFound.substring(fileFound.lastIndexOf(":")+2));
                                            byte[] buffer = new byte [fileSize];
                                            this.output.println("\n<"+dateFormat.format(date)+"> "+"Server Receving file...");
                                            FileOutputStream fos=new FileOutputStream(file,true);
                                            long bytesRead;
                                            do
                                            {
                                                bytesRead = this.clientSocket.getInputStream().read(buffer, 0, buffer.length);
                                                fos.write(buffer,0,buffer.length);
                                            }while(!(bytesRead<fileSize));
                                            fos.close();
                                            this.output.println("<"+dateFormat.format(date)+"> "+"Server received file: "+fileName+", File size: "+(fileSize/1000)+" kB");
                                            //contact receiver client then send or delete file and notify sender client of actions
                                            while(true){
                                                threads[i].output.println("<"+dateFormat.format(date)+"> "+this.clientName.substring(1)+" wants to send you a file: "+fileName+", File size: "+(fileSize/1000)+" kB"+"\n Do you accept the file? Enter '/yes' to accept or '/no' to decline: ");
                                                String answer = threads[i].input.readLine();
                                                if (answer.equalsIgnoreCase("/yes")){
                                                    try{
                                                        FileInputStream fileInput =new FileInputStream(file);
                                                        byte[] buffer2 = new byte [fileSize];
                                                        threads[i].output.println("Sending file ("+fileName+") from server to - "+threads[i].clientName.substring(1)+", with file size: "+fileSize);
                                                        int read;
                                                        while((read = fileInput.read(buffer2)) !=-1){
                                                            threads[i].output.write(buffer2, 0, read); 
                                                            threads[i].output.flush();
                                                        }
                                                        while(fileInput.available()>0){
							fileInput.read(buffer2);
							}
                                                        fileInput.close();
                                                        this.output.println("File was sent to: "+threads[i].clientName.substring(1)+"\nFile: "+fileName+", File size: "+(fileSize/1000)+" kB");
                                                        //delete file from server
                                                        if(file.delete()){
                                                        System.out.println("file loaded in by server has been deleted");
                                                        }                                   
                                                        else{
                                                            System.out.println("Delete operation has failed.");
                                                        }
                                                        break;
                                                    }
                                                    catch(FileNotFoundException e){
                                                        threads[i].output.println(endOfLine+"File Sent Unsuccessful - File Not found Execption");
                                                        this.output.println(endOfLine+"File Sent Unsuccessful - File Not found Execption");
                                                        break;
                                                    }
                                                    catch(IOException e){
                                                        threads[i].output.println(endOfLine+"File Sent Unsuccessful - IO Execption");
                                                        this.output.println(endOfLine+"File Sent Unsuccessful - IO Execption");
                                                        break;
                                                        }
                                                    
                                                }
                                                else if (answer.equalsIgnoreCase("/no")){
                                                    this.output.println(endOfLine+"File not sent to User "+threads[i].clientName.substring(1)+". File send request has been declined");
                                                    //delete file from server
                                                    if(file.delete()){
                                                        System.out.println("file loaded in by server has been deleted");
                                                    }                                   
                                                    else{
                                                        System.out.println("Delete operation has failed.");
                                                    }
                                                    break;
                                                    
                                                }
                                                else{
                                                    threads[i].output.println("Please enter just '/yes' or '/no'.");
                                                }
                                            }
                                            
                                        }   
                                    }
                                    else{
                                        count++;   
                                    }
                                }
                                if (count == threads.length){
                                    this.output.println("File Sent Unsuccessful - Incorrect format of user name specified or user name specified is not in chat. \nCorrect format: @UserName \nYou entered: "+fileMessage[1]);
                                }
                              //}       
                            }
                        }
                    }
                }
                // Privated messages directed to intended user
                if (line.startsWith("@")){
                    String[] message  =line.split("\\s", 2);
                    if (message.length > 1 && message[1] != null){
                        message[1] = message[1].trim();
                        if (!message[1].isEmpty()){
                            synchronized(this){
                                for (int i =0; i<threads.length;i++){
                                    if (threads[i] != null && threads[i] != this && threads[i].clientName != null && threads[i].clientName.equals(message[0])){
                                        threads[i].output.println("<"+dateFormat.format(date)+"> "+userName+": "+message[1]);
                                        
                                        // To show client who sent private message that it was sent.
                                        this.output.println("<"+dateFormat.format(date)+"> "+userName+": " + message[1]);
                                        break;
                                    }
                                }    
                            }
                        }
                    }
                }
                else {
                    // Public messages intended to all users
                    synchronized(this){
                        if(fileData.contains(line)){
                            continue;}
                        else if (line.equalsIgnoreCase("/yes")|| line.equalsIgnoreCase("/no")){
                            if (line.equalsIgnoreCase("/yes")){this.output.println("You entered: "+line+". Please enter your answer again to confirm file transfer");}
                            else if (line.equalsIgnoreCase("/no")){this.output.println("You entered: "+line+". Please enter your answer again to deny file transfer");}
                        }
                        else if (line.equals("/help")){
                            this.output.println(help);
                        }
                        else{
                        for (int i =0; i<threads.length;i++){
                            if (threads[i] != null && threads[i].clientName != null){
                                threads[i].output.println("<"+dateFormat.format(date)+"> "+userName+": "+line);
                            }
                        }
                    }
                    }
                }
            }
            //when user exits, breaks out of loop and displays following message to all users
            synchronized(this){
                for (int i =0; i<threads.length;i++){
                    if (threads[i] != null && threads[i] != this && threads[i].clientName != null){
                        threads[i].output.println("***User: "+userName+" has left the chat room!!!***");
                    }
                }
            }
            output.println("***"+userName+" You've logged out***");
        
            /*
            * Set current thread to null. To create space in threads list for another client 
            */
            synchronized(this){
                for (int i =0; i<threads.length;i++){
                    if (threads[i] == this){
                        threads[i] = null;
                    }
                }
            }
            //Close socket, input and output streams.
            input.close();
            output.close();
            clientSocket.close();
        }
        catch (IOException e){
            System.out.println(e);
        }
    }
}