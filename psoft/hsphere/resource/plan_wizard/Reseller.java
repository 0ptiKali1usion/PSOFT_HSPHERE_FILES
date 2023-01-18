package psoft.hsphere.resource.plan_wizard;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.global.GlobalValueProvider;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/plan_wizard/Reseller.class */
public class Reseller {
    public static synchronized Plan createPlan() throws Exception {
        HttpServletRequest request = Session.getRequest();
        PlanWizard pw = null;
        try {
            pw = new PlanWizard(request);
            pw.setName(request.getParameter("plan_name"), request.getParameter("trial"));
            Session.getLog().debug("Plan name is set");
            String cpAlias = request.getParameter("cp_alias");
            pw.setValue("_PERIOD_TYPES", "1");
            pw.setValue("_PERIOD_TYPE_0", "MONTH");
            pw.setValue("_PERIOD_SIZE_0", "1");
            pw.setValue("menuId", FMACLManager.RESELLER);
            pw.setValue("_template", "misc/add_admin_account.html");
            pw.setValue("_TEMPLATES_DIR", "reseller/");
            pw.setValue("_HARD_CREDIT", request.getParameter("hard_credit"));
            pw.setValue("_CREATED_BY_", "6");
            GlobalValueProvider.updatePlanValues(pw, request, true);
            if (request.getParameter("send_invoice") != null) {
                pw.setValue("_SEND_INVOICE", "1");
            }
            pw.startResources();
            pw.addResource("tt", "psoft.hsphere.resource.tt.TroubleTicket");
            pw.addResource("billviewer", "psoft.hsphere.resource.BillViewer");
            pw.addResource("reportviewer", "psoft.hsphere.resource.admin.ReportManager");
            pw.addResource(FMACLManager.RESELLER, "psoft.hsphere.resource.admin.ResellerResource");
            pw.addMod(FMACLManager.RESELLER, "");
            pw.addIValue(FMACLManager.RESELLER, "", 0, cpAlias, "CP alias", 0);
            pw.addResource("custom_billing", "psoft.hsphere.resource.CustomizableBillingResource");
            pw.setValue("custom_billing", "_RECURRENT_CALC", "psoft.hsphere.calc.StandardCalc");
            pw.setValue("custom_billing", "_UNIT_PRICE_", "1");
            pw.setValue("custom_billing", "_REFUND_CALC", "psoft.hsphere.calc.StandardRefundCalc");
            if (!isEmpty(request.getParameter("i_reseller_traffic"))) {
                pw.addResource("reseller_traffic", "psoft.hsphere.resource.Traffic");
                pw.addMod("reseller_traffic", "");
                if (isEmpty(request.getParameter("f_reseller_traffic"))) {
                    pw.addIValue("reseller_traffic", "", 0, "0", "", 0);
                } else {
                    pw.addIValue("reseller_traffic", "", 0, request.getParameter("f_reseller_traffic"), "", 0);
                }
                pw.addIValue("reseller_traffic", "", 0, "-1", "", 1);
                pw.setValue("_USE_RTRAFFIC_", "1");
            }
            for (String typeId : TypeRegistry.getPricedTypes()) {
                String typeName = TypeRegistry.getType(typeId);
                if (!isEmpty(request.getParameter("_FREE_UNITS_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_FREE_UNITS_" + typeName + "_", request.getParameter("_FREE_UNITS_" + typeName + "_"));
                }
                if (!isEmpty(request.getParameter("_SETUP_PRICE_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_SETUP_PRICE_" + typeName + "_", request.getParameter("_SETUP_PRICE_" + typeName + "_"));
                    pw.setValue(FMACLManager.RESELLER, "_SETUP_CALC_" + typeName, "psoft.hsphere.calc.ResellerSetupCalc");
                }
                if (!isEmpty(request.getParameter("_UNIT_PRICE_" + typeName + "_")) || !isEmpty(request.getParameter("_REFUND_PRICE_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_REFUND_CALC_" + typeName, "psoft.hsphere.calc.ResellerRefundCalc");
                }
                if (!isEmpty(request.getParameter("_UNIT_PRICE_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_UNIT_PRICE_" + typeName + "_", request.getParameter("_UNIT_PRICE_" + typeName + "_"));
                    pw.setValue(FMACLManager.RESELLER, "_RECURRENT_CALC_" + typeName, "psoft.hsphere.calc.ResellerCalc");
                }
                if (!isEmpty(request.getParameter("_USAGE_PRICE_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_USAGE_PRICE_" + typeName + "_", request.getParameter("_USAGE_PRICE_" + typeName + "_"));
                    pw.setValue(FMACLManager.RESELLER, "_USAGE_CALC_" + typeName, "psoft.hsphere.calc.ResellerUsageCalc");
                }
                if (!isEmpty(request.getParameter("_REFUND_PRICE_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_REFUND_PRICE_" + typeName + "_", request.getParameter("_REFUND_PRICE_" + typeName + "_"));
                }
                if (!isEmpty(request.getParameter("_MAX_" + typeName + "_"))) {
                    pw.setValue(FMACLManager.RESELLER, "_MAX_" + typeName + "_", request.getParameter("_MAX_" + typeName + "_"));
                }
            }
            if (!isEmpty(request.getParameter("i_summary_quota"))) {
                pw.addResource("summary_quota", "psoft.hsphere.resource.ResellerQuota");
                pw.addMod("summary_quota", "");
                if (!isEmpty(request.getParameter("f_summary_quota"))) {
                    pw.addIValue("summary_quota", "", 0, request.getParameter("f_summary_quota"), "", 0);
                } else {
                    pw.addIValue("summary_quota", "", 0, "0", "", 0);
                }
                pw.setValue("_USE_SDU_", "1");
            }
            if (!isEmpty(request.getParameter("i_reseller_backup"))) {
                pw.addResource("reseller_backup", "psoft.hsphere.resource.ResellerBackupResource");
                pw.addMod("reseller_backup", "");
                if (!isEmpty(request.getParameter("f_reseller_backup"))) {
                    pw.addIValue("reseller_backup", "", 0, request.getParameter("f_reseller_backup"), "", 0);
                } else {
                    pw.addIValue("reseller_backup", "", 0, "0", "", 0);
                }
                pw.setValue("_USE_RBS_", "1");
            }
            if (!isEmpty(request.getParameter("i_allocated_server"))) {
                pw.addResource("allocated_server", "psoft.hsphere.resource.admin.AllocatedServerResource");
            }
            if (!isEmpty(request.getParameter("i_r_ds_bandwidth"))) {
                pw.addResource("r_ds_bandwidth", "psoft.hsphere.resource.ds.ResellerBandwidth");
                pw.addMod("r_ds_bandwidth", "");
                if (isEmpty(request.getParameter("f_r_ds_bandwidth"))) {
                    pw.addIValue("r_ds_bandwidth", "", 0, "0", "", 0);
                } else {
                    pw.addIValue("r_ds_bandwidth", "", 0, request.getParameter("f_r_ds_bandwidth"), "", 0);
                }
                pw.setValue("_USE_R_DS_BANDWIDTH_", "1");
                pw.setValue("r_ds_bandwidth", "_R_DS_BANDWIDTH_TYPE_", request.getParameter("r_ds_bandwidth_type"));
            }
            pw.setValue(FMACLManager.RESELLER, "_SETUP_CALC_opensrs", "psoft.hsphere.calc.ResellerRegistrationCalc");
            pw.setValue(FMACLManager.RESELLER, "_SETUP_CALC_domain_transfer", "psoft.hsphere.calc.ResellerDomainTransferSetupCalc");
            if (!isEmpty(request.getParameter("use_dreg_prices"))) {
                pw.setValue(FMACLManager.RESELLER, "_USE_DEF_SRS_PRICE_", "1");
            } else {
                Enumeration param_names = request.getParameterNames();
                while (param_names.hasMoreElements()) {
                    String param_name = (String) param_names.nextElement();
                    if (param_name.startsWith("_SETUP_PRICE_TRANSFER_")) {
                        Session.getLog().debug("Settings domain transfer setup " + param_name + "=" + request.getParameter(param_name));
                        pw.setValue(FMACLManager.RESELLER, param_name, request.getParameter(param_name));
                    } else if (param_name.startsWith("_SETUP_PRICE_TLD_") && !isEmpty(request.getParameter(param_name))) {
                        Session.getLog().debug("Setting registrar setup " + param_name + " = " + request.getParameter(param_name));
                        pw.setValue(FMACLManager.RESELLER, param_name, request.getParameter(param_name));
                    } else if (param_name.indexOf("DST") >= 0 && param_name.indexOf("PRICE") >= 0 && !isEmpty(request.getParameter(param_name))) {
                        Session.getLog().debug("Setting dedicated server access and prices " + param_name + " = " + request.getParameter(param_name));
                        pw.setValue(FMACLManager.RESELLER, param_name, request.getParameter(param_name));
                    } else if (param_name.equals("allow_own_ds")) {
                        if ("1".equals(request.getParameter("allow_own_ds"))) {
                            pw.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_OWN_DS", "0");
                        } else {
                            pw.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_OWN_DS", "1");
                        }
                    } else if (param_name.equals("allow_ds_resell")) {
                        if ("1".equals(request.getParameter("allow_ds_resell"))) {
                            pw.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_DS_SELL", "0");
                        } else {
                            pw.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_DS_SELL", "1");
                        }
                    }
                }
            }
            pw.addResource("account", "psoft.hsphere.Account");
            pw.addMod("account", "");
            int count = 0 + 1;
            pw.addIResource("account", "", FMACLManager.RESELLER, "", 0);
            int count2 = count + 1;
            pw.addIResource("account", "", "billviewer", "", count);
            int count3 = count2 + 1;
            pw.addIResource("account", "", "reportviewer", "", count2);
            if (!isEmpty(request.getParameter("i_reseller_traffic"))) {
                count3++;
                pw.addIResource("account", "", "reseller_traffic", "", count3);
            }
            if (!isEmpty(request.getParameter("i_summary_quota"))) {
                int i = count3;
                count3++;
                pw.addIResource("account", "", "summary_quota", "", i);
            }
            if (!isEmpty(request.getParameter("i_reseller_backup"))) {
                int i2 = count3;
                count3++;
                pw.addIResource("account", "", "reseller_backup", "", i2);
            }
            if (!isEmpty(request.getParameter("i_allocated_server"))) {
                int i3 = count3;
                count3++;
                pw.addIResource("account", "", "allocated_server", "", i3);
            }
            if (!isEmpty(request.getParameter("i_r_ds_bandwidth"))) {
                int i4 = count3;
                int i5 = count3 + 1;
                pw.addIResource("account", "", "r_ds_bandwidth", "", i4);
            }
            pw.setValue(FMACLManager.RESELLER, "_SETUP_CALC_ds", "psoft.hsphere.calc.ResellerDedicatedServerSetupCalc");
            pw.setValue(FMACLManager.RESELLER, "_RECURRENT_CALC_ds", "psoft.hsphere.calc.ResellerDedicatedServerCalc");
            pw.setValue(FMACLManager.RESELLER, "_REFUND_CALC_ds", "psoft.hsphere.calc.ResellerRefundCalc");
            pw.doneResoures();
            pw.doneIResources();
            Plan resellerPlan = pw.done();
            resellerPlan.FM_accessChange(Integer.toString(Session.getAccount().getPlan().getId()) + ",");
            return resellerPlan;
        } catch (Exception ex) {
            if (pw != null) {
                pw.abort();
            }
            throw ex;
        }
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
