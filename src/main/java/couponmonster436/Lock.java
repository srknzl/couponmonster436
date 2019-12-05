package couponmonster436;

public class Lock { // used for mutual exclusion
    private boolean value;
    Lock(boolean initValue) {
        value = initValue;
    }
    public synchronized boolean P() { // atomic operation // blocking
        if(!value)return false;
        value = false;
        return true;
    }
    public synchronized void V() { // atomic operation // non-blocking
        value = true;
    }
}