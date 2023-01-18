package psoft.hsphere.axis;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Iterator;
import org.apache.axis.AxisEngine;
import org.apache.axis.Message;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.migrator.PlanCreator;
import psoft.hsphere.migrator.PlanUtils;
import psoft.hsphere.migrator.ResellerPlanHolder;
import psoft.hsphere.migrator.ResellerUserCreator;
import psoft.hsphere.migrator.creator.UserCreatorConfig;
import psoft.hsphere.migrator.extractor.InfoExtractorUtils;

/* loaded from: hsphere.zip:psoft/hsphere/axis/MigrationServices.class */
public class MigrationServices {
    private static Category log = Category.getInstance(MigrationServices.class.getName());

    public String getDescription() {
        return "Migrator functions for users/resellers and plans";
    }

    private String getServerAttachmentAsString() throws Exception {
        Iterator attachments = AxisEngine.getCurrentMessageContext().getRequestMessage().getAttachments();
        if (attachments.hasNext()) {
            AttachmentPart attach = (AttachmentPart) attachments.next();
            return attach.getContent().toString();
        }
        return null;
    }

    private void setServerAttachmentAsString(String attachment) {
        Message currMsg = AxisEngine.getCurrentMessageContext().getResponseMessage();
        AttachmentPart attach = new AttachmentPart();
        attach.setContent(attachment, "text/plain");
        currMsg.addAttachmentPart(attach);
    }

    public String createPlans(AuthToken at, boolean force, boolean active, boolean createPrices, boolean r, boolean rc, String rrc) throws Exception {
        Account a = Utils.getAccount(at);
        String resellerName = a.getUser().getLogin();
        String content = getServerAttachmentAsString();
        if (content != null) {
            Document document = InfoExtractorUtils.getDocumentFromXMLString(content);
            PlanCreator creator = new PlanCreator(document, active, createPrices);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PrintStream oldStream = PlanUtils.setOutputStream(stream);
            int resume = 0;
            if (rc) {
                resume = 1;
            } else if (1 != 0) {
                resume = 2;
            }
            ResellerPlanHolder resellerPlanHolder = new ResellerPlanHolder(resellerName);
            resellerPlanHolder.setResumePlan(rrc);
            try {
                creator.createPlansFromXML(resume, resellerPlanHolder);
                PlanUtils.unsetOutputStream(oldStream);
            } catch (Exception e) {
                PlanUtils.unsetOutputStream(oldStream);
            } catch (Throwable th) {
                PlanUtils.unsetOutputStream(oldStream);
                throw th;
            }
            return new String(stream.toByteArray());
        }
        throw new Exception("Not found attachment with plans.");
    }

    public String createUsers(AuthToken at, boolean force, boolean detailLog) throws Exception {
        Utils.getAccount(at);
        String content = getServerAttachmentAsString();
        if (content != null) {
            BufferedWriter logWriter = null;
            ByteArrayOutputStream logStream = null;
            InputSource source = InfoExtractorUtils.getInputSource(content);
            if (detailLog) {
                logStream = new ByteArrayOutputStream();
                logWriter = new BufferedWriter(new OutputStreamWriter(logStream));
            }
            UserCreatorConfig config = new UserCreatorConfig(source, logWriter, force);
            CommonUserCreator migrator = new CommonUserCreator(config);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PrintStream oldStream = PlanUtils.setOutputStream(stream);
            try {
                migrator.execute();
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
            } catch (Exception e) {
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
            } catch (Throwable th) {
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
                throw th;
            }
            return new String(stream.toByteArray());
        }
        throw new Exception("Not found attachment with users.");
    }

    private String createResellers(AuthToken at, boolean force, boolean detailLog, boolean createOnlyUsers) throws Exception {
        Utils.getAccount(at);
        String content = getServerAttachmentAsString();
        if (content != null) {
            BufferedWriter logWriter = null;
            ByteArrayOutputStream logStream = null;
            InputSource source = InfoExtractorUtils.getInputSource(content);
            if (detailLog) {
                logStream = new ByteArrayOutputStream();
                logWriter = new BufferedWriter(new OutputStreamWriter(logStream));
            }
            UserCreatorConfig config = new UserCreatorConfig(source, logWriter, force, createOnlyUsers);
            ResellerUserCreator migrator = new ResellerUserCreator(config);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            PrintStream oldStream = PlanUtils.setOutputStream(stream);
            try {
                migrator.execute();
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
            } catch (Exception e) {
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
            } catch (Throwable th) {
                PlanUtils.unsetOutputStream(oldStream);
                if (detailLog) {
                    setServerAttachmentAsString(new String(logStream.toByteArray()));
                }
                throw th;
            }
            return new String(stream.toByteArray());
        }
        throw new Exception("Not found attachment with resellers.");
    }

    public String createOnlyResellers(AuthToken at, boolean force) throws Exception {
        return createResellers(at, force, false, false);
    }

    public String createResellersUsers(AuthToken at, boolean force) throws Exception {
        return createResellers(at, force, false, true);
    }
}
