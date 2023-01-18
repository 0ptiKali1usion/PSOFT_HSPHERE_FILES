package psoft.hsp;

import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsp/PackageAlreadyExistsException.class */
public class PackageAlreadyExistsException extends Exception {
    private static Category log = Category.getInstance(PackageAlreadyExistsException.class.getName());
    protected String packageName;

    public PackageAlreadyExistsException(String packageName) {
        super("Package: " + packageName + " already exists");
        this.packageName = packageName;
    }
}
