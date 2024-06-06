import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 49161;
    private static final Map<String, ChatRoom> chatRooms = Collections.synchronizedMap(new HashMap<>());
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println("Chat Server started...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ChatRoom {
        String name;
        Set<ClientHandler> members;

        ChatRoom(String name) {
            this.name = name;
            this.members = Collections.synchronizedSet(new HashSet<>());
        }

        void broadcast(String message, ClientHandler sender) {
            synchronized (members) {
                for (ClientHandler member : members) {
                    if (member != sender) {
                        member.out.println(message);
                    }
                }
            }
        }
    }

    private static void broadcastRoomList() {
        StringBuilder roomList = new StringBuilder("/list ");
        for (String roomName : chatRooms.keySet()) {
            roomList.append(roomName).append(" ");
        }
        String roomListMessage = roomList.toString().trim();
        for (ClientHandler client : clients) {
            client.out.println(roomListMessage);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private ChatRoom currentRoom;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                out.println("Enter your username:");
                username = in.readLine();
                System.out.println("Username received: " + username);
                
                out.println("Welcome, " + username + "! You are connected to the chat server.");

                String input;
                while ((input = in.readLine()) != null) {
                    handleCommand(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (currentRoom != null) {
                    leaveRoom();
                }
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleCommand(String command) {
            if (command.startsWith("/create ")) {
                createRoom(command.substring(8));
            } else if (command.startsWith("/join ")) {
                joinRoom(command.substring(6));
            } else if (command.startsWith("/leave")) {
                leaveRoom();
            } else if (command.startsWith("/list")) {
                listRooms();
            } else if (command.startsWith("/kick ")) {
                kickUser(command.substring(6));
            } else {
                sendMessageToRoom(command);
            }
        }

        private void createRoom(String roomName) {
            synchronized (chatRooms) {
                if (chatRooms.containsKey(roomName)) {
                    out.println("Room already exists.");
                } else {
                    ChatRoom room = new ChatRoom(roomName);
                    chatRooms.put(roomName, room);
                    out.println("Room " + roomName + " created.");
                    broadcastRoomList();
                }
            }
        }

        private void joinRoom(String roomName) {
            synchronized (chatRooms) {
                if (chatRooms.containsKey(roomName)) {
                    if (currentRoom != null) {
                        leaveRoom();
                    }
                    currentRoom = chatRooms.get(roomName);
                    currentRoom.members.add(this);
                    out.println("Joined room " + roomName);
                    currentRoom.broadcast(username + " has joined the room.", this);
                } else {
                    out.println("Room does not exist.");
                }
            }
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                currentRoom.members.remove(this);
                currentRoom.broadcast(username + " has left the room.", this);
                out.println("Left the room.");
                currentRoom = null;
            }
        }

        private void listRooms() {
            StringBuilder roomList = new StringBuilder("Available rooms:");
            for (String roomName : chatRooms.keySet()) {
                roomList.append("\n").append(roomName);
            }
            out.println(roomList.toString());
        }

        private void kickUser(String username) {
            if (currentRoom != null) {
                synchronized (currentRoom.members) {
                    for (ClientHandler member : currentRoom.members) {
                        if (member.username.equals(username)) {
                            member.out.println("You have been kicked from the room.");
                            member.leaveRoom();
                            break;
                        }
                    }
                }
            }
        }

        private void sendMessageToRoom(String message) {
            if (currentRoom != null) {
                String fullMessage = username + ": " + message;
                synchronized (currentRoom.members) {  // Ensure thread-safety
                    for (ClientHandler member : currentRoom.members) {
                        member.out.println(fullMessage);
                    }
                }
            } else {
                out.println("You are not in a room.");
            }
        }
    }
}
