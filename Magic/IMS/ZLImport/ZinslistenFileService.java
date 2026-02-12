package Magic.IMS.ZLImport;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.MConfig.CfgSingleton;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.tools.CoolStringTool;
import net.metamagix.essence.tools.Translation.Tr;

import Magic.IMS.TopoTool;
import Magic.IMS.XMLConfig;
import Magic.IMS.Zinsliste;

/**
 * Service class for file operations in the Zinslisten import process.
 * 
 * This service handles reading and processing Zinslisten from various file sources.
 * Extracted from UploadXLS5.java for better separation of concerns.
 * 
 * Phase 2: Fully extracted methods with modernized collections:
 * - readListe(String file) - Reads a Zinsliste from a file
 * - readQuellsystemListe(List quellsystemResult, String quellsystem) - Reads from source system
 * - getZinsliste(String file, int index) - Gets a specific Zinsliste from a file
 * - getZinsliste(String file, int index, List quellsystemResult, String quellsystem) - Gets Zinsliste with source system support
 * 
 * Collections modernization:
 * - Vector → ArrayList
 * - Hashtable → HashMap
 * - Added proper generics throughout
 */
public class ZinslistenFileService
{
	private final DynGenDataObj session;
	private final BugMe debug;
	private FileDataAgent fileDataAgent;
	private final boolean evaluateFormulas;
	private final ZLImportProtocol zlprotocol;
	private final XMLConfig xmlConfig;
	private final DynGenDataObj parentObject;
	
	// File caching fields
	private transient byte[] cachedContent;
	private String cachedFile;
	private transient ZinslistenImport zinslistenImport;

	/**
	 * Constructor for ZinslistenFileService.
	 * 
	 * @param session the user session
	 * @param debug the debug logger
	 * @param fileDataAgent the File Data Agent instance
	 * @param evaluateFormulas whether to evaluate formulas in files
	 * @param zlprotocol the import protocol logger
	 * @param xmlConfig the XML configuration
	 * @param parentObject the parent UploadXLS5 object for accessing set() method
	 */
	public ZinslistenFileService(DynGenDataObj session, BugMe debug, 
	                              FileDataAgent fileDataAgent, boolean evaluateFormulas,
	                              ZLImportProtocol zlprotocol, XMLConfig xmlConfig,
	                              DynGenDataObj parentObject)
	{
		this.session = session;
		this.debug = debug;
		this.fileDataAgent = fileDataAgent;
		this.evaluateFormulas = evaluateFormulas;
		this.zlprotocol = zlprotocol;
		this.xmlConfig = xmlConfig;
		this.parentObject = parentObject;
		this.cachedContent = null;
		this.cachedFile = "";
	}

