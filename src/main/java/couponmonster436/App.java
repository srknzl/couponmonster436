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
    public static LinkedList<String> broadCastMessages;
    public static Vector<String> producedCouponHashes;
    public static HashSet<User> users;
    private static Thread producerThread = null;
    public static final int SERVERPORT = 6000;
    private static ServerSocket serverSocket;

    public static void main( String[] args )
    {
        users = new HashSet<>();
        producedCouponHashes = new Vector<>();
        coupons = new HashMap<>();
        broadCastMessages = new LinkedList<>();
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
    private Coupon selectedCoupon;
    private String username;
    private String name;
    private int score;

    int index = App.producedCouponHashes.size();
    int broadcastIndex = App.broadCastMessages.size();
    public CommunicationThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        System.out.println("Connected" + clientSocket);
        try {
            this.in = new Scanner(this.clientSocket.getInputStream());
            this.out = new PrintWriter(this.clientSocket.getOutputStream(),true);
            while(true){
                out.println("9");
                if(out.checkError() || Thread.interrupted()){
                    if(selectedCoupon!=null)selectedCoupon.lock.releaseLock();
                    for (User nextUser : App.users) {
                        if (nextUser.username.equals(this.username)) {
                            App.users.remove(nextUser);
                            break;
                        }
                    }
                    return;
                }
                if(this.clientSocket.getInputStream().available() >0 && in.hasNextLine()){
                    String read = in.nextLine();
                    processMessages(read);
                }

                if(index < App.producedCouponHashes.size()){
                    String hash = App.producedCouponHashes.get(index);
                    index++;
                    if(App.coupons.get(hash) != null){
                        try {
                            out.println("1" + App.coupons.get(hash).giveMessageForm());
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                while(broadcastIndex < App.broadCastMessages.size()){
                    String message = App.broadCastMessages.get(broadcastIndex);
                    System.out.println("Broadcasting "+ message);
                    broadcastIndex++;
                    out.println(message);
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
            if(selectedCoupon != null)selectedCoupon.lock.releaseLock();
        }
    }
    /*
     0 Get: Hello  -> Send: Welcome + initial coupons
     1 Send: New Coupon
     2 Send: Clear coupon
     3 Get: Answer -> Send: True/False
     4 Get: Selection -> Send: True/False
     5 Get: Dismiss
     6 Send: User data
     7 Send: All users
     8 Get: New name and username Send: Yes/No
     9 Get: Pulse Send: Pulse
    */
    public void processMessages(String message) {
        if(!message.equals("9"))System.out.println("Incoming message: " + message);
        if(message.charAt(0) == '0'){
            String[] tokens = message.substring(1).split("\\|",2);
            String name = tokens[0];
            String username = tokens[1];
            if(name.equals("")){
               name = "Coupon Monster";
            }
            if(username.equals("")){
                int random=0;
                while (App.users.contains(new User("","couponmonster"+random))){
                    random = ((int)Math.floor(Math.random()*10000));
                }
                username = "couponmonster"+random;
            }
            this.username = username;
            this.name = name;
            this.score = 0;
            App.users.add(new User(name,username));
            StringBuilder coupons = new StringBuilder();
            for (String s : App.coupons.keySet()) {
                coupons.append(App.coupons.get(s).giveMessageForm()).append(";");
            }
            if(coupons.length()>0)coupons = new StringBuilder(coupons.substring(0, coupons.length() - 1));
            out.println("0" + coupons);
            out.println("6"+this.name+"|"+this.username+"|"+this.score);
        }else if (message.charAt(0) == '3') {
            String[] tokens = message.substring(1).split("\\|");
            String hash = tokens[0];
            boolean correct;
            try{
                int answer = Integer.parseInt(tokens[1]);
                correct = App.coupons.get(hash).checkAnswer(answer);
            }catch (Exception e){
                correct = false;
            }
            if (correct){
                out.println("3Yes|" + hash);
                String reward = App.coupons.get(hash).getReward();
                int indexOfFirstTL = reward.indexOf("TL");
                int difficulty = Integer.parseInt(reward.substring(0,indexOfFirstTL))/10;
                for (User nextUser : App.users) {
                    if (nextUser.username.equals(this.username)) {
                        nextUser.score += difficulty;
                        this.score = nextUser.score;
                        break;
                    }
                }
                App.broadCastMessages.add("2" + hash + "|" + this.name + "|" + this.username + "|" + difficulty);
                App.coupons.remove(hash);
                System.out.println("Correct answer");
            }else{
                out.println("3No|" + hash);
                System.out.println("Wrong answer");
                if(App.coupons.get(hash) != null)App.coupons.get(hash).lock.releaseLock();
            }
        }else if(message.charAt(0) == '4') {
            String hash = message.substring(1);
            Coupon c = App.coupons.get(hash);
            if(c.lock.getLock()){
                selectedCoupon = c;
                out.println("4Yes|"+hash);
                System.out.println("Outgoing yes");
            }else{
                out.println("4No|"+hash);
                System.out.println("Outgoing no");
            }
        }else if(message.charAt(0) == '5'){
            String[] tokens = message.substring(1).split("\\|");
            String hash = tokens[0];
            if(selectedCoupon != null && selectedCoupon.getHash().equals(hash))selectedCoupon = null;
            if(App.coupons.get(hash) != null)App.coupons.get(hash).lock.releaseLock();
        }else if(message.charAt(0) == '6'){
            out.println("6"+this.name+"|"+this.username+"|"+this.score);
        }else if(message.charAt(0) == '7'){
            StringBuilder users = new StringBuilder();
            for (User s : App.users) {
                users.append(s.name+"|").append(s.username+"|").append(s.score).append(";");
            }
            if(users.length()>0)users = new StringBuilder(users.substring(0, users.length() - 1));
            out.println("7" + users);
        }else if(message.charAt(0) == '8'){
            String[] tokens = message.substring(1).split("\\|");
            String name = tokens[0];
            String username = tokens[1];
            User toBeChanged = null;
            for (User nextUser : App.users) {
                if (nextUser.username.equals(username)) {
                    toBeChanged = nextUser;
                    break;
                }
            }
            if(App.users.contains(new User("", username))){
                if(toBeChanged != null)toBeChanged.name = name;
                out.println("8No");
            }else{
                if(toBeChanged != null){
                    toBeChanged.name = name;
                    toBeChanged.username = username;
                    out.println("8Yes");
                }
            }
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
                try {
                    Thread.sleep((int)Math.ceil(Math.random()*15000));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!coupon.getHash().equals("")){
                    App.coupons.put(coupon.getHash(),coupon);
                    App.producedCouponHashes.add(coupon.getHash());
                }else {
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

