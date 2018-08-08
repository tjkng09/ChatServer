/** This server is 30 LINES of executable code! 9-5-10 PDB
 *  Not bad for a remote, multithreaded chat room server!
 */
import java.io.*;
import java.net.*;
import java.util.*;
 
public class BasicChatRoomServer implements Runnable 
{
// STATIC stuff ---------------------------------------------------------
public static void main(String[] args) throws IOException
    {
    if (((args.length == 1) && args[0].equals("?"))
      || (args.length > 1))  
       {
       System.out.println("Port number may be specified as a command line parameter.");
       return;
       }
    if (args.length == 0)
        new BasicChatRoomServer();
    if (args.length == 1)
        new BasicChatRoomServer(Integer.parseInt(args[0]));
    }
 
// OBJECT stuff ---------------------------------------------------------
 
private ServerSocket ss;
// key will be name of user in the chat room.
private Hashtable<String,DataOutputStream> clients = 
    new Hashtable<String,DataOutputStream>();   
 
public BasicChatRoomServer() throws IOException // CONSTRUCTOR
    {
    this(1234); // specify default port #
    }
 
public BasicChatRoomServer(int port) throws IOException // CONSTRUCTOR
    {
    System.out.println("*****************************************************************");
    System.out.println("* THIS IS THE INSTRUCTOR-PROVIDED DEMO *BASIC* CHAT ROOM SERVER *");
    System.out.println("*****************************************************************");
 
    ss = new ServerSocket(port); 
    System.out.println("ChatServer is up at "
                     + InetAddress.getLocalHost().getHostAddress()
                     + " on port " + ss.getLocalPort());
    new Thread(this).start(); // create 1st client Thread 
    }                         // to execute run() method.
 
/** Each client goes through 4 stages:
 * 1. connection processing
 * 2. join processing
 * 3. send/receive processing
 * 4. leave prcessing
 * (See sections marked 1,2,3,4 in the run() method below.)
 */
public void run() // Thread t enters here!
    {
    String    clientName = null; // These are local variables.
    Socket           s   = null; // Each client thread will 
    DataInputStream  dis = null; // have it's own copy of 
    DataOutputStream dos = null; // each variable on their
                                 // own thread's stack.
    try {
        s = ss.accept();          // Wait for client connect.
        new Thread(this).start(); // Make thread for next client.
                                  // Next client thread enters run()
                                  // and waits in ss.accept().
        // 1. CONNECTION PROCESSING
        dis = new DataInputStream (s.getInputStream());
        dos = new DataOutputStream(s.getOutputStream());
        System.out.println("Join requested.");
        clientName = dis.readUTF(); // 1st msg from client is their chat name.
        // Watch for bloopers/hackers/wrong address/wrong port
        if ((clientName == null)
         || (clientName.trim().length() == 0) // all blanks
         || !clientName.startsWith("^")) // left-to-right
           {                             // eval is key here!
           dos.writeUTF("Invalid protocol.");
           System.out.println("Received join request with invalid protocol: " + clientName);
           dos.close(); // hang up.
           return;      // and kill this client thread
           }
        // drop ^ and convert to same case to avoid duplicate keys.
        clientName = clientName.substring(1).trim().toUpperCase();  
        if (clients.containsKey(clientName)) // this name already in?
           {
           dos.writeUTF(clientName + " is already in the chat room. Please rejoin with another name.");
           System.out.println("Received join request with duplicate name: " + clientName);
           dos.close(); // hang up.
           return;      // and kill this client thread
           }
         
        // 2. "JOIN processing" for this client
        dos.writeUTF("accepted"); // confirm to client that they are in!
        // note that if write above fails, put below doesn't happen...
        clients.put(clientName,dos); // add client to Hashtable
        sendToClients("Welcome to " + clientName + " who has just joined the chat room!");
        System.out.println(clientName + " is joining");
        }
    catch (Exception e)
        {                         
        System.out.println("Connection failure during initial join: " + e);
        // don't need to hang up - connection has already failed!
        return; // kill this client's thread
        }
  
        
   // 3. "SEND/RECEIVE processing" for this client.
   try { 
       while (true)
           {  
           String msg = dis.readUTF(); // wait for this client to say something
           System.out.println("Received '" + msg + "' from " + clientName);
           sendToClients(clientName + " says: " + msg);
           }
       }
    
    // 4. "LEAVE processing" for this client.
    // The user closes the client window to leave the chat
    // room, terminating the client program and taking down
    // the connection. So the server will go to the catch
    // below from dis.readUTF() above (which is then failing).
    catch (IOException e) 
       {
       System.out.println(clientName + " is leaving.");
       clients.remove(clientName); // This is the ONLY place the client gets removed!
       sendToClients("Goodbye to " + clientName
                   + " who has just left the chat room!");
       }
    } // end of run(). client thread returns to the Thread
      // object and is terminated! (It's finished running.)
 
 
private synchronized void sendToClients(String message) 
   { 
   // synchronization ensures that all clients will get all
   // messages in the same sequence. (Another client thread
   // cannot enter even if the thread in the for loop is 
   // suspended by the O/S!)    
   DataOutputStream[] dosArray = clients.values().toArray(new DataOutputStream[0]);
   for (DataOutputStream dos : dosArray)
         {                             
         try {dos.writeUTF(message);}
         catch (IOException e) {} 
         }
         // No action need be taken here if the communications
         // writeUTF() fails because we can count on that
         // client's thread, parked on a dis.readUTF(),
         // also seeing the failure and removing the failed
         // dos from the collection! 
   }
}