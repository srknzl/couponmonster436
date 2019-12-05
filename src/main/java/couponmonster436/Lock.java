package couponmonster436;

class Lock { // used for mutual exclusion
    private boolean value;
    Lock(boolean initValue) {
        value = initValue;
    }
    synchronized boolean P() { // atomic operation // blocking
        if(!value)return false;
        value = false;
        return true;
    }
    synchronized void V() { // atomic operation // non-blocking
        value = true;
    }
}