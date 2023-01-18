package psoft.hsphere.billing.estimators;

import java.util.StringTokenizer;
import org.apache.log4j.Category;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.cache.storedobjects.AbstractStorableCache;

/* loaded from: hsphere.zip:psoft/hsphere/billing/estimators/ComplexEstimatorManager.class */
public class ComplexEstimatorManager extends AbstractStorableCache {
    private static transient Category log = null;

    public static synchronized ComplexEstimator createComplexEstimator() {
        ComplexEstimator ce = new ComplexEstimator(generateEstimatorId(), Session.getAccountId());
        Session.getCacheFactory().getCache(ComplexEstimator.class).put(ce);
        Session.getCacheFactory().getLockableCache(ComplexEstimator.class).lock(ce.getId());
        return ce;
    }

    public static long generateEstimatorId() {
        return System.currentTimeMillis();
    }

    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return ComplexEstimator.class;
    }

    public ComplexEstimator getComplexEstimator(String estimatorId) throws Exception {
        Session.getLog().debug("Inside ComplexEstimatorManager::getComplexEstimator estimatorId = " + estimatorId);
        synchronized (this) {
            long ceId = -1;
            long aId = -1;
            try {
                StringTokenizer stkn = new StringTokenizer(estimatorId, "-");
                if (stkn.hasMoreTokens()) {
                    aId = Long.parseLong(stkn.nextToken());
                    if (aId != Session.getAccountId()) {
                        throw new HSUserException("account.unauthorized_to_get_estimator", new Object[]{estimatorId});
                    }
                }
                if (stkn.hasMoreTokens()) {
                    ceId = Long.parseLong(stkn.nextToken());
                }
            } catch (NumberFormatException nex) {
                getLog().error("Unable to parse Complex Estimator ID", nex);
            }
            if (ceId > 0) {
                Session.getLog().debug("Got ComplexEstimator id = " + ceId);
                ComplexEstimator ce = (ComplexEstimator) Session.getCacheFactory().getCache(ComplexEstimator.class).get(ceId);
                if (ce != null && ce.getAccount() != aId) {
                    throw new HSUserException("account.unauthorized_to_get_estimator", new Object[]{estimatorId});
                }
                return ce;
            }
            return null;
        }
    }

    protected synchronized Category getLog() {
        if (log == null) {
            log = Category.getInstance(EstimateCreateMass.class.getName());
        }
        return log;
    }
}
