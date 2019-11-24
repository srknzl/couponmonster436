package couponmonster436;

class Lock {
    private boolean available = true;
    Lock(){
    }
    boolean getLock(){
        if (!available){
           return false;
        }
        available = false;
        return true;
    }
    void releaseLock(){
        available = true;
    }
}
