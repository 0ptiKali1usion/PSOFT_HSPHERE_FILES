package psoft.hsphere.resource.epayment;

import java.util.HashMap;
import java.util.Iterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.Session;
import psoft.hsphere.util.XMLManager;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/GenPaymentInstrument.class */
public class GenPaymentInstrument {
    protected static Class[] argInitT = {BillingInfo.class, Iterator.class};
    protected static Class[] argInitT1 = {BillingInfo.class, Accessor.class, NameModifier.class};
    protected static Class[] argLoadT = {BillingInfo.class};
    protected static HashMap iMap = new HashMap();

    static {
        iMap.put("Check", GenCheck.class);
        iMap.put("CC", GenericCreditCard.class);
        try {
            Document doc = XMLManager.getXML("MERCHANT_GATEWAYS_CONF");
            Element processors = (Element) doc.getElementsByTagName("processors").item(0);
            NodeList proclist = processors.getElementsByTagName("processor");
            for (int i = 0; i < proclist.getLength(); i++) {
                Element processor = (Element) proclist.item(i);
                Session.getLog().debug("Initializing a web processor: " + processor.getAttribute("name"));
                iMap.put(processor.getAttribute("name"), GenWebProcessor.class);
            }
        } catch (Exception ex) {
            Session.getLog().error("Problem getting payment processors info from xml: ", ex);
        }
    }

    public static PaymentInstrument getPaymentInstrument(String type, BillingInfo bi, Accessor a, NameModifier nm) throws Exception {
        Class c = (Class) iMap.get(type);
        if (c == null) {
            throw new Exception("Type not found: " + type);
        }
        Object[] argV = {bi, a, nm};
        return (PaymentInstrument) c.getConstructor(argInitT1).newInstance(argV);
    }

    public static PaymentInstrument getPaymentInstrument(String type, BillingInfo bi, Iterator i) throws Exception {
        Class c = (Class) iMap.get(type);
        if (c == null) {
            throw new Exception("Type not found: " + type);
        }
        Object[] argV = {bi, i};
        return (PaymentInstrument) c.getConstructor(argInitT).newInstance(argV);
    }

    public static PaymentInstrument getPaymentInstrument(String type, BillingInfo bi) throws Exception {
        Class c = (Class) iMap.get(type);
        if (c == null) {
            throw new Exception("Type not found: " + type);
        }
        Object[] argV = {bi};
        return (PaymentInstrument) c.getConstructor(argLoadT).newInstance(argV);
    }
}
