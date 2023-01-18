package psoft.user;

import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/user/Updatable.class */
public interface Updatable extends Attributable {
    void update(Accessor accessor, NameModifier nameModifier) throws ComplexValidatorException;
}
