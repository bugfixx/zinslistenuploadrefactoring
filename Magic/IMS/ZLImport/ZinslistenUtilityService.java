package Magic.IMS.ZLImport;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.DataAgent;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Agents.QueryResult;
import net.metamagix.essence.Agents.TemplateReader;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.MConfig.CfgSingleton;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.eSSENCETypes.eDate;
import net.metamagix.essence.tools.CoolStringTool;
import net.metamagix.essence.tools.CoolWebTool;

import Magic.IMS.reporting.helpers.ArgsHelper;

/**
 * Utility service for Zinslisten operations.
 * Provides common utility methods extracted from UploadXLS5.
 * 
 * @author Refactored from UploadXLS5
 */
public class ZinslistenUtilityService {
    
    private DynGenDataObj session;
    private DynGenDataObj global;
    private FileDataAgent fileDataAgent;
    private BugMe debug;
    private DataAgent dataAgent;
    private DynGenDataObj context;
    private ZinslistenDatabaseCRUDService crudService;
    
    private Map<String, String> topStatusValues;
    private String currentHausOid;
    private String hausverwaltung;
    private String mylang;
    private DecimalFormatSymbols symbolsDE_DE;
    private Map<String, String> assetmanagerAndIDs;
    
    /**
     * Constructor for ZinslistenUtilityService.
     * 
     * @param session Session object
     * @param global Global configuration object
     * @param fileDataAgent File data agent
     * @param debug Debug logger
     * @param dataAgent Data agent for database operations
     * @param context Context DynGenDataObj for accessing properties
     */
    public ZinslistenUtilityService(DynGenDataObj session, DynGenDataObj global, 
            FileDataAgent fileDataAgent, BugMe debug, DataAgent dataAgent, DynGenDataObj context) {
        this.session = session;
        this.global = global;
        this.fileDataAgent = fileDataAgent;
        this.debug = debug;
        this.dataAgent = dataAgent;
        this.context = context;
        this.topStatusValues = new HashMap<>();
        this.currentHausOid = "";
        this.hausverwaltung = "";
        this.mylang = "";
        this.symbolsDE_DE = DecimalFormatSymbols.getInstance(Locale.GERMANY);
    }
    
    /**
     * Get user value by delegating to CRUD service.
     * 
     * @param name Name of the user value
     * @return The user value
     */
    public String getUserValue(String name) {
        return getCrudService().getUserValue(name);
    }
    
    /**
     * Get the current language setting.
     * Returns empty string for German ("DE"), otherwise returns the language code.
     * 
     * @return Language code or empty string for German
     */
    public String getLanguage() {
        if (mylang == null || mylang.equals("")) {
            if (session != null) {
                mylang = session.getString("language").trim();
            } else {
                mylang = "";
            }
        }
        if (mylang.equalsIgnoreCase("DE")) {
            mylang = "";
        }
        return mylang;
    }
    
    /**
     * Get variable argument value from various sources.
     * Checks in order: var.argName, arg.argName, session args by ID.
     * 
     * @param argName Name of the argument
     * @return The argument value or null/empty if not found
     */
    private String getVarArg(String argName) {
        String value = context.getString("var." + argName);
        if (value == null || value.equals("")) {
            value = context.getString("arg." + argName);
        }
        if ((value == null || value.equals("")) && session != null) {
            value = session.getString("arg.oid" + context.getString("id").trim() + "." + argName);
        }
        if ((value == null || value.equals("")) && session != null) {
            String volatileId = context.getString("volatile_id");
            if (volatileId != null && !volatileId.equals("")) {
                value = session.getString("arg.oid" + volatileId + "." + argName);
            }
        }
        return value;
    }
    
    /**
     * Load top status values from template configuration.
     * Populates topStatusValues map with status codes and their text representations.
     */
    private void loadTopStatusValues() {
        if (topStatusValues == null || topStatusValues.size() == 0) {
            TemplateReader tr = TemplateReader.getInstance();
            DynGenDataObj dgdTopStatusSelector = tr.getFlavouredDGDForTemplate(
                    "CIMS.TopStatusSelector", global, session);
            
            String language = session.getString("language").toUpperCase();
            if (language.equals("DE")) {
                language = "";
            }
            
            String alternatives = (String) dgdTopStatusSelector.get("var.alternatives");
            String textalternatives = (String) dgdTopStatusSelector.get("var.textalternatives" + language);
            
            String[] alts = CoolStringTool.splitOnce(alternatives);
            String[] texts = CoolStringTool.splitOnce(textalternatives);
            try {
                while (alts != null) {
                    topStatusValues.put(new String(alts[0]), texts[0]);
                    alternatives = alts[1];
                    textalternatives = texts[1];
                    alts = CoolStringTool.splitOnce(alternatives);
                    texts = CoolStringTool.splitOnce(textalternatives);
                }
            } catch (Exception ex) {
                debug.error(this, "Can't create list of values ...");
                debug.error(ex);
            }
        }
    }
    
