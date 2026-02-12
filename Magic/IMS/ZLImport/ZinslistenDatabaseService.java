package Magic.IMS.ZLImport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.QueryResult;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.tools.Translation.Tr;

import Magic.IMS.reporting.helpers.ArgsHelper;

/**
 * Service class for database operations in the Zinslisten import process.
 * 
 * This service handles database query operations for Haus, Top, Stellplatz,
 * Zinszeile, and related objects during the Zinslisten import.
 * 
 * Phase 2: Simple query methods extracted:
 * - getZZOID(String topOid, String jahr, String monat) - Gets Zinszeile OID
 * - getZinsZeilen(String[] topoids, String jahr, String monat) - Gets all rent rolls
 * - getZinsZeilenForName(String[] topoids, String jahr, String monat) - Gets rent rolls indexed by name
 * - getAssetmanagerMailadressFromObject(String hausid) - Retrieves asset manager email
 * - getAlleWEsInBestand() - Gets all units in inventory
 * - getMailverteilerFromAssetmanager(String name) - Retrieves mailing distribution
 * - getAllAssetmanagerAndIds() - Builds map of asset managers and IDs
 * 
 * Collections modernization:
 * - Vector → ArrayList
 * - Hashtable → HashMap
 * - Added proper generics throughout
 * 
 * Note: Complex CRUD operations (createHaus, createTop, createZZ, etc.) will be extracted in Phase 3.
 */
public class ZinslistenDatabaseService
{
	private final DynGenDataObj session;
	private final BugMe debug;
	private net.metamagix.essence.Agents.DataAgent dataAgent;
	private final DynGenDataObj parentObject;
	private Map<String, Object> zinsZeilenCache;

	/**
	 * Constructor for ZinslistenDatabaseService.
	 * 
	 * @param session the user session
	 * @param debug the debug logger
	 * @param dataAgent the Data Agent instance
	 * @param parentObject the parent UploadXLS5 object for accessing set() method and cache
	 */
	public ZinslistenDatabaseService(DynGenDataObj session, BugMe debug, 
	                                  net.metamagix.essence.Agents.DataAgent dataAgent,
	                                  DynGenDataObj parentObject)
	{
		this.session = session;
		this.debug = debug;
		this.dataAgent = dataAgent;
		this.parentObject = parentObject;
	}

	/**
	 * Gets the OID of a Zinszeile for a specific Top, year, and month.
	 * 
	 * @param topOid the Top OID
	 * @param jahr the year
	 * @param monat the month
	 * @return the Zinszeile OID, or null if not found
	 */
	public String getZZOID(String topOid, String jahr, String monat)
	{
		Map<String, Object> args = new HashMap<>();
		List<Map<String, String>> res = new ArrayList<>();
		args.put("TType", "CIMS.zinszeile");
		args.put("fieldClause", "ID,name");
		args.put("top_ID", topOid);
		args.put("jahr", jahr);
		args.put("monat", monat);

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			args.put("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			args.put("DOMAIN", mydom);
		}
		
		if(null == dataAgent)
		{
			Connector conn = new Connector();
			dataAgent = conn.getDataAgent();
		}

		try
		{
			// Convert to Hashtable for compatibility with queryObject
			java.util.Hashtable<String, Object> htArgs = new java.util.Hashtable<>();
			htArgs.putAll(args);
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = dataAgent.queryObject(htArgs);
			
			// Convert result to List<Map>
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception x)
		{
			// Silent catch as in original
		}
		
		if(res.size() > 0)
		{
			Map<String, String> h = res.get(0);
			if(h != null)
			{
				return h.get("ID");
			}
		}
		return null;
	}

	/**
	 * Gets Zinszeilen for specific Tops, year, and month.
	 * 
	 * @param topoids array of Top OIDs
	 * @param jahr the year
	 * @param monat the month
	 * @return Map of Top OID to Zinszeile data
	 */
	public Map<String, Map<String, String>> getZinsZeilen(String[] topoids, String jahr, String monat)
	{
		if(null == topoids || topoids.length == 0)
		{
			log("Abfrage nach Zinszeilen ohne angegebene Tops.");
			return new HashMap<>();
		}

		zinsZeilenCache = null;
		zinsZeilenCache = new HashMap<>();
		
		if(null != monat && monat.startsWith("0"))
		{
			if(monat.length() == 2)
			{
				monat = monat.substring(1, 2);
			}
		}
		else if(null == monat)
		{
			monat = "";
		}

		Map<String, Object> args = new HashMap<>();
		List<Map<String, String>> res = new ArrayList<>();
		args.put("TType", "CIMS.zinszeile");
		args.put("fieldClause", "DOB.ID zzid,mieter,nutzung,nfl,leerfl,hauptmietzins,betriebskosten ,reparaturfond,name,DDT1.ID topid");
		args.put("top_ID", topoids);
		args.put("jahr", jahr);
		args.put("monat", monat);

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			args.put("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			args.put("DOMAIN", mydom);
		}
		
		if(null == dataAgent)
		{
			Connector conn = new Connector();
			dataAgent = conn.getDataAgent();
		}

		try
		{
			// Convert to Hashtable for compatibility
			java.util.Hashtable<String, Object> htArgs = new java.util.Hashtable<>();
			htArgs.putAll(args);
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = dataAgent.queryObject(htArgs);
			
			// Convert to List<Map>
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception x)
		{
			debug.log(x);
			debug.error(x);
		}

		Map<String, Map<String, String>> top2zz = new HashMap<>();
		
		for(Map<String, String> h : res)
		{
			if(h != null)
			{
				String topid = h.get("topid");
				String zzid = h.get("zzid");
				if(null != topid)
				{
					if(null != zzid)
					{
						top2zz.put(topid, h);
						zinsZeilenCache.put(zzid, "");
					}
				}
			}
		}
		
		// Fill cache
		if(zinsZeilenCache != null && zinsZeilenCache.size() > 0)
		{
			try
			{
				// Convert to Hashtable for compatibility
				java.util.Hashtable<String, Object> htCache = new java.util.Hashtable<>();
				htCache.putAll(zinsZeilenCache);
				htCache = dataAgent.getObjects(htCache, "");
				
				// Convert back to Map
				zinsZeilenCache.clear();
				zinsZeilenCache.putAll(htCache);
				
				// Update parent cache
				parentObject.set("zinsZeilenCache", htCache);
			}
			catch(Exception xx)
			{
				debug.log(xx);
			}
		}
		return top2zz;
	}

