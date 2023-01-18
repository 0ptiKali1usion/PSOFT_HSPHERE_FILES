package psoft.hsphere.resource.plan_wizard;

import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.global.GlobalValueProvider;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/plan_wizard/ResellerPlanEditor.class */
public class ResellerPlanEditor {
    public static synchronized Plan savePlanChanges() throws Exception {
        HttpServletRequest request = Session.getRequest();
        PlanEditor pe = new PlanEditor(request);
        try {
            boolean changePrices = isEmpty(request.getParameter("leave_prices"));
            GlobalValueProvider.updatePlanValues(pe, request, changePrices);
            if (!isEmpty(request.getParameter("hard_credit"))) {
                pe.setValue("_HARD_CREDIT", request.getParameter("hard_credit"));
            } else {
                pe.deleteValue("_HARD_CREDIT");
            }
            if (request.getParameter("send_invoice") != null) {
                pe.setValue("_SEND_INVOICE", "1");
            } else {
                pe.deleteValue("_SEND_INVOICE");
            }
            pe.addResource("account", "psoft.hsphere.Account");
            pe.addMod("account", "");
            pe.addIResource("account", "", FMACLManager.RESELLER, "");
            pe.addIResource("account", "", "billviewer", "");
            pe.addIResource("account", "", "reportviewer", "");
            pe.addResource("custom_billing", "psoft.hsphere.resource.CustomizableBillingResource");
            pe.setValue("custom_billing", "_RECURRENT_CALC", "psoft.hsphere.calc.StandardCalc");
            pe.setValue("custom_billing", "_UNIT_PRICE_", "1");
            pe.setValue("custom_billing", "_REFUND_CALC", "psoft.hsphere.calc.StandardRefundCalc");
            pe.addResource("reportviewer", "psoft.hsphere.resource.admin.ReportManager");
            pe.addResource(FMACLManager.RESELLER, "psoft.hsphere.resource.admin.ResellerResource");
            pe.addMod(FMACLManager.RESELLER, "");
            pe.addIValue(FMACLManager.RESELLER, "", 0, request.getParameter("cp_alias"), "CP alias", 0);
            if (!isEmpty(request.getParameter("i_reseller_traffic"))) {
                pe.addResource("reseller_traffic", "psoft.hsphere.resource.Traffic");
                pe.addMod("reseller_traffic", "");
                if (!isEmpty(request.getParameter("f_reseller_traffic"))) {
                    pe.addIValue("reseller_traffic", "", 0, request.getParameter("f_reseller_traffic"), "", 0);
                } else {
                    pe.addIValue("reseller_traffic", "", 0, "0", "", 0);
                }
                pe.addIValue("reseller_traffic", "", 0, "-1", "", 1);
                pe.addIResource("account", "", "reseller_traffic", "");
                pe.setValue("_USE_RTRAFFIC_", "1");
            } else {
                pe.disableResource("reseller_traffic");
                pe.disableMod("reseller_traffic", "");
                pe.disableIResource("account", "", "reseller_traffic", "");
                pe.eraseResourcePrice("reseller_traffic");
                pe.deleteValue("_USE_RTRAFFIC_");
            }
            if (!isEmpty(request.getParameter("i_summary_quota"))) {
                pe.addResource("summary_quota", "psoft.hsphere.resource.ResellerQuota");
                pe.addMod("summary_quota", "");
                if (!isEmpty(request.getParameter("f_summary_quota"))) {
                    pe.addIValue("summary_quota", "", 0, request.getParameter("f_summary_quota"), "", 0);
                } else {
                    pe.addMissingIValue("summary_quota", "", 0, "0", "", 0);
                }
                pe.addIResource("account", "", "summary_quota", "");
                pe.setValue("_USE_SDU_", "1");
            } else {
                pe.disableIResource("account", "", "summary_quota", "");
                pe.disableResource("summary_quota");
                pe.deleteValue("_USE_SDU_");
            }
            if (!isEmpty(request.getParameter("i_reseller_backup"))) {
                pe.addResource("reseller_backup", "psoft.hsphere.resource.ResellerBackupResource");
                pe.addMod("reseller_backup", "");
                if (!isEmpty(request.getParameter("f_reseller_backup"))) {
                    pe.addIValue("reseller_backup", "", 0, request.getParameter("f_reseller_backup"), "", 0);
                } else {
                    pe.addMissingIValue("reseller_backup", "", 0, "0", "", 0);
                }
                pe.addIResource("account", "", "reseller_backup", "");
                pe.setValue("_USE_RBS_", "1");
            } else {
                pe.disableIResource("account", "", "reseller_backup", "");
                pe.disableResource("reseller_backup");
                pe.deleteValue("_USE_RBS_");
            }
            if (!isEmpty(request.getParameter("i_allocated_server"))) {
                pe.addResource("allocated_server", "psoft.hsphere.resource.admin.AllocatedServerResource");
                pe.addIResource("account", "", "allocated_server", "");
            } else {
                pe.disableIResource("account", "", "allocated_server", "");
                pe.disableResource("allocated_server");
            }
            if (!isEmpty(request.getParameter("i_r_ds_bandwidth"))) {
                pe.addResource("r_ds_bandwidth", "psoft.hsphere.resource.ds.ResellerBandwidth");
                pe.addMod("r_ds_bandwidth", "");
                if (!isEmpty(request.getParameter("f_r_ds_bandwidth"))) {
                    pe.addIValue("r_ds_bandwidth", "", 0, request.getParameter("f_r_ds_bandwidth"), "", 0);
                } else {
                    pe.addIValue("r_ds_bandwidth", "", 0, "0", "", 0);
                }
                pe.addIResource("account", "", "r_ds_bandwidth", "");
                pe.setValue("_USE_R_DS_BANDWIDTH_", "1");
                String newPlanValue = request.getParameter("r_ds_bandwidth_type");
                String oldPlanValue = pe.getValue("r_ds_bandwidth", "_R_DS_BANDWIDTH_TYPE_");
                if (!pe.areLiveAccounts() || oldPlanValue == null || "".equals(oldPlanValue)) {
                    pe.setValue("r_ds_bandwidth", "_R_DS_BANDWIDTH_TYPE_", request.getParameter("r_ds_bandwidth_type"));
                } else if (oldPlanValue != null && !oldPlanValue.equals(newPlanValue)) {
                    Session.getLog().debug("An attempt to set a new value for '_R_DS_BANDWIDTH_TYPE_' was blocked since there are live accounts under the plan.");
                }
            } else {
                pe.disableResource("r_ds_bandwidth");
                pe.disableMod("r_ds_bandwidth", "");
                pe.disableIResource("account", "", "r_ds_bandwidth", "");
                pe.eraseResourcePrice("r_ds_bandwidth");
                pe.deleteValue("_USE_R_DSBANDWIDTH_");
            }
            if (changePrices) {
                Session.getLog().debug("GOING TO CHANGE PRICES");
                for (String typeId : TypeRegistry.getPricedTypes()) {
                    String typeName = TypeRegistry.getType(typeId);
                    if (!isEmpty(request.getParameter("_FREE_UNITS_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_FREE_UNITS_" + typeName + "_", request.getParameter("_FREE_UNITS_" + typeName + "_"));
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_FREE_UNITS_" + typeName + "_");
                    }
                    if (!isEmpty(request.getParameter("_SETUP_PRICE_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_SETUP_PRICE_" + typeName + "_", request.getParameter("_SETUP_PRICE_" + typeName + "_"));
                        pe.setValue(FMACLManager.RESELLER, "_SETUP_CALC_" + typeName, "psoft.hsphere.calc.ResellerSetupCalc");
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_SETUP_PRICE_" + typeName + "_");
                        pe.deleteValue(FMACLManager.RESELLER, "_SETUP_CALC_" + typeName);
                    }
                    if (!isEmpty(request.getParameter("_UNIT_PRICE_" + typeName + "_")) || !isEmpty(request.getParameter("_REFUND_PRICE_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_REFUND_CALC_" + typeName, "psoft.hsphere.calc.ResellerRefundCalc");
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_REFUND_CALC_" + typeName);
                    }
                    if (!isEmpty(request.getParameter("_UNIT_PRICE_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_UNIT_PRICE_" + typeName + "_", request.getParameter("_UNIT_PRICE_" + typeName + "_"));
                        pe.setValue(FMACLManager.RESELLER, "_RECURRENT_CALC_" + typeName, "psoft.hsphere.calc.ResellerCalc");
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_UNIT_PRICE_" + typeName + "_");
                        pe.deleteValue(FMACLManager.RESELLER, "_RECURRENT_CALC_" + typeName);
                    }
                    if (!isEmpty(request.getParameter("_USAGE_PRICE_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_USAGE_PRICE_" + typeName + "_", request.getParameter("_USAGE_PRICE_" + typeName + "_"));
                        pe.setValue(FMACLManager.RESELLER, "_USAGE_CALC_" + typeName, "psoft.hsphere.calc.ResellerUsageCalc");
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_USAGE_PRICE_" + typeName + "_");
                        pe.deleteValue(FMACLManager.RESELLER, "_USAGE_CALC_" + typeName);
                    }
                    if (!isEmpty(request.getParameter("_REFUND_PRICE_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_REFUND_PRICE_" + typeName + "_", request.getParameter("_REFUND_PRICE_" + typeName + "_"));
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_REFUND_PRICE_" + typeName + "_");
                    }
                    if (!isEmpty(request.getParameter("_MAX_" + typeName + "_"))) {
                        pe.setValue(FMACLManager.RESELLER, "_MAX_" + typeName + "_", request.getParameter("_MAX_" + typeName + "_"));
                    } else {
                        pe.deleteValue(FMACLManager.RESELLER, "_MAX_" + typeName + "_");
                    }
                }
                pe.setValue(FMACLManager.RESELLER, "_SETUP_CALC_opensrs", "psoft.hsphere.calc.ResellerRegistrationCalc");
                pe.setValue(FMACLManager.RESELLER, "_SETUP_CALC_domain_transfer", "psoft.hsphere.calc.ResellerDomainTransferSetupCalc");
                if (changePrices && isEmpty(request.getParameter("leave_domain_transfer_prices"))) {
                    pe.deleteRValuesStartsWith(FMACLManager.RESELLER, "_SETUP_PRICE_TRANSFER_");
                    Enumeration param_names = request.getParameterNames();
                    while (param_names.hasMoreElements()) {
                        String param_name = (String) param_names.nextElement();
                        if (param_name.startsWith("_SETUP_PRICE_TRANSFER_") && !isEmpty(request.getParameter(param_name))) {
                            Session.getLog().debug("Setting registrar setup " + param_name + " = " + request.getParameter(param_name));
                            pe.setValue(FMACLManager.RESELLER, param_name, request.getParameter(param_name));
                        }
                    }
                }
                if (changePrices && isEmpty(request.getParameter("leave_osrs_prices"))) {
                    pe.deleteRValuesStartsWith(FMACLManager.RESELLER, "_SETUP_PRICE_TLD_");
                    Enumeration param_names2 = request.getParameterNames();
                    while (param_names2.hasMoreElements()) {
                        String param_name2 = (String) param_names2.nextElement();
                        if (param_name2.startsWith("_SETUP_PRICE_TLD_") && !isEmpty(request.getParameter(param_name2))) {
                            Session.getLog().debug("Setting registrar setup " + param_name2 + " = " + request.getParameter(param_name2));
                            pe.setValue(FMACLManager.RESELLER, param_name2, request.getParameter(param_name2));
                        }
                    }
                }
            }
            if (isEmpty(request.getParameter("leave_prices")) && isEmpty(request.getParameter("leave_ds_prices"))) {
                if ("1".equals(request.getParameter("allow_own_ds"))) {
                    pe.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_OWN_DS", "0");
                } else {
                    pe.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_OWN_DS", "1");
                }
                pe.deleteRValuesStartsWith(FMACLManager.RESELLER, "_DST");
                pe.deleteRValuesStartsWith(FMACLManager.RESELLER, "_UNIT_PRICE__DST_");
                pe.deleteRValuesStartsWith(FMACLManager.RESELLER, "_SETUP_PRICE__DST_");
                if ("1".equals(request.getParameter("allow_ds_resell"))) {
                    pe.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_DS_SELL", "0");
                    Enumeration param_names3 = request.getParameterNames();
                    while (param_names3.hasMoreElements()) {
                        String param_name3 = (String) param_names3.nextElement();
                        if (param_name3.startsWith("_DST_")) {
                            String dstId = param_name3.substring(5, param_name3.length());
                            Session.getLog().debug("DST_ID=" + dstId);
                            String setupName = "_SETUP_PRICE__DST_" + dstId + "_";
                            String recurrentName = "_UNIT_PRICE__DST_" + dstId + "_";
                            Session.getLog().debug("SETUP_NAME=" + setupName);
                            Session.getLog().debug("RECURRENT_NAME=" + recurrentName);
                            if (!isEmpty(request.getParameter(setupName))) {
                                pe.setValue(FMACLManager.RESELLER, setupName, request.getParameter(setupName));
                            }
                            if (!isEmpty(request.getParameter(recurrentName))) {
                                pe.setValue(FMACLManager.RESELLER, recurrentName, request.getParameter(recurrentName));
                            }
                        }
                    }
                } else {
                    pe.setValue(FMACLManager.RESELLER, "_GLB_DISABLED_ALLOW_DS_SELL", "1");
                }
                pe.setValue(FMACLManager.RESELLER, "_SETUP_CALC_ds", "psoft.hsphere.calc.ResellerDedicatedServerSetupCalc");
                pe.setValue(FMACLManager.RESELLER, "_RECURRENT_CALC_ds", "psoft.hsphere.calc.ResellerDedicatedServerCalc");
                pe.setValue(FMACLManager.RESELLER, "_REFUND_CALC_ds", "psoft.hsphere.calc.ResellerRefundCalc");
            }
            Plan resellerPlan = pe.done();
            psoft.hsphere.Reseller.updateGlobalValues(resellerPlan.getId());
            return resellerPlan;
        } catch (Exception ex) {
            Session.getLog().error("Error during plan edition ", ex);
            throw ex;
        }
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
