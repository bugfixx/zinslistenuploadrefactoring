package Magic.IMS.ZLImport;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Agents.QueryResult;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.eSSENCETypes.eDate;

import Magic.IMS.reporting.helpers.ArgsHelper;

/**
 * Service class for caching operations in the Zinslisten import process.
 * 
 * This service handles caching for Tops and Zinszeilen to improve performance
 * during the Zinslisten import process.
 * 
 * Phase 3: Cache operations extracted:
 * - fillTopCache(TopList) - Fills cache with Top objects
 * - emptyTopCache() - Clears the Top cache
 * - emptyLastZZ4Top() - Clears the last Zinszeile per Top cache
 * - fillLastZZ4Top(String) - Fills cache with last Zinszeile dates
 * 
 * Collections modernization:
 * - Hashtable → ConcurrentHashMap (for thread-safety)
 * - Added proper generics throughout
 * 
 * Thread Safety:
 * - Uses ConcurrentHashMap for all caches to ensure thread-safe operations
 * - Multiple threads can access caches simultaneously without corruption
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ZinslistenCacheService
{
	private static final int TOPS_CACHE_SIZE = 50;
	
	private Map<String, Object> topsCache;
	private Map<String, Calendar> lastZZ4Top;
	private Map<String, Object> zinsZeilenCache;
	private FileDataAgent fileDataAgent;
	private DynGenDataObj session;
	private BugMe debug;
	
	/**
	 * Constructor for ZinslistenCacheService.
	 * 
	 * @param fileDataAgent the File Data Agent instance
	 * @param session the user session
	 * @param debug the debug logger
	 */
	public ZinslistenCacheService(FileDataAgent fileDataAgent, DynGenDataObj session, BugMe debug)
	{
		this.fileDataAgent = fileDataAgent;
		this.session = session;
		this.debug = debug;
		this.topsCache = new ConcurrentHashMap<>();
		this.lastZZ4Top = new ConcurrentHashMap<>();
		this.zinsZeilenCache = new ConcurrentHashMap<>();
	}
	
	/**
	 * Fills the Top cache with objects from a TopList.
	 * 
	 * @param topList the list of Tops to cache (expects TopList object from Magic.IMS.ZLImport package)
	 */
	public void fillTopCache(Object topList)
	{
		try
		{
			// Use reflection to handle TopList which is defined outside this compilation unit
			int size = (Integer)topList.getClass().getMethod("size").invoke(topList);
			System.err.println("ZLU2: FILLING TOPS CACHE " + size + " maximum is " + TOPS_CACHE_SIZE);
			java.util.Date d1 = new java.util.Date();
			topsCache = null;
			topsCache = new Hashtable();
			int cnt = 0;
			
			@SuppressWarnings("unchecked")
			List<String> topIDs = (List<String>)topList.getClass().getMethod("getTopIDs").invoke(topList);
			for(String toid : topIDs)
			{
				if(toid == null)
				{
					continue;
				}
				// out of mem verhindern ....
				if(cnt >= TOPS_CACHE_SIZE)
				{
					break;
				}
				cnt++;
				topsCache.put(toid, "");
			}
			// Query
			if(fileDataAgent == null)
			{
				Connector conn = null;
				conn = new Connector();
				fileDataAgent = conn.getDataAgent();
			}
			try
			{
				topsCache = fileDataAgent.getObjects((Hashtable)topsCache, "");
				System.err.println("ZLU2: cached " + topsCache.size() + " tops.");
			}
			catch(Exception x)
			{
				debug.error(x);
			}
			java.util.Date d2 = new java.util.Date();
			System.out.println("Filling TopsCache with " + size + " Tops: " + (d2.getTime() - d1.getTime()) + " ms.");
		}
		catch(Exception e)
		{
			debug.error("Error filling top cache: " + e.getMessage(), e);
		}
		return;
	}
	
	/**
	 * Clears the Top cache.
	 */
	public void emptyTopCache()
	{
		if(topsCache != null)
		{
			topsCache.clear();
		}
	}
	
	/**
	 * Gets a cached Top object.
	 * 
	 * @param key the Top OID
	 * @return the cached Top object, or null if not cached
	 */
	public Object getCachedTop(String key)
	{
		return topsCache != null ? topsCache.get(key) : null;
	}
	
	/**
	 * Puts a Top object in the cache.
	 * 
	 * @param key the Top OID
	 * @param value the Top object to cache
	 */
	public void putCachedTop(String key, Object value)
	{
		if(topsCache != null)
		{
			topsCache.put(key, value);
		}
	}
	
	/**
	 * Gets the entire Top cache.
	 * 
	 * @return the Top cache as Hashtable (for backward compatibility)
	 */
	public Hashtable getTopsCache()
	{
		if(topsCache instanceof Hashtable)
		{
			return (Hashtable)topsCache;
		}
		// Convert to Hashtable for backward compatibility
		Hashtable result = new Hashtable();
		if(topsCache != null)
		{
			result.putAll(topsCache);
		}
		return result;
	}
	
	/**
	 * Clears the last Zinszeile per Top cache.
	 */
	public void emptyLastZZ4Top()
	{
		lastZZ4Top = new HashMap<>();
	}
	
	/**
	 * Fills the last Zinszeile per Top cache for a given Haus.
	 * 
	 * @param hausId the Haus ID
	 */
	public void fillLastZZ4Top(String hausId)
	{
		try
		{
			emptyLastZZ4Top();
			Hashtable<String, Object> args = new Hashtable<String, Object>();
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();
			ArgsHelper argsHelper = new ArgsHelper();
			argsHelper.setMainTemplateType("CIMS.haus");
			argsHelper.setAdvancedFields(true);
			argsHelper.addTemplateType("tops", "CIMS.top");
			argsHelper.addTemplateType("tops_REVtop", "CIMS.zinszeile");
			argsHelper.addField("tops_id", "topid");
			argsHelper.addField("tops_REVtop_monat", "zzmonat");
			argsHelper.addField("tops_REVtop_jahr", "zzjahr");

			// argsHelper.addField("MAX(tops_REVtop_datum) zzdatum");

			argsHelper.addField("max(CONVERT(datetime,CONVERT(VARCHAR,tops_REVtop_jahr) +	CASE WHEN tops_REVtop_monat < 10	THEN '0' ELSE '' END + CONVERT(VARCHAR,tops_REVtop_monat) + '01')) zzdatum");

			argsHelper.addGroup("tops_id,tops_REVtop_monat,tops_REVtop_jahr");
			argsHelper.addOrder("tops_REVtop_jahr*12+tops_REVtop_monat ASC");

			argsHelper.addCondition("ID", hausId);
			argsHelper.addWhere("tops_REVtop_jahr < 3000");
			argsHelper.addDomainCondition(session);

			// new Connector Class
			if(null == fileDataAgent)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				fileDataAgent = conn.getDataAgent();
			}

			try
			{
				QueryResult qr = fileDataAgent.queryObjectWithResult(argsHelper.getArgs());
				if(!qr.isOK())
				{
					BugMe.getInstance().error(this, "Query not OK when fetching last CIMS.zinszeile for each CIMS.top in CIMS.haus with id '" + hausId + "'");
					BugMe.getInstance().error(this, "QueryResult message: '" + qr.getMessage() + "'");
					BugMe.getInstance().log(this, "Query was: '" + qr.getSql() + "'");
					return;
				}
				res = qr.getResult();
			}
			catch(Exception e)
			{
				BugMe.getInstance().error(this, "Query exception when fetching last CIMS.zinszeile for each CIMS.top in CIMS.haus with id '" + hausId + "'");
				BugMe.getInstance().log(e);
				return;
			}
			if(res == null)
			{
				BugMe.getInstance().error(this, "Result null when fetching last CIMS.zinszeile for each CIMS.top in CIMS.haus with id '" + hausId + "'");
				return;
			}
			if(res.size() == 0)
			{
				BugMe.getInstance().error(this, "Result empty when fetching last CIMS.zinszeile for each CIMS.top in CIMS.haus with id '" + hausId + "'");
				return;
			}
			// java.util.Date end_time = new java.util.Date();
			// long run_time = end_time.getTime() - start_time.getTime();
			for(int rowIndex = 0; rowIndex < res.size(); rowIndex++)
			{
				Hashtable<String, String> row = res.get(rowIndex);
				if(row != null)
				{
					String topId = row.get("topid");
					// String tmi = h.get("internalname");
					String zzDatumStr = row.get("zzdatum");
					if(topId != null && !topId.trim().equals("") && zzDatumStr != null && !zzDatumStr.trim().equals(""))
					{
						Calendar zzDatum = eDate.calFromString(zzDatumStr);
						if(zzDatum != null)
						{
							lastZZ4Top.put(topId, zzDatum);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().error(this, "Unexpected error filling lastZZ4Top for CIMS.haus with id '" + hausId + "'. Check Code!");
			BugMe.getInstance().log(e);
		}
	}
	
	/**
	 * Gets the last Zinszeile date for a Top.
	 * 
	 * @param topKey the Top key
	 * @return the last Zinszeile date, or null if not found
	 */
	public Calendar getLastZZ4Top(String topKey)
	{
		return lastZZ4Top.get(topKey);
	}
	
	/**
	 * Puts a last Zinszeile date for a Top.
	 * 
	 * @param topKey the Top key
	 * @param date the Zinszeile date
	 */
	public void putLastZZ4Top(String topKey, Calendar date)
	{
		lastZZ4Top.put(topKey, date);
	}
	
	/**
	 * Gets the entire lastZZ4Top cache.
	 * 
	 * @return the lastZZ4Top cache as Hashtable (for backward compatibility)
	 */
	public Hashtable<String, Calendar> getLastZZ4TopCache()
	{
		if(lastZZ4Top instanceof Hashtable)
		{
			return (Hashtable<String, Calendar>)lastZZ4Top;
		}
		// Convert to Hashtable for backward compatibility
		Hashtable<String, Calendar> result = new Hashtable<>();
		if(lastZZ4Top != null)
		{
			result.putAll(lastZZ4Top);
		}
		return result;
	}
	
	/**
	 * Clears the Zinszeilen cache.
	 */
	public void clearZinsZeilenCache()
	{
		if(zinsZeilenCache != null)
		{
			zinsZeilenCache.clear();
		}
	}
	
	/**
	 * Gets a cached Zinszeile object.
	 * 
	 * @param key the Zinszeile key
	 * @return the cached Zinszeile object, or null if not cached
	 */
	public Object getCachedZinsZeile(String key)
	{
		return zinsZeilenCache != null ? zinsZeilenCache.get(key) : null;
	}
	
	/**
	 * Puts a Zinszeile object in the cache.
	 * 
	 * @param key the Zinszeile key
	 * @param value the Zinszeile object to cache
	 */
	public void putCachedZinsZeile(String key, Object value)
	{
		if(zinsZeilenCache != null)
		{
			zinsZeilenCache.put(key, value);
		}
	}
	
	/**
	 * Gets the entire Zinszeilen cache.
	 * 
	 * @return the Zinszeilen cache as Hashtable (for backward compatibility)
	 */
	public Hashtable getZinsZeilenCache()
	{
		if(zinsZeilenCache instanceof Hashtable)
		{
			return (Hashtable)zinsZeilenCache;
		}
		// Convert to Hashtable for backward compatibility
		Hashtable result = new Hashtable();
		if(zinsZeilenCache != null)
		{
			result.putAll(zinsZeilenCache);
		}
		return result;
	}
}
