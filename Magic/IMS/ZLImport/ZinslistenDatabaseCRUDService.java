package Magic.IMS.ZLImport;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Agents.ParseResult;
import net.metamagix.essence.Agents.QueryResult;
import net.metamagix.essence.Agents.TemplateReader;
import net.metamagix.essence.Agents.ThreadAgent;
import net.metamagix.essence.Agents.threads.EThreadGroup;
import net.metamagix.essence.Agents.threads.EThreadParams;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.GenData.ByteArrayDataSource;
import net.metamagix.essence.GenData.GenDataClass;
import net.metamagix.essence.GenData.OriginalCurrencyValue;
import net.metamagix.essence.GenData.Slot;
import net.metamagix.essence.GenData.VectorOfHashesSorter;
import net.metamagix.essence.MConfig.CfgSingleton;
import net.metamagix.essence.SAP.loadconfig.QueryHelper;
import net.metamagix.essence.SAP.loadconfig.SAPQuery;
import net.metamagix.essence.SAPCSV.SAPCSVQuery;
import net.metamagix.essence.Swagger.SwaggerQuery;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.ajax.JSONArray;
import net.metamagix.essence.ajax.JSONException;
import net.metamagix.essence.ajax.JSONObject;
import net.metamagix.essence.eSSENCETypes.Currency;
import net.metamagix.essence.eSSENCETypes.DateTime;
import net.metamagix.essence.eSSENCETypes.eDate;
import net.metamagix.essence.tools.CoolBulkStoreTool;
import net.metamagix.essence.tools.CoolDataTool;
import net.metamagix.essence.tools.CoolJSONTool;
import net.metamagix.essence.tools.CoolLLMWrapperTool;
import net.metamagix.essence.tools.CoolStringTool;
import net.metamagix.essence.tools.CoolTemplateTool;
import net.metamagix.essence.tools.Translation.Tr;

import Magic.IMS.*;

