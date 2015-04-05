package clientapp;

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import javax.xml.ws.WebServiceRef;
import wschatserver.ChatServerService;
import wschatserver.ChatServer;

/**
 * Builds the application clients use to connect to the server and chat to other
 * client connected to the server.
 * @author cdsteer
 */
public class ClientApp {    
    //url of the wsdl file used the to acces the java methods on the server
    @WebServiceRef(wsdlLocation="https://camerons-macbook-air.local:8181/ChatTest/ChatServerService?wsdl")
    
    //GUI varibles
    private JFrame frame;
    private JTextArea myText;
    private static JTextArea otherText;
    private JScrollPane myTextScroll;
    private JScrollPane otherTextScroll;
    private static TextThread otherTextThread;
    private String textString = ""; 
    private static final int HOR_SIZE = 400;
    private static final int VER_SIZE = 150;
    
    //server details and id of user
    private ChatServerService service;
    private ChatServer port;
    private int id;
    
    private void initComponents(String host) {
        //GUI set-up
    	frame = new JFrame("Chat Client");
        myText = new JTextArea();        
        myTextScroll = new JScrollPane(myText);			
        myTextScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        myTextScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	myTextScroll.setMaximumSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
	myTextScroll.setMinimumSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
	myTextScroll.setPreferredSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        myText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                textTyped(evt);
            }
        });
        frame.getContentPane().add(myTextScroll, java.awt.BorderLayout.NORTH);
        otherText = new JTextArea();
        otherTextScroll = new JScrollPane(otherText);
        otherText.setBackground(new java.awt.Color(200, 200, 200));
        otherTextScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        otherTextScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        otherTextScroll.setMaximumSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        otherTextScroll.setMinimumSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        otherTextScroll.setPreferredSize(new java.awt.Dimension(HOR_SIZE, VER_SIZE));
        otherText.setEditable(false);               
        frame.getContentPane().add(otherTextScroll,java.awt.BorderLayout.CENTER);        
        frame.pack();
        frame.setVisible(true);
        
        //print guide on how to use the server
        otherText.append("Change your name using !name:YOUR_NEW_NAME  \n");
        otherText.append("Private message a user using !pm-THEIR_USERNAME:YOUR_MESSAGE  \n");
        
        try {
            service = new wschatserver.ChatServerService();
            port = service.getChatServerPort();
            id = port.join();
            otherTextThread = new TextThread(otherText, id, port);
            otherTextThread.start();
            frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    //leave sends id so the server know witch client to remove
                    port.leave(id);
                } catch (Exception ex) {
                    otherText.append("Exit failed. \n");
                }
                System.exit(0);
            }
          });          
        } catch (Exception ex) {
            otherText.append("Failed to connect to server.\n");
        }
    }
    /**
     * Send the text to the server to be parsed if the enter is pressed, else 
     * continue to add the charters to the message field.
     * If the user enters a backspace run code to remove last char with out 
     * sending the '\b'.
     * @param evt event object of key being pressed
     */
    private void textTyped(java.awt.event.KeyEvent evt) {
        char c = evt.getKeyChar();
        if (c == '\n'){
            try {
                port.parse(id, textString);
            } catch (Exception ie) {
                otherText.append("Failed to send message. \n");
            }
            textString = "";
        } else if (c == '\b') {
            // \b is an illigale char in xml so we must remove the chars in code
            textString = textString.substring(0, textString.length() - 1);            
        }else{
            textString = textString + c;
        }
    }
    /**
     * builds the GUI and connects to the host
     * @param args 
     */
    public static void main(String[] args) {
    	final String host = "localhost";
    	javax.swing.SwingUtilities.invokeLater(new Runnable() {
    		ClientApp client = new ClientApp();
    		public void run() {
    			client.initComponents(host);
    		}
    	});
    	
    }
}

class TextThread extends Thread {
    ObjectInputStream in;
    JTextArea otherText;
    int id;
    ChatServer port;
    
    /**
     * 
     * @param other text field with the messages
     * @param id client id so the server know who is connecting
     * @param port of the server used for method calls
     * @throws IOException 
     */
    TextThread(JTextArea other, int id, ChatServer port) throws IOException {
        otherText = other;
        this.id = id;
        this.port = port;
    }
    /**
     * while the program is running connect to the server and listen for the
     * new messages. Than print the messages in the text space below the text 
     * entry box.
     */
    public void run() {
        while (true) {
            try {    
                String newText = port.listen(id);               
                if (!newText.equals("")) {
                    otherText.append(newText + "\n");
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                otherText.append("Error reading from server. \n");           
            }  
        }
    }
}

