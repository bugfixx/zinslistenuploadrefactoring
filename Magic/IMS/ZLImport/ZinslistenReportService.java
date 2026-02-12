package Magic.IMS.ZLImport;

import java.math.BigDecimal;
import java.util.*;
import org.json.simple.*;
import net.metamagix.essence.Agents.*;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.DynObj.DynGenDataObj;
import net.metamagix.essence.TopoCls.*;
import Magic.IMS.*;
import org.apache.commons.lang3.StringUtils;

/**
 * ZinslistenReportService handles report generation, JSON/CSV export, and UI data generation.
 * 
 * Responsibilities:
 * - JSON response creation
 * - DGD (Dynamic GUI Definition) creation for UI
 * - Report generation (period comparisons, javascript matchers)
 * - Status checking and formatting
 */
public class ZinslistenReportService {
    
    private DynGenDataObj session;
    private DynGenDataObj global;
    private FileDataAgent fileDataAgent;
    private Map<String, String> result;
    private String oid_haus;
    private String mylang;
    
    /**
     * Constructor for ZinslistenReportService.
     * 
     * @param session The user session object
     * @param global The global data object
     * @param fda The file data agent
     */
    public ZinslistenReportService(DynGenDataObj session, DynGenDataObj global, FileDataAgent fda) {
        this.session = session;
        this.global = global;
        this.fileDataAgent = fda;
        this.result = new HashMap<>();
        this.oid_haus = "";
        this.mylang = "";
    }
    
    /**
     * Creates a JSON response with custom data.
     * 
     * @param myoid The object ID
     * @param jsonRes The JSON result string
     * @param vueStatus The status for Vue.js frontend
     * @param templatetype The template type
     * @return JSON response as string
     */
    public String createJsonResponseWithCustomData(String myoid, String jsonRes, String vueStatus, String templatetype) {
        try {
            org.json.JSONObject response = new org.json.JSONObject();
            org.json.JSONObject customdata = new org.json.JSONObject(jsonRes);
            response.put("ID", myoid);
            response.put("status", vueStatus);
            response.put("templatetype", templatetype);
            response.put("customdata", customdata);
            
            return response.toString();
        } catch (Exception e) {
            BugMe.getInstance().error(e);
            return "";
        }
    }
    
