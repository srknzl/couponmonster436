package couponmonster436;
import java.io.IOException;
import java.io.BufferedReader;
import java.net.*;
import java.io.InputStreamReader;

public class Server {
    private ServerSocket serverSocket;
    Thread serverThread = null;
    public static final int SERVERPORT = 6000;

    public Server(){
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
        System.out.println("Listening incoming connections on external ip of this device, port 6000...");
    }

    public void interruptThread(){
        this.serverThread.interrupt();
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket();
                InetAddress inetAddress= InetAddress.getByName("0.0.0.0");
                SocketAddress endPoint=new InetSocketAddress(inetAddress, SERVERPORT);
                serverSocket.bind(endPoint);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();

                    CommunicationThread communicationThread = new CommunicationThread(socket);
                    new Thread(communicationThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    class CommunicationThread implements Runnable {

        private Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    System.out.println(read);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
