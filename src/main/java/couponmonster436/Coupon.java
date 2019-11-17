package couponmonster436;
import java.util.Date;

public class Coupon {
    private String hash;
    private Date creationDate;
    private String problem;
    private int answer;
    private String reward;

    public int getSolveTime() {
        return solveTime;
    }
    private int solveTime;
    public Coupon(String hash, Date creationDate, String problem, String reward, int answer,int solveTime){
        this.hash = hash;
        this.creationDate = creationDate;
        this.problem = problem;
        this.answer = answer;
        this.reward = reward;
        this.solveTime = solveTime;
    }
    public Coupon(){
        this.hash = "";
        this.creationDate = new Date();
        this.problem = "";
        this.reward = "";
        this.answer = 0;
        this.solveTime = 0;
    }

    public String getProblem(){
        return this.problem;
    }
    public String getHash(){
        return this.hash;
    }

    public String getReward(){
        return this.reward;
    }
    public boolean checkAnswer(int answer){
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