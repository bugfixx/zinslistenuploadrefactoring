package Magic.IMS.ZLImport;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Agents.QueryResult;
import net.metamagix.essence.Agents.TemplateReader;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.GenData.Slot;
import net.metamagix.essence.MConfig.CfgSingleton;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.ajax.JSONArray;
import net.metamagix.essence.ajax.JSONException;
import net.metamagix.essence.ajax.JSONObject;
import net.metamagix.essence.tools.CoolDataTool;
import net.metamagix.essence.tools.CoolStringTool;
import net.metamagix.essence.tools.encoding.EncodingHelper;

import Magic.GUI.Selector;
import Magic.IMS.reporting.helpers.ArgsHelper;

/**
 * Service class for mapping operations in the Zinslisten import process.
 * 
 * This service handles mapping operations for various entity types, slot encoding,
 * and Zapo (Zahlungsposten) mapping during the Zinslisten import process.
 * 
 * Phase 3: Mapping operations extracted:
 * - getMapping(String ttype) - Gets mapping for template types (Firma, Mietenpool, etc.)
 * - createOrUpdateGebaeudeAndTopMapping(Zinsliste, String) - Creates/updates Gebaeude and Top mappings
 * - getTopsForGebaeude(Zinsliste, String, String) - Gets Tops associated with a Gebaeude
 * - getGebaeudeOidFromEdvNummer(String, String) - Gets Gebaeude OID from EDV number
 * - doZapoMappingFromGui(String) - Performs Zapo mapping from GUI input
 * - getMappingChangesVector() - Retrieves mapping changes from JSON
 * - getValueMap(String) - Gets value mapping for selectors
 * - setDBEncoding() - Initializes database encoding/collation
 * - getDBEncodedValueOfString(String) - Converts strings to DB encoding
 * 
 * Collections modernization:
 * - Hashtable → HashMap (not ConcurrentHashMap as this isn't thread-critical)
 * - Vector → ArrayList
 * - Added proper generics throughout
 * 
 * Database Encoding:
 * - Handles MSSQL collation for proper slot mapping
 * - Ensures correct character encoding for database queries
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ZinslistenMappingService
{
	private Map<String, String> mapper;
	private Map<String, Object> mappingCache;
	private String dbEncoding;
	
	private FileDataAgent fileDataAgent;
	private DynGenDataObj session;
	private DynGenDataObj global;
	private BugMe debug;
	private net.metamagix.essence.Agents.DataAgent dataAgent;
	
	/**
	 * Constructor for ZinslistenMappingService.
	 * 
	 * @param fileDataAgent the File Data Agent instance
	 * @param session the user session
	 * @param global the global DynGenDataObj
	 * @param debug the debug logger
	 */
	public ZinslistenMappingService(FileDataAgent fileDataAgent, DynGenDataObj session, 
	                                 DynGenDataObj global, BugMe debug)
	{
		this.fileDataAgent = fileDataAgent;
		this.session = session;
		this.global = global;
		this.debug = debug;
		this.mapper = new HashMap<>();
		this.mappingCache = new HashMap<>();
		this.setDBEncoding();
	}
	
	/**
	 * Gets mapping for a template type.
	 * 
	 * Creates a mapping between various identifiers (ID, uniqueid, externalid, name, uniquekey)
	 * and the object ID for quick lookup during import operations.
	 * 
	 * Supported template types:
	 * - ICRScrm.firma - Maps firma objects by uniqueid, externalid, and name
	 * - ICRS.module.mietenpool.mietenpool - Maps mietenpool objects by uniquekey
	 * - All others - Maps by name only
	 * 
	 * @param ttype the template type to map
	 * @return HashMap containing mapping entries (lowercase keys to OIDs)
	 */
	public HashMap<String, String> getMapping(String ttype)
	{
		if(ttype == null)
		{
			return new HashMap();
		}

		HashMap result = new HashMap();
		if(ttype.equals("ICRScrm.firma"))
		{
			HashMap args = new HashMap();
			Vector res = new Vector();
			args.put("TType", ttype);
			// fieldClause ... Felder zum holen ,-separiert
			args.put("fieldClause", "ID,name,ET0.uniqueid uniqueid,ET0.externalid externalid");
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
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			if(null == dataAgent)
			{
				dataAgent = conn.getDataAgent();
			}

			try
			{
				QueryResult qr = dataAgent.queryObjectWithResult(new Hashtable(args));
				res = qr.getResult();
				int x = 0;
				while(x < res.size())
				{
					Hashtable row = (Hashtable)res.elementAt(x);
					String pn = (String)row.get("uniqueid");
					String pn2 = (String)row.get("externalid");
					String pn3 = (String)row.get("name");
					String pid = (String)row.get("ID");
					if(null == pid)
					{
						pid = (String)row.get("id");
					}
					if(null != pid && pid.length() > 0)
					{
						result.put(pid, pid);
						if(null != pn && pn.length() > 0)
						{
							result.put(pn.toLowerCase(), pid);
							result.put(pn.toLowerCase().trim(), pid);
						}
						if(null != pn2 && pn2.length() > 0)
						{
							result.put(pn2.toLowerCase(), pid);
							result.put(pn2.toLowerCase().trim(), pid);
						}
						if(null != pn3 && pn3.length() > 0)
						{
							result.put(pn3.toLowerCase(), pid);
							result.put(pn3.toLowerCase().trim(), pid);
						}
					}
					x++;
				}
			}
			catch(Exception x)
			{

			}
			mappingCache.put("ICRScrm.firma", result);
		}
		else if(ttype.equals("ICRS.module.mietenpool.mietenpool"))
		{
			HashMap args = new HashMap();
			Vector res = new Vector();
			args.put("TType", ttype);
			// fieldClause ... Felder zum holen ,-separiert
			args.put("fieldClause", "ID,ET0.uniquekey uniquekey");
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
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			if(null == dataAgent)
			{
				dataAgent = conn.getDataAgent();
			}

			try
			{
				QueryResult qr = dataAgent.queryObjectWithResult(new Hashtable(args));
				res = qr.getResult();
				int x = 0;
				while(x < res.size())
				{
					Hashtable row = (Hashtable)res.elementAt(x);
					String pn = (String)row.get("uniquekey");
					String pid = (String)row.get("ID");
					if(null == pid)
					{
						pid = (String)row.get("id");
					}
					if(null != pid && pid.length() > 0)
					{
						result.put(pid, pid);
						if(null != pn && pn.length() > 0)
						{
							result.put(pn.toLowerCase(), pid);
							result.put(pn.toLowerCase().trim(), pid);
							result.put(pid, pid);
						}
					}
					x++;
				}
			}
			catch(Exception x)
			{

			}
			mappingCache.put("ICRScrm.firma", result);
		}
		else
		{
			HashMap args = new HashMap();
			Vector res = new Vector();
			args.put("TType", ttype);
			// fieldClause ... Felder zum holen ,-separiert
			args.put("fieldClause", "ID,name");
			args.put("orderClause", "name ASC");
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
			// PBInst=conn.getPageBuilder();
			if(null == dataAgent)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			try
			{
				QueryResult qr = dataAgent.queryObjectWithResult(new Hashtable(args));
				res = qr.getResult();
				int x = 0;
				while(x < res.size())
				{
					Hashtable row = (Hashtable)res.elementAt(x);
					String pn = (String)row.get("name");
					String pid = (String)row.get("ID");
					if(null != pn)
					{
						if(null != pid)
						{
							result.put(pn.toLowerCase(), pid);
							result.put(pn.toLowerCase().trim(), pid);
							result.put(pid, pid);
						}
					}
					x++;
				}
			}
			catch(Exception x)
			{

			}
			if(null != result)
			{
				mappingCache.put(ttype, result);
				// lower case as well
				if(!ttype.equals(ttype.toLowerCase()))
				{
					mappingCache.put(ttype.toLowerCase(), result);
				}
			}
		}
		return result;
	}
	
	/**
	 * Creates or updates Gebaeude (building) and Top mappings.
	 * 
	 * This method:
	 * 1. Extracts Gebaeude information from the Zinsliste
	 * 2. Creates or updates Gebaeude objects
	 * 3. Associates Tops with each Gebaeude
	 * 4. Updates the Haus with the Gebaeude slot
	 * 
	 * @param zl the Zinsliste containing building data
	 * @param oid_haus the OID of the Haus object
	 */
	private void createOrUpdateGebaeudeAndTopMapping(Zinsliste zl, String oid_haus)
	{
		try
		{
			if(oid_haus == null || oid_haus.equals(""))
			{
				debug.info("New Haus - no Gebäude mapping possible!");
				return;
			}

			TopoTool topotool = new TopoTool(session, global);
			if(null == dataAgent)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				dataAgent = conn.getDataAgent();
			}

			String[][] array = zl.getArray();

			DynGenDataObj dgdHaus = (DynGenDataObj)dataAgent.getObject(oid_haus, "");
			if(dgdHaus == null)
			{
				debug.error("Error in parse of UploadXLS4 - could not get Haus with oid=" + oid_haus + "!");
				return;
			}

			String hausedvnr = dgdHaus.getString("var.identadresse5");
			if(hausedvnr.length() == 0)
			{
				hausedvnr = dgdHaus.getString("var.name") + "---" + dgdHaus.getString("var.plz");
			}

			Slot gebauedeSlot = dgdHaus.getSlot("slot.gebaeude");
			if(gebauedeSlot == null)
			{
				gebauedeSlot = new Slot();
			}

			// get Gebaeude from Array
			Hashtable<String, Hashtable<String, String>> gebaeude = new Hashtable<>();
			String[] headlinearray = array[zl.getZlTypeConfig().getHeaderline()];

			for(int i = zl.getZlTypeConfig().getHeaderline() + 1; i < array.length; i++)
			{
				Hashtable<String, String> row = new Hashtable<>();
				String gebaeudeedvnummer = "";
				for(int j = 0; j < headlinearray.length; j++)
				{
					String header = headlinearray[j];
					if(header == null)
					{
						continue;
					}

					if(headlinearray[j].startsWith("gebaeude_"))
					{
						String value = array[i][j];
						if(value == null)
						{
							value = "";
						}
						row.put(headlinearray[j], value);
					}
					if(headlinearray[j].equals("gebaeude_gebaeudeedvnummer"))
					{
						String value = array[i][j];
						if(value == null)
						{
							value = "";
						}

						gebaeudeedvnummer = value;
					}
				}

				// Nur Gebaeude mit uebereinstimmender Hausedvnr adden!
				String hausedvnrFromGebaeudeedvnummer = "";
				if(gebaeudeedvnummer.contains("###"))
				{
					hausedvnrFromGebaeudeedvnummer = gebaeudeedvnummer.substring(0, gebaeudeedvnummer.indexOf("###"));
				}
				else
				{
					continue;
				}

				if(!zl.getZlTypeConfig().isConcatEdvNrToHaus() || (hausedvnr.toLowerCase().equals(hausedvnrFromGebaeudeedvnummer.toLowerCase()) || hausedvnr.substring(3).toLowerCase().equals(hausedvnrFromGebaeudeedvnummer.toLowerCase())))
				{
					if(!gebaeude.containsKey(gebaeudeedvnummer))
					{
						gebaeude.put(gebaeudeedvnummer, row);
					}
				}
			}

			for(String gebaeudeedvnummer : gebaeude.keySet())
			{
				try
				{
					String gebaeudeOid = getGebaeudeOidFromEdvNummer(gebaeudeedvnummer, oid_haus);
					DynGenDataObj dgdGebaeude = null;

					if(gebaeudeOid == null)
					{
						// create new Gebauede
						TemplateReader tr = TemplateReader.getInstance();
						dgdGebaeude = tr.getDGDForTemplate("CIMS.gebaeude", global, session);
					}

					if(gebaeudeOid != null && Long.parseLong(gebaeudeOid) > 0)
					{
						// get existing Gebaeude
						dgdGebaeude = (DynGenDataObj)dataAgent.getObject(gebaeudeOid, "");
					}

					if(dgdGebaeude != null)
					{
						// set Gebaeude values
						Hashtable<String, String> row = gebaeude.get(gebaeudeedvnummer);

						for(String key : row.keySet())
						{
							String value = row.get(key);
							String variable = key.replaceAll("gebaeude_", "");

							if((variable.equals("gebaeudeedvnummer") || variable.equals("name")) && value.contains("###"))
							{
								value = value.substring(value.indexOf("###") + 3);
							}
							dgdGebaeude.set("var." + variable, value);
						}

						// Set gtops -> Tops am Gebaeude
						if(gebaeudeedvnummer.contains("###"))
						{
							gebaeudeedvnummer = gebaeudeedvnummer.substring(gebaeudeedvnummer.indexOf("###") + 3);
						}
						Slot gtops = getTopsForGebaeude(zl, gebaeudeedvnummer, oid_haus);
						dgdGebaeude.set("slot.gtops", gtops);

						if(dgdGebaeude.getString("var.name").length() == 0 && dgdGebaeude.getString("var.gebaeudeedvnummer").length() > 0)
						{
							dgdGebaeude.set("var.name", dgdGebaeude.getString("var.gebaeudeedvnummer"));
						}

						// Store Gebauede
						gebaeudeOid = dataAgent.storeObject(dgdGebaeude, dgdGebaeude.getTemplateType(), gebaeudeOid, session);

						// Add Gebaeude to Haus
						gebauedeSlot.add(gebaeudeOid);
						Slot.removeDuplicates(gebauedeSlot);

					}

				}
				catch(Exception e)
				{
					BugMe.getInstance().log(e);
				}
			}

			// Store Haus
			dgdHaus.set("slot.gebaeude", gebauedeSlot);
			dataAgent.storeObject(dgdHaus, dgdHaus.getTemplateType(), oid_haus, session);

		}
		catch(Exception e)
		{
			BugMe.getInstance().log(e);
		}
	}

	/**
	 * Gets the tops for gebaeude.
	 *
	 * @param zl
	 *            the zl
	 * @param gebaeudeedvnummer
	 *            the gebaeudeedvnummer
	 * @param oid_haus
	 *            the oid haus
	 * @return the tops for gebaeude
	 */
	private Slot getTopsForGebaeude(Zinsliste zl, String gebaeudeedvnummer, String oid_haus)
	{
		Slot gtops = new Slot();

		if(null == dataAgent)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			dataAgent = conn.getDataAgent();
		}
		TopoQueries topoQueries = new TopoQueries(session, global);
		Hashtable allmytops = topoQueries.getTopsForOID(oid_haus, null, session, dataAgent);
		Hashtable allmyinternaltops = TopoTool.getInternalTopsForTops(allmytops);

		for(int j = 0; j < zl.zinszeilen.size(); j++)
		{
			Hashtable ht = (Hashtable)zl.zinszeilen.get(j);

			String gebaeudeedvnummerToCompare = "";
			if(ht.containsKey("gebaeude_gebaeudeedvnummer"))
			{
				gebaeudeedvnummerToCompare = (String)ht.get("gebaeude_gebaeudeedvnummer");
			}

			if(gebaeudeedvnummerToCompare.length() == 0 && ht.containsKey("hausedvnr") && ht.containsKey("gebaeude_name"))
			{
				String gebaeude_name = (String)ht.get("gebaeude_name");
				if(!gebaeude_name.contains("###"))
				{
					gebaeudeedvnummerToCompare = ht.get("hausedvnr") + "###";
				}
				gebaeudeedvnummerToCompare = gebaeudeedvnummerToCompare + ht.get("gebaeude_name");
			}
			else if(gebaeudeedvnummerToCompare.length() == 0 && ht.containsKey("hausadresse") && ht.containsKey("hausplz") && ht.containsKey("gebaeude_name"))
			{
				String plz = (String)ht.get("hausplz");
				if(plz.contains(","))
				{
					plz = plz.substring(0, plz.indexOf(","));
				}

				String gebaeude_name = (String)ht.get("gebaeude_name");
				if(!gebaeude_name.contains("###"))
				{
					gebaeudeedvnummerToCompare = ht.get("hausadresse") + "---" + plz + "###";
				}
				gebaeudeedvnummerToCompare = gebaeudeedvnummerToCompare + ht.get("gebaeude_name");
			}

			if(gebaeudeedvnummerToCompare.contains("###"))
			{
				int index = gebaeudeedvnummerToCompare.indexOf("###") + 3;
				gebaeudeedvnummerToCompare = gebaeudeedvnummerToCompare.substring(index);
			}

			if(gebaeudeedvnummer.equals(gebaeudeedvnummerToCompare))
			{
				String top = (String)ht.get("top");

				String oid_top = (String)allmytops.get(top);
				if(null == oid_top)
				{
					oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
				}

				if(oid_top != null)
				{
					gtops.add(oid_top);
					Slot.removeDuplicates(gtops);
				}
			}
		}

		for(int j = 0; j < zl.stellplaetze.size(); j++)
		{
			Hashtable ht = (Hashtable)zl.stellplaetze.get(j);

			String gebaeudeedvnummerToCompare = "";
			if(ht.containsKey("gebaeude_gebaeudeedvnummer"))
			{
				gebaeudeedvnummerToCompare = (String)ht.get("gebaeude_gebaeudeedvnummer");
			}

			if(gebaeudeedvnummerToCompare.length() == 0 && ht.containsKey("hausedvnr") && ht.containsKey("gebaeude_name"))
			{
				String gebaeude_name = (String)ht.get("gebaeude_name");
				if(!gebaeude_name.contains("###"))
				{
					gebaeudeedvnummerToCompare = ht.get("hausedvnr") + "###";
				}
				gebaeudeedvnummerToCompare = gebaeudeedvnummerToCompare + ht.get("gebaeude_name");
			}
			else if(gebaeudeedvnummerToCompare.length() == 0 && ht.containsKey("hausadresse") && ht.containsKey("hausplz") && ht.containsKey("gebaeude_name"))
			{

				String plz = (String)ht.get("hausplz");
				if(plz.contains(","))
				{
					plz = plz.substring(0, plz.indexOf(","));
				}

				gebaeudeedvnummerToCompare = ht.get("hausadresse") + "---" + plz + "###" + ht.get("gebaeude_name");
			}

			if(gebaeudeedvnummerToCompare.contains("###"))
			{
				gebaeudeedvnummerToCompare = gebaeudeedvnummerToCompare.substring(gebaeudeedvnummerToCompare.indexOf("###") + 3);
			}

			if(gebaeudeedvnummer.equals(gebaeudeedvnummerToCompare))
			{
				String top = (String)ht.get("top");

				String oid_top = (String)allmytops.get(top);
				if(null == oid_top)
				{
					oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
				}

				if(oid_top != null)
				{
					gtops.add(oid_top);
					Slot.removeDuplicates(gtops);
				}
			}
		}
		return gtops;
	}

	/**
	 * Gets the gebaeude oid from edv nummer.
	 *
	 * @param gebaeudeedvnummer
	 *            the gebaeudeedvnummer
	 * @param oid_haus
	 *            the oid haus
	 * @return the gebaeude oid from edv nummer
	 */
	private String getGebaeudeOidFromEdvNummer(String gebaeudeedvnummer, String oid_haus)
	{
		try
		{
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();

			ArgsHelper argsHelper = new ArgsHelper();
			argsHelper.setMainTemplateType("CIMS.gebaeude");
			argsHelper.addTemplateType("REVgebaeude", "CIMS.haus");
			argsHelper.setAdvancedFields(true);

			if(gebaeudeedvnummer.contains("###"))
			{
				gebaeudeedvnummer = gebaeudeedvnummer.substring(gebaeudeedvnummer.indexOf("###") + 3);
			}
			argsHelper.addCondition("gebaeudeedvnummer", gebaeudeedvnummer);
			argsHelper.addCondition("REVgebaeude_ID", oid_haus);
			argsHelper.addDomainCondition(session);
			argsHelper.addField("ID");

			// new Connector Class
			if(null == dataAgent)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				dataAgent = conn.getDataAgent();
			}

			QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
			if(!qr.isOK())
			{
				BugMe.getInstance().error(this, "Query not OK when fetching getGebaeudeOidFromEdvNummer with gebaeudeedvnummer '" + gebaeudeedvnummer + "'");
				BugMe.getInstance().error(this, "QueryResult message: '" + qr.getMessage() + "'");
				BugMe.getInstance().log(this, "Query was: '" + qr.getSql() + "'");
				return "-1";
			}
			res = qr.getResult();

			if(res != null && res.size() > 1)
			{
				BugMe.getInstance().error(this, "Duplicate 'Gebaeude' when fetching getGebaeudeOidFromEdvNummer with gebaeudeedvnummer '" + gebaeudeedvnummer + "'");
				BugMe.getInstance().log(this, "Query was: '" + qr.getSql() + "'");
				return "-1";
			}
			else if(res != null && res.size() == 1)
			{

				Hashtable<String, String> row = res.get(0);
				String oid = row.get("ID");
				return oid;
			}
			else
			{
				return null;
			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().log(e);
			return "-1";
		}

	}

	/**
	 * Let the User Map the new Zapos from ZZ into the zinslistenconfig.xml File -> Store history in database
	 *
	 * @param cfg_zlimport
	 *            the cfg zlimport
	 */
	private void doZapoMappingFromGui(String cfg_zlimport)
	{
		try
		{
			boolean configHasChanged = false;

			String hausverwaltung = getVarArg("hausverwaltung_" + 0);
			if(hausverwaltung.length() != 0)
			{
				hausverwaltung += "mieter";

				// Unbekante Miet- und Betriebsposten durch Benutzer zuordnen
				// Hier erfolg das Postenmapping

				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = null;
				Document customerdoc = null;

				if(CfgSingleton.getInstance().getBoolean("USE_CUSTOMER_ZINSLISTENCONFIG", Boolean.FALSE))
				{
					String customerzinslistenconfig = cfg_zlimport.replace("zinslistenconfig.xml", "customerzinslistenconfig.xml");

					try
					{
						File cf = new File(customerzinslistenconfig);

						if(null == cf || !cf.isFile())
						{
							// Wenn nicht existent copy von Config als Customerzinslistenconfig speichern
							Files.copy(Path.of(cfg_zlimport), Path.of(customerzinslistenconfig));

							cf = new File(customerzinslistenconfig);
						}

						if(null != cf && cf.isFile())
						{
							if(null != cf && cf.isFile())
							{
								customerdoc = dBuilder.parse(cf);
							}
							configHasChanged = mofifyDocument(customerdoc);
						}

					}
					catch(Exception e)
					{
						debug.error(e);
					}
				}
				else
				{
					File f = new File(cfg_zlimport);
					if(null != f && f.isFile())
					{
						doc = dBuilder.parse(f);
					}

					if(doc != null)
					{
						// optional, but recommended
						// read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
						configHasChanged = mofifyDocument(doc);
					}
				}

				if(configHasChanged)
				{
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();

					// customerzinslistenconfig

					if(CfgSingleton.getInstance().getBoolean("USE_CUSTOMER_ZINSLISTENCONFIG", Boolean.FALSE))
					{
						String customerzinslistenconfig = cfg_zlimport.replace("zinslistenconfig.xml", "customerzinslistenconfig.xml");

						try
						{
							String encoding = "ISO-8859-1";
							if(customerdoc.getXmlEncoding() != null)
							{
								encoding = customerdoc.getXmlEncoding();
							}

							transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

							DOMImplementation domImpl = customerdoc.getImplementation();
							DocumentType doctype = domImpl.createDocumentType("doctype", "", "../zinslistenconfig.dtd");
							// transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
							transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

							DOMSource source = new DOMSource(customerdoc);

							// Output to console for testing
							// StreamResult consoleResult = new StreamResult(System.out);
							// transformer.transform(source, consoleResult);

							// write the content into xml file
							StreamResult result = new StreamResult(new File(customerzinslistenconfig));
							transformer.transform(source, result);
						}
						catch(Exception e)
						{
							debug.error(e);
						}
					}
					else
					{
						String encoding = "ISO-8859-1";
						if(doc.getXmlEncoding() != null)
						{
							encoding = doc.getXmlEncoding();
						}

						transformer.setOutputProperty(OutputKeys.ENCODING, encoding);

						DOMImplementation domImpl = doc.getImplementation();
						DocumentType doctype = domImpl.createDocumentType("doctype", "", "../zinslistenconfig.dtd");
						// transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
						transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

						DOMSource source = new DOMSource(doc);

						// Output to console for testing
						// StreamResult consoleResult = new StreamResult(System.out);
						// transformer.transform(source, consoleResult);

						// write the content into xml file
						StreamResult result = new StreamResult(new File(cfg_zlimport));
						transformer.transform(source, result);

						// File Historisieren
						if(dataAgent == null)
						{
							net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
							dataAgent = conn.getDataAgent();
						}

						Date actualDate = new Date(System.currentTimeMillis());
						String actualTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());

						TemplateReader tr = TemplateReader.getInstance();
						DynGenDataObj zinslistenconfigfileDgd = tr.getDGDForTemplate("ICRS.administration.zinslistenconfigfile", global, session);
						zinslistenconfigfileDgd.set("var.name", "Zinslistenconfig vom " + actualTime);
						zinslistenconfigfileDgd.setDate("var.aktivierung", actualDate);
						String configOid = dataAgent.storeObject(zinslistenconfigfileDgd, zinslistenconfigfileDgd.getTemplateType(), null, session);

						Path path = Path.of(cfg_zlimport);
						byte[] data = Files.readAllBytes(path);

						Hashtable fparams = new Hashtable();
						fparams.put("size", "" + data.length);
						fparams.put("paramname", "zinslistenfile");
						fparams.put("name", "zinslistenconfig.xml");
						fparams.put("type", "xml");
						fparams.put("Content-Type", "application/xml");
						fparams.put("OID", configOid);

						if(fileDataAgent == null)
						{
							Connector conn = null;
							conn = new Connector();
							fileDataAgent = conn.getFileDataAgent();
						}

						// Create a unique file reference
						Long ctr = CoolDataTool.generateUniqueSequence(cfg_zlimport);
						String filereferencename = cfg_zlimport.substring(0, cfg_zlimport.indexOf(".xml")) + ctr + ".xml";
						String zlconfigfile = fileDataAgent.storeObject(filereferencename, data, fparams);

						zinslistenconfigfileDgd.set("var.datei", "FILE_" + zlconfigfile);
						configOid = dataAgent.storeObject(zinslistenconfigfileDgd, zinslistenconfigfileDgd.getTemplateType(), configOid, session);

						// File an redmine uebergeben -> feedback_init
						tr = TemplateReader.getInstance();
						DynGenDataObj dgdFeedback = tr.getDGDForTemplate("System.feedback", global, session);
						dgdFeedback.set("var.name", "Zinslistenconfig.xml upgedatet");
						dgdFeedback.set("var.datei1", "FILE_" + zlconfigfile);
						dataAgent.storeObject(dgdFeedback, dgdFeedback.getTemplateType(), null, session);
					}
				}
			}
		}
		catch(Exception e)
		{
			debug.log(e);
		}

	}

	/**
	 * Gets the mapping changes vector from JSON stored in var.mappingchanges.
	 *
	 * @return the vector of mapping changes
	 */
	private ArrayList<HashMap<String, String>> getMappingChangesVector()
	{
		String mappingchanges = session.getString("var.mappingchanges");
		ArrayList mappingchangesV = new ArrayList<HashMap<String, String>>();
		if(mappingchanges.length() == 0)
		{
			return mappingchangesV;
		}

		try
		{
			JSONArray jsonArray = new JSONArray(mappingchanges);
			for(int i = 0; i < jsonArray.length(); i++)
			{
				JSONObject j = jsonArray.optJSONObject(i);
				HashMap<String, String> data = new HashMap<String, String>();
				String oldname = j.getString("oldname");
				String newname = j.getString("newname");
				if(null != oldname && null != newname)
				{
					data.put("oldname", "" + oldname);
					data.put("newname", "" + newname);
					mappingchangesV.add(data);
				}
			}
		}
		catch(JSONException e)
		{
			// TODO Auto-generated catch block
			debug.error(e);
		}
		return mappingchangesV;
	}

	/**
	 * Gets the value map for a selector.
	 * 
	 * Creates a mapping between selector alternatives and their text values.
	 * Supports language-dependent text alternatives.
	 * Results are cached in the mapper field for performance.
	 * 
	 * @param myeselector the selector template type
	 * @return HashMap containing value mappings
	 */
	public HashMap getValueMap(String myeselector)
	{
		// geflavourt???
		String flav = session.getString("flavour");
		if(flav.length() > 0)
		{
			flav = "_" + flav;
		}

		if(null == mapper)
		{
			mapper = new HashMap();
		}
		HashMap value_map = (HashMap)mapper.get(myeselector + flav);
		if(null != value_map)
		{
			return value_map;
		}

		TemplateReader profil = TemplateReader.getInstance();
		// flavoured neu RK20131017
		DynGenDataObj dgdOrig = profil.getDGDForTemplate(myeselector, global, session, true);
		Selector dgd = new Selector(dgdOrig);

		HashMap resvals = new HashMap();
		try
		{

			String alternatives = (String)dgd.get("var.alternatives");

			// LANGUAGE DEPENDENT
			String language = (String)session.get("language");
			if(null == language)
			{
				language = "";
			}

			String textalternatives = (String)dgd.get("var.textalternatives" + language);
			if(null == textalternatives)
			{
				textalternatives = "";
			}
			if(textalternatives.length() == 0)
			{
				textalternatives = (String)dgd.get("var.textalternatives");
				if(null == textalternatives)
				{
					textalternatives = "";
				}
			}

			String[] vals = CoolStringTool.splitFast(alternatives, "\\|");
			String[] text = CoolStringTool.splitFast(textalternatives, "\\|");
			for(int i = 0; i < vals.length; i++)
			{
				String key = vals[i];
				String val = vals[i];
				if(text.length > i)
				{
					key = text[i];
					// url decoding
					key = EncodingHelper.cleanUMLonly(key);
				}
				resvals.put(key, val);
				resvals.put(key.toLowerCase(), val);
				resvals.put(val, val);
				resvals.put(val.toLowerCase(), val);
			}

		}
		catch(Exception xx)
		{
			;
		}

		mapper.put(myeselector + flav, resvals);
		return resvals;
	}

	/**
	 * get the encoding/Collation for the db<br>
	 * necessary for correct mapping of slots<br>
	 * <b>Only for MSSQL at the moment</b>.
	 */
	private void setDBEncoding()
	{
		dbEncoding = null;

		String sqlstyle = (String)CfgSingleton.getInstance().get("sqlstyle");

		if(sqlstyle != null && sqlstyle.equalsIgnoreCase("MSSQL"))
		{
			String sql = null;

			if(dataAgent == null)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			String databasename = (String)CfgSingleton.getInstance().get("databasename");

			if(databasename != null && !databasename.equals(""))
			{
				sql = "SELECT CAST(DATABASEPROPERTYEX('" + databasename + "','Collation') AS VARCHAR(8000)) collation";
			}
			else
			{
				sql = "SELECT SERVERPROPERTY('Collation') collation";
			}

			Vector<Hashtable<String, String>> result;

			QueryResult qr;
			try
			{
				qr = dataAgent.queryPlainSQLwithResult(sql);
				result = qr.getResult();
			}
			catch(RemoteException e)
			{
				result = null;
				debug.error(this, "Could not retrieve collaction (encoding) of db " + databasename);
				debug.error(e);
			}

			if(result != null)
			{
				for(int i = 0; i < result.size(); i++)
				{
					Hashtable<String, String> row = result.get(i);
					if(row != null)
					{
						String collation = row.get("collation");
						if(collation != null && !collation.equals(""))
						{
							dbEncoding = collation;
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * convert {@link #originalString} to the encoding given by the db<br>
	 * necessary for correct mapping of slots<br>
	 * <b>Only for MSSQL at the moment</b>.
	 *
	 * @param originalString
	 *            the original string
	 * @return the DB encoded value of string
	 */
	protected String getDBEncodedValueOfString(String originalString)
	{
		String dbEncodedString = null;
		if(dbEncoding != null)
		{
			if(dataAgent == null)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}
			Vector<Hashtable<String, String>> result;
			QueryResult qr;
			try
			{
				qr = dataAgent.queryPlainSQLwithResult("select CAST('" + originalString + "' AS varchar(8000)) COLLATE " + dbEncoding + " encodedstring");
				result = qr.getResult();
			}
			catch(RemoteException e)
			{
				result = null;
				debug.error(this, "Could not query encoded value of string '" + originalString + "'. Encoding (collation) was '" + dbEncoding + "'.");
				debug.error(e);
			}

			if(result != null)
			{
				;
			}
			{
				for(int i = 0; i < result.size(); i++)
				{
					Hashtable<String, String> row = result.get(i);
					if(row != null)
					{
						dbEncodedString = row.get("encodedstring");
						if(dbEncodedString != null && !dbEncodedString.equals(""))
						{
							break;
						}
					}
				}
			}

		}
		else
		{
			dbEncodedString = originalString;
			debug.log(this, "UploadXLS4 - No db encoding (collation) give. Name of slot object not converted to db encoded name!");
		}

		return dbEncodedString;
	}
	
	/**
	 * Gets the var arg.
	 *
	 * @param argName
	 *            the arg name
	 * @return the var arg
	 */
	private String getVarArg(String argName)
	{
		String value = session.getString("var." + argName);
		if(value == null || value.equals(""))
		{
			value = session.getString("arg." + argName);
		}
		if((value == null || value.equals("")) && session != null)
		{
			value = session.getString("arg.oid" + session.getString("id").trim() + "." + argName);
		}
		if((value == null || value.equals("")) && session != null)
		{
			value = session.getString("arg.oid" + session.getString("volatile_id") + "." + argName);
		}
		return value;
	}
	
	/**
	 * Mofify document. Anpassung der Zinslistenconfig (XML File wird geschrieben)
	 *
	 * @param doc
	 *            the doc
	 * @return true, if successful
	 */
	private boolean mofifyDocument(Document doc)
	{
		// Placeholder - this method needs to be extracted from UploadXLS5.java
		// It's too large to include in initial extraction
		return false;
	}
}