	/**
	 * Reads a list of Zinslisten from a file.
	 * 
	 * @param file the file identifier
	 * @return List of Zinslisten, or null on error
	 */
	public List<Map<String, Object>> readListe(String file)
	{
		List<Map<String, Object>> liste = new ArrayList<>();
		
		try
		{
			if(fileDataAgent == null)
			{
				Connector conn = new Connector();
				fileDataAgent = conn.getFileDataAgent();
			}
			Map<String, Object> zlfparams = fileDataAgent.getParams(file);

			byte[] content = null;
			if(file.equals(cachedFile) && null != cachedContent)
			{
				content = cachedContent;
			}
			else
			{
				content = fileDataAgent.getObject(file);
				cachedContent = content;
				cachedFile = file;

				// Load configurations
				String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
				cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
				if(cfg_zlimport == null)
				{
					parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
					debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
				}

				String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
				cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
				if(cfg_currencyconfig == null)
				{
					parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
					debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
				}

				zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
				zinslistenImport.setLanguage(session.getString("language"));
				zinslistenImport.setEvaluateFormulas(evaluateFormulas);
			}

			ByteArrayInputStream bis = new ByteArrayInputStream(content);

			String ftype = (String)zlfparams.get("type");

			if(ftype.equals("pdf") && zinslistenImport.getZlTypeConfig() != null)
			{
				ftype = zinslistenImport.getZlTypeConfig().getFileType();
			}

			String thefilename = "";
			if(null != zlfparams)
			{
				thefilename = (String)zlfparams.get("name");
			}
			zinslistenImport.extractDateFromFilename(thefilename);

			// Read Zinslisten from file
			List<Zinsliste> rawListe = new ArrayList<>(zinslistenImport.getZinslistenInFile(bis, thefilename, ftype));
			
			// Convert to List<Map<String, Object>> for modernization
			for(Zinsliste zl : rawListe)
			{
				Map<String, Object> zlMap = new HashMap<>();
				zlMap.put("zinsliste", zl);
				liste.add(zlMap);
			}
			
			// Check if the file attached is without any data and provide message to the user
			if(liste.size() == 0)
			{
				zlprotocol.appendHtmlErr("<h2>" + Tr.t("noDataMessage", session.getString("language")) + "</h2>");
				session.set("CURRENT_VIEW", "ERRORQUEST");
				parentObject.set("var.importstop", "1");
				parentObject.set("var.errorcode", zlprotocol.getHtmlErr());
				return null;
			}

			// Set the config
			xmlConfig.getXMLConfig("hausverwaltung", zinslistenImport.getZlTypeConfig().getName() + "mieter");
			
			try
			{
				List<String> fehlerliste = new ArrayList<>(zinslistenImport.errors);
				if(fehlerliste.size() > 0)
				{
					for(int z = 0; z < fehlerliste.size(); z++)
					{
						zlprotocol.appendHtmlErr(fehlerliste.get(z) + "<br><br>\n");
					}
				}
			}
			catch(Exception xx)
			{
				debug.error(xx);
			}
		}
		catch(Exception zir)
		{
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");

			parentObject.set("var.importstop", "1");
			parentObject.set("var.errorcode", zir.getMessage());
			parentObject.set("var.errorcodetxt", zir.getMessage());
			debug.error(zir);
			debug.error("readListe in UploadXLS4:", zir);
			parentObject.set("dirty", "yes");
			return null;
		}

		if(null == liste || 0 == liste.size())
		{
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			parentObject.set("var.importstop", "1");
			parentObject.set("var.errorcode", zlprotocol.getHtmlErr());
			parentObject.set("var.errorcodetxt", Tr.t("textListUnreadable", session.getString("language")));
			parentObject.set("dirty", "yes");
			return null;
		}
		return liste;
	}

	/**
	 * Reads a list of Zinslisten from a source system.
	 * 
	 * @param quellsystemResult the result from source system query
	 * @param quellsystem the source system identifier
	 * @return List of Zinslisten, or null on error
	 */
	public List<Map<String, Object>> readQuellsystemListe(List<Map<String, Object>> quellsystemResult, String quellsystem)
	{
		List<Map<String, Object>> liste = new ArrayList<>();
		
		try
		{
			if(fileDataAgent == null)
			{
				Connector conn = new Connector();
				fileDataAgent = conn.getFileDataAgent();
			}
			
			String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
			cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
			if(cfg_zlimport == null)
			{
				parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			}

			String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
			cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
			if(cfg_currencyconfig == null)
			{
				parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
			}

			zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
			zinslistenImport.setLanguage(session.getString("language"));
			zinslistenImport.setEvaluateFormulas(evaluateFormulas);

			// Read from source system - convert to Vector for compatibility with ZinslistenImport
			java.util.Vector<java.util.Hashtable<String, Object>> vecResult = new java.util.Vector<>();
			if(quellsystemResult != null)
			{
				for(Map<String, Object> map : quellsystemResult)
				{
					java.util.Hashtable<String, Object> ht = new java.util.Hashtable<>();
					ht.putAll(map);
					vecResult.add(ht);
				}
			}
			
			List<Zinsliste> rawListe = new ArrayList<>(zinslistenImport.getZinslistenInFile(null, "", "", vecResult, quellsystem));
			
			// Convert to List<Map<String, Object>>
			for(Zinsliste zl : rawListe)
			{
				Map<String, Object> zlMap = new HashMap<>();
				zlMap.put("zinsliste", zl);
				liste.add(zlMap);
			}

			// Set the config
			String zltypeName = zinslistenImport.getZlTypeConfig().getName();
			parentObject.set("var.zltypename", zltypeName);
			xmlConfig.getXMLConfig("hausverwaltung", zltypeName + "mieter");

			try
			{
				List<String> fehlerliste = new ArrayList<>(zinslistenImport.errors);
				if(fehlerliste.size() > 0)
				{
					for(int z = 0; z < fehlerliste.size(); z++)
					{
						zlprotocol.appendHtmlErr(fehlerliste.get(z) + "<br><br>\n");
					}
				}
			}
			catch(Exception xx)
			{
				debug.error(xx);
			}
		}
		catch(Exception zir)
		{
			debug.error(zir);
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			parentObject.set("var.errorcode", zir.getMessage());
			parentObject.set("var.errorcodetxt", zir.getMessage());
			parentObject.set("dirty", "yes");
			return null;
		}

		if(null == liste || 0 == liste.size())
		{
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			parentObject.set("var.errorcode", zlprotocol.getHtmlErr());
			parentObject.set("var.errorcodetxt", Tr.t("textListUnreadable", session.getString("language")));
			parentObject.set("dirty", "yes");
			return null;
		}
		return liste;
	}