/**
 * Service class for database CRUD operations related to Zinslisten (rent rolls).
 * Extracted from UploadXLS5 to separate database operations from the main upload logic.
 * 
 * <p>This class handles creating, reading, updating, and deleting database objects
 * for rent roll processing, including:</p>
 * <ul>
 *   <li>Building (Haus) creation and updates</li>
 *   <li>Rental unit (Top/Stellplatz) creation and updates</li>
 *   <li>Rent roll line (Zinszeile) creation and management</li>
 *   <li>Rental contract (Mietvertrag) linkage and management</li>
 *   <li>Multi-currency value handling</li>
 * </ul>
 * 
 * @author Auto-generated from refactoring
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ZinslistenDatabaseCRUDService {
    
    private final FileDataAgent fda;
    private final DynGenDataObj session;
    private final DynGenDataObj global;
    private final BugMe debug;
    private final Magic.IMS.UploadXLS5 parentObject;
    
    private int overwritezz = 0;
    private int createzz = 0;
    private int resultSizeOfStoredObjects = 0;
    
    /**
     * Constructor for ZinslistenDatabaseCRUDService
     * 
     * @param fda FileDataAgent for database operations
     * @param session Session DynGenDataObj containing user session data
     * @param global Global DynGenDataObj containing global application data
     * @param debug BugMe instance for logging and debugging
     * @param parentObject Reference to parent UploadXLS5 object for accessing shared fields and methods
     */
    public ZinslistenDatabaseCRUDService(FileDataAgent fda, DynGenDataObj session, DynGenDataObj global, BugMe debug, Magic.IMS.UploadXLS5 parentObject) {
        this.fda = fda;
        this.session = session;
        this.global = global;
        this.debug = debug;
        this.parentObject = parentObject;
    }
    
    /**
     * Gets the count of overwritten Zinszeilen
     * @return Count of overwritten entries
     */
    public int getOverwriteZZCount() {
        return overwritezz;
    }
    
    /**
     * Gets the count of created Zinszeilen
     * @return Count of created entries
     */
    public int getCreateZZCount() {
        return createzz;
    }
    
    /**
     * Gets the result size of stored objects
     * @return Size of stored objects result
     */
    public int getResultSizeOfStoredObjects() {
        return resultSizeOfStoredObjects;
    }
    
		private boolean junkStore(Hashtable newtops, String oid_haus)
	{
		try
		{
			parentObject.log("Lege " + newtops.size() + " Mieteinheiten an.");
			Hashtable tres = storeObjectsJunked(newtops, session);
			if(tres != null)
			{
				addTopsToHaus(tres, parentObject.oid_haus);

				// if(oid_gebaeude != null && oid_gebaeude.length() > 0)
				// {
				// addTopsToGebaeude(tres, oid_gebaeude);
				// }
			}
		}
		catch(Exception xx)
		{
			parentObject.log("Problem storing objects...");
			return false;
		}
		return true;
	}

		private Zinsliste createVerknuepfungZuMietvertrag(TopList top_list, Zinsliste zl, boolean fehlerabfrage)
	{
		Hashtable<String, Vector> topsToWrite = new HashMap<String, Vector>();

		// Ueber alle Elemente der Toplist iterieren und cheken ob Mietvertrag vorhanden ist
		if(parentObject.zinslistenImport.getZlTypeConfig().isVertragsverknuepfung())
		{
			try
			{

				String[] oids_top = top_list.getTopIDs();

				for(int i = 0; i < oids_top.length; i++)
				{
					if(null == parentObject.topsCache)
					{
						parentObject.topsCache = new HashMap();
					}
					if(!parentObject.topsCache.containsKey(oids_top[i]))
					{
						// topscache irgendwie kapputt?
						debug.error(this, "bad top im topscache: " + oids_top[i]);
					}
				}

				Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();

				Hashtable<String, Object> args = new HashMap<String, Object>();

				args.put("advancedfields", "TRUE");

				String mydom = (String)session.get("domainid");
				if(mydom.length() == 0)
				{
					args.put("DOMAIN", "ALLDOMAINS");
				}
				else
				{
					args.put("DOMAIN", mydom);
				}

				args.put("TType", "CIMS.mietvertrag");
				args.put("mieterfirma_templatetype", "ICRScrm.firma");
				args.put("mieterfirma_OUTERJOIN", "TRUE");

				args.put("tops_templatetype", "CIMS.top");
				args.put("fieldClause", "ID,DOB.name vertragsname,ET0.vertragid,SLOTCOLLAPSE(tops_ID) tops,SLOTCOLLAPSE(mieterfirma_name) mieterfirma, ET0.nfl");
				args.put("tops_OUTERJOIN", "TRUE");
				// einschraenkung sinnlos, da zu diesem Zeitpunkt noch nicht bekannt ist zu welchen Haus/Top der Vertrag zugeordnet ist!
				// args.put("tops_REVtop_ID", oids_top); // Einschraenkung auf die aktuell in der Zinsliste vorkommenden tops pro haus

				if(null == parentObject.DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					parentObject.DAInst = conn.getDataAgent();
				}
				QueryResult qr = parentObject.DAInst.queryObjectWithResult(args);
				if(!qr.isOK())
				{
					debug.error(this, "problem with sql in UploadXLS4:" + qr.getSql());
				}
				res1 = qr.getResult();

				// Tops den Mietvertraegen zuordnen
				Hashtable<String, MietvertragElement> topidsZuMietvertrag = new HashMap<String, MietvertragElement>();
				for(int j = 0; j < zl.zinszeilen.size(); j++)
				{
					Hashtable ht = (Hashtable)zl.zinszeilen.get(j);
					String top = (String)ht.get("top");

					TopElement te = null;
					// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
					String sapnummer = "";
					if(te == null && ht.containsKey("sapnummer"))
					{
						sapnummer = (String)ht.get("sapnummer");
						te = top_list.getTopBySapnummer(sapnummer);
					}

					// try fallback to topname
					if(te == null)
					{
						te = top_list.getTop(top);
					}

					// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
					if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
					{
						String tmpTop = top.substring(0, top.indexOf(" |"));
						te = top_list.getTop(tmpTop);
					}

					String mietvertragsnummer = (String)ht.get("mietvertragzuordnung");
					if(mietvertragsnummer == null || mietvertragsnummer.length() == 0)
					{
						// Zuordnung ueber MietvertragsID
						mietvertragsnummer = (String)ht.get("vertragid");
					}
					if(mietvertragsnummer == null)
					{
						mietvertragsnummer = "";
					}

					if(mietvertragsnummer.contains(","))
					{
						mietvertragsnummer = mietvertragsnummer.substring(0, mietvertragsnummer.indexOf(","));
					}

					// System.out.println("Mietvertragsnummer: " + mietvertragsnummer);

					if(parentObject.zinslistenImport.getZlTypeConfig().isErroronemptyvertragsid() && mietvertragsnummer.equals(""))
					{
						zl.addError(Tr.t("textNoContractNumber", session.getString("language")) + " " + top, mietvertragsnummer, ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, top);
						continue;
					}

					String mietvertragsId = "";
					String topsSlot = "";
					String vertragsnfl = "";
					String vertragsname = "";
					String mieterfirma = "";

					// checken ob mietvertragsnummer vorkommt
					boolean mietvertragExists = false;
					for(int k = 0; k < res1.size(); k++)
					{
						Hashtable queryResult = res1.get(k);

						String vertragsId = (String)queryResult.get("vertragid");
						if(vertragsId.equals(mietvertragsnummer) && mietvertragsnummer.length() > 0)
						{
							mietvertragsId = (String)queryResult.get("ID");
							topsSlot = (String)queryResult.get("tops");
							vertragsnfl = (String)queryResult.get("nfl");
							vertragsname = (String)queryResult.get("vertragsname");
							mieterfirma = (String)queryResult.get("mieterfirma");

							mietvertragExists = true;
							break;
						}
					}

					// if Mietvertrag does not exists
					if(parentObject.zinslistenImport.getZlTypeConfig().isErroronnewvertragsid() && parentObject.zinslistenImport.getZlTypeConfig().isCreateonnewvertragsid() == false && mietvertragExists == false)
					{
						// Fehler nur ausgeben wenn kein neuer Mietvertrag angelegt wird
						zl.addError(Tr.t("textNoContractWithContractNumber1", session.getString("language")) + " " + mietvertragsnummer + " " + Tr.t("textNoContractWithContractNumber2", session.getString("language")) + " " + top, mietvertragsnummer, ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, top);
					}

					if(!fehlerabfrage)
					{

						// if Mietvertrag does not exists - create new one
						// PKO - 20150414 Keinen Mietvertrag fuer eine Leerstehung anlegen
						String mieter = ht.get("mieter").toString().toLowerCase();
						if(parentObject.zinslistenImport.getZlTypeConfig().isCreateonnewvertragsid() && mietvertragExists == false && (!(parentObject.checkLeerstandString(mieter))))
						{
							// Neuen Mietvertrag anlegen und neue Id in mietvertragsId schreiben

							if(parentObject.PBInst == null)
							{
								Connector conn = null;
								conn = new Connector();
								parentObject.PBInst = conn.getPageBuilder();
							}

							String tcode = parentObject.PBInst.readTemplate("CIMS.mietvertrag");

							DynGenDataObj newMietvertragDgd = new DynGenDataObj();
							if(null == parentObject.DAInst)
							{
								net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
								parentObject.DAInst = conn.getDataAgent();
							}
							newMietvertragDgd.DAInst = parentObject.DAInst;
							// build it with templatecode
							newMietvertragDgd.init(tcode, global, session);
							newMietvertragDgd.set("var.vertragid", mietvertragsnummer);

							// Store Mietvertrag
							mietvertragsId = parentObject.DAInst.storeObject(newMietvertragDgd, "CIMS.mietvertrag", null, session);
							if(mietvertragsId == null)
							{
								mietvertragsId = "";
							}
							else
							{
								Hashtable<String, String> h = new HashMap<String, String>();
								h.put("ID", mietvertragsId);
								h.put("vertragid", mietvertragsnummer);
								h.put("tops", "");
								res1.add(h);
							}

						}

						// top zu mietvertrag zuordnen
						if(mietvertragsId.length() > 0 && te != null)
						{
							if(topidsZuMietvertrag.containsKey(mietvertragsId))
							{
								// Vector tops = topidsZuMietvertrag.get(mietvertragsId);
								MietvertragElement mvElement = topidsZuMietvertrag.get(mietvertragsId);
								Vector newTops = mvElement.getNewTops();
								newTops.add(te.id);
								mvElement.addTopElement(te);
								mvElement.setId(mietvertragsId);
								mvElement.setNewTops(newTops);
								mvElement.setExistingTops(topsSlot);
								mvElement.setMietvertragnfl(vertragsnfl);
								mvElement.setMietvertragsnummer(mietvertragsnummer);
								mvElement.setVertragsname(vertragsname);
								mvElement.setMieterfirma(mieterfirma);
								topidsZuMietvertrag.put(mietvertragsId, mvElement);
							}
							else
							{
								MietvertragElement mvElement = new MietvertragElement();
								Vector newTops = new ArrayList();
								newTops.add(te.id);
								mvElement.addTopElement(te);
								mvElement.setId(mietvertragsId);
								mvElement.setNewTops(newTops);
								mvElement.setExistingTops(topsSlot);
								mvElement.setMietvertragnfl(vertragsnfl);
								mvElement.setMietvertragsnummer(mietvertragsnummer);
								mvElement.setVertragsname(vertragsname);
								mvElement.setMieterfirma(mieterfirma);
								topidsZuMietvertrag.put(mietvertragsId, mvElement);
							}
						}

						// compare new with existing tops
						Enumeration enu = topidsZuMietvertrag.keys();
						while(enu.hasMoreElements())
						{
							String mietvertragsid = (String)enu.nextElement();
							MietvertragElement mvElement = topidsZuMietvertrag.get(mietvertragsid);

							Vector newTops = mvElement.getNewTops();
							Vector existingTops = mvElement.getExistingTops();

							// compare the vectors
							if(newTops.equals(existingTops))
							{
								// if newTops and existingTops are equal do nothing
								topsToWrite.put(mietvertragsid, newTops);
							}
							else
							{
								// if newTops and existingTops are not equal write it (add to topsToWrite)
								topsToWrite.put(mietvertragsid, newTops);
							}
						}
					}
				}

				// mietvertraege schreiben
				Enumeration e = topsToWrite.keys();
				while(e.hasMoreElements())
				{
					// key=mietvertragsid
					String mietvertragsid = (String)e.nextElement();
					if(mietvertragsid == null)
					{
						continue;
					}

					if(null == parentObject.DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						parentObject.DAInst = conn.getDataAgent();
					}

					DynGenDataObj mietvertragDgd = (DynGenDataObj)parentObject.DAInst.getObject(mietvertragsid, "");
					Vector topids = topsToWrite.get(mietvertragsid);

					String[] tids = new String[topids.size()];
					topids.toArray(tids);

					// check Mietvertragsflaeche VS Topsflaeche
					Vector<Hashtable<String, String>> resSumTops = new Vector<Hashtable<String, String>>();
					Hashtable<String, Object> argsSumTops = new HashMap<String, Object>();
					argsSumTops.put("TType", "CIMS.top");
					argsSumTops.put("fieldClause", "sum(nfl) sumtopnfl");
					argsSumTops.put("ID", tids);
					if(mydom.length() == 0)
					{
						argsSumTops.put("DOMAIN", "ALLDOMAINS");
					}
					else
					{
						argsSumTops.put("DOMAIN", mydom);
					}

					QueryResult qrSumTops = parentObject.DAInst.queryObjectWithResult(argsSumTops);
					resSumTops = qrSumTops.getResult();

					// wenn flaechen nicht ident -> Mittelschweren fehler
					MietvertragElement mvElement = topidsZuMietvertrag.get(mietvertragsid);

					for(int l = 0; l < resSumTops.size(); l++)
					{
						Hashtable queryResult = resSumTops.get(l);

						String topsnfl = (String)queryResult.get("sumtopnfl");
						if(!topsnfl.equals(mvElement.getMietvertragnfl()))
						{
							// Fehler wenn Vertragsflaeche und Topsflaechen nicht ueberein stimmen
							// zl.addError(Tr.t("textWrongNFL1", session.getString("language")) + " [" + mvElement.getMietvertragsnummer() + "] " + Tr.t("textWrongNFL2",
							// session.getString("language")), mvElement.getMietvertragsnummer(), ErrorInfo.MITTEL, ErrorInfo.EINTRAGSFEHLER, "");

							Vector newTops = mvElement.getNewTops();
							Vector existingTops = mvElement.getExistingTops();

							String topElements = "";
							String topUrls = "";
							for(TopElement te : mvElement.getTopElements())
							{
								topUrls = topUrls + CoolStringTool.buildLink(te.id, "SHOW", "", te.getName(), "", "_blank", "ajaxLink redlink", global, session);
								topUrls = topUrls + ", ";
							}

							if(topUrls.endsWith(", "))
							{
								topUrls = topUrls.substring(0, topUrls.length() - 2);
							}

							zl.addError(Tr.t("textWrongNFLinMV", session.getString("language"), Currency.formatDouble(Currency.makeDoubleFromCurrency(mvElement.getMietvertragnfl())), mvElement.getVertragsname(), mvElement.getMieterfirma(), mvElement.getMietvertragsnummer(), topUrls, Currency.formatDouble(Currency.makeDoubleFromCurrency(topsnfl))), "", ErrorInfo.MITTEL, ErrorInfo.EINTRAGSFEHLER, "");

							// textWrongNFLinMV=NFL {0} im Vertrag {1} ({2}) mit ID {3} und den zugeh\u00F6rigen Top {4} mit NFL {5} stimmt nicht \u00FCberein\!

						}
					}

					Slot tops = new Slot(tids);

					mietvertragDgd.set("slot.tops", tops);

					// Write all in zinslistenconfig defined Values to Mietvertrag
					mietvertragDgd = writeMietvertragsValues(mietvertragDgd, zl);

					parentObject.DAInst.storeObject(mietvertragDgd, "CIMS.mietvertrag", mietvertragsid, session);
				}
			}

			catch(Exception e)
			{
				debug.log(e);
				debug.error("Konnte Mietvertragsverknuepfung nicht anlagen " + e.getStackTrace());
			}
		}
		return zl;
	}

	/**
	 * holen einer zinsliste mit index.
	 *
	 * @param file
	 *            the file
	 * @param index
	 *            the index
	 * @return the zinsliste
	 */

		public String createHaus(Zinsliste zl)
	{
		// create group object
		try
		{

			if(parentObject.PBInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				parentObject.PBInst = conn.getPageBuilder();
			}

			String tcode = parentObject.PBInst.readTemplate("CIMS.haus");

			DynGenDataObj dgd = new DynGenDataObj();
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			dgd.DAInst = parentObject.DAInst;
			// build it with templatecode
			dgd.init(tcode, global, session);
			dgd.set("var.name", zl.haus);
			dgd.set("var.plz", zl.plz);
			dgd.set("var.ort", zl.ort);

			String zlhausverwalter = zl.getHausverwalter();
			if(null == zlhausverwalter || zlhausverwalter.trim().equals(""))
			{
				dgd.set("var.hausverwalter", zlhausverwalter);
			}

			// CHECK USER LAND VALUE
			String land = "";
			if(parentObject.userland.length() > 0)
			{
				land = parentObject.userland;
			}
			else
			{
				land = (String)parentObject.get("var.land");
				if(null == land)
				{
					land = "";
				}
			}

			if(land.length() > 0)
			{
				dgd.set("var.land", land);
			}

			String hausverwaltung = (String)parentObject.get("var.hausverwaltung");
			if(null != hausverwaltung)
			{
				dgd.set("var.hausverwaltung", hausverwaltung);
			}

			String betreuer = (String)parentObject.get("var.betreuer");
			if(null != betreuer)
			{
				dgd.set("var.betreuer", betreuer);
			}

			Slot gschaft = (Slot)parentObject.get("slot.gschaft");
			if(null != gschaft)
			{
				dgd.set("slot.gschaft", gschaft);
			}

			Slot gfeld = (Slot)parentObject.get("slot.gfeld");
			if(null != gfeld)
			{
				dgd.set("slot.gfeld", gfeld);
			}

			if(zl.getEdvNr() != null && zl.getHausverwaltung() != null && zl.getHausverwaltung().length() > 2)
			{
				String edvnummer = zl.getHausverwaltung().substring(0, 3) + zl.getEdvNr();
				dgd.set("var.identadresse5", edvnummer);
			}

			// dgd.set("var.domainid","1");
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			String id = parentObject.DAInst.storeObject(dgd, "CIMS.haus", null, session);
			return id;
		}
		catch(Exception e)
		{
			debug.error(e);
			debug.log(e);
			return null;
		}
	}

		public String updateHaus(Zinsliste zl, String oid)
	{
		Hashtable<String, String> additionalFields = zl.getAdditionalFields();
		if(additionalFields == null || additionalFields.size() == 0)
		{
			return oid;
		}

		try
		{
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			if(oid == null)
			{
				oid = "";
			}
			else
			{
				DynGenDataObj hausDGD = (DynGenDataObj)parentObject.DAInst.getObject(oid, null);
				String templateType = (String)hausDGD.get("TEMPLATETYPE");
				if(!templateType.contains("CIMS.haus"))
				{
					oid = "";
				}

				for(String key : additionalFields.keySet())
				{
					if(key.contains("var."))
					{
						hausDGD.set(key, additionalFields.get(key));
					}
					else
					{
						hausDGD.set("var." + key, additionalFields.get(key));
					}
				}
				String id = parentObject.DAInst.storeObject(hausDGD, "CIMS.haus", oid, session);
			}
		}
		catch(Exception ex)
		{
			debug.error("Error in updateHaus of UploadXLS4:");
			debug.log(ex);
			oid = "";
		}
		return oid;
	}

		public String getTopOID(String name, String hausid)
	{
		Hashtable args = new HashMap();
		Vector res = new ArrayList();
		args.put("TType", "CIMS.top");
		// fieldClause ... Felder zum holen ,-separiert
		args.put("fieldClause", "ID,name");
		args.put("REVtops_ID", hausid);
		args.put("name", name);

		// BAUSTELLE
		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			args.put("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			args.put("DOMAIN", mydom);
		}
		// new Connector Class
		if(null == parentObject.DAInst)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			parentObject.DAInst = conn.getDataAgent();
		}
		java.util.Date start_time = new java.util.Date();

		try
		{
			res = parentObject.DAInst.queryObject(args);
		}
		catch(Exception x)
		{

		}
		java.util.Date end_time = new java.util.Date();
		long run_time = end_time.getTime() - start_time.getTime();

		if(res.size() > 0)
		{
			Hashtable h = (Hashtable)res.elementAt(0);
			if(h != null)
			{
				return (String)h.get("ID");
			}
		}
		return null;
	}

		public DynGenDataObj createTop(Hashtable ht, String oid_haus, Zinsliste zl)
	{
		return createTopOrStellplatz(ht, parentObject.oid_haus, true, zl);
	}

		public DynGenDataObj createStellplatz(Hashtable ht, String oid_haus, Zinsliste zl)
	{
		return createTopOrStellplatz(ht, parentObject.oid_haus, false, zl);
	}

		public DynGenDataObj createTopOrStellplatz(Hashtable ht, String oid_haus, boolean is_a_top, Zinsliste zl)
	{

		try
		{

			// If isImportmieterwechsel==true and it is not the main (actual) tenant do not create a rental unit but only a rent-roll line
			if(zl.isImportmieterwechsel())
			{
				if(ht.containsKey("mieterwechsel") && ht.get("mieterwechsel").equals("1"))
				{
					return null;
				}
			}

			if(parentObject.PBInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				parentObject.PBInst = conn.getPageBuilder();
			}

			DynGenDataObj dgdTop = TemplateReader.getInstance().getDGDForTemplate("CIMS.top", global, session, true);
			if(is_a_top)
			{
				// das ist kein stellplatz!
				dgdTop.set("var.stellplatz", "0");
				dgdTop.set("var.name", ht.get("top"));
			}
			else
			{
				// das ist ein stellplatz!
				dgdTop.set("var.stellplatz", "1");
				dgdTop.set("var.name", ht.get("top"));
			}

			dgdTop = writeCommonValues(ht, dgdTop, zl);

			if(zl.isModifySollMiete())
			{
				dgdTop.set("var.ismodifysollmiete", "1");
			}
			else
			{
				dgdTop.set("var.ismodifysollmiete", "0");
			}

			if(zl.isModifyZielMiete())
			{
				dgdTop.set("var.ismodifyzielmiete", "1");
			}
			else
			{
				dgdTop.set("var.ismodifyzielmiete", "0");
			}

			String status = (String)ht.get("status");
			if(null == status)
			{
				status = "";
			}
			if(status.equalsIgnoreCase("sold"))
			{
				dgdTop.set("var.status", "-1"); // verkauft
			}
			else if(status.equalsIgnoreCase("-3"))
			{
				dgdTop.set("var.status", "-3"); // zusammengelegt
			}

			// get sollmiete

			String Bemerkung = (String)ht.get("bemerkung");
			if(null != Bemerkung)
			{
				dgdTop.set("var.text", Bemerkung);
			}

			// PKO 20150806 - Set useraccess on newly created ZZ
			dgdTop.setBoolean("useraccesschange", true);

			String topName = (String)dgdTop.get("var.name");
			String newTopName = (String)ht.get("top");
			if(newTopName != null && topName != null && !newTopName.trim().equals(""))
			{
				TopData topData = TopUtil.getTopData(newTopName);
				TopData oldTopData = TopUtil.getTopData(topName);

				// Wenn SAPNUMMER konfiguriert ist, dann soll der Topname nicht geändert werden!
				// PKO Falls SAPNUMMER konfiguriert ist, aber eine EDV Nr. im Namen dann die wegschneiden
				if(ht.containsKey("sapnummer") && topData.hasEdvNr())
				{
					String newTopname = topName.replace("|" + oldTopData.getEdvNr() + "|", "").trim();
					dgdTop.set("var.name", newTopname);
				}
				else if(topData.hasEdvNr() && !ht.containsKey("sapnummer"))
				{
					dgdTop.set("var.name", newTopName);
				}
			}

			return dgdTop;

		}
		catch(Exception e)
		{
			debug.error(e);
			// System.err.println("bad top/stellplatz:" + ht.toString());
			debug.log(e);
			debug.error(this, "bad top/stellplatz:" + ht.toString());
			return null;
		}
	}

		public DynGenDataObj updateTopOrStellplatz(String oid_top, Hashtable ht, Zinsliste zl, boolean is_a_top, CoolBulkStoreTool mycbst)
	{
		if(parentObject.enableDetailedLogging)
		{
			parentObject.endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateTopOrStellplatz 1: " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
			parentObject.starttime = System.currentTimeMillis();
		}

		boolean nameChanged = false;
		parentObject.xc.getXMLConfig("hausverwaltung", parentObject.zinslistenImport.getZlTypeConfig().getName() + "mieter");

		String md5old = "";
		DynGenDataObj dgdTop = null;
		if(parentObject.DAInst == null)
		{
			Connector conn = null;
			conn = new Connector();
			parentObject.DAInst = conn.getDataAgent();
		}

		try
		{
			String standjahr = "";
			String standmonat = "";

			// TEUER
			if(null == parentObject.topsCache)
			{
				parentObject.topsCache = new HashMap();
			}
			try
			{
				dgdTop = (DynGenDataObj)parentObject.topsCache.get(oid_top);
			}
			catch(Exception xx)
			{
				// topscache irgendwie kapputt?
				debug.error(this, "bad top im topscache: " + oid_top);
				dgdTop = null;
			}

			if(null == dgdTop)
			{
				// System.err.println("ZLU2: not in TopsCache " + oid_top);
				dgdTop = (DynGenDataObj)parentObject.DAInst.getObject(oid_top, "");
			}

			// use values from Zinszeile -> normal import

			String topName = (String)dgdTop.get("var.name");
			String newTopName = (String)ht.get("top");
			if(newTopName != null && topName != null && !newTopName.trim().equals(""))
			{
				TopData topData = TopUtil.getTopData(newTopName);
				TopData oldTopData = TopUtil.getTopData(topName);

				// Wenn SAPNUMMER konfiguriert ist, dann soll der Topname nicht geändert werden!
				// PKO Falls SAPNUMMER konfiguriert ist, aber eine EDV Nr. im Namen dann die wegschneiden
				if(ht.containsKey("sapnummer") && topData.hasEdvNr())
				{
					if(oldTopData != null)
					{
						String newTopname = topName.replace("|" + oldTopData.getEdvNr() + "|", "").trim();
						dgdTop.set("var.name", newTopname);
						nameChanged = true;
					}
				}
				else if(topData.hasEdvNr() && !ht.containsKey("sapnummer"))
				{
					dgdTop.set("var.name", newTopName);
					nameChanged = true;
				}
			}
			Hashtable h = dgdTop.exhale();
			String hs = h.toString();
			md5old = net.metamagix.essence.tools.md5sum.md5sum(hs);

			try
			{
				// CHECK ob die Topografie nicht ueberschrieben werden soll ????
				boolean topoanpassung = parentObject.getBoolean("var.topoanpassung", Boolean.TRUE);
				if(!topoanpassung)
				{
					return null;
				}
				// CHECK ob es letztstand ist????
				Calendar lastZZCal = null;
				if(oid_top != null && parentObject.lastZZ4Top != null)
				{
					lastZZCal = parentObject.lastZZ4Top.get(oid_top);
				}
				// DN 20171129: immer mit zinszeilen von DB vergleichen! Delete von Zinszeilen macht sonst zorres...
				if(null == lastZZCal)
				{
					standjahr = "0";
					standmonat = "0";
				}
				else
				{
					standjahr = "" + lastZZCal.get(Calendar.YEAR);
					standmonat = "" + (lastZZCal.get(Calendar.MONTH) + 1);
				}
				Integer isj = Integer.valueOf(standjahr);
				Integer ism = Integer.valueOf(standmonat);

				if(isj > 2100)
				{
					System.err.println("ZLU2: Zinsliste " + zl.plz + " " + zl.adresse + " " + zl.jahr + "/" + zl.monat + " aelter als Stand " + standjahr + "/" + standmonat + " in Top/Stellplatz -> ABER UNREALISTISCH DA ZU WEIT IN DER ZUKUNFT -> JAHR WIRD AUF 0 GESETZT UND DATEN WERDEN ALS AKTUELL IMPORTIERT!" + (String)ht.get("top") + "!");
					standjahr = "0";
					isj = 0;
				}

				Integer isjzl = Integer.valueOf(zl.jahr);
				Integer ismzl = Integer.valueOf(zl.monat);
				// alte zinsliste
				if(isjzl.intValue() < isj.intValue())
				{
					System.err.println("ZLU2: Zinsliste " + zl.plz + " " + zl.adresse + " " + zl.jahr + "/" + zl.monat + " aelter als Stand " + standjahr + "/" + standmonat + " in Top/Stellplatz " + (String)ht.get("top") + "!");
					return null;
				}
				if(isjzl.intValue() == isj.intValue() && ismzl.intValue() < ism.intValue())
				{
					System.err.println("ZLU2: Zinsliste " + zl.plz + " " + zl.adresse + " " + zl.jahr + "/" + zl.monat + " aelter als Stand " + standjahr + "/" + standmonat + " in Top/Stellplatz " + (String)ht.get("top") + "!");
					System.err.println("ZLU2: Zinsliste " + zl.jahr + "/" + zl.monat + " aelter als Stand " + standjahr + "/" + standmonat + "!");
					return null;
				}

			}
			catch(Exception x)
			{
				// updaten
			}

			String infoplus = "";
			if(zl.isModifySollMiete())
			{
				infoplus += ", mit Hebung Sollhauptmietzins ";
			}
			if(zl.isModifyZielMiete())
			{
				infoplus += ", mit Hebung Zielhauptmietzins ";
			}

			parentObject.log("Zinsliste " + zl.plz + " " + zl.adresse + " " + zl.jahr + "/" + zl.monat + " jünger als Stand " + standjahr + "/" + standmonat + " in Top/Stellplatz " + (String)ht.get("top") + " update" + infoplus);

			dgdTop.set("var.standjahr", zl.jahr);
			dgdTop.set("var.standmonat", zl.monat);

			if(zl.isModifySollMiete())
			{
				dgdTop.set("var.ismodifysollmiete", "1");
			}
			else
			{
				dgdTop.set("var.ismodifysollmiete", "0");
			}

			if(zl.isModifyZielMiete())
			{
				dgdTop.set("var.ismodifyzielmiete", "1");
			}
			else
			{
				dgdTop.set("var.ismodifyzielmiete", "0");
			}

			if(is_a_top)
			{
				// das ist ein stellplatz!
				dgdTop.set("var.stellplatz", "0");
			}
			else
			{
				// das ist kein stellplatz!
				dgdTop.set("var.stellplatz", "1");
			}

			// START JPI -- If Top has a rental start in the last 2 months and in the rent list it is a vacancy do not ovverride the top values
			boolean vermietungtopuebeschreibtzinsliste = parentObject.getBoolean("var.vermietungtopuebeschreibtzinsliste", Boolean.FALSE);
			String vermietungtopuebeschreibtzinslistemonate = (String)parentObject.get("var.vermietungtopuebeschreibtzinslistemonate");
			String vermietungtopuebeschreibtzinslisteaction = (String)parentObject.get("var.vermietungtopuebeschreibtzinslisteaction");

			String leerstehung = (String)ht.get("leerstehung");

			boolean error = false;

			Date mietbeginnDate = dgdTop.getDate("var.mietvertragvon");
			Date nowDate = null;

			int differenceInDays = 0;
			int pruefperiodeInDays = 0;
			int differenceZZDatumMinusNow = 0;

			if(leerstehung != null && vermietungtopuebeschreibtzinsliste && mietbeginnDate != null && leerstehung.equals("1"))
			{

				try
				{
					// wenn werte am top nicht von der zinszeile ueberschrieben werden sollen return dgd ohne werte zu veraendern

					long pruefperiode = Long.parseLong(vermietungtopuebeschreibtzinslistemonate);
					nowDate = new Date(System.currentTimeMillis());

					differenceInDays = (int)((nowDate.getTime() - mietbeginnDate.getTime()) / (1000 * 60 * 60 * 24));
					pruefperiodeInDays = (int)pruefperiode * 31;
				}
				catch(Exception e)
				{
					error = true;
				}

				if(!error && differenceInDays < pruefperiodeInDays)
				{
					return dgdTop;
				}
				else
				{
					// werte fuer top & zinsliste schreiben ...
					dgdTop = writeCommonValues(ht, dgdTop, zl);
				}
			}
			else
			{
				// werte fuer top & zinsliste schreiben ...
				dgdTop = writeCommonValues(ht, dgdTop, zl);
			}
			// END JPI

			// calculate $gesamtflaeche for EGB/SOM
			if(CfgSingleton.getInstance().getBoolean("CALCULATEEGBFLAECHEN", Boolean.FALSE))
			{
				FlaechenBerechnungen flBerechnungen = FlaechenBerechnungen.getFlavouredInstance(session);
				flBerechnungen.berechneTopOderZinszeilenFlaechen(dgdTop, session);
			}

			// modifiy the special vars from config
			dgdTop = Top.modifyVars(dgdTop, debug, parentObject.xc, session);

			dgdTop = Top.manipulateSollZielMiete(dgdTop, debug);

			try
			{
				// Variable greenlease auf mieterwechsel prüfen
				// ist der Wert gesetzt?
				if(CfgSingleton.getInstance().getBoolean("MIETERCHANGEEMPTYMIETERFIRMAUNDBRANCHE", Boolean.FALSE))
				{
					// PKO 20190411 - 11693-Ankermieter und Branche bei Übergang zu Leerstand leeren
					String mieterVorher = (String)dgdTop.get("var.mieterold");
					String mieterAktuell = (String)dgdTop.get("var.mieter");

					if(mieterVorher == null || mieterVorher.length() == 0)
					{
						mieterVorher = mieterAktuell;
						dgdTop.set("var.mieterold", mieterAktuell);
					}
					// Mieterwechsel
					boolean mieterwechsel = false;

					if(!mieterAktuell.equalsIgnoreCase(mieterVorher))
					{
						if(mieterAktuell.equals("Leerstehung"))
						{
							mieterwechsel = true;
						}
						else if(mieterVorher.equals("Leerstehung"))
						{
							// kein Wechsel, Neuvermietung
							dgdTop.set("var.mieterold", mieterAktuell);
						}
						else if(StringUtils.getLevenshteinDistance(mieterVorher.toLowerCase(), mieterAktuell.toLowerCase()) > 1)
						{
							mieterwechsel = true;
						}
						if(mieterwechsel)
						{
							dgdTop.set("var.mieterold", mieterAktuell);
							dgdTop.set("var.greenlease", "-1");
						}
					}
				}
			}
			catch(

			Exception xx)
			{
				debug.log(xx);
			}

		}

		catch(Exception e)
		{
			debug.error(e);
			debug.log(e);
			return null;
		}

		if(parentObject.DAInst == null)
		{
			Connector conn = null;
			conn = new Connector();
			parentObject.DAInst = conn.getDataAgent();
		}

		// IN STORE THREAD ADDEN - WENN ES NEU IST!!!!
		Hashtable h = dgdTop.exhale();
		String hs = h.toString();
		String md5new = net.metamagix.essence.tools.md5sum.md5sum(hs);

		if(!md5new.equals(md5old) || nameChanged)
		{
			mycbst.addObject(oid_top, dgdTop);
		}

		if(parentObject.enableDetailedLogging)
		{
			parentObject.endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateTopOrStellplatz 2: " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
			parentObject.starttime = System.currentTimeMillis();
		}

		return dgdTop;
	}

		public boolean addTopToHaus(String oid_top, String oid_haus)
	{
		try
		{
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			DynGenDataObj h = (DynGenDataObj)parentObject.DAInst.getObject(parentObject.oid_haus, "");
			Slot tops = (Slot)h.get("slot.tops");
			tops.add(oid_top);
			h.set("slot.tops", tops);
			String id = parentObject.DAInst.storeObject(h, "CIMS.haus", parentObject.oid_haus, session);
			if(id.equals(parentObject.oid_haus))
			{
				return true;
			}
			return false;
		}
		catch(Exception c)
		{
			debug.error(this, "could not set haus slot for top");
			debug.error(c);
			return false;
		}

	}

		public boolean addTopsToHaus(Hashtable oids, String oid_haus)
	{
		try
		{
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			DynGenDataObj h = (DynGenDataObj)parentObject.DAInst.getObject(parentObject.oid_haus, "");
			Slot tops = (Slot)h.get("slot.tops");

			Enumeration e = oids.keys();
			while(e.hasMoreElements())
			{
				String toid = (String)e.nextElement();
				if(null != toid)
				{
					;
				}
				tops.add(toid);
			}
			h.set("slot.tops", tops);
			String id = parentObject.DAInst.storeObject(h, "CIMS.haus", parentObject.oid_haus, session);
			if(null == id)
			{
				parentObject.log("Konnte Haus (" + parentObject.oid_haus + ") nicht speichern!");
				debug.error(this, "Konnte Haus (" + parentObject.oid_haus + ") nicht speichern!");
				return false;
			}
			if(id.equals(parentObject.oid_haus))
			{
				return true;
			}
			return false;
		}
		catch(Exception c)
		{
			c.printStackTrace();
			debug.error(this, "could not set top slot for haus");
			debug.log(c);
			return false;
		}

	}

		public boolean addTopsToGebaeude(Hashtable topszugebaeuden)
	{
		try
		{
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}

			Enumeration gebaeude = topszugebaeuden.keys();
			while(gebaeude.hasMoreElements())
			{
				String oid_gebaeude = (String)gebaeude.nextElement();

				DynGenDataObj h = (DynGenDataObj)parentObject.DAInst.getObject(oid_gebaeude, "");
				Slot gtops = (Slot)h.get("slot.gtops");

				Hashtable oids = (Hashtable)topszugebaeuden.get(oid_gebaeude);
				Enumeration e = oids.keys();
				while(e.hasMoreElements())
				{
					String gtoid = (String)e.nextElement();
					if(null != gtoid)
					{
						gtops.add(gtoid);
					}
				}
				h.set("slot.gtops", gtops);
				String id = parentObject.DAInst.storeObject(h, "CIMS.gebaeude", oid_gebaeude, session);
				if(null == id)
				{
					parentObject.log("Konnte Gebaeude (" + oid_gebaeude + ") nicht speichern!");
					debug.error(this, "Konnte Gebaeude (" + oid_gebaeude + ") nicht speichern!");
				}
			}
			return true;
		}
		catch(Exception c)
		{
			c.printStackTrace();
			debug.error(this, "could not set gtop slot for gebaeude");
			debug.log(c);
			return false;
		}

	}

		public DynGenDataObj createZZ(Hashtable ht, Zinsliste zl, String oid_top, String zz_oid)
	{
		java.util.Date zzdatum = null;

		// create zinszeilen object
		try
		{
			DynGenDataObj topdgd = null;
			if(null == parentObject.topsCache)
			{
				parentObject.topsCache = new HashMap();
			}
			try
			{
				topdgd = (DynGenDataObj)parentObject.topsCache.get(oid_top);
			}
			catch(Exception xx)
			{
				// topscache irgendwie kapputt?
				debug.error(this, "bad top im topscache: " + oid_top);
				topdgd = null;
			}

			if(null == topdgd)
			{
				// System.err.println("ZLU2: not in TopsCache " + oid_top);
				topdgd = (DynGenDataObj)parentObject.DAInst.getObject(oid_top, "");
			}

			if(parentObject.PBInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				parentObject.PBInst = conn.getPageBuilder();
			}

			boolean createNewZZ = true;
			DynGenDataObj oldzzdgd = null;
			if(zz_oid != null && parentObject.zinsZeilenCache.containsKey(zz_oid))
			{
				oldzzdgd = (DynGenDataObj)parentObject.zinsZeilenCache.get(zz_oid);
			}
			if(oldzzdgd != null && !getBoolean("var.altezinszeilenloeschen", Boolean.FALSE))
			{
				Date topDatum = topdgd.getDate("var.datum");
				Date zzDatum = oldzzdgd.getDate("var.datum");
				if(zzDatum != null && topDatum != null && (zzDatum.before(topDatum)))
				{
					createNewZZ = false;
				}
			}

			DynGenDataObj dgdZZ = null;
			if(createNewZZ)
			{
				TemplateReader tr = TemplateReader.getInstance();
				dgdZZ = tr.getDGDForTemplate("CIMS.zinszeile", global, session);
			}
			else
			{
				dgdZZ = oldzzdgd;
			}
			// String tcode = parentObject.PBInst.readTemplate("CIMS.zinszeile");
			// if(null == tcode) System.err.println("ZLU2: CIMS.zinszeile code is null!");

			// DynGenDataObj dgd = new DynGenDataObj();
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			dgdZZ.DAInst = parentObject.DAInst;
			// build it with templatecode
			// dgd.init(tcode, global, session);

			String hs = ht.toString();
			String md5hash = net.metamagix.essence.tools.md5sum.md5sum(hs);
			dgdZZ.set("var.md5hash", md5hash);

			// Wenn SAPNUMMER konfiguriert ist, dann soll der Name fuer die ZZ vom Top kommen
			if(null != ht.get("top") && !ht.containsKey("sapnummer"))
			{
				dgdZZ.set("var.name", ht.get("top"));
			}
			else
			{
				dgdZZ.set("var.name", topdgd.get("var.name"));
			}
			if(null != zl.monat)
			{
				dgdZZ.set("var.monat", zl.monat);
			}
			if(null != zl.jahr)
			{
				dgdZZ.set("var.jahr", zl.jahr);
			}

			// datum schreiben
			try
			{
				SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
				String monat = zl.getMonat();
				if(monat.length() == 1)
				{
					monat = "0" + monat;
				}
				zzdatum = dateFormat.parse("01." + monat + "." + zl.getJahr());
				if(null != zzdatum)
				{
					// System.out.println("ZZDatum " + zl.jahr + "/" + zl.monat + " = " + zzdatum.toString());
					String zzdatestr = net.metamagix.essence.eSSENCETypes.eDate.stringFromDate(zzdatum);
					dgdZZ.set("var.datum", zzdatestr);
				}
			}
			catch(Exception xxc)
			{
				debug.log(xxc);
			}

			if(CfgSingleton.getInstance().performSynchronizeZZFromTop())
			{
				// Write Defined Top Values to ZZ before setting values from Import
				dgdZZ = Top.synchronizeZZFromTop(topdgd, dgdZZ, parentObject.oid_haus, session, parentObject.DAInst, debug);
			}

			try
			{
				// werte fuer top & zinsliste schreiben ...

				// START Vermietung ueberschreibt Top wenn aktiviert -- If Top has a rental start in the check period months and in the rent list it is a vacancy do not ovverride the top values

				String vermietungtopuebeschreibtzinsliste = (String)parentObject.get("var.vermietungtopuebeschreibtzinsliste");
				String vermietungtopuebeschreibtzinslistemonate = (String)parentObject.get("var.vermietungtopuebeschreibtzinslistemonate");
				String vermietungtopuebeschreibtzinslisteaction = (String)parentObject.get("var.vermietungtopuebeschreibtzinslisteaction");

				Date mietbeginnDate = topdgd.getDate("var.mietvertragvon");
				String leerstehung = (String)ht.get("leerstehung");

				boolean error = false;

				Date nowDate = null;
				int differenceInDays = 0;
				int pruefperiodeInDays = 0;
				int differenceZZDatumMinusNow = 0;

				// default werte aus Top uebernehmen
				String pm_enforcetoptozzvalues = CfgSingleton.getInstance().getString("PM_ENFORCETOPTOZZVALUES").trim();
				String[] propagationValues = CoolStringTool.splitFast(pm_enforcetoptozzvalues, "\\|");
				if(null == propagationValues || propagationValues.length == 0)
				{
					propagationValues = new String[]{
						"anvermietung",
						"status",
						"eigenfremdnutzung",
						"budgetrelevant",
						"propertymanagementaccounting",
						"slot.vermieterfirma",
						"vermieterkostenstelle",
						"slot.mieterfirma",
						"mieterkostenstelle",
						"vertragsflaeche",
						"vertragsmietepm",
						"planfl",
						"marktmietepm",
						"vertragsmietepm",
						"freifinanziert"};
				}
				CoolDataTool.propagateValues(topdgd, dgdZZ, propagationValues, propagationValues, false);

				// Gibts nur für JP. Macht me keinen SINN
				if(leerstehung != null && vermietungtopuebeschreibtzinsliste != null && mietbeginnDate != null && vermietungtopuebeschreibtzinsliste.equals("1") && leerstehung.equals("1"))
				{
					try
					{
						long pruefperiode = Long.parseLong(vermietungtopuebeschreibtzinslistemonate);
						nowDate = new Date(System.currentTimeMillis());

						differenceInDays = (int)((nowDate.getTime() - mietbeginnDate.getTime()) / (1000 * 60 * 60 * 24));
						differenceZZDatumMinusNow = (int)((nowDate.getTime() - zzdatum.getTime()) / (1000 * 60 * 60 * 24));

						pruefperiodeInDays = (int)pruefperiode * 31;

						// nowMinusPruefperiode = new Date(System.currentTimeMillis() - (24L * 60L * 60L * 1000L * 31L * pruefperiode));
					}
					catch(Exception e)
					{
						error = true;
					}

					// if(!error && mietbeginnDate.after(nowMinusPruefperiode) && zzdatum.after(nowMinusPruefperiode))
					if(!error && differenceInDays < pruefperiodeInDays && differenceZZDatumMinusNow < pruefperiodeInDays)
					{
						// write the top values to zinszeile
						writeCommonValues(topdgd, dgdZZ, zl);

						String topname = (String)topdgd.get("var.name");
						String mieter = (String)topdgd.get("var.mieter");
						String istmietepm = (String)topdgd.get("var.istmietepm");

						if(vermietungtopuebeschreibtzinslisteaction.equals("1"))
						{
							zl.addError(Tr.t("textRentRollVacancy", session.getString("language")), mieter + ": " + istmietepm, ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, topname);
						}
					}
					else
					{
						// werte fuer top & zinsliste schreiben ...
						dgdZZ = writeCommonValues(ht, dgdZZ, zl);
					}
				}
				else
				{
					if(leerstehung != null && mietbeginnDate != null && leerstehung.equals("1"))
					{
						try
						{
							long pruefperiode = Long.parseLong(vermietungtopuebeschreibtzinslistemonate);
							// nowMinusPruefperiode = new Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L * 31L * pruefperiode);
						}
						catch(Exception e)
						{
							error = true;
						}

						if(!error && differenceInDays < pruefperiodeInDays && differenceZZDatumMinusNow < pruefperiodeInDays && vermietungtopuebeschreibtzinslisteaction.equals("1"))
						{
							String topname = (String)topdgd.get("var.name");
							String mieter = (String)topdgd.get("var.mieter");
							String istmietepm = (String)topdgd.get("var.istmietepm");

							zl.addError(Tr.t("textRentRollVacancy", session.getString("language")), mieter + ": " + istmietepm, ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, topname);
						}
					}

					// werte fuer top & zinsliste schreiben ...
					dgdZZ = writeCommonValues(ht, dgdZZ, zl);

					// Nutzung aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
					// wir nehmen die nutzung aus dem top? wenn sie befüllt ist
					String topdgdNutzung = topdgd.getString("var.nutzung");
					if(parentObject.zinslistenImport.getZlTypeConfig().isOverrideNutzung() && (topdgdNutzung == null || topdgdNutzung.length() > 0))
					{
						dgdZZ.set("var.nutzung", topdgdNutzung);
					}

				} // END Vermietung ueberschreibt Top wenn aktiviert
			}
			catch(Exception xxc)
			{
				debug.log(xxc);
			}

			// USTSATZ + UST
			// siehe RM Ticket 1710: ACHTUNG: ht.get("ustsatz1") beinhaltet dem wert der Umsatzsteuer und nicht den Prozentsatz!
			if(ht.get("ustsatz1") != null && ht.containsKey("ustwert1") && ht.get("ustwert1") != null)
			{
				dgdZZ.set("var.ustwert1", ht.get("ustwert1"));
				dgdZZ.set("var.ustsatz1", ht.get("ustsatz1"));
			}
			else if(ht.get("ustsatz1") != null)
			{
				dgdZZ.set("var.ustwert1", ht.get("ustsatz1"));
			}
			dgdZZ.set("var.ustsatz1", zl.ustsatz1);

			if(ht.get("ustsatz2") != null && ht.containsKey("ustwert2") && ht.get("ustwert2") != null)
			{
				dgdZZ.set("var.ustsatz2", ht.get("ustsatz2"));
				dgdZZ.set("var.ustwert2", ht.get("ustwert2"));
			}
			else if(ht.get("ustsatz2") != null)
			{
				dgdZZ.set("var.ustwert2", ht.get("ustsatz2"));
			}
			dgdZZ.set("var.ustsatz2", zl.ustsatz2);

			Integer errorpos = (Integer)ht.get("errorpos");
			if(null != errorpos)
			{
				// PKO 20171107 - Was ist das fuerr ein Kaese?
				// dgd.set("var.status", "-1");
				dgdZZ.set("var.statustext", zl.getError(errorpos.intValue()));
			}

			if(null != ht.get("bemerkung"))
			{
				dgdZZ.set("var.text", ht.get("bemerkung"));
			}

			String greenlease = topdgd.getString("var.greenlease");
			if(greenlease != null && greenlease.length() > 0)
			{
				dgdZZ.set("var.greenlease", greenlease);
			}
			Slot top = new Slot();
			top.add(oid_top);
			dgdZZ.set("slot.top", top);
			dgdZZ.set("var.topID", oid_top);

			if(zl.isModifySollMiete())
			{
				dgdZZ.set("var.ismodifysollmiete", "1");
			}
			else
			{
				dgdZZ.set("var.ismodifysollmiete", "0");
			}

			if(zl.isModifyZielMiete())
			{
				dgdZZ.set("var.ismodifyzielmiete", "1");
			}
			else
			{
				dgdZZ.set("var.ismodifyzielmiete", "0");
			}

			if(!parentObject.zlfile.startsWith("FILE_"))
			{
				parentObject.zlfile = "FILE_" + parentObject.zlfile;
			}

			dgdZZ.set("var.zlfile", parentObject.zlfile);
			dgdZZ.set("var.zlfileeigentuemer", parentObject.zlfile_e);

			// ueberpruefungen aus dem top - fixes / features
			// dgd = Top.modifyVars(dgd, debug);

			// System.out.println("BEFORE: TemplateType: " + dgd.getTemplateType() + " LS-art:" + dgd.getString("var.leerstehungsart") + " LS-Subart:" + dgd.getString("var.leerstehungssubart"));

			dgdZZ = Top.modifyVars(dgdZZ, debug, parentObject.xc, session);
			dgdZZ = Top.manipulateSollZielMiete(dgdZZ, debug);

			// System.out.println("AFTER: TemplateType: " + dgd.getTemplateType() + " LS-art:" + dgd.getString("var.leerstehungsart") + " LS-Subart:" + dgd.getString("var.leerstehungssubart"));

			// PKO 20150806 - Set useraccess on newly created ZZ
			dgdZZ.setBoolean("useraccesschange", true);

			return dgdZZ;
		}
		catch(Exception e)
		{
			debug.error(e);
			debug.log(e);
			return null;
		}
	}

		private DynGenDataObj writeMietvertragsValues(DynGenDataObj mietvertragDgd, Zinsliste zl)
	{
		String vertragidDGD = (String)mietvertragDgd.get("var.vertragid");

		for(int j = 0; j < zl.zinszeilen.size(); j++)
		{
			Hashtable ht = (Hashtable)zl.zinszeilen.get(j);

			String vertragidZinszeile = (String)ht.get("mietvertragzuordnung");
			if(vertragidZinszeile == null || vertragidZinszeile.length() == 0)
			{
				vertragidZinszeile = (String)ht.get("vertragid");
			}
			if(vertragidZinszeile == null)
			{
				vertragidZinszeile = "";
			}
			if(vertragidZinszeile.contains(","))
			{
				vertragidZinszeile = vertragidZinszeile.substring(0, vertragidZinszeile.indexOf(","));
			}

			if(vertragidDGD.equals(vertragidZinszeile))
			{

				Hashtable vars = mietvertragDgd.getSubs("var");

				for(Object key : vars.keySet())
				{
					String value = (String)ht.get(key);
					if(value == null || value.trim().length() == 0)
					{
						value = (String)ht.get(key + "sub");
					}
					if(value == null || value.trim().length() == 0)
					{
						value = (String)ht.get(key + "_sub");
					}
					if(value != null)
					{
						mietvertragDgd.set("var." + key, value);
					}
				}
			}
		}

		return mietvertragDgd;
	}

		public DynGenDataObj writeCommonValues(DynGenDataObj topdgd, DynGenDataObj zzdgd, Zinsliste zl)
	{
		ValueReplacement vr = new ValueReplacement(topdgd, session);
		parentObject.flavour = (String)session.get("parentObject.flavour");

		// System.out.println("TemplateType: " + topdgd.getTemplateType());
		// System.out.println("TemplateType: " + dgd.getTemplateType());

		// PKO - 20150331 - Map all existing values to variables - spezial handling afterwards
		Hashtable topvars = topdgd.getSubs("var");
		Hashtable zlvars = zzdgd.getSubs("var");

		for(Object key : topvars.keySet())
		{
			try
			{
				if(zlvars.containsKey(key))
				{
					String value = (String)topdgd.get("var." + key);
					if(value != null)
					{
						// pruefen ob selector oder wert
						String selector = zzdgd.getString("var." + key + ".SELECTOR");
						if(selector.length() > 0)
						{
							Hashtable opts = parentObject.getValueMap(selector);
							if(opts.containsKey(value))
							{
								value = (String)opts.get(value);
							}
						}
						if(key.equals("nutzung") && !(parentObject.zinslistenImport.getZlTypeConfig().isOverrideNutzung() && 0 < zzdgd.getString("var.nutzung").length()))
						{
							String nutzung = (String)topdgd.get("var.nutzung");
							if(null != nutzung)
							{
								zzdgd.set("var.nutzung", nutzung);
							}
						}
						else
						{
							zzdgd.set("var." + key, value);
						}
					}
				}
			}
			catch(Exception ex)
			{
				// Do nothing
				// debug.error(ex);
			}
		}

		// TODO Remove all unneccessary mappings !!!!!!!!!!!

		// Kategorie aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
		String kategorie = (String)topdgd.get("var.kategorie");
		if(null != kategorie)
		{
			zzdgd.set("var.kategorie", kategorie);
		}

		// String subnutzung = (String)topdgd.get("var.subnutzung");
		// if(null != subnutzung)
		// {
		// dgd.set("var.subnutzung", subnutzung);
		// }

		String kautionsart = (String)topdgd.get("var.kaution");
		if(null != kautionsart)
		{
			zzdgd.set("var.kaution", kautionsart);
		}

		String leistungsbeginn = (String)topdgd.get("var.leistungsbeginn");
		if(null != leistungsbeginn)
		{
			zzdgd.set("var.leistungsbeginn", leistungsbeginn);
		}

		String leistungsende = (String)topdgd.get("var.leistungsende");
		if(null != leistungsende)
		{
			zzdgd.set("var.leistungsende", leistungsende);
		}

		String kautionshoehe = (String)topdgd.get("var.kautionshoehe");
		if(null != kautionshoehe)
		{
			zzdgd.set("var.kautionshoehe", kautionshoehe);
		}
		addOriginalCurrencyValues("kautionshoehe", "kautionshoehe", zzdgd, topdgd);

		String kautionshoehesoll = (String)topdgd.get("var.kautionshoehesoll");
		if(null != kautionshoehesoll)
		{
			zzdgd.set("var.kautionshoehesoll", kautionshoehesoll);
		}
		addOriginalCurrencyValues("kautionshoehesoll", "kautionshoehesoll", zzdgd, topdgd);

		String kautionsdatum = (String)topdgd.get("var.kautionsdatum");
		if(null != kautionsdatum)
		{
			zzdgd.set("var.kautionsdatum", kautionsdatum);
		}

		String kautionsinfo = (String)topdgd.get("var.kautionsinfo");
		if(null != kautionsinfo)
		{
			zzdgd.set("var.kautionsinfo", kautionsinfo);
		}

		String mietsaldo = (String)topdgd.get("var.mietsaldo");
		if(null != mietsaldo)
		{
			zzdgd.set("var.mietsaldo", mietsaldo);
		}
		addOriginalCurrencyValues("mietsaldo", "mietsaldo", zzdgd, topdgd);

		// #16272 Mahnungen- Schnittstelle für ICRS

		String mahn3 = (String)topdgd.get("var.mahn3");
		if(null != mahn3)
		{
			zzdgd.set("var.mahn3", mahn3);
		}

		String mahn12 = (String)topdgd.get("var.mahn12");
		if(null != mahn12)
		{
			zzdgd.set("var.mahn12", mahn12);
		}

		String mahn24 = (String)topdgd.get("var.mahn24");
		if(null != mahn24)
		{
			zzdgd.set("var.mahn24", mahn24);
		}

		String mieteremail = (String)topdgd.get("var.mieteremail");
		if(null != mieteremail)
		{
			zzdgd.set("var.mieteremail", mieteremail);
		}

		String mietertelefon = (String)topdgd.get("var.mietertelefon");
		if(null != mietertelefon)
		{
			zzdgd.set("var.mietertelefon", mietertelefon);
		}

		String bewohner = (String)topdgd.get("var.bewohner");
		if(null != bewohner)
		{
			zzdgd.set("var.bewohner", bewohner);
		}

		String reparaturfond = (String)topdgd.get("var.reparaturfond");
		if(null == reparaturfond)
		{
			zzdgd.set("var.reparaturfond", reparaturfond);
		}
		addOriginalCurrencyValues("reparaturfond", "reparaturfond", zzdgd, topdgd);

		String mieteinheitenanz = (String)topdgd.get("var.mieteinheitenanz");
		if(null != mieteinheitenanz)
		{
			zzdgd.set("var.mieteinheitenanz", mieteinheitenanz);
		}

		String vertragid = (String)topdgd.get("var.vertragid");
		if(null != vertragid)
		{
			zzdgd.set("var.vertragid", vertragid);
		}

		String mieterid = (String)topdgd.get("var.mieterid");
		if(null != mieterid)
		{
			zzdgd.set("var.mieterid", mieterid);
		}

		String mietvertragvon = (String)topdgd.get("var.mietvertragvon");
		if(null != mietvertragvon)
		{
			zzdgd.set("var.mietvertragvon", mietvertragvon);
		}

		String mietvertragbis = (String)topdgd.get("var.mietvertragbis");
		if(null != mietvertragbis)
		{
			zzdgd.set("var.mietvertragbis", mietvertragbis);
		}

		String mietvertragbisvm = (String)topdgd.get("var.mietvertragbisvm");
		if(null != mietvertragbisvm)
		{
			zzdgd.set("var.mietvertragbisvm", mietvertragbisvm);
		}

		String istmietepmvm = (String)topdgd.get("var.istmietepmvm");
		if(null != istmietepmvm)
		{
			zzdgd.set("var.istmietepmvm", istmietepmvm);
		}
		addOriginalCurrencyValues("istmietepmvm", "istmietepmvm", zzdgd, topdgd);

		String vertragsmietepm = (String)topdgd.get("var.vertragsmietepm");
		if(null != vertragsmietepm)
		{
			zzdgd.set("var.vertragsmietepm", vertragsmietepm);
		}
		addOriginalCurrencyValues("vertragsmietepm", "vertragsmietepm", zzdgd, topdgd);

		String vertragsinfos = (String)topdgd.get("var.vertragsinfos");
		if(null != vertragsinfos)
		{
			zzdgd.set("var.vertragsinfos", vertragsinfos);
		}

		String vertragsoptionen = (String)topdgd.get("var.vertragsoptionen");
		if(null != vertragsoptionen)
		{
			zzdgd.set("var.vertragsoptionen", vertragsoptionen);
		}

		String verlaengerungsoptionart = (String)topdgd.get("var.verlaengerungsoptionart");
		if(null != verlaengerungsoptionart)
		{
			zzdgd.set("var.verlaengerungsoptionart", verlaengerungsoptionart);
		}

		String periodizitaet = (String)topdgd.get("var.periodizitaet");
		if(null != periodizitaet)
		{
			zzdgd.set("var.periodizitaet", periodizitaet);
		}

		String ustpflichtig = (String)topdgd.get("var.ustpflichtig");
		if(null != ustpflichtig)
		{
			zzdgd.set("var.ustpflichtig", ustpflichtig);
		}

		String zimmer = (String)topdgd.get("zimmer");
		if(null != zimmer)
		{
			zzdgd.set("var.zimmer", zimmer);
		}

		String bkabrechnungsum = (String)topdgd.get("var.bkabrechnungsum");
		if(null != bkabrechnungsum)
		{
			zzdgd.set("var.bkabrechnungsum", bkabrechnungsum);
		}
		addOriginalCurrencyValues("bkabrechnungsum", "bkabrechnungsum", zzdgd, topdgd);

		String mietzahlungvon = (String)topdgd.get("var.mietzahlungvon");
		if(null != mietzahlungvon)
		{
			zzdgd.set("var.mietzahlungvon", mietzahlungvon);
		}

		String indexanpassungdatum = (String)topdgd.get("var.indexanpassungsdatum");
		if(null != indexanpassungdatum)
		{
			zzdgd.set("var.indexanpassungsdatum", indexanpassungdatum);
		}

		String indexfrequenz = (String)topdgd.get("var.indexfrequenz");
		if(null != indexfrequenz)
		{
			zzdgd.set("var.indexfrequenz", indexfrequenz);
		}

		String mietfreiezeit = (String)topdgd.get("var.mietfreiezeit");
		if(null != mietfreiezeit)
		{
			zzdgd.set("var.mietfreiezeit", mietfreiezeit);
		}

		String mietfreibetrag = (String)topdgd.get("var.mietfreibetrag");
		if(null != mietfreibetrag)
		{
			zzdgd.set("var.mietfreibetrag", mietfreibetrag);
		}
		addOriginalCurrencyValues("mietfreibetrag", "mietfreibetrag", zzdgd, topdgd);

		String kuendigungsfrist = (String)topdgd.get("var.kuendigungsfrist");
		if(null != kuendigungsfrist)
		{
			zzdgd.set("var.kuendigungsfrist", kuendigungsfrist);
		}

		String sonderkuendigungsfrist = (String)topdgd.get("var.sonderkuendigungsfrist");
		if(null != sonderkuendigungsfrist)
		{
			zzdgd.set("var.sonderkuendigungsfrist", sonderkuendigungsfrist);
		}

		String kuendigungsdatum = (String)topdgd.get("var.kuendigungsdatum");
		if(null != kuendigungsdatum)
		{
			zzdgd.set("var.kuendigungsdatum", kuendigungsdatum);
		}

		String kuendigungsverzicht = (String)topdgd.get("var.kuendigungsverzicht");
		if(null != kuendigungsverzicht)
		{
			zzdgd.set("var.kuendigungsverzicht", kuendigungsverzicht);
		}

		String firstbodate = (String)topdgd.get("var.firstbodate");
		if(null != firstbodate)
		{
			zzdgd.set("var.firstbodate", firstbodate);
		}

		String sonderkuendigungsdatum = (String)topdgd.get("var.sonderkuendigungsdatum");
		if(null != sonderkuendigungsdatum)
		{
			zzdgd.set("var.sonderkuendigungsdatum", sonderkuendigungsdatum);
		}

		String verlaengerungsoption = (String)topdgd.get("var.verlaengerungsoption");
		if(null != verlaengerungsoption)
		{
			zzdgd.set("var.verlaengerungsoption", verlaengerungsoption);
		}

		String indexart = (String)topdgd.get("var.indexart");
		if(null != indexart)
		{
			zzdgd.set("var.indexart", indexart);
		}

		String zahlungsref = (String)topdgd.get("var.zahlungsref");
		if(null != zahlungsref)
		{
			zzdgd.set("var.zahlungsref", zahlungsref);
		}

		String indexdatum = (String)topdgd.get("var.indexdatum");
		if(null != indexdatum)
		{
			zzdgd.set("var.indexdatum", indexdatum);
		}

		String addonflproz = (String)topdgd.get("var.addonflproz");
		if(null != addonflproz)
		{
			zzdgd.set("var.addonflproz", addonflproz);
		}

		String ustwert = (String)topdgd.get("var.ustwert");
		if(null != ustwert)
		{
			zzdgd.set("var.ustwert", ustwert);
		}
		addOriginalCurrencyValues("ustwert", "ustwert", zzdgd, topdgd);

		String istmieteplusbkpm = (String)topdgd.get("var.istmieteplusbkpm");
		if(null != istmieteplusbkpm)
		{
			zzdgd.set("var.istmieteplusbkpm", istmieteplusbkpm);
		}
		addOriginalCurrencyValues("istmieteplusbkpm", "istmieteplusbkpm", zzdgd, topdgd);

		String istmieteplusbkplusustpm = (String)topdgd.get("var.istmieteplusbkplusustpm");
		if(null != istmieteplusbkplusustpm)
		{
			zzdgd.set("var.istmieteplusbkplusustpm", istmieteplusbkplusustpm);
		}
		addOriginalCurrencyValues("istmieteplusbkplusustpm", "istmieteplusbkplusustpm", zzdgd, topdgd);

		// |vpi2010|vpi2005|vpi2000|vpi96|vpi86|vpi76|vpi66|vpi1|vpi2|khpi|lhki45|lhki38|individuell1|individuell2|individuell3|individuell4|individuell5]
		// unbekannt|VPI 2010|VPI 2005|VPI 2000|VPI 96|VPI 86|VPI 76|VPI 66|VPI I|VPI II|khpi|LHKI 45|LHKI 38|Individueller Index 1|Individueller Index 2|Individueller Index 3|Individueller Index
		// 4|Individueller Index 5]
		// unknown|VPI 2010|VPI 2005|VPI 2000|VPI 96|VPI 86|VPI 76|VPI 66|VPI I|VPI II|khpi|LHKI 45|LHKI 38|Individual Index 1|Individual Index 2|Individual Index 3|Individual Index 4|Individual Index
		// 5]

		// Bei Fred nicht to Lowercase - fuer alle anderen lieber so belassen damit es keine Side Effects gibt.
		if(!parentObject.flavour.equals("icrsfred"))
		{
			String indexbasis = (String)topdgd.get("indexbasis");
			if(null != indexbasis)
			{
				zzdgd.set("var.indexbasis", indexbasis.toLowerCase());
			}
		}

		Hashtable indexArtSelectorValues = null;
		if(null != vr)
		{
			indexArtSelectorValues = vr.getValueMap("CIMS." + parentObject.flavour + ".IndexArtSelector");
			if(indexArtSelectorValues == null || indexArtSelectorValues.size() == 0)
			{
				indexArtSelectorValues = vr.getValueMap("CIMS.IndexArtSelector");
			}
		}
		String value = "";
		// String indexschwelle = (String)topdgd.get("indexschwelle");
		// if(null != indexschwelle)
		// {
		// Enumeration en = indexArtSelectorValues.keys();
		// while(en.hasMoreElements())
		// {
		// String htkey = (String)en.nextElement();
		// String htvalue = (String)indexArtSelectorValues.get(htkey);
		//
		// if(htvalue.equalsIgnoreCase(indexschwelle.toLowerCase()))
		// {
		// value = htkey;
		// break;
		// }
		// }
		//
		// if(value != null)
		// {
		// zzdgd.set("var.indexschwelle", value);
		// addOriginalCurrencyValues("indexschwelle", "indexschwelle", zzdgd, topdgd);
		// }
		// else
		// {
		// zzdgd.set("var.indexschwelle", "");
		// removeOriginalCurrencyValues("indexschwelle", zzdgd);
		// }
		// }

		String indexschwelleprozent = (String)topdgd.get("indexschwelleprozent");
		try
		{
			double idx = Double.parseDouble(indexschwelleprozent.replaceAll(",", ".")) * 100;
			indexschwelleprozent = (idx + "").replaceAll("[.]", ",");
		}
		catch(Exception ex)
		{}
		if(null != indexschwelleprozent)
		{
			zzdgd.set("var.indexschwelleprozent", indexschwelleprozent);
		}

		String erstmaligeindexierung = (String)topdgd.get("erstmaligeindexierung");
		if(null != erstmaligeindexierung)
		{
			zzdgd.set("var.erstmaligeindexierung", erstmaligeindexierung);
		}

		String indexversatz = (String)topdgd.get("indexversatz");
		if(null != indexversatz)
		{
			if(indexversatz.contains(","))
			{
				indexversatz = indexversatz.substring(0, indexversatz.indexOf(","));
			}
			zzdgd.set("var.indexversatz", indexversatz);
		}

		String indexweitergabe = (String)topdgd.get("indexweitergabe");
		if(null != indexweitergabe)
		{
			zzdgd.set("var.indexweitergabe", indexweitergabe);
		}

		String staffel1 = (String)topdgd.get("var.staffel1");
		if(null != staffel1)
		{
			zzdgd.set("var.staffel1", staffel1);
		}

		String staffelan1 = (String)topdgd.get("var.staffelan1");
		if(null != staffelan1)
		{
			zzdgd.set("var.staffelan1", staffelan1);
		}
		addOriginalCurrencyValues("staffelan1", "staffelan1", zzdgd, topdgd);

		String staffel2 = (String)topdgd.get("var.staffel2");
		if(null != staffel2)
		{
			zzdgd.set("var.staffel2", staffel2);
		}

		String staffelan2 = (String)topdgd.get("var.staffelan2");
		if(null != staffelan2)
		{
			zzdgd.set("var.staffelan2", staffelan2);
		}
		addOriginalCurrencyValues("staffelan2", "staffelan1", zzdgd, topdgd);

		String staffel3 = (String)topdgd.get("var.staffel3");
		if(null != staffel3)
		{
			zzdgd.set("var.staffel3", staffel3);
		}

		String staffelan3 = (String)topdgd.get("var.staffelan3");
		if(null != staffelan3)
		{
			zzdgd.set("var.staffelan3", staffelan3);
		}
		addOriginalCurrencyValues("staffelan3", "staffelan3", zzdgd, topdgd);

		String staffel4 = (String)topdgd.get("var.staffel4");
		if(null != staffel4)
		{
			zzdgd.set("var.staffel4", staffel4);
		}

		String staffelan4 = (String)topdgd.get("var.staffelan4");
		if(null != staffelan4)
		{
			zzdgd.set("var.staffelan4", staffelan4);
		}
		addOriginalCurrencyValues("staffelan4", "staffelan4", zzdgd, topdgd);

		String verlaengerungsoptionperiod = (String)topdgd.get("var.verlaengerungsoptionperiod");
		if(null != verlaengerungsoptionperiod)
		{
			zzdgd.set("var.verlaengerungsoptionperiod", verlaengerungsoptionperiod);
		}

		String ankuendigungsfrist = (String)topdgd.get("var.ankuendigungsfrist");
		if(null != ankuendigungsfrist)
		{
			zzdgd.set("var.ankuendigungsfrist", ankuendigungsfrist);
		}

		String nkpauschale = (String)topdgd.get("var.nkpauschale");
		if(null != nkpauschale)
		{
			zzdgd.set("var.nkpauschale", nkpauschale);
		}
		addOriginalCurrencyValues("nkpauschale", "nkpauschale", zzdgd, topdgd);

		String strompauschale = (String)topdgd.get("var.strompauschale");
		if(null != strompauschale)
		{
			zzdgd.set("var.strompauschale", strompauschale);
		}
		addOriginalCurrencyValues("strompauschale", "strompauschale", zzdgd, topdgd);

		String bkstrommietflaecheustsatz = (String)topdgd.get("bkstrommietflaecheustsatz");
		if(null != bkstrommietflaecheustsatz)
		{
			zzdgd.set("var.bkstrommietflaecheustsatz", getCorrectedUstSatz(bkstrommietflaecheustsatz, topdgd));
		}

		String bkreinigungustsatz = (String)topdgd.get("bkreinigungustsatz");
		if(null != bkreinigungustsatz)
		{
			zzdgd.set("var.bkreinigungustsatz", getCorrectedUstSatz(bkreinigungustsatz, topdgd));
		}

		String bkstromallgemeinustsatz = (String)topdgd.get("bkstromallgemeinustsatz");
		if(null != bkstromallgemeinustsatz)
		{
			zzdgd.set("var.bkstromallgemeinustsatz", getCorrectedUstSatz(bkstromallgemeinustsatz, topdgd));
		}

		String bkliftustsatz = (String)topdgd.get("bkliftustsatz");
		if(null != bkliftustsatz)
		{
			zzdgd.set("var.bkliftustsatz", getCorrectedUstSatz(bkliftustsatz, topdgd));
		}

		String bkdiverseustsatz = (String)topdgd.get("bkdiverseustsatz");
		if(null != bkdiverseustsatz)
		{
			zzdgd.set("var.bkdiverseustsatz", getCorrectedUstSatz(bkdiverseustsatz, topdgd));
		}

		String bkheizungustsatz = (String)topdgd.get("bkheizungustsatz");
		if(null != bkheizungustsatz)
		{
			zzdgd.set("var.bkheizungustsatz", getCorrectedUstSatz(bkheizungustsatz, topdgd));
		}

		String bksonderbetriebskostenustsatz = (String)topdgd.get("bksonderbetriebskostenustsatz");
		if(null != bksonderbetriebskostenustsatz)
		{
			zzdgd.set("var.bksonderbetriebskostenustsatz", getCorrectedUstSatz(bksonderbetriebskostenustsatz, topdgd));
		}

		String bkreparaturfondustsatz = (String)topdgd.get("bkreparaturfondustsatz");
		if(null != bkreparaturfondustsatz)
		{
			zzdgd.set("var.bkreparaturfondustsatz", getCorrectedUstSatz(bkreparaturfondustsatz, topdgd));
		}

		String reparaturfondustsatz = (String)topdgd.get("reparaturfondustsatz");
		if(null != reparaturfondustsatz)
		{
			zzdgd.set("var.reparaturfondustsatz", getCorrectedUstSatz(reparaturfondustsatz, zzdgd));
		}

		String bkvstustsatz = (String)topdgd.get("bkvstustsatz");
		if(null != bkvstustsatz)
		{
			zzdgd.set("var.bkvstustsatz", getCorrectedUstSatz(bkvstustsatz, topdgd));
		}

		String hmzvstustsatz = (String)topdgd.get("hmzvstustsatz");
		if(null != hmzvstustsatz)
		{
			zzdgd.set("var.hmzvstustsatz", getCorrectedUstSatz(hmzvstustsatz, topdgd));
		}

		String hmzdiverseustsatz = (String)topdgd.get("hmzdiverseustsatz");
		if(null != hmzdiverseustsatz)
		{
			zzdgd.set("var.hmzdiverseustsatz", getCorrectedUstSatz(hmzdiverseustsatz, topdgd));
		}

		String hmzfreiwustsatz = (String)topdgd.get("hmzfreiwustsatz");
		if(null != hmzfreiwustsatz)
		{
			zzdgd.set("var.hmzfreiwustsatz", getCorrectedUstSatz(hmzfreiwustsatz, topdgd));
		}

		String hmzsonstigeustsatz = (String)topdgd.get("hmzsonstigeustsatz");
		if(null != hmzsonstigeustsatz)
		{
			zzdgd.set("var.hmzsonstigeustsatz", getCorrectedUstSatz(hmzsonstigeustsatz, topdgd));
		}

		String ustsatz = (String)topdgd.get("ustsatz");
		if(null != ustsatz)
		{
			zzdgd.set("var.ustsatz", getCorrectedUstSatz(ustsatz, topdgd));
		}

		String ebene = (String)topdgd.get("var.ebene");
		if(null != ebene)
		{
			zzdgd.set("var.ebene", ebene);
		}

		String mietpreisbindung = (String)topdgd.get("var.mietpreisbindung");
		if(null != mietpreisbindung)
		{
			zzdgd.set("var.mietpreisbindung", mietpreisbindung);
		}

		String mietpreisbindungsart = (String)topdgd.get("var.mietpreisbindungsart");
		if(null != mietpreisbindungsart)
		{
			zzdgd.set("var.mietpreisbindungsart", mietpreisbindungsart);
		}

		// TODO gehört an Hand des Selectors - nicht HARDCODED!
		Hashtable anvermietungSelectorValues = null;
		if(null != vr)
		{
			anvermietungSelectorValues = vr.getValueMap("CIMS." + parentObject.flavour + ".AnVermietungTopSelector");
			if(anvermietungSelectorValues == null || anvermietungSelectorValues.size() == 0)
			{
				anvermietungSelectorValues = vr.getValueMap("CIMS.AnVermietungTopSelector");
			}
		}

		String anvermietung = (String)topdgd.get("anvermietung");
		value = "";
		if(null != anvermietung)
		{
			Enumeration en = anvermietungSelectorValues.keys();
			while(en.hasMoreElements())
			{
				String htkey = (String)en.nextElement();
				String htvalue = (String)anvermietungSelectorValues.get(htkey);

				if(htvalue.equalsIgnoreCase(anvermietung.toLowerCase()))
				{
					value = htkey;
					break;
				}
			}

			if(value != null)
			{
				zzdgd.set("var.anvermietung", value);
			}
			else
			{
				zzdgd.set("var.anvermietung", "");
			}
		}

		String geburtsjahr = (String)topdgd.get("var.mgeburtsjahr");
		if(null != geburtsjahr)
		{
			zzdgd.set("var.mgeburtsjahr", geburtsjahr);
		}

		String myval = (String)topdgd.get("var.hmzdiverse");
		if(null == myval)
		{
			zzdgd.set("var.hmzdiverse", myval);
		}
		addOriginalCurrencyValues("hmzdiverse", "hmzdiverse", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzindexnachverrechnung");
		if(null == myval)
		{
			zzdgd.set("var.hmzindexnachverrechnung", myval);
		}
		addOriginalCurrencyValues("hmzindexnachverrechnung", "hmzindexnachverrechnung", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzumsatz");
		if(null == myval)
		{
			zzdgd.set("var.hmzumsatz", myval);
		}
		addOriginalCurrencyValues("hmzumsatz", "hmzumsatz", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzumsatzanteil");
		if(null == myval)
		{
			zzdgd.set("var.hmzumsatzanteil", myval);
		}
		addOriginalCurrencyValues("hmzumsatzanteil", "hmzumsatzanteil", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzumsatzbasis");
		if(null == myval)
		{
			zzdgd.set("var.hmzumsatzbasis", myval);
		}
		addOriginalCurrencyValues("hmzumsatzbasis", "hmzumsatzbasis", zzdgd, topdgd);

		String hmzumsatzcurrency = (String)topdgd.get("var.hmzumsatzcurrency");
		if(null != hmzumsatzcurrency)
		{
			zzdgd.set("var.hmzumsatzcurrency", hmzumsatzcurrency);
		}

		myval = (String)topdgd.get("var.hmzpar18");
		if(null == myval)
		{
			zzdgd.set("var.hmzpar18", myval);
		}
		addOriginalCurrencyValues("hmzpar18", "hmzpar18", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzgarage");
		if(null == myval)
		{
			zzdgd.set("var.hmzgarage", myval);
		}
		addOriginalCurrencyValues("hmzgarage", "hmzgarage", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzzuschlag");
		if(null == myval)
		{
			zzdgd.set("var.hmzzuschlag", myval);
		}
		addOriginalCurrencyValues("hmzzuschlag", "hmzzuschlag", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzminderung");
		if(null == myval)
		{
			zzdgd.set("var.hmzminderung", myval);
		}
		addOriginalCurrencyValues("hmzminderung", "hmzminderung", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzsonstige");
		if(null == myval)
		{
			zzdgd.set("var.hmzsonstige", myval);
		}
		addOriginalCurrencyValues("hmzsonstige", "hmzsonstige", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzvst");
		if(null == myval)
		{
			zzdgd.set("var.hmzvst", myval);
		}
		addOriginalCurrencyValues("hmzvst", "hmzvst", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzfreiw");
		if(null == myval)
		{
			zzdgd.set("var.hmzfreiw", myval);
		}
		addOriginalCurrencyValues("hmzfreiw", "hmzfreiw", zzdgd, topdgd);

		myval = (String)topdgd.get("var.hmzmietfrei");
		if(null == myval)
		{
			zzdgd.set("var.hmzmietfrei", myval);
		}
		addOriginalCurrencyValues("hmzmietfrei", "hmzmietfrei", zzdgd, topdgd);

		myval = (String)topdgd.get("var.evb");
		if(null == myval)
		{
			zzdgd.set("var.evb", myval);
		}
		addOriginalCurrencyValues("evb", "evb", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bkdiverse");
		if(null == myval)
		{
			zzdgd.set("var.bkdiverse", myval);
		}
		addOriginalCurrencyValues("bkdiverse", "bkdiverse", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bklift");
		if(null == myval)
		{
			zzdgd.set("var.bklift", myval);
		}
		addOriginalCurrencyValues("bklift", "bklift", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bkgarage");
		if(null == myval)
		{
			zzdgd.set("var.bkgarage", myval);
		}
		addOriginalCurrencyValues("bkgarage", "bkgarage", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bkwarmwasser");
		if(null == myval)
		{
			zzdgd.set("var.bkwarmwasser", myval);
		}
		addOriginalCurrencyValues("bkwarmwasser", "bkwarmwasser", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bkheizung");
		if(null == myval)
		{
			zzdgd.set("var.bkheizung", myval);
		}
		addOriginalCurrencyValues("bkheizung", "bkheizung", zzdgd, topdgd);

		myval = (String)topdgd.get("var.bkklimaanlage");
		if(null == myval)
		{
			zzdgd.set("var.bkklimaanlage", myval);
		}
		addOriginalCurrencyValues("bkklimaanlage", "bkklimaanlage", zzdgd, topdgd);

		for(int i = 1; i <= 10; i++)
		{
			myval = (String)topdgd.get("var.bkposten" + i);
			if(null == myval)
			{
				zzdgd.set("var.bkposten" + i, myval);
			}
			addOriginalCurrencyValues("bkposten" + i, "bkposten" + i, zzdgd, topdgd);

			myval = (String)topdgd.get("var.hmzposten" + i);
			if(null == myval)
			{
				zzdgd.set("var.hmzposten" + i, myval);
			}
			addOriginalCurrencyValues("hmzposten" + i, "hmzposten" + i, zzdgd, topdgd);
		}

		String mieter = (String)topdgd.get("var.mieter");
		if(null != mieter)
		{
			zzdgd.set("var.mieter", mieter);
		}

		String vermieter = (String)topdgd.get("var.vermieter");
		if(null != vermieter)
		{
			zzdgd.set("var.vermieter", vermieter);
		}

		String nfl = (String)topdgd.get("var.nfl");
		if(null != nfl)
		{
			zzdgd.set("var.nfl", nfl);
		}

		String gesamtflaeche = (String)topdgd.get("var.gesamtflaeche");
		if(null != gesamtflaeche)
		{
			zzdgd.set("var.gesamtflaeche", gesamtflaeche);
		}

		String gesamtflaechebrutto = (String)topdgd.get("var.gesamtflaechebrutto");
		if(null != gesamtflaechebrutto)
		{
			zzdgd.set("var.gesamtflaechebrutto", gesamtflaechebrutto);
		}

		String kaufpreis = (String)topdgd.get("var.kaufpreis");
		if(null != kaufpreis)
		{
			zzdgd.set("var.kaufpreis", kaufpreis);
		}
		addOriginalCurrencyValues("kaufpreis", "kaufpreis", zzdgd, topdgd);

		String verkaufspreis = (String)topdgd.get("var.verkaufspreis");
		if(null != verkaufspreis)
		{
			zzdgd.set("var.verkaufspreis", verkaufspreis);
		}
		addOriginalCurrencyValues("verkaufspreis", "verkaufspreis", zzdgd, topdgd);

		String sollverkaufspreis = (String)topdgd.get("var.sollverkaufspreis");
		if(null != sollverkaufspreis)
		{
			zzdgd.set("var.sollverkaufspreis", sollverkaufspreis);
		}
		addOriginalCurrencyValues("sollverkaufspreis", "sollverkaufspreis", zzdgd, topdgd);

		String verkaufsdatum = (String)topdgd.get("var.verkaufsdatum");
		if(null != verkaufsdatum)
		{
			zzdgd.set("var.verkaufsdatum", verkaufsdatum);
		}

		String uebernahmedatum = (String)topdgd.get("var.uebernahmedatum");
		if(null != uebernahmedatum)
		{
			zzdgd.set("var.uebernahmedatum", uebernahmedatum);
		}

		String einkaufsdatum = (String)topdgd.get("var.einkaufsdatum");
		if(null != einkaufsdatum)
		{
			zzdgd.set("var.einkaufsdatum", einkaufsdatum);
		}

		String fertigstellungsdatum = (String)topdgd.get("var.fertigstellungsdatum");
		if(null != fertigstellungsdatum)
		{
			zzdgd.set("var.fertigstellungsdatum", fertigstellungsdatum);
		}

		String kaeufer = (String)topdgd.get("var.kaeufer");
		if(null != kaeufer)
		{
			zzdgd.set("var.kaeufer", kaeufer);
		}

		Hashtable statusSelectorValues = null;
		try
		{
			statusSelectorValues = parentObject.getValueMap("CIMS." + parentObject.flavour + ".TopStatusSelector");
			if(statusSelectorValues == null || statusSelectorValues.size() == 0)
			{
				statusSelectorValues = parentObject.getValueMap("CIMS.TopStatusSelector");
			}
		}
		catch(Exception ex)
		{}

		String status = (String)topdgd.get("var.status");
		value = "";
		if(null != status)
		{
			Enumeration en = statusSelectorValues.keys();
			while(en.hasMoreElements())
			{
				String htkey = (String)en.nextElement();
				String htvalue = (String)statusSelectorValues.get(htkey);

				if(htkey.equalsIgnoreCase(status.toLowerCase()))
				{
					value = htvalue;
					break;
				}
			}

			if(value != null)
			{
				zzdgd.set("var.status", value);
			}
			else
			{
				zzdgd.set("var.status", "");
			}
		}

		status = value;

		if(status != null && status.equalsIgnoreCase("sold"))
		{
			zzdgd.set("var.status", "-1"); // verkauft
		}
		else if(status == null || status.equals(""))
		{
			// top kommt -> auf im besitz setzen

			String oldstatus = (String)zzdgd.get("var.status");
			if(null == oldstatus)
			{
				oldstatus = "";
			}

			// es sei denn es ist schon verkauft ....
			if(!oldstatus.equals("-1") && !oldstatus.equals("2"))
			{
				zzdgd.set("var.status", "1"); // im besitz
			}
		}

		// TODO SELECTORVALUES .... ZLImportEMBank replace with head!!

		String ffl = (String)topdgd.get("var.leerfl");
		if(null != ffl)
		{
			zzdgd.set("var.leerfl", ffl);
		}

		String allgaddonfl = (String)topdgd.get("var.allgaddonfl");
		if(null != allgaddonfl)
		{
			zzdgd.set("var.allgaddonfl", allgaddonfl);
		}

		String leerstehung = (String)topdgd.get("var.leerstehung");
		if(leerstehung != null)
		{
			zzdgd.set("var.leerstehung", leerstehung);
		}

		String bk = (String)topdgd.get("var.bk");
		if(null != bk)
		{
			zzdgd.set("var.bk", bk);
		}
		addOriginalCurrencyValues("bk", "bk", zzdgd, topdgd);

		String betriebskosten = (String)topdgd.get("var.betriebskosten");
		if(null != betriebskosten)
		{
			zzdgd.set("var.betriebskosten", betriebskosten);
		}
		addOriginalCurrencyValues("betriebskosten", "betriebskosten", zzdgd, topdgd);

		String istmietepm = (String)topdgd.get("var.istmietepm");
		if(null != istmietepm)
		{
			zzdgd.set("var.istmietepm", istmietepm);
		}
		addOriginalCurrencyValues("istmietepm", "istmietepm", zzdgd, topdgd);

		String hauptmietzins = (String)topdgd.get("var.hauptmietzins");
		if(null != hauptmietzins)
		{
			zzdgd.set("var.hauptmietzins", hauptmietzins);
		}
		addOriginalCurrencyValues("hauptmietzins", "hauptmietzins", zzdgd, topdgd);

		String sollMiete = (String)topdgd.get("var.sollmietepm");
		if(sollMiete != null)
		{
			zzdgd.set("var.sollmietepm", sollMiete);
		}
		addOriginalCurrencyValues("sollmietepm", "sollmietepm", zzdgd, topdgd);

		String sollhauptmietzins = (String)topdgd.get("var.sollhauptmietzins");
		if(sollhauptmietzins != null)
		{
			zzdgd.set("var.sollhauptmietzins", sollhauptmietzins);
		}
		addOriginalCurrencyValues("sollhauptmietzins", "sollhauptmietzins", zzdgd, topdgd);

		try
		{
			String newYear = zl.jahr;
			String newMonth = zl.monat;
			if(newMonth.length() == 1)
			{
				newMonth = "0" + newMonth;
			}
			String newDate = "01." + newMonth + "." + newYear;
			zzdgd.set("var.datum", newDate);

		}
		catch(Exception e1)
		{
			// do nothing - date not exists on zinszeile
		}

		return zzdgd;
	}

		public DynGenDataObj writeCommonValues(Hashtable ht, DynGenDataObj dgd, Zinsliste zl)
	{
		ValueReplacement vr = new ValueReplacement(dgd, session);
		parentObject.flavour = (String)session.get("parentObject.flavour");

		// Put 0 values for Minderungen und Zuschlaege if not present in ht
		if(!ht.containsKey("mietzinszuschlaege"))
		{
			ht.put("mietzinszuschlaege", "0");
		}
		if(!ht.containsKey("mietzinsminderungen"))
		{
			ht.put("mietzinsminderungen", "0");
		}
		// IST FUER AREALIS EIN TICKET ABER NOCH NICHT BESTAETIGT VON BARBORA
		// if(dgd.getTemplateType().equals("CIMS.top"))
		// {
		if(ht.containsKey("mieteinheitenanz"))
		{
			// when vom top übernehmen beide true setzen
			// if(CfgSingleton.getInstance().getBoolean("ZLIMPORT_EINHEITENANZAHLFROMTOPWHENEMPTY", Boolean.FALSE))
			// {
			// String value = (String)ht.get("mieteinheitenanz");
			// if(null == value) value = "";
			// if(!value.matches("\\d+"))
			// {
			// value = dgd.getString("var.mieteinheitenanz").trim();
			// ht.put("mieteinheitenanz", value);
			// }
			// }
			//
			// //when nicht von einheit diese true setzen
			if(CfgSingleton.getInstance().getBoolean("ZLIMPORT_EINHEITENANZAHL1WHENEMPTY", Boolean.FALSE))
			{
				String value = (String)ht.get("mieteinheitenanz");
				if(null == value) value = "";
				if(!value.matches("\\d+"))
				{
					ht.put("mieteinheitenanz", "1");
				}
			}

		}
		// If Dates are not correct set it to ""
		Enumeration e = ht.keys();
		String REGEX_DATE = "([\\d]{1,2})\\.([\\d]{1,2})\\.([\\d]{4})";

		Pattern pattern = Pattern.compile(REGEX_DATE);

		int year = -1;
		int month = -1;
		int day = -1;

		// check if dates are correct
		while(e.hasMoreElements())
		{
			String key = "";
			String value = "";

			try
			{
				key = (String)e.nextElement();
				value = (String)ht.get(key);

				Matcher matcher = pattern.matcher(value);

				if(matcher.matches())
				{
					day = Integer.parseInt(matcher.group(1));
					month = Integer.parseInt(matcher.group(2));
					year = Integer.parseInt(matcher.group(3));

					if(day < 1 || day > 31 || month < 1 || month > 12 || year < 1780 || year > 3000)
					{
						ht.put(key, "");
					}
				}
			}
			catch(Exception ex)
			{
				// silent catch because no dramatic error
			}
		}

		if(parentObject.flavour.equals("icrskag"))
		{
			// AS - #26104 wenn kautionsart "keine Kaution" dann kautionshohe 0
			if(ht.containsKey("kaution") || ht.containsKey("kautionsart"))
			{

				String kautionsart = (String)ht.get("kaution");
				if(kautionsart == null)
				{
					kautionsart = (String)ht.get("kautionsart");
				}
				// wenn kautionsart offene kaution, dann kautionshoehe 0
				if(kautionsart != null && kautionsart != "" && kautionsart.length() > 0 && kautionsart.equals("6"))
				{
					ht.put("kautionshoehe", "0");
				}
				// wenn kautionsart keine kaution, dann kautionshoehe und kautionshoehesoll leer
				String kautionshoehe = (String)ht.get("kautionshoehe");
				if(kautionshoehe != null && kautionshoehe.length() == 0 && kautionsart.equals("7"))
				{
					ht.put("kautionshoehesoll", "");
					ht.put("kautionshoehesoll_sub", "");
				}
			}
		}
		if(ht.containsKey("mieter")) // wenn leerstehung indexdatum indexbasis leer setzen
		{
			String mieter = (String)ht.get("mieter");
			if(mieter != null && mieter != "" && mieter.length() > 0 && parentObject.checkLeerstandString(mieter))
			{
				boolean checkleer = parentObject.checkLeerstandString(mieter);
				if(checkleer)
				{
					if(ht.containsKey("indexdatum"))
					{
						ht.put("indexdatum", "");
					}
					if(ht.containsKey("indexbasis"))
					{
						ht.put("indexbasis", "");
					}
				}
			}
		}

		if(ht.containsKey("indexdatum"))
		{
			String indexdatum = (String)ht.get("indexdatum");
			if(indexdatum.contains("2000"))// manchmal liefern HV 01.01.2000
			{
				ht.put("indexdatum", "");
			}
		}

		// PKO - 20150331 - START Map all existing values to variables - spezial handling afterwards
		boolean isLeerstand = dgd.getBoolean("var.leerstehung", false);
		String mietvertragvon = "";
		if(isLeerstand && ht.containsKey("mietvertragvon"))
		{
			mietvertragvon = (String)ht.get("mietvertragvon");
			if(mietvertragvon.length() == 0)
			{
				mietvertragvon = (String)dgd.get("var.mietvertragvon");
				ht.put("mietvertragvon", mietvertragvon);
			}
		}

		if(isLeerstand && !ht.containsKey("mietvertragvon"))
		{
			// get Value from latest MV
			String mvvalue = getLatestMVDatenFromMietvertrag(dgd.id);
			if(mvvalue == null)
			{
				mvvalue = "";
			}
			ht.put("mietvertragvon", mvvalue);
		}

		// mieterfirma / Debitor zuordnung zu top und zinszeile
		if(parentObject.flavour.equals("icrsare"))
		{
			// Nur einmal den Slot mieterfirma (Debitor) schreiben um Sideeffekts zu vermeiden -> Wenn noetig auch andere Slots moeglich
			if(ht.containsKey("mieterfirma___uniqueid"))
			{
				Hashtable slotsToWrite = new HashMap();
				for(Object key : ht.keySet())
				{
					if(key.toString().startsWith("mieterfirma"))
					{
						slotsToWrite.put(key, ht.get(key));
					}
				}
				slotsToWrite.put("mieterfirma___typ", "DEBITOR");

				writeSlots(slotsToWrite, dgd, true, false);
			}
		}
		else
		{
			// Nur einmal den Slot mieterfirma (Debitor) schreiben um Sideeffekts zu vermeiden -> Wenn noetig auch andere Slots moeglich
			if(ht.containsKey("mieterfirma"))
			{
				Hashtable slotsToWrite = new HashMap();
				for(Object key : ht.keySet())
				{
					if(key.toString().contains("mieterfirma"))
					{
						slotsToWrite.put(key, ht.get(key));
					}
				}
				writeSlots(slotsToWrite, dgd);
			}

			if(ht.containsKey("vermieterfirma"))
			{
				Hashtable slotsToWrite = new HashMap();
				for(Object key : ht.keySet())
				{
					if(key.toString().contains("vermieterfirma"))
					{
						slotsToWrite.put(key, ht.get(key));
					}
				}
				writeSlots(slotsToWrite, dgd);
			}

			// Nur einmal den Slot Preismodell schreiben
			if(ht.containsKey("preismodell"))
			{
				Hashtable slotsToWrite = new HashMap();
				for(Object key : ht.keySet())
				{
					if(key.toString().contains("preismodell"))
					{
						slotsToWrite.put(key, ht.get(key));
					}
				}
				writeSlots(slotsToWrite, dgd);
			}
		}

		if(ht.containsKey("firmabranche"))
		{
			Hashtable slotsToWrite = new HashMap();
			slotsToWrite.put("firmabranche", ht.get("firmabranche"));
			writeSlots(slotsToWrite, dgd, false, true);
		}

		if(ht.containsKey("mietenpools"))
		{
			Hashtable slotsToWrite = new HashMap();
			slotsToWrite.put("mietenpools", ht.get("mietenpools"));
			writeSlots(slotsToWrite, dgd, false, true);
		}

		if(ht.containsKey("leerstehungsarten"))
		{
			Hashtable slotsToWrite = new HashMap();
			slotsToWrite.put("leerstehungsarten", ht.get("leerstehungsarten"));
			writeSlots(slotsToWrite, dgd, false, true);
		}

		// PKO 20160609 #7204-Wenn true (default=false) wird bei Wechsel von Leerstehung auf Vermietung die Sollmiete gleich der Istmiete gesetzt
		if(parentObject.zinslistenImport.getZlTypeConfig().isSetsollmietetoistmietebeiwechselleeraufvermietet())
		{
			Boolean leer = dgd.getBoolean("var.leerstehung", Boolean.FALSE);
			String actualmieter = ht.get("mieter").toString().toLowerCase();

			if(leer && !parentObject.checkLeerstandString(actualmieter))
			{
				String hauptmietzins = (String)ht.get("hmz");
				dgd.set("var.sollmietepm", hauptmietzins);
				addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
				dgd.set("var.sollhauptmietzins", hauptmietzins);
				addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
			}
		}

		boolean leerstand = false;
		try
		{
			String actualmieter = ht.get("mieter").toString().toLowerCase();
			leerstand = parentObject.checkLeerstandString(actualmieter);
		}
		catch(Exception e1)
		{
			BugMe.getInstance().log(this, "Leerstandscheck nicht moeglich, kein mieter gesetzt - " + ht.toString());
			leerstand = false;
		}

		// Wenn isSetSollMieteOnLeerstehung == false, dann sollmiete auf 0 setzten
		if(!parentObject.zinslistenImport.getZlTypeConfig().isSetSollMieteOnLeerstehung())
		{
			if(leerstand)
			{
				String sollmiete = "0";
				dgd.set("var.sollmietepm", sollmiete);
				addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
				dgd.set("var.sollhauptmietzins", sollmiete);
				addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
			}
		}

		// PKO 20161004 Mantis #23135-Sollmieten-Steuerung 7204-Wenn true (default=false) wird die Sollmiete an Erhöhung und Verminderung der IST-Miete anpasst (Nur wenn IST-Miete != null &&
		// IST-Miete<>0

		dgd.set("var.ismodifysollmieteinbothdirections", "0");

		if(!leerstand)
		{
			if((parentObject.zinslistenImport.getZlTypeConfig().isModifysollmieteinbothdirections() && (!ht.containsKey("sollmietepm") || ht.get("sollmietepm").toString().length() == 0)) || (ht.containsKey("ismodifysollmieteinbothdirections") && ht.get("ismodifysollmieteinbothdirections").equals("1")))
			{
				String hauptmietzins = (String)ht.get("hmz");

				if(hauptmietzins != null && hauptmietzins.length() > 0 && !hauptmietzins.startsWith("0"))
				{
					dgd.set("var.sollmietepm", hauptmietzins);
					addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
					dgd.set("var.sollhauptmietzins", hauptmietzins);
					addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);

					// sollmiete in den ht geben
					ht.put("sollmietepm", hauptmietzins);
				}
				else
				{
					hauptmietzins = dgd.getString("var.sollmietepm");
					ht.put("sollmietepm", hauptmietzins);
					addOriginalCurrencyValues("sollhauptmietzins", "sollmietepm", dgd, ht);
					dgd.set("var.sollhauptmietzins", hauptmietzins);
					addOriginalCurrencyValues("sollhauptmietzins", "sollmietepm", dgd, ht);
				}

				ht.put("ismodifysollmieteinbothdirections", "1");
				dgd.set("var.ismodifysollmieteinbothdirections", "1");
			}
		}
		if(ht.containsKey("sollmietepm") && !ht.containsKey("sollhauptmietzins"))
		{
			dgd.set("var.sollhauptmietzins", ht.get("sollmietepm"));
			addOriginalCurrencyValues("sollhauptmietzins", "sollmietepm", dgd, ht);
		}

		try
		{
			Boolean rsam2 = CfgSingleton.getInstance().getBoolean("RECHNESOLLMIETENAUSM2", Boolean.FALSE);
			if(rsam2)
			{
				Boolean stellplatz = dgd.getBoolean("var.stellplatz", Boolean.FALSE);
				Integer anzahl = dgd.getInteger("var.mieteinheitenanz", 1);
				Currency nfl = dgd.getCurrency("var.nfl");
				if(nfl == null)
				{
					nfl = new Currency(0);
				}
				Currency leerfl = dgd.getCurrency("var.leerfl");
				if(leerfl == null)
				{
					leerfl = new Currency(0);
				}
				Currency fl = nfl.add(leerfl);
				long fl_long = fl.getLongValue();

				// Currency smpmm2 = getCurrency("var.sollmietepm2");
				// Currency smpmbcm2 = getCurrency("var.sollmietepm2bc");
				// Currency smpmwcm2 = getCurrency("var.sollmietepm2wc");

				Currency sollmietepm = dgd.getCurrency("var.sollmietepm");
				if(sollmietepm == null || sollmietepm.getLongValue() == 0)
				{
					sollmietepm = dgd.getCurrency("var.sollhauptmietzins");
				}

				if(stellplatz)
				{
					// STellplätze
					if(anzahl != null && anzahl.toString().length() > 0 && sollmietepm != null && sollmietepm.toString().length() > 0 && sollmietepm.getLongValue() > 0)
					{
						Currency sollmietepm2 = sollmietepm.divide(new Currency("" + anzahl.intValue()));
						if(sollmietepm2 != null)
						{
							dgd.set("var.sollmietepm2", sollmietepm2.getFormattedStringValue());
						}
					}
				}
				else
				{
					// Flaechen
					if(fl_long > 0 && sollmietepm != null && sollmietepm.toString().length() > 0 && sollmietepm.getLongValue() > 0)
					{
						Currency sollmietepm2 = sollmietepm.divide(fl);
						if(sollmietepm2 != null)
						{
							dgd.set("var.sollmietepm2", sollmietepm2.getFormattedStringValue());
						}
					}
				}
			}
		}
		catch(Exception ex)
		{
			debug.error("Zinslistenupload -> RECHNESOLLMIETENAUSM2 nicht möglich: " + ex.getMessage());
		}

		TemplateReader profil = TemplateReader.getInstance();
		// System.out.println("TemplateType: " + dgd.getTemplateType());
		DynGenDataObj flavoureddgd = profil.getFlavouredDGDForTemplate(dgd.getTemplateType(), global, session);

		// Felder die vor dem Schreiben auf leer gesetzt werden sollen, damit sie im Falle einer Postenneuzuordnung keinen Wert mehr beinhalten
		Vector<String> columnsSetToEmpty = new Vector<>();

		// Bei der SOM sollen Posten, die nicht im File sind nicht auf Leer gesetzt werden, sondern einfach gelassen Error #25708
		if(CfgSingleton.getInstance().getBoolean("ICRSPM_SET_MISSING_POSTEN_TO_EMPTY", Boolean.TRUE))
		{

			columnsSetToEmpty.add("hmzdiverse");
			columnsSetToEmpty.add("hmzpar18");
			columnsSetToEmpty.add("hmzgarage");
			columnsSetToEmpty.add("hmzzuschlag");
			columnsSetToEmpty.add("hmzminderung");
			columnsSetToEmpty.add("hmzsonstige");
			columnsSetToEmpty.add("hmzvst");
			columnsSetToEmpty.add("hmzposten1");
			columnsSetToEmpty.add("hmzposten2");
			columnsSetToEmpty.add("hmzposten3");
			columnsSetToEmpty.add("hmzposten4");
			columnsSetToEmpty.add("hmzposten5");
			columnsSetToEmpty.add("hmzfreiw");
			columnsSetToEmpty.add("evb");
			columnsSetToEmpty.add("bkdiverse");
			columnsSetToEmpty.add("bklift");
			columnsSetToEmpty.add("bkgarage");
			columnsSetToEmpty.add("bkwarmwasser");
			columnsSetToEmpty.add("bkheizung");
			columnsSetToEmpty.add("bkklimaanlage");
			columnsSetToEmpty.add("bkrunrat");
			columnsSetToEmpty.add("bkkanal");
			columnsSetToEmpty.add("bkwasser");
			columnsSetToEmpty.add("bkabgaben");
			columnsSetToEmpty.add("bkreinigung");
			columnsSetToEmpty.add("bkstrommietflaeche");
			columnsSetToEmpty.add("bkstromallgemein");
			columnsSetToEmpty.add("bksonderbetriebskosten");
			columnsSetToEmpty.add("bk");

			for(int i = 0; i < columnsSetToEmpty.size(); i++)
			{
				String key = columnsSetToEmpty.get(i);
				dgd.set("var." + key, "0");
			}

		}

		Hashtable vars = dgd.getSubs("var");
		for(Object key : vars.keySet())
		{
			try
			{
				if(CfgSingleton.getInstance().getBoolean("ICRSPM_SET_MISSING_POSTEN_TO_EMPTY", Boolean.TRUE))
				{
					// Ustsaetze auf Leer setzten wenn sie nicht in der Zinsliste vorhanden sind - sonst steht bei jedem Import der alte Wert drinnen
					if((key.toString().startsWith("hmz") || key.toString().startsWith("bk")) && key.toString().endsWith("ustsatz") && !(ht.containsKey(key) || ht.containsKey(key + "sub") || ht.containsKey(key + "_sub")))
					{
						dgd.set("var." + key, "");
					}
					// Ustwerte auf Leer setzten wenn sie nicht in der Zinsliste vorhanden sind - sonst steht bei jedem Import der alte Wert drinnen
					if((key.toString().startsWith("hmz") || key.toString().startsWith("bk")) && key.toString().endsWith("ustwert") && !(ht.containsKey(key) || ht.containsKey(key + "sub") || ht.containsKey(key + "_sub")))
					{
						dgd.set("var." + key, "");
					}
				}

				if(ht.containsKey(key) || ht.containsKey(key + "sub") || ht.containsKey(key + "_sub"))
				{
					String value = (String)ht.get(key) + "";
					if(value == null || value.trim().length() == 0)
					{
						value = (String)ht.get(key + "_sub");
					}
					if(value == null)
					{
						value = "";
					}
					if(value.equals("null"))
					{
						value = "";
					}
					if(value != null)
					{
						// pruefen ob selector oder wert
						String selector = flavoureddgd.getString("var." + key + ".SELECTOR");
						if(selector.length() > 0)
						{
							Hashtable opts = parentObject.getValueMap(selector);
							if(opts.containsKey(value))
							{
								value = (String)opts.get(value);
							}
							else if(opts.containsKey(value.toLowerCase()))
							{
								value = (String)opts.get(value.toLowerCase());
							}
							else if(opts.containsKey(value.trim()))
							{
								value = (String)opts.get(value.trim());
							}
							else if(opts.containsKey(value.trim().toLowerCase()))
							{
								value = (String)opts.get(value.trim().toLowerCase());
							}

							// wenn value = "" -> umgekehrtes mapping auch noch probieren
							// Kündigungsregel muss ausgeschlossen werden. Wenn value = "", dann wird "-" vom Selektor geholt
							// und das ist kein gültiger Wert und Wault wird nicht berechnet
							boolean emptykuendigungsregel = false;
							if(key.equals("kuendigungsregel") && value.equals(""))
							{
								emptykuendigungsregel = true;
							}
							if(!emptykuendigungsregel)
							{
								if(value.equals(""))
								{
									opts = vr.getValueMap(selector);
									if(opts.containsKey(value))
									{
										value = (String)opts.get(value);
									}
									else if(opts.containsKey(value.toLowerCase()))
									{
										value = (String)opts.get(value.toLowerCase());
									}
									else if(opts.containsKey(value.trim()))
									{
										value = (String)opts.get(value.trim());
									}
									else if(opts.containsKey(value.trim().toLowerCase()))
									{
										value = (String)opts.get(value.trim().toLowerCase());
									}
								}
							}
						}

						// PKO 20150417 - Workaround for in zinslistenconfig mapped subnutzung
						if(key.equals("subnutzung"))
						{
							if(parentObject.xc.getSubNutzungsWerte() == null)
							{
								parentObject.xc.getXMLConfig("hausverwaltung", parentObject.zinslistenImport.getZlTypeConfig().getName() + "mieter");
							}
							Hashtable subnutzungen = parentObject.xc.getSubNutzungsWerte();

							if(subnutzungen.containsKey(value))
							{
								value = subnutzungen.get(value).toString().toUpperCase();
							}
							else if(subnutzungen.containsKey(value.toLowerCase()))
							{
								value = subnutzungen.get(value.toLowerCase()).toString().toUpperCase();
							}

							// PKO 20181010 - Wenn nur die Subnutzung vorhanden ist und keine Nutzung, dann Nutzung aus Subnutzung herausschneiden zB Subnutzung W-FA -> W == Nutzung
							if(!ht.containsKey("nutzung") && value != null && value.length() > 0 && value.contains("-"))
							{
								String nutzung = value.substring(0, value.indexOf("-"));
								dgd.set("var.nutzung", nutzung);
							}
						}

						// PKO 20150720 - Workaround: Wenn Leerstand und kein MV Beginn fuer Top dann MV Ende oder Kuendigungsdatum vom letzten Mietvertrag setzen

						// Top Leerstand?
						if(key.toString().contains("mietvertragvon"))
						{

							String mietvertragvonTop = (String)dgd.get("var.mietvertragvon");
							String mietvertragvonZinsliste = value;
							String dgdId = (String)dgd.get("id");

							if(value != null && value.length() > 0)
							{
								// leave value as it is
							}
							else if(isLeerstand && mietvertragvonTop.length() == 0)
							{

								if(mietvertragvonZinsliste.length() > 0)
								{
									value = mietvertragvonZinsliste;
								}
								else
								{
									// get Value from latest MV
									if(dgdId != null) // Also CIMS.top (Bei Zinszeile ist die id null)
									{
										value = getLatestMVDatenFromMietvertrag(dgdId);
									}
								}

							}
						}

						// Nutzung aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
						// wir nehmen die nutzung aus dem top? wenn sie befüllt ist
						String dgdNutzung = dgd.getString("var.nutzung");
						boolean overrideNutzung = parentObject.zinslistenImport.getZlTypeConfig().isOverrideNutzung();

						String templatetype = dgd.getTemplateType();
						if(key.equals("nutzung") && overrideNutzung && dgdNutzung.length() > 0)
						{
							// do nothing
						}
						else
						{
							// if(key.equals("nutzung"))
							// {
							// System.out.println("Nutzung: " + value + " / DGD-Nutzung: " + dgdNutzung + "/ Overreide: " + overrideNutzung + " / TType: " + templatetype);
							// }

							if(key.equals("nutzung"))
							{
								if(null != value && value.length() > 3)
								{
									parentObject.zlprotocol.appendHtmlErr(Tr.t("textTypeOfUseTooLong1", session.getString("language")) + " " + value + " " + Tr.t("textTypeOfUseTooLong2", session.getString("language")) + " " + value.substring(0, 3) + ".<br>\n");
									parentObject.zlprotocol.appendTxtErr(Tr.t("textTypeOfUseTooLong1", session.getString("language")) + " " + value + " " + Tr.t("textTypeOfUseTooLong2", session.getString("language")) + " " + value.substring(0, 3) + "!");
									value = value.substring(0, 3);
								}
							}

							if(value == null)
							{
								value = "";
							}
							String vartype = (String)dgd.get("var." + key + ".TYPE");
							if(vartype == null)
							{
								vartype = "";
							}
							if(vartype.toUpperCase().equals("DATE") && value.matches("([0-9]{2})\\.([0-9]{2})\\.([0-9]{4})"))
							{
								String[] parts = value.split("\\.");
								if(parts.length == 3)
								{
									int datejahr = Integer.parseInt(parts[2]);

									if(datejahr < 1800 || year > 3000)
									{
										value = "";
									}
								}
							}
							else if(vartype.toUpperCase().equals("DATE") && value.matches("([0-9]{2})\\.([0-9]{2})\\.([0-9]{2})"))
							{
								// do nothing
							}

							dgd.set("var." + key, value);

							// 23456 - Set Multicurrency Values
							addOriginalCurrencyValues(key.toString(), key.toString(), dgd, ht);
						}
					}
				}

			}
			catch(Exception ex)
			{
				// Do nothing
				debug.error(ex);
			}
		}

		// PKO - 20150331 - END Map all existing values to variables - spezial handling afterwards

		// WAULT auf ZZ schreiben
		String ttype = dgd.getTemplateType();

		if(dgd.getTemplateType().equals("CIMS.zinszeile"))
		{
			try
			{
				TopoTool topotool = new TopoTool(session, global);
				String oid_haus = topotool.getHausOID(zl);

				if(null == parentObject.DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					parentObject.DAInst = conn.getDataAgent();
				}
				DynGenDataObj hausDGD = (DynGenDataObj)parentObject.DAInst.getObject(parentObject.oid_haus, null);

				WAULTBerechnungResult wbResult = WAULTBerechnung.setWaultAndRelatedValues(Calendar.getInstance(), dgd, hausDGD, session);

				if(wbResult.getTopZinszeileOrMietvertragDgd() != null)
				{
					dgd = wbResult.getTopZinszeileOrMietvertragDgd();
				}
			}
			catch(Exception ex)
			{
				debug.error(ex);
			}
		}

		// TODO Remove all unneccessary mappings !!!!!!!!!!!

		// Kategorie aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
		String kategorie = (String)ht.get("kategorie");
		if(null != kategorie)
		{
			// changed from 10 to 11 by Mehtap
			if(kategorie.length() > 20)
			{
				// changed 10 to 11 by Mehtap
				kategorie = kategorie.substring(0, 20);
				parentObject.zlprotocol.appendHtmlErr(Tr.t("textCategoryTooLong", session.getString("language")) + "<br>\n");
				parentObject.zlprotocol.appendTxtErr(Tr.t("textCategoryTooLong", session.getString("language")));
			}
			dgd.set("var.kategorie", kategorie);
		}

		// Kautionsart aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
		String kautionsart = (String)ht.get("kautionsart");
		if(null == kautionsart)
		{
			kautionsart = (String)ht.get("kaution");
		}

		if(null != kautionsart)
		{
			kautionsart = kautionsart.trim();
			if(kautionsart.length() > 0)
			{
				try
				{
					// int ka = Integer.parseInt(kautionsart);
					dgd.set("var.kaution", kautionsart);
				}
				catch(Exception xx)
				{
					parentObject.zlprotocol.appendHtmlErr(Tr.t("textTypeOfDeposit1", session.getString("language")) + " " + kautionsart + " " + Tr.t("textTypeOfDeposit2", session.getString("language")) + "<br>\n");
					parentObject.zlprotocol.appendTxtErr(Tr.t("textTypeOfDeposit1", session.getString("language")) + " " + kautionsart + " " + Tr.t("textTypeOfDeposit2", session.getString("language")));
				}
			}
		}

		// Reparaturrücklagen aus dem ht (Hashtable mit Werten aus der Zinsliste übernehmen
		String myval = (String)ht.get("reparaturfond");
		if(null == myval)
		{
			myval = "0,0";
		}
		if(myval.equals(""))
		{
			myval = "0,0";
		}
		dgd.set("var.reparaturfond", myval);

		String verwaltungshonorar = (String)ht.get("verwaltungshonorar");
		if(null == verwaltungshonorar)
		{
			verwaltungshonorar = "0,0";
		}
		if(myval.equals(""))
		{
			verwaltungshonorar = "0,0";
		}
		dgd.set("var.verwaltungshonorar", verwaltungshonorar);

		String vertragsmietepm = (String)ht.get("vertragsmietepm");
		if(null != vertragsmietepm && vertragsmietepm.trim().length() > 0 && vertragsmietepm.matches("[\\d\\.\\,]+"))
		{
			dgd.set("var.vertragsmietepm", vertragsmietepm);
		}

		String periodizitaet = (String)ht.get("periodizitaet");
		if(null != periodizitaet)
		{
			if(periodizitaet.contains(","))
			{
				periodizitaet = periodizitaet.substring(0, periodizitaet.indexOf(","));
			}
			dgd.set("var.periodizitaet", periodizitaet);
		}

		// String kuendigungsregel = (String)ht.get("kuendigungsregel");
		// if(null != kuendigungsregel)
		// {
		// if(kuendigungsregel.contains(","))
		// {
		// kuendigungsregel = kuendigungsregel.substring(0, kuendigungsregel.indexOf(","));
		// }
		//
		// dgd.set("kuendigungsregel", kuendigungsregel);
		// }

		String kuendigungsfrist = (String)ht.get("kuendigungsfrist");
		if(null != kuendigungsfrist)
		{
			if(kuendigungsfrist.contains(","))
			{
				kuendigungsfrist = kuendigungsfrist.substring(0, kuendigungsfrist.indexOf(","));
			}

			dgd.set("var.kuendigungsfrist", kuendigungsfrist);
		}

		String sonderkuendigungsfrist = (String)ht.get("sonderkuendigungsfrist");
		if(null != sonderkuendigungsfrist)
		{
			if(sonderkuendigungsfrist.contains(","))
			{
				sonderkuendigungsfrist = sonderkuendigungsfrist.substring(0, sonderkuendigungsfrist.indexOf(","));
			}

			dgd.set("var.sonderkuendigungsfrist", sonderkuendigungsfrist);
		}

		Hashtable indexArtSelectorValues = null;
		try
		{
			indexArtSelectorValues = vr.getValueMap("CIMS.IndexArtSelector");
		}
		catch(Exception ex)
		{}
		String value = "";

		String indexschwelleprozent = (String)ht.get("indexschwelleprozent");
		try
		{
			double idx = Double.parseDouble(indexschwelleprozent.replaceAll(",", ".")) * 100;
			indexschwelleprozent = (idx + "").replaceAll("[.]", ",");
		}
		catch(Exception ex)
		{}
		if(null != indexschwelleprozent)
		{
			dgd.set("var.indexschwelleprozent", indexschwelleprozent);
		}

		String indexversatz = (String)ht.get("indexversatz");
		if(null != indexversatz)
		{
			if(indexversatz.contains(","))
			{
				indexversatz = indexversatz.substring(0, indexversatz.indexOf(","));
			}
			dgd.set("var.indexversatz", indexversatz);
		}

		String indexweitergabe = (String)ht.get("indexweitergabe");
		try
		{
			double idx = Double.parseDouble(indexweitergabe.replaceAll(",", ".")) * 100;
			indexweitergabe = (idx + "").replaceAll("[.]", ",");
		}
		catch(Exception ex)
		{}
		if(null != indexweitergabe)
		{
			dgd.set("var.indexweitergabe", indexweitergabe);
		}

		boolean MODIFY_LETZTE_INDEXIERUNG = CfgSingleton.getInstance().getBoolean("MODIFY_LETZTE_INDEXIERUNG", Boolean.FALSE);

		try
		{
			if(MODIFY_LETZTE_INDEXIERUNG)
			{
				modifyLetzteIndexierung(ht, zl, dgd);
			}
		}
		catch(Exception ex)
		{
			debug.log("");

		}

		String geburtsjahr = (String)ht.get("geburtsjahr");
		if(null != geburtsjahr)
		{
			try
			{
				// int gj = Integer.parseInt(geburtsjahr);
				dgd.set("var.mgeburtsjahr", geburtsjahr);
			}
			catch(Exception xx)
			{
				debug.log(xx);
			}
		}

		// System.err.println("ZLU2: Setting Rep Fond:"+myval);

		for(int i = 1; i <= 10; i++)
		{
			dgd = writeZZValue2DGD("bkposten" + i, "bkposten" + i, "0,0", dgd, ht);
			addOriginalCurrencyValues("bkposten" + i, "bkposten" + i, dgd, ht);
			dgd = writeZZValue2DGD("bkposten" + i + "ustsatz", "bkposten" + i + "ustsatz", null, dgd, ht);

			dgd = writeZZValue2DGD("hmzposten" + i, "hmzposten" + i, "0,0", dgd, ht);
			addOriginalCurrencyValues("hmzposten" + i, "hmzposten" + i, dgd, ht);
			dgd = writeZZValue2DGD("hmzposten" + i + "ustsatz", "hmzposten" + i + "ustsatz", null, dgd, ht);
		}

		// SUBPOSTEN HMZ
		dgd = writeZZValue2DGD("hmzdiverse", "diverse hauptmietzinse", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzdiverse", "diverse hauptmietzinse", dgd, ht);
		dgd = writeZZValue2DGD("hmzdiverseustsatz", "diverse hauptmietzinseustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzpar18", "§ 18 mietzins", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzpar18", "§ 18 mietzins", dgd, ht);
		dgd = writeZZValue2DGD("hmzpar18ustsatz", "§ 18 mietzinsustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzumsatz", "hmzumsatz", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzumsatz", "hmzumsatz", dgd, ht);

		dgd = writeZZValue2DGD("hmzumsatzcurrency", "hmzumsatzcurrency", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzumsatzanteil", "hmzumsatzanteil", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzumsatzanteil", "hmzumsatzanteil", dgd, ht);

		dgd = writeZZValue2DGD("hmzumsatzbasis", "hmzumsatzbasis", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzumsatzbasis", "hmzumsatzbasis", dgd, ht);

		dgd = writeZZValue2DGD("hmzindexnachverrechnung", "hmzindexnachverrechnung", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzindexnachverrechnung", "hmzindexnachverrechnung", dgd, ht);
		// #13575 es gibt kein Feld hmzindexnachverrechnungustsatz im System, unten wird hmzindexnachverrechnung uberschrieben
		// dgd = writeZZValue2DGD("hmzindexnachverrechnung", "hmzindexnachverrechnungustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzgarage", "garagenmietzins", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzgarage", "garagenmietzins", dgd, ht);
		dgd = writeZZValue2DGD("hmzgarageustsatz", "garagenmietzinsustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzzuschlag", "mietzinszuschlaege", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzzuschlag", "mietzinszuschlaege", dgd, ht);
		dgd = writeZZValue2DGD("hmzzuschlagustsatz", "mietzinszuschlaegeustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzminderung", "mietzinsminderungen", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzminderung", "mietzinsminderungen", dgd, ht);
		dgd = writeZZValue2DGD("hmzminderungustsatz", "mietzinsminderungenustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzsonstige", "sonstige mietzinse", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzsonstige", "sonstige mietzinse", dgd, ht);
		dgd = writeZZValue2DGD("hmzsonstigeustsatz", "sonstige mietzinseustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzvst", "vorsteuer kuerzung", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzvst", "vorsteuer kuerzung", dgd, ht);
		dgd = writeZZValue2DGD("hmzvstustsatz", "vorsteuer kuerzungustsatz", null, dgd, ht);

		// Möbelmiete
		dgd = writeZZValue2DGD("hmzposten1", "moebelmiete", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzposten1", "moebelmiete", dgd, ht);
		dgd = writeZZValue2DGD("hmzposten1ustsatz", "moebelmieteustsatz", null, dgd, ht);

		// Darlehen-Akonto
		dgd = writeZZValue2DGD("hmzposten2", "darlehen-akonto", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzposten2", "darlehen-akonto", dgd, ht);
		dgd = writeZZValue2DGD("hmzposten2ustsatz", "darlehen-akontoustsatz", null, dgd, ht);

		// Baukostenzuschuss
		dgd = writeZZValue2DGD("hmzposten3", "baukostenzuschuss", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzposten3", "baukostenzuschuss", dgd, ht);
		dgd = writeZZValue2DGD("hmzposten3ustsatz", "baukostenzuschussustsatz", null, dgd, ht);

		// Keller Miete
		dgd = writeZZValue2DGD("hmzposten4", "kellermiete", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzposten4", "kellermiete", dgd, ht);
		dgd = writeZZValue2DGD("hmzposten4ustsatz", "kellermieteustsatz", null, dgd, ht);

		// Teschnische Ausstattung
		dgd = writeZZValue2DGD("hmzposten5", "teschnischeausstattung", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzposten5", "teschnischeausstattung", dgd, ht);
		dgd = writeZZValue2DGD("hmzposten5ustsatz", "teschnischeausstattungustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzfreiw", "hmzfreiw", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzfreiw", "hmzfreiw", dgd, ht);
		dgd = writeZZValue2DGD("hmzfreiwustsatz", "hmzfreiwustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("hmzverwohnung", "hmzverwohnung", "0,0", dgd, ht);
		addOriginalCurrencyValues("hmzverwohnung", "hmzverwohnung", dgd, ht);
		dgd = writeZZValue2DGD("hmzverwohnung", "hmzverwohnung", null, dgd, ht);

		dgd = writeZZValue2DGD("evb", "erhaltungsbeitrag", "0,0", dgd, ht);
		addOriginalCurrencyValues("evb", "erhaltungsbeitrag", dgd, ht);
		dgd = writeZZValue2DGD("evbustsatz", "erhaltungsbeitragustsatz", null, dgd, ht);

		// Im Falle von mietfreien Zeiten die Miete in € als Mietminderung

		dgd = writeZZValue2DGD("hmzmietfrei", "mietfreie zeit", "0,0", dgd, ht);

		dgd = writeZZValue2DGD("bkdiverse", "diverse betriebskosten", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkdiverse", "diverse betriebskosten", dgd, ht);
		dgd = writeZZValue2DGD("bkdiverseustsatz", "diverse betriebskostenustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bklift", "bk fuer lift", "0,0", dgd, ht);
		addOriginalCurrencyValues("bklift", "bk fuer lift", dgd, ht);
		dgd = writeZZValue2DGD("bkliftustsatz", "bk fuer liftustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkgarage", "bk fuer garage", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkgarage", "bk fuer garage", dgd, ht);
		dgd = writeZZValue2DGD("bkgarageustsatz", "bk fuer garageustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkwarmwasser", "bk fuer warmwasser", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkwarmwasser", "bk fuer warmwasser", dgd, ht);
		dgd = writeZZValue2DGD("bkwarmwasserustsatz", "bk fuer warmwasserustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkwasser", "bk fuer wasser", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkwasser", "bk fuer wasser", dgd, ht);
		dgd = writeZZValue2DGD("bkwasserustsatz", "bk fuer wasserustsatz", null, dgd, ht);

		// oeffentliche abgaben
		dgd = writeZZValue2DGD("bkabgaben", "oeffentliche abgaben", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkabgaben", "oeffentliche abgaben", dgd, ht);
		dgd = writeZZValue2DGD("bkabgabenustsatz", "oeffentliche abgabenustsatz", null, dgd, ht);

		// bk fuer muell
		dgd = writeZZValue2DGD("bkrunrat", "bk fuer muell", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkrunrat", "bk fuer muell", dgd, ht);
		dgd = writeZZValue2DGD("bkrunratustsatz", "bk fuer muellustsatz", null, dgd, ht);

		// bk fuer kanal
		dgd = writeZZValue2DGD("bkkanal", "bk fuer kanal", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkkanal", "bk fuer kanal", dgd, ht);
		dgd = writeZZValue2DGD("bkkanalustsatz", "bk fuer kanalustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkheizung", "bk fuer heizung", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkheizung", "bk fuer heizung", dgd, ht);
		dgd = writeZZValue2DGD("bkheizungustsatz", "bk fuer heizungustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkklimaanlage", "bk fuer klimaanlage", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkklimaanlage", "bk fuer klimaanlage", dgd, ht);
		dgd = writeZZValue2DGD("bkklimaanlageustsatz", "bk fuer klimaanlageustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkreinigung", "bk fuer reinigung", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkreinigung", "bk fuer reinigung", dgd, ht);
		dgd = writeZZValue2DGD("bkreinigungustsatz", "bk fuer reinigungustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bksonderbetriebskosten", "bksonderbetriebskosten", "0,0", dgd, ht);
		addOriginalCurrencyValues("bksonderbetriebskosten", "bksonderbetriebskosten", dgd, ht);
		dgd = writeZZValue2DGD("bksonderbetriebskostenustsatz", "bksonderbetriebskostenustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkstrommietflaeche", "bkstrommietflaeche", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkstrommietflaeche", "bkstrommietflaeche", dgd, ht);
		dgd = writeZZValue2DGD("bkstrommietflaecheustsatz", "bkstrommietflaecheustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkstromallgemein", "bkstromallgemein", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkstromallgemein", "bkstromallgemein", dgd, ht);
		dgd = writeZZValue2DGD("bkstromallgemeinustsatz", "bkstromallgemeinustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkvst", "bkvst", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkvst", "bkvst", dgd, ht);
		dgd = writeZZValue2DGD("bkvstustsatz", "bkvstustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkreparaturfond", "bkreparaturfond", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkreparaturfond", "bkreparaturfond", dgd, ht);
		dgd = writeZZValue2DGD("bkreparaturfondustsatz", "bkreparaturfondustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkversicherung", "bkversicherung", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkversicherung", "bkversicherung", dgd, ht);
		dgd = writeZZValue2DGD("bkversicherungustsatz", "bkversicherungustsatz", null, dgd, ht);

		dgd = writeZZValue2DGD("bkverwaltung", "bkverwaltung", "0,0", dgd, ht);
		addOriginalCurrencyValues("bkverwaltung", "bkverwaltung", dgd, ht);
		dgd = writeZZValue2DGD("bkverwaltungustsatz", "bkverwaltungustsatz", null, dgd, ht);

		String mieter = (String)ht.get("mieter");
		if(null != mieter)
		{
			if(mieter.length() > 255)
			{
				mieter = mieter.substring(0, 255);
				parentObject.zlprotocol.appendHtmlErr(Tr.t("textTenantTooLong", session.getString("language")) + "<br>\n");
				parentObject.zlprotocol.appendTxtErr(Tr.t("textTenantTooLong", session.getString("language")));
			}
			dgd.set("var.mieter", mieter);
		}

		String vermieter = (String)ht.get("vermieter");
		if(null != vermieter)
		{
			if(vermieter.length() > 255)
			{
				vermieter = vermieter.substring(0, 255);
				parentObject.zlprotocol.appendHtmlErr(Tr.t("textLessorTooLong", session.getString("language")) + "<br>\n");
				parentObject.zlprotocol.appendTxtErr(Tr.t("textLessorTooLong", session.getString("language")));
			}
			dgd.set("var.vermieter", vermieter);
		}

		String nfl = (String)ht.get("nfl");
		// System.err.println("ZLU2: NFL"+nfl);
		if(null != nfl)
		{
			dgd.set("var.nfl", nfl);
		}
		else if(dgd.getString("var.nfl").length() == 0)
		{
			dgd.set("var.nfl", "0,0");
		}

		String gesamtflaeche = (String)ht.get("gesamtflaeche");
		if(null != gesamtflaeche)
		{
			dgd.set("var.gesamtflaeche", gesamtflaeche);
		}
		else if(dgd.getString("var.gesamtflaeche").length() == 0)
		{
			dgd.set("var.gesamtflaeche", "0,0");
		}

		String addonflproz = (String)ht.get("addonflproz");
		if(null != addonflproz)
		{
			dgd.set("var.addonflproz", addonflproz);
		}
		else if(dgd.getString("var.addonflproz").length() == 0)
		{
			dgd.set("var.addonflproz", "0,0");
		}

		String gesamtflaechebrutto = (String)ht.get("gesamtflaechebrutto");
		if(null != gesamtflaechebrutto)
		{
			dgd.set("var.gesamtflaechebrutto", gesamtflaechebrutto);
		}
		else if(dgd.getString("var.gesamtflaechebrutto").length() == 0)
		{
			dgd.set("var.gesamtflaechebrutto", "0,0");
		}

		// System.err.println("ZLU2: KAEUFER:" + kaeufer);

		Hashtable statusSelectorValues = null;
		try
		{
			statusSelectorValues = parentObject.getValueMap("CIMS." + parentObject.flavour + ".TopStatusSelector");
			if(statusSelectorValues == null || statusSelectorValues.size() == 0)
			{
				statusSelectorValues = parentObject.getValueMap("CIMS.TopStatusSelector");
			}
		}
		catch(Exception ex)
		{}

		// we only update top if this rentroll is the most recent one
		if(!zl.isOldList())
		{
			String status = (String)ht.get("status");
			value = "";
			if(null != status)
			{
				Enumeration en = statusSelectorValues.keys();
				while(en.hasMoreElements())
				{
					String htkey = (String)en.nextElement();
					String htvalue = (String)statusSelectorValues.get(htkey);

					if(htkey.equalsIgnoreCase(status.toLowerCase()))
					{
						value = htvalue;
						break;
					}
				}

				if((value == null || value.length() == 0) && statusSelectorValues.containsKey(status.toLowerCase()))
				{
					value = status;
				}

				if(value != null)
				{
					dgd.set("var.status", value);
				}
				else
				{
					dgd.set("var.status", "");
				}
			}

			status = value;

			if(status != null && status.equalsIgnoreCase("sold"))
			{
				dgd.set("var.status", "-1"); // verkauft
			}
			else if(status == null || status.equals(""))
			{
				// top kommt -> auf im besitz setzen

				String oldstatus = (String)dgd.get("var.status");
				if(null == oldstatus)
				{
					oldstatus = "";
				}

				// mgo 20150708 - Status des Top beibehalten!
				// PKO 20170426 - Sehr eigenartiges Konstrukt -> Wozu wird das konkret verwendet? -> Status -3 hinzugefuegt
				// es sei denn es ist schon verkauft ....
				if(!oldstatus.equals("-1") && !oldstatus.equals("2"))
				{
					dgd.set("var.status", "1"); // im besitz
				}
			}
		}
		// END

		// System.out.println("Status: " + dgd.get("var.status") + " // TType: " + dgd.getTemplateType() + " // Name: " + dgd.get("var.name"));

		String ffl = (String)ht.get("ffl");
		if(null != ffl)
		{
			dgd.set("var.leerfl", ffl);
		}
		else
		{
			dgd.set("var.leerfl", "0,0");
		}

		String leerstehung = (String)ht.get("leerstehung");
		if(leerstehung != null)
		{
			dgd.set("var.leerstehung", leerstehung);
		}

		String bk = (String)ht.get("bk");
		if(null != bk)
		{
			dgd.set("var.betriebskosten", bk);
			addOriginalCurrencyValues("betriebskosten", "bk", dgd, ht);
			dgd.set("var.bk", bk);
			addOriginalCurrencyValues("bk", "bk", dgd, ht);
		}
		else
		{
			dgd.set("var.betriebskosten", "0,0");
			removeOriginalCurrencyValues("betriebskosten", dgd);
			dgd.set("var.bk", "0,0");
			removeOriginalCurrencyValues("bk", dgd);
		}

		String HMZ = (String)ht.get("hmz");

		// System.err.println("ZLU2: HMZ"+HMZ);
		if(null != HMZ)
		{
			dgd.set("var.istmietepm", HMZ);
			addOriginalCurrencyValues("istmietepm", "hmz", dgd, ht);

			dgd.set("var.hauptmietzins", HMZ);
			addOriginalCurrencyValues("hauptmietzins", "hmz", dgd, ht);

			// Wird im PreStore vom Top gesetzt - pko 20100217 sollmiete nicht korrekt gesetzt
			/*
			 * // SOLLMIETE nur schreiben, wenn >= ISTMIETE oder nicht gesetzt. String sHMZ = (String)dgd.get("var.sollmietepm"); if(null == sHMZ) sHMZ = ""; if(sHMZ.length() == 0) sHMZ = "0"; // nur
			 * wenn sollmiete<istmiete/monat try { long l_hmz = net.metamagix.essence.eSSENCETypes.Currency.getlong(HMZ); long l_sollhmz = net.metamagix.essence.eSSENCETypes.Currency.getlong(sHMZ);
			 * if(l_hmz > 0) { // nur ueberschreiben wenn ein hmz ist // System.err.println("ZLU2: Setting SollHMZ "+HMZ+" because l_hmz="+l_hmz); if(l_hmz > l_sollhmz) { dgd.set("var.sollmietepm",
			 * HMZ); dgd.set("var.sollhauptmietzins", HMZ); } } } catch(Exception xx) { debug.log(xx); }
			 */

		}

		String finanzierungist = (String)ht.get("finanzierungist");
		if(null != finanzierungist)
		{
			dgd.set("var.finanzierungist", finanzierungist);
		}

		String finanzierungsoll = (String)ht.get("finanzierungsoll");
		if(null != finanzierungsoll)
		{
			dgd.set("var.finanzierungsoll", finanzierungsoll);
		}

		String vertragsmiete2pm = (String)ht.get("vertragsmiete2pm");
		if(vertragsmiete2pm != null)
		{
			dgd.set("var.vertragsmiete2pm", vertragsmiete2pm);
			addOriginalCurrencyValues("vertragsmiete2pm", "vertragsmiete2pm", dgd, ht);
		}

		// Bei WAG wird die sollmietepm aus einer eigenen Spalte übernommen
		String sollmietepm = (String)ht.get("sollmietepm");
		if(sollmietepm != null)
		{
			dgd.set("var.sollmietepm", sollmietepm);
			addOriginalCurrencyValues("sollmietepm", "sollmietepm", dgd, ht);
		}
		else
		{
			// get sollmiete
			String sollMiete = (String)ht.get("sollmiete");
			if(sollMiete != null)
			{
				if(zl.isModifySollMiete())
				{
					// check if sollmiete > last sollmiete
					long oldSollHmz = getAsLong((String)dgd.get("var.sollmietepm"));
					long newSollHmz = getAsLong(sollMiete);
					if(newSollHmz > oldSollHmz)
					{
						dgd.set("var.sollmietepm", sollMiete);
						addOriginalCurrencyValues("sollmietepm", "sollmiete", dgd, ht);
						dgd.set("var.sollhauptmietzins", sollMiete);
						addOriginalCurrencyValues("sollhauptmietzins", "sollmiete", dgd, ht);
					}
				}
				else if(!zl.isModifySollMiete() && zl.isSetSollMiete())
				{
					if(dgd.getTemplateType().equals("CIMS.top"))
					{
						String sollmietepmTop = dgd.getString("var.sollmietepm");
						if(sollmietepmTop != null && sollmietepmTop.length() > 0)
						{
							ht.put("sollmiete", sollmietepmTop);
						}
						else
						{
							// set value
							dgd.set("var.sollmietepm", sollMiete);
							addOriginalCurrencyValues("sollhauptmietzins", "sollmiete", dgd, ht);
							dgd.set("var.sollhauptmietzins", sollMiete);
							addOriginalCurrencyValues("sollhauptmietzins", "sollmiete", dgd, ht);
						}
					}
				}

				else
				{
					// set value
					dgd.set("var.sollmietepm", sollMiete);
					addOriginalCurrencyValues("sollhauptmietzins", "sollmiete", dgd, ht);
					dgd.set("var.sollhauptmietzins", sollMiete);
					addOriginalCurrencyValues("sollhauptmietzins", "sollmiete", dgd, ht);
				}
			}
		}

		// PKO: 20171102 - Sollmiete soll gesetzt werden wenn noch keine vorhanden (parameter 'setsollmiete' setzt sollmiete in HT) -> Muss dann auch auf ZZ geschrieben werden
		if((dgd.getString("var.sollmietepm") == null || dgd.getString("var.sollmietepm").length() == 0) && ht.containsKey("sollmiete") && ht.get("sollmiete").toString().length() > 0)
		{
			dgd.set("var.sollmietepm", ht.get("sollmiete"));
		}

		// Set Date on Zinszeile
		try
		{
			String newYear = zl.jahr;
			String newMonth = zl.monat;
			if(newMonth.length() == 1)
			{
				newMonth = "0" + newMonth;
			}
			String newDate = "01." + newMonth + "." + newYear;
			dgd.set("var.datum", newDate);

		}
		catch(Exception e1)
		{
			// do nothing - date not exists on zinszeile
		}

		if(!parentObject.zinslistenImport.getZlTypeConfig().isDisablesollmietenbestandteile())
		{

			// PKO 20221115: 19517-Sollmiete Berechnung
			Vector sollmietenbestandteile = parentObject.xc.getSollmietenbestandteile();
			if(sollmietenbestandteile != null && sollmietenbestandteile.size() > 0)
			{

				BigDecimal sollmieteBD = new BigDecimal("0");

				for(int i = 0; i < sollmietenbestandteile.size(); i++)
				{
					if(ht.containsKey(sollmietenbestandteile.get(i)) && ht.get(sollmietenbestandteile.get(i)).toString().length() > 0)
					{
						BigDecimal bestandteil = new BigDecimal(ht.get(sollmietenbestandteile.get(i)).toString().replaceAll(",", "\\."));
						sollmieteBD = sollmieteBD.add(bestandteil);
					}
				}

				String sollmieteValue = sollmieteBD.toPlainString().replaceAll("\\.", ",");

				if(sollmieteValue != null)
				{
					dgd.set("var.sollmietepm", sollmieteValue);
					// addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);
					dgd.set("var.sollhauptmietzins", sollmieteValue);
					// addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);

					// sollmiete in den ht geben
					ht.put("sollmietepm", sollmieteValue);
					ht.put("sollmiete", sollmieteValue);

					// Sollmiete soll im Top.java dann auch nicht mehr bereinigt werden, weil das schon die korrekte Miete ist!!!
					dgd.set("var.ismodifysollmiete", "0");
				}
			}

		}

		if(CfgSingleton.getInstance().getBoolean("PM_SET_SOLLMIETE_TO_MARKTMIETE", Boolean.FALSE))
		{
			dgd.set("var.sollmietepm", dgd.getString("var.marktmietepm"));
			dgd.set("var.sollhauptmietzins", dgd.getString("var.marktmietepm"));
			// addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);

			// sollmiete in den ht geben
			ht.put("sollmietepm", dgd.getString("var.marktmietepm"));
			ht.put("sollmiete", dgd.getString("var.marktmietepm"));
		}
		else if(CfgSingleton.getInstance().getBoolean("PM_SET_SOLLMIETE_ON_LEERSTAND_TO_MARKTMIETE", Boolean.FALSE) && dgd.getBoolean("var.leerstehung", false))
		{
			dgd.set("var.sollmietepm", dgd.getString("var.marktmietepm"));
			dgd.set("var.sollhauptmietzins", dgd.getString("var.marktmietepm"));
			// addOriginalCurrencyValues("sollhauptmietzins", "hmz", dgd, ht);

			// sollmiete in den ht geben
			ht.put("sollmietepm", dgd.getString("var.marktmietepm"));
			ht.put("sollmiete", dgd.getString("var.marktmietepm"));
		}
		Top.setVertragsmiete(dgd);

		return dgd;
	}

		private OriginalCurrencyValue getOriginalCurrencyValue(String key, Hashtable ht)
	{
		try
		{
			if(CfgSingleton.getInstance().isMultiCurrencySystem())
			{
				BigDecimal baseValueBD = null;
				BigDecimal ovValueBD = null;
				Date odValueDate = null;
				BigDecimal oxValueBD = null;

				String ovValue = "";
				String ocValue = "";
				String oxValue = "";
				String odValue = "";

				if(ht.containsKey(key + " (ov)"))
				{
					ovValue = (String)ht.get(key + " (ov)");
				}
				else if(ht.containsKey(key + ".OV"))
				{
					if(ht.get(key + ".OV") instanceof BigDecimal)
					{
						ovValueBD = (BigDecimal)ht.get(key + ".OV");
					}
					else
					{
						ovValue = (String)ht.get(key + ".OV");
					}
				}

				if(ht.containsKey(key + " (oc)"))
				{
					ocValue = (String)ht.get(key + " (oc)");
				}
				else if(ht.containsKey(key + ".OC"))
				{
					ocValue = (String)ht.get(key + ".OC");
				}

				if(ht.containsKey(key + " (ox)"))
				{
					oxValue = (String)ht.get(key + " (ox)");
				}
				else if(ht.containsKey(key + ".OX"))
				{
					if(ht.get(key + ".OX") instanceof BigDecimal)
					{
						oxValueBD = (BigDecimal)ht.get(key + ".OX");
					}
					else
					{
						oxValue = (String)ht.get(key + ".OX");
					}
				}

				if(ht.containsKey(key + " (od)"))
				{
					odValue = (String)ht.get(key + " (od)");
				}
				else if(ht.containsKey(key + ".OD"))
				{
					if(ht.get(key + ".OD") instanceof Date)
					{
						odValueDate = (Date)ht.get(key + ".OD");
					}
					else
					{
						odValue = (String)ht.get(key + ".OD");
					}
				}

				if(ovValue.length() == 0 && ocValue.length() == 0 && oxValue.length() == 0 && odValue.length() == 0)
				{
					return null;
				}

				String value = (String)ht.get(key);
				if(value != null && value.length() > 0)
				{
					baseValueBD = new BigDecimal(value.replaceAll(",", "\\."));
				}

				if(ovValue != null && ovValue.length() > 0)
				{
					ovValueBD = new BigDecimal(ovValue.replaceAll(",", "\\."));
				}

				if(oxValue != null && oxValue.length() > 0)
				{
					oxValueBD = new BigDecimal(oxValue.replaceAll(",", "\\."));
				}

				if(odValueDate == null)
				{
					odValueDate = net.metamagix.essence.eSSENCETypes.eDate.dateFromString(odValue);
				}

				OriginalCurrencyValue ocv = new OriginalCurrencyValue(baseValueBD, ocValue, ovValueBD, odValueDate, oxValueBD);
				if(ocv.isOk())
				{
					return ocv;
				}
			}
			return null;
		}
		catch(Exception e)
		{
			debug.error(e);
			return null;
		}

	}

	/**
	 * update dgd with zz value.
	 *
	 * @param targetname
	 *            name of targetfield
	 * @param sourcename
	 *            name of sourcefield
	 * @param default_on_empty_or_null
	 *            what to do when null
	 * @param dgd
	 *            dgd to write

	 * @param ht
	 *            Hashtable containing source values
	 * @return Updated DynGenDataObj with written value
	 */
		private DynGenDataObj writeZZValue2DGD(String targetname, String sourcename, String default_on_empty_or_null, DynGenDataObj dgd, Hashtable ht)
	{
		String oldval = (String)dgd.get("var." + targetname);

		String newval = (String)ht.get(sourcename);
		if(StringUtils.isAllBlank(oldval, newval) && StringUtils.isNotBlank(default_on_empty_or_null))
		{
			newval = default_on_empty_or_null;
		}
		if(null != newval)
		{
			// bugfix: https://pm.metamagix.net/issues/11732 ustsatz beim import nicht richtig uebersetzt
			String selector = dgd.getString("var." + targetname + ".SELECTOR");
			if(selector.equals("ICRScrm.UstsatzSelector"))
			{
				newval = getCorrectedUstSatz(newval, dgd);
			}
			dgd.set("var." + targetname, newval);
		}
		return dgd;
	}

		private String getLatestMVDatenFromMietvertrag(String dgdId)
	{
		try
		{
			String[] oids_top = new String[1];
			oids_top[0] = dgdId;

			for(int i = 0; i < oids_top.length; i++)
			{
				if(null == parentObject.topsCache)
				{
					parentObject.topsCache = new HashMap();
				}
				if(!parentObject.topsCache.containsKey(oids_top[i]))
				{
					// topscache irgendwie kapputt?
					debug.error(this, "bad top im topscache: " + oids_top[i]);
				}
			}

			Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();

			Hashtable<String, Object> args = new HashMap<String, Object>();

			args.put("advancedfields", "TRUE");

			String mydom = (String)session.get("domainid");
			if(mydom.length() == 0)
			{
				args.put("DOMAIN", "ALLDOMAINS");
			}
			else
			{
				args.put("DOMAIN", mydom);
			}

			args.put("TType", "CIMS.mietvertrag");
			args.put("tops_templatetype", "CIMS.top");
			args.put("fieldClause", "ID,ET0.vertragid,ET0.kuendigungsdatum,ET0.mietvertragbis,tops_name topname, tops_ID topid");
			args.put("tops_id", oids_top);
			// args.put("tops_OUTERJOIN", "TRUE");
			args.put("orderClause", "ET0.mietvertragvon DESC");

			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			QueryResult qr = parentObject.DAInst.queryObjectWithResult(args);
			res1 = qr.getResult();

			if(res1.size() > 0)
			{
				Hashtable h = res1.elementAt(0);
				if(h != null)
				{
					String mietvertragvon = (String)h.get("kuendigungsdatum");
					if(mietvertragvon == null || mietvertragvon.length() == 0)
					{
						mietvertragvon = (String)h.get("mietvertragbis");
					}

					DateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);

					Date dateMv = null;
					if(mietvertragvon.length() > 0)
					{
						dateMv = format.parse(mietvertragvon);
					}
					try
					{
						mietvertragvon = format.format(dateMv);
					}
					catch(Exception e)
					{
						mietvertragvon = "";
					}

					return mietvertragvon;
				}
			}

		}
		catch(Exception e)
		{
			debug.error(e);
		}

		// TODO Auto-generated method stub
		return null;
	}

		private Hashtable<String, String> getLatestIndexDatumFromZZ(String adresse, String topname, String jahr, String monat, String mietvertragvonZL)
	{

		Hashtable<String, String> indexHashtoTop = new HashMap<String, String>();
		try
		{
			Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();
			Hashtable<String, Object> args = new HashMap<String, Object>();

			StringBuffer sqlbuffer = new StringBuffer();

			args.put("TType", "CIMS.top");
			args.put("REVtops_templatetype", "CIMS.haus");
			args.put("fieldClause", "DOB.id, ET0.id topid");
			args.put("REVtops_ID", adresse);
			args.put("name", topname);
			args.put("whereClause", "ET0.id is not null");
			String mydom = (String)session.get("domainid");
			if(mydom.length() == 0)
			{
				args.put("DOMAIN", "ALLDOMAINS");
			}
			else
			{
				args.put("DOMAIN", mydom);
			}
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			QueryResult qr = parentObject.DAInst.queryObjectWithResult(args);
			res1 = qr.getResult();
			String topid = null;
			if(res1.size() > 0)
			{
				Hashtable h = res1.elementAt(0);
				if(h != null)
				{
					topid = (String)h.get("topid");
				}
			}

			if(topid != null && mietvertragvonZL != null && mietvertragvonZL.length() > 0)
			{
				if(mietvertragvonZL.matches("\\d\\d[.]\\d\\d[.]\\d\\d\\d\\d") || mietvertragvonZL.matches("\\d\\d[.]\\d\\d[.]\\d\\d"))
				{
					String[] tmpDate = mietvertragvonZL.split("[.]");
					if(tmpDate[2].length() == 2)
					{
						if(Integer.parseInt(tmpDate[2]) > 50)
						{
							mietvertragvonZL = tmpDate[0] + "." + tmpDate[1] + ".19" + tmpDate[2];
						}
						else
						{
							mietvertragvonZL = tmpDate[0] + "." + tmpDate[1] + ".20" + tmpDate[2];
						}
					}
				}
			}

			Vector<Hashtable<String, String>> res2 = new Vector<Hashtable<String, String>>();
			Hashtable<String, Object> args2 = new HashMap<String, Object>();
			args2.put("advancedfields", "TRUE");
			args2.put("TType", "CIMS.zinszeile");
			args2.put("top_templatetype", "CIMS.top");
			args2.put("top_id", topid);
			args2.put("fieldClause", "ID,ET0.indexdatum,ET0.indexbasis,ET0.datum");
			String whereClause = "";
			if(mietvertragvonZL != null)
			{
				Date ed = eDate.dateFromString(mietvertragvonZL);
				Calendar cal = Calendar.getInstance();
				cal.setTime(ed);
				String sqlstyle = (String)CfgSingleton.getInstance().get("sqlstyle");
				String mietvertragvonformatted = DateTime.getSqlDateString(cal, sqlstyle);
				whereClause = "ET0.mietvertragvon = " + mietvertragvonformatted + " and ";
			}
			if(monat != null && jahr != null)
			{
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.MONTH, Integer.valueOf(monat) - 1);
				cal.set(Calendar.YEAR, Integer.valueOf(jahr));
				cal.set(Calendar.DAY_OF_MONTH, 1);
				cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
				Date minzzdatum = cal.getTime();
				String sqlstyle = (String)CfgSingleton.getInstance().get("sqlstyle");
				String zldateformatted = DateTime.getSqlDateString(cal, sqlstyle);
				whereClause = whereClause + " ET0.datum <= " + zldateformatted + " and ";
			}
			whereClause = whereClause + " ET0.mietvertragvon < ET0.indexdatum";
			args2.put("whereClause", whereClause);

			args2.put("orderClause", "jahr*12+monat DESC");
			String mydom2 = (String)session.get("domainid");
			if(mydom2.length() == 0)
			{
				args2.put("DOMAIN", "ALLDOMAINS");
			}
			else
			{
				args2.put("DOMAIN", mydom2);
			}
			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			QueryResult qr2 = parentObject.DAInst.queryObjectWithResult(args2);
			res2 = qr2.getResult();
			if(res2.size() > 0)
			{
				Hashtable h = res2.elementAt(0);
				if(h != null)
				{
					String indexdatum = (String)h.get("indexdatum");
					String indexbasis = (String)h.get("indexbasis");
					indexHashtoTop.put("indexdatum", indexdatum);
					indexHashtoTop.put("indexbasis", indexbasis);
					return indexHashtoTop;
				}
			}

		}
		catch(Exception e)
		{
			debug.error(e);
		}

		return null;

	}

		private void addOriginalCurrencyValues(String targetFieldName, String sourceFieldName, DynGenDataObj target, DynGenDataObj source)
	{
		String targetFieldNameOrigValue = targetFieldName + ".OV";
		String targetFieldNameOrigCurr = targetFieldName + ".OC";
		String targetFieldNameOrigCurrExchangeRate = targetFieldName + ".OX";
		String targetFieldNameOrigDate = targetFieldName + ".OD";
		String sourceFieldNameOrigValue = sourceFieldName + ".OV";
		String sourceFieldNameOrigCurr = sourceFieldName + ".OC";
		String sourceFieldNameOrigCurrExchangeRate = targetFieldName + ".OX";
		String sourceFieldNameOrigDate = targetFieldName + ".OD";

		String sourceFieldOrigValue = source.getString("var." + sourceFieldNameOrigValue).trim();
		String sourceFieldOrigCurr = source.getString("var." + sourceFieldNameOrigCurr).trim();
		String sourceFieldOrigCurrExchangeRate = source.getString("var." + sourceFieldNameOrigCurrExchangeRate).trim();
		String sourceFieldOrigDate = source.getString("var." + sourceFieldNameOrigDate).trim();
		if(sourceFieldOrigValue.length() > 0 && sourceFieldOrigCurr.length() > 0 && sourceFieldOrigCurrExchangeRate.length() > 0)
		{
			target.set("var." + targetFieldNameOrigValue, sourceFieldOrigValue);
			target.set("var." + targetFieldNameOrigCurr, sourceFieldOrigCurr);
			target.set("var." + targetFieldNameOrigCurrExchangeRate, sourceFieldOrigCurrExchangeRate);
			target.set("var." + targetFieldNameOrigDate, sourceFieldOrigDate);

		}
		else
		{
			removeOriginalCurrencyValues(targetFieldNameOrigValue, target);
		}

	}

		private void addOriginalCurrencyValues(String targetFieldName, String sourceFieldName, DynGenDataObj target, Hashtable source)
	{
		// Sauberes Mullticurrencyhandling prüft ob die nötigen Währungsfelder vorhanden sind
		if(CfgSingleton.getInstance().isMultiCurrencySystem())
		{
			OriginalCurrencyValue ocv = getOriginalCurrencyValue(sourceFieldName, source);
			if(ocv != null && ocv.isOk())
			{
				target.setMoney("var." + targetFieldName, ocv);
				return;
			}
		}

		// Lagacy Code - vermutlich Gryphon - fallback wenn unvollständige Währungsdaten
		String targetFieldNameOrigValue = targetFieldName + ".OV";
		String targetFieldNameOrigCurr = targetFieldName + ".OC";
		String targetFieldNameOrigDate = targetFieldName + ".OD";
		String targetFieldNameOrigCurrExchangeRate = targetFieldName + ".OX";
		String sourceFieldNameOrigValue = sourceFieldName + ".OV";
		String sourceFieldNameOrigCurr = sourceFieldName + ".OC";
		String sourceFieldNameOrigDate = sourceFieldName + ".OD";
		String sourceFieldNameOrigCurrExchangeRate = sourceFieldName + ".OX";

		String sourceFieldOrigValue = "";
		if(source.get(sourceFieldNameOrigValue) instanceof BigDecimal)
		{
			sourceFieldOrigValue = ((BigDecimal)source.get(sourceFieldNameOrigValue)).toPlainString().replaceAll("\\.", ",");
		}
		else
		{
			sourceFieldOrigValue = (String)source.get(sourceFieldNameOrigValue);
		}

		if(null == sourceFieldOrigValue)
		{
			sourceFieldOrigValue = "";
		}

		String sourceFieldOrigCurr = (String)source.get(sourceFieldNameOrigCurr);
		if(null == sourceFieldOrigCurr)
		{
			sourceFieldOrigCurr = "";
		}

		String sourceFieldOrigDate = "";
		if(source.get(sourceFieldNameOrigDate) instanceof Date)
		{
			sourceFieldOrigDate = new SimpleDateFormat("dd.MM.yyyy").format((Date)source.get(sourceFieldNameOrigDate));
		}
		else
		{
			sourceFieldOrigDate = (String)source.get(sourceFieldNameOrigDate);
			if(null == sourceFieldOrigDate)
			{
				sourceFieldOrigDate = "";
			}
		}

		String sourceFieldOrigCurrExchangeRate = "";
		if(source.get(sourceFieldNameOrigValue) instanceof BigDecimal)
		{
			sourceFieldOrigCurrExchangeRate = ((BigDecimal)source.get(sourceFieldNameOrigCurrExchangeRate)).toPlainString().replaceAll("\\.", ",");
		}
		else
		{
			sourceFieldOrigCurrExchangeRate = (String)source.get(sourceFieldNameOrigCurrExchangeRate);
		}

		if(null == sourceFieldOrigCurrExchangeRate)
		{
			sourceFieldOrigCurrExchangeRate = "";
		}
		if(sourceFieldOrigValue.length() > 0 && sourceFieldOrigCurr.length() > 0 && sourceFieldOrigCurrExchangeRate.length() > 0)
		{
			target.set("var." + targetFieldNameOrigValue, sourceFieldOrigValue);
			target.set("var." + targetFieldNameOrigCurr, sourceFieldOrigCurr);
			target.set("var." + targetFieldNameOrigCurrExchangeRate, sourceFieldOrigCurrExchangeRate);
			target.set("var." + targetFieldNameOrigDate, sourceFieldOrigDate);
		}
		else
		{
			removeOriginalCurrencyValues(targetFieldNameOrigValue, target);
		}
	}

		private void removeOriginalCurrencyValues(String fieldname, DynGenDataObj dgd)
	{
		dgd.del(fieldname + ".OV");
		dgd.del(fieldname + ".OC");
		dgd.del(fieldname + ".OX");
	}

		private String getCorrectedUstSatz(String umsatzvalue, DynGenDataObj dgd)
	{
		String correctedValue = UstsatzSelector.getAlternativeCorrected(umsatzvalue, session);
		if(correctedValue == null)
		{
			if(!CoolStringTool.empty(umsatzvalue) && null == correctedValue)
			{
				BugMe.getInstance().error(this, "ATTENTION - Correction of Ust-Satz " + umsatzvalue + " went wrong - returned empty value!");
			}
			correctedValue = "";
		}
		return correctedValue;
	}

		private long getAsLong(String value)
	{
		if(value != null)
		{
			if(value.equals(""))
			{
				value = "0";
			}
			return net.metamagix.essence.eSSENCETypes.Currency.getlong(value);
		}
		return 0;
	}

		public boolean deleteZinsZeilen(String[] topoids, String jahr, String monat)
	{
		boolean delres = true;
		Hashtable args = new HashMap();
		Vector res = new ArrayList();
		args.put("TType", "CIMS.zinszeile");
		// fieldClause ... Felder zum holen ,-separiert
		args.put("fieldClause", "DOB.ID zzid,name,DDT1.ID topid");
		args.put("top_ID", topoids);
		args.put("jahr", jahr);
		args.put("monat", monat);

		// BAUSTELLE
		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			args.put("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			args.put("DOMAIN", mydom);
		}
		// new Connector Class
		if(null == parentObject.DAInst)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			parentObject.DAInst = conn.getDataAgent();
		}
		// java.util.Date start_time = new java.util.Date();

		try
		{
			res = parentObject.DAInst.queryObject(args);
		}
		catch(Exception x)
		{

		}
		// java.util.Date end_time = new java.util.Date();
		// long run_time = end_time.getTime() - start_time.getTime();

		// Hashtable top2zz = new HashMap();
		// System.err.println("ZLU2: res size is "+res.size());
		for(int x = 0; x < res.size(); x++)
		{
			Hashtable h = (Hashtable)res.elementAt(x);
			if(h != null)
			{
				String topid = (String)h.get("topid");
				String zzid = (String)h.get("zzid");
				if(null != topid)
				{
					if(null != zzid)
					{
						try
						{
							if(!parentObject.DAInst.deleteObject(zzid, session))
							{
								// ERROR
								delres = false;
								session.set("ERROR.MAIN", Tr.t("textErrorDeleteRentRoll1", session.getString("language")) + " " + zzid + " " + Tr.t("textErrorDeleteRentRoll2", session.getString("language")));
							}
						}
						catch(Exception de)
						{
							debug.log(de);
							session.set("ERROR.MAIN", Tr.t("textErrorDeleteRentRoll1", session.getString("language")) + " " + zzid + " " + Tr.t("textErrorDeleteRentRoll2", session.getString("language")));
						}
					}
				}
			}
		}
		return delres;
	}

		public Hashtable zinszeilenAnlegen(Zinsliste zl, TopList top_list, String hausOid, boolean appendToHTMLResult)
	{
		Date d1 = new Date();

		if(parentObject.enableDetailedLogging)
		{
			parentObject.starttime = System.currentTimeMillis();
		}

		try
		{
			Hashtable zz2store = new HashMap();
			// gibt es die zz schon?
			// wie sehen die vormonatszinszeilen aus

			String[] tarr = top_list.getTopIDs();
			System.err.println("TOP LIST before zinszeilenAnlegen:");
			System.err.println(top_list.toString());

			// VORJAHR
			int vormonat = Integer.parseInt(zl.monat) - 1;
			int vorjahr = Integer.parseInt(zl.jahr);
			if(vormonat <= 0)
			{
				vormonat = 12;
				vorjahr = vorjahr - 1;
			}

			Hashtable zzeilen_existing = new HashMap();
			// wenn alte loeschen ... hier löschen
			// loeschen alter zinszeilen
			if(getBoolean("var.altezinszeilenloeschen", Boolean.FALSE))
			{
				TopoTool.deleteZinsZeilenFromHaus(hausOid, zl.jahr, zl.monat, session);
			}
			else
			{
				zzeilen_existing = getZinsZeilen(tarr, "" + zl.jahr, "" + zl.monat);
			}

			// Wenn in der zinslistenconfig.xml "Mieterwechsel == true" dann auf jeden Fall die Zeilen mit $mieterwechsel == 1 loeschen
			if(zl.isImportmieterwechsel())
			{
				TopoTool.deleteZinsZeilenFromHaus(hausOid, zl.jahr, zl.monat, session, Boolean.TRUE);
			}

			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}

			// STORE THREADS FUER TOPS UND STELLPLAETZE
			// Magic.System.StoreThread myStoreThreadTops = new Magic.System.StoreThread(parentObject.DAInst, session);
			// Magic.System.StoreThread myStoreThreadStellplaetze = new Magic.System.StoreThread(parentObject.DAInst, session);
			CoolBulkStoreTool cbst = new CoolBulkStoreTool(25, parentObject.DAInst, session);
			System.out.println("ZZ Top size: " + zl.zinszeilen.size());
			for(int j = 0; j < zl.zinszeilen.size(); j++)
			{

				Hashtable ht = (Hashtable)zl.zinszeilen.get(j);
				String top = (String)ht.get("top");
				String tmieter = (String)ht.get("mieter");
				if(null != tmieter && tmieter.indexOf("BAT") >= 0)
				{
					System.err.println("I HAVE GOT TOP FOR ZZ CREATE  " + top);
				}

				TopElement te = null;
				// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
				String sapnummer = "";
				if(te == null && ht.containsKey("sapnummer"))
				{
					sapnummer = (String)ht.get("sapnummer");
					te = top_list.getTopBySapnummer(sapnummer);
				}

				// try fallback to topname
				if(te == null)
				{
					te = top_list.getTop(top);
				}

				// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
				if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
				{
					String tmpTop = top.substring(0, top.indexOf(" |"));
					te = top_list.getTop(tmpTop);
				}

				if(te != null)
				{
					System.out.println("" + j + ". TopId/Topname/Sapnummer: " + te.id + "/" + top + "/" + sapnummer);
				}
				else
				{
					System.out.println("" + j + ". TopId/Topname/Sapnummer: " + "NULL/" + top + "/" + sapnummer);
				}

				String status = (String)ht.get("status");
				if(null == status)
				{
					status = "";
				}

				boolean write_it = false;
				if(null != te)
				{
					// // top ist im besitz
					// if(te.isBought())
					// mgo 20150629 - nicht nur gekaufte, sondern auch angemietete oder geplante Einheiten importieren!
					if(te.isActive())
					{
						// wir importieren in der aktuellen Periode keine Zinslisten zu zusammengelegten Tops, ausser der User hat das angefordert
						write_it = true;
					}
					else if(zl.isOldList())
					{
						// wir importieren in der vergangenen Periode auch Zinslisten zu zusammengelegten Tops,
						write_it = true;

					}
					else if(status.length() > 0)
					{
						write_it = true;
					}

					// PKO 20170405 ## 8575-Alter Rentroll Import auf Aldabra ZZ nicht angelegt
					if(!write_it)
					{
						// System.out.println("Write Tops in every status: " + top);
						write_it = true;
					}
				}

				if(write_it)
				{ // das top gibt es und wir duerfen schreiben ....

					// create zinszeile - gibt es sie schon?
					Hashtable mh = (Hashtable)zzeilen_existing.get(te.id);

					String old_zz = null;
					if(null != mh)
					{
						old_zz = (String)mh.get("zzid");
					}

					// Gibt es schon eine Zinszeile ja/nein
					if(null != old_zz)
					{
						// wir ueberschreiben eine alte zinszeile!!!
						if(appendToHTMLResult)
						{
							parentObject.zlprotocol.appendHtmlRes(Tr.t("textRentRollForRentalUnit1", session.getString("language")) + " " + top + " " + Tr.t("textRentRollForRentalUnit2", session.getString("language")) + "<br>\n");
						}
						parentObject.log("Zinszeile für Mieteinheit/Top " + top + " überschrieben.");
						// DynGenDataObj oldzzdgd = (DynGenDataObj)parentObject.zinsZeilenCache.get(old_zz);
						// ZINSZEILE SCHREIBEN
						DynGenDataObj dgdzz = createZZ(ht, zl, te.id, old_zz);

						// System.out.println("TemplateType: " + dgdzz.getTemplateType() + " LS-art:" + dgdzz.getString("var.leerstehungsart") + " LS-Subart:" +
						// dgdzz.getString("var.leerstehungssubart"));

						if(null != dgdzz)
						{
							// if(oldzzdgd != null && !dgdzz.equals(oldzzdgd))
							// {
							// // TOP SCHREIBEN
							// DynGenDataObj tos = updateTopOrStellplatz(te.id, ht, zl, true, cbst);
							// // ZZ an Top anpassen
							// dgdzz = setZZExtras(dgdzz, tos, zl);
							// dgdzz = setSelectedValuesFromPreviousZZ(dgdzz, oldzzdgd);
							//
							zz2store.put(old_zz, dgdzz);
							// System.err.println("ZLU2: top zinszeile changed."); // + md5new + "!=" + md5old);
							// }
							// else
							// {
							// TOP nochmal SCHREIBEN
							updateTopOrStellplatz(te.id, ht, zl, true, cbst);
							// }
							overwritezz++;
						}
					}
					else
					{
						// ZINSZEILE SCHREIBEN
						DynGenDataObj dgdzz = createZZ(ht, zl, te.id, old_zz);
						if(null != dgdzz)
						{
							// TOP SCHREIBEN
							DynGenDataObj tos = updateTopOrStellplatz(te.id, ht, zl, true, cbst);

							// ZZ an Top anpassen
							dgdzz = setZZExtras(dgdzz, tos, zl);

							// DynGenDataObj previousZZ = getPreviousZZ(te.id, zl);
							//
							// if(previousZZ != null)
							// {
							// dgdzz = setSelectedValuesFromPreviousZZ(dgdzz, previousZZ);
							// }

							zz2store.put("NEW" + createzz, dgdzz);
							createzz++;
						}
					}

					zzeilen_existing.remove(te.id);
				}

			}

			try
			{
				if(parentObject.enableDetailedLogging)
				{
					parentObject.starttime = System.currentTimeMillis();
				}
				cbst.finish();
				if(parentObject.enableDetailedLogging)
				{
					parentObject.endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log cbst.finish(); " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
					parentObject.starttime = System.currentTimeMillis();
				}
			}
			catch(Exception rex)
			{
				debug.log(rex);
			}

			System.out.println("ZZ Stellplatz size: " + zl.zinszeilen.size());
			for(int j = 0; j < zl.stellplaetze.size(); j++)
			{
				Hashtable ht = (Hashtable)zl.stellplaetze.get(j);
				String top = (String)ht.get("top");
				String tmieter = (String)ht.get("mieter");
				if(null != tmieter && tmieter.indexOf("BAT") >= 0)
				{
					System.err.println("I HAVE GOT TOP FOR ZZ CREATE  " + top);
				}

				TopElement te = null;
				// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
				String sapnummer = "";
				if(te == null && ht.containsKey("sapnummer"))
				{
					sapnummer = (String)ht.get("sapnummer");
					te = top_list.getTopBySapnummer(sapnummer);
				}

				// try fallback to topname
				if(te == null)
				{
					te = top_list.getTop(top);
				}

				// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
				if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
				{
					String tmpTop = top.substring(0, top.indexOf(" |"));
					te = top_list.getTop(tmpTop);
				}

				String status = (String)ht.get("status");
				if(null == status)
				{
					status = "";
				}

				boolean write_it = false;
				if(null != te)
				{
					// // top ist im besitz
					// if(te.isBought())
					// mgo 20150629 - nicht nur gekaufte, sondern auch angemietete oder geplante Einheiten importieren!
					if(te.isActive())
					{
						write_it = true;
					}
					else if(zl.isOldList())
					{
						write_it = true;
					}
					else if(status.length() > 0)
					{
						write_it = true;
					}

					// PKO 20170405 ## 8575-Alter Rentroll Import auf Aldabra ZZ nicht angelegt
					if(!write_it)
					{
						// System.out.println("Write Stellplatz in every status: " + top);
						write_it = true;
					}
				}

				if(write_it)
				{ // das top gibt es und wir duerfen schreiben ....
					// gibt es sie schon?
					Hashtable mh = (Hashtable)zzeilen_existing.get(te.id);
					String old_zz = null;
					if(null != mh)
					{
						old_zz = (String)mh.get("zzid");
					}

					// Gibt es schon eine Zinszeile ja/nein
					if(null != old_zz)
					{
						// wir ueberschreiben eine alte zinszeile!!!
						if(appendToHTMLResult)
						{
							parentObject.zlprotocol.appendHtmlRes(Tr.t("textRentRollForParkingSpace1", session.getString("language")) + " " + top + " " + Tr.t("textRentRollForParkingSpace2", session.getString("language")) + "<br>\n");
						}
						parentObject.log("Zinszeile für Stellplatz " + top + " überschrieben.<br>\n");

						DynGenDataObj dgdzz = createZZ(ht, zl, te.id, old_zz);

						if(null != dgdzz)
						{
							// TOP nochmal SCHREIBEN
							zz2store.put(old_zz, dgdzz);
							updateTopOrStellplatz(te.id, ht, zl, false, cbst);
							overwritezz++;
						}
					}
					else
					{
						// ZINSZEILE SCHREIBEN
						DynGenDataObj dgdzz = createZZ(ht, zl, te.id, old_zz);
						if(null != dgdzz)
						{
							DynGenDataObj tos = updateTopOrStellplatz(te.id, ht, zl, false, cbst);
							// ZZ an Top anpassen
							dgdzz = setZZExtras(dgdzz, tos, zl);

							zz2store.put("NEW" + createzz, dgdzz);
							createzz++;
						}
					}
					zzeilen_existing.remove(te.id);
				}
				// Junk store for stellplaetze
			}

			try
			{
				if(parentObject.enableDetailedLogging)
				{
					parentObject.starttime = System.currentTimeMillis();
				}
				cbst.finish();
				if(parentObject.enableDetailedLogging)
				{
					parentObject.endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log cbst.finish(); " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
					parentObject.starttime = System.currentTimeMillis();
				}
			}
			catch(Exception rex)
			{
				debug.log(rex);
			}

			if(parentObject.enableDetailedLogging)
			{
				parentObject.endtime = System.currentTimeMillis();
				BugMe.getInstance().log("############ Log zinszeilenAnlegen 2: " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
				parentObject.starttime = System.currentTimeMillis();
			}

			// update haus-name
			updateHausName(zl.haus, hausOid, zl);

			return zz2store;
		}
		catch(Exception ex)
		{
			debug.log(ex);
			debug.error(ex);
			return new HashMap();
		}
	}

	/**

		private void updateHausName(String hausName, String hausOid, Zinsliste zl)
	{
		if(parentObject.enableDetailedLogging)
		{
			parentObject.endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateHausName 1: " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
			parentObject.starttime = System.currentTimeMillis();
		}

		DynGenDataObj hausObj = null;
		boolean dostore = false;
		if(null != hausOid && hausOid.length() > 0)
		{
			try
			{
				hausObj = (DynGenDataObj)parentObject.DAInst.getObject(hausOid, "");
				// identadresse5 setzen
				HausData hausData = HausUtil.getHausData(hausName);
				if(hausData != null && hausData.hasEdvNr())
				{
					if(zl.zinszeilen.size() > 0)
					{
						Hashtable ht = (Hashtable)zl.zinszeilen.get(0);
						if(ht.containsKey("hausadresse") && ht.get("hausadresse").toString().length() > 0)
						{
							// Wenn dieser parameter auf False, dann soll der Objektname nicht gesetzt werden
							// mit Boolean.TRUE wird default TRUE für alle cfg gesetzt.
							boolean shouldUpdate = CfgSingleton.getInstance().getBoolean("PM_UPDATE_HAUSADRESSE", Boolean.TRUE);
						boolean hasExistingName = !hausObj.getString("var.name").trim().equals("");
						if(!shouldUpdate && hasExistingName)
							{
								// do nothing
							}
							else
							{
								hausObj.set("var.name", ht.get("hausadresse"));
								dostore = true;
							}
						}
						if(ht.containsKey("hausort") && ht.get("hausort").toString().length() > 0)
						{
							hausObj.set("var.ort", ht.get("hausort"));
							dostore = true;
						}
						if(ht.containsKey("hausplz") && ht.get("hausplz").toString().length() > 0)
						{
							hausObj.set("var.plz", ht.get("hausplz"));
							dostore = true;
						}
					}
					else if(zl.stellplaetze.size() > 0)
					{
						Hashtable ht = (Hashtable)zl.stellplaetze.get(0);

						if(ht.containsKey("hausadresse") && ht.get("hausadresse").toString().length() > 0)
						{
							// Wenn dieser parameter auf False, dann soll der Objektname nicht gesetzt werden
							// mit Boolean.TRUE wird default TRUE für alle cfg gesetzt.
							boolean shouldUpdate = CfgSingleton.getInstance().getBoolean("PM_UPDATE_HAUSADRESSE", Boolean.TRUE);
						boolean hasExistingName = !hausObj.getString("var.name").trim().equals("");
						if(!shouldUpdate && hasExistingName)
							{
								// do nothing
							}
							else
							{
								hausObj.set("var.name", ht.get("hausadresse"));
								dostore = true;
							}
						}
						if(ht.containsKey("hausort") && ht.get("hausort").toString().length() > 0)
						{
							hausObj.set("var.ort", ht.get("hausort"));
							dostore = true;
						}
						if(ht.containsKey("hausplz") && ht.get("hausplz").toString().length() > 0)
						{
							hausObj.set("var.plz", ht.get("hausplz"));
							dostore = true;
						}
					}

					String edvNr = hausData.getEdvNr();
					// store as identadresse5
					String identadresse5 = (String)hausObj.get("var.identadresse5");
					if(identadresse5 == null || !identadresse5.equals(edvNr))
					{
						hausObj.set("var.identadresse5", edvNr);
						dostore = true;
					}

					if(dostore)
					{
						parentObject.DAInst.storeObject(hausObj, "CIMS.haus", hausOid, session);
					}
				}
			}
			catch(Exception xc)
			{
				parentObject.log("Unbekanntes Haus mit ID " + hausOid);
				debug.log(parentObject.xc);
			}
		}

		if(parentObject.enableDetailedLogging)
		{
			parentObject.endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateHausName 2: " + ((parentObject.endtime - parentObject.starttime) / 1000) + " seconds");
			parentObject.starttime = System.currentTimeMillis();
		}
	}


		public DynGenDataObj setSelectedValuesFromPreviousZZ(DynGenDataObj dgdzz, DynGenDataObj oldzzdgd)
	{
		// PKO 20170215 ## 8595-Fwd: AW: Quartalsvergleich Q3 2016 und Q4 2016
		String leerstehungsart = oldzzdgd.getString("var.leerstehungsart");
		dgdzz.set("var.leerstehungsart", leerstehungsart);

		// PKO 20201001
		BigDecimal sollmiete = dgdzz.getBigDecimal("var.sollhauptmietzins", 2);
		if(sollmiete.compareTo(BigDecimal.ZERO) == 0)
		{
			dgdzz.set("var.sollhauptmietzins", oldzzdgd.get("var.sollhauptmietzins"));
		}

		BigDecimal zielmiete = dgdzz.getBigDecimal("var.zielhauptmietzins", 2);
		if(zielmiete.compareTo(BigDecimal.ZERO) == 0)
		{
			dgdzz.set("var.zielhauptmietzins", oldzzdgd.get("var.zielhauptmietzins"));
		}

		return dgdzz;
	}

		public DynGenDataObj setZZExtras(DynGenDataObj dgdzz, DynGenDataObj tos, Zinsliste zl)
	{
		if(null == tos)
		{
			return dgdzz;
		}

		// ZZ mit sollwerten schreiben ...
		String shmz = (String)tos.get("var.sollmietepm");
		if(null == shmz)
		{
			shmz = (String)tos.get("var.sollhauptmietzins");
		}
		if(null != shmz)
		{
			dgdzz.set("var.sollmietepm", shmz);
			dgdzz.set("var.sollhauptmietzins", shmz);
		}
		String zhmz = (String)tos.get("var.zielmietepm");
		if(null == zhmz)
		{
			zhmz = (String)tos.get("var.zielhauptmietzins");
		}
		if(null != zhmz)
		{
			dgdzz.set("var.zielmietepm", zhmz);
			dgdzz.set("var.zielhauptmietzins", zhmz);
		}

		// PKO 20170217 ## 0023544-Tenant Company in Rent Roll Import automatisiert aus Rental Unit uebernehmen
		try
		{
			if(parentObject.flavour.equals("icrssom"))
			{
				if(dgdzz.getSlot("slot.mieterfirma") == null || dgdzz.getSlot("slot.mieterfirma").size() == 0)
				{
					dgdzz.set("slot.mieterfirma", tos.getSlot("slot.mieterfirma", new Slot()));
				}
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}

		try
		{
			// wir nehmen die nutzung aus dem top? wenn sie befüllt ist
			if(parentObject.zinslistenImport.getZlTypeConfig().isOverrideNutzung())
			{
				String topnutzung = tos.getString("var.nutzung");
				if(topnutzung.length() > 0)
				{
					parentObject.log("Nutzung Zinszeile wird mit Topnutzung " + topnutzung + " ueberschieben, Top " + dgdzz.getName() + zl.plz + " " + zl.ort + " " + zl.adresse + " " + zl.monat + " " + zl.jahr + " " + zl.typ);
					dgdzz.set("var.nutzung", topnutzung);
				}
			}

			Boolean leer = dgdzz.getBoolean("var.leerstehung", Boolean.FALSE);

			// START - ARE Specific Stuff - aber fuer alle Kunden gueltig
			if(leer)
			{
				String leerstehungssubart = tos.getString("var.leerstehungssubart");
				String leerstehungsart = tos.getString("var.leerstehungsart");
				dgdzz.set("var.leerstehungssubart", leerstehungssubart);
				dgdzz.set("var.leerstehungsart", leerstehungsart);

				String mietvertragvon = tos.getString("var.mietvertragvon");
				dgdzz.set("var.mietvertragvon", mietvertragvon);
			}
			else
			{
				// Wenn es keine Leerstehung ist, dann leerstehungsart auf "" setzten
				dgdzz.set("var.leerstehungssubart", "");
			}

			// PKO - 20160111 leerstandsdauer hier setzten von top auf ZZ
			String leerstehungsdauer = tos.getString("var.leerstehungsdauer");
			if(leerstehungsdauer != null && leerstehungsdauer.length() > 0)
			{
				dgdzz.set("var.leerstehungsdauer", leerstehungsdauer);
			}

			if(parentObject.flavour.equals("icrsare"))
			{
				try
				{
					BigDecimal nfl = tos.getBigDecimal("var.nfl", 2);
					if(null == nfl)
					{
						nfl = BigDecimal.ZERO;
					}
					BigDecimal leerfl = tos.getBigDecimal("var.leerfl", 2);
					if(null == leerfl)
					{
						leerfl = BigDecimal.ZERO;
					}

					BigDecimal flaeche;
					if(nfl.doubleValue() == leerfl.doubleValue())
					{
						flaeche = nfl;
					}
					else if(nfl.doubleValue() == 0 && leerfl.doubleValue() > 0)
					{
						flaeche = leerfl;
					}
					else if(nfl.doubleValue() > 0 && leerfl.doubleValue() == 0)
					{
						flaeche = nfl;
					}
					else
					{
						flaeche = nfl;
					}
					dgdzz.setBigDecimal("var.gesamtflaeche", flaeche);
				}
				catch(Exception ex)
				{
					debug.log(ex);
				}
			}

			// ENDE - ARE Specific Stuff - aber fuer alle Kunden gueltig

			// sollen Nutzung und Flaeche ueberschrieben werden?
			if(parentObject.zinslistenImport.getZlTypeConfig().isOverrideFlaeche())
			{
				String topfl = tos.getString("var.vertragsflaeche");
				if(topfl.length() > 0)
				{
					BigDecimal topflBD = net.metamagix.essence.eSSENCETypes.Currency.getBigDecimal(topfl);
					// vertragsflaeche >0?
					if(topflBD.compareTo(BigDecimal.ZERO) > 0)
					{
						// leer ?
						String leerstehung = (String)dgdzz.get("var.leerstehung");
						if(leerstehung != null)
						{
							parentObject.log("Flaeche wird mit Vertragsflaeche " + topfl + " ueberschieben, Top " + dgdzz.getName() + zl.plz + " " + zl.ort + " " + zl.adresse + " " + zl.monat + " " + zl.jahr + " " + zl.typ);
							if(leer)
							{
								dgdzz.set("var.leerfl", topfl);
								dgdzz.set("var.nfl", "0");
								tos.set("var.leerfl", topfl);
								tos.set("var.nfl", "0");
							}
							else
							{
								dgdzz.set("var.leerfl", "0");
								dgdzz.set("var.nfl", topfl);
								tos.set("var.leerfl", "0");
								tos.set("var.nfl", topfl);
							}
						}
					}
				}
			}
		}
		catch(Exception xc)
		{
			debug.log(parentObject.xc);
		}

		dgdzz = Top.manipulateSollZielMiete(dgdzz, debug);

		// calculate $gesamtflaeche for EGB/SOM
		if(CfgSingleton.getInstance().getBoolean("CALCULATEEGBFLAECHEN", Boolean.FALSE))
		{
			FlaechenBerechnungen flBerechnungen = FlaechenBerechnungen.getFlavouredInstance(session);
			flBerechnungen.berechneTopOderZinszeilenFlaechen(dgdzz, session);
		}

		TopBerechnungen topBerechnungen = TopBerechnungen.getFlavouredInstance(session);
		topBerechnungen.berechneTopOderZinszeile(dgdzz, session);

		return dgdzz;
	}

	/**
	 * get a user value specified something var.xxx or slot.yyy
	 *
	 * @param name
	 *            the name
	 * @return the user value
	 */
	public String getUserValue(String name)
	{
		try
		{
			if(null == session)
			{
				return null;
			}
			String userid = (String)session.get("userid");
			if(null == userObj)
			{
				userObj = (DynGenDataObj)parentObject.DAInst.getObject(userid, "");
			}
			if(userObj != null)
			{
				String val = (String)userObj.get("var." + name);
				return val;
			}
		}
		catch(Exception uee)
		{}
		return null;
	}

		public Hashtable storeObjectsJunked(Hashtable res, DynGenDataObj ses)
	{
		Hashtable storeres = new HashMap();
		try
		{
			Hashtable<String, DynGenDataObj> temp = new HashMap<String, DynGenDataObj>();
			Enumeration ek = res.keys();
			int count = 0;
			while(ek.hasMoreElements())
			{
				count++;
				// System.err.println("JUNKSTORE COUNT " + count + " of " + res.size());
				String name = (String)ek.nextElement();
				DynGenDataObj val = (DynGenDataObj)res.get(name);
				temp.put(name, val);
				// junk voll oder letztes element
				if(count % STORE_JUNK == 0 || count == res.size())
				{
					// zwischenspeichern ...
					try
					{
						if(null == parentObject.DAInst)
						{
							net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
							parentObject.DAInst = conn.getDataAgent();
						}
						// System.err.println("JUNKSTORE STORE " + temp.size() + "elements");
						Hashtable<String, Boolean> tempStoreRes = parentObject.DAInst.storeObjects(temp, ses);
						Enumeration tempnum = tempStoreRes.keys();
						while(tempnum.hasMoreElements())
						{
							String tname = (String)tempnum.nextElement();
							Boolean tval = tempStoreRes.get(tname);

							if(!tval.booleanValue())
							{
								try
								{
									// store fehler
									DynGenDataObj bad_dgd = (DynGenDataObj)res.get(tname);
									if(null != bad_dgd)
									{
										parentObject.zlprotocol.appendHtmlErr("" + bad_dgd.getString("var.name") + " (" + bad_dgd.getString("TEMPLATETYPE") + ") " + Tr.t("textIsCorrupted", session.getString("language")) + "<br>");
										parentObject.zlprotocol.appendTxtErr("" + (String)bad_dgd.get("var.name") + " (" + bad_dgd.getString("TEMPLATETYPE") + ") " + Tr.t("textIsCorrupted", session.getString("language")) + "\n");
									}
								}
								catch(Exception xe)
								{
									debug.error(xe);
								}
							}
							storeres.put(tname, tval);
						}
						temp = new HashMap<String, DynGenDataObj>();
						tempStoreRes = new HashMap<String, Boolean>();
					}
					catch(Exception xx)
					{
						debug.error(xx);
					}
				}
			}
		}
		catch(Exception px)
		{
			debug.error(px);
		}
		return storeres;
	}

		private void writeSlots(Hashtable vals, DynGenDataObj dgd)
	{
		writeSlots(vals, dgd, false, false);
	}

		private void writeSlots(Hashtable vals, DynGenDataObj dgd, boolean setOnlySingleValue, boolean createObject)
	{
		String displayname = "";
		if(parentObject.flavour.equals("icrsfred") || parentObject.flavour.equals("icrsare") || parentObject.flavour.equals("icrswi"))
		{
			createObject = true;
		}

		String ses_username = (String)session.get("var.username");

		try
		{
			// StringBuffer resultcode = new StringBuffer();
			Hashtable slots = dgd.getSubs("slot");
			for(Enumeration e = slots.keys(); e.hasMoreElements();)
			{
				String name = (String)e.nextElement();

				displayname = (String)dgd.get("slot." + name + "." + Tr.t("displayname", mylang));

				if(null == displayname)
				{
					displayname = "";
				}

				Object val = vals.get(name);

				if(null == val && displayname.trim().length() > 0)
				{
					val = vals.get(displayname.toLowerCase());
				}

				Slot currSlot = dgd.getSlot(name);

				if(null != val)
				{
					if(dgd.write_access(session, "slot." + name))
					{
						String[] myvals = new String[1];
						myvals[0] = val.toString().trim();
						Slot currentSlot = dgd.getSlot(name);
						Slot oldSlot = new Slot();
						if(currentSlot != null)
						{
							for(int i = 0; i < currentSlot.size(); i++)
							{
								oldSlot.addReference(new String[]{
									(String)currentSlot.get(i)}, -1);
							}
						}
						boolean clearvaluesslot = parentObject.getBoolean("var.clearvalues" + name, Boolean.FALSE);

						// bei clearvalues immer loeschen, wenn nicht leer
						if(clearvaluesslot && !oldSlot.isEmpty())
						{
							Slot f = new Slot();
							dgd.set("slot." + name, f);
							dgd.set("dirty", "yes");
							debug.info(this, "Slot " + name + " cleared.");
						}
						// leere zelle, bei do delete loeschen wenn nicht leer
						String gt = "";

						DynGenDataObj flavouredDgd = TemplateReader.getInstance().getDGDObjectForTemplate(dgd.getTemplateType(), null, session, true, false);

						gt = (String)flavouredDgd.get("slot." + name + ".GUITYPE");

						if(gt == null || gt.length() == 0 || gt.toLowerCase().equals("none"))
						{
							gt = (String)dgd.get("slot." + name + ".GUITYPE");
						}

						if(null == gt)
						{
							gt = "";
							// System.err.println(" slot " + name + " has guitype " + gt);
						}

						if(gt.equalsIgnoreCase("useraccesslist") || gt.equalsIgnoreCase("useraccesswritelist") || gt.equalsIgnoreCase("simpleslot") || gt.equalsIgnoreCase("autolist") || gt.equalsIgnoreCase("autolistextended"))
						{
							// Separator für mehrere Werte
							// myvals = CoolStringTool.splitFast(", ?", val.toString());
							myvals = val.toString().split(",");
							debug.chat(this, " found " + myvals.length + " elements in " + val.toString());
						}

						String ttype = "";
						ttype = (String)dgd.get("slot." + name + ".TEMPLATETYPE");

						if(null != ttype)
						{
							debug.chat("found templatetype " + ttype + " for slot " + name);
							// es koennten ja mehrere sein (bei simpleslot)
							// TODO: nur wenn exposed?

							StringBuffer multiSlotWritten = new StringBuffer();
							StringBuffer multiRealSlotWritten = new StringBuffer();
							for(int sx = 0; sx < myvals.length; sx++)
							{
								String slotval = myvals[sx].trim();
								// DN 20150120: fix for not UTF-8 encoded DBs!
								slotval = getDBEncodedValueOfString(slotval);
								debug.chat("found value " + slotval + " for slot " + name);

								Hashtable slot_els = getMapping(ttype);

								// SLOTVAL AUCH UPDATEN
								String fid = null;
								// nur wenn wirklich vorhanden!!!
								if(slot_els.containsKey(slotval.toLowerCase()))
								{
									fid = (String)slot_els.get(slotval.toLowerCase());
								}

								// bei mieterfirma -- temporaere haessliche loesung - wenns mal oefter gebraucht wird was gescheites draus machen
								if(fid == null && name.equals("mieterfirma"))
								{
									if(vals.containsKey("mieterfirma___uniqueid") && slot_els.containsKey(vals.get("mieterfirma___uniqueid")))
									{
										fid = (String)slot_els.get(vals.get("mieterfirma___uniqueid"));
									}
									else if(vals.containsKey("mieterfirma___externalid"))
									{
										fid = vals.get("mieterfirma___externalid") == null ? null : (String)slot_els.get(vals.get("mieterfirma___externalid"));
									}
								}

								// für SOM auch bei Vermieterfirma
								if(parentObject.flavour.equals("icrssom") && fid == null && name.equals("vermieterfirma"))
								{
									if(vals.containsKey("vermieterfirma___uniqueid") && slot_els.containsKey(vals.get("vermieterfirma___uniqueid")))
									{
										fid = (String)slot_els.get(vals.get("vermieterfirma___uniqueid"));
									}
									else if(vals.containsKey("vermieterfirma___externalid"))
									{
										fid = vals.get("vermieterfirma___externalid") == null ? null : (String)slot_els.get(vals.get("vermieterfirma___externalid"));
									}
								}

								// id auch pruefen, slotval koennte auch id sein!
								// else if(slot_els.containsValue(slotval.toLowerCase()))
								// {
								// fid = slotval;
								// }
								DynGenDataObj f_dgd = null;
								String r = null;
								// CHECK obslot objekt existiert...

								if(null != fid)
								{
									debug.chat("found id " + fid + " for slot " + name);

									f_dgd = (DynGenDataObj)parentObject.DAInst.getObject(fid, "");
								}

								if(null == f_dgd && createObject && slotval.length() > 0)
								{

									debug.chat("create " + slotval + " from template " + ttype);
									f_dgd = new DynGenDataObj();
									f_dgd.DAInst = parentObject.DAInst;
									// build it with templatecode
									if(parentObject.PBInst == null)
									{
										Connector conn = null;
										conn = new Connector();
										parentObject.PBInst = conn.getPageBuilder();
									}
									String f_tcode = parentObject.PBInst.readTemplate(ttype);
									f_dgd.init(f_tcode, global, session);
									f_dgd.set("var.name", slotval);

									boolean noValueFound = true;
									// Set Values for Slod DGD -> Key must be in the Format SLOTNAME___VARIABLEINTEMLATE
									for(Enumeration keys = vals.keys(); keys.hasMoreElements();)
									{
										String key = (String)keys.nextElement();
										if(key.startsWith(name + "___"))
										{
											String value = (String)vals.get(key);
											String varname = key.replaceAll(name + "___", "");

											f_dgd.set("var." + varname, value);
											noValueFound = false;
										}
									}

									if(!noValueFound || createObject)
									{
										r = parentObject.DAInst.storeObject(f_dgd, "", fid, session);
										debug.chat("created object " + r + " from template " + ttype);
										if(null == r)
										{
											// errorcode.append("<div class=\"errortext\">" + Tr.t("textLine", mylang) + " " + zeile + " " + Tr.t("textCaution", mylang) + " " + slotval + " " +
											// Tr.t("errorCouldNotCreateLine2", mylang) + "</div>" + "<br>");
											// resulttable.append("<tr><td><div class=\"errortext\">" + zeile + "</div></td><td>" + "</td><td><div class=\"errortext\">" + Tr.t("textCaution",
											// mylang) + "</div></td><td>" + " \"" + slotval + "\" " + Tr.t("errorCouldNotCreateLine2", mylang) + " " + name + "!</td></tr>");
											// csvErrorCollector.addError(zeile, Tr.t("textCaution", mylang), CSVErrorCollector.SEVERITY_MEDIUM, slotval + " " + Tr.t("errorCouldNotCreateLine2",
											// mylang),
											// Tr.t("textCaution", mylang), null, ses_username);
										}
										else if(null == fid)
										{
											// resultcode.append("<a class='ajaxLink' href= \"NA?OID=" + r + "\">" + slotval + "</a>) " + Tr.t("infoLineCreated", mylang) + "<br>");
											// String link = CoolStringTool.buildLink(r, "SHOW", "", slotval, "", global, session);

											// resulttable.append("<tr><td>" + zeile + "</td><td>" + link + "</td><td>" + Tr.t("infoLineCreated", mylang) + "</td><td>" + "</td></tr>");

											// keine doppelten creates!!!!
											fid = r;
											slot_els.put(slotval.toLowerCase(), r);

											if(vals.containsKey("mieterfirma___uniqueid"))
											{
												slot_els.put(vals.get("mieterfirma___uniqueid"), r);
											}
											else if(vals.containsKey("mieterfirma___externalid"))
											{
												slot_els.put(vals.get("mieterfirma___externalid"), r);
											}

										}
										else
										{
											// resultcode.append("<a class='ajaxLink' href= \"NA?OID=" + r + "\">" + slotval + "</a>) " + Tr.t("infoLineUpdated", mylang) + "<br>");
											// String link = CoolStringTool.buildLink(r, "SHOW", "", slotval, "", global, session);
											// resulttable.append("<tr><td>" + zeile + "</td><td>" + link + "</td><td>" + Tr.t("infoLineUpdated", mylang) + "</td><td>" + "</td></tr>");
											// // TODO cache dgd
											// addDGDToCache(r, ttype, f_dgd);
										}
										mappingCache.remove(ttype);
										// lower case as well
										mappingCache.remove(ttype.toLowerCase());
									}
								}

								// SLOTVALSSLOT SETZEN

								String guitype = "";
								guitype = (String)dgd.get("slot." + name + ".GUITYPE");

								if(null == guitype)
								{
									guitype = "";
								}

								Slot f = new Slot();
								// wenn comboslot -> neuen wert als einzigen schreiben...
								// wenn kein comboslot -> neuen wert adden...
								boolean issingelevalue = false;
								if(guitype.equalsIgnoreCase("comboslot"))
								{
									issingelevalue = true;
								}

								if(setOnlySingleValue)
								{
									issingelevalue = true;
								}

								else if(guitype.equalsIgnoreCase("autolist"))
								{
									String arg = dgd.getString("slot." + name + ".ARG");
									if(arg.equals("1"))
									{
										issingelevalue = true;
									}
								}

								if(!issingelevalue)
								{
									f = (Slot)dgd.get("slot." + name);
								}

								if(null == f)
								{
									f = new Slot();
								}
								if(null != fid)
								{
									String[] refs = new String[1];
									refs[0] = fid;
									f.delReference(refs);
									f.addReference(refs, 0);
								}
								// slot has changed -> write to dgd

		private DynGenDataObj getPreviousZZ(String id, Zinsliste zl)
	{
		DynGenDataObj previousZZ = null;

		try
		{
			ArgsHelper argsHelper = new ArgsHelper(new HashMap<String, Object>());
			argsHelper.setMainTemplateType("CIMS.zinszeile");
			argsHelper.setAdvancedFields(true);
			argsHelper.addField("DOB.ID", "zzid");
			argsHelper.addField("ET0.datum");

			argsHelper.addTemplateType("top", "CIMS.top");
			argsHelper.addCondition("top_ID", id);

			String sqlstyle = (String)CfgSingleton.getInstance().get("sqlstyle");

			String datum = "01." + zl.monat + "." + zl.jahr;

			Date zzdate = new SimpleDateFormat("dd.MM.yyyy").parse(datum);

			eDate mydate = new eDate(zzdate);
			String dbcompatibel_date = mydate.getDBFieldValue(sqlstyle);
			argsHelper.addWhere("ET0.datum < " + dbcompatibel_date);

			argsHelper.addOrder("ET0.jahr*12+ET0.monat DESC");

			String mydom = null;
			if(session != null)
			{
				mydom = (String)session.get("domainid");
			}
			if(mydom == null || mydom.length() == 0)
			{
				argsHelper.addCondition("DOMAIN", "ALLDOMAINS");
			}
			else
			{
				argsHelper.addCondition("DOMAIN", mydom);
			}

			if(null == parentObject.DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				parentObject.DAInst = conn.getDataAgent();
			}
			QueryResult qr = null;
			Vector<Hashtable<String, String>> result = null;

			qr = parentObject.DAInst.queryObjectWithResult(argsHelper.getArgs());
			result = qr.getResult();

			if(result != null && result.size() > 0)
			{
				Hashtable row = result.get(0);

				String zzId = String.valueOf(row.get("zzid"));
				if(zzId != null && zzId.length() > 0)
				{
					previousZZ = (DynGenDataObj)parentObject.DAInst.getObject(zzId, null);
				}
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}

		return previousZZ;
	}

		private void modifyLetzteIndexierung(Hashtable ht, Zinsliste zl, DynGenDataObj dgd)
	{
		try
		{
			String mieter = (String)ht.get("mieter");
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

			if(mieter != null && !parentObject.checkLeerstandString(mieter))
			{
				Date dateLetzteIndexierung = null;
				String indexbasisdatum = (String)ht.get("indexdatum");
				String status = (String)ht.get("status");
				String hausOID = "";
				if(indexbasisdatum != null && indexbasisdatum.length() > 0)
				{
					dateLetzteIndexierung = sdf.parse(indexbasisdatum);
					if(indexbasisdatum.matches("\\d\\d[.]\\d\\d[.]\\d\\d\\d\\d") || indexbasisdatum.matches("\\d\\d[.]\\d\\d[.]\\d\\d"))
					{
						String[] tmpDate = indexbasisdatum.split("[.]");
						if(tmpDate[2].length() == 2)
						{
							if(Integer.parseInt(tmpDate[2]) > 50)
							{
								indexbasisdatum = tmpDate[0] + "." + tmpDate[1] + ".19" + tmpDate[2];
							}
							else
							{
								indexbasisdatum = tmpDate[0] + "." + tmpDate[1] + ".20" + tmpDate[2];
							}
						}
					}
				}
				String monat = zl.monat;
				if(monat.length() == 1)
				{
					monat = "0" + monat;
				}

				String jahr = zl.jahr;
				if(jahr.length() == 1)
				{
					jahr = "0" + jahr;
				}
				String importdate = "01." + monat + "." + jahr;
				Date DateImportdate = sdf.parse(importdate);
				String plz = zl.plz;
				String objekt = zl.adresse;
				String topname = dgd.getName();
				TopoTool topoTool = new TopoTool(session, global);
				String hausOid = topoTool.getHausOID(zl);
				String mietvertragvonzl = (String)ht.get("mietvertragvon");
				if(mietvertragvonzl != null && mietvertragvonzl.length() > 0)
				{
					if(indexbasisdatum != null && !(indexbasisdatum.equals("")))
					{
						DateFormat df = DateFormat.getDateInstance();
						Date dt1 = null;
						Date dt2 = null;
						try
						{
							dt1 = df.parse(mietvertragvonzl);
							dt2 = df.parse(indexbasisdatum);
						}
						catch(Exception ex)
						{
							debug.error(this, "Error in UploadXLS4: Couldn't parse date!");
						}
						Calendar mvvoncal = Calendar.getInstance();
						mvvoncal.setTime(dt1);
						Calendar letzteindexcal = Calendar.getInstance();
						letzteindexcal.setTime(dt2);

						Date now = new Date();
						Calendar nowcal = Calendar.getInstance();
						nowcal.setTime(now);

						if(letzteindexcal.before(mvvoncal) || letzteindexcal.equals(mvvoncal))
						{
							// letzteindexcal wenn kleiner als mietvertragsbeginn
							// und nicht gleich mietvertrag ist
							dgd.set("var.indexdatum", "");
							if(!parentObject.flavour.equals("icrskag"))
							{
								dgd.set("var.indexbasis", "");
							}

						}
						else if((letzteindexcal.before(nowcal)))
						{
							// letzteindexcal wenn es nicht in zukunft liegt
							// bedeutet letzte indexierung letzteindexcal wenn es nicht in zukunft liegt
							dgd.set("var.indexdatum", indexbasisdatum);
						}
					}
				}
				if((dateLetzteIndexierung != null && dateLetzteIndexierung.after(DateImportdate)) || (indexbasisdatum == null || indexbasisdatum.length() == 0))
				{
					Hashtable<String, String> lastindexbasisdatumHash = getLatestIndexDatumFromZZ(hausOid, topname, jahr, monat, mietvertragvonzl);
					if(lastindexbasisdatumHash != null)
					{
						String indexdatum = lastindexbasisdatumHash.get("indexdatum");
						String indexbasis = lastindexbasisdatumHash.get("indexbasis");
						dgd.set("var.indexdatum", indexdatum);
						dgd.set("var.indexbasis", indexbasis);
					}
					else
					{
						dgd.set("var.indexdatum", "");
						if(!parentObject.flavour.equals("icrskag"))
						{
							dgd.set("var.indexbasis", "");
						}
					}
				}
			}
			else
			{
				dgd.set("var.indexbasis", "");
				dgd.set("var.indexdatum", "");
			}
		}
		catch(Exception ex)
		{
			debug.log("UploadXLS4 " + ex);
		}
	}

}
