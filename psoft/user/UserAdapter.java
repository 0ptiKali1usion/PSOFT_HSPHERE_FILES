package psoft.user;

import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import psoft.persistance.PersistanceError;
import psoft.persistance.UniversalPersistanceManager;
import psoft.util.Config;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.HashAccessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/user/UserAdapter.class */
public class UserAdapter {
    protected UniversalPersistanceManager upm = Config.getUPM("psoft.user");

    public void checkPresence(Vector groups, Accessor data, NameModifier nm) throws ComplexValidatorException {
        Enumeration en = groups.elements();
        while (en.hasMoreElements()) {
            ExtendedValidator exVal = groupValidator((String) en.nextElement());
            exVal.checkPresence(data, nm);
        }
    }

    public User signUp(Accessor data, NameModifier nm) throws ComplexValidatorException {
        return new GenericUser(data, nm);
    }

    public void update(User u, Accessor data, NameModifier nm) throws ComplexValidatorException {
        ((GenericUser) u).update(data, nm);
    }

    public User login(Accessor data, NameModifier nm) throws UserLoginException {
        try {
            User u = (User) this.upm.get(data, nm, GenericUser.class);
            if (u.login(data.get(nm.getName("login")), data.get(nm.getName("password")))) {
                return u;
            }
            throw new UserLoginException("wrong password");
        } catch (PersistanceError pe) {
            throw new UserLoginException(pe.getMessage());
        }
    }

    public Accessor validate(Accessor data, NameModifier nm, Vector groups) throws ComplexValidatorException {
        Hashtable mutated = new Hashtable();
        Enumeration en = groups.elements();
        while (en.hasMoreElements()) {
            ExtendedValidator exVal = groupValidator((String) en.nextElement());
            mutated.putAll(exVal.validate(data, nm));
        }
        return new HashAccessor(mutated);
    }

    public ExtendedValidator groupValidator(String group) {
        try {
            ExtendedValidator exVal = (ExtendedValidator) Class.forName(group).getDeclaredConstructor(null).newInstance(null);
            return exVal;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e2) {
            e2.printStackTrace();
            return null;
        } catch (InstantiationException e3) {
            e3.printStackTrace();
            return null;
        } catch (NoSuchMethodException e4) {
            e4.printStackTrace();
            return null;
        } catch (InvocationTargetException e5) {
            e5.printStackTrace();
            return null;
        }
    }
}
