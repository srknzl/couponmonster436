package couponmonster436;

public class User {
    public String name;
    public String username;
    public int score;
    public User(String name, String username){
        this.name = name;
        this.username = username;
        this.score = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            return ((User)obj).username.equals(this.username);
        }
        return super.equals(obj);
    }
    @Override
    public String toString(){
        return this.name+ this.username + this.score;
    }
}
