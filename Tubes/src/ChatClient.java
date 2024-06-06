import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 49161;
    private PrintWriter out;
    private BufferedReader in;

    private JTextField usernameField;
    private JTextArea chatArea;
    private JTextField inputField;
    private JList<String> roomList;
    private DefaultListModel<String> roomListModel;

    public ChatClient() {
        initialize();
        connectToServer();
    }

    private void initialize() {
        setTitle("Chat Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        usernameField = new JTextField(20);
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        inputField = new JTextField(30);
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Username:"));
        topPanel.add(usernameField);
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        topPanel.add(connectButton);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(inputField);
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.add(new JLabel("Rooms"), BorderLayout.NORTH);
        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        JPanel roomButtonsPanel = new JPanel(new FlowLayout());
        JButton createRoomButton = new JButton("Create Room");
        createRoomButton.addActionListener(e -> createRoom());
        JButton joinRoomButton = new JButton("Join Room");
        joinRoomButton.addActionListener(e -> joinRoom());
        roomButtonsPanel.add(createRoomButton);
        roomButtonsPanel.add(joinRoomButton);
        roomPanel.add(roomButtonsPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(roomPanel, BorderLayout.EAST);
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        String username = usernameField.getText().trim();
        if (!username.isEmpty()) {
            out.println(username);
        }
    }

    private void createRoom() {
        String roomName = JOptionPane.showInputDialog(this, "Enter room name:");
        if (roomName != null && !roomName.trim().isEmpty()) {
            out.println("/create " + roomName);
        }
    }

    private void joinRoom() {
        String roomName = roomList.getSelectedValue();
        if (roomName != null) {
            out.println("/join " + roomName);
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private class ServerListener implements Runnable {
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/list ")) {
                        updateRoomList(message.substring(6));
                    } else {
                        chatArea.append(message + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ChatClient.this, "Disconnected from server", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        private void updateRoomList(String roomList) {
            SwingUtilities.invokeLater(() -> {
                roomListModel.clear();
                String[] rooms = roomList.split(" ");
                for (String room : rooms) {
                    roomListModel.addElement(room);
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClient client = new ChatClient();
            client.setVisible(true);
        });
    }
}
