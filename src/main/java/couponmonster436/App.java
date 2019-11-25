package couponmonster436;



import java.io.*;
import java.net.*;
import java.util.*;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class App
{
    static Map<String,Coupon> coupons;
    static LinkedList<String> broadCastMessages;
    static Vector<String> producedCouponHashes;
    static Vector<User> users;
    static Vector<CommunicationThread> Communications;
    private static final int SERVERPORT = 6000;
    private static ServerSocket serverSocket;

    public static void main( String[] args )
    {
        users = new Vector<>();
        producedCouponHashes = new Vector<>();
        Communications = new Vector<>();
        coupons = new HashMap<>();
        broadCastMessages = new LinkedList<>();
        Thread producerThread = new Thread(new ProducerThread());
        producerThread.start();
        try {
            serverSocket = new ServerSocket(App.SERVERPORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket socket;
        System.out.println("Listening incoming connections on external ip of this device, port 6000...");
        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                socket = serverSocket.accept();
                System.out.println("Connected");
                CommunicationThread communicationThread = new CommunicationThread(socket);
                Communications.add(communicationThread);
                new Thread(communicationThread).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
class CommunicationThread implements Runnable {
    private Socket clientSocket;
    private PrintWriter out;
    private Coupon selectedCoupon;
    private String username = "";
    private String name = "";
    private int score = 0;
    private int pulseCounter = 0;

    private int index = App.producedCouponHashes.size();
    private int broadcastIndex = App.broadCastMessages.size();
    CommunicationThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        System.out.println("Connected" + clientSocket);
        try {
            Scanner in = new Scanner(this.clientSocket.getInputStream());
            this.out = new PrintWriter(this.clientSocket.getOutputStream(),true);
            while(true){
                pulseCounter = (pulseCounter+1)%10;
                if(pulseCounter == 0)out.println("9");
                if(out.checkError() || Thread.interrupted()){
                    if(selectedCoupon!=null)selectedCoupon.lock.releaseLock();
                    for (User nextUser : App.users) {
                        if (nextUser.username.equals(this.username)) {
                            App.users.remove(nextUser);
                            break;
                        }
                    }
                    App.Communications.removeElementAt(App.Communications.indexOf(this));
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
            App.Communications.removeElementAt(App.Communications.indexOf(this));
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
    private void processMessages(String message) {
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
            //System.out.println("Trying get lock of: "+ hash);
            //System.out.println("Coupon: "+ c.toString());
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
            if(App.coupons.get(hash) != null){
                //System.out.println("Release lock of "+hash);
                App.coupons.get(hash).lock.releaseLock();
            }
        }else if(message.charAt(0) == '6'){
            out.println("6"+this.name+"|"+this.username+"|"+this.score);
        }else if(message.charAt(0) == '7'){
            StringBuilder users = new StringBuilder();
            for (User s : App.users) {
                users.append(s.name).append("|").append(s.username).append("|").append(s.score).append(";");
            }
            if(users.length()>0)users = new StringBuilder(users.substring(0, users.length() - 1));
            out.println("7" + users);
        }else if(message.charAt(0) == '8'){
            String[] tokens = message.substring(1).split("\\|",3);
            String name = tokens[0];
            String username = tokens[1];
            String currentUsername = tokens[2];
            User toBeChanged = new User("","");
            CommunicationThread threadToBeUpdated = null;
            for (CommunicationThread c : App.Communications){
                if(c.username.equals(currentUsername)){
                    threadToBeUpdated = c;
                }
            }
            for (User nextUser : App.users) {
                if (nextUser.username.equals(currentUsername)) {
                    toBeChanged = nextUser;
                    break;
                }
            }
            if(App.users.contains(new User("", username))){
                if(threadToBeUpdated != null){
                    toBeChanged.name = name;
                    threadToBeUpdated.name = name;
                    out.println("8No" + "|" + threadToBeUpdated.name  + "|" + threadToBeUpdated.username);
                }else{
                    out.println("8No"+ "|" + name + "|" + username);
                }
            }else{
                if(threadToBeUpdated != null){
                    toBeChanged.name = name;
                    threadToBeUpdated.name = name;
                    toBeChanged.username = username;
                    threadToBeUpdated.username = username;
                    out.println("8Yes" + "|" + threadToBeUpdated.name + "|" + threadToBeUpdated.username);
                }else {
                    out.println("8No" + "|" + name + "|" + username);
                }

            }
        }
    }
}
class ProducerThread implements Runnable{
    private Coupon generate() {
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
    private static String operator(){
        String opr="s";
        int a=(int)(Math.random()*3+1);
        switch (a){
            case 1:opr="+"; break;
            case 2:opr="-"; break;
            case 3:opr="*"; break;
        }
        return opr;
    }
    private static String generateProblem(int difficulty){
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
    private static int getTimeOfProblem(int difficulty){
        return Math.max((int)Math.ceil(difficulty/1.4),3);
    }
    private static int getResultOfProblem(String problem){
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
        //noinspection InfiniteLoopStatement
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