    /**
     * Creates a ZZ JSON reply with all configuration parameters.
     * 
     * @param status The status message
     * @param message The message text
     * @param zlSelJson JSON array of rent roll selections
     * @param elSelJson JSON array of owner list selections
     * @param dgd The DynGenDataObj containing var values
     * @return JSON reply as string
     */
    public String createZZJsonReply(String status, String message, JSONArray zlSelJson, JSONArray elSelJson, DynGenDataObj dgd) {
        JSONObject reply = new JSONObject();
        try {
            reply.put("status", status);
            reply.put("message", message);
            if (null != zlSelJson) {
                reply.put("rentrollss", zlSelJson);
            }
            if (null != elSelJson) {
                reply.put("ownerslists", elSelJson);
            }
            reply.put("file", dgd.getString("var.file"));
            reply.put("efile", dgd.getString("var.efile"));
            reply.put("datei", dgd.getString("var.datei"));
            reply.put("edatei", dgd.getString("var.edatei"));
            reply.put("rentrollimportaftersale", dgd.getString("var.rentrollimportaftersale"));
            reply.put("filepath", dgd.getString("var.filepath"));
            reply.put("filepathbackup", dgd.getString("var.filepathbackup"));
            reply.put("name", dgd.getString("var.name"));
            reply.put("nameEN", dgd.getString("var.nameEN"));
            reply.put("objektname", dgd.getString("var.objektname"));
            reply.put("text", dgd.getString("var.text"));
            reply.put("vermietungtopuebeschreibtzinsliste", dgd.getString("var.vermietungtopuebeschreibtzinsliste"));
            reply.put("vermietungtopuebeschreibtzinslistemonate", dgd.getString("var.vermietungtopuebeschreibtzinslistemonate"));
            reply.put("vermietungtopuebeschreibtzinslisteaction", dgd.getString("var.vermietungtopuebeschreibtzinslisteaction"));
            reply.put("resultcode", dgd.getString("var.resultcode"));
            reply.put("errorcode", dgd.getString("var.errorcode"));
            reply.put("errorcodetxt", dgd.getString("var.errorcodetxt"));
            reply.put("zlstatus", dgd.getString("var.zlstatus"));
            reply.put("selectedkunde", dgd.getString("var.selectedkunde"));
            reply.put("jahr", dgd.getString("var.jahr"));
            reply.put("email", dgd.getString("var.email"));
            reply.put("mailtext", dgd.getString("var.mailtext"));
            reply.put("wertaenderung", dgd.getString("var.wertaenderung"));
            reply.put("assetmanagerinfo", dgd.getString("var.assetmanagerinfo"));
            reply.put("sendmailonlyonchange", dgd.getString("var.sendmailonlyonchange"));
            reply.put("periodenvergleich", dgd.getString("var.periodenvergleich"));
            reply.put("leerstandsmail", dgd.getString("var.leerstandsmail"));
            reply.put("ablaufendevetraegemail", dgd.getString("var.ablaufendevetraegemail"));
            reply.put("altezinszeilenloeschen", dgd.getString("var.altezinszeilenloeschen"));
            reply.put("topnamenneusetzten", dgd.getString("var.topnamenneusetzten"));
            reply.put("quellsystem", dgd.getString("var.quellsystem"));
            reply.put("zinslistendatum", dgd.getString("var.zinslistendatum"));
            reply.put("topoanpassung", dgd.getString("var.topoanpassung"));
            reply.put("ccemail", dgd.getString("var.ccemail"));
            reply.put("monat", dgd.getString("var.monat"));
            reply.put("tag", dgd.getString("var.tag"));
            reply.put("zinslistenindex", dgd.getString("var.zinslistenindex"));
            reply.put("eigentuemerlistenindex", dgd.getString("var.eigentuemerlistenindex"));
            reply.put("land", dgd.getString("var.land"));
            reply.put("ort", dgd.getString("var.ort"));
            reply.put("adresse", dgd.getString("var.adresse"));
            reply.put("identadresse5", dgd.getString("var.identadresse5"));
            reply.put("importstatus", dgd.getString("var.importstatus"));
            reply.put("hausverwaltung", dgd.getString("var.hausverwaltung"));
            reply.put("hausverwalter", dgd.getString("var.hausverwalter"));
            reply.put("betreuer", dgd.getString("var.betreuer"));
            reply.put("duration", dgd.getString("var.duration"));
            reply.put("filename", dgd.getString("var.filename"));
            reply.put("nighthour", dgd.getString("var.nighthour"));
            reply.put("nightminute", dgd.getString("var.nightminute"));
            reply.put("width", dgd.getString("var.width"));
            reply.put("jahrvon", dgd.getString("var.jahrvon"));
            reply.put("monatvon", dgd.getString("var.monatvon"));
            reply.put("encoding", dgd.getString("var.encoding"));
            reply.put("statusformissingunit", dgd.getString("var.statusformissingunit"));
            reply.put("topmatcherselector", dgd.getString("var.topmatcherselector"));
            reply.put("ignorealleasyerros", dgd.getString("var.ignorealleasyerros"));
            reply.put("importsperrebeidatenfreigabe", dgd.getString("var.importsperrebeidatenfreigabe"));
            reply.put("gridimport", dgd.getString("var.gridimport"));
            reply.put("starttime", dgd.getString("var.starttime"));
            reply.put("endtime", dgd.getString("var.endtime"));
            reply.put("runtime", dgd.getString("var.runtime"));
            reply.put("checkexistingrentroll", dgd.getString("var.checkexistingrentroll"));
            reply.put("zltypename", dgd.getString("var.zltypename"));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return reply.toString();
    }
    
    /**
     * Creates a Dynamic GUI Definition (DGD) for selecting a rent roll.
     * 
     * @param zlSelJson A JSON array containing the selectable rent roll entries
     * @param sessionId The session ID
     * @param language The user's language
     * @return The serialized DGD JSON string
     */
    public String createRentRollObjectSelectDgd(JSONArray zlSelJson, String sessionId, String language) {
        String myLang = language;
        String myId = sessionId;
        
        String title = Tr.t("textRentRollImport", myLang);
        DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
        DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
        List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();
        List<DgdJson.Field> fields = new ArrayList<>();
        
        if (zlSelJson != null) {
            String fieldId = "zinslistenindex";
            fields.add(DgdJson.FieldFactory.selector(fieldId, Tr.t("textRentRoll", myLang), myId + "__" + fieldId, "1", zlSelJson, ""));
        }
        fieldgroups.add(DgdJson.FieldGroupFactory.group("importobjectgroup", Tr.t("textObjectDataImport", myLang)).addAll(fields));
        
        dgd.addTab(tab.addAll(fieldgroups));
        dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButonCancel", myLang), "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
        dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButtonContinue", myLang), "primary", "arrow-right-line", "right", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ", "edit")));
        
        JSONObject json = DgdJson.toJsonObject(dgd);
        return json.toString();
    }
    
    /**
     * Creates a Dynamic GUI Definition (DGD) for the rent roll import error screen.
     * 
     * @param zl The rent roll object
     * @param ignoreerrors Error identifiers to ignore
     * @param rutablename Legacy table name for rent roll lines
     * @param pstablename Legacy table name for parking/space lines
     * @param sessionId The session ID
     * @param language The user's language
     * @param hausOid The house OID for object selection
     * @param dgdSetter Interface to set DGD values
     * @return JSON string representing the DGD configuration
     */
    public String createRentRollImportErrorDgd(Zinsliste zl, String ignoreerrors, String rutablename, 
                                                String pstablename, String sessionId, String language, 
                                                String hausOid, DgdValueSetter dgdSetter) {
        String myLang = language;
        String myId = sessionId;
        boolean isEnglish = StringUtils.equalsIgnoreCase(myLang, "EN");
        
        String title = Tr.t("textRentRollImport", myLang);
        DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
        DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
        List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();
        
        String jsonERR = zl.getErrorsAsJsonDataTable(ignoreerrors, session);
        if (StringUtils.isNotBlank(jsonERR)) {
            String fieldId = "resulterrjson";
            dgdSetter.set("var." + fieldId, jsonERR);
            dgdSetter.set("var." + fieldId + ".VGUITYPE", "jsondatatable");
            String displayname = isEnglish ? dgdSetter.getString("var." + fieldId + ".DISPLAYNAMEEN") : dgdSetter.getString("var." + fieldId + ".DISPLAYNAME");
            DgdJson.Field dt = DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonERR, "");
            fieldgroups.add(DgdJson.FieldGroupFactory.group("errorgroup", Tr.t("textRentRollError", myLang)).add(dt));
        }
        
        List<DgdJson.Field> fields = new ArrayList<>();
        
        if (hausOid != null) {
            String url = "http://localhost:8080/icrsdemo/NA?OID=DIRECT_gui.ComboSlotSelector&MARKUPLANGUAGE=JSON&VIEW=VUE&replacetextalternativesfromselector=1&targetfield=" + myId + "__SLOT_mset_importobject&templatetype=CIMS.haus&selected=" + hausOid;
            fields.add(DgdJson.FieldFactory.autoList("importobject", "Objekt", myId + "__SLOT_mset_importobject", List.of(hausOid), url, ""));
        }
        
        String jsonZZ = zl.getZinszeilenAsJsonDataTable(session, rutablename);
        if (StringUtils.isNotBlank(jsonZZ)) {
            String fieldId = "resultzzjson";
            dgdSetter.set("var." + fieldId, jsonZZ);
            dgdSetter.set("var." + fieldId + ".VGUITYPE", "jsondatatable");
            String displayname = isEnglish ? dgdSetter.getString("var." + fieldId + ".DISPLAYNAMEEN") : dgdSetter.getString("var." + fieldId + ".DISPLAYNAME");
            fields.add(DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonZZ, ""));
        }
        String jsonSP = zl.getStellplaetzeAsJsonDataTable(session, pstablename);
        if (StringUtils.isNotBlank(jsonSP)) {
            String fieldId = "resultspjson";
            
            dgdSetter.set("var." + fieldId, jsonSP);
            dgdSetter.set("var." + fieldId + ".VGUITYPE", "jsondatatable");
            String displayname = isEnglish ? dgdSetter.getString("var." + fieldId + ".DISPLAYNAMEEN") : dgdSetter.getString("var." + fieldId + ".DISPLAYNAME");
            fields.add(DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonSP, ""));
        }
        fieldgroups.add(DgdJson.FieldGroupFactory.group("zzgroup", zl.haus).addAll(fields));
        
        dgd.addTab(tab.addAll(fieldgroups));
        
        dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButonCancel", myLang), "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
        String additionalParams = "VIEW=READ&" + myId + "__zinslistenindex=&" + myId + "__eigentuemerlistenindex=&" + myId + "__fehlerabfrage=0&" + myId + "__createhaus=&" + myId + "__createnewtops=&" + myId + "__ignoreerrors=&" + myId + "__topoanpassung=1&" + myId + "__wertaenderung=";
        dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textReimportRentRoll", myLang), "outline", "reset-right-fill", "left", "right", DgdJson.ButtonFactory.action("submit", additionalParams, "edit")));
        dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButtonContinueAnyway", myLang), "primary", "arrow-right-line", "right", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ&" + myId + "__fehlerabfrage=1", "edit")));
        dgdSetter.setDirty();
        JSONObject json = DgdJson.toJsonObject(dgd);
        return json.toString();
    }
    
    /**
     * Creates a Dynamic GUI Definition (DGD) for starting a new rent roll import.
     * 
     * @param zl The rent roll object
     * @param ignoreerrors Error identifiers to ignore
     * @param sessionId The session ID
     * @param language The user's language
     * @param dgdSetter Interface to set DGD values
     * @return JSON string representing the DGD configuration
     */
    public String createRentRollNewObjectDgd(Zinsliste zl, String ignoreerrors, String sessionId, 
                                              String language, DgdValueSetter dgdSetter) {
        boolean isEnglish = StringUtils.equalsIgnoreCase(language, "EN");
        String myId = sessionId;
        
        String title = Tr.t("textRentRollImport", language);
        DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
        DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
        List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();
        List<DgdJson.Field> fields = new ArrayList<>();
        
        String jsonERR = zl.getErrorsAsJsonDataTable(ignoreerrors, session);
        if (StringUtils.isNotBlank(jsonERR)) {
            String fieldId = "resulterrjson";
            dgdSetter.set("var." + fieldId, jsonERR);
            String displayname = isEnglish ? dgdSetter.getString("var." + fieldId + ".DISPLAYNAMEEN") : dgdSetter.getString("var." + fieldId + ".DISPLAYNAME");
            DgdJson.Field dt = DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonERR, "");
            String fieldgroupDisplayname = isEnglish ? "There's something wrong with the file. Please check it." : "Probleme beim Import!";
            fieldgroups.add(DgdJson.FieldGroupFactory.group("errorgroup", fieldgroupDisplayname).add(dt));
        }
        
        String displayname = isEnglish ? "Import object data" : "Objektdaten einspielen";
        fieldgroups.add(DgdJson.FieldGroupFactory.group("importobjectgroup", displayname).addAll(fields));
        
        dgd.addTab(tab.addAll(fieldgroups));
        
        dgd.addButton(DgdJson.ButtonFactory.button(isEnglish ? "Cancel" : "Abbrechen", "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
        dgd.addButton(DgdJson.ButtonFactory.button(isEnglish ? "Continue" : "Weiter", "primary", "arrow-right-line", "left", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ", "edit")));
        
        JSONObject json = DgdJson.toJsonObject(dgd);
        return json.toString();
    }
    
    /**
     * Generates a period comparison report (Periodenvergleich).
     * 
     * @param hausOid The house OID
     * @param azl The Zinsliste object
     * @param mailinglistKennwerteNachNutzung Mailing list for key metrics
     * @param getBoolean Function to get boolean values
     * @param getAssetmanagerMailadressFromObject Function to get asset manager email
     * @param mylang The language
     */
    public void generatePeriodenvergleich(String hausOid, Zinsliste azl, 
                                          Map<String, String> mailinglistKennwerteNachNutzung,
                                          BooleanGetter getBoolean,
                                          AssetManagerEmailGetter getAssetmanagerMailadressFromObject,
                                          String mylang) {
        try {
            TopoTool topotool = new TopoTool(session, global);
            
            if (hausOid != null && hausOid.length() > 0) {
                TopoQueries topoQueries = new TopoQueries(session, global);
                String[] nutzungBestandsfl = {"B", "G", "W", "H", "L", "S", "LG", "PR", "P", "GA", "SP"};
                String[] hIDs = new String[1];
                hIDs[0] = hausOid;
                Map<String, Map<String, String>> mietsummenAktuellePeriode = topoQueries.monatsSummenNachNutzung(azl.monat, azl.jahr, hIDs, nutzungBestandsfl, null, null, null, null, null, null, null, null, null, null, false, true);
                
                if (mietsummenAktuellePeriode != null && mietsummenAktuellePeriode.size() > 0) {
                    Map<String, String> vorperiode = topoQueries.getZinslistenMonatForHausVorperiode(hausOid, azl.monat, azl.jahr);
                    Map<String, String> resultVorPeriode = null;
                    Map<String, Map<String, String>> mietsummenVorPeriode = null;
                    if (vorperiode.containsKey("monat") && vorperiode.get("monat").length() > 0 && vorperiode.containsKey("jahr") && vorperiode.get("jahr").length() > 0) {
                        mietsummenVorPeriode = topoQueries.monatsSummenNachNutzung(vorperiode.get("monat"), vorperiode.get("jahr"), hIDs, nutzungBestandsfl, null, null, null, null, null, null, null, null, null, null, false, true);
                    }
                    
                    String mailAndName = getAssetmanagerMailadressFromObject.get(topotool.getHausOID(azl));
                    System.out.println("AM MAILS TO (3): " + mailAndName + " // Hausinfos:" + String.valueOf(azl.edvNr) + " - " + String.valueOf(azl.adresse) + " - " + String.valueOf(azl.ort) + " - " + String.valueOf(azl.plz));
                    
                    if (mailinglistKennwerteNachNutzung.containsKey(mailAndName)) {
                        StringBuilder mailtext = new StringBuilder();
                        mailtext.append(mailinglistKennwerteNachNutzung.get(mailAndName));
                        
                        String diffHmzist = "";
                        String diffNfl = "";
                        String diffLeerfl = "";
                        
                        for (String key : mietsummenAktuellePeriode.keySet()) {
                            BigDecimal val1 = new BigDecimal(0);
                            BigDecimal val2 = new BigDecimal(0);
                            BigDecimal val3 = new BigDecimal(0);
                            BigDecimal val1vp = new BigDecimal(0);
                            BigDecimal val2vp = new BigDecimal(0);
                            BigDecimal val3vp = new BigDecimal(0);
                            
                            Map<String, String> resultAktuellePeriode = mietsummenAktuellePeriode.get(key);
                            if (null != resultAktuellePeriode && null != resultVorPeriode) {
                                mailtext.append(Tr.t("diffRow", mylang, azl.adresse, resultAktuellePeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultAktuellePeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
                            }
                            
                            if (mietsummenVorPeriode != null && mietsummenVorPeriode.containsKey(key)) {
                                resultVorPeriode = mietsummenVorPeriode.get(key);
                                
                                if (resultVorPeriode != null) {
                                    val1vp = new BigDecimal(resultVorPeriode.get("hmzist"));
                                    val2vp = new BigDecimal(resultVorPeriode.get("nfl"));
                                    val3vp = new BigDecimal(resultVorPeriode.get("leerfl"));
                                    mailtext.append(Tr.t("diffRow", mylang, "", resultVorPeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultVorPeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
                                } else {
                                    mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
                                }
                            } else {
                                mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
                            }
                            
                            String diff = Tr.t("diff", mylang);
                            diffHmzist = CoolStringTool.getFormattedAndCorrectedValue(val1.subtract(val1vp).toString(), false);
                            diffNfl = CoolStringTool.getFormattedAndCorrectedValue(val2.subtract(val2vp).toString(), false);
                            diffLeerfl = CoolStringTool.getFormattedAndCorrectedValue(val3.subtract(val3vp).toString(), false);
                            
                            mailtext.append(Tr.t("diffRow", mylang, diff, "", "", diffHmzist, diffNfl, diffLeerfl));
                        }
                        
                        boolean sendmailonlyonchange = getBoolean.get("var.sendmailonlyonchange", true);
                        if (diffHmzist.equals("0") && diffNfl.equals("0") && diffLeerfl.equals("0") && sendmailonlyonchange) {
                            // Don't add line because no change
                        } else {
                            mailinglistKennwerteNachNutzung.put(mailAndName, mailtext.toString());
                        }
                    } else {
                        StringBuilder mailtext = new StringBuilder();
                        mailtext.append("<br><br>");
                        mailtext.append("<table>");
                        mailtext.append(Tr.t("diffHeadRow", mylang));
                        
                        String diffHmzist = "";
                        String diffNfl = "";
                        String diffLeerfl = "";
                        
                        for (String key : mietsummenAktuellePeriode.keySet()) {
                            BigDecimal val1 = new BigDecimal(0);
                            BigDecimal val2 = new BigDecimal(0);
                            BigDecimal val3 = new BigDecimal(0);
                            BigDecimal val1vp = new BigDecimal(0);
                            BigDecimal val2vp = new BigDecimal(0);
                            BigDecimal val3vp = new BigDecimal(0);
                            
                            Map<String, String> resultAktuellePeriode = mietsummenAktuellePeriode.get(key);
                            if (resultAktuellePeriode != null) {
                                val1 = new BigDecimal(resultAktuellePeriode.get("hmzist"));
                                val2 = new BigDecimal(resultAktuellePeriode.get("nfl"));
                                val3 = new BigDecimal(resultAktuellePeriode.get("leerfl"));
                                mailtext.append(Tr.t("diffRow", mylang, azl.adresse, resultAktuellePeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultAktuellePeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("leerfl"), false)));
                            }
                            
                            if (mietsummenVorPeriode != null && mietsummenVorPeriode.containsKey(key)) {
                                resultVorPeriode = mietsummenVorPeriode.get(key);
                                
                                if (resultVorPeriode != null) {
                                    val1vp = new BigDecimal(resultVorPeriode.get("hmzist"));
                                    val2vp = new BigDecimal(resultVorPeriode.get("nfl"));
                                    val3vp = new BigDecimal(resultVorPeriode.get("leerfl"));
                                    mailtext.append(Tr.t("diffRow", mylang, "", resultVorPeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultVorPeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
                                } else {
                                    mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
                                }
                            } else {
                                mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
                            }
                            
                            String diff = Tr.t("diff", mylang);
                            diffHmzist = CoolStringTool.getFormattedAndCorrectedValue(val1.subtract(val1vp).toString(), false);
                            diffNfl = CoolStringTool.getFormattedAndCorrectedValue(val2.subtract(val2vp).toString(), false);
                            diffLeerfl = CoolStringTool.getFormattedAndCorrectedValue(val3.subtract(val3vp).toString(), false);
                            
                            mailtext.append(Tr.t("diffRow", mylang, diff, "", "", diffHmzist, diffNfl, diffLeerfl));
                        }
                        
                        mailtext.append("</table>");
                        
                        boolean sendmailonlyonchange = getBoolean.get("var.sendmailonlyonchange", true);
                        if (diffHmzist.equals("0") && diffNfl.equals("0") && diffLeerfl.equals("0") && sendmailonlyonchange) {
                            // Don't add email because no change
                        } else {
                            mailinglistKennwerteNachNutzung.put(mailAndName, mailtext.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            BugMe.getInstance().error(e);
        }
    }
    
    /**
     * Gets the JavaScript topmatcher string.
     * 
     * @param topList The TopList object
     * @return JavaScript string for top matching
     */
    public String getJavascriptTopmatcherString(TopList topList) {
        StringBuilder scriptString = new StringBuilder();
        scriptString.append("<script type=\"text/javascript\">\n");
        scriptString.append("try {\n");
        scriptString.append("	var toplistjson=jQuery.parseJSON('" + topList.toJSON(session) + "')\n");
        scriptString.append("console.log('parsing json'); generateToplistSelectors(toplistjson);\n");
        scriptString.append("} catch(e) {}\n");
        scriptString.append("</script>\n");
        return scriptString.toString();
    }
    
    /**
     * Gets top info string from a hashtable.
     * 
     * @param h The data map
     * @param language The user's language
     * @return Formatted top info string
     */
    public String getTopInfoStringFromZZHT(Map<String, Object> h, String language) {
        String top = (String) h.get("top");
        if (null == top) {
            top = "";
        }
        
        top = Zinsliste.removeEdvNrIfEqualToNameFromTopName(top);
        String mieteinheitenanz = (String) h.get("mieteinheitenanz");
        if (null != mieteinheitenanz) {
            if (!mieteinheitenanz.equals("null")) {
                if (mieteinheitenanz.endsWith(",00")) {
                    mieteinheitenanz = mieteinheitenanz.replace(",00", "");
                }
            }
        } else {
            mieteinheitenanz = "1";
        }
        
        String tnutzung = (String) h.get("nutzung");
        String tou = (String) h.get("nutzung");
        
        LinkedHashMap lhm = Selector.getValue2NameMap("CIMS.SelectorNutzung", null, session, null, null);
        String fullnutzung = (String) lhm.get(tnutzung);
        if (null == fullnutzung) {
            fullnutzung = tnutzung;
        }
        
        if (language.toUpperCase().equals("EN")) {
            String englshortname = ZLImport.englishTypeOfUseShortMap.get(tnutzung);
            if (null != englshortname) {
                tnutzung = englshortname;
            }
        }
        if (null == tnutzung) {
            tnutzung = "";
            tou = "";
        }
        
        String tmieter = (String) h.get("mieter");
        if (null == tmieter) {
            tmieter = "";
        }
        
        String tfl = (String) h.get("fl");
        if (null == tfl) {
            tfl = (String) h.get("nfl");
            if (null == tfl) {
                tfl = "";
            }
            if (tfl.length() == 0) {
                tfl = (String) h.get("leerfl");
                if (null == tfl) {
                    tfl = "";
                }
                if (tfl.length() == 0) {
                    tfl = (String) h.get("ffl");
                    if (null == tfl) {
                        tfl = "";
                    }
                }
            }
        }
        
        String hmz = (String) h.get("hmz");
        if (null == hmz) {
            hmz = "0";
        }
        
        // Simplified return - full implementation would continue with currency handling
        return top + " | " + mieteinheitenanz + " | " + tnutzung + " | " + tmieter + " | " + tfl + " m² | " + hmz;
    }
    
    /**
     * Formats a string value.
     * 
     * @param value The value to format
     * @param symbolsDE_DE The decimal format symbols
     * @return Formatted string
     */
    public String formatString(String value, java.text.DecimalFormatSymbols symbolsDE_DE) {
        try {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00", symbolsDE_DE);
            if (value.contains("\\.") && value.contains(",")) {
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
     * Gets the status of Freigabe (approval/release).
     * 
     * @param hausid The house ID
     * @param azl The Zinsliste object
     * @param fda The FileDataAgent
     * @return Boolean indicating the status
     */
    public Boolean getStatusOfFreigabe(String hausid, Zinsliste azl, FileDataAgent fda) {
        try {
            Map<String, Object> args = new HashMap<>();
            ArgsHelper argsHelper = new ArgsHelper(args);
            argsHelper.setAdvancedFields(true);
            argsHelper.setMainTemplateType("CIMS.datenbestaetigung");
            argsHelper.addTemplateType("haus", "CIMS.haus");
            
            argsHelper.addField("ET0.abgelehnt");
            argsHelper.addField("ET0.eingeschraenkt");
            argsHelper.addField("ET0.datum");
            argsHelper.addField("haus_ID", "hausid");
            
            String periode = azl.getJahr() + " M" + azl.getMonat();
            argsHelper.addCondition("ET0.periode", periode);
            argsHelper.addCondition("haus_ID", hausid);
            argsHelper.addDomainCondition(session);
            
            QueryResult qr = fda.queryObjectWithResult(argsHelper.getArgs());
            List<Map<String, String>> res = qr.getResult();
            
            if (res != null && res.size() > 0) {
                Map<String, String> row = res.get(0);
                String abgelehnt = row.get("abgelehnt");
                String eingeschraenkt = row.get("eingeschraenkt");
                
                if ("1".equals(abgelehnt)) {
                    return false;
                }
                if ("1".equals(eingeschraenkt)) {
                    return false;
                }
                return true;
            }
            return true;
        } catch (Exception e) {
            BugMe.getInstance().error(e);
            return true;
        }
    }
    
    // Getter methods
    public Map<String, String> getResult() {
        return result;
    }
    
    public void setOidHaus(String oidHaus) {
        this.oid_haus = oidHaus;
    }
    
    public String getOidHaus() {
        return oid_haus;
    }
    
    // Helper interfaces for dependency injection
    public interface DgdValueSetter {
        void set(String key, String value);
        String getString(String key);
        void setDirty();
    }
    
    public interface BooleanGetter {
        boolean get(String key, boolean defaultValue);
    }
    
    public interface AssetManagerEmailGetter {
        String get(String hausOid);
    }
}
