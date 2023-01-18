package psoft.hsp;

import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsp/PackageConflictException.class */
public class PackageConflictException extends Exception {
    private static Category log = Category.getInstance(PackageConflictException.class.getName());
    String filename;
    int type;

    public PackageConflictException(int type, String filename) {
        super("File in use by another package: " + filename);
        this.filename = filename;
        this.type = type;
    }

    public PackageConflictException(String message) {
        super(message);
    }
}
