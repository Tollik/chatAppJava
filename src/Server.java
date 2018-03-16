import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
The server that can be run both as a console application or a GUI
 */
public class Server {
    // a uniques ID for each connection
    private static int uniqueId;
    // an ArrayList to keep the list of the Client
    private ArrayList<ClientThread> a1;
    // if i am in a GUI
    private ServerGUI sg;
    // to display time
    private SimpleDateFormat sdf;
    // the port number to listen for connection
    private int port;
    // the boolean that will be turned off to stop the server
    private boolean keepGoing;

    /*
    Server constructor that receive the port to listen to for connection as parameter
    *
    * in console
     */
    public Server(int port) {
        this(port,null);
    }

    public Server(int port, ServerGUI sg) {
        //GUI or not
        this.sg = sg;
        // the port
        this.port = port;
        // to display hh:mm:ss
        sdf = new SimpleDateFormat("HH:mm:ss");
        //ArrayList for the client list
        a1 = new ArrayList<ClientThread>();
    }
    public void start() {
        keepGoing = true;
        // create socket server and wait for connection request
        try{
            // create the server socket and wait for connection requests.
            ServerSocket serverSocket = new ServerSocket(port);
            // infinite loop to wait for connections
            while (keepGoing) {
                // format message saying we are waiting
                display("Server waiting for Client on port " + port + ".");

                Socket socket = serverSocket.accept(); // accept connection

                // if i was asked to stop
                if (!keepGoing)
                    break;
                ClientThread t = new ClientThread(socket); // make a thread of it
                a1.add(t); // save it in the arrayList
                t.start();
            }
            // i was asked to stop
            try{
                serverSocket.close();
                for (int i = 0; i < a1.size(); ++i) {
                    ClientThread tc = a1.get(i);
                    try{
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch (IOException ioE) {
                        // not much i can do
                    }
                }
            }catch (Exception e) {
                display("Exception closing the server and clients: " + e);
            }

        }catch (IOException e) {
            String msg = sdf.format(new Date()) + "Exception on new Server socket" + e + "\n";
            display(msg);
        }
    }
    /*
    for the GUI to stop the server
     */
    protected void stop() {
        keepGoing = false;
        // connect to myself as Client to exit statement
        // Socket socket = serverSocket.accept();
        try{
            new Socket("localhost",port);
        } catch (Exception e) {
            // nothing i can really do
        }
    }
    /*
    display an event (not a message) to the console or the GUI
     */
    protected void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        if (sg == null) {
            System.out.println(time);
        }else {
            sg.appendEvent(time + "\n");
        }
    }
    private synchronized void broadcast(String message) {
        // add HH:MM:SS and \n to a message
        String time = sdf.format(new Date());
        String messageLF = time + " " + message + "\n";
        // display message on console or GUI
        if (sg == null) {
            System.out.println(messageLF);
        }else {
            sg.appendRoom(messageLF); // append in the room window
        }
        // we loop in reverse order in case w would have to remove a client
        // because it has disconnected
        for (int i = a1.size(); --i >= 0;) {
            ClientThread ct = a1.get(i);
            // try to write to the Client ofit fails remove it from the list
            if (!ct.writeMsg(messageLF)) {
                a1.remove(i);
                display("Disconnected Client " + ct.username + "removed from the list");
            }
        }
    }
    // for a client who logoff using the LOGOUT message
    synchronized void remove(int id) {
        // scan the array list untill we found the id
        for (int i = 0; i < a1.size(); ++i) {
            ClientThread ct = a1.get(i);
            // found it
            if (ct.id == id) {
                a1.remove(i);
                return;
            }
        }
    }
    /*
    To run as a console application just open a console window and:
    > java Server
    > java server portNUmiber
    If the port number is not specified , 1500 is used
     */
    public static void main(String[] args) {
        // start server on port1500 unless a PortNumber is specified
        int portNumber = 52875;
        switch (args.length) {
            case 1:
                try{
                    portNumber = Integer.parseInt(args[0]);
                }catch (Exception e) {
                    System.out.println("Invalid port number");
                    System.out.println("Usage is: > java Server [portNumber]");
                    return;
                }
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Server [portNumber]");
                return;
        }
        // create a server object and start it
        Server server = new Server(portNumber);
        server.start();
    }
    /*
    One instance of this thread will run for each client
     */
    class ClientThread extends Thread {
        // the socket where to listen/talk
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        // my unique id (easier for disconnection)
        int id;
        // the username of the client
        String username;
        // the only type of message a will receive
        ChatMessage cm;
        // the date i connect
        String date;

        // Constructor
        ClientThread(Socket socket) {
            // a unique id
            id = ++uniqueId;
            this.socket = socket;
            // Creating both data stream
            System.out.println("Thread trying to create object INput/Output Stream");
            try {
                // create output first
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                // read the username
                username = (String) sInput.readObject();
                display(username + " just connected");
            }catch (IOException e) {
                display("Exception creating new input/output Streams: " + e);
                return;
            }
            // have to catch ClassNotFoundException
            // but we read a String, we sure it'll work
            catch (ClassNotFoundException e) {

            }
            date = new Date().toString() + "\n";
        }
        // what will run forever
        public void run() {
            // to loop until LOGOUT
            boolean keepGoing = true;
            while (keepGoing) {
                // read a String (which is an object)
                try{
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException e) {
                    display(username + " Exception reading Streams: " + e);
                    break;
                }
                catch (ClassNotFoundException e2){
                    break;
                }
                // the message part of the ChatMessage
                String message = cm.getMessage();

                // Switch on the type of message receive
                switch (cm.getType()) {
                    case ChatMessage.MESSAGE:
                        broadcast(username + ": " + message);
                        break;
                    case ChatMessage.LOGOUT:
                        display(username + " disconnected with a LOGOUT message.");
                        keepGoing = false;
                        break;
                    case ChatMessage.WHOISIN:
                        writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                        // scan a1 the users connected
                        for (int i = 0; i < a1.size(); ++i) {
                            ClientThread ct = a1.get(i);
                            writeMsg((i+1)+ ") " + ct.username + " since " + ct.date);
                        }
                        break;
                }
            }
            // remove myself from the arrayList containing the list of the connected Clients
            remove(id);
            close();
        }
        // try to close everything
        private void close() {
            // try to close the connetion
            try{
                if (sOutput != null) sOutput.close();
            } catch (Exception e) {

            }
            try {
                if (sInput != null) sInput.close();
            } catch (Exception e) {}
            try {
                if (socket != null) socket.close();
            } catch (Exception e) {}

        }
        /*
            Write a String tothe Client output stream
             */
        private boolean writeMsg(String msg) {
            // if client is still connected sent the message to it
            if (!socket.isConnected()) {
                close();
                return false;
            }
            // write the message to the stream
            try {
                sOutput.writeObject(msg);
            }
            // if an error occurs , do not just inform the user
            catch(IOException e){
                display("Error sending message to " + username);
                display(e.toString());
            }
            return true;
        }
    }
}
