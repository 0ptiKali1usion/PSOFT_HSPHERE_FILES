package psoft.hsphere.tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;

/* loaded from: hsphere.zip:psoft/hsphere/tools/SQLTypesResourceBundle.class */
public class SQLTypesResourceBundle {
    PropertyResourceBundle bundle;

    public SQLTypesResourceBundle(String fileName) throws Exception {
        try {
            File inputFile = new File(fileName);
            FileInputStream fileStream = new FileInputStream(inputFile);
            this.bundle = new PropertyResourceBundle(fileStream);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public String getPropertyValue(String propName) {
        return this.bundle.getString(propName);
    }

    public Enumeration getAllNames() {
        return this.bundle.getKeys();
    }

    public String getString(String propName) {
        return this.bundle.getString(propName);
    }
}
