import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    // for I/O
    private ObjectInputStream sInput; // to read from the socket
    private ObjectOutputStream sOutput; // to write to the socket
    private Socket socket;

    // using GUI or not
    private ClientGUI cg;

    // the server, the port and the username
    private String server, username;
    private int port;

    /*
    * Constructor called by console mode
    * server: the server address
    * port: the port nuimber
    * username: the username
     */
    Client(String server, int port, String username) {
        // which calls the common constructor with the GUI set to null
        this(server, port,username, null);
    }
    /**
     * Constructor called when using the GUI
     * in console mode the ClientGUI parameter is null
     */
    Client(String server, int port, String username, ClientGUI cg) {
        this.server = server;
        this.port = port;
        this.username = username;
        // save if we are in GUI mode or not
        this.cg = cg;
    }
    // to start the dialog
    public boolean start() {
        // try to connect to the server
        try{
            socket = new Socket(server,port);
        }
        // if it failed not much we can do
        catch (Exception ec) {
            display("Error connecting to the server: " + ec);
            return false;
        }
        String msg = "Connection accpeted " + socket.getInetAddress() + ":" + socket.getPort();
        display(msg);

        // creating data stream
        try{
            sInput = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException eIO) {
            display("Exception creating new Input/Output Streams: " + eIO);
            return false;
        }

        // creates the Thread to listen from the server
        new ListenFromServer().start();
        // Send our username to the server this is the only message that we
        // will send as a String. All other messages will be ChatMessage objects.
        try{
            sOutput.writeObject(username);
        } catch (IOException iEO) {
            display("Exception during login: " + iEO);
            disconnect();
            return false;
        }
        // success we inform the caller the id worked
        return true;
    }
    // To send a message to the console or the GUI
    private void display(String msg) {
        if (cg == null) {
            System.out.println(msg);
        } else {
            cg.append(msg + "\n");
        }
    }
    // to send a message to the server
    void sendMessage(ChatMessage msg) {
        try {
            sOutput.writeObject(msg);
        }catch (IOException e) {
            display("Exception writing to the server: " + e);
        }
    }
    /*
        When something goes wrong
        Close the Input/Output streams and disconnect not much to do in the catch clause
     */
    private void disconnect() {
        try{
            if (sInput != null) sInput.close();
        } catch (Exception e) {}
        try{
            if (sOutput != null) sOutput.close();
        } catch (Exception e) {}
        try{
            if (socket != null) socket.close();
        } catch (Exception e) {}
        // inform the GUI
        if (cg != null) cg.connectionFailed();
    }

    public static void main(String[] args) {
        // default values
        int portNumber = 52875;
        String serverAddress = "localhost";
        String userName = "Anonymous";

        // depending on the number of arguments provided we fall through
        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 3:
                serverAddress = args[2];
            //> javac Client username portNumber
            case 2:
                try{
                    portNumber = Integer.parseInt(args[1]);
                }catch (Exception e) {
                    System.out.println("Invalid port number");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
                //> javac Client username
            case 1:
                userName = args[0];
                // > java Client
            case 0:
                break;
            default:
                System.out.println("Usage is: > java Client [username] [portNUmber] [serverAddress]");
                return;
        }

        // create the Client object
        Client client = new Client(serverAddress, portNumber, userName);
        // test if we can start the connection to the Server
        // if it failed , nothing we can do.
        if (!client.start()) return;

        // wait for messages from user
        Scanner scan = new Scanner(System.in);
        // loop forever for message from the user
        while (true) {
            System.out.println("> ");
            // read message from user
            String msg = scan.nextLine();
            // logout if if message is LOGOUT
            if (msg.equalsIgnoreCase("LOGOUT")){
                client.sendMessage(new ChatMessage(ChatMessage.LOGOUT,""));
                // break to disconnect
                break;
            }
            // message WHOISIN
            else if (msg.equalsIgnoreCase("WHOISIN")){
                client.sendMessage(new ChatMessage(ChatMessage.WHOISIN,""));
            } else {
                client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, msg));
            }
        }
        // done disconnect
        client.disconnect();
    }
    /*
    * a class that waits for the message from the server and append them to the JTextArea if we have
    * a GUI or simpoly System.out.println() if in console mode.
     */
    class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                try{
                    String msg = (String) sInput.readObject();
                    // if console mode print the message and add back the prompt
                    if (cg == null) {
                        System.out.println(msg);
                        System.out.print("> ");
                    } else {
                        cg.append(msg);
                    }
                } catch(IOException e) {
                    display("Server has close the connection: " + e);
                    if (cg != null)
                        cg.connectionFailed();
                    break;
                }
                // can't happen with a String object but need the catch anyhow
                catch (ClassNotFoundException e2) {}
            }
        }
    }


}