    /**
     * Set the import status.
     * Status codes: 0=Waiting for execution, 1=Import running, 2=Import complete, 3=Verification needed, 4=Error.
     * No change if status is already 2 (complete) or 4 (error).
     * 
     * @param string The new import status
     */
    private void setImportStatus(String string) {
        try {
            // 2=Fertig, 4=Fehler > keine Aenderung wenn Import Fehrlerhaft oder Fertig, bedeutet er ist komplett durch
            String actualImportStatus = context.getString("var.importstatus");
            if (actualImportStatus.equals("2") || actualImportStatus.equals("4")) {
                return;
            }
            
            if (null == dataAgent) {
                net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
                dataAgent = conn.getDataAgent();
            }
            
            String id = (String) session.get("CURRENT_OID");
            if (id == null || id.length() == 0) {
                id = (String) context.get("id");
            }
            context.set("var.importstatus", string);
            
            fixFileLink();
            
            if (id == null || id.length() == 0) {
                dataAgent.storeObject(context, context.getTemplateType(), null, session);
            } else {
                dataAgent.storeObject(context, context.getTemplateType(), id, session);
            }
        } catch (Exception e) {
            debug.error(e);
        }
    }
    
    /**
     * Fix file link workaround.
     * "file" is a reserved word in MySQL so cannot be exposed - this provides a workaround.
     */
    public void fixFileLink() {
        // file is a reserved word in mysql so cannot be exposed ... therefore this workaround
        if (!context.getString("var.datei").equals(context.getString("var.file"))) {
            context.set("var.datei", context.getString("var.file"));
        }
        if (!context.getString("var.edatei").equals(context.getString("var.efile"))) {
            context.set("var.edatei", context.getString("var.efile"));
        }
    }
    
    /**
     * Get Hausverwaltung (property management) from Haus OID.
     * 
     * @param oid_haus The Haus OID
     */
    private void getHausverwaltungFromHausOid(String oid_haus) {
        try {
            ArrayList<HashMap<String, String>> res = new ArrayList<>();
            
            ArgsHelper argsHelper = new ArgsHelper();
            argsHelper.setMainTemplateType("CIMS.haus");
            argsHelper.addTemplateType("hausverwaltungneu", "ICRScrm.firma");
            argsHelper.setAdvancedFields(true);
            
            argsHelper.addCondition("ID", oid_haus);
            argsHelper.addDomainCondition(session);
            argsHelper.addField("ID");
            argsHelper.addField("hausverwaltungneu_name", "hausverwaltung");
            
            // new Connector Class
            if (null == dataAgent) {
                net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
                dataAgent = conn.getDataAgent();
            }
            
            QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
            res = qr.getResult();
            
            if (res != null && res.size() == 1) {
                HashMap<String, String> row = res.get(0);
                String hausverwaltung = row.get("hausverwaltung");
                this.hausverwaltung = hausverwaltung;
            }
        } catch (Exception e) {
            BugMe.getInstance().error(e);
        }
    }
    