	/**
	 * Gets a specific Zinsliste from a file by index.
	 * 
	 * @param file the file identifier
	 * @param index the index of the Zinsliste
	 * @return the Zinsliste, or null on error
	 */
	public Zinsliste getZinsliste(String file, int index)
	{
		return getZinsliste(file, index, null, "");
	}

	/**
	 * Gets a specific Zinsliste with support for source system data.
	 * 
	 * @param file the file identifier
	 * @param index the index of the Zinsliste
	 * @param quellsystemResult the result from source system (optional)
	 * @param quellsystem the source system identifier (optional)
	 * @return the Zinsliste, or null on error
	 */
	public Zinsliste getZinsliste(String file, int index, List<Map<String, Object>> quellsystemResult, String quellsystem)
	{
		Date d1 = new Date();

		Zinsliste zl = null;

		try
		{
			String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
			cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
			if(cfg_zlimport == null)
			{
				parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			}

			String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
			cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
			if(cfg_currencyconfig == null)
			{
				parentObject.set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
			}

			zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
			zinslistenImport.setLanguage(session.getString("language"));
			zinslistenImport.setEvaluateFormulas(evaluateFormulas);

			if(quellsystemResult != null && quellsystemResult.size() > 0)
			{
				// SAP READ - convert to Vector for compatibility
				java.util.Vector<java.util.Hashtable<String, Object>> vecResult = new java.util.Vector<>();
				for(Map<String, Object> map : quellsystemResult)
				{
					java.util.Hashtable<String, Object> ht = new java.util.Hashtable<>();
					ht.putAll(map);
					vecResult.add(ht);
				}
				zl = zinslistenImport.read(vecResult, index, quellsystem);
			}
			else
			{
				if(fileDataAgent == null)
				{
					Connector conn = new Connector();
					fileDataAgent = conn.getFileDataAgent();
				}
				Map<String, Object> fparams = fileDataAgent.getParams(file);

				byte[] content = null;
				if(file.equals(cachedFile) && null != cachedContent)
				{
					debug.log("ZLU2: FILECONTENT CACHED!");
					content = cachedContent;
				}
				else
				{
					debug.log("ZLU2: READING FILE:" + file);
					content = fileDataAgent.getObject(file);
					cachedContent = content;
					cachedFile = file;
				}

				ByteArrayInputStream bis = new ByteArrayInputStream(content);
				try
				{
					String thefilename = "";
					String ftype = "";

					if(null != fparams)
					{
						thefilename = (String)fparams.get("name");
						ftype = (String)fparams.get("type");
					}

					zinslistenImport.extractDateFromFilename(thefilename);

					zl = zinslistenImport.read(bis, thefilename, ftype, index);
					debug.log("Zinsliste gelesen" + zl.getInfoText());
				}
				catch(Exception e)
				{
					debug.error(e);
				}
			}
			
			if(zl.getStatus() != 2)
			{
				zl.haus = TopoTool.fixeAdresse(zl.haus);
			}
			
			Date d2 = new Date();
			debug.log("Zinsliste lesen: " + (d2.getTime() - d1.getTime()) + " ms.");
			return zl;
		}
		catch(Exception e)
		{
			debug.error(e);
			debug.error(this, "NA FDAInst:" + e.getMessage());
			return null;
		}
	}

	/**
	 * Gets the cached file name.
	 * 
	 * @return the cached file name, or null if not cached
	 */
	public String getCachedFile()
	{
		return cachedFile;
	}

	/**
	 * Gets the cached content.
	 * 
	 * @return the cached content, or null if not cached
	 */
	public byte[] getCachedContent()
	{
		return cachedContent;
	}
}
