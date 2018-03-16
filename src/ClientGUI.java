import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientGUI extends JFrame implements ActionListener {
    private static final long serialVersionUID = 1L;
    // will first hold "Username:", later on "Enter Message"
    private JLabel label;
    // to hold the Username and later on the messages
    private JTextField tf;
    // to hold the server address and the port number
    private JTextField tfServer, tfPort;
    // to logout and get the list of the users
    private JButton login, logout, whoIsIn;
    // for the chat room
    private JTextArea ta;
    // if it's for connection
    private boolean connected;
    // the client object
    private Client client;
    // the default port number
    private int defaultPort;
    private String defaultHost;

    // Constructor connection receiving a socket number
    ClientGUI(String host, int port) {

        super("Chat Client");
        defaultPort = port;
        defaultHost = host;

        // the NorthPanel with:
        JPanel northPanel = new JPanel(new GridLayout(3,1));
        // the server name and the port number
        JPanel serverAndPort = new JPanel(new GridLayout(1,5,1,3));
        // the two JTextField with default value for server address and port number
        tfServer = new JTextField(host);
        tfPort = new JTextField("" + port);
        tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

        serverAndPort.add(new JLabel("Server Address:   "));
        serverAndPort.add(tfServer);
        serverAndPort.add(new JLabel("Port Number:  "));
        serverAndPort.add(tfPort);
        serverAndPort.add(new JLabel(""));
        // adds the server and port field to the GUI
        northPanel.add(serverAndPort);

        // the label and the  TextField
        label = new JLabel("Enter your username below", SwingConstants.CENTER);
        northPanel.add(label);
        tf = new JTextField("Anonymous");
        tf.setBackground(Color.WHITE);
        northPanel.add(tf);
        add(northPanel, BorderLayout.NORTH);

        // The center panel which is the chat room
        ta = new JTextArea("Welcome to the chat room\n", 80,80);
        JPanel centerPanel = new JPanel(new GridLayout(1,1));
        centerPanel.add(new JScrollPane(ta));
        ta.setEditable(false);
        add(centerPanel,BorderLayout.CENTER);

        // the 3 buttons
        login = new JButton("Login");
        login.addActionListener(this);
        logout = new JButton("Logout");
        logout.addActionListener(this);
        logout.setEnabled(false); // you have to ogin before being able to logout
        whoIsIn = new JButton("Who is in");
        whoIsIn.addActionListener(this);
        whoIsIn.setEnabled(false);

        JPanel southPanel = new JPanel();

        southPanel.add(login);
        southPanel.add(logout);
        southPanel.add(whoIsIn);
        add(southPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(600,600);
        setVisible(true);
        tf.requestFocus();
    }

    // called by the client to append text in the TextArea
    void append(String str) {
        ta.append(str);
        ta.setCaretPosition(ta.getText().length() - 1);
    }
    // called by the GUI is the connection failed
    // we reset our buttons, label, textfield
    void connectionFailed() {
        login.setEnabled(true);
        logout.setEnabled(false);
        whoIsIn.setEnabled(false);
        label.setText("Enter your username below");
        tf.setText("Anonymous");
        // reset port numiber and host name as a construction frame
        tfPort.setText("" + defaultPort);
        tfServer.setText("" + defaultHost);
        // let the user change them
        tfServer.setEditable(false);
        tfPort.setEditable(false);
        // don't react to a <CR> after the username
        tf.removeActionListener(this);
        connected = false;
    }
    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();
        // if it is the Logout button
        if (o == logout) {
            client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
            return;
        }
        // if it the who is in button
        if (o == whoIsIn) {
            client.sendMessage(new ChatMessage(ChatMessage.WHOISIN,""));
            return;
        }
        // OK it is coming from the JTextField
        if (connected) {
            client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf.getText()));
            tf.setText("");
            return;
        }
        if (o == login) {
            // ok it is a connection request
            String username = tf.getText().trim();
            // empty username ignore it
            if (username.length() == 0) {
                return;
            }
            // mpty server address ignore it
            String server = tfServer.getText().trim();
            if (server.length() == 0) {
                return;
            }
            // empty of invalid port nuimber
            String portNumber = tfPort.getText().trim();
            if (portNumber.length() == 0) {
                return;
            }
            int port = 0;
            try{
                port = Integer.parseInt(portNumber);
            }catch (Exception ex) {
                return;
            }
         // try creating a new client with GUI
         client = new Client(server,port, username, this);
            // test if we can start the client
            if (!client.start()) return;;
            tf.setText("");
            label.setText("Enter your message below");
            connected = true;

            // disbale login button
            login.setEnabled(false);
            // enable the two buttons
            logout.setEnabled(true);
            whoIsIn.setEnabled(true);
            // disable the server and port JTextField
            tfServer.setEditable(false);
            tfPort.setEditable(false);
            // action listener for when the user enter a message
            tf.addActionListener(this);
        }
    }
    // to start the whole thing the server
    public static void main(String[] args) {
        new ClientGUI("localhost", 52875);
    }
}