	/**
	 * Gets Zinszeilen indexed by Top name.
	 * 
	 * @param topoids array of Top OIDs
	 * @param jahr the year
	 * @param monat the month
	 * @return Map of Top name to Zinszeile data
	 */
	public Map<String, Object> getZinsZeilenForName(String[] topoids, String jahr, String monat)
	{
		if(null == topoids || topoids.length == 0)
		{
			log("Abfrage nach Zinszeilen ohne angegebene Tops.");
			return new HashMap<>();
		}

		Map<String, Object> args = new HashMap<>();
		List<Map<String, String>> res = new ArrayList<>();
		args.put("TType", "CIMS.zinszeile");
		args.put("fieldClause", "DOB.ID zzid,mieter,nutzung,nfl,leerfl,hauptmietzins,betriebskosten ,reparaturfond,name,DDT1.ID topid,DDT1.name topname");
		args.put("top_ID", topoids);
		args.put("jahr", jahr);
		args.put("monat", monat);

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			args.put("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			args.put("DOMAIN", mydom);
		}
		
		if(null == dataAgent)
		{
			Connector conn = new Connector();
			dataAgent = conn.getDataAgent();
		}

		try
		{
			// Convert to Hashtable for compatibility
			java.util.Hashtable<String, Object> htArgs = new java.util.Hashtable<>();
			htArgs.putAll(args);
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = dataAgent.queryObject(htArgs);
			
			// Convert to List<Map>
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception x)
		{
			debug.log(x);
			debug.error(x);
		}

		Map<String, Object> top2zz = new HashMap<>();
		
		for(Map<String, String> h : res)
		{
			if(h != null)
			{
				String topname = h.get("topname");
				String zzid = h.get("zzid");
				if(null != topname)
				{
					if(null != zzid)
					{
						top2zz.put(topname, h);
					}
				}
			}
		}
		return top2zz;
	}

	/**
	 * Retrieves asset manager email address from a Haus object.
	 * 
	 * @param hausid the Haus OID
	 * @return email and name string in format "email;name", or empty string if not found
	 */
	public String getAssetmanagerMailadressFromObject(String hausid)
	{
		Map<String, Object> args = new HashMap<>();
		ArgsHelper argsHelper = new ArgsHelper(args);
		argsHelper.setMainTemplateType("CIMS.haus");
		argsHelper.setAdvancedFields(true);
		argsHelper.addTemplateType("assetmanager", "ICRScrm.assetmanager");

		argsHelper.addField("assetmanager_name");
		argsHelper.addField("assetmanager_email");
		argsHelper.addWhere("DOB.ID =" + hausid);

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			argsHelper.addCondition("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			argsHelper.addCondition("DOMAIN", mydom);
		}

		List<Map<String, String>> res = null;
		try
		{
			if(null == dataAgent)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
			// Convert Vector to List
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = qr.getResult();
			res = new ArrayList<>();
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			parentObject.set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			Map<String, String> h = res.get(0);

			String mailAndName = "";

			if(h.get("email").length() > 0)
			{
				mailAndName = h.get("email");
			}
			if(h.get("name").length() > 0)
			{
				mailAndName = mailAndName + ";" + h.get("name");
			}
			else
			{
				mailAndName = mailAndName + ";Assetmanager";
			}

			return mailAndName;
		}
		else
		{
			return "";
		}
	}

