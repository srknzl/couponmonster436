package couponmonster436;

public class Lock {
    static boolean available = true;
    public Lock(){
    }
    public boolean getLock(){
        if (!available){
           return false;
        }
        available = false;
        return true;
    }
    public void releaseLock(){
        available = true;
    }
}
