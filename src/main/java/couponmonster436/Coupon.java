package couponmonster436;
import java.util.Date;

public class Coupon {
    private String hash;
    private Date creationDate;
    private String problem;
    private int answer;
    private String reward;
    Lock lock;


    private int solveTime;
    Coupon(String hash, Date creationDate, String problem, String reward, int answer, int solveTime){
        this.hash = hash;
        this.creationDate = creationDate;
        this.problem = problem;
        this.answer = answer;
        this.reward = reward;
        this.solveTime = solveTime;
        this.lock = new Lock();
    }
    Coupon(){
        this.hash = "";
        this.creationDate = new Date();
        this.problem = "";
        this.reward = "";
        this.answer = 0;
        this.solveTime = 0;
        this.lock = new Lock();
    }

    String getHash(){
        return this.hash;
    }

    String getReward(){
        return this.reward;
    }
    boolean checkAnswer(int answer){
        return this.answer == answer;
    }

    @Override
    public String toString() {
        return "Coupon{" + "\n" +
                "hash='" + hash + '\'' + "\n" +
                ", creationDate=" + creationDate + "\n" +
                ", problem='" + problem + '\'' + "\n" +
                ", answer=" + answer + "\n" +
                ", reward='" + reward + '\'' + "\n" +
                ", solveTime=" + solveTime + "\n" +
                '}';
    }
    public String giveMessageForm(){
        return hash + "|" + problem + "|" + answer + "|" + reward + "|" + solveTime;
    }
}