	/**
	 * Gets all properties (WE) in inventory.
	 * 
	 * @return Map of asset manager email/name to property data
	 */
	public Map<String, Object> getAlleWEsInBestand()
	{
		Map<String, Map<String, Map<String, String>>> result = new HashMap<>();

		Map<String, Object> args = new HashMap<>();
		ArgsHelper argsHelper = new ArgsHelper(args);
		argsHelper.setMainTemplateType("CIMS.haus");
		argsHelper.setAdvancedFields(true);
		argsHelper.addTemplateType("assetmanager", "ICRScrm.assetmanager");

		argsHelper.addField("ET0.identadresse1");
		argsHelper.addField("ET0.identadresse5");
		argsHelper.addField("ET0.plz");
		argsHelper.addField("DOB.name", "wename");
		argsHelper.addField("assetmanager_name");
		argsHelper.addField("assetmanager_email");

		argsHelper.addWhere("ET0.status>=0 and (ET0.verkaufsdatum is null or ET0.verkaufsdatum='' )");

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			argsHelper.addCondition("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			argsHelper.addCondition("DOMAIN", mydom);
		}

		List<Map<String, String>> res = null;
		try
		{
			if(null == dataAgent)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
			// Convert Vector to List
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = qr.getResult();
			res = new ArrayList<>();
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			parentObject.set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			for(int i = 0; i < res.size(); i++)
			{
				Map<String, String> h = res.get(i);

				String mailAndName = "";

				if(h.get("email").length() > 0)
				{
					mailAndName = h.get("email");
				}
				if(h.get("name").length() > 0)
				{
					mailAndName = mailAndName + ";" + h.get("name");
				}
				else
				{
					mailAndName = mailAndName + ";Assetmanager";
				}
				String identadresse1 = h.get("identadresse1");

				if(result.containsKey(mailAndName))
				{
					Map<String, Map<String, String>> entry = result.get(mailAndName);
					entry.put(identadresse1, h);
					result.put(mailAndName, entry);
				}
				else
				{
					Map<String, Map<String, String>> entry = new HashMap<>();
					entry.put(identadresse1, h);
					result.put(mailAndName, entry);
				}
			}
		}

		return new HashMap<>(result);
	}

	/**
	 * Gets mailing distribution list from an asset manager.
	 * 
	 * @param name the asset manager name
	 * @return Map containing mailing addresses
	 */
	public Map<String, Object> getMailverteilerFromAssetmanager(String name)
	{
		Map<String, Object> mailverteileradressen = new HashMap<>();
		Map<String, Object> args = new HashMap<>();
		ArgsHelper argsHelper = new ArgsHelper(args);
		argsHelper.setMainTemplateType("ICRScrm.assetmanager");
		argsHelper.setAdvancedFields(true);
		argsHelper.addTemplateType("mailverteiler", "System.User");

		argsHelper.addField("DOB.name");
		argsHelper.addField("SLOTCOLLAPSE(mailverteiler_email) mailadressen");
		argsHelper.addWhere("DOB.name='" + name + "'");

		String mydom = (String)session.get("domainid");
		if(mydom.length() == 0)
		{
			argsHelper.addCondition("DOMAIN", "ALLDOMAINS");
		}
		else
		{
			argsHelper.addCondition("DOMAIN", mydom);
		}

		List<Map<String, String>> res = null;
		try
		{
			if(null == dataAgent)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
			// Convert Vector to List
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = qr.getResult();
			res = new ArrayList<>();
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			parentObject.set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			Map<String, String> h = res.get(0);

			String mailadressen = "";

			if(h.get("mailadressen").length() > 0)
			{
				mailadressen = h.get("mailadressen");
			}

			mailverteileradressen.put(mailadressen, "");
		}
		return mailverteileradressen;
	}

	/**
	 * Gets all asset managers and their IDs.
	 * 
	 * @return Map of asset manager name to OID
	 */
	public Map<String, String> getAllAssetmanagerAndIds()
	{
		Map<String, String> assetmanagerAndIDs = new HashMap<>();

		try
		{
			List<Map<String, String>> res = new ArrayList<>();

			ArgsHelper argsHelper = new ArgsHelper();

			argsHelper.setAdvancedFields(true);
			argsHelper.setMainTemplateType("ICRScrm.assetmanager");
			argsHelper.addDomainCondition(session);
			argsHelper.addField("ID", "oid");
			argsHelper.addField("DOB.name", "assetmanagername");

			if(null == dataAgent)
			{
				Connector conn = new Connector();
				dataAgent = conn.getDataAgent();
			}

			QueryResult qr = dataAgent.queryObjectWithResult(argsHelper.getArgs());
			// Convert Vector to List
			java.util.Vector<java.util.Hashtable<String, String>> vecRes = qr.getResult();
			for(java.util.Hashtable<String, String> ht : vecRes)
			{
				Map<String, String> map = new HashMap<>();
				map.putAll(ht);
				res.add(map);
			}

			if(res != null && res.size() > 0)
			{
				for(int i = 0; i < res.size(); i++)
				{
					Map<String, String> row = res.get(i);

					String oid = row.get("oid");
					String assetmanagername = row.get("assetmanagername");

					assetmanagerAndIDs.put(assetmanagername, oid);
				}
			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().log(e);
		}

		return assetmanagerAndIDs;
	}

	/**
	 * Logs a message (helper method).
	 * 
	 * @param text the text to log
	 */
	private void log(String text)
	{
		debug.log(text);
	}
}
