/*
 * Tyler King
 * Lab 4 - Chat Room Client
 * Spring 2016
 */
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
 
public class ChatClient implements ActionListener, Runnable 
{
 
public  static void main(String[] args) throws IOException
   {
    if (((args.length != 3) && args[0].equals("?"))
      || (args.length > 3)
      || (args.length == 0))
       {
       System.out.println("Please enter chat name as first command line parameter.");
       System.out.println("Chat name containing blanks must be enclosed in quotes.");
       System.out.println("Chat Server address should be your second command line parameter.");
       System.out.println("Password should be your third command line parameter.");
       System.out.println("Each seperate parameter should be seperated by a space.");
       return;
       }
   if (args.length == 1)
       new ChatClient(args[0]);
   if (args.length == 2)
       new ChatClient(args[0],args[1]);
   if (args.length == 3)
       new ChatClient(args[0],args[1],args[2]);
   }
 
private Socket           s;
private DataInputStream  dis; 
private DataOutputStream dos;
private String newLine = System.getProperty("line.separator"); 
 
private JFrame       window           = new JFrame("Basic Chat Room Client");
private JButton      hiddenSendButton = new JButton();
private JTextArea    inChatTextArea   = new JTextArea(10,40);
private JScrollPane  inChatScrollPane = new JScrollPane(inChatTextArea);
private JTextArea    outChatTextArea  = new JTextArea(10,40); // rows,cols
private JScrollPane  outChatScrollPane= new JScrollPane(outChatTextArea);
private JSplitPane   splitPane        = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,inChatScrollPane,outChatScrollPane);
 
public ChatClient(String chatName) throws IOException // constructor
  {
  this(chatName,"localhost",1234);
  System.out.println("(In 1-parm constructor.)");
  }
 
public ChatClient(String chatName, String serverAddress) throws IOException // constructor
  {
  this(chatName, serverAddress, 1234);
  System.out.println("(In 2-parm constructor.)");
  }
 
public ChatClient(String chatName, String serverAddress, String password) throws IOException // constructor
    {
    System.out.println("Tyler King");
    System.out.println("ECE 309 - Lab 4");
    System.out.println("Chat Room Client");
 
    // Build the GUI
    // For the mnemonics to work, the send button must be on the GUI.
    // But we can write the splitPane OVER it and cover it up!
    window.getContentPane().add(hiddenSendButton); 
    window.getContentPane().add(splitPane);        
     
    // set initial GUI attributes 
    hiddenSendButton.setMnemonic(KeyEvent.VK_ENTER);//alt-ENTER same as sendButton
    outChatTextArea.setEditable(false);    // always keep cursor out
    inChatTextArea.setEditable(false);    // INITIALLY keep cursor out
    outChatTextArea.setFont(new Font("default style", Font.BOLD, 20));
    inChatTextArea.setFont(new Font("default style", Font.BOLD, 20));
    splitPane.setDividerLocation(500);//set divider 500 pixels from left border 
 
    // Register with GUI objects for event notification
    hiddenSendButton.addActionListener(this); 
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
    // Show window
    window.setSize(1000,400); // width, height
    window.setVisible(true);
 
    // Connect to the chat server
    try {
        outChatTextArea.setText("Connecting to chat room server at "
                              + serverAddress
                              + " on port "
                              + 4444);
        // Make a connection to the ChatServer and join the chat room.
        // If this is successful continue to build a GUI and start an app thread.
        s   = new Socket(serverAddress, 4444);
        dis = new DataInputStream(s.getInputStream());  // fail here if 
        dos = new DataOutputStream(s.getOutputStream());// s is not set.
        dos.writeUTF("^" + chatName); // send join request as 1st msg
        String serverReply = dis.readUTF();
        if (serverReply.equalsIgnoreCase("accepted"))
           {
           outChatTextArea.append(newLine + "Connected to chat room server."); 
           // Modify GUI attributes for chatting!
           window.setTitle(chatName + " is in the Chat Room!       Use alt-Enter to send chat.");
           inChatTextArea.setEditable(true); // allow typing
           inChatTextArea.requestFocus();    // set cursor in
 
           // make receive thread only if connection was successful.  
           new Thread(this).start(); // Make a receive thread
           }                         // and jump it into run().
         else//Being here means *connection* to the server was successful but
           { //the server is rejecting our *join* due to a duplicate name or bad host addr or port.
           outChatTextArea.append(newLine + serverReply);
           outChatTextArea.append(newLine + "Are you sure you're using the correct server address and port number?");
           outChatTextArea.append(newLine + "Close window to exit. Then restart client.");
           System.out.println("Server rejected join request. Reply was: " + serverReply);
/*EXIT*/   return; // stop app if can't join. (main thread killed but GUI will stay up)
           } 
        }
    catch(IOException e) // Being here means the initial connection attempt failed.
        {                // Address not correct, server not up, or protocol is bad.
        outChatTextArea.append(newLine + e.toString()); 
        outChatTextArea.append(newLine + "Are you sure you're using the correct server address and port number?");
        outChatTextArea.append(newLine + "Close window to exit. Then restart client.");
/*EXIT*/throw e; // also notify loading program of connection error.
        }
    }
 
 
public void actionPerformed(ActionEvent ae)
    {
    // This is a little unusual (but simple!) We don't have to test for who is
    // calling, because we only activated one GUI object: the SEND button.
    // But then we didn't ever show the SEND button on the GUI, so we find ourself
    // here when the mnemonic alt-ENTER is pressed.
       String chat = inChatTextArea.getText().trim();
       if (chat.length() == 0) return; // ignore blank input.
       inChatTextArea.setText("");     // else clear input area.
       System.out.println("Sending to server: " + chat);
       try {
           dos.writeUTF(chat);
           }
       catch(IOException ioe) // Being here probably means we have been in the chat
           {}                 // room for a while but now the connection has gone down.
    // If the line to the server has gone down, we're probably going to see it first
    // in run() which is parked on a readUTF(). We must have a catch, but need not 
    // duplicate the actions taken in the catch in run(). 
    }
 
 
public void run() // receive Thread t enters here.
  {
  try {
      while(true)
         {
         String chatMessage = dis.readUTF(); // wait for server to send.
         outChatTextArea.append(newLine + chatMessage);
         outChatTextArea.setCaretPosition(outChatTextArea.getDocument().getLength()); // scroll to bottom
         inChatTextArea.requestFocus();
         }
      }
  catch (IOException e) // Being here means the line to the server is down.
      {                 // Further send/receive is impossible. We are done.
      outChatTextArea.append(newLine + e.toString());
      outChatTextArea.append(newLine + "Must close the window and then restart client.");
      inChatTextArea.setEditable(false);  // don't allow any more typing
      } 
  // Being at this point means the thread encountered a problem in the receive loop.
  // The jump to the catch block exited the while(true) loop, and continuing
  // out the bottom of the catch block will encounter the end of the run()
  // method, and the thread will return to the Thread object, where it 
  // will be terminated by the Thread object. (perfect!)
  }
}