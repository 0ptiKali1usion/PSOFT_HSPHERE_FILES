package psoft.util;

/* loaded from: hsphere.zip:psoft/util/UniqueNameGenerator.class */
public class UniqueNameGenerator {
    String username;
    int maxSize;
    int maxIterations;
    int count = -1;

    public UniqueNameGenerator(String username, int maxSize, int maxIterations) {
        this.username = username;
        this.maxSize = maxSize;
        this.maxIterations = maxIterations - 1;
    }

    public String next() {
        String tmp;
        if (this.count == this.maxIterations) {
            return null;
        }
        if (this.count == -1) {
            this.count++;
            if (this.username.length() < this.maxSize) {
                return this.username;
            }
            return this.username.substring(0, this.maxSize);
        }
        int size = this.maxSize - getOffset();
        if (this.username.length() < size) {
            tmp = this.username + this.count;
        } else {
            tmp = this.username.substring(0, size) + this.count;
        }
        this.count++;
        return tmp;
    }

    private int getOffset() {
        if (this.count < 10) {
            return 1;
        }
        if (this.count < 100) {
            return 2;
        }
        if (this.count < 1000) {
            return 3;
        }
        if (this.count < 10000) {
            return 4;
        }
        if (this.count < 100000) {
            return 5;
        }
        return this.count < 1000000 ? 6 : Integer.MAX_VALUE;
    }

    public static void main(String[] args) {
        UniqueNameGenerator un = new UniqueNameGenerator("AbraCodabra", 11, 15);
        while (true) {
            String name = un.next();
            if (name == null) {
                break;
            }
            System.err.println(name);
        }
        UniqueNameGenerator un2 = new UniqueNameGenerator("AbraCodabra", 12, 26);
        while (true) {
            String name2 = un2.next();
            if (name2 == null) {
                break;
            }
            System.err.println(name2);
        }
        UniqueNameGenerator un3 = new UniqueNameGenerator("Abra", 12, 10);
        while (true) {
            String name3 = un3.next();
            if (name3 != null) {
                System.err.println(name3);
            } else {
                return;
            }
        }
    }
}
