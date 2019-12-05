package couponmonster436;

class Lock { // used for mutual exclusion
    private boolean value;
    Lock(boolean initValue) {
        value = initValue;
    }
    synchronized boolean P() { // atomic operation // blocking
        if(!value){
            System.out.println("P called, value is false return false");
            return false;
        }
        System.out.println("P called, value is true");
        value = false;
        System.out.println("P called, value is now false return true");
        return true;
    }
    synchronized void V() { // atomic operation // non-blocking
        System.out.println("V is called value is now true");
        value = true;
    }
}