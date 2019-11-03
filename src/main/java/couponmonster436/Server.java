package couponmonster436;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Random;


public class Server {
    private ServerSocket serverSocket;
    Thread serverThread = null;
    Thread producerThread = null;
    public static final int SERVERPORT = 6000;
    private static HashMap<Integer,String> coupons;
    private static int counter = 0;

    public Server(){
        coupons = new HashMap<>();
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
        this.producerThread = new Thread(new ProducerThread());
        this.producerThread.start();
        System.out.println("Listening incoming connections on external ip of this device, port 6000...");
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
        private BufferedWriter output;
        private int lastLength = 0;


        public CommunicationThread(Socket clientSocket) {

            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
                this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();
                    if(read == null) {
                        System.out.println("Disconnected");
                        return;
                    }
                    if(read.equals("coupons")){

                    }else if(read.equals("select coupon")){

                    }else if(read.equals("answer coupon")){

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void informNewCoupons(){
            int newSize = coupons.size();
            if(lastLength == newSize){
                return;
            }
            for(int i = lastLength; i < newSize;i++){
                try {
                    this.output.write(coupons.get(i));
                    this.output.newLine();
                    this.output.flush();
                }catch (IOException e1){
                    e1.printStackTrace();
                }
            }
        }

    }
    class ProducerThread implements Runnable{
        public String generate() {
            int leftLimit = 97; // letter 'a'
            int rightLimit = 122; // letter 'z'
            int targetStringLength = 30;
            Random random = new Random();
            StringBuilder buffer = new StringBuilder(targetStringLength);
            for (int i = 0; i < targetStringLength; i++) {
                int randomLimitedInt = leftLimit + (int)
                        (random.nextFloat() * (rightLimit - leftLimit + 1));
                buffer.append((char) randomLimitedInt);
            }
            return buffer.toString();
        }
        @Override
        public void run() {
            while(true){
                String token = generate();
                System.out.println(token);
                try {
                    Thread.sleep((Math.round(Math.random()*30000))); // 0 to 30 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                coupons.put(counter,token);
                counter++;
            }
        }
    }
}