    /**
     * Get expiring contracts for a specific asset manager.
     * 
     * @param startDatum Start date for the period
     * @param endDatum End date for the period
     * @param assetmanager Asset manager identifier
     * @return Map containing asset manager as key and formatted message as value
     */
    private HashMap<String, String> getAblaufendeVertraegeForAssetmanager(
            Calendar startDatum, Calendar endDatum, String assetmanager) {
        
        HashMap<String, String> ablaufendevertraege = new HashMap<>();
        
        try {
            ArrayList<HashMap<String, String>> res = new ArrayList<>();
            
            ArgsHelper argsHelper = new ArgsHelper();
            
            argsHelper.setAdvancedFields(true);
            argsHelper.setMainTemplateType("CIMS.top");
            argsHelper.addTemplateType("REVtops", "CIMS.haus");
            argsHelper.addTemplateType("REVtops_assetmanager", "ICRScrm.assetmanager");
            
            argsHelper.addCondition("REVtops_assetmanager_name", 
                    assetmanager.substring(assetmanager.indexOf(";") + 1));
            argsHelper.addDomainCondition(session);
            argsHelper.addWhere("ET0.mietvertragbis <= CONVERT(datetime, '" + 
                    eDate.stringFromDate(endDatum.getTime()) + "', 104) AND ET0.mietvertragbis >= CONVERT(datetime, '" + 
                    eDate.stringFromDate(startDatum.getTime()) + "', 104) AND ET0.status='1'");
            
            argsHelper.addField("ID", "oid");
            argsHelper.addField("DOB.name", "topname");
            argsHelper.addField("REVtops_name", "adresse");
            argsHelper.addField("REVtops_identadresse1", "sapnummer");
            argsHelper.addField("REVtops_identadresse5", "senummer");
            argsHelper.addField("ET0.vertragid");
            argsHelper.addField("ET0.mieter");
            argsHelper.addField("ET0.istmietepm/100.", "istmietepm");
            argsHelper.addField("ET0.mietvertragbis");
            
            if (null == dataAgent) {
                net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
                dataAgent = conn.getDataAgent();
            }
            
            QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
            res = qr.getResult();
            
            if (res != null && res.size() > 0) {
                StringBuilder resultLines = new StringBuilder();
                
                for (int i = 0; i < res.size(); i++) {
                    HashMap<String, String> row = res.get(i);
                    
                    String oid = row.get("oid");
                    String topname = row.get("topname");
                    String adresse = row.get("adresse");
                    String sapnummer = row.get("sapnummer");
                    String senummer = row.get("senummer");
                    String vertragid = row.get("vertragid");
                    String mieter = row.get("mieter");
                    
                    String istmietepm = row.get("istmietepm");
                    istmietepm = formatString(istmietepm);
                    
                    String mietvertragbis = row.get("mietvertragbis");
                    if (mietvertragbis != null && mietvertragbis.contains(" ")) {
                        mietvertragbis = mietvertragbis.substring(0, mietvertragbis.indexOf(" ")).trim();
                    }
                    
                    String topurl = CoolStringTool.buildLink(oid, "SHOW", "", topname, "", "_blank", 
                            "ajaxLink redlink", global, session);
                    
                    String line = sapnummer + " " + adresse + " " + topurl + ", Vertragsnummer: " + vertragid + 
                            ", Mieter: <b>" + mieter + "</b>, Miete p.M.: " + istmietepm + 
                            "&euro; Mietvertragsende: <b>" + mietvertragbis + "</b><br><br>";
                    
                    resultLines.append(line);
                }
                
                if (resultLines.toString().length() > 0) {
                    StringBuilder salutation = new StringBuilder();
                    
                    salutation.append("Sehr geehrte(r) " + 
                            assetmanager.substring(assetmanager.indexOf(";") + 1) + "!");
                    salutation.append("<br>");
                    salutation.append("<br>");
                    salutation.append("Die folgenden Verträge laufen in den nächsten 6 Monaten ab:");
                    salutation.append("<br>");
                    salutation.append("<br>");
                    salutation.append(resultLines.toString());
                    salutation.append("<br>");
                    salutation.append("<br>");
                    salutation.append("Sollte eine Verlängerung oder Wiedervermietung geplant sein, " +
                            "ersuchen wir um <b>rechtzeitige Erfassung im SAP</b> bis zum Monatsletzten des Vormonats, " +
                            "um den korrekten Datenstand im PMS abbilden zu können.");
                    salutation.append("<br>");
                    salutation.append("<br>");
                    salutation.append("Eine verzögerte Erfassung kann in einer höheren Leerstandsquote resultieren!");
                    salutation.append("<br>");
                    salutation.append("<br>");
                    
                    if (assetmanagerAndIDs == null || assetmanagerAndIDs.size() == 0) {
                        assetmanagerAndIDs = getAllAssetmanagerAndIds(session);
                    }
                    
                    String dynurl = (String) CfgSingleton.getInstance().get("DYNAMIC_URLPATH", session, "dynamicurlpath");
                    if (null == dynurl) {
                        debug.error(this, "cannot read DYNAMIC_URLPATH!");
                        dynurl = "/NA";
                    }
                    
                    String sessid = session.getString("SESSIONID");
                    String linkClass = "ajaxLink";
                    String linkTarget = "_blank";
                    String url = dynurl + "?OID=DIRECT_ICRS.reports.report&reporttemplate=ICRS.reports.icrsare.auslaufendevertraegerepare";
                    
                    if (assetmanagerAndIDs.containsKey(assetmanager.substring(assetmanager.indexOf(";") + 1))) {
                        url += "&addfilterpreselectedvalues=queryassetmanager_ID=" + 
                                assetmanagerAndIDs.get(assetmanager.substring(assetmanager.indexOf(";") + 1));
                    }
                    
                    url += "&VIEW=SHOW&wrapper=NO";
                    String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
                    StringBuilder urlSB = new StringBuilder();
                    urlSB.append("<a href=\"");
                    urlSB.append(CoolWebTool.getUsedDomain(session));
                    urlSB.append(dynurl);
                    urlSB.append("?OID=" + CfgSingleton.getHijaxTarget(session) + "&contenturl=");
                    urlSB.append(encodedUrl);
                    urlSB.append("&FLAVOUR=");
                    String flavour = session.getString("flavour");
                    urlSB.append(flavour);
                    urlSB.append("&ESSENCEID=");
                    urlSB.append(sessid);
                    urlSB.append("\" ");
                    if (null != linkClass && linkClass.trim().length() > 0) {
                        urlSB.append(" class=\"" + linkClass + "\" ");
                    }
                    if (null != linkTarget && linkTarget.trim().length() > 0) {
                        urlSB.append(" target= \"" + linkTarget + "\" ");
                    }
                    urlSB.append(">");
                    urlSB.append("hier");
                    urlSB.append("</a>");
                    
                    salutation.append("Zur Abfrage der aktuell ablaufenden Mietverträge für Ihr Teilportfolio klicken Sie bitte " + 
                            urlSB + ".");
                    
                    salutation.append("<br>");
                    
                    ablaufendevertraege.put(assetmanager, salutation.toString());
                }
            }
        } catch (Exception e) {
            BugMe.getInstance().log(e);
        }
        
        return ablaufendevertraege;
    }
    
