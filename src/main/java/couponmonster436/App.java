package couponmonster436;



import java.io.*;
import java.net.*;
import java.util.*;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class App
{
    public static Map<String,Coupon> coupons;
    private static Thread producerThread = null;
    public static final int SERVERPORT = 6000;
    private static ServerSocket serverSocket;

    public static void main( String[] args )
    {
        coupons = new HashMap<>();
        producerThread = new Thread(new ProducerThread());
        producerThread.start();
        try {
            serverSocket = new ServerSocket(App.SERVERPORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket socket = null;
        System.out.println("Listening incoming connections on external ip of this device, port 6000...");
        while (true) {
            try {
                socket = serverSocket.accept();
                System.out.println("Connected");
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
    private Scanner in;
    private PrintWriter out;
    int index = App.coupons.size();
    public CommunicationThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        System.out.println("Connected" + clientSocket);
        try {
            this.in = new Scanner(this.clientSocket.getInputStream());
            this.out = new PrintWriter(this.clientSocket.getOutputStream(),true);
            while(true){
                if(this.clientSocket.getInputStream().available() > 0 && in.hasNextLine()){
                    String read = in.nextLine();
                    processMessages(read);
                }
                if(index < App.coupons.size()){
                    Coupon toBeTransfered = App.coupons.get(index);
                    index++;
                    try {
                        out.println("1" + toBeTransfered.giveMessageForm());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                clientSocket.close();
            } catch (IOException ef) {
                ef.printStackTrace();
            }
        }
    }

    public void processMessages(String message) {
        System.out.println("Incoming message: " + message);
        if (message.charAt(0) == '3') {
            String[] tokens = message.substring(1).split("\\|");
            String hash = tokens[0];
            boolean correct;
            try{
                int answer = Integer.parseInt(tokens[1]);
                correct = App.coupons.get(hash).checkAnswer(answer);
            }catch (NumberFormatException e){
                correct = false;
            }
            if (correct){
                out.println("3Success|"+hash);
            }

        }else if(message.charAt(0) == '4') {

        }else if(message.charAt(0) == '0'){
            StringBuilder coupons = new StringBuilder();
            for (String s : App.coupons.keySet()) {
                coupons.append(App.coupons.get(s).giveMessageForm()).append(";");
            }
            if(coupons.length()>0)coupons = new StringBuilder(coupons.substring(0, coupons.length() - 1));
            out.println("2" + coupons);
        }
    }
}
class ProducerThread implements Runnable{
    public Coupon generate() {
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
        int difficulty = ((int)(Math.random()*20+1));
        String problem = generateProblem(difficulty);
        int result = getResultOfProblem(problem);
        if (result == Integer.MAX_VALUE) return new Coupon();
        return new Coupon(buffer.toString(),new Date(),problem,difficulty*10 + "TL" + " discount for shoppings above " + difficulty*50 + " TL.",result,getTimeOfProblem((difficulty)));
    }
    public static String operator(){
        String opr="s";
        int a=(int)(Math.random()*3+1);
        switch (a){
            case 1:opr="+"; break;
            case 2:opr="-"; break;
            case 3:opr="*"; break;
        }
        return opr;
    }
    public static String generateProblem(int difficulty){
        StringBuilder question = new StringBuilder();
        int a=(int)(Math.random()*difficulty+1);
        question.append(a);
        boolean close = false;
        int openTime = 0;
        int opened = 0;
        int closed = 0;
        for(int i=0;i<difficulty/2;i++){
            String operator = operator();
            if(operator.equals("*"))
                a=(int)(Math.random()*difficulty/2+1);
            else
                a=(int)(Math.random()*difficulty+1);
            question.append(operator);
            if(Math.random() > 0.5 && !close){
                question.append("(");
                openTime = i;
                close = true;
                opened++;
            }
            question.append(a);
            if(Math.random()>0.5 && close && i-openTime > 1){
                question.append(")");
                close = false;
                closed++;
            }
        }
        while (closed < opened){
            question.append(")");
            closed++;
        }
        return question.toString();
    }
    public static int getTimeOfProblem(int difficulty){
        return Math.max((int)Math.ceil(difficulty/1.4),3);
    }
    public static int getResultOfProblem(String problem){
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        try {
            return (int) engine.eval(problem);
        }catch (ScriptException e){
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }
    @Override
    public void run() {
        while(true){
            if(App.coupons.size()<20){
                Coupon coupon = generate();
                if(!coupon.getHash().equals("")){
                    App.coupons.put(coupon.getHash(),coupon);
                }else {
                    System.out.println("Coupon could not generated");
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

