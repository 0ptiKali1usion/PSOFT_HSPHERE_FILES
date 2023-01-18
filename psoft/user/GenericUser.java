package psoft.user;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import java.util.Date;
import psoft.persistance.UniversalPersistanceManager;
import psoft.util.Config;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/user/GenericUser.class */
public class GenericUser implements User {

    /* renamed from: id */
    private int f249id;
    protected String signupURL;
    protected Date timeStamp;

    /* renamed from: gi */
    GenericInfo f250gi;

    /* renamed from: pm */
    protected UniversalPersistanceManager f251pm;

    @Override // psoft.user.User
    public void setTimeStamp(Date ts) {
        this.timeStamp = ts;
    }

    @Override // psoft.user.User
    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public GenericUser(Accessor data, NameModifier nm) throws ComplexValidatorException {
        this(-1, null, null, null);
        updateFields(data, nm);
        setPersistanceManager(Config.getUPM("psoft.user"));
        setId(this.f251pm.getNewId(getClass()));
        this.f251pm.insert(this);
    }

    public GenericUser(int id, String login, String password, String email) {
        this.signupURL = null;
        this.f251pm = null;
        this.f249id = id;
        this.f250gi = new GenericInfo(login, password, email);
    }

    public void update(Accessor data, NameModifier nm) throws ComplexValidatorException {
        updateFields(data, nm);
        this.f251pm.update(this);
    }

    protected void updateFields(Accessor data, NameModifier nm) throws ComplexValidatorException {
        this.f250gi.update(data, nm);
    }

    public void setPersistanceManager(UniversalPersistanceManager pm) {
        this.f251pm = pm;
    }

    @Override // psoft.user.User
    public int getId() {
        return this.f249id;
    }

    public void setId(int newId) {
        this.f249id = newId;
    }

    @Override // psoft.user.User
    public String getLogin() {
        return (String) this.f250gi.getAttrib("login");
    }

    @Override // psoft.user.User
    public String getPassword() {
        return (String) this.f250gi.getAttrib("password");
    }

    public String getSignupURL() {
        return this.signupURL;
    }

    public String setSignupURL(String URL) {
        this.signupURL = URL;
        return URL;
    }

    @Override // psoft.user.User
    public String getEmail() {
        return (String) this.f250gi.getAttrib("email");
    }

    @Override // psoft.user.User
    public boolean login(String login, String password) {
        return getLogin().equals(login) && getPassword().equals(password);
    }

    public TemplateModel get(String key) {
        String temp = null;
        if (key.equals("login")) {
            temp = getLogin();
        } else if (key.equals("id")) {
            temp = "" + this.f249id;
        } else if (key.equals("password")) {
            temp = getPassword();
        } else if (key.equals("email")) {
            temp = getEmail();
        } else if (key.equals("signupURL")) {
            temp = getSignupURL();
        }
        if (temp != null) {
            return new SimpleScalar(temp);
        }
        return null;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.user.User
    public Accessor getAccessor() {
        return new AttributeAccessor(this.f250gi);
    }

    protected GenericInfo getInfo() {
        return this.f250gi;
    }

    protected void setEmail(String value) {
        this.f250gi.email = value;
    }
}