    /**
     * Get all asset managers and their IDs.
     * 
     * @param session Session object
     * @return Map of asset manager names to IDs
     */
    private HashMap<String, String> getAllAssetmanagerAndIds(DynGenDataObj session) {
        HashMap<String, String> assetmanagerAndIDs = new HashMap<>();
        
        try {
            ArrayList<HashMap<String, String>> res = new ArrayList<>();
            
            ArgsHelper argsHelper = new ArgsHelper();
            argsHelper.setMainTemplateType("ICRScrm.assetmanager");
            argsHelper.setAdvancedFields(true);
            
            argsHelper.addDomainCondition(session);
            argsHelper.addField("ID");
            argsHelper.addField("DOB.name", "name");
            
            if (null == dataAgent) {
                net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
                dataAgent = conn.getDataAgent();
            }
            
            QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
            res = qr.getResult();
            
            if (res != null && res.size() > 0) {
                for (int i = 0; i < res.size(); i++) {
                    HashMap<String, String> row = res.get(i);
                    String id = row.get("ID");
                    String name = row.get("name");
                    assetmanagerAndIDs.put(name, id);
                }
            }
        } catch (Exception e) {
            BugMe.getInstance().error(e);
        }
        
        return assetmanagerAndIDs;
    }
    
    /**
     * Format a numeric string to German decimal format.
     * 
     * @param value The value to format
     * @return Formatted string
     */
    private String formatString(String value) {
        try {
            DecimalFormat df = new DecimalFormat("#,##0.00", symbolsDE_DE);
            if (value.contains(".") && value.contains(",")) {
                value = value.replaceAll("\\.", "");
            }
            
            value = value.replaceAll(",", ".");
            
            String result = df.format(Double.parseDouble(value));
            return result;
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * Get information about who called this method.
     * Returns className.methodName:lineNumber of the calling method.
     * 
     * @return Caller information as string
     */
    public static String whoCalledMe() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement caller = stackTraceElements[4];
        String classname = caller.getClassName();
        String methodName = caller.getMethodName();
        int lineNumber = caller.getLineNumber();
        return classname + "." + methodName + ":" + lineNumber;
    }
    
    /**
     * Show the full call stack for debugging.
     * 
     * @param debug Debug logger to output the call stack
     */
    public static void showCallStack(BugMe debug) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        for (int i = 2; i < stackTraceElements.length; i++) {
            StackTraceElement ste = stackTraceElements[i];
            String classname = ste.getClassName();
            String methodName = ste.getMethodName();
            int lineNumber = ste.getLineNumber();
            debug.error(classname + "." + methodName + ":" + lineNumber);
        }
    }
    
    /**
     * Get the CRUD service, creating it if necessary.
     * 
     * @return CRUD service instance
     */
    private ZinslistenDatabaseCRUDService getCrudService() {
        if (crudService == null) {
            crudService = new ZinslistenDatabaseCRUDService(fileDataAgent, session, global, debug, context);
        }
        return crudService;
    }
    
    /**
     * Get top status values map.
     * 
     * @return Map of status codes to text values
     */
    public Map<String, String> getTopStatusValues() {
        if (topStatusValues == null || topStatusValues.isEmpty()) {
            loadTopStatusValues();
        }
        return topStatusValues;
    }
    
    /**
     * Get current Haus OID.
     * 
     * @return Current Haus OID
     */
    public String getCurrentHausOid() {
        return currentHausOid;
    }
    
    /**
     * Set current Haus OID.
     * 
     * @param oid The Haus OID
     */
    public void setCurrentHausOid(String oid) {
        this.currentHausOid = oid;
    }
    
    /**
     * Get Hausverwaltung.
     * 
     * @return Hausverwaltung value
     */
    public String getHausverwaltung() {
        return hausverwaltung;
    }
}
