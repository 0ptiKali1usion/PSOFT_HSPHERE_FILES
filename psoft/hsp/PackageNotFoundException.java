package psoft.hsp;

/* loaded from: hsphere.zip:psoft/hsp/PackageNotFoundException.class */
public class PackageNotFoundException extends Exception {
    String packageName;

    public PackageNotFoundException(String packageName) {
        super("Package not found: " + packageName);
        this.packageName = packageName;
    }

    public String getPackageName() {
        return this.packageName;
    }
}
