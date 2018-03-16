import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class ServerGUI extends JFrame implements ActionListener, WindowListener{
    private static final long serialVersionUID = 1L;
    // the stop and start button
    private JButton stopStart;
    // JTextArea for the chat room and the event
    private JTextArea chat, event;
    // The port number
    private JTextField tPortNumber;
    // my server
    private Server server;

    //server constructor that receive rhe port to listen to for connection as parameter
    ServerGUI(int port) {
        super("Chat Server");
        server = null;
        // in the NorthPanel the PortNUmber the Start and Stop Buttons
        JPanel north = new JPanel();
        north.add(new JLabel("Port Number: "));
        tPortNumber = new JTextField("  " + port);
        north.add(tPortNumber);
        // to stop or start the server, we start with "Start"
        stopStart = new JButton("Start");
        stopStart.addActionListener(this);
        north.add(stopStart);
        add(north, BorderLayout.NORTH);

        // the event and chat room
        JPanel center = new JPanel(new GridLayout(2,1));
        chat = new JTextArea(80,80);
        chat.setEditable(false);
        appendRoom("Chat room.\n");
        center.add(new JScrollPane(chat));
        event = new JTextArea(80,80);
        event.setEditable(false);
        appendEvent("Events log.\n");
        center.add(new JScrollPane(event));
        add(center);

        //need to be informed when the user click the close button on the frame
        addWindowListener(this);
        setSize(400,600);
        setVisible(true);
    }

    // append message to the two JTextArea
    // position at the end
    void appendRoom(String str) {
        chat.append(str);
        chat.setCaretPosition(chat.getText().length() - 1);
    }
    void appendEvent(String str) {
        event.append(str);
        event.setCaretPosition(chat.getText().length() - 1);
    }

    // start or stop where clicked
    public void actionPerformed(ActionEvent e) {
        // if running we have to stop
        if (server != null) {
            server.stop();
            server = null;
            tPortNumber.setEditable(true);
            stopStart.setText("Start");
            return;
        }
        // ok start the server
        int port;
        try {
            port = Integer.parseInt(tPortNumber.getText().trim());
        } catch (Exception ex) {
            appendEvent("Invalid port number");
            return;
        }
        // create a new server
        server = new Server(port, this);
        // and start it as a thread
        new ServerRunning().start();
        stopStart.setText("Stop");
        tPortNumber.setEditable(false);
    }
    // entry point to start the server
    public static void main(String[] args) {
        // start server default port 1500
        new ServerGUI(52875);
    }
    /*
    if the user click the X button to close the application
    we need to close the connection with the server to free the port
     */
    public void windowClosing(WindowEvent e) {
        // if my server exists
        if (server != null) {
            try{
                server.stop();
            } catch (Exception eClose) {}
            server = null;
        }
        // dispose the frame
        dispose();
        System.exit(0);
    }
    // we can ignore other WindowListener method
    public void windowClosed(WindowEvent e) {}
    public void windowOpened(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowActivated(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}

    /* A thread to run the server
     */
    class ServerRunning extends Thread {
        public void run() {
            server.start(); // should execute until it fails
            // the server failed
            stopStart.setText("Start");
            tPortNumber.setEditable(true);
            appendEvent("Server crashed.\n");
            server = null;
        }
    }
}
