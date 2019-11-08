/**
 *
 * @author dillon
 */
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/*
* A chat server App that allows private and public messages to be communicated 
* amongst clients connected to server. Clients can send private messages by 
* prefixing the receiver client's name with the "@" character.
*/
public class ChatAppServer {
    // create server socket reference
    private static ServerSocket MySevice = null;
    //create client socket reference
    private static Socket clientSocket = null;
    
    // The server can accept up to maxUser connections at a time.
    private static final int maxUsers = 20;
    private static final clientThread[] threads = new clientThread[maxUsers];
    
    public static void main(String[] args){
        // set default port number and provide option to change port number.
        int portNumber = 51352;
        Scanner sc = new Scanner(System.in);
        String response;
        String portNum = "";
        String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
        while(true){
            System.out.println(endOfLine+"Currently using default settings: port number = " +portNumber +"\nEnter 'yes' to change default settings or enter 'no' to continue with default settings:"+endOfLine);
            response = sc.nextLine();
            if (response.equalsIgnoreCase("yes")){
                System.out.println(endOfLine+"Enter port number to connect to:");
                try{
                    portNum = sc.nextLine();
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
        * Opening server socket on a port number
        */
        try{
            MySevice = new ServerSocket(portNumber);
        }
        catch (IOException e){
            System.out.println(e);
        }
        
        //System.out.println(System.getProperty("user.dir"));
        System.out.println(endOfLine+"***********  Server set up complete!! ***********"+endOfLine);
        
        // A new client thread created for each client connected to server.
        while (true){
            try{
                clientSocket = MySevice.accept();
                int i;
                for (i = 0; i< threads.length; i++){
                    if(threads.length == 0){
                      (threads[i] =  new clientThread(clientSocket, threads)).start();
                      break;  
                    }
                    else if(threads[i] == null){
                        (threads[i] =  new clientThread(clientSocket, threads)).start();
                        break;
                    }
                }
                
                // Once 20 clients connected to server, server prevents other potential clients until a connected client disconnects.
                if (i == maxUsers){
                    PrintStream output = new PrintStream(clientSocket.getOutputStream());
                    output.println(endOfLine+"Please try to connect again later, Server is too busy.");
                    output.close();
                    clientSocket.close(); 
                }
            }
            catch (IOException e){
                System.out.println(e);
            }
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

class clientThread extends Thread{
    
    private  String clientName = null;
    private  BufferedReader input = null;
    private  PrintStream output = null;
    private  Socket clientSocket = null;
    private  clientThread[] threads = null;
    private  int maxUsers;
    String endOfLine = "\n-------------------------------------------------------------------------------------------------------------------------\n";
    String help = endOfLine+"***Instructions***\n***To leave enter '/exit' in a new line.***\n***To send Private messages enter user name with '@' sign in front of name, a space and the message e.g. @Mark 'Hey Mark'***\n***To send files to a user enter  the /sendFile command, the user's name with '@' sign in front of the name and file directory of file to send - all seperated by a space e.g /sendFile @Mark /home/Pictures/image.jpg \n***To display these intructions again enter '/help' ***"+endOfLine;
    
    public clientThread(Socket clientSocket, clientThread[] threads){
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxUsers = threads.length;
    }
    
    @Override
    public void run(){
        int maxUsers = this.maxUsers;
        clientThread[] threads = this.threads;
        
        try{
            // input and output streams created for each client thread

            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new PrintStream(clientSocket.getOutputStream());
            String userName;
            while (true){
                output.println(endOfLine+"Enter your name to display in chat: ");
                userName = input.readLine().trim();
                if (!userName.contains("@")){
                    break;
                }
                else{
                    output.println(endOfLine+"The name should not contain '@' character.");
                }
            }
            
            //Opening messages for clients.
            output.println(endOfLine+"***Welcome " +userName + " to the ChatApp chat room.***\n");
            output.println(endOfLine+"***Instructions***\n***To leave enter '/exit' in a new line.***\n***To send Private messages enter user name with '@' sign in front of name, a space and the message e.g. @Mark 'Hey Mark'***");
            output.println("\n***To send files to a user enter  ther /sendFile command, the user's name with '@' sign in front of the name and file directory of file to send - all seperated by a space e.g /sendFile @Mark /home/Pictures/image.jpg \n***To display these intructions again enter '/help' ***"+endOfLine);
            synchronized(this){
                for (int i =0; i<threads.length;i++){
                    if (threads[i] != null && threads[i] == this){
                        clientName = "@"+userName;
                        break;
                    }
                }
                for(int i =0; i<threads.length;i++){
                    if(threads[i] != null && threads[i] != this){
                        threads[i].output.println(endOfLine+"***New user: " +userName+" has entered the chat room!!!***");
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
                                        this.output.println(endOfLine+"<"+dateFormat.format(date)+"> "+"Uploading file from: "+fileMessage[2]);
                                        String fileFound = this.input.readLine();
                                        String fileExt = ".jpg";
                                        String fileName = "";
                                        try{
                                        fileExt = fileMessage[2].substring(fileMessage[2].lastIndexOf("."));
                                        fileName = fileMessage[2].substring(fileMessage[2].lastIndexOf("/")+1);
                                        }catch(IndexOutOfBoundsException e){System.err.println(endOfLine+"\nFile Sent Unsuccessful - Not the proper format from directory given: " + fileMessage[2]);}
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
                                            this.output.println(endOfLine+"\n<"+dateFormat.format(date)+"> "+"Server Receving file...");
                                            FileOutputStream fos=new FileOutputStream(file,true);
                                            long bytesRead;
                                            do
                                            {
                                                bytesRead = this.clientSocket.getInputStream().read(buffer, 0, buffer.length);
                                                fos.write(buffer,0,buffer.length);
                                            }while(!(bytesRead<fileSize));
                                            fos.close();
                                            this.output.println(endOfLine+"<"+dateFormat.format(date)+"> "+"Server received file: "+fileName+", File size: "+(fileSize/1000)+" kB");
                                            //contact receiver client then send or delete file and notify sender client of actions
                                            while(true){
                                                threads[i].output.println(endOfLine+"<"+dateFormat.format(date)+"> "+this.clientName.substring(1)+" wants to send you a file: "+fileName+", File size: "+(fileSize/1000)+" kB"+"\n Do you accept the file? Enter '/yes' to accept or '/no' to decline: ");
                                                String answer = threads[i].input.readLine();
                                                if (answer.equalsIgnoreCase("/yes")){
                                                    try{
                                                        FileInputStream fileInput =new FileInputStream(file);
                                                        byte[] buffer2 = new byte [fileSize];
                                                        threads[i].output.println(endOfLine+"Sending file ("+fileName+") from server to - "+threads[i].clientName.substring(1)+", with file size: "+fileSize);
                                                        int read;
                                                        while((read = fileInput.read(buffer2)) !=-1){
                                                            threads[i].output.write(buffer2, 0, read); 
                                                            threads[i].output.flush();
                                                        }
                                                        while(fileInput.available()>0){
							fileInput.read(buffer2);
							}
                                                        fileInput.close();
                                                        this.output.println(endOfLine+"File was sent to: "+threads[i].clientName.substring(1)+"\nFile: "+fileName+", File size: "+(fileSize/1000)+" kB");
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
                                                    threads[i].output.println(endOfLine+"Please enter just '/yes' or '/no'.");
                                                }
                                            }
                                            
                                        }   
                                    }
                                    else{
                                        count++;   
                                    }
                                }
                                if (count == threads.length){
                                    this.output.println(endOfLine+"File Sent Unsuccessful - Incorrect format of user name specified or user name specified is not in chat. \nCorrect format: @UserName \nYou entered: "+fileMessage[1]);
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
                                        threads[i].output.println(endOfLine+"<"+dateFormat.format(date)+"> "+userName+": "+message[1]);
                                        
                                        // To show client who sent private message that it was sent.
                                        this.output.println(endOfLine+"<"+dateFormat.format(date)+"> "+userName+": " + message[1]);
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
                            if (line.equalsIgnoreCase("/yes")){this.output.println(endOfLine+"You entered: "+line+". Please enter your answer again to confirm file transfer");}
                            else if (line.equalsIgnoreCase("/no")){this.output.println(endOfLine+"You entered: "+line+". Please enter your answer again to deny file transfer");}
                        }
                        else if (line.equals("/help")){
                            this.output.println(help);
                        }
                        else{
                        for (int i =0; i<threads.length;i++){
                            if (threads[i] != null && threads[i].clientName != null){
                                threads[i].output.println(endOfLine+"<"+dateFormat.format(date)+"> "+userName+": "+line);
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
                        threads[i].output.println(endOfLine+"***User: "+userName+" has left the chat room!!!***"+endOfLine);
                    }
                }
            }
            output.println(endOfLine+"***"+userName+" You've logged out***"+endOfLine);
        
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


