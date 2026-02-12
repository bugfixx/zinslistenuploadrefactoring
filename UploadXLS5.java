package Magic.IMS;

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
import net.metamagix.essence.tools.CoolWebTool;
import net.metamagix.essence.tools.Liquid.LiquidParserMailWrapper;
import net.metamagix.essence.tools.Translation.Tr;
import net.metamagix.essence.tools.encoding.EncodingHelper;
import net.metamagix.essence.vue.DgdJson;

import Magic.GUI.Selector;
import Magic.GUI.UstsatzSelector;
import Magic.IMS.ProcessProgress.ProcessStatus;
import Magic.IMS.Buchhaltung.LockingSingleton;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.DatabaseSource;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.DatabaseSourceConfig;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.ExcelObjectUploadXMLDefined;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.UploadListeImport;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.UploadListeTypeConfig;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.UploadListeTypeIdentifyer;
import Magic.IMS.ExcelUpload.xmlDefinedUpload.openitemUpload.ExcelOpenItemUploadXMLDefined;
import Magic.IMS.ZLImport.ZLImport;
import Magic.IMS.ZLImport.ZLTypeConfig;
import Magic.IMS.ZLImport.ZinslistenImportThread;
import Magic.IMS.ZLImport.ZinslistenValidationService;
import Magic.IMS.dwh.DWHHandler;
import Magic.IMS.dwh.DataPackage;
import Magic.IMS.icrsfred.csv.dao.FileUpload;
import Magic.IMS.icrsfred.csv.dao.FredDAO;
import Magic.IMS.icrsfred.csv.exceptions.FredProcessException;
import Magic.IMS.icrsfred.csv.exceptions.PersistenceException;
import Magic.IMS.reporting.helpers.ArgsHelper;
import Magic.Query.ValueReplacement;
import Magic.System.Login;

// TODO: Auto-generated Javadoc
/**
 * UploadXLS4 übernimmt Zinslisten und sorgt für deren Einspielen. Zinslisten koennen einzeln oder als Komplettpaket importiert werden, der Import kann auch über einen Hintergrund-Prozess mit
 * abschließender Email erfolgen
 *
 * CHANGES RK20100323 - ZLImport Protokoll Kapselung und Info Mail
 *
 * @author Peter Korbl, Randolph Kepplinger
 * @version 0.9 cleaned code
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused", "serial", "deprecation"})
public class UploadXLS5 extends DynGenDataObj implements Process
{

	/** The takes. */
	transient private Zinsliste takes;

	/** The tops cache size. */
	private static int TOPS_CACHE_SIZE = 50;

	/** The valid. */
	public boolean valid = true;

	/** The shortinfo. */
	public String shortinfo = "";

	/** The bcc_emails. */
	public String bcc_emails = "";

	/** The SEND_MAILTO_ASSETMANAGER-variable. */
	public String mailtoamcfg = "";

	/** The session. */
	public DynGenDataObj session = null;

	/** The user obj. */
	transient public DynGenDataObj userObj = null;

	/** The FDA inst. */
	transient public FileDataAgent FDAInst = null;

	/** The zinslisten import. */
	transient private ZinslistenImport zinslistenImport = null;

	/** The zlfile. */
	private String zlfile = "";

	/** The zlfile e. */
	private String zlfile_e = "";

	/** The userland. */
	private String userland = "";

	/** The cachedcontent. */
	transient byte[] cachedcontent = null;

	/** The cachedfile. */
	String cachedfile = "";

	/** The cimslog. */
	transient private net.metamagix.essence.Bugs.BugMe cimslog = null;

	/** The debug. */
	transient private net.metamagix.essence.Bugs.BugMe debug = null;

	/** The zlprotocol. */
	transient private Magic.IMS.ZLImport.ZLImportProtocol zlprotocol = null;

	/** The zins zeilen cache. */
	transient private Hashtable zinsZeilenCache = null;

	/** The validation service. */
	transient private ZinslistenValidationService validationService = null;

	/** The file service. */
	transient private Magic.IMS.ZLImport.ZinslistenFileService fileService = null;

	/** The database service. */
	transient private Magic.IMS.ZLImport.ZinslistenDatabaseService databaseService = null;

	/** The mail service. */
	transient private Magic.IMS.ZLImport.ZinslistenMailService mailService = null;

	/** The cache service. */
	transient private Magic.IMS.ZLImport.ZinslistenCacheService cacheService = null;

	/** The mapping service. */
	transient private Magic.IMS.ZLImport.ZinslistenMappingService mappingService = null;

	/** The crud service. */
	transient private Magic.IMS.ZLImport.ZinslistenDatabaseCRUDService crudService = null;

	/** The csv str. */
	private String csvStr = "";

	/** The overwritezz. */
	// transient private Magic.System.ThreadPool pool = null;
	private int overwritezz = 0;

	/** The createzz. */
	private int createzz = 0;

	/** The tops cache. */
	transient Hashtable topsCache = null;
	/** contains the date of the last CIMS.zinszeile for each top */
	transient Hashtable<String, Calendar> lastZZ4Top = null;

	/** The global. */
	public DynGenDataObj global = null;

	/** The thread agent. */
	private transient ThreadAgent threadAgent;

	/** The xc. */
	private transient XMLConfig xc;

	/** The flavour. */
	private String flavour = "";

	/** The do junk store. */
	// PKO 20110606 Because of memory issues
	private final boolean doJunkStore = true;

	/** The store junk. */
	private static int STORE_JUNK = 50;

	/** The result size of stored objects. */
	private int resultSizeOfStoredObjects = 0;

	/** The mapper. */
	transient Hashtable mapper = null;

	/** The mylang. */
	protected String mylang = "";

	/** The mapping cache. */
	transient protected Hashtable mappingCache = null;

	/** wichtig um slots korrekt aufzuloesen! <b>Only for MSSQL at the moment</b>. */
	protected transient String dbEncoding = null;

	/** The evaluate formulas. */
	protected boolean evaluateFormulas = true;

	/** The result. */
	// this contains the import result for SAP Loadsequence import
	private final Hashtable<String, String> result = new Hashtable<String, String>();

	/** The mailinglist. */
	private Hashtable<String, String> mailinglist = new Hashtable<String, String>();

	/** The leerstandmailinglist. */
	private Hashtable<String, String> leerstandmailinglist = new Hashtable<String, String>();

	/** The ablaufendevertraegemailinglist. */
	private Hashtable<String, String> ablaufendevertraegemailinglist = new Hashtable<String, String>();

	/** The mailinglist kennwerte nach nutzung. */
	private final Hashtable<String, String> mailinglistKennwerteNachNutzung = new Hashtable<String, String>();

	/** The assetmanager and I ds. */
	private Hashtable<String, String> assetmanagerAndIDs = new Hashtable<String, String>();

	/** The top status values. */
	// Values from TopStatusSelector.tpl
	Hashtable topStatusValues = new Hashtable();

	/** The statusformissingunit split. */
	String[] statusformissingunitSplit = null;

	/** The processid. */
	private long processid = -1;

	/** The pp. */
	private ProcessProgress pp = null;

	/** The actual progress. */
	private BigDecimal actualProgress = BigDecimal.ZERO;

	/** The dao. */
	private final FredDAO dao;

	/** The file name pattern. */
	private static Pattern FILE_NAME_PATTERN = Pattern.compile("(.*)FRED(\\d*).(csv|xlsx)");

	/** The zl upload object ids. */
	private Vector zlUploadObjectIds = new Vector();

	/** German Locale for Digits. */
	private DecimalFormatSymbols symbolsDE_DE = DecimalFormatSymbols.getInstance(Locale.GERMANY);

	/** The format. */
	private DateFormat format = new SimpleDateFormat("dd.MM.yyyy");

	/** Actual Haus ID. */
	String oid_haus = "";

	/** Hausverwaltung from 1 of the properties for grid. */
	String hausverwaltung = "";

	/** Lockname - don't allow duplicate imports of same file. */
	String lockname = "";

	/** The directory. */
	private static String verzeichnis = "";

	/** Fileparameter. */
	Hashtable myfparams = null;

	/** The errorsformailinglist. */
	private StringBuffer errorsformailinglist = new StringBuffer();

	/** The newimportedtops. */
	private StringBuffer newimportedtops = new StringBuffer();

	private boolean enableDetailedLogging = true;
	private Long starttime = null;
	private Long endtime = null;

	/**
	 * Instantiates a new upload XLS 4.
	 */
	public UploadXLS5()
	{
		super();
		if(null == topsCache)
		{
			topsCache = new Hashtable();
		}

		this.dao = new FredDAO();

		initMyself();
	}

	/**
	 * Instantiates a new upload XLS 4.
	 *
	 * @param c
	 *            the c
	 */
	public UploadXLS5(GenDataClass c)
	{
		super(c);
		this.dao = new FredDAO();
		setup();
	}

	/**
	 * Instantiates a new upload XLS 4.
	 *
	 * @param gdc
	 *            the gdc
	 * @param gl
	 *            the gl
	 * @param se
	 *            the se
	 */
	// needed for EventEngine !!!!
	public UploadXLS5(GenDataClass gdc, GenDataClass gl, GenDataClass se)
	{
		super(gdc);
		mapper = new Hashtable();
		global = (DynGenDataObj)gl;
		session = (DynGenDataObj)se;
		this.dao = new FredDAO();
		initMyself();

	}

	/**
	 * Inits the myself.
	 */
	private void initMyself()
	{

		cimslog = BugMe.getInstance("cimslogfile");
		Integer i = CfgSingleton.getInstance().getInteger("DEBUG_LEVEL", 1);
		debug = net.metamagix.essence.Bugs.BugMe.getInstance();

		if(session != null)
		{
			mylang = session.getString("language");
		}

		mappingCache = new Hashtable();
		setDBEncoding();
		
		// Initialize validation service
		validationService = new ZinslistenValidationService(session, debug, DAInst);
	}

	/**
	 * Method parse.
	 *
	 * @param templatecode
	 *            the templatecode
	 * @param glo
	 *            global , not used! the glo
	 * @param ses
	 *            user session the ses
	 * @return ParseResult
	 */
	@Override
	public ParseResult parse(String templatecode, DynGenDataObj glo, DynGenDataObj ses)
	{

		// debug.error("========================================================================================================================");
		// showCallStack(debug);
		// debug.error("g() was called by " + whoCalledMe());
		// debug.error("========================================================================================================================");

		String last_oid_haus = null;
		// GET THE VIEW !!!
		session = ses;
		global = glo;
		zlprotocol = new Magic.IMS.ZLImport.ZLImportProtocol("Zinslistenimport");

		String markuplanguage = session.getString("markuplanguage");
		String view = (String)session.get("CURRENT_VIEW");
		if(view.equals("")) view = (String)session.get("VIEW");

		boolean isJSON = false;
		if(markuplanguage.equalsIgnoreCase("JSON") && !view.equals("VUE"))
		{
			isJSON = true;
		}

		FileUpload fileUpload = new FileUpload();
		Long fileSequenceNumber = -1L;

		// get Values From TopStatusSelector
		getTopStatusValues();

		boolean importerror = true;

		session.set("TRANSACTIONTRIGGER", "RENTROLLIMPORT");

		// Setzt den aktuellen Status des Zinslistenimports
		setImportStatus("1");

		TopoQueries tq = new TopoQueries(session, global);

		initMyself();

		String dynurl = (String)CfgSingleton.getInstance().get("DYNAMIC_URLPATH", session, "dynamicurlpath");
		if(null == dynurl)
		{
			debug.error(this, "cannot read DYNAMIC_URLPATH!");
			dynurl = "/NA";
		}

		String flavour = (String)session.get("flavour");
		// flavour from object -> import from directory
		if(null == flavour)
		{
			flavour = (String)this.get("var.flavour");
			session.set("flavour", flavour);
		}
		if(null == flavour)
		{
			flavour = "";
		}

		// mgo 20160323 EGI RISK - wenn nicht gesetzt, Formeln auswerten
		// kann aber mit $evaluateformulas[init:0] deaktiviert werden
		evaluateFormulas = this.getBoolean("var.evaluateformulas", true);

		// hostname from object -> import from directory
		String hostname = (String)session.get("domainname");

		if(hostname == null || hostname.length() == 0)
		{
			hostname = (String)CfgSingleton.getInstance().get("domainname");
			if(hostname == null || hostname.length() == 0)
			{
				session.set("domainname", hostname);
			}
		}

		if(hostname == null || hostname.length() == 0)
		{
			hostname = (String)this.get("var.hostname");
			session.set("domainname", hostname);
		}

		String redirectobj = "";
		if(flavour.startsWith("icrs"))
		{
			redirectobj = "DIRECT_CIMS.index&FLAVOUR=" + flavour + "&contenturl=";
		}

		long start_upload = System.currentTimeMillis();

		// USER LAND
		userland = getUserValue("land");
		if(null == userland)
		{
			userland = "";
		}

		bcc_emails = (String)CfgSingleton.getInstance().get("PM_IMPORT_BCC");
		if(null != bcc_emails && bcc_emails.trim().length() > 0)
		{
			bcc_emails = "," + bcc_emails;
		}
		else
		{
			bcc_emails = "";
		}

		mailtoamcfg = (String)CfgSingleton.getInstance().get("SENDMAIL_NOT_TOASSETMANAGER");

		TopoTool topotool = new TopoTool(session, global);

		// ACHTUNG NEU RK 2007 10 24 config files geflavoured!!!
		String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");

		if(null == cfg_zlimport)
		{
			set("var.errorcode", Tr.t("textNoZLImportConfig", session.getString("language")));
			debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			session.set("TRANSACTIONTRIGGER", "");
			// Setzt den aktuellen Status des Zinslistenimports
			setImportStatus("3");
			if(isJSON)
			{
				String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
				return new ParseResult(jerr, 0, "", ses);
			}
			return super.parse(templatecode, global, session);
		}

		cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
		if(cfg_zlimport == null)
		{
			set("var.errorcode", Tr.t("textNoZLImportConfig", session.getString("language")));
			debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			session.set("TRANSACTIONTRIGGER", "");
			// Setzt den aktuellen Status des Zinslistenimports
			setImportStatus("3");
			if(isJSON)
			{
				String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
				return new ParseResult(jerr, 0, "", ses);
			}
			return super.parse(templatecode, global, session);
		}

		if(cfg_zlimport != null)
		{
			xc = new XMLConfig(cfg_zlimport, debug);
		}

		String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
		cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
		if(cfg_currencyconfig == null)
		{
			set("var.errorcode", Tr.t("textNoZLImportConfig", session.getString("language")));
			debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
			session.set("TRANSACTIONTRIGGER", "");
			// Setzt den aktuellen Status des Zinslistenimports
			setImportStatus("3");
			if(isJSON)
			{
				String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
				return new ParseResult(jerr, 0, "", ses);
			}
			return super.parse(templatecode, global, session);
		}

		// ACHTUNG NEU RK 2007 10 24 config files geflavoured!!!

		// PKO 20181114 #5512-Feature #5510: ICRS Pain in the Ass Themen
		doZapoMappingFromGui(cfg_zlimport);

		String file = (String)this.get("var.file");
		String efile = (String)this.get("var.efile");

		try
		{
			fileSequenceNumber = getSequenceNumber(file);
			if(fileSequenceNumber > -1)
			{
				fileUpload = dao.findFileUploadBySequence(fileSequenceNumber);
			}
		}
		catch(Exception e)
		{
			// Exception is common -> FileUpload object only exists on FRED Upload!
		}

		String sapconnection = (String)this.get("var.sapconnection");
		if(sapconnection == null)
		{
			sapconnection = "";
		}

		// default==Zinsliste; fioaxera==FIO Axera; kein einfach auf andere erweitert werden ...
		String quellsystem = this.getString("var.quellsystem");
		if(quellsystem == null || quellsystem.equals(""))
		{
			quellsystem = "default";
		}
		if(sapconnection.equals("1"))
		{
			quellsystem = "sapare";
		}
		if(quellsystem.equals("fioaxera") || quellsystem.equals("sapcsv") || quellsystem.startsWith("databasesource"))
		{
			sapconnection = "1";
		}

		if(file.length() == 0 && sapconnection != null && sapconnection.equals("1") && (view.equals("NIGHTMAILTHREAD") || view.equals("MAILTHREAD") || view.equals("AUTOMATICIMPORT") || view.equals("THREADRESULT")))
		{
			view = "AUTOMATICIMPORT";
		}

		boolean ignoresimpleerrors = this.getBoolean("var.ignoresimpleerrors");

		String store = (String)this.get("var.store");
		if(null == store)
		{
			store = "0";
		}

		if(file.startsWith("FILE_"))
		{
			zlfile = new String(file);
			file = file.substring(5);
		}
		if(efile.startsWith("FILE_"))
		{
			zlfile_e = new String(efile);
			efile = efile.substring(5);
		}
		// System.err.println("ZLU2: FILE:"+file);

		try
		{
			if(file != null && file.length() > 0)
			{
				if(FDAInst == null)
				{
					Connector conn = null;
					conn = new Connector();
					FDAInst = conn.getFileDataAgent();
				}

				myfparams = FDAInst.getParams(file);
				if(null != myfparams)
				{
					lockname = (String)myfparams.get("name");

					if(lockname != null && !lockname.equals(""))
					{
						if(LockingSingleton.getInstance().isrunningWithTimeout(lockname, 120))
						{
							String language = session.getString("language");
							String err = Tr.t("No duplicate imports with the same file allowed!", mylang);
							if(isJSON)
							{
								String jerr = CoolJSONTool.createJsonErrorString(err);
								return new ParseResult(jerr, 0, "", ses);
							}
							return new ParseResult(err, 0, "", ses);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}

		String wertaenderung = (String)this.get("var.wertaenderung");
		if(null == wertaenderung || wertaenderung.length() == 0)
		{
			wertaenderung = "2";
		}

		String assetmanagerinfo = (String)this.get("var.assetmanagerinfo");
		if(null == assetmanagerinfo)
		{
			assetmanagerinfo = "0";
		}

		String sid = TopoTool.getStatusOID((String)session.get("domainid"));
		int frozenyear = 0;
		int frozenmonth = 0;
		if(null == sid)
		{
			cimslog.log("Kein gesperrtes Datum gesetzt, alle Zinslisten koennen importiert werden.");
		}
		else
		{
			// stand holen
			try
			{
				if(null == DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					DAInst = conn.getDataAgent();
				}
				DynGenDataObj sdgd = (DynGenDataObj)DAInst.getObject(sid, "");
				String fj = (String)sdgd.get("var.jahr");
				String fm = (String)sdgd.get("var.monat");
				Integer year = Integer.valueOf(fj);
				Integer month = Integer.valueOf(fm);
				frozenyear = year.intValue();
				frozenmonth = month.intValue();
			}
			catch(Exception x)
			{
				debug.log(x);
			}
		}

		// System.err.println("ZLU2: FROZEN STATUS "+frozenyear+"/"+frozenmonth);
		if(view.endsWith("INFO"))
		{
			String result = this.getString("var.resultcode");

			debug.log("Importresult before ESSENCEID replacement" + result);

			try
			{
				String essenceId = session.getString("SESSIONID");
				String request = session.getString("request");
				String path = (String)CfgSingleton.getInstance().get("DYNAMIC_URLPATH", session, "dynamicurlpath");

				result = CoolTemplateTool.fixUrlsInHTML(result, request, path, essenceId);
			}
			catch(URISyntaxException e)
			{
				debug.error("cannot fix URLS in HTML", e);
			}

			debug.log("Importresult after ESSENCEID replacement" + result);

			this.set("var.resultcode", result);
		}
		else if(view.endsWith("SEND") || view.indexOf("INFOMAILFEEDBACK") >= 0)
		{
			// SEND INFOMAIL
			String mres = this.sendMailWithErrors();
			set("var.resultcode", mres);
		}
		else if(view.endsWith("MAILTHREAD"))
		{
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}
			String myid = (String)get("id");
			String userid = (String)session.get("userid");
			if(threadAgent == null)
			{
				Connector connector = new Connector();
				threadAgent = connector.getThreadAgent();
				if(null == threadAgent)
				{
					// Problem - background import not possible
					debug.error(this, Tr.t("NOTHREADAGENT", mylang));
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");
					String err = Tr.t("NOTHREADAGENT", mylang);
					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(err);
						return new ParseResult(jerr, 0, "", ses);
					}
					return new ParseResult(err, 0, "", ses);
				}
			}

			try
			{
				EThreadParams params = new EThreadParams();

				Date now = new Date();
				GregorianCalendar crecalendar = new GregorianCalendar();
				crecalendar.setTime(now);

				int hrs = 24;
				int mins = 24;

				if(view.endsWith("NIGHTMAILTHREAD"))
				{
					String hrs_str = (String)get("var.nighthour");
					if(null != hrs_str)
					{
						try
						{
							hrs = Integer.parseInt(hrs_str);
							int hrs_now = crecalendar.get(Calendar.HOUR_OF_DAY);
							if(hrs_now < hrs)
							{
								crecalendar.add(Calendar.HOUR, 24);
							}
							crecalendar.set(Calendar.HOUR_OF_DAY, hrs);
						}
						catch(Exception xx)
						{}
					}
					String mins_str = (String)get("var.nightminute");
					if(null != mins_str)
					{
						try
						{
							mins = Integer.parseInt(mins_str);
							// int mins_now = crecalendar.get(Calendar.MINUTE);
							crecalendar.set(Calendar.MINUTE, mins);
						}
						catch(Exception xx)
						{}
					}
				}
				params.put(ZinslistenImportThread.SCHEDULE_TIME, net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()));
				params.put(ZinslistenImportThread.ZINSLISTE, createLink(zlfile, "Datei", session));

				String user = "";
				try
				{
					DynGenDataObj userDgd = (DynGenDataObj)DAInst.getObject((String)this.get("properties.creator"), "System.User");
					user = (String)userDgd.get("var.name");
				}
				catch(Exception e)
				{
					debug.error(e);
				}
				params.put(ZinslistenImportThread.USER, user);
				String nowS = net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime());
				cimslog.log("\nZinslistenimport Thread gesetzt auf " + nowS + "\n");
				threadAgent.createThread("Magic.IMS.ZLImport.ZinslistenImportThread", myid, "Zinslistenimport (" + nowS + ")", userid, params, crecalendar.getTime(), "ZLImportThreads", EThreadGroup.MODE_PARALLEL, EThreadGroup.INFINITE, 1, session, 4);
			}
			catch(Exception e)
			{
				debug.log(e);
			}
		}
		else if(view.endsWith("AUTOMATICIMPORT"))
		{
			pp = this.registerProcess();
			updateProgess(BigDecimal.ZERO, "Import gestartet", ProcessStatus.RUNNING);

			LockingSingleton.getInstance().enter(lockname);

			Date starttime = new Date();

			StringBuffer good = new StringBuffer();
			Vector tmpliste = null;
			Vector quellsystemResult = null;

			// Hier den SAP import einklinken!!!!
			if(sapconnection.equals("1") && quellsystem.equals("sapare") && file.length() == 0)
			{
				String sapimportname = this.getString("var.sapimportname");

				if(sapimportname.length() > 0)
				{
					SAPQuery query = new SAPQuery();
					quellsystemResult = query.getMergedXMLAContentAsVector(sapimportname);
					if(quellsystemResult == null)
					{
						result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
						zlprotocol.appendMailMsg("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
						set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");
						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							return new ParseResult(jerr, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}

					// Delete rows that are marked to delete -> column 'RejectLine' == 1
					QueryHelper.writeQueryResultToFilesystem(quellsystemResult, sapimportname + "_before_rejecting_lines_", quellsystem);

					quellsystemResult = SAPQuery.deleteMarkedRows(quellsystemResult);

					tmpliste = readQuellsystemListe(quellsystemResult, quellsystem);

				}
			}
			// 1==FIO Axera
			else if(sapconnection.equals("1") && quellsystem.equals("fioaxera") && file.length() == 0)
			{
				// Datum noch aus GUI holen -> Default ist aktuelles Datum
				// zinslistendatum

				SwaggerQuery scc = new SwaggerQuery();

				Hashtable<String, String> parameters = new Hashtable<>();
				String year = "";
				String month = "";
				String day = "";

				month = getString("var.monatvon");
				year = getString("var.jahrvon");

				Calendar cal = GregorianCalendar.getInstance();
				cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
				cal.set(Calendar.YEAR, Integer.parseInt(year));
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

				day = String.valueOf(cal.getActualMaximum(Calendar.DAY_OF_MONTH));

				Calendar actualcal = GregorianCalendar.getInstance();

				if(!cal.before(actualcal))
				{
					day = String.valueOf(actualcal.get(Calendar.DAY_OF_MONTH));
				}

				if(day.length() == 1)
				{
					day = "0" + day;
				}

				if(month.length() == 1)
				{
					month = "0" + month;
				}

				// 2021-10-14
				String dueDate = year + "-" + month + "-" + day;

				parameters.put("dueDate", dueDate);

				quellsystemResult = scc.getZinslistenQueryResult(parameters);
				if(quellsystemResult == null)
				{
					result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					zlprotocol.appendMailMsg("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
					set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");
					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						return new ParseResult(jerr, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_", quellsystem);

				tmpliste = readQuellsystemListe(quellsystemResult, quellsystem);
			}
			else if(sapconnection.equals("1") && quellsystem.equals("sapcsv") && file.length() == 0)
			{
				SAPCSVQuery sapcsvquery = new SAPCSVQuery();

				Hashtable<String, String> parameters = new Hashtable<>();

				String fileWithPath = this.getString("var.filepath");
				String fileWithPathBackup = this.getString("var.filepathbackup");

				parameters.put("filewithpath", fileWithPath);
				parameters.put("filepathbackup", fileWithPathBackup);

				quellsystemResult = sapcsvquery.getZinslistenQueryResult(parameters);

				if(quellsystemResult == null)
				{
					result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					zlprotocol.appendMailMsg("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
					set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();
					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						return new ParseResult(jerr, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_rentroll_", quellsystem);

				tmpliste = readQuellsystemListe(quellsystemResult, quellsystem);
			}
			else if(sapconnection.equals("1") && quellsystem.startsWith("databasesource") && file.length() == 0)
			{
				String connectionname = quellsystem.replaceAll("databasesource-", "");
				String databasesource = "RentRoll" + StringUtils.capitalize(connectionname) + "Query";
				String uploadlistetypeconfig = connectionname + "listetypeconfig";

				if(databasesource.length() > 0)
				{
					try
					{
						quellsystemResult = getDatabaseContent(databasesource, uploadlistetypeconfig);

						if(quellsystemResult == null)
						{
							result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
							zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
							zlprotocol.appendMailMsg("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
							session.set("CURRENT_VIEW", "ERRORQUEST");
							set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
							set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
							this.set("dirty", "yes");
							session.set("TRANSACTIONTRIGGER", "");
							// Setzt den aktuellen Status des Zinslistenimports
							setImportStatus("3");

							LockingSingleton.getInstance().leave(lockname);

							updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
							pp.deregisterProcess();
							if(isJSON)
							{
								String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
								return new ParseResult(jerr, 0, "", ses);
							}
							return super.parse(templatecode, glo, ses);
						}

						QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_rentroll_", quellsystem);

						tmpliste = readQuellsystemListe(quellsystemResult, quellsystem);
					}
					catch(Exception e)
					{
						debug.error(e);
					}
				}
			}
			else
			{

				tmpliste = readListe(file);

				if(quellsystem.equals("sapare") && file.length() > 0)
				{
					tmpliste = SAPQuery.deleteMarkedRows(tmpliste);
				}

			}

			String myoid = (String)ses.get("CURRENT_OID");
			if(myoid == null || myoid.length() == 0)
			{
				myoid = (String)this.get("id");
			}

			try
			{
				if(null == DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					DAInst = conn.getDataAgent();
				}
				if(myoid == null)
				{
					myoid = "";
				}
				else
				{
					DynGenDataObj testdgd = (DynGenDataObj)DAInst.getObject(myoid, null);
					String templateType = (String)testdgd.get("TEMPLATETYPE");
					if(!templateType.contains("zinslistenupload"))
					{
						myoid = "";
					}
				}
			}
			catch(RemoteException e1)
			{
				debug.error(e1);
			}

			// Create new Object for every Object (Haus)
			String templateType = (String)this.get("TEMPLATETYPE");

			String oidnew = "";
			if(myoid.length() > 0)
			{
				oidnew = myoid;
			}
			else
			{
				try
				{
					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}
					this.fixFileLink();
					oidnew = DAInst.storeObject(this, templateType, null, session);
				}
				catch(Exception e)
				{
					debug.error(e);
				}
			}

			if(tmpliste == null)
			{
				set("var.errorcode", Tr.t("textFile", session.getString("language")) + " (" + file + ") " + Tr.t("textNoListInFile2", session.getString("language")) + ". Import" + Tr.t("textNotFinished", session.getString("language")));
				debug.error(this, "Import not possible (" + file + "). Incorrect rent roll file.");
				session.set("TRANSACTIONTRIGGER", "");

				// PKO - 20190607 Benachrichtigung an User senden wenn die Datei nicht eingelesen werden kann
				// RK more logging
				String filename = this.getString("var.name");
				String mynewfilelink = createLink(zlfile, filename, session);
				// String directory = (String)CfgSingleton.getInstance().get("UPLOAD_FILE_DIR");

				// directory = "" + this.getString("var.filepath");
				String filenameforemail = this.getString("var.filename");

				if((filenameforemail == null || filenameforemail.length() == 0) && myfparams != null)
				{
					filenameforemail = String.valueOf(myfparams.get("name"));
				}

				String subject = Tr.t("textChangeMailSubject", session.getString("language")) + ": " + Tr.t("textNoImport", session.getString("language")) + " (" + filenameforemail + ")";

				String message = "<br><br>" + Tr.t("textFile", session.getString("language")) + " (" + file + ") ";
				if(StringUtils.isNotBlank(verzeichnis))
				{
					message = message + Tr.t("textDirectory", session.getString("language")) + " (" + verzeichnis + ") ";
				}
				message = message + Tr.t("textNotFinished", session.getString("language"));
				message = message + "<br/><br/>" + mynewfilelink;

				zlprotocol.appendHtmlRes(message);

				sendMailWithErrorsToExcecutor(subject, message);

				String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
				String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);

				updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + "." + message, link, rrplainlink, last_oid_haus);

				// Setzt den aktuellen Status des Zinslistenimports
				setImportStatus("3");

				LockingSingleton.getInstance().leave(lockname);

				updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
				pp.deregisterProcess();

				if(isJSON)
				{
					String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
					return new ParseResult(jerr, 0, "", ses);
				}
				return super.parse(templatecode, global, session);
			}
			Vector liste = new Vector();
			Hashtable eigentuemerhash = new Hashtable();
			for(int p = 0; p < tmpliste.size(); p++)
			{
				Hashtable e = (Hashtable)tmpliste.elementAt(p);
				String text = (String)e.get("text");
				Integer index = (Integer)e.get("index");
				if(text.indexOf("Mieterliste") >= 0)
				{
					liste.addElement(e);
				}
				else if(text.indexOf("Eigentümerliste") >= 0)
				{
					eigentuemerhash.put(text.toLowerCase(), index);
				}
			}

			liste = VectorOfHashesSorter.sort(liste, "text");

			zlprotocol.appendHtmlRes("<div id=\"datatablesmmx\" class='datatablesmmx'><div class='dataTablesDownloadButton' onclick=\"downloadCSVfile('datatableicrs','table_results')\"> </div><table class='display' id='datatableicrs' width='98%'>");
			zlprotocol.appendHtmlRes("<thead><tr><th>" + Tr.t("textObject", session.getString("language")) + "</th><th>" + Tr.t("textResult", session.getString("language")) + "</th></tr></thead><tbody>");

			Hashtable<String, Hashtable<String, Hashtable<String, String>>> missingWEs = new Hashtable<String, Hashtable<String, Hashtable<String, String>>>();
			if(quellsystem.equals("sapare") && file.length() == 0)
			{
				Hashtable<String, Hashtable<String, Hashtable<String, String>>> alleWEsInBestand = getAlleWEsInBestand();

				Enumeration ek = alleWEsInBestand.keys();
				while(ek.hasMoreElements())
				{
					String mailAndName = (String)ek.nextElement();
					Hashtable row = alleWEsInBestand.get(mailAndName);

					boolean foundHaus = false;
					for(int i = 0; i < liste.size(); i++)
					{

						Hashtable e = (Hashtable)liste.elementAt(i);
						String identadresse1Zinsliste = String.valueOf(e.get("specialIdentification"));

						if(row.containsKey(identadresse1Zinsliste) || row.containsKey(identadresse1Zinsliste.replace("  ", " ")) || row.containsKey(identadresse1Zinsliste.replace(" ", "  ")))
						{
							foundHaus = true;
						}
					}
					if(!foundHaus)
					{
						missingWEs.put(mailAndName, row);
					}
				}
			}

			updateProgess(new BigDecimal("0.05"), "File eingelesen", null);

			// Precheck
			Zinsliste azl = null;

			int errorcounter = 0;
			Set<String> hausOIDs = new HashSet<String>();
			for(int p = 0; p < liste.size(); p++)
			{
				resultSizeOfStoredObjects = 0;
				String error2append = "";
				java.util.Date start_time = new java.util.Date();
				Hashtable e = (Hashtable)liste.elementAt(p);
				String text = (String)e.get("text");
				Integer index = (Integer)e.get("index");
				String eigentuemertext = CoolStringTool.replaceStr(text, "Mieterliste", "Eigentümerliste");
				Integer pose = (Integer)eigentuemerhash.get(eigentuemertext.toLowerCase());
				Hashtable<String, String> haeuserMitFehlern = new Hashtable<String, String>();

				// mieterliste?
				azl = null;
				if(!quellsystem.equals("default"))
				{
					azl = getZinsliste(file, index.intValue(), quellsystemResult, quellsystem);
				}
				else
				{
					azl = getZinsliste(file, index.intValue());
				}

				String neuePosten = azl.getNeuePosten();
				if(neuePosten.length() > 0)
				{

					if(neuePosten.startsWith("<h"))
					{
						neuePosten = neuePosten.substring(neuePosten.lastIndexOf("\">") + 2);
						neuePosten = neuePosten.replace("</td>", "");
						neuePosten = neuePosten.replace("</table>", "");
					}
					zlprotocol.appendHtmlRes("<br><tr><td>" + Tr.t("textNewZapos", session.getString("language")) + " " + azl.getAdresse() + " " + azl.getPlz() + " " + azl.getOrt() + " " + azl.getEdvNr() + "</td><td>" + neuePosten + "</td></tr>");
					zlprotocol.appendMailMsg("<br><tr><td>" + Tr.t("textNewZapos", session.getString("language")) + " " + azl.getAdresse() + " " + azl.getPlz() + " " + azl.getOrt() + " " + azl.getEdvNr() + "</td><td>" + neuePosten + "</td></tr>");
				}

				shortinfo = azl.getShortInfos();

				// Für Liste der durchgeführten Imports
				String jahr = azl.getJahr();
				String monat = azl.getMonat();
				String land = azl.getLand();
				if(land == null || land.equals(""))
				{
					land = "AT";
				}
				String ort = azl.getOrt();
				String adresse = azl.getAdresse();

				this.set("var.jahr", jahr);
				this.set("var.monat", monat);
				this.set("var.land", "");
				this.set("var.ort", "");
				this.set("var.adresse", "Verarbeitung Objekte " + p + "/" + liste.size());
				this.set("var.sapconnection", "");

				double calculation = (Double.parseDouble(String.valueOf(p)) / Double.parseDouble(String.valueOf(liste.size())));
				if(calculation > 0.1)
				{
					calculation = calculation - 0.1;
				}
				else
				{
					calculation = 0.05;
				}

				updateProgess(new BigDecimal(calculation), "Verarbeitung Objekte " + p + "/" + liste.size(), null);

				updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_PENDING, "RRImport file " + file + "import Objects " + p + "/" + liste.size(), "", "", last_oid_haus);

				setImportStatus("1");

				try
				{

					if(!quellsystem.equals("default"))
					{
						if(zlfile == null || zlfile.length() == 0)
						{
							String tmpDirectory = (String)CfgSingleton.getInstance().get("UPLOAD_FILE_DIR");
							String filetype = "csv";

							if(file.length() == 0)
							{

								String content = "";

								content = QueryHelper.getCSVFromVector(quellsystemResult, quellsystem);

								String actualTime = new SimpleDateFormat("_yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
								String outfilename = quellsystem + "_file_zinslistenimport" + actualTime + ".csv";

								FileWriter writer = new FileWriter(tmpDirectory + System.getProperty("file.separator") + outfilename);
								writer.append(content);
								writer.flush();
								writer.close();

								file = outfilename;
							}

							Path path = Path.of(tmpDirectory + System.getProperty("file.separator") + file);
							byte[] data = Files.readAllBytes(path);

							Hashtable fparams = new Hashtable();
							fparams.put("size", "" + data.length);
							fparams.put("paramname", "zinslistenfile");
							fparams.put("name", file);
							fparams.put("type", filetype);
							fparams.put("Content-Type", "application/txt");
							fparams.put("OID", oidnew);

							if(FDAInst == null)
							{
								Connector conn = null;
								conn = new Connector();
								FDAInst = conn.getFileDataAgent();
							}

							// Create a unique file reference
							Long ctr = CoolDataTool.generateUniqueSequence(file);
							String filereferencename = file.substring(0, file.indexOf(".csv")) + ctr + "." + filetype;
							zlfile = FDAInst.storeObject(filereferencename, data, fparams);
						}

						if(!zlfile.startsWith("FILE_"))
						{
							zlfile = "FILE_" + zlfile;
						}

						this.set("var.file", zlfile);
						this.set("dirty", "yes");

						this.set("var.jahr", jahr);
						this.set("var.monat", monat);
						this.set("var.land", "");
						this.set("var.ort", "");
						this.set("var.adresse", quellsystem + " Import");
						this.set("var.hausverwaltung", hausverwaltung);
						this.set("var.filename", lockname);
						this.set("var.sapconnection", "2");

						if(null != mailtoamcfg && mailtoamcfg.trim().length() > 0)
						{
							if(mailtoamcfg.equals("1") || mailtoamcfg.equalsIgnoreCase("yes"))
							{
								this.set("var.assetmanagerinfo", "0");
							}
						}
						else
						{
							this.set("var.assetmanagerinfo", "1");
						}

					}

				}
				catch(Exception ex)
				{
					debug.error(ex);
				}

				oid_haus = null;

				// check ob man darf
				if(azl == null || azl.jahr == null || azl.monat == null)
				{

					String url = dynurl + "?VIEW=READ&FLAVOUR=" + flavour + "&fehlerabfrage=0&createhaus=&createnewtops=" + "&ignoreerrors=&OID=" + oidnew + "&" + "zinslistenindex=" + index.intValue();
					if(redirectobj.length() > 0)
					{
						url = dynurl + "?OID=" + redirectobj + URLEncoder.encode(url, StandardCharsets.UTF_8);
					}
					zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class='ajaxLink redlink' href=\"" + url + "\" target=\"_new\">" + Tr.t("textCouldNotCreateRentRoll", session.getString("language")) + "</a></font></td></tr>");// nixgut
					zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class='ajaxLink redlink' href=\"" + url + "\" target=\"_new\">" + Tr.t("textCouldNotCreateRentRoll", session.getString("language")) + "</a></font></td></tr>");// nixgut

				}
				else if(Integer.parseInt(azl.monat) > 12 || Integer.parseInt(azl.monat) < 1 || Integer.parseInt(azl.jahr) > 2100 || Integer.parseInt(azl.jahr) < 1970)
				{
					zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\">" + Tr.t("textCouldNotCreateRentRollBadMonth", session.getString("language"), azl.monat, azl.jahr) + "</font></td></tr>");// nixgut
					zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\">" + Tr.t("textCouldNotCreateRentRollBadMonth", session.getString("language"), azl.monat, azl.jahr) + "</font></td></tr>");// nixgut

				}
				else
				{ // azl ok.
					if(frozenyear < Integer.parseInt(azl.jahr) || frozenyear == Integer.parseInt(azl.jahr) && frozenmonth < Integer.parseInt(azl.monat))
					{
						String sessid = session.getString("SESSIONID");

						String url = dynurl + "?VIEW=READ&FLAVOUR=" + flavour + "&fehlerabfrage=0&createhaus=&createnewtops=" + "&ignoreerrors=&OID=" + oidnew + "&zinslistenindex=" + index.intValue() + "&ESSENCEID=" + sessid;
						String urlError = dynurl + "?COPY=" + oidnew + "&OID=NEW0&VIEW=READ&FLAVOUR=" + flavour + "&fehlerabfrage=0&createhaus=&createnewtops=" + "&ignoreerrors=&zinslistenindex=" + index.intValue() + "&ESSENCEID=" + sessid;

						String topoanpassung = getString("var.topoanpassung");
						if(!topoanpassung.equals("1"))
						{
							topoanpassung = "0";
						}
						url += "&topoanpassung=" + topoanpassung;

						String altezinszeilenloeschen = getString("var.altezinszeilenloeschen");
						if(!altezinszeilenloeschen.equals("1"))
						{
							altezinszeilenloeschen = "0";
						}
						url += "&altezinszeilenloeschen=" + altezinszeilenloeschen;

						url += "&wertaenderung=" + wertaenderung;

						if(null != pose)
						{
							// listen holen
							Zinsliste azle = getZinsliste(file, pose.intValue());
							// System.err.println("ZLU2: " + pose.toString() + ":" + eigentuemertext);
							ZinslistenImport zli = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
							zli.setLanguage(session.getString("language"));
							zli.setEvaluateFormulas(evaluateFormulas);
							azl = zli.mergeZinslisten(azl, azle);
							url += "&" + "eigentuemerlistenindex=" + pose.intValue();
						}
						else
						{
							url += "&" + "eigentuemerlistenindex=";
						}

						if(redirectobj.length() > 0)
						{
							url = dynurl + "?OID=" + redirectobj + URLEncoder.encode(url, StandardCharsets.UTF_8);
							urlError = dynurl + "?OID=" + redirectobj + URLEncoder.encode(urlError, StandardCharsets.UTF_8);
						}

						String domain = CoolWebTool.getUsedDomain(session);
						if(!urlError.contains(domain))
						{
							urlError = domain + urlError;
						}

						oid_haus = topotool.getHausOID(azl);

						Boolean importsperrebeidatenfreigabe = this.getBoolean("var.importsperrebeidatenfreigabe", Boolean.FALSE);
						if(importsperrebeidatenfreigabe)
						{
							Boolean importAllowed = getStatusOfFreigabe(oid_haus, azl);

							if(!importAllowed)
							{
								// set error and return parseresult
								zlprotocol.appendHtmlErr("<h2>" + Tr.t("textRentRollApproved", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + azl.getBaseInfosInHTML(session.getString("language")));
								shortinfo = azl.getShortInfos();

								set("var.resultcode", zlprotocol.getHtmlRes());
								set("var.errorcode", zlprotocol.getHtmlErr());
								set("var.errorcodetxt", zlprotocol.getTxtErr());
								set("var.errorcodecsv", azl.getErrorsInCSV("", session));
								this.set("dirty", "yes");
								session.set("TRANSACTIONTRIGGER", "");
								// Setzt den aktuellen Status des Zinslistenimports
								setImportStatus("3");

								LockingSingleton.getInstance().leave(lockname);

								updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
								pp.deregisterProcess();
								if(isJSON)
								{
									String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
									return new ParseResult(jerr, 0, "", ses);
								}
								return super.parse(templatecode, glo, ses);
							}
						}

						if(oid_haus != null && oid_haus.length() > 0)
						{
							getHausverwaltungFromHausOid(oid_haus);
						}

						if(oid_haus == null && zinslistenImport.getZlTypeConfig().isCreatenewpropertiesautomatically())
						{
							try
							{
								TemplateReader tr = TemplateReader.getInstance();
								DynGenDataObj dgdHaus = tr.getDGDForTemplate("CIMS.haus", global, session);

								dgdHaus.set("var.name", azl.getAdresse());
								dgdHaus.set("var.plz", azl.getPlz());
								dgdHaus.set("var.ort", azl.getOrt());
								dgdHaus.set("var.identadresse5", azl.getEdvNr());
								dgdHaus.set("var.land", azl.getLand());

								// Values from ICRSConfig
								dgdHaus.set("slot.gschaft", ICRSConfig.getInstance(session).get("slot.gschaft"));

								dgdHaus.set("slot.gfeld", ICRSConfig.getInstance(session).get("slot.gfeld"));
								dgdHaus.set("slot.assetmanager", ICRSConfig.getInstance(session).get("slot.assetmanager"));
								dgdHaus.set("slot.hausverwaltungneu", ICRSConfig.getInstance(session).get("slot.hausverwaltungneu"));

								dgdHaus.set("var.status", ICRSConfig.getInstance(session).get("var.status"));
								dgdHaus.set("var.hausnutzung", ICRSConfig.getInstance(session).get("var.hausnutzung"));

								oid_haus = DAInst.storeObject(dgdHaus, dgdHaus.getTemplateType(), null, session);

								String edvnummer = "";
								if(azl.getEdvNr().length() > 0 && azl.getHausverwaltung().length() > 0)
								{
									edvnummer = "|" + azl.getHausverwaltung() + "" + azl.getEdvNr() + "|";
								}

								String adressemitedv = (azl.getAdresse() + " " + edvnummer).trim();

								String hauslink = DynGenDataObj.createLink(oid_haus, "Link", session);
								zlprotocol.appendMailMsg("<br><tr><td>" + (azl.getEdvNr() + " " + adressemitedv + " " + azl.getPlz() + " " + hauslink + " " + azl.getOrt() + " " + azl.getLand()).trim() + " " + Tr.t("textObjectCreated", session.getString("language")) + "</td></tr>");
								zlprotocol.appendHtmlRes("<br><tr><td>" + (azl.getEdvNr() + " " + adressemitedv + " " + azl.getPlz() + " " + hauslink + " " + azl.getOrt() + " " + azl.getLand()).trim() + " " + Tr.t("textObjectCreated", session.getString("language")) + "</td></tr>");

								zlprotocol.addCsvLine(new String[]{
									azl.getEdvNr() + " " + adressemitedv,
									azl.getPlz(),
									"",
									Tr.t("textObjectCreated", session.getString("language")),
									"",
									""});

								// Also do Googlemaps Query
								GoogleMapsQuery gmq = new GoogleMapsQuery(session);
								gmq.updateLageplaeneAndGeoData(null, oid_haus);
							}
							catch(Exception ex)
							{
								debug.error(ex);
							}
						}

						last_oid_haus = oid_haus;

						if(StringUtils.isNotBlank(oid_haus))
						{
							hausOIDs.add(oid_haus);
						}

						boolean createplaintops = false;
						boolean createplainstellplaetze = false;
						boolean isoldlist = false;
						if(null != oid_haus)
						{

							// check auf berechtigung !!!
							String origuserid = getString("var.origuserid").trim();
							if(origuserid.matches("\\d+") && CfgSingleton.getInstance().hasIcrsAccessCreateSpvGroups())
							{
								// change session
								Login login = new Login();
								session = login.getUserSession(origuserid, global, null);
							}
							if(topotool.mayWriteHaus(oid_haus, session))
							{
								if(getBoolean("var.checkexistingrentroll", Boolean.FALSE))
								{
									int zzanz = tq.countZinszeilen(oid_haus, jahr, monat);
									if(zzanz > 0)
									{
										azl.error("", Tr.t("textExistingRentroll", session.getString("language")), Tr.t("textExistingRentrollInfo", session.getString("language"), monat, jahr), ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER);
									}
								}
								// Falls additionalFields in den Hausinfos befüllt ist, Haus updaten
								String hausOID = updateHaus(azl, oid_haus);
								if(hausOID == null || hausOID.equals(""))
								{
									debug.error("Error in parse of UploadXLS4 - could not update Haus with oid=" + oid_haus + "!");
								}
								TopList top_list = new TopList(session, global, DAInst, oid_haus, false);
								set("var.toplistjson", top_list.toJSON(ses));
								if(getBoolean("var.topmatcherselector", Boolean.TRUE))
								{
									zlprotocol.appendHtmlRes(getJavascriptTopmatcherString(top_list));
								}

								// HAUS EXITIERT
								String ignoreerrors = getIgnoreErrorsForHaus(oid_haus);
								azl = checkHausStatus(oid_haus, azl, zinslistenImport.getZlTypeConfig());
								azl.ignoreErrors(ignoreerrors);
								isoldlist = azl.isOldList();

								debug.error("AUTOMATIC IMPORT Nr: " + p + " Status: " + azl.status);

								String ignorealleasyerros = this.getString("var.ignorealleasyerros");
								if(0 == azl.status || (azl.status == 1 && (zinslistenImport.getZlTypeConfig().isIgnorealleasyerros() && ignorealleasyerros.equals("0") || ignorealleasyerros.equals("1")))) // oder
								{

									try
									{
										if(null == DAInst)
										{
											net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
											DAInst = conn.getDataAgent();
										}
										DynGenDataObj hausDGD = (DynGenDataObj)DAInst.getObject(oid_haus, null);
										Date lastimportHaus = hausDGD.getDate("var.lastimport");

										Calendar cal = GregorianCalendar.getInstance();
										cal.set(Calendar.MONTH, Integer.parseInt(azl.monat) - 1);
										cal.set(Calendar.YEAR, Integer.parseInt(azl.jahr));
										cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
										Date actualZiliimport = cal.getTime();

										if(null == lastimportHaus || actualZiliimport.after(lastimportHaus))
										{
											String actualZiliimportDateStr = net.metamagix.essence.eSSENCETypes.eDate.stringFromDate(actualZiliimport);

											hausDGD.set("var.runningimport", actualZiliimportDateStr);
											hausDGD.set("dirty", "yes");
											String id = DAInst.storeObject(hausDGD, hausDGD.getTemplateType(), oid_haus, session);
										}
									}
									catch(Exception ex)
									{
										debug.error("Error in setting Importdate on CIMS.haus in UploadXLS4:");
										debug.log(ex);
									}

									// KEINE FEHLER

									// GIBT'S DIE TOPS
									boolean createtops = false;
									Hashtable allmytops = tq.getTopsForOID(oid_haus, null, session, DAInst);

									// mit bereinigten namen !!!
									Hashtable allmyinternaltops = TopoTool.getInternalTopsForTops(allmytops);

									Hashtable alltopsmerged = new Hashtable();
									alltopsmerged.putAll(allmytops);
									alltopsmerged.putAll(allmyinternaltops);

									fillTopCache(top_list);

									fillLastZZ4Top(oid_haus);

									if(wertaenderung.equals("1"))
									{
										boolean vergleich_ok = azl.vergleicheMitTops(topsCache, alltopsmerged, lastZZ4Top, null, ses, oid_haus, zinslistenImport);
										if(!vergleich_ok)
										{
											log("Fehlerhafter Zinslistenvergleich [2] bei Haus " + azl.edvNr + " " + azl.haus + " beim automatischen Upload.");
										}
									}
									else if(wertaenderung.equals("2"))
									{
										boolean vergleich_ok = azl.vergleicheMitTops(topsCache, alltopsmerged, lastZZ4Top, zlprotocol, ses, oid_haus, zinslistenImport);
										if(!vergleich_ok)
										{
											log("Fehlerhafter Zinslistenvergleich [3] bei Haus " + azl.edvNr + " " + azl.haus + " beim automatischen Upload.");
										}

									}

									Hashtable newtops = new Hashtable();

									int ntcount = 0;

									Hashtable tops_in_zl = new Hashtable();
									String sapnummer = "";

									// TOPS DURCHGEHEN
									for(int j = 0; j < azl.zinszeilen.size(); j++)
									{
										Hashtable ht = (Hashtable)azl.zinszeilen.get(j);
										String top = (String)ht.get("top");

										TopElement te = null;
										// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
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

										// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so
										if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
										{
											String tmpTop = top.substring(0, top.indexOf(" |"));
											te = top_list.getTop(tmpTop);
										}

										String oid_top = null;
										if(te != null)
										{
											oid_top = te.getId();
										}

										if(oid_top == null && zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())// 27392 legt neue top ohne zu erlauben
										{
											DynGenDataObj dgd = createStellplatz(ht, oid_haus, azl);
											if(null != dgd)
											{
												newtops.put("NEW" + ntcount, dgd);

												zlprotocol.appendMailMsg("<br> " + Tr.t("textParkingSpaceExists1", session.getString("language")) + " " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " " + Tr.t("textRentalUnitExists3", session.getString("language")));

												log("stellplatz " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " neu angelegt.");

												ntcount++;
												if(newtops.size() > STORE_JUNK)
												{
													junkStore(newtops, oid_haus);
													newtops.clear();
													ntcount = 0;
												}
											}
										}
										if(zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())// 27392
										{
											if(null == oid_top)
											{
												createtops = true;
												createplaintops = true;

												DynGenDataObj dgd = createTop(ht, oid_haus, azl);
												if(null != dgd)
												{
													newtops.put("NEW" + ntcount, dgd);

													zlprotocol.appendMailMsg("<br> " + Tr.t("textRentalUnitExists1", session.getString("language")) + " " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " " + Tr.t("textRentalUnitExists3", session.getString("language")));
													log("top " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " neu angelegt. <br>");

													ntcount++;
													if(newtops.size() >= STORE_JUNK)
													{
														junkStore(newtops, oid_haus);
														newtops.clear();
														ntcount = 0;
													}
												}

												log("Unbekannte(s) Mieteinheit/Top " + top + " in Haus " + azl.edvNr + " " + azl.haus + " beim automatischen Upload.");
											}
											else
											{
												tops_in_zl.put(TopoTool.unifyTop(top), oid_top);
												if(te.hasEdvNr())
												{
													tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
												}
												if(sapnummer.length() > 0)
												{
													tops_in_zl.put("sapnummer" + sapnummer, te.id);
												}
											}
										}
									}
									// STELLPLAETZE DURCHGEHEN
									for(int j = 0; j < azl.stellplaetze.size(); j++)
									{
										Hashtable ht = (Hashtable)azl.stellplaetze.get(j);
										String top = (String)ht.get("top");

										TopElement te = null;
										// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
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

										// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so
										if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
										{
											String tmpTop = top.substring(0, top.indexOf(" |"));
											te = top_list.getTop(tmpTop);
										}

										String oid_top = null;
										if(te != null)
										{
											oid_top = te.getId();
										}

										if(null == oid_top)
										{
											createtops = true;
											createplainstellplaetze = true;

											if(zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())
											{
												DynGenDataObj dgd = createStellplatz(ht, oid_haus, azl);
												if(null != dgd)
												{
													newtops.put("NEW" + ntcount, dgd);

													zlprotocol.appendMailMsg("<br> " + Tr.t("textParkingSpaceExists1", session.getString("language")) + " " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " " + Tr.t("textRentalUnitExists3", session.getString("language")));

													log("stellplatz " + ht.get("top") + " in " + azl.haus + " " + azl.plz + " " + azl.ort + " neu angelegt.");
													ntcount++;

													if(newtops.size() > STORE_JUNK)
													{
														junkStore(newtops, oid_haus);
														newtops.clear();
														ntcount = 0;
													}
												}
											}

											log("Unbekannter Stellplatz " + top + " in Haus " + azl.edvNr + " " + azl.haus + " beim automatischen Upload.");
										}
										else
										{
											tops_in_zl.put(TopoTool.unifyTop(top), oid_top);
											if(te.hasEdvNr())
											{
												tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
											}
											if(sapnummer.length() > 0)
											{
												tops_in_zl.put("sapnummer" + sapnummer, te.id);
											}
										}
									}

									// CREATE NEW ONES
									if(newtops.size() > 0)
									{
										log("Lege " + newtops.size() + " Mieteinheiten an.");
										Hashtable tres = storeObjectsJunked(newtops, session);
										if(tres != null)
										{
											addTopsToHaus(tres, oid_haus);
										}

										allmytops = tq.getTopsForOID(oid_haus, null, session, DAInst);
										allmyinternaltops = TopoTool.getInternalTopsForTops(allmytops);
										top_list = new TopList(session, global, DAInst, oid_haus, false);
										set("var.toplistjson", top_list.toJSON(ses));
									}

									int missingzz = 0;

									Hashtable allmytopsAKTIV = null;
									if(xc != null)
									{
										String zltypeName = zinslistenImport.getZlTypeConfig().getName();
										this.set("var.zltypename", zltypeName);
										xc.getXMLConfig("hausverwaltung", zltypeName + "mieter");

									}
									if(xc != null && xc.getExpectedtopsofstatus().size() > 0)
									{
										String[] status = new String[xc.getExpectedtopsofstatus().size()];

										int i = 0;
										for(Object statuskey : xc.getExpectedtopsofstatus().keySet())
										{
											status[i] = String.valueOf(statuskey);
											i++;
										}

										allmytopsAKTIV = tq.getTopsForOID(oid_haus, status, session, DAInst);

									}
									else
									{
										allmytopsAKTIV = tq.getTopsForOID(oid_haus, new String[]{
											"1",
											"2"}, session, DAInst);
									}

									Enumeration ek = allmytopsAKTIV.keys();
									while(ek.hasMoreElements())
									{
										String topname = (String)ek.nextElement();
										// kommt das top in der zinszeile vor?
										String unifiedTopName = TopoTool.unifyTop(topname);
										String topEdvNr = "";
										try
										{
											if(topname.matches(".* \\|.*\\|"))
											{
												topEdvNr = "topedvnummer" + topname.substring(topname.indexOf(" |") + 2, topname.length() - 1);
											}
										}
										catch(Exception ex)
										{
											debug.error(ex);
										}

										// Wenn keine Sapnummer in der Config verwendet wird, dann auch nicht auf sapnummer pruefen!
										if(topname.startsWith("sapnummer") && sapnummer.length() == 0)
										{
											continue;
										}

										if(!tops_in_zl.containsKey(unifiedTopName) && !tops_in_zl.containsKey(topEdvNr) && !tops_in_zl.containsKey("sapnummer" + sapnummer))
										{
											String mytid = (String)allmytopsAKTIV.get(topname);
											if(mytid == null)
											{
												mytid = (String)allmytopsAKTIV.get("sapnummer" + sapnummer);
											}

											// PKO - 20160113 #6634-ME noch da obwohl inaktiv in BW
											if(zinslistenImport.getZlTypeConfig().isSettopautomatischaufzusammengelegtOrcreatenewtopsautomatically() && null != mytid)
											{
												// get top from id and set status to -3 = verkauft
												try
												{
													if(null == DAInst)
													{
														net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
														DAInst = conn.getDataAgent();
													}
													DynGenDataObj topdgdforstatuschange = (DynGenDataObj)DAInst.getObject(mytid, "");
													topdgdforstatuschange.set("var.status", "-3");
													DAInst.storeObject(topdgdforstatuschange, topdgdforstatuschange.getTemplateType(), mytid, session);

												}
												catch(Exception ex)
												{
													debug.error(ex);
												}
											}
											else
											{
												// achtung top kommt nicht vor!
												if(null != mytid)
												{
													missingzz++;
												}
											}
										}
									}

									// EXISTIEREN BEREITS
									if(!createtops || zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())
									{
										// JA! IMPORTIEREN
										try
										{
											if(null == DAInst)
											{
												net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
												DAInst = conn.getDataAgent();
											}
											Hashtable zz2store = zinszeilenAnlegen(azl, top_list, oid_haus, false);
											importerror = false;

											Date d1a = new Date();

											// Hashtable zzres = storeObjectsJunked(zz2store, session);
											resultSizeOfStoredObjects += storeObjectsJunked(zz2store, session).size();

											Date d2a = new Date();
											System.out.println("Massenspeicherung von " + resultSizeOfStoredObjects + " Objekten: " + (d2a.getTime() - d1a.getTime()) + " ms.");

											String myurl = dynurl + "?OID=" + oid_haus;
											if(redirectobj.length() > 0)
											{
												myurl = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl, StandardCharsets.UTF_8);
											}

											if(0 == missingzz)
											{
												myurl = CoolStringTool.buildLink(oid_haus, "SHOW", "", azl.edvNr + " " + azl.haus + " " + azl.plz, "", "_blank", "ajaxLink redlink", global, session);
												good.append("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (" + resultSizeOfStoredObjects + "/" + zz2store.size() + " " + Tr.t("textRentRolls", session.getString("language")) + ")</td></tr>");
												zlprotocol.appendMailMsg("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (" + resultSizeOfStoredObjects + "/" + zz2store.size() + " " + Tr.t("textRentRolls", session.getString("language")) + ")</td></tr>");
												zlprotocol.appendHtmlRes("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (" + resultSizeOfStoredObjects + "/" + zz2store.size() + " " + Tr.t("textRentRolls", session.getString("language")) + ")</td></tr>");
											}
											else
											{
												String myEDVNr = azl.edvNr;
												if(null == myEDVNr)
												{
													myEDVNr = "";
												}
												else
												{
													myEDVNr = myEDVNr + " ";
												}

												myurl = CoolStringTool.buildLink(oid_haus, "SHOW", "", myEDVNr + " " + azl.haus + " " + azl.plz, "", "ajaxLink redlink", global, session);
												String myanurl = CoolStringTool.buildLink(oidnew, "READ", "fehlerabfrage=0&createhaus=&createnewtops=" + "&ignoreerrors=&zinslistenindex=" + index.intValue() + "&topoanpassung=" + topoanpassung + "&altezinszeilenloeschen=" + altezinszeilenloeschen + "&wertaenderung=" + wertaenderung, Tr.t("textCaution", session.getString("language")) + " " + missingzz + " " + Tr.t("textMissingRentRolls", session.getString("language")), "", "ajaxLink redlink", global, session);

												good.append("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (<font color=\"#aa0000\">" + myanurl + "</font>)</td></tr>");
												zlprotocol.appendMailMsg("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (<font color=\"#aa0000\">" + myanurl + "</font>)</td></tr>");
												zlprotocol.appendHtmlRes("<br><tr><td>" + myurl + "</td><td>" + Tr.t("textImported", session.getString("language")) + " (<font color=\"#aa0000\">" + myanurl + "</font>)</td></tr>");
											}

											if(!TopoQueries.writeImportInfoForHaus(oid_haus, azl.monat, azl.jahr, DAInst, session))
											{
												debug.log(this, "could not update hausimport info for haus with oid " + oid_haus + "!");
											}

										}
										catch(Exception exe)
										{
											debug.error(exe);
											zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a  class=\"ajaxLink redlink\" href=\"" + url + "\" target=\"_blank\">" + Tr.t("textImportError", session.getString("language")) + "</a></font></td></tr>");
											zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a  class=\"ajaxLink redlink\" href=\"" + url + "\" target=\"_blank\">" + Tr.t("textImportError", session.getString("language")) + "</a></font></td></tr>");
										}
									}
									else
									{
										if(assetmanagerinfo.equals("1"))
										{
											// Mail to responsible Assetmanager
											String mailAndName = getAssetmanagerMailadressFromObject(topotool.getHausOID(azl));
											if(mailAndName.length() > 0)
											{
												if(mailinglist.containsKey(mailAndName))
												{
													// get email and append link
													StringBuffer mailtext = new StringBuffer();
													mailtext.append(mailinglist.get(mailAndName));

													if(createplainstellplaetze && createplaintops)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnitsAndParkingSpaces", session.getString("language")) + "</a></font>");
													}
													else if(createplainstellplaetze)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewParkingSpaces", session.getString("language")) + "</a></font>");
													}
													else if(createplaintops)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnits", session.getString("language")) + "</a></font>");
													}

													mailinglist.put(mailAndName, mailtext.toString());
												}
												else
												{
													// add email and headers then append link
													StringBuffer mailtext = new StringBuffer();

													if(createplainstellplaetze && createplaintops)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnitsAndParkingSpaces", session.getString("language")) + "</a></font>");
													}
													else if(createplainstellplaetze)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewParkingSpaces", session.getString("language")) + "</a></font>");
													}
													else if(createplaintops)
													{
														mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnits", session.getString("language")) + "</a></font>");
													}

													mailinglist.put(mailAndName, mailtext.toString());
												}
											}
										}

										// createtops
										if(createplainstellplaetze && createplaintops)
										{
											zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnitsAndParkingSpaces", session.getString("language")) + "</a></font></td></tr>");
											errorcounter++;
										}
										else if(createplainstellplaetze)
										{
											zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewParkingSpaces", session.getString("language")) + "</a></font></td></tr>");
											errorcounter++;
										}
										else if(createplaintops)
										{
											zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a  class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewRentalUnits", session.getString("language")) + "</a></font></td></tr>");
											errorcounter++;
										}

									}

									String allerrors = azl.getOnlyTextfromErrors(session);
									String[] errors = allerrors.split("____");
									String myerrors = "";
									for(int x = 0; x < errors.length; x++)
									{
										myerrors = myerrors + "<br>" + errors[x];
									}
									if(!myerrors.equals("<br>") && myerrors.length() > 0)
									{
										zlprotocol.appendMailMsg("<br>" + Tr.t("textErrorInRentRoll", session.getString("language")) + "<br/>" + myerrors + "<br/>");
									}

									if(assetmanagerinfo.equals("1"))
									{
										// Mail to responsible Assetmanager
										String mailAndName = getAssetmanagerMailadressFromObject(topotool.getHausOID(azl));

										// PKO - REMOVE - Only testing purpose
										System.out.println("AM MAILS TO (1): " + mailAndName + " // Hausinfos:" + String.valueOf(azl.edvNr) + " - " + String.valueOf(azl.adresse) + " - " + String.valueOf(azl.ort) + " - " + String.valueOf(azl.plz));

										if(mailAndName.length() > 0)
										{
											if(mailinglist.containsKey(mailAndName))
											{
												// get email and append link
												StringBuffer mailtext = new StringBuffer();
												mailtext.append(mailinglist.get(mailAndName));
												String myurl = dynurl + "?OID=" + oid_haus;
												if(redirectobj.length() > 0)
												{
													myurl = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl, StandardCharsets.UTF_8);
												}
												myurl = CoolStringTool.buildLink(oid_haus, "SHOW", "", azl.edvNr + " " + azl.haus + " " + azl.plz, "", "_blank", "ajaxLink redlink", global, session);

												mailtext.append("<br>" + myurl + " " + Tr.t("textImported", session.getString("language")) + "</td></tr>");

												mailinglist.put(mailAndName, mailtext.toString());
											}
											else
											{
												// add email and headers then append link
												StringBuffer mailtext = new StringBuffer();
												String myurl = dynurl + "?OID=" + oid_haus;
												if(redirectobj.length() > 0)
												{
													myurl = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl, StandardCharsets.UTF_8);
												}
												myurl = CoolStringTool.buildLink(oid_haus, "SHOW", "", azl.edvNr + " " + azl.haus + " " + azl.plz, "", "_blank", "ajaxLink redlink", global, session);

												mailtext.append("<br>" + myurl + " " + Tr.t("textImported", session.getString("language")) + "</td></tr>");

												mailinglist.put(mailAndName, mailtext.toString());
											}
										}
									}

								}
								else
								{

									// moeglicherweise nur leicht
									zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textEasyErrors", session.getString("language")) + "</a></font></td></tr>");
									haeuserMitFehlern.put(oid_haus, "<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textEasyErrors", session.getString("language")) + "</a></font></td></tr>");
									String allerrors = azl.getOnlyTextfromErrors(session);
									String[] errors = allerrors.split("____");
									String myerrors = "";
									for(int x = 0; x < errors.length; x++)
									{
										myerrors = myerrors + "<br>" + errors[x];
									}
									if(!myerrors.equals("<br>") && myerrors.length() > 0)
									{
										// errorsformailinglist
										// .append("<br>" + Tr.t("textErrorInRentRoll", session.getString("language"))
										// + "<br/>" + "<br/>" + myerrors + "<br/>");
										zlprotocol.appendMailMsg("<br>" + Tr.t("textErrorInRentRoll", session.getString("language")) + "<br/>" + myerrors + "<br/>");
									}

									if(assetmanagerinfo.equals("1"))
									{
										// Mail to responsible Assetmanager
										String mailAndName = getAssetmanagerMailadressFromObject(topotool.getHausOID(azl));

										// PKO - REMOVE - Only testing purpose
										System.out.println("AM MAILS TO (1): " + mailAndName + " // Hausinfos:" + String.valueOf(azl.edvNr) + " - " + String.valueOf(azl.adresse) + " - " + String.valueOf(azl.ort) + " - " + String.valueOf(azl.plz));

										if(mailAndName.length() > 0)
										{
											if(mailinglist.containsKey(mailAndName))
											{
												// get email and append link
												StringBuffer mailtext = new StringBuffer();
												mailtext.append(mailinglist.get(mailAndName));
												mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textEasyErrors", session.getString("language")) + "</a></font>");
												mailinglist.put(mailAndName, mailtext.toString());
											}
											else
											{
												// add email and headers then append link
												StringBuffer mailtext = new StringBuffer();
												// mailtext.append("<div id=\"datatablesmmx\"><table class='display' id='datatableicrs' width='98%'>");
												mailtext.append("<br>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " <font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textEasyErrors", session.getString("language")) + "</a></font>");
												mailinglist.put(mailAndName, mailtext.toString());
											}
										}
									}
									importerror = true;
									errorcounter++;
									// setImportStatus("3");
								}
							}
							else
							{ // KEINE BERECHTIGUNG !!!!!
								zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td>" + Tr.t("textNoPermission", session.getString("language")) + "</td></tr>");
								zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td>" + Tr.t("textNoPermission", session.getString("language")) + "</td></tr>");
								importerror = true;
								errorcounter++;
								// setImportStatus("3");
								set("var.errorcode", Tr.t("textNoPermission", session.getString("language")));
								debug.error(this, "ZLIMPORT:" + Tr.t("textNoPermission", session.getString("language")));
								// Setzt den aktuellen Status des Zinslistenimports
								setImportStatus("3");
								this.set("dirty", "yes");
								session.set("TRANSACTIONTRIGGER", "");

								String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
								String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
								updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_ERROR, "RRImport file " + file + ". No write access ", link, rrplainlink, last_oid_haus);
								if(isJSON)
								{
									String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
									return new ParseResult(jerr, 0, "", ses);
								}
								return super.parse(templatecode, global, session);

							}

							// Update Haus nach Top/ZZ Update to trigger Hausberechnungen -> Statusaenderungen am Haus
							String hausOID = updateHaus(azl, oid_haus);
						}
						else
						{
							zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewHouse", session.getString("language")) + "</a></font></td></tr>");
							zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td><font color=\"#aa0000\"><a class=\"redlink\" href=\"" + urlError + "\" target=\"_blank\">" + Tr.t("textNewHouse", session.getString("language")) + "</a></font></td></tr>");
							importerror = true;
							errorcounter++;
							// setImportStatus("3");
						}
					}
					else
					{ // ZU ALTE LISTE !!!!!
						zlprotocol.appendHtmlRes("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td>" + Tr.t("textTooOld", session.getString("language")) + "</td></tr>");
						zlprotocol.appendMailMsg("<br><tr><td>" + azl.edvNr + " " + azl.haus + " " + azl.plz + " </td><td>" + Tr.t("textTooOld", session.getString("language")) + "</td></tr>");
						importerror = true;
						errorcounter++;
						// setImportStatus("3");
					}
				}

				// Gebaeude anlegen / Updaten wenn vorhanden + Mapping zu den Tops!
				createOrUpdateGebaeudeAndTopMapping(azl, oid_haus);

				debug.info("I've Datapackages -> No:" + azl.getDhwDatapackages().size());
				for(DataPackage dataPackage : azl.getDhwDatapackages())
				{
					debug.info("Datapackage Name:" + dataPackage.dbtable);
					String[][] datapackage = dataPackage.data;
					for(int i = 0; i < datapackage.length; i++)
					{
						StringBuffer buff = new StringBuffer();
						for(int j = 0; j < datapackage[0].length; j++)
						{
							buff.append(datapackage[i][j]);
							buff.append(";");
						}
						debug.info(buff.toString());
					}

					// TODO: delete restrictions wie in dwh buchungen!
					DWHHandler dwhHandler = DWHHandler.getDWHHandler(dataPackage, session);
					dwhHandler.deleteFactsByUniqueEntryDefinitionFieldnames(dataPackage);
					dwhHandler.insert(dataPackage);
				}

				// 22975-7606-Infoerweiterung beim Zinslistenimport
				// query auf ZZ um die aktuelle und vorperiode zu bekommen!
				boolean periodenvergleich = this.getBoolean("var.periodenvergleich", Boolean.TRUE);
				if(periodenvergleich)
				{
					generatePeriodenvergleich(oid_haus, azl);
				}

				java.util.Date end_time = new java.util.Date();
				long run_time = end_time.getTime() - start_time.getTime();
				System.out.println("" + azl.edvNr + azl.haus + " - import " + run_time / 1000 + " secs.");
			}

			this.set("var.adresse", "Verarbeitung abgeschlossen // OK: " + (liste.size() - errorcounter) + " // Nicht OK: " + errorcounter + " // Gesamt: " + liste.size());

			this.set("var.hausverwaltung", hausverwaltung);
			this.set("var.filename", lockname);
			this.set("var.sapconnection", "");

			updateProgess(new BigDecimal("0.9"), "Verarbeitung abgeschlossen // OK: " + (liste.size() - errorcounter) + " // Nicht OK: " + errorcounter + " // Gesamt: " + liste.size(), null);

			String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
			String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
			updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_PENDING, "RRImport file " + file + "import Done // OK: " + (liste.size() - errorcounter) + " // Not OK: " + errorcounter + " // Total: " + liste.size(), link, rrplainlink, last_oid_haus);

			setImportStatus("1");

			// execute success callback
			zinslistenImport.onSuccess(hausOIDs);

			zlprotocol.appendHtmlRes("</tbody></table></div>");

			// Add Changes to the Assetmanager notification
			csvStr = zlprotocol.getCSV();

			String filename = this.getString("var.name");
			String mynewfilelink = createLink(zlfile, filename, session);
			String filenameforemail = this.getString("var.filename");

			if((filenameforemail == null || filenameforemail.length() == 0) && myfparams != null)
			{
				filenameforemail = String.valueOf(myfparams.get("name"));
			}
			zlprotocol.appendMailMsg("<br><br>" + Tr.t("textFile", session.getString("language")) + " " + filenameforemail + " (" + link + ")");

			String[] mailLines = zlprotocol.getMailMsg().split("<br>\n");

			// Nur wenn Parameter sendmailonlyonchange=1 gesetzt ist Mail an die AMs schicken
			boolean sendmailonlyonchange = this.getBoolean("var.sendmailonlyonchange", true);

			System.out.println("sendMailToAssetmanager[1]: " + sendmailonlyonchange);
			if(sendmailonlyonchange)
			{
				String newCsvStr = "";
				String newMailLines = "";
				boolean hasChanges = false;
				if(csvStr.length() > 0)
				{
					String[] protokoll = csvStr.split("\n");
					for(int i = 0; i < protokoll.length; i++)
					{
						if(!protokoll[i].toLowerCase().contains("keine änderungen") && !protokoll[i].toLowerCase().contains("no change for object"))
						{
							newCsvStr += protokoll[i] + "\n";
						}
					}
				}
				csvStr = newCsvStr;

				if(mailLines.length > 0)
				{
					for(int i = 0; i < mailLines.length; i++)
					{
						if(!mailLines[i].toLowerCase().contains("keine änderungen") && !mailLines[i].toLowerCase().contains("no change for object"))
						{
							newMailLines += mailLines[i] + "<br>\n";
						}
					}
				}
				mailLines = newMailLines.split("<br>\n");
			}

			Pattern wePattern = Pattern.compile("(WE|SE)[ ]{1,2}([0-9/]*)(.*)([\\d]{4,5}) (.*)");
			Pattern adressePattern = Pattern.compile("(.*) ([0-9]{4,5}) (.*)");
			Matcher matcher = null;

			for(int i = 0; i < mailLines.length; i++)
			{

				String[] line = mailLines[i].split(";");

				String adresse = "";
				String edvnr = "";
				String identadresse1 = "";
				String plz = "";

				String lineWithoutToplink = "";
				if(line[0].contains("<a"))
				{
					lineWithoutToplink = line[0].substring(0, line[0].indexOf("<a"));
				}
				else
				{
					lineWithoutToplink = line[0];
				}

				if(line[0].matches("(WE|SE) [0-9/]*.*") || line[0].startsWith("WE ") || line[0].startsWith("SE "))
				{
					try
					{
						matcher = wePattern.matcher(line[0]);
						if(matcher.matches())
						{
							String wenummer = matcher.group(2);
							if(wenummer.length() > 10)
							{
								// 0010/10041301 -> GS Nummer wegschneiden
								wenummer = wenummer.substring(0, wenummer.length() - 3);
							}
							identadresse1 = "WE " + wenummer;
						}
					}
					catch(Exception e)
					{
						debug.error("ERROR Line for WE/SE Pattern: " + line[0]);
						// debug.error(e);
					}
				}
				else if(lineWithoutToplink.contains("|"))
				{
					adresse = line[0].substring(0, line[0].indexOf("|")).trim();
					edvnr = line[0].substring(line[0].indexOf("|") + 1, line[0].lastIndexOf("|")).trim();
				}
				else
				{
					try
					{
						matcher = adressePattern.matcher(line[0]);
						adresse = matcher.group(1);
						plz = matcher.group(2);
					}
					catch(Exception e)
					{
						debug.error("ERROR Line for Adresse/Plz: " + line[0]);
						// debug.error(e);
					}
				}

				// Fallback for wrong adress
				if(adresse.contains("<a class=ajaxLink"))
				{
					adresse = line[0].substring(0, line[0].indexOf("<a class=ajaxLink")).trim();
					adresse += " Dummy";

					matcher = adressePattern.matcher(adresse);

					if(matcher.matches())
					{
						adresse = matcher.group(1);
						plz = matcher.group(2);
					}
				}

				if(adresse.length() == 0 && identadresse1.length() == 0)
				{
					continue;
				}

				Zinsliste zl = new Zinsliste();
				zl.setAdresse(adresse);
				zl.setEdvNr(edvnr);
				if(identadresse1.length() > 0)
				{
					zl.setSpecialIdentification(identadresse1);
				}
				zl.setPlz(plz);

				String mailAndName = getAssetmanagerMailadressFromObject(topotool.getHausOID(zl));

				// PKO - So ein Kaese -> da werden Haeuser die nicht dem jeweiligen AM gehoehren irgendjemand zugeorndet -> kommt aus Zinsliste.java
				if(mailAndName.length() > 0)
				{
					String mAndN[] = mailAndName.split(";");
					String mail = mAndN[0];
					String name = mAndN[1];

					// #6996 - 90364 - WE wechselt Buchungskreis
					// Liefert WEs die nicht in der SAP Query mitkommen aber noch im bestand sind und kein Verkaufsdatum haben
					if(mailinglist.containsKey(mailAndName))
					{
						try
						{
							// get email and append link
							StringBuffer mailtext = new StringBuffer();
							mailtext.append(mailinglist.get(mailAndName));

							mailtext.append(mailLines[i] + "<br>");
							mailinglist.put(mailAndName, mailtext.toString());
						}
						catch(Exception e)
						{
							if(mailLines == null)
							{
								debug.error("MailLines is null");
							}
							else
							{
								debug.error("i: " + i + " // mailLines.size: " + mailLines.length);
							}
						}
					}
					else
					{
						try
						{
							// add email and headers then append link
							StringBuffer mailtext = new StringBuffer();
							mailtext.append("<br><br>");

							mailtext.append(mailLines[i] + "<br>");
							mailinglist.put(mailAndName, mailtext.toString());
						}
						catch(Exception e)
						{
							if(mailLines == null)
							{
								debug.error("MailLines is null");
							}
							else
							{
								debug.error("i: " + i + " // mailLines.size: " + mailLines.length);
							}
						}
					}
				}
			}

			boolean leerstandsmail = this.getBoolean("var.leerstandsmail", Boolean.FALSE);
			if(leerstandsmail && mailinglist.size() > 0)
			{
				try
				{
					if(assetmanagerAndIDs == null || assetmanagerAndIDs.size() == 0)
					{
						assetmanagerAndIDs = getAllAssetmanagerAndIds(session);
					}

					Hashtable<String, String> oidAndArea = getOidAndAreaOfVacantRentrolls(session);

					Enumeration keys = mailinglist.keys();
					while(keys.hasMoreElements())
					{
						String key = (String)keys.nextElement();
						String value = mailinglist.get(key);

						String[] lines = value.split("<br>");
						StringBuffer newValue = new StringBuffer();

						for(int i = 0; i < lines.length; i++)
						{
							if(lines[i].contains("auf \"Leerstehung\""))
							{
								String actualOid = lines[i].substring(lines[i].indexOf("OID%3D"));
								actualOid = actualOid.replace("OID%3D", "");
								actualOid = actualOid.substring(0, actualOid.indexOf("&"));

								if(oidAndArea.containsKey(actualOid))
								{
									String newLine = lines[i].substring(0, lines[i].indexOf("</a>"));
									newLine += "</a>";
									newLine += ", ";
									newLine += "MV-Fläche (SAP): " + oidAndArea.get(actualOid).replace(".", ",") + "m&sup2;";
									newLine += ", ";
									newLine += "LIS-Status: unklassifiziert";
									newLine += "<br>";
									newValue.append(newLine);
								}

							}
						}

						if(newValue.length() > 0)
						{
							StringBuffer salutation = new StringBuffer();

							salutation.append("Sehr geehrte(r) " + key.substring(key.indexOf(";") + 1) + "!");
							salutation.append("<br>");
							salutation.append("<br>");
							salutation.append("Bitte klassifizieren Sie die folgenden neuen Leerstände:");
							salutation.append("<br>");
							salutation.append("<br>");
							salutation.append(newValue.toString());
							salutation.append("<br>");
							salutation.append("<br>");

							// Url fuer Leerstandsreprot
							// String reporturl = CoolStringTool.buildLink(oidnew, "SHOW", "", "Importdetails", "", "_blank", "ajaxLink redlink", global, session);

							String sessid = session.getString("SESSIONID");
							String linkClass = "ajaxLink";
							String linkTarget = "_blank";
							String url = dynurl + "?OID=DIRECT_ICRS.reports.report&reporttemplate=ICRS.reports.icrsare.leerstandslistetopsextendedrepare";

							if(assetmanagerAndIDs.containsKey(key.substring(key.indexOf(";") + 1)))
							{
								url += "&addfilterpreselectedvalues=queryassetmanager_ID=" + assetmanagerAndIDs.get(key.substring(key.indexOf(";") + 1));
							}

							url += "&VIEW=SHOW&wrapper=NO";
							String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
							StringBuffer urlSB = new StringBuffer();
							urlSB.append("<a href=\"");
							urlSB.append(CoolWebTool.getUsedDomain(session));
							urlSB.append(dynurl);
							urlSB.append("?OID=" + CfgSingleton.getHijaxTarget(session) + "&contenturl=");
							urlSB.append(encodedUrl);
							urlSB.append("&FLAVOUR=");
							urlSB.append(flavour);
							urlSB.append("&ESSENCEID=");
							urlSB.append(sessid);
							urlSB.append("\" ");
							if(null != linkClass && linkClass.trim().length() > 0)
							{
								urlSB.append(" class=\"" + linkClass + "\" ");
							}
							if(null != linkTarget && linkTarget.trim().length() > 0)
							{
								urlSB.append(" target= \"" + linkTarget + "\" ");
							}
							urlSB.append(">");
							urlSB.append("hier");
							urlSB.append("</a>");

							salutation.append("Zur Abfrage der aktuellen Leerstandsliste für Ihr Teilportfolio klicken Sie bitte " + urlSB + ".");
							salutation.append("<br>");
							salutation.append("<br>");
							salutation.append("Die Anleitung zur Klassifizierung des Leerstands im BIG-Konzern finden Sie im PMS unter Dokumente / PMS Dokumente / PMS Paper. ");
							salutation.append("<br>");
							salutation.append("<br>");
							salutation.append("Vielen Dank für Ihre Unterstützung!");

							leerstandmailinglist.put(key, salutation.toString());
						}

					}

					// send Mails
					Date now = new Date();
					GregorianCalendar crecalendar = new GregorianCalendar();
					crecalendar.setTime(now);

					Hashtable<String, String> newleerstandmailinglist = new Hashtable<String, String>();

					for(String key : leerstandmailinglist.keySet())
					{

						String mailAndName[] = key.split(";");
						String mail = mailAndName[0];
						String name = mailAndName[1];

						String value = leerstandmailinglist.get(key);

						// Add Mailverteiler to Mailinglist
						Hashtable mailverteiler = getMailverteilerFromAssetmanager(name);
						Enumeration mvkeys = mailverteiler.keys();
						String verteileradressekey = "";
						String newMailAdresses = mail;
						while(mvkeys.hasMoreElements())
						{
							verteileradressekey = mvkeys.nextElement().toString().replaceAll(" ", "");
							// mailinglistNew.put(verteileradressekey, value);
							if(verteileradressekey.length() > 0 && !leerstandmailinglist.contains(verteileradressekey))
							{
								newMailAdresses = newMailAdresses + "," + verteileradressekey;
							}
						}

						newleerstandmailinglist.put(newMailAdresses, value);
					}

					BugMe.getInstance("maillogfile").log("RRIMPORT> [" + getName() + "] preparing vacancy mail to " + newleerstandmailinglist.toString());

					if(assetmanagerinfo.equals("1"))
					{
						sendMailToAssetmanager(newleerstandmailinglist, "unklassifizierter Leerstand – Datenimport vom " + net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()) + "");

					}
				}
				catch(Exception e)
				{
					debug.error(e);
				}

			}

			boolean ablaufendevetraegemail = this.getBoolean("var.ablaufendevetraegemail", Boolean.FALSE);
			if(ablaufendevetraegemail && mailinglist.size() > 0)
			{
				try
				{
					String jahr = this.getString("var.jahr");
					String monat = this.getString("var.monat");
					if(monat.length() == 1)
					{
						monat = "0" + monat;
					}
					String ablaufendeVertraegeInMonaten = "6";

					Date date = format.parse("01." + monat + "." + jahr);

					// Date stichtag = this.getDate("var.stichtag");

					Calendar startDatum = new GregorianCalendar();
					startDatum.setTime(date);

					Calendar endDatum = new GregorianCalendar();
					endDatum.setTime(date);
					endDatum.add(Calendar.MONTH, +6);

					Enumeration keys = mailinglist.keys();
					while(keys.hasMoreElements())
					{
						String key = (String)keys.nextElement();
						String value = mailinglist.get(key);

						Hashtable<String, String> ablaufendevertraege = getAblaufendeVertraegeForAssetmanager(startDatum, endDatum, key);
						ablaufendevertraegemailinglist.putAll(ablaufendevertraege);
					}

					// send Mails
					Date now = new Date();
					GregorianCalendar crecalendar = new GregorianCalendar();
					crecalendar.setTime(now);

					Hashtable<String, String> newablaufendevertraegemailinglist = new Hashtable<String, String>();

					for(String key : ablaufendevertraegemailinglist.keySet())
					{

						String mailAndName[] = key.split(";");
						String mail = mailAndName[0];
						String name = mailAndName[1];

						String value = ablaufendevertraegemailinglist.get(key);

						// Add Mailverteiler to Mailinglist
						Hashtable mailverteiler = getMailverteilerFromAssetmanager(name);
						Enumeration mvkeys = mailverteiler.keys();
						String verteileradressekey = "";
						String newMailAdresses = mail;
						while(mvkeys.hasMoreElements())
						{
							verteileradressekey = mvkeys.nextElement().toString().replaceAll(" ", "");
							// mailinglistNew.put(verteileradressekey, value);
							if(verteileradressekey.length() > 0 && !newablaufendevertraegemailinglist.contains(verteileradressekey))
							{
								newMailAdresses = newMailAdresses + "," + verteileradressekey;
							}
						}

						newablaufendevertraegemailinglist.put(newMailAdresses, value);

					}
					BugMe.getInstance("maillogfile").log("RRIMPORT> [" + getName() + "] preparing terminating contracts mail to " + newablaufendevertraegemailinglist.toString());

					if(assetmanagerinfo.equals("1"))
					{
						sendMailToAssetmanager(newablaufendevertraegemailinglist, "In den nächsten " + ablaufendeVertraegeInMonaten + " Monaten ablaufende Verträge - Datenimport vom " + net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()) + "");
					}
				}
				catch(Exception e)
				{
					debug.error(e);
				}
			}

			// Add Ending Table to every Mail recipiant (Assetmanager)
			if(mailinglist.size() > 0)
			{
				Enumeration keys = mailinglist.keys();
				while(keys.hasMoreElements())
				{
					String key = (String)keys.nextElement();
					String value = mailinglist.get(key);

					// value = value + "</table></div><br><br><br>";
					value = value + "<br><br><br>";

					// Add also mailinglistKennwerteNachNutzung here !!!
					// mailinglistKennwerteNachNutzung Table schliessen
					for(String mailinglistKennwerteNachNutzungKey : mailinglistKennwerteNachNutzung.keySet())
					{
						// get email and append link
						StringBuffer mailtext = new StringBuffer();
						mailtext.append(mailinglistKennwerteNachNutzung.get(mailinglistKennwerteNachNutzungKey));
						mailtext.append("</table><br><br><br>");
						mailinglistKennwerteNachNutzung.put(mailinglistKennwerteNachNutzungKey, mailtext.toString());
					}

					if(mailinglistKennwerteNachNutzung != null && mailinglistKennwerteNachNutzung.size() > 0 && mailinglistKennwerteNachNutzung.containsKey(key))
					{
						value = value + mailinglistKennwerteNachNutzung.get(key);
					}

					mailinglist.put(key, value);
				}
			}

			// if maillist > 0 then add salutation to assetmanager mail and change key to mailadress
			if(mailinglist.size() > 0)
			{
				mailinglist = formatMailContent(mailinglist, Boolean.FALSE);
			}

			// if maillist > 0 then add salutaion to assetmanager mail and change key to mailadress
			if(mailinglist.size() > 0)
			{
				Hashtable<String, String> mailinglistNew = new Hashtable<String, String>();

				Enumeration keys = mailinglist.keys();
				while(keys.hasMoreElements())
				{
					String key = (String)keys.nextElement();
					String value = mailinglist.get(key);

					String mailAndName[] = key.split(";");
					String mail = mailAndName[0];
					String name = mailAndName[1];

					String salutation = Tr.t("textSalutationAssetmanagerNotify", session.getString("language")) + " " + name + "<br><br>";
					salutation = salutation + Tr.t("textAssetmanagerNotify", session.getString("language"));

					StringBuffer missingWEsText = new StringBuffer();

					if(missingWEs.containsKey(key))
					{
						// missingWEsText.append("<br>Folgende WEs fehlen im Datenload:<br>");

						Hashtable<String, Hashtable<String, String>> h = missingWEs.get(key);

						Enumeration keys1 = h.keys();
						while(keys1.hasMoreElements())
						{
							String key1 = (String)keys1.nextElement();
							Hashtable row = h.get(key1);
							missingWEsText.append("<b>SE " + row.get("identadresse5") + " / " + row.get("wename") + " / " + row.get("plz") + " fehlt im SAP Import!</b><br>");
						}
					}

					value = salutation + "<br><br>" + missingWEsText.toString() + "<br>" + value;

					String footer = "<br><br>" + Tr.t("textFooter1AssetmanagerNotify", session.getString("language")) + "<br><br>";
					footer = footer + Tr.t("textFooter2AssetmanagerNotify", session.getString("language")) + "<br>";
					footer = footer + session.get("domainname");

					value = value + footer;

					mailinglistNew.put(mail, value);

					// Add Mailverteiler to Mailinglist
					Hashtable mailverteiler = getMailverteilerFromAssetmanager(name);
					Enumeration mvkeys = mailverteiler.keys();
					String verteileradressekey = "";
					String newMailAdresses = mail;
					while(mvkeys.hasMoreElements())
					{
						verteileradressekey = mvkeys.nextElement().toString().replaceAll(" ", "");
						// mailinglistNew.put(verteileradressekey, value);
						if(verteileradressekey.length() > 0 && !newMailAdresses.contains(verteileradressekey))
						{
							newMailAdresses = newMailAdresses + "," + verteileradressekey;
						}
					}

					mailinglistNew.remove(mail);
					mailinglistNew.put(newMailAdresses, value);

				}
				mailinglist = mailinglistNew;
			}

			String rc = zlprotocol.getHtmlRes();
			if(StringUtils.isNotBlank(rc))
			{
				rc = formatResultCode(rc, Boolean.TRUE);
			}
			set("var.resultcode", rc);
			set("var.errorcode", "");
			set("var.errorcodetxt", "");

			// result.put("importresult", rc);

			String myurl = CoolStringTool.buildLink(oidnew, "SHOW", "", "Importdetails", "", "_blank", "ajaxLink redlink", global, session);

			String sessid = session.getString("SESSIONID");
			String linkClass = "ajaxLink redlink";
			String linkTarget = "_blank";

			String url = dynurl + "?OID=" + oidnew + "&VIEW=INFO&ESSENCEID=" + sessid;
			String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
			StringBuffer urlSB = new StringBuffer();
			urlSB.append("<a href=\"");
			urlSB.append(CoolWebTool.getUsedDomain(session));
			urlSB.append(dynurl);
			urlSB.append("?OID=" + CfgSingleton.getHijaxTarget(session) + "&contenturl=");
			urlSB.append(encodedUrl);
			urlSB.append("&FLAVOUR=");
			urlSB.append(flavour);
			urlSB.append("&ESSENCEID=");
			urlSB.append(sessid);
			urlSB.append("\" ");
			if(null != linkClass && linkClass.trim().length() > 0)
			{
				urlSB.append(" class=\"" + linkClass + "\" ");
			}
			if(null != linkTarget && linkTarget.trim().length() > 0)
			{
				urlSB.append(" target= \"" + linkTarget + "\" ");
			}
			urlSB.append(">");
			urlSB.append("Importdetails");
			urlSB.append("</a>");

			result.put("importresult", urlSB.toString());

			this.set("dirty", "yes");

			try
			{
				this.fixFileLink();
				oidnew = DAInst.storeObject(this, templateType, oidnew, session);
			}
			catch(Exception exc)
			{
				debug.error(exc);
			}

			String datasource = Tr.t("textdatasource", session.getString("language"));
			String myfilename = this.get("var.filename").toString();
			zlprotocol.appendMailMsg("<br><br>" + datasource + ": \n" + myfilename);

			long end_upload = System.currentTimeMillis();
			long upload_time_s = (end_upload - start_upload) / 1000;
			long upload_time_m = upload_time_s / 60;
			upload_time_s = upload_time_s % 60;
			this.set("var.duration", "" + upload_time_m + "m " + upload_time_s + "s");
			zlprotocol.appendMailMsg("<br>processing: \n" + upload_time_m + "m " + upload_time_s + "s");
			if(!zlprotocol.isCSVEmpty())
			{ // nur wenn das protokoll schon Einträge hat haengen wir die Zeit an...
				zlprotocol.addCsvLine(new String[]{
					"<br>",
					datasource + ": " + myfilename,
					"",
					"",
					"processing:",
					"",
					"",
					upload_time_m + "m " + upload_time_s + "s",
					"",
					""});
			}
			// end storeObjects

			// Formatierung ok
			if(assetmanagerinfo.equals("1"))
			{
				sendMailToAssetmanager(mailinglist, "");
			}
			// an user icrs und an bcc
			sendMailWithChanges();

			// sendMailToExcecutor(rc);

			Date endtime = new Date();
			long runtimeinms = endtime.getTime() - starttime.getTime();

			String runtime = "%d min, %d sec".formatted(TimeUnit.MILLISECONDS.toMinutes(runtimeinms), TimeUnit.MILLISECONDS.toSeconds(runtimeinms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runtimeinms)));

			// System.out.println("##### ZINSLISTENIMPORT Runtime for Import - " + "zinslistenimport" + " - tooks: " + runtime + " #####");
			debug.error("##### ZINSLISTENIMPORT Runtime for Import - " + "zinslistenimport" + " - tooks: " + runtime + " #####");

			result.put("runtime", runtime);

			LockingSingleton.getInstance().leave(lockname);

			updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);

			pp.deregisterProcess();
		}
		else if(view.endsWith("READ"))
		{
			if(enableDetailedLogging)
			{
				starttime = System.currentTimeMillis();
			}

			pp = this.registerProcess();
			updateProgess(BigDecimal.ZERO, "Import gestartet", ProcessStatus.RUNNING);

			LockingSingleton.getInstance().enter(lockname);

			Vector quellsystemResult = null;

			// Hier den SAP import einklinken!!!!
			if(sapconnection.equals("1") && quellsystem.equals("sapare") && file.length() == 0)
			{
				String sapimportname = this.getString("var.sapimportname");

				if(sapimportname.length() > 0)
				{
					SAPQuery query = new SAPQuery();
					quellsystemResult = query.getMergedXMLAContentAsVector(sapimportname);
					if(quellsystemResult == null)
					{
						result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
						set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");
						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();

						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							return new ParseResult(jerr, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);

					}

					// Delete rows that are marked to delete -> column 'RejectLine' == 1
					QueryHelper.writeQueryResultToFilesystem(quellsystemResult, sapimportname + "_before_rejecting_lines_", quellsystem);

					quellsystemResult = SAPQuery.deleteMarkedRows(quellsystemResult);

					if(enableDetailedLogging)
					{
						endtime = System.currentTimeMillis();
						BugMe.getInstance().log("############ Log1: " + ((endtime - starttime) / 1000) + " seconds");
						starttime = System.currentTimeMillis();
					}
				}
			}
			// 1==FIO Axera
			else if(sapconnection.equals("1") && quellsystem.equals("fioaxera") && file.length() == 0)
			{
				// Datum noch aus GUI holen -> Default ist aktuelles Datum
				// zinslistendatum

				SwaggerQuery scc = new SwaggerQuery();

				Hashtable<String, String> parameters = new Hashtable<>();
				String year = "";
				String month = "";
				String day = "";

				month = getString("var.monatvon");
				year = getString("var.jahrvon");

				Calendar cal = GregorianCalendar.getInstance();
				cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
				cal.set(Calendar.YEAR, Integer.parseInt(year));
				cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

				day = String.valueOf(cal.getActualMaximum(Calendar.DAY_OF_MONTH));

				Calendar actualcal = GregorianCalendar.getInstance();

				if(!cal.before(actualcal))
				{
					day = String.valueOf(actualcal.get(Calendar.DAY_OF_MONTH));
				}

				// Zinslisten import Datum adden ....

				// Parameter for old query
				// parameters.put("fiscalYearNumber", year);
				// parameters.put("bookingPeriodNumber", month);

				// Parameter for new query

				if(day.length() == 1)
				{
					day = "0" + day;
				}

				if(month.length() == 1)
				{
					month = "0" + month;
				}

				// 2021-10-14
				String dueDate = year + "-" + month + "-" + day;

				parameters.put("dueDate", dueDate);

				quellsystemResult = scc.getZinslistenQueryResult(parameters);

				if(quellsystemResult == null)
				{
					result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
					set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();

					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						return new ParseResult(jerr, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_rentroll_", quellsystem);
			}
			else if(sapconnection.equals("1") && quellsystem.equals("sapcsv") && file.length() == 0)
			{
				SAPCSVQuery sapcsvquery = new SAPCSVQuery();

				Hashtable<String, String> parameters = new Hashtable<>();

				quellsystemResult = sapcsvquery.getZinslistenQueryResult(parameters);

				if(quellsystemResult == null)
				{
					result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
					set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();

					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						return new ParseResult(jerr, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_rentroll_", quellsystem);
			}
			else if(sapconnection.equals("1") && quellsystem.startsWith("databasesource") && file.length() == 0)
			{
				// $databasesource[init:HausRelionQuery][GUITYPE:NONE]
				// $uploadlistetypeconfig[init:relionlistetypeconfig.xml][GUITYPE:NONE]

				String connectionname = quellsystem.replaceAll("databasesource-", "");
				String databasesource = "RentRoll" + StringUtils.capitalize(connectionname) + "Query";
				String uploadlistetypeconfig = connectionname + "listetypeconfig";

				if(databasesource.length() > 0)
				{
					try
					{
						quellsystemResult = getDatabaseContent(databasesource, uploadlistetypeconfig);

						if(quellsystemResult == null)
						{
							result.put("datastructure", Tr.t("errorNoSapQueryDatastructure", session.getString("language")));
							zlprotocol.appendHtmlErr("<h2>" + Tr.t("errorNoSapQueryDatastructure", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
							session.set("CURRENT_VIEW", "ERRORQUEST");
							set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
							set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
							this.set("dirty", "yes");
							session.set("TRANSACTIONTRIGGER", "");
							// Setzt den aktuellen Status des Zinslistenimports
							setImportStatus("3");

							LockingSingleton.getInstance().leave(lockname);

							updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
							pp.deregisterProcess();
							if(isJSON)
							{
								String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
								return new ParseResult(jerr, 0, "", ses);
							}
							return super.parse(templatecode, glo, ses);
						}

						QueryHelper.writeQueryResultToFilesystem(quellsystemResult, quellsystem + "_rentroll_", quellsystem);
					}
					catch(Exception e)
					{
						debug.error(e);
					}
				}
			}

			String oidnew = "";
			String myoid = (String)ses.get("CURRENT_OID");
			if(myoid == null || myoid.length() == 0)
			{
				myoid = (String)this.get("id");
			}

			if(myoid.length() > 0)
			{
				oidnew = myoid;
			}
			if(sapconnection.equals("2"))
			{
				String templateType = (String)this.get("TEMPLATETYPE");
				try
				{
					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}
					this.fixFileLink();
					oidnew = DAInst.storeObject(this, templateType, null, session);
				}
				catch(Exception e)
				{
					debug.error(e);
				}
			}

			java.util.Date start_time = new java.util.Date();
			String sessid = "";
			if(null != session)
			{
				sessid = (String)session.get("SESSIONID");
			}
			super.set("dirty", "yes");

			new Hashtable<String, String>();

			// hat der user ein paar fehler zum ignorieren angeklickt????
			String ignorefutureerrors = (String)this.get("var.ignoreerrors");
			if(null == ignorefutureerrors)
			{
				ignorefutureerrors = (String)this.get("arg.ignoreerrors");
			}
			if(ignorefutureerrors == null || ignorefutureerrors.length() == 0)
			{
				ignorefutureerrors = (String)session.get("arg.oid" + this.volatile_id + ".ignoreerrors");
			}

			if(null == ignorefutureerrors)
			{
				ignorefutureerrors = "";
			}
			if(ignorefutureerrors.length() > 0)
			{
				ignorefutureerrors = CoolStringTool.replaceStr(ignorefutureerrors, "____", "\n");
			}

			Zinsliste zl = null;
			Zinsliste zle = null;
			super.parse(templatecode, glo, ses);
			// zinslisten file ueber file data agent holen
			if(null == FDAInst)
			{
				net.metamagix.essence.Agents.Connector conn = null;
				conn = new net.metamagix.essence.Agents.Connector();
				FDAInst = conn.getFileDataAgent();
			}

			boolean fehlerabfrage = false;
			try
			{

				// -----------------------------------
				// ZINSLISTE
				// -----------------------------------

				// haben wir eine aus mehreren ausgewaehlt?
				// INDEX WENN ES MEHRERE SIND
				String zinslistenindex = this.getString("var.zinslistenindex");
				String lastzinslistenindex = this.getString("var.lastzinslistenindex");

				// System.err.println("ZLU2: CIMS UPLOAD ZLINDEX " + zinslistenindex);

				Integer zlIndex = null;
				Integer lastzlIndex = null;
				if(lastzinslistenindex.matches("\\d+"))
				{
					lastzlIndex = Integer.valueOf(lastzinslistenindex);
				}
				if(zinslistenindex.matches("\\d+"))
				{
					zlIndex = Integer.valueOf(zinslistenindex);
					this.set("var.zinslistenindex", zinslistenindex);
					this.set("var.lastzinslistenindex", zinslistenindex);
					this.set("dirty", "yes");
				}

				// INDEX WENN ES MEHRERE SIND
				String eigentuemerlistenindex = (String)this.get("var.eigentuemerlistenindex");
				if(null == eigentuemerlistenindex)
				{
					eigentuemerlistenindex = "";
				}
				// System.err.println("ZLU2: CIMS UPLOAD EL INDEX " + eigentuemerlistenindex);
				Integer zleIndex = null;
				try
				{
					zleIndex = Integer.valueOf(eigentuemerlistenindex);
				}
				catch(Exception x)
				{
					// ERROR
				}

				if(null == zlIndex)
				{
					Vector liste = null;

					if(sapconnection.equals("1") && (zlfile == null || zlfile.length() == 0))
					{
						liste = readQuellsystemListe(quellsystemResult, quellsystem);
					}
					else
					{
						liste = readListe(file);
					}
					// System.err.println("ZLU2: IN err steht" + err.toString());

					if(liste == null)
					{
						session.set("TRANSACTIONTRIGGER", "");
						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString("Empty list.");
							return new ParseResult(jerr, 0, "", ses);
						}
						return super.parse(templatecode, global, session);
					}

					if(1 == liste.size())
					{
						// index holen
						Hashtable e = (Hashtable)liste.elementAt(0);
						try
						{
							zlIndex = (Integer)e.get("index");
						}
						catch(Exception e2)
						{
							zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
							session.set("CURRENT_VIEW", "ERRORQUEST");
							set("var.errorcode", Tr.t("textCantReadListBegin", session.getString("language")));
							set("var.errorcodetxt", Tr.t("textCantReadListBegin", session.getString("language")));
							this.set("dirty", "yes");
							session.set("TRANSACTIONTRIGGER", "");
							// Setzt den aktuellen Status des Zinslistenimports

							String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
							String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
							updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + "." + Tr.t("textCantReadListBegin", session.getString("language")), link, rrplainlink, last_oid_haus);

							setImportStatus("3");

							LockingSingleton.getInstance().leave(lockname);

							updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
							pp.deregisterProcess();
							if(isJSON)
							{
								String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
								return new ParseResult(jerr, 0, "", ses);
							}
							return super.parse(templatecode, glo, ses);
						}
					}
					else
					{// USER FRAGEN

						// liste sortieren ...
						liste = VectorOfHashesSorter.sort(liste, "text");
						// System.err.println("ZLU2: BUILDING SELECT!!!!!!!!!");
						session.set("CURRENT_VIEW", "MULTI");
						StringBuffer el_sel = new StringBuffer();
						StringBuffer zl_sel = new StringBuffer();
						org.json.simple.JSONArray el_sel_json = new org.json.simple.JSONArray();
						org.json.simple.JSONArray zl_sel_json = new org.json.simple.JSONArray();
						StringBuffer rest_sel = new StringBuffer();
						// Talk mit Peter am 18.8.: "Mieterliste" weg tun, weils komisch ausschaut wenns alphabetisch ist und man weiss ohnehin was es is
						boolean showMieterlisteInSeletor = false;
						boolean zlselected = false;
						boolean elselected = false;
						for(int p = 0; p < liste.size(); p++)
						{
							// System.err.println("ZLU2: Liste "+p);
							Hashtable e = (Hashtable)liste.elementAt(p);
							String text = (String)e.get("text");
							Integer index = (Integer)e.get("index");
							if(text.indexOf("Mieterliste") >= 0)
							{
								String cleantext = CoolStringTool.replaceStr(text, "Mieterliste, ", "");
								String languageconformtext = CoolStringTool.replaceStr(text, "Mieterliste, ", Tr.t("rentalunitlist", session.getString("language") + ", "));
								if(null != zlIndex)
								{
									if(zlIndex.intValue() == index.intValue())
									{
										zlselected = true;
									}
									else
									{
										zlselected = false;
									}
								}
								else if(null != lastzlIndex)
								{
									if(lastzlIndex.intValue() == index.intValue())
									{
										zlselected = true;
									}
									else
									{
										zlselected = false;
									}
								}

								if(showMieterlisteInSeletor)
								{
									if(zlselected)
									{
										zl_sel.append("<option value=" + index + " selected>" + languageconformtext + "</option>\n");
										org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
										optionJ.put("name", languageconformtext);
										optionJ.put("value", index);
										optionJ.put("selected", true);
										zl_sel_json.add(optionJ);
									}
									else
									{
										zl_sel.append("<option value=" + index + ">" + languageconformtext + "</option>\n");
										org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
										optionJ.put("name", languageconformtext);
										optionJ.put("value", index);
										optionJ.put("selected", false);
										zl_sel_json.add(optionJ);
									}
								}
								else
								{
									if(zlselected)
									{
										zl_sel.append("<option value=" + index + " selected>" + cleantext + "</option>\n");
										org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
										optionJ.put("name", cleantext);
										optionJ.put("value", index);
										optionJ.put("selected", true);
										zl_sel_json.add(optionJ);
									}
									else
									{
										zl_sel.append("<option value=" + index + ">" + cleantext + "</option>\n");
										org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
										optionJ.put("name", cleantext);
										optionJ.put("value", index);
										optionJ.put("selected", false);
										zl_sel_json.add(optionJ);
									}

								}

								zlselected = false;
							}
							else if(text.indexOf("Eigentümerliste") >= 0)
							{
								String languageconformtext = CoolStringTool.replaceStr(text, "Eigentümerliste, ", Tr.t("ownerslist", session.getString("language") + ", "));

								if(elselected)
								{
									el_sel.append("<option value=" + index + " selected>" + text + "</option>\n");
									org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
									optionJ.put("name", text);
									optionJ.put("value", index);
									optionJ.put("selected", true);
									el_sel_json.add(optionJ);
								}
								else
								{
									el_sel.append("<option value=" + index + ">" + languageconformtext + "</option>\n");
									org.json.simple.JSONObject optionJ = new org.json.simple.JSONObject();
									optionJ.put("name", languageconformtext);
									optionJ.put("value", index);
									optionJ.put("selected", false);
									el_sel_json.add(optionJ);
								}

								if(null != zleIndex)
								{
									if(zleIndex.intValue() == index.intValue())
									{
										elselected = true;
									}
									else
									{
										elselected = false;
									}
								}
							}
							else
							{
								rest_sel.append("<option value=" + index + ">" + text + "</option>\n");
							}
						}
						el_sel.append("<option value=\"\">" + Tr.t("textNone", session.getString("language")) + "</option>\n");
						zl_sel.append(rest_sel.toString());
						set("var.zlselector", zl_sel.toString());
						set("var.elselector", el_sel.toString());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + "." + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createRentRollObjectSelectDgd(zl_sel_json);
							String response = createJsonResponseWithCustomData(getId(), jsonRes, "RENTROLLSELECT", getTemplateType());
							return new ParseResult(response, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log2: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// ---------------------------------------------
				// ZINSLISTE HOLEN
				// ---------------------------------------------

				// Hashtable pro liste mit text String, index Integer

				if(sapconnection.equals("1") && (zlfile == null || zlfile.length() == 0))
				{
					zl = getZinsliste(file, zlIndex.intValue(), quellsystemResult, quellsystem);

					if(enableDetailedLogging)
					{
						endtime = System.currentTimeMillis();
						BugMe.getInstance().log("############ Log3: " + ((endtime - starttime) / 1000) + " seconds");
						starttime = System.currentTimeMillis();
					}
				}
				else
				{
					zl = getZinsliste(file, zlIndex.intValue());
					Vector<Hashtable<String, String>> mappingchangesV = getMappingChangesVector();
					System.out.println("Zinsliste vor Usermanipulation");
					System.out.println(zl.toString());
					if(mappingchangesV.size() > 0)
					{
						zl.setzeZinszeilenKorrekturen(mappingchangesV);
					}
					System.out.println("Zinsliste nach Usermanipulation");
					System.out.println(zl.toString());
					System.out.println("Zinsliste ready?");
				}

				updateProgess(new BigDecimal(0.2), "Liste eingelesen", null);

				// schwerwiegendes problem???
				if(null == zl.adresse)
				{
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("textCantReadRentRoll", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")) + "<br/>" + zl.getErrorsInHTML("", session));
					shortinfo = zl.getShortInfos();
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.resultcode", zlprotocol.getHtmlRes());
					set("var.errorcode", zlprotocol.getHtmlErr());
					set("var.errorcodecsv", zl.getErrorsInCSV("", session));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");

					String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
					String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
					updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + "." + zlprotocol.getHtmlRes(), link, rrplainlink, last_oid_haus);

					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();

					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						String jsonRes = createZZJsonReply("error", jerr, null, null);
						return new ParseResult(jsonRes, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				// für Liste durchgeführter Importe,
				String jahr = zl.getJahr();
				String monat = zl.getMonat();
				String land = zl.getLand();
				if(land == null || land.equals(""))
				{
					land = "AT";
				}
				String ort = zl.getOrt();
				String adresse = zl.getAdresse();
				if(this.getBoolean("var.sendmailankundenbetreuer", Boolean.FALSE))
				{
					try
					{
						String emailkundenbetreuer = zl.getAdditionalFields().get("kundenbetreueremail");
						if(null != emailkundenbetreuer && emailkundenbetreuer.trim().length() > 0)
						{
							this.set("var.emailkundenbetreuer", emailkundenbetreuer);
						}
						else
						{
							debug.error(this, "UploadXLS4 es gibt keine kundenbetreuer email " + emailkundenbetreuer);

						}
					}
					catch(Exception ex)
					{
						debug.error("UploadXLS4 es gibt keine kundenbetreuer email");
						debug.error(ex);
					}
				}
				this.set("var.jahr", jahr);
				this.set("var.monat", monat);
				this.set("var.land", land);
				this.set("var.ort", ort);
				this.set("var.adresse", adresse);
				this.set("var.hausverwaltung", hausverwaltung);
				this.set("var.filename", lockname);
				this.set("var.sapconnection", "");
				try
				{

					if(sapconnection.equals("1"))
					{
						if(zlfile == null || zlfile.length() == 0)
						{
							String tmpDirectory = (String)CfgSingleton.getInstance().get("UPLOAD_FILE_DIR");
							String filetype = "csv";

							if(file.length() == 0)
							{
								String content = "";

								content = QueryHelper.getCSVFromVector(quellsystemResult, quellsystem);

								String actualTime = new SimpleDateFormat("_yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
								String outfilename = quellsystem + "_file_zinslistenimport" + actualTime + ".csv";

								FileWriter writer = new FileWriter(tmpDirectory + System.getProperty("file.separator") + outfilename);
								writer.append(content);
								writer.flush();
								writer.close();

								file = outfilename;
							}

							Path path = Path.of(tmpDirectory + System.getProperty("file.separator") + file);
							byte[] data = Files.readAllBytes(path);

							Hashtable fparams = new Hashtable();
							fparams.put("size", "" + data.length);
							fparams.put("paramname", "zinslistenfile");
							fparams.put("name", file);
							fparams.put("type", filetype);
							fparams.put("Content-Type", "application/txt");
							fparams.put("OID", oidnew);

							if(FDAInst == null)
							{
								Connector conn = null;
								conn = new Connector();
								FDAInst = conn.getFileDataAgent();
							}

							// Create a unique file reference
							Long ctr = CoolDataTool.generateUniqueSequence(file);
							String filereferencename = file.substring(0, file.indexOf(".csv")) + ctr + "." + filetype;
							zlfile = FDAInst.storeObject(filereferencename, data, fparams);

							if(enableDetailedLogging)
							{
								endtime = System.currentTimeMillis();
								BugMe.getInstance().log("############ Log4: " + ((endtime - starttime) / 1000) + " seconds");
								starttime = System.currentTimeMillis();
							}
						}

					}

				}
				catch(Exception ex)
				{
					debug.error(ex);
				}

				// System.err.println("ZLU2: CIMS UPLOAD READ ZL ");

				// check ob man darf
				if(frozenyear > Integer.parseInt(zl.jahr) || frozenyear == Integer.parseInt(zl.jahr) && frozenmonth >= Integer.parseInt(zl.monat))
				{
					// NEIN MAN DARF NICHT !!!
					// GAR NICHT GUT
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("textRentRollTooOld", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")));
					shortinfo = zl.getShortInfos();

					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.resultcode", zlprotocol.getHtmlRes());
					set("var.errorcode", zlprotocol.getHtmlErr());
					set("var.errorcodetxt", zlprotocol.getTxtErr());
					set("var.errorcodecsv", zl.getErrorsInCSV("", session));
					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();

					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						String jsonRes = createZZJsonReply("error", jerr, null, null);
						return new ParseResult(jsonRes, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}

				this.set("var.objektname", zl.haus);

				// GIBT'S DAS HAUS?
				// sinnvolle adresse????
				oid_haus = topotool.getHausOID(zl);

				Boolean importsperrebeidatenfreigabe = this.getBoolean("var.importsperrebeidatenfreigabe", Boolean.FALSE);
				if(importsperrebeidatenfreigabe)
				{
					Boolean importAllowed = getStatusOfFreigabe(oid_haus, zl);

					if(!importAllowed)
					{
						// set error and return parseresult
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textRentRollApproved", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();

						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");
						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("warning", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}
				}

				if(oid_haus != null && oid_haus.length() > 0)
				{
					getHausverwaltungFromHausOid(oid_haus);
				}

				if(oid_haus == null && zinslistenImport.getZlTypeConfig().isCreatenewpropertiesautomatically())
				{
					try
					{
						TemplateReader tr = TemplateReader.getInstance();
						DynGenDataObj dgdHaus = tr.getDGDForTemplate("CIMS.haus", global, session);

						dgdHaus.set("var.name", zl.getAdresse());
						dgdHaus.set("var.plz", zl.getPlz());
						dgdHaus.set("var.ort", zl.getOrt());
						dgdHaus.set("var.identadresse5", zl.getEdvNr());
						dgdHaus.set("var.land", zl.getLand());

						// Values from ICRSConfig
						dgdHaus.set("slot.gschaft", ICRSConfig.getInstance(session).get("slot.gschaft"));

						dgdHaus.set("slot.gfeld", ICRSConfig.getInstance(session).get("slot.gfeld"));
						dgdHaus.set("slot.assetmanager", ICRSConfig.getInstance(session).get("slot.assetmanager"));
						dgdHaus.set("slot.hausverwaltungneu", ICRSConfig.getInstance(session).get("slot.hausverwaltungneu"));

						dgdHaus.set("var.status", ICRSConfig.getInstance(session).get("var.status"));
						dgdHaus.set("var.hausnutzung", ICRSConfig.getInstance(session).get("var.hausnutzung"));

						oid_haus = DAInst.storeObject(dgdHaus, dgdHaus.getTemplateType(), null, session);

						String edvnummer = "";
						if(zl.getEdvNr().length() > 0 && zl.getHausverwaltung().length() > 0)
						{
							edvnummer = "|" + zl.getHausverwaltung() + "" + zl.getEdvNr() + "|";
						}

						String adressemitedv = (zl.getAdresse() + " " + edvnummer).trim();

						String hauslink = DynGenDataObj.createLink(oid_haus, "Link", session);
						zlprotocol.appendMailMsg((zl.getEdvNr() + " " + adressemitedv + " " + zl.getPlz() + " " + hauslink + " " + zl.getOrt() + " " + zl.getLand()).trim() + " " + Tr.t("textObjectCreated", session.getString("language")) + "<br>\n");

						zlprotocol.addCsvLine(new String[]{
							zl.getEdvNr() + " " + adressemitedv,
							zl.getPlz(),
							"",
							Tr.t("textObjectCreated", session.getString("language")),
							"",
							""});

						// Also do Googlemaps Query
						GoogleMapsQuery gmq = new GoogleMapsQuery(session);
						gmq.updateLageplaeneAndGeoData(null, oid_haus);
					}
					catch(Exception ex)
					{
						debug.error(ex);
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log5: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				last_oid_haus = oid_haus;
				// System.err.println("ZLU2: haus id " + oid_haus);

				String ignoreerrors = "";
				boolean isoldlist = false;
				if(null != oid_haus)
				{
					// check auf berechtigung !!!
					String origuserid = getString("var.origuserid").trim();
					if(origuserid.matches("\\d+") && CfgSingleton.getInstance().hasIcrsAccessCreateSpvGroups())
					{
						// change session
						Login login = new Login();
						session = login.getUserSession(origuserid, global, null);
					}

					if(!topotool.mayWriteHaus(oid_haus, session))
					{
						// GAR NICHT GUT
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorMasterData", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoPermissionImportObject", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();

						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));

						log("zinslistenimport - keine berechtigung fuer haus " + zl.haus + " " + zl.plz + " " + zl.ort + " " + zl.adresse + " " + zl.monat + " " + zl.jahr + " " + zl.typ);

						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_ERROR, "RRImport file " + file + ". No write access for " + zl.haus + " " + zl.plz + " " + zl.ort + " " + zl.adresse + " " + zl.monat + " " + zl.jahr + " " + zl.typ, link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("error", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}

					if(getBoolean("var.checkexistingrentroll", Boolean.FALSE))
					{
						int zzanz = tq.countZinszeilen(oid_haus, jahr, monat);
						if(zzanz > 0)
						{
							zl.error("", Tr.t("textExistingRentroll", session.getString("language")), Tr.t("textExistingRentrollInfo", session.getString("language"), monat, jahr), ErrorInfo.MITTEL, ErrorInfo.EINTRAGSFEHLER);
						}
					}

					ignoreerrors = getIgnoreErrorsForHaus(oid_haus);
					if(ignoreerrors.length() > 0)
					{
						if(ignorefutureerrors.length() > 0)
						{
							ignoreerrors = ignoreerrors + "\n" + ignorefutureerrors;
						}
						ignoreerrors = CoolStringTool.replaceStr(ignoreerrors, "____", "\n");
					}
					else if(ignorefutureerrors.length() > 0)
					{
						ignoreerrors = ignorefutureerrors;
					}
					// write it to the house!!!!
					if(ignorefutureerrors.length() > 0)
					{
						ignoreerrors = writeIgnoreErrorsForHaus(oid_haus, ignoreerrors);
					}

					// RK20110125
					// in der Methode sollten generelle Hausüberprüfungen landen um das besser zu sammeln
					zl = checkHausStatus(oid_haus, zl, zinslistenImport.getZlTypeConfig());

					// Aktuelles ZZ Datum am Haus setzten ansonst verwendet das Syncronize das vorherige Datum und aktualisiert beim Store vom top auch die aktuelle Zinszeile
					try
					{
						if(null == DAInst)
						{
							net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
							DAInst = conn.getDataAgent();
						}
						DynGenDataObj hausDGD = (DynGenDataObj)DAInst.getObject(oid_haus, null);
						Date lastimportHaus = hausDGD.getDate("var.lastimport");

						Calendar cal = GregorianCalendar.getInstance();
						cal.set(Calendar.MONTH, Integer.parseInt(zl.monat) - 1);
						cal.set(Calendar.YEAR, Integer.parseInt(zl.jahr));
						cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
						Date actualZiliimport = cal.getTime();

						if(null == lastimportHaus || actualZiliimport.after(lastimportHaus))
						{
							String actualZiliimportDateStr = net.metamagix.essence.eSSENCETypes.eDate.stringFromDate(actualZiliimport);

							hausDGD.set("var.runningimport", actualZiliimportDateStr);
							hausDGD.set("dirty", "yes");
							String id = DAInst.storeObject(hausDGD, hausDGD.getTemplateType(), oid_haus, session);
						}
					}
					catch(Exception ex)
					{
						debug.error("Error in setting Importdate on CIMS.haus in UploadXLS4:");
						debug.log(ex);
					}

					isoldlist = zl.isOldList();
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log6: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// aendert den status !
				zl.ignoreErrors(ignoreerrors);

				// adresse korrigieren
				zl.haus = TopoTool.fixeAdresse(zl.haus);

				// log, wenn ein user zum fehlerignorieren aufruft!
				if(ignoreerrors.length() > 0)
				{
					log(" folgende fehler sind bei haus " + zl.haus + " in zukunft zu ignorieren:" + ignoreerrors);
				}

				Hashtable allmytops = new Hashtable();
				Hashtable allmytopsInfoHash = new Hashtable();
				// mit bereinigten namen !!!
				Hashtable allmyinternaltops = new Hashtable();

				if(null != oid_haus)
				{
					allmytops = tq.getTopsForOID(oid_haus, null, session, DAInst);
					allmytopsInfoHash = tq.getTopsInfosForHausOID(oid_haus, null, session, DAInst);
					// mit bereinigten namen !!!
					allmyinternaltops = TopoTool.getInternalTopsForTops(allmytops);
					TopList top_list = new TopList(session, global, DAInst, oid_haus, false);
					// prefetch tops !!!
					fillTopCache(top_list);
					// prefetch last CIMS.zinszeile for each top
					fillLastZZ4Top(oid_haus);

					Hashtable alltopsmerged = new Hashtable();
					alltopsmerged.putAll(allmytops);
					alltopsmerged.putAll(allmyinternaltops);

					// ?????????????????????????????????????????
					// precheck veraenderungen
					// change top check
					if(wertaenderung.equals("1"))
					{
						zl.vergleicheMitTops(topsCache, alltopsmerged, lastZZ4Top, null, ses, oid_haus, zinslistenImport);
					}
					else
					{
						zl.vergleicheMitTops(topsCache, alltopsmerged, lastZZ4Top, zlprotocol, ses, oid_haus, zinslistenImport);
					}

				}

				// Ueberpruefung ob Zinsliste aus dem Vormonat eingespielt wurde - PKO 12.03.2012
				// System.out.println("ZL-Datum: " + zl.monat + " / " + zl.jahr);
				if(zl.monthlyRentRoll())
				{ // haben wir den Vormonat?
					Vector res = tq.getZinslistenMonateForHaus(oid_haus);
					if(res != null && res.size() > 0)
					{
						Hashtable zldate = (Hashtable)res.get(0);

						int m1 = 0;
						int m2 = 0;
						try
						{
							m1 = Integer.parseInt(zldate.get("jahr").toString()) * 12 + Integer.parseInt(zldate.get("monat").toString());
						}
						catch(Exception e)
						{
							debug.error(this, "bad values for jahr " + zldate.get("jahr") + " and monat " + zldate.get("monat") + ".");
						}

						try
						{
							m2 = Integer.parseInt(zl.jahr.toString()) * 12 + Integer.parseInt(zl.monat.toString());
						}
						catch(Exception e)
						{
							debug.error(this, "bad values for jahr " + zl.jahr + " and monat " + zl.monat + ".");

						}

						int result = m2 - m1;
						// System.out.println("Month Difference: " + result);

						if(result > 1)
						{
							zl.addError(Tr.t("textLastRentRoll", session.getString("language")) + " " + zldate.get("monat").toString() + "/" + zldate.get("jahr").toString() + " " + Tr.t("textCurrentRentRoll", session.getString("language")) + " " + zl.monat.toString() + "/" + zl.jahr.toString(), "", ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, "");
						}
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log7: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// Ticket 17995
				// alternatives=0|1|2][textalternatives=lt. Zinslistenconfig|Ja|Nein
				String ignorealleasyerros = this.getString("var.ignorealleasyerros");

				// LEICHTER FEHLER
				if(zl.status == 1 && (!zinslistenImport.getZlTypeConfig().isIgnorealleasyerros() || ignorealleasyerros.equals("2")))
				{
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorInRentRoll", session.getString("language")) + "</h2><br />" + zl.getErrorsInHTML(ignoreerrors, session));
					zlprotocol.appendTxtErr(Tr.t("textErrorInRentRoll", session.getString("language")) + "<br/>" + zl.getErrors() + "<br/>" + zl.getBaseInfos());
					shortinfo = zl.getShortInfos();

					if(zl.getErrorsInHTML(ignoreerrors, session).length() > 0)
					{
						fehlerabfrage = true;
					}
				}
				// SCHWERER FEHLER
				// RK20110125 ==2 auf >=2 gesetzt Error 3 ist grober Fehler!
				else if(zl.status >= 2)
				{
					// CHECKT AUCH GLEICH, OB USER ERRORS SCHON MAL UNWICHTIG GEZEICHNET HAT
					zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorMasterData", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")) + "<br/><h2>" + Tr.t("textError", session.getString("language")) + "</h2><br/>" + zl.getErrorsInHTML(ignoreerrors, session));
					zlprotocol.appendTxtErr(Tr.t("textErrorMasterData", session.getString("language")) + "\n" + Tr.t("textNoImport", session.getString("language")) + "\n" + zl.getBaseInfos() + "\n" + zl.getErrors());

					shortinfo = zl.getShortInfos();
					session.set("CURRENT_VIEW", "ERRORQUEST");
					set("var.resultcode", zlprotocol.getHtmlRes());
					set("var.errorcode", zlprotocol.getHtmlErr());
					set("var.errorcodetxt", zlprotocol.getTxtErr());
					set("var.errorcodecsv", zl.getErrorsInCSV("", session));
					set("var.zlstatus", "2");

					this.set("dirty", "yes");
					session.set("TRANSACTIONTRIGGER", "");
					// Setzt den aktuellen Status des Zinslistenimports
					setImportStatus("3");
					if(null == oid_haus)
					{
						set("var.importstop", "1");
					}

					String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
					String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
					updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_ERROR, "RRImport file " + file + ". " + Tr.t("textErrorMasterData", session.getString("language")) + " " + Tr.t("textNoImport", session.getString("language")) + " " + zl.getBaseInfos() + " " + zl.getErrors(), link, rrplainlink, last_oid_haus);

					LockingSingleton.getInstance().leave(lockname);

					updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
					pp.deregisterProcess();
					if(isJSON)
					{
						String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
						String jsonRes = createZZJsonReply("error", jerr, null, null);
						return new ParseResult(jsonRes, 0, "", ses);
					}
					return super.parse(templatecode, glo, ses);
				}
				// -----------------------------------
				// EIGENTUEMERLISTE
				// -----------------------------------
				// haben wir eine aus mehreren ausgewaehlt?
				// System.err.println("ZLU2: CIMS UPLOAD READ EL?");
				if(!zlfile_e.equals("") || eigentuemerlistenindex.length() > 0)
				{
					if(eigentuemerlistenindex.length() > 0)
					{
						// System.err.println("ZLU2: XLSUPLOAD " + zleIndex.intValue() + ": eigentuemerliste");
						zle = getZinsliste(file, zleIndex.intValue());
						// adresse korrigieren
						zle.haus = TopoTool.fixeAdresse(zle.haus);
					}
					else
					{
						zle = getZinsliste(efile, 0);
						// aendert den status !
						zle.ignoreErrors(ignoreerrors);
						// adresse korrigieren
						zle.haus = TopoTool.fixeAdresse(zle.haus);
					}

					// check ob man darf
					if(frozenyear > Integer.parseInt(zl.jahr) || frozenyear == Integer.parseInt(zl.jahr) && frozenmonth >= Integer.parseInt(zl.monat))
					{
						// NEIN MAN DARF NICHT !!!
						// GAR NICHT GUT
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textOwnerList", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zl.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();
						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("error", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}

					// haben wir die noetigen Stammdaten?

					if(zle.status == 1 && (!zinslistenImport.getZlTypeConfig().isIgnorealleasyerros() || ignorealleasyerros.equals("2")))
					{
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorOwnerList", session.getString("language")) + "</h2><br>" + zle.getErrorsInHTML(ignoreerrors));
						zlprotocol.appendTxtErr(Tr.t("textErrorOwnerList", session.getString("language")) + "\n" + zle.getErrors() + "\n" + zle.getBaseInfos());
						zlprotocol.appendHtmlRes(zle.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();
						if(zle.getErrorsInHTML(ignoreerrors, session).length() > 0)
						{
							fehlerabfrage = true;
						}
					}
					else if(zle.status == 2)
					{
						// GAR NICHT GUT
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorMasterDataOwnerList", session.getString("language")) + "</h2><br/><h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2><br/>" + zle.getBaseInfosInHTML(session.getString("language")) + "<br/><h2>" + Tr.t("textError", session.getString("language")) + "</h2><br/>" + zle.getErrorsInHTML(ignoreerrors, session));
						zlprotocol.appendTxtErr(Tr.t("textErrorMasterData", session.getString("language")) + "\n" + Tr.t("textNoImport", session.getString("language")) + "\n" + zle.getBaseInfos() + "\n" + zle.getErrors());
						shortinfo = zl.getShortInfos();

						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zle.getErrorsInCSV("", session));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + Tr.t("textErrorMasterData", session.getString("language")) + " " + Tr.t("textNoImport", session.getString("language")) + "\n" + zle.getBaseInfos() + " " + zle.getErrors(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("error", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}

					// -----------------------------------
					// MERGE ZINSLISTE
					// -----------------------------------
					// EIGENTUEMER MERGEN
					// System.err.println("ZLU2: Upload Config File "+cfg_zlimport);
					ZinslistenImport zli = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
					zli.setLanguage(session.getString("language"));
					zli.setEvaluateFormulas(evaluateFormulas);
					// System.err.println("ZLU2: CIMS UPLOAD MERGING EIGENTUEMER+MIETER");
					zl = zli.mergeZinslisten(zl, zle);
					// aendert den status !
					zl.ignoreErrors(ignoreerrors);
					// haben wir die noetigen Stammdaten?

					// LEICHTER FEHLER
					if(zl.status == 1 && (!zinslistenImport.getZlTypeConfig().isIgnorealleasyerros() || ignorealleasyerros.equals("2")))
					{
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorCombinedRentRoll", session.getString("language")) + "</h2><br>");
						String errorsIH = zl.getErrorsInHTML(ignoreerrors, session);
						zlprotocol.appendHtmlErr(errorsIH);
						zlprotocol.appendTxtErr(Tr.t("textErrorCombinedRentRoll", session.getString("language")) + "\n");
						zlprotocol.appendTxtErr(zl.getErrors());
						zlprotocol.appendTxtErr(zl.getBaseInfos());
						shortinfo = zl.getShortInfos();
						if(errorsIH.length() > 0)
						{
							fehlerabfrage = true;
						}
					}
					// SCHWERER FEHLER
					if(zl.status == 2)
					{
						// GAR NICHT GUT
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textErrorCommonMasterDataOwnerList", session.getString("language")) + "</h2>");
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
						zlprotocol.appendHtmlErr(zl.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();
						zlprotocol.appendHtmlErr("<h2>" + Tr.t("textError", session.getString("language")) + "</h2>");
						zlprotocol.appendHtmlErr(zl.getErrorsInHTML(ignoreerrors, session));

						zlprotocol.appendTxtErr(Tr.t("textErrorMasterData", session.getString("language")) + "\n");
						zlprotocol.appendTxtErr(Tr.t("textNoImport", session.getString("language")) + "\n");
						zlprotocol.appendTxtErr(zl.getBaseInfos());
						zlprotocol.appendTxtErr(zl.getErrors());
						session.set("CURRENT_VIEW", "ERRORQUEST");
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));
						set("var.zlstatus", "2");

						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("error", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log8: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// IGNORIERT DER USER LEICHTE FEHLER
				if(fehlerabfrage)
				{
					// Create Mietvertragsverknuepfung
					if(oid_haus != null && oid_haus.length() > 0)
					{
						TopList top_list = new TopList(session, global, DAInst, oid_haus, false);
						set("var.toplistjson", top_list.toJSON(ses));
						if(getBoolean("var.topmatcherselector", Boolean.TRUE))
						{
							zlprotocol.appendHtmlRes(getJavascriptTopmatcherString(top_list));
						}
						zl = createVerknuepfungZuMietvertrag(top_list, zl, true);
					}
					String fabfrage = (String)this.get("arg.fehlerabfrage");

					if(fabfrage == null || fabfrage.length() == 0)
					{
						fabfrage = (String)session.get("arg.oid" + this.volatile_id + ".fehlerabfrage");
					}
					if(fabfrage == null || fabfrage.length() == 0)
					{
						fabfrage = session.getString("arg.fehlerabfrage").trim();
					}

					if(enableDetailedLogging)
					{
						endtime = System.currentTimeMillis();
						BugMe.getInstance().log("############ Log9: " + ((endtime - starttime) / 1000) + " seconds");
						starttime = System.currentTimeMillis();
					}

					if(!fabfrage.equals("1"))
					{
						session.set("CURRENT_VIEW", "ERRORQUEST");

						zlprotocol.clearHtmlErr();
						String indextemplate = getString("var.indextemplate");
						String haustargetview = getString("var.haustargetview");
						String targetstring = "";

						boolean openhausinnewtab = getBoolean("var.openhausinnewtab", Boolean.FALSE);

						if(haustargetview.trim().length() == 0)
						{
							haustargetview = "SHOW";
						}
						if(indextemplate.trim().length() == 0)
						{
							indextemplate = "CIMS.index";
						}

						String myurl = dynurl + "?OID=" + oid_haus + "&VIEW=" + haustargetview + "&ESSENCEID=" + sessid;

						if(redirectobj.length() > 0)
						{
							myurl = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl, StandardCharsets.UTF_8) + "&ESSENCEID=" + sessid;
						}

						if(openhausinnewtab)
						{
							myurl = URLDecoder.decode(myurl, "UTF-8");
							if(myurl.indexOf("contenturl=") >= 0)
							{
								myurl = myurl.split("contenturl=")[1];
							}
							String encodedurl = URLEncoder.encode(myurl, "UTF-8");
							targetstring = " target='_blank'";
							myurl = dynurl + "?OID=DIRECT_" + indextemplate + "&contenturl=" + encodedurl;
						}
						zlprotocol.appendHtmlRes("\n\n<h2 id='hausInfoHeader'>" + Tr.t("textObject", session.getString("language")) + " <a  class='ajaxLink redlink'" + targetstring + " href=\"" + myurl + "\">" + zl.haus + "</a></h2>");

						zlprotocol.appendHtmlErr(zl.getErrorsInHTML(ignoreerrors, session));

						// hier die postenzuordnung adden
						zlprotocol.appendHtmlErr(zl.getPostenZuordnungDropdown());

						String rutablename = "datatableicrs";
						zlprotocol.appendHtmlRes("<h2 id='" + rutablename + "_h2label'>" + Tr.t("textRentalUnits", session.getString("language")) + "</h2>");
						zlprotocol.appendHtmlRes(zl.getZinszeilenInHTML(session, rutablename));
						zlprotocol.appendHtmlRes("<br />\n");
						String pstablename = "datatableicrsstpl";
						zlprotocol.appendHtmlRes("<h2 id='" + pstablename + "_h2label'>" + Tr.t("textParkingSpaces", session.getString("language")) + "</h2>");
						zlprotocol.appendHtmlRes(zl.getStellplaetzeInHTML(session, pstablename));

						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));
						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jsonRes = createRentRollImportErrorDgd(zl, ignoreerrors, rutablename, pstablename);
							String response = createJsonResponseWithCustomData(getId(), jsonRes, "RENTROLLSELECT", getTemplateType());
							return new ParseResult(response, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
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

					log("zinslistenimport trotz leichter fehler." + zl.haus + " " + zl.plz + " " + zl.ort + " " + zl.adresse + " " + zl.monat + " " + zl.jahr + " " + zl.typ + infoplus);
				}

				// HAUS EXISTIERT NICHT FRAGEN!!!!
				if(null == oid_haus)
				{
					String maycreatehaus = (String)this.get("var.maycreatehaus");
					// muß explizit auf 0 gesetzt werden, damit das Haus nicht angelegt werden kann
					if(maycreatehaus == null)
					{
						maycreatehaus = "1";
					}

					if(!maycreatehaus.equals("0"))
					{
						String createhaus = (String)this.get("arg.createhaus");
						if(createhaus == null || createhaus.length() == 0)
						{
							createhaus = (String)session.get("arg.oid" + this.volatile_id + ".createhaus");
						}
						if(null == createhaus)
						{
							createhaus = "";
						}
						if(!createhaus.equals("1"))
						{
							// soll es angelegt werden ???
							session.set("CURRENT_VIEW", "NEWOBJECT");
							zlprotocol.clearHtmlRes();
							zlprotocol.appendHtmlRes(zl.getBaseInfosInHTML(session.getString("language")));
							shortinfo = zl.getShortInfos();
							set("var.resultcode", zlprotocol.getHtmlRes());
							set("var.errorcode", zlprotocol.getHtmlErr());
							set("var.errorcodetxt", zlprotocol.getTxtErr());
							set("var.errorcodecsv", zl.getErrorsInCSV("", session));

							set("var.land", zl.getLand());
							set("var.ort", zl.getOrt());
							set("var.adresse", zl.getAdresse());
							set("var.plz", zl.getPlz());
							set("var.hausverwaltung", zl.getTyp());
							set("var.hausverwalter", zl.getHausverwalter());

							if(zl.getEdvNr() != null && zl.getHausverwaltung() != null && zl.getHausverwaltung().length() > 2)
							{
								String edvnummer = zl.getHausverwaltung().substring(0, 3) + zl.getEdvNr();
								set("var.identadresse5", edvnummer);
							}

							this.set("dirty", "yes");
							session.set("TRANSACTIONTRIGGER", "");

							String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
							String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
							updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

							// Setzt den aktuellen Status des Zinslistenimports
							setImportStatus("3");

							LockingSingleton.getInstance().leave(lockname);

							updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
							pp.deregisterProcess();
							if(isJSON)
							{
								String jsonRes = createRentRollNewObjectDgd(zl, ignoreerrors);
								String response = createJsonResponseWithCustomData(getId(), jsonRes, "NEWOBJECT", getTemplateType());
								if(StringUtils.isNotBlank(response)) return new ParseResult(response, 0, "", ses);
							}
							return super.parse(templatecode, glo, ses);
						}
						oid_haus = createHaus(zl);
						last_oid_haus = oid_haus;
						if(null != oid_haus)
						{
							zlprotocol.appendHtmlRes("<h2>" + Tr.t("textObjectCreated", session.getString("language")) + "</h2>");
							String infoplus = "";
							if(zl.isModifySollMiete())
							{
								infoplus += ", mit Hebung Sollhauptmietzins ";
							}
							if(zl.isModifyZielMiete())
							{
								infoplus += ", mit Hebung Zielhauptmietzins ";
							}

							log("zinslistenimport trotz leichter fehler." + zl.haus + " " + zl.plz + " " + zl.ort + " neu angelegt" + infoplus);
						}
						else
						{
							zlprotocol.appendHtmlRes("<h2>" + Tr.t("textObjectNotCreated", session.getString("language")) + "</h2>");
						}
					}
					else
					{
						// soll es angelegt werden ???
						session.set("CURRENT_VIEW", "INFO");
						zlprotocol.clearHtmlRes();
						zlprotocol.appendHtmlRes(zl.getBaseInfosInHTML(session.getString("language")));
						zlprotocol.appendHtmlRes("<h1>" + Tr.t("textPropertyMustBeCreatedBefore", session.getString("language")) + "</h1>");
						shortinfo = zl.getShortInfos();
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));

						set("var.land", zl.getLand());
						set("var.ort", zl.getOrt());
						set("var.adresse", zl.getAdresse());
						set("var.plz", zl.getPlz());
						set("var.hausverwaltung", zl.getTyp());
						set("var.hausverwalter", zl.getHausverwalter());

						this.set("dirty", "yes");
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("info", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log10: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				String indextemplate = getString("var.indextemplate");
				String haustargetview = getString("var.haustargetview");
				String targetstring = "";

				boolean openhausinnewtab = getBoolean("var.openhausinnewtab", Boolean.FALSE);

				if(haustargetview.trim().length() == 0)
				{
					haustargetview = "SHOW";
				}
				if(indextemplate.trim().length() == 0)
				{
					indextemplate = "CIMS.index";
				}

				String myurl = dynurl + "?OID=" + oid_haus + "&VIEW=" + haustargetview + "&ESSENCEID=" + sessid;

				if(redirectobj.length() > 0)
				{
					myurl = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl, StandardCharsets.UTF_8) + "&ESSENCEID=" + sessid;
				}

				if(openhausinnewtab)
				{
					myurl = URLDecoder.decode(myurl, "UTF-8");

					if(myurl.indexOf("contenturl=") >= 0)
					{
						myurl = myurl.split("contenturl=")[1];
					}
					String encodedurl = URLEncoder.encode(myurl, "UTF-8");
					targetstring = " target='_blank'";
					myurl = dynurl + "?OID=DIRECT_" + indextemplate + "&contenturl=" + encodedurl;
				}

				zlprotocol.appendHtmlRes("\n\n<h2 id='hausInfoHeader'>" + Tr.t("textObject", session.getString("language")) + " <a  class='ajaxLink redlink'" + targetstring + " href=\"" + myurl + "\">" + zl.haus + "</a></h2>");
				zlprotocol.appendHtmlRes("<div id='hausInfoField' style='display:none;'>");
				zlprotocol.appendHtmlRes(zl.getBaseInfosInHTML(session.getString("language")));
				shortinfo = zl.getShortInfos();
				zlprotocol.appendHtmlRes("</div>");

				zlprotocol.appendHtmlRes("<h2 id='zinszeilenInfoHeader'>" + Tr.t("textRentRolls", session.getString("language")) + "</h2>");
				zlprotocol.appendHtmlRes("<div id='zinszeilenInfoField' style='display:none;'>");
				// --------------------------------------------
				// ZINSZEILENCHECK
				// --------------------------------------------
				boolean createtops = false;

				Hashtable tops_in_zl = new Hashtable();
				Hashtable<String, DynGenDataObj> topsZumUpdate = new Hashtable<String, DynGenDataObj>();
				Hashtable<String, DynGenDataObj> stellplaetzeZumUpdate = new Hashtable<String, DynGenDataObj>();

				// CREATE NEW TOPS
				String createnewtops = (String)this.get("arg.createnewtops");

				if(createnewtops == null || createnewtops.length() == 0)
				{
					createnewtops = (String)session.get("arg.oid" + this.volatile_id + ".createnewtops");
				}
				if(createnewtops == null || createnewtops.length() == 0)
				{
					createnewtops = (String)session.get("arg" + this.volatile_id + ".createnewtops");
				}
				if(createnewtops == null || createnewtops.length() == 0)
				{
					createnewtops = (String)session.get("arg.createnewtops");
				}

				if(null == createnewtops)
				{
					createnewtops = "";
				}
				if(createnewtops.equals(""))
				{
					createnewtops = "-1";
				}

				if(zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())
				{
					createnewtops = "1";
				}
				boolean zusammenlegenautomatisch = zinslistenImport.getZlTypeConfig().isSettopautomatischaufzusammengelegtOrcreatenewtopsautomatically();

				TopList top_list = new TopList(session, global, DAInst, oid_haus, false);
				System.err.println("ZU2: Topliste mit " + top_list.size() + " Elementen ...");
				set("var.toplistjson", top_list.toJSON(ses));
				// top_list.print();

				// check verkaufsdatum

				// noch nicht entschieden!!!!
				if(createnewtops.equals("-1"))// ||zusammenlegenautomatisch)
				{
					StringBuffer ct = new StringBuffer();

					StringBuffer sballe = new StringBuffer();

					int count_unbekannte_tops = 0;
					int count_verkaufte_tops = 0;
					int count_zusammengelegte_tops = 0;
					String sapnummer = "";

					double overallcalculation = 0.2;
					for(int j = 0; j < zl.zinszeilen.size(); j++)
					{
						double calculation = (Double.parseDouble(String.valueOf(j)) / Double.parseDouble(String.valueOf(zl.zinszeilen.size()))) * 0.2;
						updateProgess(new BigDecimal(overallcalculation + calculation), "Verarbeitung Einheiten " + (j + 1) + "/" + zl.zinszeilen.size(), null);

						Hashtable ht = (Hashtable)zl.zinszeilen.get(j);
						String top = (String)ht.get("top");

						String zzInfo = getTopInfoStringFromZZHT(ht);

						TopElement te = null;
						// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
						if(te == null && ht.containsKey("sapnummer"))
						{
							sapnummer = (String)ht.get("sapnummer");
							te = top_list.getTopBySapnummer(sapnummer);
						}

						// try fallback to topname
						if(te == null && top != null && top.length() > 0)
						{
							te = top_list.getTop(top);
						}

						// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
						if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String toptmp = top.substring(0, top.indexOf(" |"));
							te = top_list.getTop(toptmp);
						}

						if(te != null)
						{
							boolean updateTop = false;

							te.setName(top.trim());
							te.setInternalname(TopoTool.unifyTop(top).trim());

							// update Topname
							if(null == DAInst)
							{
								net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
								DAInst = conn.getDataAgent();
							}
							DynGenDataObj topDgd = (DynGenDataObj)topsCache.get(te.getId());
							// FALLBACK
							if(null == topDgd)
							{
								log("top " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " nicht im cache.");
								topDgd = (DynGenDataObj)DAInst.getObject(te.getId(), "CIMS.top");
							}

							boolean topnamenneusetzten = getBoolean("var.topnamenneusetzten", Boolean.FALSE);
							if(ht.containsKey("sapnummer") && !topnamenneusetzten)
							{
								// PKO 20190315 - Topname nicht aendern wenn eine sapnummer in der zinslistenconfig konfiguriert ist!
								// Betrifft atm nur die WertInvest
								// Bei der ARE gibts auch eine Sapnummer -> Sollte aber keine Auswirkung haben
							}
							else if(ht.containsKey("sapnummer") && topnamenneusetzten)
							{
								String topname = te.getName().trim();
								if(topname.contains("|"))
								{
									topname = topname.substring(0, topname.indexOf("|")).trim();
								}

								topDgd.set("var.name", topname);
								updateTop = true;
							}
							else
							{
								if(!topDgd.get("var.name").equals(te.getName().trim()))
								{
									topDgd.set("var.name", te.getName().trim());
									updateTop = true;
								}
							}

							if(!topDgd.get("var.internalname").equals(te.getInternalname()))
							{
								topDgd.set("var.internalname", te.getInternalname());
								updateTop = true;
							}

							if(updateTop)
							{
								topsZumUpdate.put(te.getId(), topDgd);
							}
						}

						String utop = TopoTool.unifyTop(top);
						if(null == te)
						{
							ct.append("<input type=checkbox name=\"createtop_" + utop + "\" value=1> " + Tr.t("textUnknownRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textUnknownRentalUnit2", session.getString("language")) + " " + Tr.t("textUnknownRentalUnit3", session.getString("language")) + "<br>\n");
							count_unbekannte_tops++;
							createtops = true;
						}
						else if(te.isBought())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
						}
						// mgo 20150625 - angemietete Tops auch berücksichtigen
						else if(te.isLeased())
						{ // angemietet
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
						}
						else if(te.isSold())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textSoldRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textSoldRentalUnit2", session.getString("language")) + " " + Tr.t("textSoldRentalUnit3", session.getString("language")) + "<br>\n");
							createtops = true;
							count_verkaufte_tops++;
							zlprotocol.appendTxtErr(Tr.t("textSoldRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textSoldRentalUnit2", session.getString("language")) + " " + zl.haus + ", " + zl.plz + " " + Tr.t("textSoldRentalUnit3", session.getString("language")));

						}
						else if(te.isMerged())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							if(isoldlist)
							{
								// bei einer alten Liste setzen wir diesen status nicht um
							}
							else
							{
								ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + topStatusValues.get("-3") + " " + Tr.t("textRentalUnitExists1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textCombinedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
								createtops = true;
							}
							count_zusammengelegte_tops++;
						}
						else if(te.isPlanned())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textPlannedRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textPlannedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
							createtops = true;
						}
						else if(te.isInspected())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textInspectedRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textInspectedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
							createtops = true;
						}

					}

					overallcalculation = 0.6;

					// Update Top Names
					// Hashtable updateRes = storeObjectsJunked(topsZumUpdate, session);
					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}

					Hashtable updateRes = DAInst.storeObjects(topsZumUpdate, session);
					updateProgess(new BigDecimal(overallcalculation), "Speichern von " + zl.zinszeilen.size() + " Einheiten abgeschlossen", null);

					for(int j = 0; j < zl.stellplaetze.size(); j++)
					{
						double calculation = (Double.parseDouble(String.valueOf(j)) / Double.parseDouble(String.valueOf(zl.stellplaetze.size()))) * 0.2;
						updateProgess(new BigDecimal(overallcalculation + calculation), "Verarbeitung Stellplätze " + (j + 1) + "/" + zl.stellplaetze.size(), null);

						Hashtable ht = (Hashtable)zl.stellplaetze.get(j);
						String top = (String)ht.get("top");

						String zzInfo = getTopInfoStringFromZZHT(ht);

						TopElement te = null;
						// EDVNr. Hausverwaltung bzw. SAPNummer (Are)
						if(te == null && ht.containsKey("sapnummer"))
						{
							sapnummer = (String)ht.get("sapnummer");
							te = top_list.getTopBySapnummer(sapnummer);
						}

						// try fallback to topname
						if(te == null && top != null && top.length() > 0)
						{
							te = top_list.getTop(top);
						}

						// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
						if(te == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String toptmp = top.substring(0, top.indexOf(" |"));
							te = top_list.getTop(toptmp);
						}

						if(te != null)
						{
							boolean updateTop = false;

							te.setName(top.trim());
							te.setInternalname(TopoTool.unifyTop(top).trim());

							// update Topname
							if(null == DAInst)
							{
								net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
								DAInst = conn.getDataAgent();
							}
							DynGenDataObj topDgd = (DynGenDataObj)topsCache.get(te.getId());
							// FALLBACK
							if(null == topDgd)
							{
								log("top " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " nicht im cache.");
								topDgd = (DynGenDataObj)DAInst.getObject(te.getId(), "CIMS.top");
							}

							boolean topnamenneusetzten = getBoolean("var.topnamenneusetzten", Boolean.FALSE);
							if(ht.containsKey("sapnummer") && !topnamenneusetzten)
							{
								// PKO 20190315 - Topname nicht aendern wenn eine sapnummer in der zinslistenconfig konfiguriert ist!
								// Betrifft atm nur die WertInvest
								// Bei der ARE gibts auch eine Sapnummer -> Sollte aber keine Auswirkung haben
							}
							else if(ht.containsKey("sapnummer") && topnamenneusetzten)
							{
								String topname = te.getName().trim();
								if(topname.contains("|"))
								{
									topname = topname.substring(0, topname.indexOf("|")).trim();
								}

								topDgd.set("var.name", topname);
								updateTop = true;
							}
							else
							{
								if(!topDgd.get("var.name").equals(te.getName().trim()))
								{
									topDgd.set("var.name", te.getName().trim());
									updateTop = true;
								}
							}

							if(!topDgd.get("var.internalname").equals(te.getInternalname()))
							{
								topDgd.set("var.internalname", te.getInternalname());
								updateTop = true;
							}

							if(updateTop)
							{
								stellplaetzeZumUpdate.put(te.getId(), topDgd);
							}

						}

						String utop = TopoTool.unifyTop(top);

						if(null == te)
						{
							ct.append("<input type=checkbox name=\"createtop_" + utop + "\" value=1> " + Tr.t("textUnknownRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textUnknownRentalUnit2", session.getString("language")) + " " + Tr.t("textUnknownRentalUnit3", session.getString("language")) + "<br>\n");

							createtops = true;
							count_unbekannte_tops++;
						}
						else if(te.isBought())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
						}
						// mgo 20150625 - angemietete Tops auch berücksichtigen
						else if(te.isLeased())
						{ // angemietet
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
						}
						else if(te.isSold())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textSoldRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textSoldRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
							createtops = true;
							count_verkaufte_tops++;

							zlprotocol.appendTxtErr(Tr.t("textSoldRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textSoldRentalUnit2", session.getString("language")) + " " + zl.haus + ", " + zl.plz + " " + Tr.t("textSoldRentalUnit3", session.getString("language")));
						}
						else if(te.isMerged())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							if(isoldlist)
							{
								// bei einer alten Liste setzen wir diesen status nicht um
							}
							else
							{
								ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + topStatusValues.get("-3") + " " + Tr.t("textRentalUnitExists1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textCombinedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
								createtops = true;
							}
							count_zusammengelegte_tops++;
						}
						else if(te.isPlanned())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textPlannedRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textPlannedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
							createtops = true;
						}
						else if(te.isInspected())
						{ // gekauftz
							tops_in_zl.put(utop, te.id);
							if(te.hasEdvNr())
							{
								tops_in_zl.put("topedvnummer" + te.getEdvNr(), te.id);
							}
							if(sapnummer.length() > 0)
							{
								tops_in_zl.put("sapnummer" + sapnummer, te.id);
							}
							// tops_in_zl.put(te.internalname, te.id);
							ct.append("<input type=checkbox name=\"" + te.id + "__status\" value=1> " + Tr.t("textInspectedRentalUnit1", session.getString("language")) + " \"" + zzInfo + "\" " + Tr.t("textInspectedRentalUnit2", session.getString("language")) + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("1") + "<br>\n");
							createtops = true;
						}
					}

					// Update Top Names
					// Hashtable updateRes = storeObjectsJunked(topsZumUpdate, session);
					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}

					overallcalculation = 0.8;
					Hashtable updateResStellplaetze = DAInst.storeObjects(stellplaetzeZumUpdate, session);
					updateProgess(new BigDecimal(overallcalculation), "Speichern von " + zl.stellplaetze.size() + " Stellplätzen abgeschlossen", null);

					if(count_unbekannte_tops > 0)
					{
						ct.append("<b>" + count_unbekannte_tops + " " + Tr.t("textCountUnknownRU", session.getString("language")) + "</b><br>\n");
					}
					if(count_verkaufte_tops > 0)
					{
						ct.append("<b>" + count_verkaufte_tops + " " + Tr.t("textCountSoldRU", session.getString("language")) + "</b><br>\n");
					}
					if(count_zusammengelegte_tops > 0)
					{
						ct.append("<b>" + count_zusammengelegte_tops + " " + Tr.t("textCountCombinedRU", session.getString("language")) + "</b><br>\n");
					}

					Hashtable allmytopsAKTIV = null;
					if(xc != null)
					{
						String zltypeName = zinslistenImport.getZlTypeConfig().getName();
						this.set("var.zltypename", zltypeName);
						xc.getXMLConfig("hausverwaltung", zltypeName + "mieter");
					}
					if(xc != null && xc.getExpectedtopsofstatus().size() > 0)
					{
						String[] status = new String[xc.getExpectedtopsofstatus().size()];

						int i = 0;
						for(Object statuskey : xc.getExpectedtopsofstatus().keySet())
						{
							status[i] = String.valueOf(statuskey);
							i++;
						}

						allmytopsAKTIV = tq.getTopsForOID(oid_haus, status, session, DAInst);

					}
					else
					{
						allmytopsAKTIV = tq.getTopsForOID(oid_haus, new String[]{
							"1",
							"2"}, session, DAInst);
					}

					// uber tops mit status gekauft iterieren
					Enumeration e = allmytopsAKTIV.keys();
					boolean headerMissing = true;
					boolean buttonSelection = false;

					while(e.hasMoreElements())
					{
						String topname = (String)e.nextElement();
						if(null != topname)
						{
							// kommt das top in der zinszeile vor?
							String unifiedTopName = TopoTool.unifyTop(topname);
							String topEdvNr = "";
							try
							{
								if(topname.matches(".* \\|.*\\|"))
								{
									topEdvNr = "topedvnummer" + topname.substring(topname.indexOf(" |") + 2, topname.length() - 1);
								}
							}
							catch(Exception ex)
							{
								debug.error(ex);
							}

							// Wenn keine Sapnummer in der Config verwendet wird, dann auch nicht auf sapnummer pruefen!
							if(!topname.startsWith("sapnummer"))
							{
								// Sapnummer in Top auf "" setzten wenn es in der Config nicht verwendet wird, macht sonst nur troubles
								String mytid = (String)allmytopsAKTIV.get(topname);
								DynGenDataObj topdgd = (DynGenDataObj)DAInst.getObject(mytid, "");
								String sapnummerTop = topdgd.getString("var.sapnummer");

								if(sapnummerTop.length() == 0)
								{
									// do nothing
								}
								else
								{
									topdgd.set("var.sapnummer", "");
									DAInst.storeObject(topdgd, topdgd.getTemplateType(), mytid, session);
								}
							}

							if(topname.startsWith("sapnummer"))
							{
								sapnummer = topname.replaceAll("sapnummer", "");
							}

							boolean topexists = false;
							if(topname.startsWith("sapnummer") && tops_in_zl.containsKey(topname))
							{
								topexists = true;
							}
							else if(tops_in_zl.containsKey(unifiedTopName))
							{
								topexists = true;
							}
							else if(tops_in_zl.containsKey(topEdvNr))
							{
								topexists = true;
							}
							else if(unifiedTopName.contains("sapnummer"))
							{
								String newunifiedTopName = unifiedTopName.replace("sapnummer", "topedvnummer");
								if(tops_in_zl.containsKey(newunifiedTopName))
								{
									topexists = true;
								}
							}

							if(!topexists)
							{
								String mytid = (String)allmytopsAKTIV.get(topname);
								if(mytid == null)
								{
									mytid = (String)allmytopsAKTIV.get("sapnummer" + sapnummer);
								}

								// PKO - 20160113 #6634-ME noch da obwohl inaktiv in BW
								if(!isoldlist)
								{ // bei alten Listen brauchen wir keine Statusaenderung
									if(zinslistenImport.getZlTypeConfig().isSettopautomatischaufzusammengelegtOrcreatenewtopsautomatically() && null != mytid)
									{
										// get top from id and set status to -3 = verkauft
										try
										{
											if(null == DAInst)
											{
												net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
												DAInst = conn.getDataAgent();
											}
											DynGenDataObj topdgdforstatuschange = (DynGenDataObj)DAInst.getObject(mytid, "");
											topdgdforstatuschange.set("var.status", "-3");
											DAInst.storeObject(topdgdforstatuschange, topdgdforstatuschange.getTemplateType(), mytid, session);

										}
										catch(Exception ex)
										{
											debug.error(ex);
										}
									}
									else
									{
										// achtung top kommt nicht vor!
										if(null != mytid)
										{

											String topinfo = (String)allmytopsInfoHash.get(mytid);
											if(null == topinfo)
											{
												topinfo = topname;
											}

											if(sapnummer != null && sapnummer.length() > 0)
											{
												topinfo = "|" + sapnummer + "| " + topinfo;
											}

											if(topEdvNr != null && topEdvNr.length() > 0)
											{
												topinfo = "|" + topEdvNr + "| " + topinfo;
											}

											// -1|-2|-3
											String statusformissingunit = (String)get("var.statusformissingunit");

											if(statusformissingunit == null || !statusformissingunit.contains("|"))
											{
												statusformissingunit = "";
											}
											else
											{
												statusformissingunitSplit = statusformissingunit.split("\\|");
											}

											if(statusformissingunit.length() == 0 || statusformissingunitSplit.length <= 1)
											{
												ct.append("<input type=checkbox name=\"" + mytid + "__status" + "\" value=\"-3\"> " + Tr.t("textRentalUnitMissing1", session.getString("language")) + " \"" + topinfo + "\" " + Tr.t("textRentalUnitMissing2", session.getString("language")) + " " + " " + Tr.t("textTo", session.getString("language")) + " " + topStatusValues.get("-3") + " " + Tr.t("textRentalUnitMissing4", session.getString("language")) + "<br>\n");
											}
											else
											{
												if(headerMissing)
												{
													ct.append("<b>" + Tr.t("textRentalUnitMissing5", session.getString("language")) + "</b><br>\n");
													headerMissing = false;
													buttonSelection = true;
												}

												// get Values From TopStatusSelector
												getTopStatusValues();

												int i = 0;
												for(i = 0; i < statusformissingunitSplit.length; i++)
												{
													try
													{
														ct.append("<input type=radio name=\"" + mytid + "__status" + "\" class=\"radio_" + i + "\" value=\"" + statusformissingunitSplit[i] + "\"> " + topStatusValues.get(statusformissingunitSplit[i]) + "&nbsp;&nbsp;");
													}
													catch(Exception exc)
													{
														debug.error(exc);
													}
												}
												ct.append("<input type=radio name=\"" + mytid + "__status" + "\" class=\"radio_" + (i + 1) + "\" value=\"\" checked=checked> " + Tr.t("textStatusBelassen", session.getString("language")) + "&nbsp;&nbsp;-&nbsp;&nbsp;");
												ct.append(Tr.t("textRentalUnitMissing1", session.getString("language")) + " \"" + topinfo + "\" <br>\n");
											}

											createtops = true;
										}
									}
								}
							}
						}

					}

					if(buttonSelection)
					{
						int ii = 0;
						for(ii = 0; ii < statusformissingunitSplit.length; ii++)
						{
							try
							{
								sballe.append("<a  class='ajaxLink buttonRed' href='#' \n onClick='markJqueryRadio(\"uploadform\",\"radio_" + ii + "\"); return false;'>" + Tr.t("textButtonSelectAll", session.getString("language")) + " " + topStatusValues.get(statusformissingunitSplit[ii]) + "</a>\n");
							}
							catch(Exception exc)
							{
								debug.error(exc);
							}
						}
						sballe.append("<a  class='ajaxLink buttonRed' href='#' \n onClick='markJqueryRadio(\"uploadform\",\"radio_" + (ii + 1) + "\"); return false;'>" + Tr.t("textButtonSelectAllBelasse", session.getString("language")) + "</a>\n");
					}
					else
					{
						sballe.append("<a  class='ajaxLink buttonRed' href='#' \n onClick='markJquery(\"uploadform\",0); return false;'>" + Tr.t("textButtonSelectAll", session.getString("language")) + "</a>\n");
						sballe.append("<a  class='ajaxLink buttonRed' href='#' \n onClick='markJquery(\"uploadform\",1); return false;'>" + Tr.t("textButtonInvertSelection", session.getString("language")) + "</a>\n");
					}

					if(createtops)
					{
						// Welche Tops haben wir?
						StringBuffer exitops = new StringBuffer();
						exitops.append("<br>\n");

						session.set("CURRENT_VIEW", "NEWTOPS");
						zlprotocol.clearHtmlRes();
						zlprotocol.appendHtmlRes(zl.getBaseInfosInHTML(session.getString("language")));
						shortinfo = zl.getShortInfos();
						zlprotocol.appendHtmlRes(ct.toString());
						zlprotocol.appendHtmlRes(sballe.toString());
						zlprotocol.appendHtmlRes(exitops.toString());
						set("var.resultcode", zlprotocol.getHtmlRes());
						set("var.errorcode", zlprotocol.getHtmlErr());
						set("var.errorcodetxt", zlprotocol.getTxtErr());
						set("var.errorcodecsv", zl.getErrorsInCSV("", session));
						session.set("TRANSACTIONTRIGGER", "");

						String link = CoolStringTool.buildLink(myoid, "INFO", "", "Import Result", "", global, session);
						String rrplainlink = buildFredLink(myoid, "INFO", "", "Import Result", "", global, session);
						updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_FEEDBACK, "RRImport file " + file + ". " + zlprotocol.getTxtErr(), link, rrplainlink, last_oid_haus);

						// Setzt den aktuellen Status des Zinslistenimports
						setImportStatus("3");

						LockingSingleton.getInstance().leave(lockname);

						updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);
						pp.deregisterProcess();
						if(isJSON)
						{
							String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
							String jsonRes = createZZJsonReply("warn", jerr, null, null);
							return new ParseResult(jsonRes, 0, "", ses);
						}
						return super.parse(templatecode, glo, ses);
					}
				}
				else if(createnewtops.equals("1"))
				{
					Hashtable newtops = new Hashtable();
					int ntcount = 0;

					String oid_gebaeude = "";

					for(int j = 0; j < zl.zinszeilen.size(); j++)
					{
						Hashtable ht = (Hashtable)zl.zinszeilen.get(j);

						// PKO - Gebaeude oid needed to write slot gtops on gebaeude
						if(ht.containsKey("gebaeudeedvnummer"))
						{
							String gebaeudeedvnummer = (String)ht.get("gebaeudeedvnummer");
							oid_gebaeude = topotool.getGebaeudeOIDFromEDVNummer(gebaeudeedvnummer, oid_haus);
						}

						String top = (String)ht.get("top");
						String oid_top = (String)allmytops.get(top);
						if(null == oid_top)
						{
							oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
						}

						// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
						if(oid_top == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String tmpTop = top.substring(0, top.indexOf(" |"));
							oid_top = (String)allmytops.get(tmpTop);
						}
						if(oid_top == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String sapnummer = top.substring(top.indexOf(" |"));
							sapnummer = sapnummer.replaceAll("\\|", "").trim();
							oid_top = (String)allmytops.get("sapnummer" + sapnummer);
						}

						if(null != oid_top)
						{
							String myurl2 = dynurl + "?OID=" + oid_top;
							if(redirectobj.length() > 0)
							{
								myurl2 = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl2, StandardCharsets.UTF_8) + "&ESSENCEID=" + sessid;
							}
							String topinfo = (String)allmytopsInfoHash.get(oid_top);
							if(null == topinfo)
							{
								topinfo = top;
							}

							boolean topnamenneusetzten = getBoolean("var.topnamenneusetzten", Boolean.FALSE);
							if(ht.containsKey("sapnummer") && topnamenneusetzten)
							{
								String topname = top.trim();
								if(topname.contains("|"))
								{
									topname = topname.substring(0, topname.indexOf("|")).trim();
								}

								DynGenDataObj topDgd = (DynGenDataObj)topsCache.get(oid_top);
								topDgd.set("var.name", topname);
								DAInst.storeObject(topDgd, topDgd.getTemplateType(), oid_top, session);
							}

							zlprotocol.appendHtmlRes(Tr.t("textRentalUnitExists1", session.getString("language")) + " <a  class='ajaxLink redlink' href=\"" + myurl2 + "\">" + topinfo + "</a> " + Tr.t("textRentalUnitExists2", session.getString("language")) + "<br>\n");

							// todo ayse
							Hashtable allmytopsAKTIV = null;
							if(xc != null)
							{
								String zltypeName = zinslistenImport.getZlTypeConfig().getName();
								this.set("var.zltypename", zltypeName);
								xc.getXMLConfig("hausverwaltung", zltypeName + "mieter");
							}
							if(xc != null && xc.getExpectedtopsofstatus().size() > 0)
							{
								String[] status = new String[xc.getExpectedtopsofstatus().size()];

								int i = 0;
								for(Object statuskey : xc.getExpectedtopsofstatus().keySet())
								{
									status[i] = String.valueOf(statuskey);
									i++;
								}

								allmytopsAKTIV = tq.getTopsForOID(oid_haus, status, session, DAInst);

							}
							else
							{
								allmytopsAKTIV = tq.getTopsForOID(oid_haus, new String[]{
									"1",
									"2"}, session, DAInst);
							}

							// uber tops mit status gekauft iterieren
							Enumeration e = allmytopsAKTIV.keys();
							boolean headerMissing = true;
							boolean buttonSelection = false;

							while(e.hasMoreElements())
							{
								String topname = (String)e.nextElement();
								if(null != topname)
								{
									// kommt das top in der zinszeile vor?
									String unifiedTopName = TopoTool.unifyTop(topname);
									String topEdvNr = "";
									try
									{
										if(topname.matches(".* \\|.*\\|"))
										{
											topEdvNr = "topedvnummer" + topname.substring(topname.indexOf(" |") + 2, topname.length() - 1);
										}
									}
									catch(Exception ex)
									{
										debug.error(ex);
									}

									// Wenn keine Sapnummer in der Config verwendet wird, dann auch nicht auf sapnummer pruefen!
									if(!topname.startsWith("sapnummer"))
									{
										// Sapnummer in Top auf "" setzten wenn es in der Config nicht verwendet wird, macht sonst nur troubles
										String mytid = (String)allmytopsAKTIV.get(topname);
										DynGenDataObj topdgd = (DynGenDataObj)DAInst.getObject(mytid, "");
										String sapnummerTop = topdgd.getString("var.sapnummer");

										if(sapnummerTop.length() == 0)
										{
											// do nothing
										}
										else
										{
											topdgd.set("var.sapnummer", "");
											DAInst.storeObject(topdgd, topdgd.getTemplateType(), mytid, session);
										}
									}

									if(topname.startsWith("sapnummer"))
									{
										String sapnummer = topname.replaceAll("sapnummer", "");
									}

									boolean topexists = false;
									if(topname.startsWith("sapnummer") && tops_in_zl.containsKey(topname))
									{
										topexists = true;
									}
									else if(tops_in_zl.containsKey(unifiedTopName))
									{
										topexists = true;
									}
									else if(tops_in_zl.containsKey(topEdvNr))
									{
										topexists = true;
									}

									if(!topexists)
									{
										String mytid = (String)allmytopsAKTIV.get(topname);
										if(mytid == null)
										{
											String sapnummer = (String)ht.get("sapnummer");
											mytid = (String)allmytopsAKTIV.get("sapnummer" + sapnummer);
										}

										// PKO - 20160113 #6634-ME noch da obwohl inaktiv in BW
										if(!isoldlist)
										{ // bei alten Listen brauchen wir keine Statusaenderung
											if(zinslistenImport.getZlTypeConfig().isSettopautomatischaufzusammengelegtOrcreatenewtopsautomatically() && null != mytid)
											{
												// get top from id and set status to -3 = verkauft
												try
												{
													if(null == DAInst)
													{
														net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
														DAInst = conn.getDataAgent();
													}
													DynGenDataObj topdgdforstatuschange = (DynGenDataObj)DAInst.getObject(mytid, "");
													// stelle zwei
													topdgdforstatuschange.set("var.status", "-3");
													DAInst.storeObject(topdgdforstatuschange, topdgdforstatuschange.getTemplateType(), mytid, session);

												}
												catch(Exception ex)
												{
													debug.error(ex);
												}
											}
										}
									}
								}

							}
						}
						else
						{
							// TOP EXISTIERT NICHT FRAGEN!!!!
							String utop = TopoTool.unifyTop(top);

							String cts = (String)this.get("arg.createtop_" + utop);
							if(cts == null || cts.length() == 0)
							{
								cts = (String)session.get("arg.oid" + this.volatile_id + ".createtop_" + TopoTool.unifyTop(top));
							}

							if(null == cts)
							{
								cts = "";
							}
							this.set("arg.createtop_" + TopoTool.unifyTop(top), "");

							// System.err.println("createtop unify: " + top + " -> " + TopoTool.unifyTop(top) + "[create=" + cts + "]");

							if(cts.equals("1") || zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())
							{
								Object zinszeileStatus = ht.get("status");
								if(isoldlist)
								{
									// zusammengelegt
									ht.put("status", "-3");
								}
								DynGenDataObj dgd = createTop(ht, oid_haus, zl);
								if(null != dgd)
								{
									newtops.put("NEW" + ntcount, dgd);
									zlprotocol.appendMailMsg("<br> " + Tr.t("textRentalUnitExists1", session.getString("language")) + " " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " " + Tr.t("textRentalUnitExists3", session.getString("language")));
									log("top " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " neu angelegt.");
									ntcount++;
									if(newtops.size() >= STORE_JUNK)
									{
										junkStore(newtops, oid_haus);
										newtops.clear();
										ntcount = 0;
									}
								}
								ht.remove("status");
								if(zinszeileStatus != null)
								{
									ht.put("status", zinszeileStatus);
								}
							}
							else
							{
								log("top " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " NICHT neu angelegt.");
							}
						}

					}

					for(int j = 0; j < zl.stellplaetze.size(); j++)
					{
						Hashtable ht = (Hashtable)zl.stellplaetze.get(j);

						// PKO - Gebaeude oid needed to write slot gtops on gebaeude
						if(ht.containsKey("gebaeudeedvnummer"))
						{
							String gebaeudeedvnummer = (String)ht.get("gebaeudeedvnummer");
							oid_gebaeude = topotool.getGebaeudeOIDFromEDVNummer(gebaeudeedvnummer, oid_haus);
						}

						String top = (String)ht.get("top");
						String oid_top = (String)allmytops.get(top);
						if(null == oid_top)
						{
							oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
						}

						// wenn eine Sapnummer konfiguriert ist und im Topnamen eine EDV Nummer steht -> als Fallback die EDV Nummer aus dem Topnamen entfernen und den Topnamen so suchen
						if(oid_top == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String tmpTop = top.substring(0, top.indexOf(" |"));
							oid_top = (String)allmytops.get(tmpTop);
						}
						if(oid_top == null && ht.containsKey("sapnummer") && top.matches(".* \\|.*\\|"))
						{
							String sapnummer = top.substring(top.indexOf(" |"));
							sapnummer = sapnummer.replaceAll("\\|", "").trim();
							oid_top = (String)allmytops.get("sapnummer" + sapnummer);
						}

						if(null != oid_top)
						{
							String myurl2 = dynurl + "?OID=" + oid_top;
							if(redirectobj.length() > 0)
							{
								myurl2 = dynurl + "?OID=" + redirectobj + URLEncoder.encode(myurl2, StandardCharsets.UTF_8) + "&ESSENCEID=" + sessid;
							}
							String topinfo = (String)allmytopsInfoHash.get(oid_top);
							if(null == topinfo)
							{
								topinfo = top;
							}

							boolean topnamenneusetzten = getBoolean("var.topnamenneusetzten", Boolean.FALSE);
							if(ht.containsKey("sapnummer") && topnamenneusetzten)
							{
								String topname = top.trim();
								if(topname.contains("|"))
								{
									topname = topname.substring(0, topname.indexOf("|")).trim();
								}

								DynGenDataObj topDgd = (DynGenDataObj)topsCache.get(oid_top);
								topDgd.set("var.name", topname);
								DAInst.storeObject(topDgd, topDgd.getTemplateType(), oid_top, session);
							}

							zlprotocol.appendHtmlRes(Tr.t("textParkingSpaceExists1", session.getString("language")) + " <a  class='ajaxLink redlink' href=\"" + myurl2 + "\">" + topinfo + "</a> " + Tr.t("textParkingSpaceExists2", session.getString("language")) + "<br>\n");
						}
						else
						{
							// Stellplatz EXISTIERT NICHT FRAGEN!!!!
							String cts = (String)this.get("arg.createtop_" + TopoTool.unifyTop(top));
							if(cts == null || cts.length() == 0)
							{
								cts = (String)session.get("arg.oid" + this.volatile_id + ".createtop_" + TopoTool.unifyTop(top));
							}
							if(null == cts)
							{
								cts = "";
							}
							this.set("arg.createtop_" + TopoTool.unifyTop(top), "");

							// System.err.println("createtop unify: " + top + " -> " + TopoTool.unifyTop(top) + "[create=" + cts + "]");

							if(cts.equals("1") || zinslistenImport.getZlTypeConfig().isCreatenewtopsautomatically())
							{
								Object zinszeileStatus = ht.get("status");
								if(isoldlist)
								{
									// zusammengelegt
									ht.put("status", "-3");
								}
								DynGenDataObj dgd = createStellplatz(ht, oid_haus, zl);
								if(null != dgd)
								{
									newtops.put("NEW" + ntcount, dgd);
									zlprotocol.appendMailMsg("<br> " + Tr.t("textParkingSpaceExists1", session.getString("language")) + " " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " " + Tr.t("textRentalUnitExists3", session.getString("language")));
									log("stellplatz " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " neu angelegt.");
									ntcount++;

									if(newtops.size() > STORE_JUNK)
									{
										junkStore(newtops, oid_haus);
										newtops.clear();
										ntcount = 0;
									}
								}
								ht.remove("status");
								if(zinszeileStatus != null)
								{
									ht.put("status", zinszeileStatus);
								}
							}
							else
							{
								log("stellplatz " + ht.get("top") + " in " + zl.haus + " " + zl.plz + " " + zl.ort + " NICHT neu angelegt.");
							}

						}

					}

					// CREATE NEW ONES
					if(newtops.size() > 0)
					{
						log("Lege " + newtops.size() + " Mieteinheiten an.");
						Hashtable tres = storeObjectsJunked(newtops, session);
						if(tres != null)
						{
							addTopsToHaus(tres, oid_haus);
						}

						allmytops = tq.getTopsForOID(oid_haus, null, session, DAInst);
						allmyinternaltops = TopoTool.getInternalTopsForTops(allmytops);
						top_list = new TopList(session, global, DAInst, oid_haus, false);
						set("var.toplistjson", top_list.toJSON(ses));

					}

					// Tops den Gebaeuden zuordnen
					Hashtable<String, Hashtable<String, String>> topszugebaeuden = new Hashtable<String, Hashtable<String, String>>();
					Hashtable<String, String> topzugebaeude = new Hashtable<String, String>();
					String old_oid_gebaeude = "";

					for(int j = 0; j < zl.zinszeilen.size(); j++)
					{
						Hashtable ht = (Hashtable)zl.zinszeilen.get(j);

						if(ht.containsKey("gebaeudeedvnummer"))
						{
							String gebaeudeedvnummer = (String)ht.get("gebaeudeedvnummer");
							oid_gebaeude = topotool.getGebaeudeOIDFromEDVNummer(gebaeudeedvnummer, oid_haus);
							if(oid_gebaeude == null)
							{
								oid_gebaeude = "";
							}
						}

						if(old_oid_gebaeude.equals(""))
						{
							old_oid_gebaeude = oid_gebaeude;
						}

						if(!old_oid_gebaeude.equals(oid_gebaeude) && old_oid_gebaeude != null && old_oid_gebaeude.length() > 0)
						{
							topszugebaeuden.put(old_oid_gebaeude, topzugebaeude);
							topzugebaeude = new Hashtable<String, String>();
							old_oid_gebaeude = oid_gebaeude;
						}

						String top = (String)ht.get("top");
						String oid_top = (String)allmytops.get(top);
						if(null == oid_top)
						{
							oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
						}
						if(oid_gebaeude != null && oid_gebaeude.length() > 0 && oid_top != null && oid_top.length() > 0)
						{
							topzugebaeude.put(oid_top, "");
						}
					}

					topzugebaeude = new Hashtable<String, String>();
					for(int j = 0; j < zl.stellplaetze.size(); j++)
					{
						Hashtable ht = (Hashtable)zl.stellplaetze.get(j);

						if(ht.containsKey("gebaeudeedvnummer"))
						{
							String gebaeudeedvnummer = (String)ht.get("gebaeudeedvnummer");
							oid_gebaeude = topotool.getGebaeudeOIDFromEDVNummer(gebaeudeedvnummer, oid_haus);
							if(oid_gebaeude == null)
							{
								oid_gebaeude = "";
							}
						}

						if(old_oid_gebaeude.equals(""))
						{
							old_oid_gebaeude = oid_gebaeude;
						}

						if(!old_oid_gebaeude.equals(oid_gebaeude) && old_oid_gebaeude != null && old_oid_gebaeude.length() > 0)
						{
							topszugebaeuden.put(old_oid_gebaeude, topzugebaeude);
							topzugebaeude = new Hashtable<String, String>();
							old_oid_gebaeude = oid_gebaeude;
						}

						String top = (String)ht.get("top");
						String oid_top = (String)allmytops.get(top);
						if(null == oid_top)
						{
							oid_top = (String)allmyinternaltops.get(TopoTool.unifyTop(top));
						}
						if(oid_top != null && oid_gebaeude != null && oid_gebaeude.length() > 0)
						{
							topzugebaeude.put(oid_top, "");
						}
					}

					if(topszugebaeuden.size() > 0)
					{
						addTopsToGebaeude(topszugebaeuden);
					}

				}
				else if(createnewtops.equals("0"))
				{
					// weiter aber ohne neuanlegen
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log11: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// Falls additionalFields in den Hausinfos befüllt ist, Haus updaten
				String hausOID = updateHaus(zl, oid_haus);
				if(hausOID == null || hausOID.equals(""))
				{
					debug.error("Error in parse of UploadXLS4 - could not update Haus with oid=" + oid_haus + "!");
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log12: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// Create Mietvertragsverknuepfung
				zl = createVerknuepfungZuMietvertrag(top_list, zl, false);

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log13: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				Hashtable zz2store = zinszeilenAnlegen(zl, top_list, oid_haus, true);

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log14: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				importerror = false;
				top_list = null;

				Date d1 = new Date();
				resultSizeOfStoredObjects += storeObjectsJunked(zz2store, session).size();

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log15: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				// Gebaeude anlegen / Updaten wenn vorhanden + Mapping zu den Tops!
				createOrUpdateGebaeudeAndTopMapping(zl, oid_haus);

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log16: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				debug.info("I've Datapackages -> No:" + zl.getDhwDatapackages().size());
				for(DataPackage dataPackage : zl.getDhwDatapackages())
				{
					debug.info("Datapackage Name:" + dataPackage.dbtable);
					String[][] datapackage = dataPackage.data;
					for(int i = 0; i < datapackage.length; i++)
					{
						StringBuffer buff = new StringBuffer();
						for(int j = 0; j < datapackage[0].length; j++)
						{
							buff.append(datapackage[i][j]);
							buff.append(";");
						}
						debug.info(buff.toString());
					}

					DWHHandler dwhHandler = DWHHandler.getDWHHandler(dataPackage, session);
					dwhHandler.deleteFactsByUniqueEntryDefinitionFieldnames(dataPackage);
					dwhHandler.insert(dataPackage);
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log17: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				Date d2 = new Date();
				System.out.println("Einlesen: " + (d2.getTime() - d1.getTime()) + " ms.");
				zlprotocol.appendHtmlRes("" + createzz + " " + Tr.t("textFrom", session.getString("language")) + " " + (createzz + overwritezz) + " " + Tr.t("textRentRollsCreated", session.getString("language")) + "<br>\n");
				zlprotocol.appendHtmlRes("" + overwritezz + " " + Tr.t("textFrom", session.getString("language")) + " " + (createzz + overwritezz) + " " + Tr.t("textRentRollsOverwritten", session.getString("language")) + "<br>\n");
				zlprotocol.appendHtmlRes("</div>");
				zlprotocol.appendHtmlRes("<h2>" + Tr.t("textRentalUnits", session.getString("language")) + "</h2>");
				zlprotocol.appendHtmlRes(zl.getZinszeilenInHTML(session));
				zlprotocol.appendHtmlRes("<h2>" + Tr.t("textParkingSpaces", session.getString("language")) + "</h2>");
				zlprotocol.appendHtmlRes(zl.getStellplaetzeInHTML(session));
				if(resultSizeOfStoredObjects > 0)
				{
					if(!TopoQueries.writeImportInfoForHaus(oid_haus, zl.monat, zl.jahr, DAInst, session))
					{
						debug.log(this, "could not update hausimport info for haus with oid " + oid_haus + "!");
					}
				}
				String rc = zlprotocol.getHtmlRes();
				set("var.resultcode", rc);

				// Clear Errors and then add all again
				zlprotocol.clearHtmlErr();

				zlprotocol.appendHtmlErr(zl.getErrorsInHTML(ignoreerrors, session));
				set("var.errorcode", zlprotocol.getHtmlErr());
				set("var.errorcodetxt", zlprotocol.getTxtErr());
				this.set("dirty", "yes");

				// 22975-7606-Infoerweiterung beim Zinslistenimport
				// query auf ZZ um die aktuelle und vorperiode zu bekommen!
				String mailAndName = getAssetmanagerMailadressFromObject(oid_haus);

				boolean periodenvergleich = this.getBoolean("var.periodenvergleich", Boolean.TRUE);
				Hashtable<String, String> mailinglistKennwerteNachNutzung = new Hashtable<String, String>();
				if(periodenvergleich)
				{
					try
					{
						generatePeriodenvergleich(oid_haus, zl);
					}
					catch(Exception e)
					{
						// Do nothing -> keine vorperiode vorhanden
						;
					}
				}

				if(enableDetailedLogging)
				{
					endtime = System.currentTimeMillis();
					BugMe.getInstance().log("############ Log18: " + ((endtime - starttime) / 1000) + " seconds");
					starttime = System.currentTimeMillis();
				}

				if(mailinglistKennwerteNachNutzung.containsKey(mailAndName))
				{
					zlprotocol.appendMailMsg(mailinglistKennwerteNachNutzung.get(mailAndName));
					zlprotocol.addCsvLine(new String[]{
						mailinglistKennwerteNachNutzung.get(mailAndName),
						"",
						"",
						"",
						"",
						""});
				}

				// // Text for new units
				// if (newimportedtops != null && newimportedtops.length() > 0) {
				// zlprotocol.appendMailMsg("<br>\n");
				// zlprotocol.appendMailMsg(newimportedtops.toString());
				// }

				// // errors in the mail with changes
				// if (errorsformailinglist != null && errorsformailinglist.length() > 0) {
				// zlprotocol.appendMailMsg("<br>\n");
				// zlprotocol.appendMailMsg(errorsformailinglist.toString());
				// }

				String datasource = Tr.t("textdatasource", session.getString("language"));
				String filenameforemail = this.get("var.filename").toString();
				zlprotocol.appendMailMsg("<br><br>" + datasource + ": \n" + filenameforemail);

				long end_upload = System.currentTimeMillis();
				long upload_time_s = (end_upload - start_upload) / 1000;
				long upload_time_m = upload_time_s / 60;
				upload_time_s = upload_time_s % 60;
				this.set("duration", "" + upload_time_m + "m " + upload_time_s + "s");
				zlprotocol.appendMailMsg("<br>processing: \n" + upload_time_m + "m " + upload_time_s + "s");
				if(!zlprotocol.isCSVEmpty())
				{
					zlprotocol.addCsvLine(new String[]{
						"<br>",
						datasource + ": " + filenameforemail,
						"",
						"",
						"processing:",
						"",
						"",
						upload_time_m + "m " + upload_time_s + "s",
						"",
						""});
				}
				if(assetmanagerinfo.equals("1"))
				{
					sendMailToAssetmanagerSingleObject(mailAndName, zl.haus);
				}
				sendMailWithChanges();

			}

			catch(Exception fe)
			{
				debug.error(fe);
			}

			// READ FILE XLS
			java.util.Date end_time = new java.util.Date();
			long run_time = end_time.getTime() - start_time.getTime();
			debug.log("HAUS ZL IMPORT - TOTAL RUN TIME " + run_time / 1000 + " secs.");

			long end_upload = System.currentTimeMillis();
			long upload_time_s = (end_upload - start_upload) / 1000;
			long upload_time_m = upload_time_s / 60;
			upload_time_s = upload_time_s % 60;
			this.set("var.duration", "" + upload_time_m + "m " + upload_time_s + "s");
			this.set("var.hausverwaltung", hausverwaltung);
			this.set("var.filename", lockname);

			LockingSingleton.getInstance().leave(lockname);

			updateProgess(BigDecimal.ONE, "Verarbeitung abgeschlossen", null);

			if(enableDetailedLogging)
			{
				endtime = System.currentTimeMillis();
				BugMe.getInstance().log("############ Log19: " + ((endtime - starttime) / 1000) + " seconds");
				starttime = System.currentTimeMillis();
			}

			pp.deregisterProcess();
		}
		topsCache = null;
		zinsZeilenCache = null;

		session.set("TRANSACTIONTRIGGER", "");

		// Setzt den aktuellen Status des Zinslistenimports
		debug.error("REMOVE AFTER DEBUG: sapconnection:" + sapconnection + " // quellsystem: " + quellsystem);
		if(sapconnection.equals("1") && quellsystem.equals("sapcsv") && file.length() > 0)
		{
			// Clean Up import directory
			debug.error("INFO: Cleaning up Import directory start ...");
			SAPCSVQuery sapcsvquery = new SAPCSVQuery();
			sapcsvquery.moveFileToBackupFolder(this);
			debug.error("INFO: Cleaning up Import directory done ...");
		}

		if(!view.endsWith("INFO"))
		{
			if(!importerror)
			{
				updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_OK, "RRImport for file " + file + " - Ready", "", "", last_oid_haus);
				setImportStatus("2");
			}
			else
			{
				updateFileUploadStatus(dao, fileUpload, fileSequenceNumber, FileUpload.STATUS_ERROR, "RRImport for file " + file + " - Error", "", "", last_oid_haus);
				setImportStatus("3");
			}
		}
		if(isJSON)
		{
			String jerr = CoolJSONTool.createJsonErrorString(getString("var.errorcode"));
			String jsonRes = createZZJsonReply("info", "...", null, null);
			return new ParseResult(jsonRes, 0, "", ses);
		}
		return super.parse(templatecode, glo, ses);
	}

	/**
	 * Creates a standardized JSON response object that wraps custom JSON data
	 * together with common Vue response metadata.
	 *
	 * The response contains an ID, status, template type, and a customdata object
	 * parsed from the provided JSON string. If JSON creation fails, an empty
	 * string is returned and the error is logged.
	 *
	 * @param myoid
	 *            Identifier to include in the response.
	 * @param jsonRes
	 *            JSON string representing the custom data payload.
	 * @param vueStatus
	 *            Status value expected by the Vue frontend.
	 * @param templatetype
	 *            Template type identifier for frontend handling.
	 *
	 * @return
	 *         A JSON string containing the wrapped response, or an empty string on error.
	 */
	private String createJsonResponseWithCustomData(String myoid, String jsonRes, String vueStatus, String templatetype)
	{
		try
		{
			org.json.JSONObject response = new org.json.JSONObject();
			org.json.JSONObject customdata = new org.json.JSONObject(jsonRes);
			response.put("ID", myoid);
			response.put("status", vueStatus);
			response.put("templatetype", templatetype);
			response.put("customdata", customdata);

			return response.toString();
		}
		catch(Exception e)
		{
			BugMe.getInstance().error(e);
			return "";
		}
	}

	/**
	 * Gets the status of freigabe.
	 *
	 * @param hausid
	 *            the hausid
	 * @param azl
	 *            the azl
	 * @return the status of freigabe
	 */
	private Boolean getStatusOfFreigabe(String hausid, Zinsliste azl)
	{
		try
		{
			Hashtable<String, Object> args = new Hashtable<String, Object>();
			ArgsHelper argsHelper = new ArgsHelper(args);
			argsHelper.setAdvancedFields(true);
			argsHelper.setMainTemplateType("CIMS.datenbestaetigung");
			argsHelper.addTemplateType("haus", "CIMS.haus");

			argsHelper.addField("ET0.abgelehnt");
			argsHelper.addField("ET0.eingeschraenkt");
			argsHelper.addField("ET0.datum");
			argsHelper.addField("haus_ID", "hausid");

			// Bsp: Periode = 2024 M2
			String periode = azl.getJahr() + " M" + azl.getMonat();

			argsHelper.addWhere("haus_ID =" + hausid + " AND ET0.periode='" + periode + "'");

			String mydom = (String)session.get("domainid");
			if(mydom.length() == 0)
			{
				argsHelper.addCondition("DOMAIN", "ALLDOMAINS");
			}
			else
			{
				argsHelper.addCondition("DOMAIN", mydom);
			}

			// query result vector
			Vector<Hashtable<String, String>> res = null;
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();

			if(res.size() > 0)
			{
				Hashtable<String, String> row = res.get(0);

				String abgelehnt = row.get("abgelehnt");
				if(abgelehnt.equals("0"))
				{
					// import not allowed
					return false;
				}
				else
				{
					// Import allowed
					return true;
				}
			}
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			set("var.result", "Interner Fehler:" + qe.getMessage());

		}
		return true;
	}

	/**
	 * Format mail content.
	 * 
	 * Note: Made public for access by ZinslistenMailService. This method is called
	 * from the mail service to format mailing list content before sending.
	 *
	 * @param mailinglist
	 *            the mailinglist
	 * @param wholetext
	 *            the wholetext
	 * @return the hashtable
	 */
	public HashMap<String, String> formatMailContent(HashMap<String, String> mailinglist, Boolean wholetext)
	{
		HashMap<String, String> mailinglistNew = new HashMap<>();

		for(String key : mailinglist.keySet())
		{
			String value = mailinglist.get(key);

			String[] valueSplitt = value.split("<br>");

			StringBuffer newObjects = new StringBuffer();
			StringBuffer unknownObjects = new StringBuffer();
			StringBuffer changes = new StringBuffer();
			StringBuffer noChanges = new StringBuffer();
			StringBuffer errors = new StringBuffer();
			StringBuffer newTops = new StringBuffer();
			StringBuffer unknownTops = new StringBuffer();
			StringBuffer salutation = new StringBuffer();
			StringBuffer footer = new StringBuffer();
			StringBuffer filelink = new StringBuffer();
			StringBuffer zapos = new StringBuffer();
			StringBuffer importresult = new StringBuffer();
			StringBuffer periodcomparison = new StringBuffer();

			for(int i = 0; i < valueSplitt.length; i++)
			{
				valueSplitt[i] = valueSplitt[i].replaceAll("<br>", "").trim();
				if(valueSplitt[i].length() > 0)
				{

					// neue Objekte
					if(valueSplitt[i].contains(Tr.t("textObjectCreated", session.getString("language"))))
					{
						if(newObjects.length() == 0)
						{
							newObjects.append("<br><br>" + Tr.t("textObjectCreated", session.getString("language")) + "<br><br>");
						}

						newObjects.append(valueSplitt[i].substring(0, valueSplitt[i].indexOf(Tr.t("textObjectCreated", session.getString("language")))).trim() + "<br>");
					}

					// unbekannte Objekte - nicht angelegt
					if(valueSplitt[i].contains(Tr.t("textNewHouse", session.getString("language"))))
					{
						if(unknownObjects.length() == 0)
						{
							unknownObjects.append("<br><br>" + Tr.t("textunknownobjects", session.getString("language")) + "<br><br>");
						}

						unknownObjects.append(valueSplitt[i] + "<br>");
					}

					// neue Tops
					else if(valueSplitt[i].contains(Tr.t("textRentalUnitExists3", session.getString("language"))))
					{
						if(newTops.length() == 0)
						{
							newTops.append("<br><br>" + Tr.t("textnewtops", session.getString("language")) + "<br><br>");
						}
						newTops.append(valueSplitt[i] + "<br>");
					}

					// neue Tops wurden nicht angelegt
					else if(valueSplitt[i].contains(Tr.t("textNewRentalUnits", session.getString("language"))))
					{
						if(unknownTops.length() == 0)
						{
							unknownTops.append("<br><br>" + Tr.t("textunknowntops", session.getString("language")) + "<br><br>");
						}
						unknownTops.append(valueSplitt[i] + "<br>");
					}

					// link auf das Import
					else if(valueSplitt[i].contains(Tr.t("textFile", session.getString("language"))))
					{
						if(filelink.length() == 0)
						{
							filelink.append("<br><br>");
						}
						filelink.append("<br>" + valueSplitt[i] + "<br>");
					}

					// salutation
					else if(valueSplitt[i].contains(Tr.t("textSalutationAssetmanagerNotify", session.getString("language"))))
					{
						// salutation
						if(!wholetext)
						{
							salutation.append(valueSplitt[i] + "<br>");
						}
						else
						{
							salutation.append(Tr.t("userSalutation", session.getString("language")) + "<br>");
						}
					}

					// anfangstext
					else if(valueSplitt[i].contains(Tr.t("textAssetmanagerNotify", session.getString("language"))))
					{
						salutation.append(valueSplitt[i]);
					}

					// footer
					else if(valueSplitt[i].contains(Tr.t("textFooter1AssetmanagerNotify", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textFooter2AssetmanagerNotify", session.getString("language"))))
					{
						// salutation
						footer.append("<br>" + valueSplitt[i] + "<br>");
					}

					// Fehlern auflisten
					else if(valueSplitt[i].contains(Tr.t("textEasyErrors", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textGeneralError", session.getString("language"))) || valueSplitt[i].toLowerCase().contains("nicht importiert") || valueSplitt[i].toLowerCase().contains("fehler:") || valueSplitt[i].toLowerCase().contains("not imported") || valueSplitt[i].toLowerCase().contains("sap datenstruktur") || valueSplitt[i].toLowerCase().contains("sap datastructure") || valueSplitt[i].toLowerCase().contains("kein import m") || valueSplitt[i].toLowerCase().contains("t proceed with import"))
					{
						if(errors.length() == 0)
						{
							errors.append("<br><br>" + Tr.t("textAssetmanagerErrorsWE", session.getString("language")) + "<br><br>");
						}

						errors.append(valueSplitt[i] + "<br>");
					}

					// Tops ohne Änderungen
					else if(valueSplitt[i].toLowerCase().contains("nderungen im haus") || valueSplitt[i].toLowerCase().contains("no change for object"))
					{
						if(noChanges.length() == 0)
						{
							noChanges.append("<br><br>" + Tr.t("textAssetmanagerNoChangesWE", session.getString("language")) + "<br><br>");
						}

						if(valueSplitt[i].contains("Keine") && !noChanges.toString().contains(valueSplitt[i].substring(0, valueSplitt[i].indexOf("Keine")).trim()))
						{
							noChanges.append(valueSplitt[i].substring(0, valueSplitt[i].indexOf("Keine")).trim() + "<br>");
						}
					}

					// verlinkung
					else if(valueSplitt[i].toLowerCase().contains("importiert.") || valueSplitt[i].toLowerCase().contains("imported."))
					{
						if(importresult.length() == 0)
						{
							importresult.append("<br><br>" + Tr.t("textlink", session.getString("language")) + ": <br><br>");
						}

						// liefert auch anzahl von den importierten Einheiten, well wholetext = true
						if(!wholetext)
						{
							int stringend = 0;
							if(valueSplitt[i].indexOf("importiert.") > 0)
							{
								stringend = valueSplitt[i].indexOf("importiert.");
							}
							importresult.append(valueSplitt[i].substring(0, stringend) + "<br>");
						}
						else
						{
							importresult.append(valueSplitt[i] + "<br>");
						}
					}

					// Periodenvergleich für AM
					else if(valueSplitt[i].contains(Tr.t("diffHeadRow", session.getString("language"))))
					{
						periodcomparison.append(valueSplitt[i] + "<br>");
					}

					// newZapos
					else if(valueSplitt[i].contains(Tr.t("textNewZapos", session.getString("language"))))
					{
						if(zapos.length() == 0)
						{
							zapos.append("<br><br>" + Tr.t("textzapos", session.getString("language")) + "<br><br>");
						}
						String zapostext = Tr.t("textNewZapos", session.getString("language")).trim();
						zapos.append(valueSplitt[i].substring(valueSplitt[i].indexOf(zapostext)).trim() + "<br>");
					}

					// from to
					else if(valueSplitt[i].contains(Tr.t("textFrom", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textTo", session.getString("language"))))
					{
						if(changes.length() == 0)
						{
							changes.append("<br><br>" + Tr.t("textAssetmanagerChangesWE", session.getString("language")) + "<br><br>");
						}
						changes.append(valueSplitt[i] + "<br>");
					}
					else
					{
						continue;
					}
				}
			}

			value = salutation.toString() + "<br>" + zapos.toString() + unknownObjects.toString() + unknownTops.toString() + filelink.toString() + newObjects.toString() + newTops.toString() + errors.toString() + importresult.toString() + "<br>" + changes.toString() + noChanges.toString() + "<br>" + periodcomparison.toString() + "<br>" + footer.toString() + "<br>";

			mailinglistNew.put(key, value);

		}
		return mailinglistNew;
	}

	/**
	 * Format var.resultcode.
	 *
	 * @param htmlresbuf
	 *            the htmlresbuf
	 * @param wholetext
	 *            the wholetext
	 * @return string
	 */
	private String formatResultCode(String htmlresbuf, Boolean wholetext)
	{
		String htmlresbufNew = "";

		String[] valueSplitt = new String[]{};
		String[] valueSplitt2 = new String[]{};

		valueSplitt2 = htmlresbuf.split("<tbody>");
		StringBuffer header = new StringBuffer();
		header.append("<tbody>" + valueSplitt2[0]);

		// valueSplitt2[1] = valueSplitt2[1].replaceAll("<br>", "").trim();
		valueSplitt = valueSplitt2[1].split("</tbody></table></div>");
		valueSplitt = valueSplitt[0].split("<br>");

		StringBuffer newObjects = new StringBuffer();
		StringBuffer unknownObjects = new StringBuffer();
		StringBuffer changes = new StringBuffer();
		StringBuffer noChanges = new StringBuffer();
		StringBuffer errors = new StringBuffer();
		StringBuffer newTops = new StringBuffer();
		StringBuffer unknownTops = new StringBuffer();
		StringBuffer salutation = new StringBuffer();
		StringBuffer footer = new StringBuffer();
		StringBuffer filelink = new StringBuffer();
		StringBuffer zapos = new StringBuffer();
		StringBuffer importresult = new StringBuffer();
		StringBuffer periodcomparison = new StringBuffer();

		for(int i = 0; i < valueSplitt.length; i++)
		{
			valueSplitt[i] = valueSplitt[i].replaceAll("<br>", "").trim();
			if(valueSplitt[i].length() > 0)
			{

				// neue Objekte
				if(valueSplitt[i].contains(Tr.t("textObjectCreated", session.getString("language"))))
				{
					if(newObjects.length() == 0)
					{
						newObjects.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textObjectCreated", session.getString("language")) + "</font></b></td></tr>");
					}

					newObjects.append(valueSplitt[i].substring(0, valueSplitt[i].indexOf(Tr.t("textObjectCreated", session.getString("language")))).trim());
				}

				// unbekannte Objekte - nicht angelegt
				if(valueSplitt[i].contains(Tr.t("textNewHouse", session.getString("language"))))
				{
					if(unknownObjects.length() == 0)
					{
						unknownObjects.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textunknownobjects", session.getString("language")) + "</b></font></td></tr>");
					}

					unknownObjects.append(valueSplitt[i]);
				}

				// neue Tops
				else if(valueSplitt[i].contains(Tr.t("textRentalUnitExists3", session.getString("language"))))
				{
					if(newTops.length() == 0)
					{
						newTops.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textnewtops", session.getString("language")) + "</font></b></td></tr>");
					}
					newTops.append(valueSplitt[i]);
				}

				// neue Tops wurden nicht angelegt
				else if(valueSplitt[i].contains(Tr.t("textNewRentalUnits", session.getString("language"))))
				{
					if(unknownTops.length() == 0)
					{
						unknownTops.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textunknowntops", session.getString("language")) + "</font></b></td></tr>");
					}
					unknownTops.append(valueSplitt[i]);
				}

				// link auf das Import
				// else if(valueSplitt[i].contains(Tr.t("textFile", session.getString("language"))))
				// {
				// if(filelink.length() == 0)
				// {
				// filelink.append("<br><br>");
				// }
				// filelink.append("<br>" + valueSplitt[i] + "<br>");
				// }

				// salutation
				else if(valueSplitt[i].contains(Tr.t("textSalutationAssetmanagerNotify", session.getString("language"))))
				{
					// salutation
					if(!wholetext)
					{
						salutation.append(valueSplitt[i] + "<br>");
					}
					else
					{
						salutation.append(Tr.t("userSalutation", session.getString("language")) + "<br>");
					}
				}

				// anfangstext
				else if(valueSplitt[i].contains(Tr.t("textAssetmanagerNotify", session.getString("language"))))
				{
					salutation.append(valueSplitt[i] + "<br>");
				}

				// footer
				else if(valueSplitt[i].contains(Tr.t("textFooter1AssetmanagerNotify", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textFooter2AssetmanagerNotify", session.getString("language"))))
				{
					// salutation
					footer.append("<br>" + valueSplitt[i] + "<br>");
				}

				// Fehlern auflisten
				else if(valueSplitt[i].contains(Tr.t("textEasyErrors", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textGeneralError", session.getString("language"))) || valueSplitt[i].toLowerCase().contains("nicht importiert") || valueSplitt[i].toLowerCase().contains("fehler:") || valueSplitt[i].toLowerCase().contains("not imported") || valueSplitt[i].toLowerCase().contains("sap datenstruktur") || valueSplitt[i].toLowerCase().contains("sap datastructure") || valueSplitt[i].toLowerCase().contains("kein import m") || valueSplitt[i].toLowerCase().contains("t proceed with import"))
				{
					if(errors.length() == 0)
					{
						errors.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textAssetmanagerErrorsWE", session.getString("language")) + "</font></b></td></tr>");
					}

					errors.append(valueSplitt[i]);
				}

				// Tops ohne Änderungen
				else if(valueSplitt[i].toLowerCase().contains("nderungen im haus") || valueSplitt[i].toLowerCase().contains("no change for object"))
				{
					if(noChanges.length() == 0)
					{
						noChanges.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textAssetmanagerNoChangesWE", session.getString("language")) + "</font></b></td></tr>");
					}

					if(valueSplitt[i].contains("Keine") && !noChanges.toString().contains(valueSplitt[i].substring(0, valueSplitt[i].indexOf("Keine")).trim()))
					{
						noChanges.append(valueSplitt[i].substring(0, valueSplitt[i].indexOf("Keine")).trim());
					}
				}

				// verlinkung
				else if(valueSplitt[i].toLowerCase().contains("importiert.") || valueSplitt[i].toLowerCase().contains("imported."))
				{
					if(importresult.length() == 0)
					{
						importresult.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textlink", session.getString("language")) + ": </font></b></td></tr>");
					}

					// liefert auch anzahl von den importierten Einheiten, well wholetext = true
					if(!wholetext)
					{
						int stringend = 0;
						if(valueSplitt[i].indexOf("importiert.") > 0)
						{
							stringend = valueSplitt[i].indexOf("importiert.");
						}
						importresult.append(valueSplitt[i].substring(0, stringend));
					}
					else
					{
						importresult.append(valueSplitt[i]);
					}
				}

				// Periodenvergleich für AM
				else if(valueSplitt[i].contains(Tr.t("diffHeadRow", session.getString("language"))))
				{
					periodcomparison.append(valueSplitt[i]);
				}

				// newZapos
				else if(valueSplitt[i].contains(Tr.t("textNewZapos", session.getString("language"))))
				{
					if(zapos.length() == 0)
					{
						zapos.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textzapos", session.getString("language")) + " </font></b></td></tr>");
					}
					String zapostext = Tr.t("textNewZapos", session.getString("language")).trim();
					zapos.append("<tr><td>" + valueSplitt[i].substring(valueSplitt[i].indexOf(zapostext) + 41).trim() + "<br>");
					// zapos.append(valueSplitt[i]);
				}

				// from to
				else if(valueSplitt[i].contains(Tr.t("textFrom", session.getString("language"))) || valueSplitt[i].contains(Tr.t("textTo", session.getString("language"))))
				{
					if(changes.length() == 0)
					{
						changes.append("<tr><td>...</td><td>...</td></tr><tr><td><b><font color=\"#ba1a02\">" + Tr.t("textAssetmanagerChangesWE", session.getString("language")) + "</font></b></td></tr>");
					}
					changes.append(valueSplitt[i]);
				}

				// import fehlerhaft
				else if(valueSplitt[i].contains(Tr.t("textNotFinished", session.getString("language"))))
				{
					errors.append(valueSplitt[i]);
				}
				else
				{
					continue;
				}
			}
		}

		htmlresbufNew = header + "<br>" + zapos.toString() + unknownObjects.toString() + unknownTops.toString() + filelink.toString() + newObjects.toString() + newTops.toString() + errors.toString() + importresult.toString() + changes.toString() + noChanges.toString() + footer.toString() + " </tbody></table></div>";

		// htmlresbufNew.put(key, value);

		return htmlresbufNew;
	}

	/**
	 * Creates the or update gebaeude and top mapping.
	 *
	 * @param zl
	 *            the zl
	 * @param oid_haus
	 *            the oid haus
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
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			String[][] array = zl.getArray();

			DynGenDataObj dgdHaus = (DynGenDataObj)DAInst.getObject(oid_haus, "");
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
			String[] headlinearray = array[zinslistenImport.getZlTypeConfig().getHeaderline()];

			for(int i = zinslistenImport.getZlTypeConfig().getHeaderline() + 1; i < array.length; i++)
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

				if(!zinslistenImport.getZlTypeConfig().isConcatEdvNrToHaus() || (hausedvnr.toLowerCase().equals(hausedvnrFromGebaeudeedvnummer.toLowerCase()) || hausedvnr.substring(3).toLowerCase().equals(hausedvnrFromGebaeudeedvnummer.toLowerCase())))
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
						dgdGebaeude = (DynGenDataObj)DAInst.getObject(gebaeudeOid, "");
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
						gebaeudeOid = DAInst.storeObject(dgdGebaeude, dgdGebaeude.getTemplateType(), gebaeudeOid, session);

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
			DAInst.storeObject(dgdHaus, dgdHaus.getTemplateType(), oid_haus, session);

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

		if(null == DAInst)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			DAInst = conn.getDataAgent();
		}
		TopoQueries topoQueries = new TopoQueries(session, global);
		Hashtable allmytops = topoQueries.getTopsForOID(oid_haus, null, session, DAInst);
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
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
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
	 * Get the values from the Flavoured TopStatusSelector.
	 *
	 * @return the top status values
	 */
	private void getTopStatusValues()
	{
		if(topStatusValues == null || topStatusValues.size() == 0)
		{

			TemplateReader tr = TemplateReader.getInstance();
			DynGenDataObj dgdTopStatusSelector = tr.getFlavouredDGDForTemplate("CIMS.TopStatusSelector", global, session);

			String language = session.getString("language").toUpperCase();
			if(language.equals("DE"))
			{
				language = "";
			}

			String alternatives = (String)dgdTopStatusSelector.get("var.alternatives");
			String textalternatives = (String)dgdTopStatusSelector.get("var.textalternatives" + language);

			String[] alts = CoolStringTool.splitOnce(alternatives);
			String[] texts = CoolStringTool.splitOnce(textalternatives);
			try
			{
				while(alts != null)
				{
					topStatusValues.put(new String(alts[0]), texts[0]);
					alternatives = alts[1];
					textalternatives = texts[1];
					alts = CoolStringTool.splitOnce(alternatives);
					texts = CoolStringTool.splitOnce(textalternatives);
				}
			}
			catch(Exception ex)
			{
				debug.error(this, "Can't create list of values ...");
				debug.error(ex);
			}
		}
	}

	/**
	 * Setzt den aktuellen Importstatus 0|1|2|3 Wartet auf Ausfuehrung|Import laeuft|Import fertig|Pruefung noetig.
	 *
	 * @param string
	 *            the new import status
	 */
	private void setImportStatus(String string)
	{
		try
		{

			// 2=Fertig, 4=Fehler > keine Aenderung wenn Import Fehrlerhaft oder Fertig, bedeutet er ist komplett durch
			String actualImportStatus = this.getString("var.importstatus");
			if(actualImportStatus.equals("2") || actualImportStatus.equals("4"))
			{
				return;
			}

			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			String id = (String)session.get("CURRENT_OID");
			if(id == null || id.length() == 0)
			{
				id = (String)this.get("id");
			}
			this.set("var.importstatus", string);

			this.fixFileLink();

			if(id == null || id.length() == 0)
			{
				DAInst.storeObject(this, this.getTemplateType(), null, session);
			}
			else
			{
				DAInst.storeObject(this, this.getTemplateType(), id, session);
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}
	}

	/**
	 * Junk store.
	 *
	 * @param newtops
	 *            the newtops
	 * @param oid_haus
	 *            the oid_haus
	 * @return true, if successful
	 */
	private boolean junkStore(Hashtable newtops, String oid_haus)
	{
		try
		{
			log("Lege " + newtops.size() + " Mieteinheiten an.");
			Hashtable tres = storeObjectsJunked(newtops, session);
			if(tres != null)
			{
				addTopsToHaus(tres, oid_haus);

				// if(oid_gebaeude != null && oid_gebaeude.length() > 0)
				// {
				// addTopsToGebaeude(tres, oid_gebaeude);
				// }
			}
		}
		catch(Exception xx)
		{
			log("Problem storing objects...");
			return false;
		}
		return true;
	}

	/**
	 * Creates the verknuepfung zu mietvertrag.
	 *
	 * @param top_list
	 *            the top list
	 * @param zl
	 *            the zl
	 * @param fehlerabfrage
	 *            the fehlerabfrage
	 * @return the zinsliste
	 */
	private Zinsliste createVerknuepfungZuMietvertrag(TopList top_list, Zinsliste zl, boolean fehlerabfrage)
	{
		Hashtable<String, Vector> topsToWrite = new Hashtable<String, Vector>();

		// Ueber alle Elemente der Toplist iterieren und cheken ob Mietvertrag vorhanden ist
		if(zinslistenImport.getZlTypeConfig().isVertragsverknuepfung())
		{
			try
			{

				String[] oids_top = top_list.getTopIDs();

				for(int i = 0; i < oids_top.length; i++)
				{
					if(null == topsCache)
					{
						topsCache = new Hashtable();
					}
					if(!topsCache.containsKey(oids_top[i]))
					{
						// topscache irgendwie kapputt?
						debug.error(this, "bad top im topscache: " + oids_top[i]);
					}
				}

				Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();

				Hashtable<String, Object> args = new Hashtable<String, Object>();

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

				if(null == DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					DAInst = conn.getDataAgent();
				}
				QueryResult qr = DAInst.queryObjectWithResult(args);
				if(!qr.isOK())
				{
					debug.error(this, "problem with sql in UploadXLS4:" + qr.getSql());
				}
				res1 = qr.getResult();

				// Tops den Mietvertraegen zuordnen
				Hashtable<String, MietvertragElement> topidsZuMietvertrag = new Hashtable<String, MietvertragElement>();
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

					if(zinslistenImport.getZlTypeConfig().isErroronemptyvertragsid() && mietvertragsnummer.equals(""))
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
					if(zinslistenImport.getZlTypeConfig().isErroronnewvertragsid() && zinslistenImport.getZlTypeConfig().isCreateonnewvertragsid() == false && mietvertragExists == false)
					{
						// Fehler nur ausgeben wenn kein neuer Mietvertrag angelegt wird
						zl.addError(Tr.t("textNoContractWithContractNumber1", session.getString("language")) + " " + mietvertragsnummer + " " + Tr.t("textNoContractWithContractNumber2", session.getString("language")) + " " + top, mietvertragsnummer, ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, top);
					}

					if(!fehlerabfrage)
					{

						// if Mietvertrag does not exists - create new one
						// PKO - 20150414 Keinen Mietvertrag fuer eine Leerstehung anlegen
						String mieter = ht.get("mieter").toString().toLowerCase();
						if(zinslistenImport.getZlTypeConfig().isCreateonnewvertragsid() && mietvertragExists == false && (!(checkLeerstandString(mieter))))
						{
							// Neuen Mietvertrag anlegen und neue Id in mietvertragsId schreiben

							if(PBInst == null)
							{
								Connector conn = null;
								conn = new Connector();
								PBInst = conn.getPageBuilder();
							}

							String tcode = PBInst.readTemplate("CIMS.mietvertrag");

							DynGenDataObj newMietvertragDgd = new DynGenDataObj();
							if(null == DAInst)
							{
								net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
								DAInst = conn.getDataAgent();
							}
							newMietvertragDgd.DAInst = DAInst;
							// build it with templatecode
							newMietvertragDgd.init(tcode, global, session);
							newMietvertragDgd.set("var.vertragid", mietvertragsnummer);

							// Store Mietvertrag
							mietvertragsId = DAInst.storeObject(newMietvertragDgd, "CIMS.mietvertrag", null, session);
							if(mietvertragsId == null)
							{
								mietvertragsId = "";
							}
							else
							{
								Hashtable<String, String> h = new Hashtable<String, String>();
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
								Vector newTops = new Vector();
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

					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}

					DynGenDataObj mietvertragDgd = (DynGenDataObj)DAInst.getObject(mietvertragsid, "");
					Vector topids = topsToWrite.get(mietvertragsid);

					String[] tids = new String[topids.size()];
					topids.toArray(tids);

					// check Mietvertragsflaeche VS Topsflaeche
					Vector<Hashtable<String, String>> resSumTops = new Vector<Hashtable<String, String>>();
					Hashtable<String, Object> argsSumTops = new Hashtable<String, Object>();
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

					QueryResult qrSumTops = DAInst.queryObjectWithResult(argsSumTops);
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

					DAInst.storeObject(mietvertragDgd, "CIMS.mietvertrag", mietvertragsid, session);
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
	public Zinsliste getZinsliste(String file, int index)
	{
		return getZinsliste(file, index, null, "");
	}

	/**
	 * Is used for Event Triggered Automated Zinslistenimport from Flexigrid Upload.
	 */
	public void processZinslistenuploadFromFlexigrid()
	{
		if(zlUploadObjectIds.size() == 0)
		{

			// START - this is only a temporary solution because eventengine has no flavour, hostname, ect.

			String domainname = (String)session.get("domainname");
			if(domainname == null || domainname.length() == 0)
			{
				domainname = (String)CfgSingleton.getInstance().get("domainname");
				if(domainname == null || domainname.length() == 0)
				{
					session.set("domainname", domainname);
				}
			}
			if(domainname == null || domainname.length() == 0)
			{
				session.set("domainname", "localhost");
			}
			String flavour = (String)session.get("flavour");
			if(flavour == null || flavour.length() == 0)
			{
				session.set("flavour", "icrsiw");
			}
			String currentview = (String)session.get("CURRENT_VIEW");
			if(currentview == null || currentview.length() == 0)
			{
				session.set("CURRENT_VIEW", "AUTOMATICIMPORT");
			}
			String view = (String)session.get("VIEW");
			if(view == null || view.length() == 0)
			{
				session.set("VIEW", "AUTOMATICIMPORT");
			}

			session.get("userid");

			// Get all Zinslistenupload Objects for Import
			// -> Alle Objekte die noch kein Importergebnis haben (Später noch deren startzeit/modified zeid länger als X Stunden her ist)

			Hashtable args = new Hashtable();
			Vector res = new Vector();
			args.put("TType", "ICRS.zinslisten.zinslistenupload");
			// fieldClause ... Felder zum holen ,-separiert
			args.put("fieldClause", "ID,ET0.gridimport,ET0.starttime,ET0.endtime");
			args.put("whereClause", " gridimport='1' and (endtime is null or endtime ='')");
			args.put("fetchSIZE", "1");

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
			if(null == DAInst)
			{
				DAInst = conn.getDataAgent();
			}

			try
			{
				QueryResult qr = DAInst.queryObjectWithResult(args);
				res = qr.getResult();
				for(int i = 0; i < res.size(); i++)
				{
					Hashtable h = (Hashtable)res.elementAt(0);
					if(h != null)
					{
						zlUploadObjectIds.add(h.get("ID"));
					}
				}

				if(zlUploadObjectIds != null && zlUploadObjectIds.size() > 0)
				{
					for(int i = 0; i < zlUploadObjectIds.size(); i++)
					{
						DynGenDataObj zlUploadDgd = (DynGenDataObj)DAInst.getObject(zlUploadObjectIds.get(i).toString(), "ICRS.zinslisten.zinslistenupload");

						// get old Upload Object

						if(FDAInst == null)
						{
							if(conn == null)
							{
								conn = new Connector();
							}
							FDAInst = conn.getFileDataAgent();
						}

						String file = (String)zlUploadDgd.get("var.file");
						if(file.startsWith("FILE_"))
						{
							file = file.substring(5);
						}
						Hashtable fparams = FDAInst.getParams(file);

						byte[] data = FDAInst.getObject(file);

						if(data == null)
						{
							// Delete unnecessary Object created by Flexigrid Zinslistenupload
							String zzUpliadOid = zlUploadObjectIds.get(i).toString();
							DAInst.deleteObject(zzUpliadOid, session);
							continue;
						}

						Hashtable vars = this.getSubs("var");
						for(Object key : vars.keySet())
						{
							try
							{
								String value = (String)zlUploadDgd.get("var." + key);
								if(value != null)
								{
									// pruefen ob selector oder wert
									String selector = this.getString("var." + key + ".SELECTOR");
									if(selector.length() > 0)
									{
										Hashtable opts = getValueMap(selector);
										if(opts.containsKey(value))
										{
											value = (String)opts.get(value);
										}
									}

									this.set("var." + key, value);
								}

							}
							catch(Exception ex)
							{
								debug.error(ex);
							}
						}

						if(null != mailtoamcfg && mailtoamcfg.trim().length() > 0)
						{
							if(mailtoamcfg.equals("1") || mailtoamcfg.equalsIgnoreCase("yes"))
							{
								this.set("var.assetmanagerinfo", "0");
							}
						}
						else
						{
							this.set("var.assetmanagerinfo", "1");
						}

						// set starttime
						Date dStart = new Date(System.currentTimeMillis());
						this.setDate("var.starttime", dStart);

						// run Zinslistenimport
						TemplateReader tr = TemplateReader.getInstance();
						parse(tr.getCodeForTemplate(this.getTemplateType()), global, session);

						// set endtime and store dgd again!
						Date dEnd = new Date(System.currentTimeMillis());
						this.setDate("var.endtime", dEnd);

						long runtimeinms = dEnd.getTime() - dStart.getTime();
						String runtime = "%d min, %d sec".formatted(TimeUnit.MILLISECONDS.toMinutes(runtimeinms), TimeUnit.MILLISECONDS.toSeconds(runtimeinms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runtimeinms)));
						this.set("var.runtime", runtime);

						String oid = (String)this.get("id");

						/* Handle Uplaod File start */

						if(fparams == null && data != null)
						{
							fparams = new Hashtable();
							fparams.put("size", "" + data.length);
							fparams.put("paramname", "zinslistenfile");
							fparams.put("name", file);
							String filetype = file.substring(file.indexOf(".")).replace(".", "");
							fparams.put("type", filetype);

							// if there are filetypes not defined yet -> need to add them later
							if(filetype.equals("csv") || filetype.equals("txt") || filetype.equals("dat") || filetype.equals("exp"))
							{
								fparams.put("Content-Type", "application/txt");
							}
							else
							{
								fparams.put("Content-Type", "application/vnd.ms-excel");
							}
						}

						// Create a unique file reference
						String inckey = "ZinslistenUploadFromGrid";
						Long ctr = CoolDataTool.generateUniqueSequence(inckey);

						String ftype = (String)fparams.get("type");

						String filereferencename = inckey + ctr + "." + ftype;

						// Put OID from new Object to file params
						fparams.put("OID", oid);

						zlfile = FDAInst.storeObject(filereferencename, data, fparams);

						this.set("var.file", "FILE_" + zlfile);

						/* Handle Uplaod File end */

						this.fixFileLink();
						DAInst.storeObject(this, this.getTemplateType(), oid, session);

						// Delete unnecessary Object created by Flexigrid Zinslistenupload
						String zzUpliadOid = zlUploadObjectIds.get(i).toString();
						DAInst.deleteObject(zzUpliadOid, session);
					}

					// Erst wenn alle Importiert sind, duerfen neue importiert werden!
					zlUploadObjectIds.clear();
				}
			}

			catch(Exception e)
			{
				debug.error(e);
				setImportStatus("3");
			}

		}
	}

	/**
	 * holen einer zinsliste mit index.
	 *
	 * @param file
	 *            the file
	 * @param index
	 *            the index
	 * @param quellsystemResult
	 *            the quellsystem result
	 * @param quellsystem
	 *            the quellsystem
	 * @return the zinsliste
	 */
	private Zinsliste getZinsliste(String file, int index, Vector quellsystemResult, String quellsystem)
	{
		Date d1 = new Date();

		Zinsliste zl = null;

		try
		{
			// ACHTUNG NEU RK 2007 10 24 config files geflavoured!!!
			String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
			cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
			if(cfg_zlimport == null)
			{
				set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			}

			String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
			cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
			if(cfg_currencyconfig == null)
			{
				set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
			}

			zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
			zinslistenImport.setLanguage(session.getString("language"));
			zinslistenImport.setEvaluateFormulas(evaluateFormulas);

			if(quellsystemResult != null && quellsystemResult.size() > 0)
			{
				// SAP READ - here
				zl = zinslistenImport.read(quellsystemResult, index, quellsystem);
			}
			else
			{

				if(FDAInst == null)
				{
					Connector conn = null;
					conn = new Connector();
					FDAInst = conn.getFileDataAgent();
				}
				Hashtable fparams = FDAInst.getParams(file);

				byte[] content = null;
				if(file.equals(cachedfile) && null != cachedcontent)
				{
					System.err.println("ZLU2: FILECONTENT CACHED!");
					content = cachedcontent;
				}
				else
				{
					System.err.println("ZLU2: READING FILE:" + file);
					content = FDAInst.getObject(file);
					cachedcontent = content;
					cachedfile = file;

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

					// added: filename for date-extraction
					zl = zinslistenImport.read(bis, thefilename, ftype, index);
					System.err.println("Zinsliste gelesen" + zl.getInfoText());
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
			System.out.println("Zinsliste lesen: " + (d2.getTime() - d1.getTime()) + " ms.");
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
	 * Creates the haus.
	 *
	 * @param zl
	 *            the zl
	 * @return the string
	 */
	public String createHaus(Zinsliste zl)
	{
		return getCrudService().createHaus(zl);
	}

	/**
	 * Update haus.
	 *
	 * @param zl
	 *            the zl
	 * @param oid
	 *            the oid
	 * @return the string
	 */
	public String updateHaus(Zinsliste zl, String oid)
	{
		return getCrudService().updateHaus(zl, oid);
	}

	/**
	 * Gets the top oid.
	 *
	 * @param name
	 *            the name
	 * @param hausid
	 *            the hausid
	 * @return the top oid
	 */
	public String getTopOID(String name, String hausid)
	{
		return getCrudService().getTopOID(name, hausid);
	}

	/**
	 * Fill top cache.
	 *
	 * @param topList
	 *            the top list
	 */
	public void fillTopCache(TopList topList)
	{
		getCacheService().fillTopCache(topList);
	}

	/**
	 * Empty last ZZ 4 top.
	 */
	private void emptyLastZZ4Top()
	{
		getCacheService().emptyLastZZ4Top();
	}

	/**
	 * Fill last ZZ 4 top.
	 *
	 * @param hausId
	 *            the haus id
	 */
	private void fillLastZZ4Top(String hausId)
	{
		getCacheService().fillLastZZ4Top(hausId);
	}

	/**
	 * Gets the ignore errors for haus.
	 *
	 * @param hausid
	 *            the hausid
	 * @return the ignore errors for haus
	 */
	public String getIgnoreErrorsForHaus(String hausid)
	{
		if(validationService == null)
		{
			validationService = new ZinslistenValidationService(session, debug, DAInst);
		}
		return validationService.getIgnoreErrorsForHaus(hausid);
	}

	/**
	 * 20110125 RK: klärt ob das Haus bereits verkauft wurde oder Verkaufsdatum vor Importdatum liegt hierher gehören ähnliche Prüfungen am Haus<br>
	 * 20130919 DN: dies methode muss immer <b>VOR zl.ignoreErrors(ignoreerrors)</b> aufgerufen werden, damit die fehler aus der Hauspruefung bei ignoreerros beruecksichtigt werden!
	 *
	 * @param hausid
	 *            the hausid
	 * @param zlnew
	 *            the zlnew
	 * @param zlTypeConfig
	 *            the zl type config
	 * @return the haus status
	 */
	public Zinsliste checkHausStatus(String hausid, Zinsliste zlnew, ZLTypeConfig zlTypeConfig)
	{
		if(validationService == null)
		{
			validationService = new ZinslistenValidationService(session, debug, DAInst);
		}
		boolean importhausstatusinaktiv = getBoolean("var.importhausstatusinaktiv", Boolean.FALSE);
		boolean rentrollimportaftersale = getBoolean("var.rentrollimportaftersale", Boolean.TRUE);
		flavour = (String)session.get("flavour");
		return validationService.checkHausStatus(hausid, zlnew, zlTypeConfig, importhausstatusinaktiv, rentrollimportaftersale, flavour);
	}

	/**
	 * Write ignore errors for haus.
	 *
	 * @param hausid
	 *            the hausid
	 * @param errs
	 *            the errs
	 * @return the string
	 */
	public String writeIgnoreErrorsForHaus(String hausid, String errs)
	{
		if(validationService == null)
		{
			validationService = new ZinslistenValidationService(session, debug, DAInst);
		}
		return validationService.writeIgnoreErrorsForHaus(hausid, errs);
	}

	/**
	 * Gets the cache service, initializing it lazily if needed.
	 *
	 * @return the cache service
	 */
	private Magic.IMS.ZLImport.ZinslistenCacheService getCacheService()
	{
		if(cacheService == null)
		{
			cacheService = new Magic.IMS.ZLImport.ZinslistenCacheService(FDAInst, session, debug);
		}
		return cacheService;
	}

	/**
	 * Gets the mapping service, initializing it lazily if needed.
	 *
	 * @return the mapping service
	 */
	private Magic.IMS.ZLImport.ZinslistenMappingService getMappingService()
	{
		if(mappingService == null)
		{
			mappingService = new Magic.IMS.ZLImport.ZinslistenMappingService(FDAInst, session, global, debug);
		}
		return mappingService;
	}

	/**
	 * Gets the CRUD service, initializing it lazily if needed.
	 *
	 * @return the CRUD service
	 */
	private Magic.IMS.ZLImport.ZinslistenDatabaseCRUDService getCrudService()
	{
		if(crudService == null)
		{
			crudService = new Magic.IMS.ZLImport.ZinslistenDatabaseCRUDService(FDAInst, session, global, debug, this);
		}
		return crudService;
	}

	/**
	 * Creates the top.
	 *
	 * @param ht
	 *            the ht
	 * @param oid_haus
	 *            the oid_haus
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj createTop(Hashtable ht, String oid_haus, Zinsliste zl)
	{
		return getCrudService().createTop(ht, oid_haus, zl);
	}

	/**
	 * Creates the stellplatz.
	 *
	 * @param ht
	 *            the ht
	 * @param oid_haus
	 *            the oid_haus
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj createStellplatz(Hashtable ht, String oid_haus, Zinsliste zl)
	{
		return getCrudService().createStellplatz(ht, oid_haus, zl);
	}

	/**
	 * haus oder stellplatz anlegen.
	 *
	 * @param ht
	 *            werte aus dem zinslistenreader
	 * @param oid_haus
	 *            haus id
	 * @param is_a_top
	 *            top oder stellplatz
	 * @param zl
	 *            die zinsliste
	 * @return das top oder den erzeugten stellplatz
	 */
	public DynGenDataObj createTopOrStellplatz(Hashtable ht, String oid_haus, boolean is_a_top, Zinsliste zl)
	{
		return getCrudService().createTopOrStellplatz(ht, oid_haus, is_a_top, zl);
	}

	/**
	 * Top oder Stellplatz updaten
	 *
	 * bei neuem upload letztstand.
	 *
	 * @param oid_top
	 *            the oid_top
	 * @param ht
	 *            the ht
	 * @param zl
	 *            the zl
	 * @param is_a_top
	 *            the is_a_top
	 * @param mycbst
	 *            the mycbst
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj updateTopOrStellplatz(String oid_top, Hashtable ht, Zinsliste zl, boolean is_a_top, CoolBulkStoreTool mycbst)
	{
		return getCrudService().updateTopOrStellplatz(oid_top, ht, zl, is_a_top, mycbst);
	}

	/**
	 * Top dem Haus hinzufügen mittels top OID und haus OID.
	 *
	 * @param oid_top
	 *            the oid_top
	 * @param oid_haus
	 *            the oid_haus
	 * @return true, if successful
	 */
	public boolean addTopToHaus(String oid_top, String oid_haus)
	{
		return getCrudService().addTopToHaus(oid_top, oid_haus);
	}

	/**
	 * mehrere Tops dem Haus hinzufügen mittels top OIDs als Hashtable und haus OID.
	 *
	 * @param oids
	 *            the oids
	 * @param oid_haus
	 *            the oid_haus
	 * @return true, if successful
	 */
	public boolean addTopsToHaus(Hashtable oids, String oid_haus)
	{
		return getCrudService().addTopsToHaus(oids, oid_haus);
	}

	/**
	 * mehrere Tops dem Gebaeude hinzufügen mittels top OIDs als Hashtable und haus OID.
	 *
	 * @param topszugebaeuden
	 *            the topszugebaeuden
	 * @return true, if successful
	 */
	public boolean addTopsToGebaeude(Hashtable topszugebaeuden)
	{
		return getCrudService().addTopsToGebaeude(topszugebaeuden);
	}

	/**
	 * Zinszeile erzeugen.
	 *
	 * @param ht
	 *            zinszeilen info vom reader
	 * @param zl
	 *            zinsliste
	 * @param oid_top
	 *            top-id
	 * @param zz_oid
	 *            zinszeilen id
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj createZZ(Hashtable ht, Zinsliste zl, String oid_top, String zz_oid)
	{
		return getCrudService().createZZ(ht, zl, oid_top, zz_oid);
	}

	/**
	 * Write all in zinslistenimportconfig.xml defined Values to Mietvertrag
	 *
	 * @param mietvertragDgd
	 *            the mietvertrag dgd
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 */
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

	/**
	 * JPI -- If Top has a rental start in the last 2 months and in the rent list it is a vacancy do not ovverride the top values Vorhandene Top Werte auf zinszeile schreiben ...
	 *
	 * @param topdgd
	 *            topdgd
	 * @param zzdgd
	 *            the dgd
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 *         written dgd
	 */
	public DynGenDataObj writeCommonValues(DynGenDataObj topdgd, DynGenDataObj zzdgd, Zinsliste zl)
	{
		return getCrudService().writeCommonValues(topdgd, zzdgd, zl);
	}

	/**
	 * gemeinsam werte in top und zinszeile schreiben ...
	 *
	 * @param ht
	 *            the hashtable from import
	 * @param dgd
	 *            the dgd, top or zz
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 *
	 */
	public DynGenDataObj writeCommonValues(Hashtable ht, DynGenDataObj dgd, Zinsliste zl)
	{
		return getCrudService().writeCommonValues(ht, dgd, zl);
	}

	/**
	 * Gets the original currency value.
	 *
	 * @param key
	 *            to check
	 * @param ht
	 *            Hashtable with importvalues
	 * @return null if no original Currency Value
	 */
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
	 *            zinszeile
	 * @return dgd with change field
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

	/**
	 * Gets the latest MV daten from mietvertrag.
	 *
	 * @param dgdId
	 *            the dgd id
	 * @return the latest MV daten from mietvertrag
	 */
	private String getLatestMVDatenFromMietvertrag(String dgdId)
	{
		try
		{
			String[] oids_top = new String[1];
			oids_top[0] = dgdId;

			for(int i = 0; i < oids_top.length; i++)
			{
				if(null == topsCache)
				{
					topsCache = new Hashtable();
				}
				if(!topsCache.containsKey(oids_top[i]))
				{
					// topscache irgendwie kapputt?
					debug.error(this, "bad top im topscache: " + oids_top[i]);
				}
			}

			Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();

			Hashtable<String, Object> args = new Hashtable<String, Object>();

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

			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}
			QueryResult qr = DAInst.queryObjectWithResult(args);
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

	/**
	 * get latest ZZ fuer letzte indexbasis und indexart.
	 *
	 * @param adresse
	 *            the adresse
	 * @param topname
	 *            the topname
	 * @return get latest ZZ indexbasis und indexart
	 */
	private Hashtable<String, String> getLatestIndexDatumFromZZ(String adresse, String topname, String jahr, String monat, String mietvertragvonZL)
	{

		Hashtable<String, String> indexHashtoTop = new Hashtable<String, String>();
		try
		{
			Vector<Hashtable<String, String>> res1 = new Vector<Hashtable<String, String>>();
			Hashtable<String, Object> args = new Hashtable<String, Object>();

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
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}
			QueryResult qr = DAInst.queryObjectWithResult(args);
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
			Hashtable<String, Object> args2 = new Hashtable<String, Object>();
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
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}
			QueryResult qr2 = DAInst.queryObjectWithResult(args2);
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

	/**
	 * schreibt in die Felder .OV original Value, .OC original currency und .OX original exchange rate ist einer der werte nicht befuellt dann nicht
	 *
	 * @param targetFieldName
	 *            zielfeldname
	 * @param sourceFieldName
	 *            quellfeldname
	 * @param target
	 *            ziel dgd
	 * @param source
	 *            quell dgd
	 */
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

	/**
	 * schreibt in die Felder .OV original Value, .OC original currency und .OX original exchange rate ist einer der werte nicht befuellt dann nicht
	 *
	 * @param targetFieldName
	 *            zielfeldname
	 * @param sourceFieldName
	 *            quellfeldname
	 * @param target
	 *            ziel dgd
	 * @param source
	 *            quell hash
	 */
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

	/**
	 * Removes the original currency values.
	 *
	 * @param fieldname
	 *            the fieldname
	 * @param dgd
	 *            the dgd
	 */
	private void removeOriginalCurrencyValues(String fieldname, DynGenDataObj dgd)
	{
		dgd.del(fieldname + ".OV");
		dgd.del(fieldname + ".OC");
		dgd.del(fieldname + ".OX");
	}

	/**
	 * versucht aus 20 20% zu machen und aus 0.2 ebenso
	 *
	 * @param umsatzvalue
	 *            der gelesene wert
	 * @param dgd
	 *            das dgd zb top
	 * @return korrigierten wert
	 */
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

	/**
	 * Gets the long.
	 *
	 * @param value
	 *            the value
	 * @return the long
	 */
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

	/**
	 * Zinszeilen OID holen
	 *
	 * mittels top OID, jahr und monat.
	 *
	 * @param oid_top
	 *            the oid_top
	 * @param jahr
	 *            the jahr
	 * @param monat
	 *            the monat
	 * @return the zZOID
	 */
	public String getZZOID(String oid_top, String jahr, String monat)
	{
		// Delegate to database service
		if(databaseService == null)
		{
			databaseService = new Magic.IMS.ZLImport.ZinslistenDatabaseService(session, debug, DAInst, this);
		}
		return databaseService.getZZOID(oid_top, jahr, monat);
	}

	/**
	 * Delete zins zeilen.
	 *
	 * @param topoids
	 *            the topoids
	 * @param jahr
	 *            the jahr
	 * @param monat
	 *            the monat
	 * @return true, if successful
	 */
	public boolean deleteZinsZeilen(String[] topoids, String jahr, String monat)
	{
		return getCrudService().deleteZinsZeilen(topoids, jahr, monat);
	}

	/**
	 * Zinszeilen fuer bestimmte Tops, Monat Jahr holen.
	 *
	 * @param topoids
	 *            the topoids
	 * @param jahr
	 *            the jahr
	 * @param monat
	 *            the monat
	 * @return the zins zeilen
	 */
	public Hashtable<String, Hashtable<String, String>> getZinsZeilen(String[] topoids, String jahr, String monat)
	{
		if(null == topoids || topoids.length == 0)
		{
			log("Abfrgae nach Zinszeilen ohne angegebene Tops.");
			return new Hashtable();
		}

		zinsZeilenCache = null;
		zinsZeilenCache = new Hashtable();
		if(null != monat && monat.startsWith("0"))
		{
			if(monat.length() == 2)
			{
				monat = monat.substring(1, 1);
			}
		}
		else if(null == monat)
		{
			monat = "";
		}
		// System.err.println();
		Hashtable args = new Hashtable();
		Vector<Hashtable<String, String>> res = new Vector();
		args.put("TType", "CIMS.zinszeile");
		// fieldClause ... Felder zum holen ,-separiert
		args.put("fieldClause", "DOB.ID zzid,mieter,nutzung,nfl,leerfl,hauptmietzins,betriebskosten ,reparaturfond,name,DDT1.ID topid");
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
		if(null == DAInst)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			DAInst = conn.getDataAgent();
		}
		// java.util.Date start_time = new java.util.Date();

		try
		{
			res = DAInst.queryObject(args);
		}
		catch(Exception x)
		{
			debug.log(x);
			debug.error(x);
		}
		// java.util.Date end_time = new java.util.Date();
		// long run_time = end_time.getTime() - start_time.getTime();

		Hashtable<String, Hashtable<String, String>> top2zz = new Hashtable<String, Hashtable<String, String>>();
		// System.err.println("ZLU2: res size is "+res.size());
		for(int x = 0; x < res.size(); x++)
		{
			Hashtable<String, String> h = res.elementAt(x);
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
		// System.err.println("ZLU2: FOUND "+res.size()+" ZZ "+top2zz.toString());
		// cache befuellen!!!!
		if(zinsZeilenCache != null && zinsZeilenCache.size() > 0)
		{
			// System.err.println("ZLU2: FILLING ZINSZEILEN CACHE " + zinsZeilenCache.size());

			// cache befuellen !!!
			try
			{
				zinsZeilenCache = DAInst.getObjects(zinsZeilenCache, "");
				// System.err.println("ZLU2: cached " + zinsZeilenCache.size() + " zinszeilen.");
			}
			catch(Exception xx)
			{
				debug.log(xx);
			}
		}
		return top2zz;
	}

	/**
	 * Gets the zins zeilen for name.
	 *
	 * @param topoids
	 *            the topoids
	 * @param jahr
	 *            the jahr
	 * @param monat
	 *            the monat
	 * @return the zins zeilen for name
	 */
	public Hashtable getZinsZeilenForName(String[] topoids, String jahr, String monat)
	{
		if(null == topoids || topoids.length == 0)
		{
			log("Abfrage nach Zinszeilen ohne angegebene Tops.");
			return new Hashtable();
		}

		Hashtable args = new Hashtable();
		Vector res = new Vector();
		args.put("TType", "CIMS.zinszeile");
		// fieldClause ... Felder zum holen ,-separiert

		args.put("fieldClause", "DOB.ID zzid,mieter,nutzung,nfl,leerfl,hauptmietzins,betriebskosten ,reparaturfond,name,DDT1.ID topid,DDT1.name topname");
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
		if(null == DAInst)
		{
			net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
			DAInst = conn.getDataAgent();
		}
		// java.util.Date start_time = new java.util.Date();

		try
		{
			res = DAInst.queryObject(args);
		}
		catch(Exception x)
		{
			debug.log(x);
			debug.error(x);
		}
		// java.util.Date end_time = new java.util.Date();
		// long run_time = end_time.getTime() - start_time.getTime();

		Hashtable top2zz = new Hashtable();
		// System.err.println("ZLU2: res size is "+res.size());
		for(int x = 0; x < res.size(); x++)
		{
			Hashtable h = (Hashtable)res.elementAt(x);
			if(h != null)
			{
				String topname = (String)h.get("topname");
				String zzid = (String)h.get("zzid");
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
	 * Send mail.
	 *
	 * @return the string
	 */
	public String sendMailWithErrors()
	{
		if(mailService == null)
		{
			mailService = new Magic.IMS.ZLImport.ZinslistenMailService(
				this, session, debug, FDAInst, PBInst, bcc_emails, mailtoamcfg);
		}
		return mailService.sendMailWithErrors();
	}

	/**
	 * sends mails to assetmanagers with infos for their objects.
	 *
	 * @param mailinglist
	 *            Hashtable
	 * @param subject
	 *            the subject
	 */
	public void sendMailToAssetmanager(Hashtable<String, String> mailinglist, String subject)
	{
		if(mailService == null)
		{
			mailService = new Magic.IMS.ZLImport.ZinslistenMailService(
				this, session, debug, FDAInst, PBInst, bcc_emails, mailtoamcfg);
		}
		mailService.sendMailToAssetmanager(mailinglist, subject);
	}

	/**
	 * Creates the message.
	 *
	 * @param from
	 *            the from
	 * @param to_addresses
	 *            the to addresses
	 * @param cc_addresses
	 *            the cc addresses
	 * @param bcc_addresses
	 *            the bcc addresses
	 * @param subject
	 *            the subject
	 * @param body
	 *            the body
	 * @param attachments
	 *            the attachments
	 */
	public static void createMessage(String from, Vector to_addresses, Vector cc_addresses, Vector bcc_addresses, String subject, String body, List<File> attachments)
	{
		try
		{
			Message message = new MimeMessage(Session.getInstance(System.getProperties()));
			message.setFrom(new InternetAddress(from));
			String to = "";

			if(to_addresses != null && to_addresses.size() > 0)
			{
				for(int i = 0; i < to_addresses.size(); i++)
				{
					if(to.length() == 0)
					{
						to = to_addresses.get(i).toString();
					}
					message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to_addresses.get(i).toString()));
				}
			}
			if(cc_addresses != null && cc_addresses.size() > 0)
			{
				for(int i = 0; i < cc_addresses.size(); i++)
				{
					message.addRecipients(Message.RecipientType.CC, InternetAddress.parse(cc_addresses.get(i).toString()));
				}
			}
			if(bcc_addresses != null && bcc_addresses.size() > 0)
			{
				for(int i = 0; i < bcc_addresses.size(); i++)
				{
					message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(bcc_addresses.get(i).toString()));
				}
			}
			// message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
			message.setSubject(subject);
			// create the message part
			// MimeBodyPart content = new MimeBodyPart();
			// fill message
			message.setContent(body, "text/html; charset=utf-8");
			// content.setText(body);
			message.setSentDate(new Date());

			// Multipart multipart = new MimeMultipart();
			// multipart.addBodyPart(content);
			// add attachments

			// for(File file : attachments)
			// {
			// MimeBodyPart attachment = new MimeBodyPart();
			// DataSource source = new FileDataSource(file);
			// attachment.setDataHandler(new DataHandler(source));
			// attachment.setFileName(file.getName());
			// multipart.addBodyPart(attachment);
			// }
			// // integration
			// message.setContent(multipart);

			// store file
			// Timestamp + Random Number

			File directory = new File(String.valueOf((String)CfgSingleton.getInstance().get("log_directory") + System.getProperty("file.separator") + "MailBackup"));
			if(!directory.exists())
			{
				directory.mkdir();
			}

			int randomNum = 100 + (int)(ThreadLocalRandom.current().nextDouble() * 500);
			String actualTime = new SimpleDateFormat("_yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
			String filepath = (String)CfgSingleton.getInstance().get("log_directory") + System.getProperty("file.separator") + "MailBackup" + System.getProperty("file.separator");
			String filename = to.replaceAll("@", "_") + "_" + subject.replaceAll(" ", "_").replaceAll(":", "-") + "_" + actualTime + "_" + randomNum + ".eml";

			message.writeTo(new FileOutputStream(new File(filepath + filename)));
		}
		catch(MessagingException ex)
		{
			BugMe.getInstance().error(ex);
		}
		catch(IOException ex)
		{
			BugMe.getInstance().error(ex);
		}
	}

	/**
	 * Send mail to assetmanager single object.
	 *
	 * @param mailAndName
	 *            the mail and name
	 * @param haus
	 *            the haus
	 */
	public void sendMailToAssetmanagerSingleObject(String mailAndName, String haus)
	{

		// Nur wenn Parameter sendmailonlyonchange=1 gesetzt ist Mail an die AMs schicken
		boolean sendmailonlyonchange = this.getBoolean("var.sendmailonlyonchange", true);
		if(sendmailonlyonchange)
		{
			boolean hasChanges = false;
			if(zlprotocol.getMailMsg().length() > 0)
			{
				String[] protokoll = zlprotocol.getMailMsg().split("<br>");
				String tmp = Arrays.toString(protokoll);
				for(int i = 0; i < tmp.length(); i++)
				{
					if(!tmp.toLowerCase().contains("keine änderungen") && !tmp.toLowerCase().contains("no change for object"))
					{
						hasChanges = true;
						break;
					}
				}
			}
			// Keine Aenderung -> kein Mail schicken
			if(!hasChanges)
			{
				return;
			}
		}

		// keine Eintraege -> keine Mail mit Änderungen
		if(zlprotocol.isCSVEmpty())
		{
			return;
		}

		StringBuffer error = new StringBuffer();

		// pko@metamagix.net;PKO assetmanager

		String mail = "";
		String assetmanager = "";

		if(mailAndName.contains(";"))
		{
			String[] tmp = mailAndName.split(";");
			mail = tmp[0];
			assetmanager = tmp[1];
		}
		else
		{
			mail = mailAndName;
		}

		if(mail.indexOf("@") < 0)
		{
			return;
		}

		String smtp = (String)session.get("site.smtphost");
		if(null == smtp || smtp.equals(""))
		{
			smtp = (String)CfgSingleton.getInstance().get("SMTP_HOST");
		}
		if(null == smtp)
		{
			smtp = "";
		}
		if(smtp.equals(""))
		{
			smtp = "localhost";
			debug.error(this, "no smtp host (SMTP_HOST) defined in essence.cfg, using localhost ");
		}

		CfgSingleton cfg = CfgSingleton.getInstance();

		String from = (String)this.get("var.from");
		if(null == from || from.equals(""))
		{
			from = (String)session.get("site.fromemail");
		}
		if(null == from || from.equals(""))
		{
			from = (String)cfg.get("fromemail");
		}
		// DEPRECIATED
		if(null == from || from.equals(""))
		{
			from = (String)cfg.get("MAILADDRESS");
		}
		// DEPRECIATED
		if(null == from || from.equals(""))
		{
			from = (String)cfg.get("FROM_MAILADDRESS");
		}
		if(null == from || from.equals(""))
		{
			from = "root@localhost";
			error.append(Tr.t("textErrorNoFrom1", session.getString("language")) + Tr.t("textErrorNoFrom2", session.getString("language")) + " ");
			debug.error(this, "no from (fromemail) defined in essence.cfg," + "no from (fromemail) defined in Site, using root ");
		}

		Vector to_addresses = new Vector();
		new Vector();

		// Beistrich wird weiter oben dazugegeben
		mail = mail + bcc_emails;

		if(mail.contains(","))
		{
			String[] tmp = mail.split(", ");

			for(int i = 0; i < tmp.length; i++)
			{
				to_addresses.add(tmp[i].trim());
			}
		}
		else
		{
			to_addresses.add(mail);
		}

		// if(!mail.equals(""))
		// {
		//
		// to_addresses.addElement(mail);
		// }

		Date now = new Date();
		GregorianCalendar crecalendar = new GregorianCalendar();
		crecalendar.setTime(now);

		String filenameforemail = this.get("var.filename").toString();
		int ee = shortinfo.length() - 1;
		String subject = Tr.t("textAssetmanagerMailSubject", session.getString("language")) + " " + haus + " " + shortinfo.substring(0, ee) + " - (" + filenameforemail + ") " + " (" + net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()) + ")";

		String name = "";

		if(assetmanager.length() > 0)
		{
			name = assetmanager;
		}
		else
		{
			name = mail;
		}

		String salutation = Tr.t("textSalutationAssetmanagerNotify", session.getString("language")) + " " + name + "<br><br>";
		salutation = salutation + Tr.t("textAssetmanagerNotify", session.getString("language")) + "<br>";

		String lang = session.getString("language").toUpperCase();
		if(!lang.equals("EN")) lang = "DE";

		String msgtext = zlprotocol.getMailMsg();
		if(CfgSingleton.getInstance().hasLLMModule())
		{ // make a message summary
			// initialize client
			String username = "";
			if(null != session) username = session.getString("var.username");
			CoolLLMWrapperTool cllw = new CoolLLMWrapperTool();
			net.metamagix.essence.LLM.Message msgtextSummaryMsg = cllw.getSummary(msgtext, lang, username, 100);
			String msgtextSummary = msgtextSummaryMsg.getContent().toString();
			if(null != msgtextSummary)
			{
				if(lang.equals("DE"))
				{
					msgtext = "AI-Zusammenfassung:\n</br>" + msgtextSummary + "\n\n</br></br>Vollständiger Bericht:\n</br></br>" + msgtext;
				}
				else
				{
					msgtext = "AI Summary:\n</br>" + msgtextSummary + "\n\n</br></br>Full Report:\n</br></br>" + msgtext;
				}
			}
		}

		String value = salutation + msgtext;

		LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0023", subject, value, "standardemail.html", session, this, null);
		lpmw.sendTo(to_addresses);

		// value = "<html><head></head><body>" + value + "</body></html>";

		// MailThread mt = new MailThread(to_addresses, subject, value, from, smtp, from, session);
		// // only the header, parts hav to do it themselves
		// mt.setCharacterEncoding("UTF-8");
		// mt.setContentType(MailingModule.HTML_CONTENT);
		// mt.start();
	}

	/**
	 * Send mail.
	 *
	 * @return the string
	 */
	public void sendMailWithChanges()
	{
		if(mailService == null)
		{
			mailService = new Magic.IMS.ZLImport.ZinslistenMailService(
				this, session, debug, FDAInst, PBInst, bcc_emails, mailtoamcfg);
		}
		mailService.sendMailWithChanges();
	}

	/**
	 * Send mail with results to the user.
	 *
	 * @param results
	 *            mail message
	 */
	public void sendMailToExcecutor(String results)
	{

		String useremail = session.getString("var.useremail");
		if(session.getBoolean("var.emailjanein", Boolean.TRUE))
		{

			if(useremail.indexOf("@") < 0)
			{
				return;
			}

			String smtp = (String)session.get("site.smtphost");
			if(null == smtp || smtp.equals(""))
			{
				smtp = (String)CfgSingleton.getInstance().get("SMTP_HOST");
			}
			if(null == smtp)
			{
				smtp = "";
			}
			if(smtp.equals(""))
			{
				smtp = "localhost";
				debug.error(this, "no smtp host (SMTP_HOST) defined in essence.cfg, using localhost ");
			}

			CfgSingleton cfg = CfgSingleton.getInstance();

			String from = (String)this.get("var.from");
			if(null == from || from.equals(""))
			{
				from = (String)session.get("site.fromemail");
			}
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("fromemail");
			}
			// DEPRECIATED
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("MAILADDRESS");
			}
			// DEPRECIATED
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("FROM_MAILADDRESS");
			}
			if(null == from || from.equals(""))
			{
				from = "root@localhost";
				debug.error(this, "no from (fromemail) defined in essence.cfg," + "no from (fromemail) defined in Site, using root ");
			}

			Vector to_addresses = new Vector();
			Vector cc_addresses = new Vector();
			// Vector cc_addresses = new Vector();

			useremail = useremail + bcc_emails;

			if(!useremail.equals(""))
			{
				if(useremail.contains(","))
				{
					String[] tmp = useremail.split(",");

					for(int i = 0; i < tmp.length; i++)
					{
						cc_addresses.add(tmp[i].trim());
					}
				}
				else
				{
					to_addresses.add(useremail);
				}
			}

			String filenameforemail = this.getString("var.filename");

			shortinfo = shortinfo.replaceAll("\\n", "").trim();
			String subject = "Automatischer Zinslisten Import " + shortinfo + " - (" + filenameforemail + ")";

			Vector parts = new Vector();

			// String mydomain = (String)session.get("domainid");

			// MailThread mt = new MailThread(to_addresses, subject, results, from, smtp, from, session);
			debug.log("send email" + results);
			// only the header, parts hav to do it themselves
			// mt.setCharacterEncoding("UTF-8");
			// mt.setContentType(MailingModule.HTML_CONTENT);
			// mt.start();

			LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0025", subject, results.toString(), "standardemail.html", session, this, null);

			lpmw.sendTo(to_addresses);
		}
		else
		{
			BugMe.getInstance("maillog").log("Not sending upload email to executor, to " + useremail + ", user turned off email!");
		}
	}

	/**
	 * Send mail with errors to the user, if the file cannot be read (by automated
	 * import).
	 *
	 * @param subject
	 *            the subject
	 * @param message
	 *            the message
	 */
	public void sendMailWithErrorsToExcecutor(String subject, String message)
	{

		String useremail = session.getString("var.useremail");
		useremail = useremail + bcc_emails;

		if(session.getBoolean("var.emailjanein", Boolean.TRUE))
		{

			if(useremail.indexOf("@") < 0)
			{
				return;
			}

			String smtp = (String)session.get("site.smtphost");
			if(null == smtp || smtp.equals(""))
			{
				smtp = (String)CfgSingleton.getInstance().get("SMTP_HOST");
			}
			if(null == smtp)
			{
				smtp = "";
			}
			if(smtp.equals(""))
			{
				smtp = "localhost";
				debug.error(this, "no smtp host (SMTP_HOST) defined in essence.cfg, using localhost ");
			}

			CfgSingleton cfg = CfgSingleton.getInstance();

			String from = (String)this.get("var.from");
			if(null == from || from.equals(""))
			{
				from = (String)session.get("site.fromemail");
			}
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("fromemail");
			}
			// DEPRECIATED
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("MAILADDRESS");
			}
			// DEPRECIATED
			if(null == from || from.equals(""))
			{
				from = (String)cfg.get("FROM_MAILADDRESS");
			}
			if(null == from || from.equals(""))
			{
				from = "root@localhost";
				debug.error(this, "no from (fromemail) defined in essence.cfg," + "no from (fromemail) defined in Site, using root ");
			}

			Vector to_addresses = new Vector();
			// Vector cc_addresses = new Vector();

			if(!useremail.equals(""))
			{
				if(useremail.contains(","))
				{
					String[] tmp = useremail.split(",");

					for(int i = 0; i < tmp.length; i++)
					{
						to_addresses.add(tmp[i].trim());
					}
				}
				else
				{
					to_addresses.add(useremail);
				}
			}

			Vector parts = new Vector();

			debug.log("send email" + message);

			LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0028", subject, message.toString(), "standardemail.html", session, this, null);

			lpmw.sendTo(to_addresses);

		}
		else
		{
			BugMe.getInstance("maillog").log("Not sending upload email to executor, to " + useremail + ", user turned off email!");
		}
	}

	/**
	 * Gets the file part.
	 *
	 * @param OID
	 *            the oID
	 * @return the file part
	 */
	MimeBodyPart getFilePart(String OID)
	{
		if(mailService == null)
		{
			mailService = new Magic.IMS.ZLImport.ZinslistenMailService(
				this, session, debug, FDAInst, PBInst, bcc_emails, mailtoamcfg);
		}
		return mailService.getFilePart(OID);
	}

	/**
	 * Log.
	 *
	 * @param text
	 *            the text
	 */
	private void log(String text)
	{
		String ses_username = (String)session.get("var.username");
		if(null == ses_username)
		{
			ses_username = "unknown";
		}
		String hostip = (String)session.get("hostip");
		if(null == hostip)
		{
			hostip = "unknown";
		}
		cimslog.log_entry(ses_username + " " + text + " [" + hostip + "]");
	}

	/**
	 * Read liste.
	 *
	 * @param file
	 *            the file
	 * @return the vector
	 */
	private Vector readListe(String file)
	{
		Vector liste = new Vector();
		// ---------------------------------------------
		// LISTEN LESEN
		// ---------------------------------------------
		try
		{
			if(FDAInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				FDAInst = conn.getFileDataAgent();
			}
			Hashtable zlfparams = FDAInst.getParams(file);

			byte[] content = null;
			if(file.equals(cachedfile) && null != cachedcontent)
			{
				// System.err.println("ZLU2: FILECONTENT CACHED!");
				content = cachedcontent;
			}
			else
			{
				// System.err.println("ZLU2: READING FILE:" + file);
				content = FDAInst.getObject(file);
				cachedcontent = content;
				cachedfile = file;

				// ACHTUNG NEU RK 2007 10 24 config files geflavoured!!!
				String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
				cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
				if(cfg_zlimport == null)
				{
					set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
					debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
				}

				String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
				cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
				if(cfg_currencyconfig == null)
				{
					set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
					debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
				}
				// ACHTUNG NEU RK 2007 10 24 config files geflavoured!!!

				zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
				zinslistenImport.setLanguage(session.getString("language"));
				zinslistenImport.setEvaluateFormulas(evaluateFormulas);

			}

			ByteArrayInputStream bis = new ByteArrayInputStream(content);

			// get configfile

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

			// added: filename for date-extraction
			liste = zinslistenImport.getZinslistenInFile(bis, thefilename, ftype);
			// Check if the file attached is without any data and provide message to the user
			if(liste.size() == 0)
			{
				zlprotocol.appendHtmlErr("<h2>" + Tr.t("noDataMessage", session.getString("language")) + "</h2>");
				session.set("CURRENT_VIEW", "ERRORQUEST");
				set("var.importstop", "1");
				set("var.errorcode", zlprotocol.getHtmlErr());
				return null;
			}

			// Set the config
			// maiordomus1000mieter
			xc.getXMLConfig("hausverwaltung", zinslistenImport.getZlTypeConfig().getName() + "mieter");
			try
			{
				Vector fehlerliste = zinslistenImport.errors;
				if(fehlerliste.size() > 0)
				{
					for(int z = 0; z < fehlerliste.size(); z++)
					{
						// System.err.println("ZLU2: ERROR "+(String)fehlerliste.elementAt(z));
						zlprotocol.appendHtmlErr((String)fehlerliste.elementAt(z) + "<br><br>\n");
					}
				}
			}
			catch(Exception xx)
			{
				debug.error(xx);
			}
			// wenn keine zinsliste - vector ist leer
			// System.err.println("ZLU2: In "+file+" sind "+liste.size()+" Zinslisten.");
		}
		catch(Exception zir)
		{
			// GAR NICHT GUT
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");

			set("var.importstop", "1");
			set("var.errorcode", zir.getMessage());
			set("var.errorcodetxt", zir.getMessage());
			debug.error(zir);
			debug.error("readListe in UploadXLS4:", zir);
			this.set("dirty", "yes");
			return null;
		}

		if(null == liste || 0 == liste.size())
		{
			// GAR NICHT GUT
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			set("var.importstop", "1");
			set("var.errorcode", zlprotocol.getHtmlErr());
			set("var.errorcodetxt", Tr.t("textListUnreadable", session.getString("language")));
			this.set("dirty", "yes");
			return null;
		}
		return liste;
	}

	/**
	 * Read liste.
	 *
	 * @param quellsystemResult
	 *            the quellsystem result
	 * @param quellsystem
	 *            the quellsystem
	 * @return the vector
	 */
	private Vector readQuellsystemListe(Vector quellsystemResult, String quellsystem)
	{
		Vector liste = new Vector();
		// ---------------------------------------------
		// LISTEN LESEN
		// ---------------------------------------------
		try
		{
			if(FDAInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				FDAInst = conn.getFileDataAgent();
			}
			String cfg_zlimport = (String)CfgSingleton.getInstance().get("ZINSLISTENIMPORTCONFIG");
			cfg_zlimport = CoolStringTool.getFlavouredFilename(cfg_zlimport, session);
			if(cfg_zlimport == null)
			{
				set("var.errorcode", Tr.t("textNoZINSLISTENIMPORTCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENIMPORTCONFIG gefunden");
			}

			String cfg_currencyconfig = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
			cfg_currencyconfig = CoolStringTool.getFlavouredFilename(cfg_currencyconfig, session);
			if(cfg_currencyconfig == null)
			{
				set("var.errorcode", Tr.t("textNoZINSLISTENCURRENCYCONFIG", session.getString("language")));
				debug.error(this, "Keine ZINSLISTENCURRENCYCONFIG gefunden");
			}

			zinslistenImport = new ZinslistenImport(cfg_zlimport, cfg_currencyconfig, debug, session);
			zinslistenImport.setLanguage(session.getString("language"));
			zinslistenImport.setEvaluateFormulas(evaluateFormulas);

			// added: filename for date-extraction
			liste = zinslistenImport.getZinslistenInFile(null, "", "", quellsystemResult, quellsystem);

			// Set the config
			// maiordomus1000mieter
			String zltypeName = zinslistenImport.getZlTypeConfig().getName();
			this.set("var.zltypename", zltypeName);
			xc.getXMLConfig("hausverwaltung", zltypeName + "mieter");

			try
			{
				Vector fehlerliste = zinslistenImport.errors;
				if(fehlerliste.size() > 0)
				{
					for(int z = 0; z < fehlerliste.size(); z++)
					{
						// System.err.println("ZLU2: ERROR "+(String)fehlerliste.elementAt(z));
						zlprotocol.appendHtmlErr((String)fehlerliste.elementAt(z) + "<br><br>\n");
					}
				}
			}
			catch(Exception xx)
			{
				debug.error(xx);
			}
			// wenn keine zinsliste - vector ist leer
			// System.err.println("ZLU2: In "+file+" sind "+liste.size()+" Zinslisten.");
		}
		catch(Exception zir)
		{
			// GAR NICHT GUT
			debug.error(zir);
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			set("var.errorcode", zir.getMessage());
			set("var.errorcodetxt", zir.getMessage());
			this.set("dirty", "yes");
			return null;
		}

		if(null == liste || 0 == liste.size())
		{
			// GAR NICHT GUT
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textDataNotReadable", session.getString("language")) + "</h2>");
			zlprotocol.appendHtmlErr("<h2>" + Tr.t("textNoImport", session.getString("language")) + "</h2>");
			session.set("CURRENT_VIEW", "ERRORQUEST");
			set("var.errorcode", zlprotocol.getHtmlErr());
			set("var.errorcodetxt", Tr.t("textListUnreadable", session.getString("language")));
			this.set("dirty", "yes");
			return null;
		}
		return liste;
	}

	/**
	 * Zinszeilen anlegen und Tops/Stellplaetze gleichzeitig schreiben.
	 *
	 * @param zl
	 *            Zinsliste
	 * @param top_list
	 *            Top Liste
	 * @param hausOid
	 *            Die Haus OID
	 * @param appendToHTMLResult
	 *            the append to HTML result
	 * @return the hashtable
	 */
	public Hashtable zinszeilenAnlegen(Zinsliste zl, TopList top_list, String hausOid, boolean appendToHTMLResult)
	{
		return getCrudService().zinszeilenAnlegen(zl, top_list, hausOid, appendToHTMLResult);
	}

	/**
	 * Updated den Haus-Namen mit evtl vorhandener EDV-Nr.
	 * und falls hausadresse, hausort, hausplz in der zinslistenconfig.xml configuriert sind werden diese werte aus der zinsliste am Haus aktualisiert
	 *
	 * @param hausName
	 *            the haus name
	 * @param hausOid
	 *            Die Haus OID
	 * @param zl
	 *            the zl
	 */
	private void updateHausName(String hausName, String hausOid, Zinsliste zl)
	{
		if(enableDetailedLogging)
		{
			endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateHausName 1: " + ((endtime - starttime) / 1000) + " seconds");
			starttime = System.currentTimeMillis();
		}

		DynGenDataObj hausObj = null;
		boolean dostore = false;
		if(null != hausOid && hausOid.length() > 0)
		{
			try
			{
				hausObj = (DynGenDataObj)DAInst.getObject(hausOid, "");
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
							if(!CfgSingleton.getInstance().getBoolean("PM_UPDATE_HAUSADRESSE", Boolean.TRUE && !hausObj.getString("var.name").trim().equals("")))
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
							if(!CfgSingleton.getInstance().getBoolean("PM_UPDATE_HAUSADRESSE", Boolean.TRUE && !hausObj.getString("var.name").trim().equals("")))
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
						DAInst.storeObject(hausObj, "CIMS.haus", hausOid, session);
					}
				}
			}
			catch(Exception xc)
			{
				log("Unbekanntes Haus mit ID " + hausOid);
				debug.log(xc);
			}
		}

		if(enableDetailedLogging)
		{
			endtime = System.currentTimeMillis();
			BugMe.getInstance().log("############ Log updateHausName 2: " + ((endtime - starttime) / 1000) + " seconds");
			starttime = System.currentTimeMillis();
		}
	}

	/**
	 * Sets Selected Values From Previous ZZ.
	 *
	 * @param dgdzz
	 *            the dgdzz
	 * @param oldzzdgd
	 *            the oldzzdgd
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj setSelectedValuesFromPreviousZZ(DynGenDataObj dgdzz, DynGenDataObj oldzzdgd)
	{
		return getCrudService().setSelectedValuesFromPreviousZZ(dgdzz, oldzzdgd);
	}

	/**
	 * Sets the zz extras.
	 *
	 * @param dgdzz
	 *            the dgdzz
	 * @param tos
	 *            the tos
	 * @param zl
	 *            the zl
	 * @return the dyn gen data obj
	 */
	public DynGenDataObj setZZExtras(DynGenDataObj dgdzz, DynGenDataObj tos, Zinsliste zl)
	{
		return getCrudService().setZZExtras(dgdzz, tos, zl);
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
		return getCrudService().getUserValue(name);
	}

	/**
	 * massenspeicherung zerlegen - aus performance gruenden ...
	 *
	 * @param res
	 *            the res
	 * @param ses
	 *            user session the ses
	 * @return the hashtable
	 */
	public Hashtable storeObjectsJunked(Hashtable res, DynGenDataObj ses)
	{
		return getCrudService().storeObjectsJunked(res, ses);
	}

	/**
	 * Gets the assetmanager mailadress from object.
	 *
	 * @param hausid
	 *            the hausid
	 * @return the assetmanager mailadress from object
	 */
	private String getAssetmanagerMailadressFromObject(String hausid)
	{

		Hashtable<String, Object> args = new Hashtable<String, Object>();
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

		// query result vector
		Vector<Hashtable<String, String>> res = null;
		try
		{
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			Hashtable<String, String> h = res.get(0);

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
	 * Gets the alle W es in bestand.
	 *
	 * @return the alle W es in bestand
	 */
	private Hashtable getAlleWEsInBestand()
	{
		Hashtable<String, Hashtable<String, Hashtable<String, String>>> result = new Hashtable<String, Hashtable<String, Hashtable<String, String>>>();

		Hashtable<String, Object> args = new Hashtable<String, Object>();
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

		// query result vector
		Vector<Hashtable<String, String>> res = null;
		try
		{
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			for(int i = 0; i < res.size(); i++)
			{
				Hashtable<String, String> h = res.get(0);

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

				// Sollt ein Vector of Hashes sein sonst überschreibt sich das immer

				if(result.containsKey(mailAndName))
				{
					Hashtable<String, Hashtable<String, String>> entry = result.get(mailAndName);
					entry.put(identadresse1, h);
					result.put(mailAndName, entry);
				}
				else
				{
					Hashtable<String, Hashtable<String, String>> entry = new Hashtable<String, Hashtable<String, String>>();
					entry.put(identadresse1, h);
					result.put(mailAndName, entry);
				}

			}
		}

		return result;

	}

	/**
	 * Gets the mailverteiler from assetmanager.
	 *
	 * @param name
	 *            the name
	 * @return the mailverteiler from assetmanager
	 */
	private Hashtable getMailverteilerFromAssetmanager(String name)
	{
		Hashtable<String, Object> mailverteileradressen = new Hashtable<String, Object>();
		Hashtable<String, Object> args = new Hashtable<String, Object>();
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

		// query result vector
		Vector<Hashtable<String, String>> res = null;
		try
		{
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();
		}
		catch(Exception qe)
		{
			debug.error(this, "Exception querying objects.");
			debug.error(qe);
			set("var.result", "Interner Fehler:" + qe.getMessage());
		}

		if(res.size() > 0)
		{
			Hashtable<String, String> h = res.get(0);

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
	 * Is used for Event Triggered Automated Zinslistenimport.
	 *
	 * @return 0 id ok negative on error
	 */
	public int processAutomaticZinslistenimport()
	{
		if(threadAgent == null)
		{
			Connector connector = new Connector();
			threadAgent = connector.getThreadAgent();
		}

		try
		{
			EThreadParams threadParams[] = threadAgent.getAllThreadParams();

			// Nur ein import zur gleichen Zeit!
			if(threadParams.length > 0)
			{
				return 0;
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}

		// Is executed if files comes from Networkdrive or similar physical file
		ArrayList<String> fileNames = new ArrayList<String>();
		String fileWithPath = this.getString("var.filepath");
		verzeichnis = fileWithPath;
		String fileWithPathBackup = this.getString("var.filepathbackup");

		if(fileWithPath == null || fileWithPath.length() == 0)
		{
			fileWithPath = (String)CfgSingleton.getInstance().get("PM_RR_UPLOAD_DIR");
			if(fileWithPath != null && fileWithPath.length() > 0)
			{
				fileWithPathBackup = fileWithPath + "_BACKUP";
			}
		}

		String quellsystem = this.getString("var.quellsystem");
		if((fileWithPath == null || fileWithPath.length() == 0) && quellsystem.equals("default"))
		{
			fileWithPath = (String)CfgSingleton.getInstance().get("UPLOAD_FILE_DIR") + System.getProperty("file.separator") + "ZZIMPORT";
			fileWithPathBackup = (String)CfgSingleton.getInstance().get("UPLOAD_FILE_DIR") + System.getProperty("file.separator") + "ZZIMPORT_BACKUP";
		}

		// Nur für Default importe quellsystem == "default" -> SAPCSV Import hat eigenes Handling weil mehrere Files gemerged werden ...
		if(fileWithPath != null && fileWithPath.length() > 0 && fileWithPathBackup != null && fileWithPathBackup.length() > 0 && quellsystem.equals("default"))
		{
			try
			{
				fileWithPath = fileWithPath + System.getProperty("file.separator");
				fileWithPathBackup = fileWithPathBackup + System.getProperty("file.separator");

				// Create folders if not exist
				File folder = new File(fileWithPath);
				if(!folder.exists() && !folder.isDirectory())
				{
					folder.mkdirs();
				}
				File backupfolder = new File(fileWithPathBackup);
				if(!backupfolder.exists() && !backupfolder.isDirectory())
				{
					backupfolder.mkdirs();
				}

				File[] listOfFiles = folder.listFiles();

				if(listOfFiles == null)
				{
					debug.error("Can't get Files from Directory (Access Denied): " + fileWithPath);
				}
				else
				{
					for(File myfile : listOfFiles)
					{
						if(myfile.isFile())
						{
							fileNames.add(fileWithPath + myfile.getName());
						}
					}

					if(fileNames.size() == 0)
					{
						debug.log("No Zinslisten Files in Directory: " + fileWithPath);
						return -2; // Status -2 -> Do not store result
					}
					else
					{
						Collections.sort(fileNames.subList(1, fileNames.size()));
					}
				}
			}
			catch(Exception e)
			{
				{
					debug.error("Zinslistenupload - errorCannotReadFile" + e.getMessage());
					session.set("ERROR.MAIN", Tr.t("errorCannotReadFile", session.getString("language")) + " " + e.getMessage());
					session.set("ERRORID", session.get("CURRENT_OID"));
					session.set("ERRORVIEW", "SHOW");
					debug.error(e);
					return -1;
				}
			}

			// Do the upload!!!!
			for(int i = 0; i < fileNames.size(); i++)
			{
				try
				{

					if(null == DAInst)
					{
						net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
						DAInst = conn.getDataAgent();
					}

					String myoid = "";

					String userid = (String)session.get("userid");

					// Vector csvresult = null;

					if(fileNames.get(i).length() > 0)
					{
						try
						{
							Path path = Path.of(fileNames.get(i));
							byte[] data = Files.readAllBytes(path);

							String filename = fileNames.get(i).substring(fileNames.get(i).lastIndexOf(System.getProperty("file.separator")) + 1);
							String filetype = filename.substring(filename.lastIndexOf(".") + 1);

							Hashtable fparams = new Hashtable();
							fparams.put("size", "" + data.length);
							fparams.put("paramname", "zinslistenfile");
							fparams.put("name", filename);
							fparams.put("type", filetype);

							// if there are filetypes not defined yet -> need to add them later
							if(filetype.equals("csv") || filetype.equals("txt") || filetype.equals("dat") || filetype.equals("exp"))
							{
								fparams.put("Content-Type", "application/txt");
							}
							else
							{
								fparams.put("Content-Type", "application/vnd.ms-excel");
							}

							if(FDAInst == null)
							{
								Connector conn = null;
								conn = new Connector();
								FDAInst = conn.getFileDataAgent();
							}

							// Create a unique file reference
							String inckey = "ZinslistenUploadFromDirectory";
							Long ctr = CoolDataTool.generateUniqueSequence(inckey);

							String filereferencename = inckey + ctr + "." + filetype;

							zlfile = FDAInst.storeObject(filereferencename, data, fparams);

							String templateType = "ICRS.zinslisten.zinslistenupload";
							TemplateReader tr = TemplateReader.getInstance();
							DynGenDataObj myuploaddgd = tr.getFlavouredDGDForTemplate(templateType, global, session);

							Hashtable vars = this.getSubs("var");
							for(Object key : vars.keySet())
							{
								try
								{
									String value = (String)this.get("var." + key);
									if(value != null)
									{
										// pruefen ob selector oder wert
										String selector = this.getString("var." + key + ".SELECTOR");
										if(selector.length() > 0)
										{
											Hashtable opts = getValueMap(selector);
											if(opts.containsKey(value))
											{
												value = (String)opts.get(value);
											}
										}

										myuploaddgd.set("var." + key, value);
									}

								}
								catch(Exception ex)
								{
									debug.error(ex);
								}
							}

							if(!zlfile.startsWith("FILE_"))
							{
								zlfile = "FILE_" + zlfile;
							}
							myuploaddgd.set("var.file", zlfile);

							myuploaddgd.set("dirty", "yes");

							// Flavour needed
							// how get flavour from template?
							// String templatetype = (String)this.get("TEMPLATETYPE");
							flavour = (String)this.get("var.flavour");
							myuploaddgd.set("var.flavour", flavour);

							String hostname = (String)this.get("var.hostname");
							if(hostname != null && hostname.length() > 0)
							{
								myuploaddgd.set("var.hostname", hostname);
							}
							else
							{
								myuploaddgd.set("var.hostname", "localhost");
							}

							mailtoamcfg = (String)CfgSingleton.getInstance().get("SENDMAIL_NOT_TOASSETMANAGER");

							// always notify assetmanager on automatic import from filesystem
							if(null != mailtoamcfg && mailtoamcfg.trim().length() > 0)
							{
								if(mailtoamcfg.equals("1") || mailtoamcfg.equalsIgnoreCase("yes"))
								{
									myuploaddgd.set("var.assetmanagerinfo", "0");
								}
							}
							else
							{
								myuploaddgd.set("var.assetmanagerinfo", "1");
							}

							myoid = DAInst.storeObject(myuploaddgd, templateType, null, session);

							if(myoid != null && myoid.length() > 0)
							{
								// remove the file from filesystem
								try
								{
									File fileToDelete = new File(fileNames.get(i));

									byte[] buffer = new byte[1024];

									try
									{
										String filePath = fileNames.get(i);

										String actualTime = new SimpleDateFormat("_yyyyMMddHHmmss").format(Calendar.getInstance().getTime());
										String fileName = StringUtils.replace(filePath, fileWithPath, "");
										String outNameWithPath = StringUtils.replace(filePath, fileWithPath, fileWithPathBackup) + actualTime + ".zip"; // add creation time!
										String inNameWithPath = fileNames.get(i);

										FileOutputStream fos = new FileOutputStream(outNameWithPath);
										ZipOutputStream zos = new ZipOutputStream(fos);
										ZipEntry ze = new ZipEntry(fileName);
										zos.putNextEntry(ze);
										FileInputStream in = new FileInputStream(inNameWithPath);

										int len;
										while((len = in.read(buffer)) > 0)
										{
											zos.write(buffer, 0, len);
										}

										in.close();
										zos.closeEntry();

										// remember close it
										zos.close();

										if(fileToDelete.delete())
										{
											debug.info(fileToDelete.getName() + " is deleted!");
										}
										else
										{
											debug.error(fileToDelete.getName() + "failed to delete.");
										}

									}
									catch(IOException ex)
									{
										debug.error(ex);
									}

								}
								catch(Exception e)
								{
									debug.error(e);
								}
							}

						}
						catch(Exception ex)
						{
							session.set("ERROR.MAIN", Tr.t("errorCannotReadFile", session.getString("language")) + " " + ex.getMessage());
							session.set("ERRORID", session.get("CURRENT_OID"));
							session.set("ERRORVIEW", "SHOW");
							debug.error(ex);
							return -1;
						}

						EThreadParams params = new EThreadParams();
						Date now = new Date();
						GregorianCalendar crecalendar = new GregorianCalendar();
						crecalendar.setTime(now);

						params.put(ZinslistenImportThread.SCHEDULE_TIME, net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()));
						params.put(ZinslistenImportThread.ZINSLISTE, createLink(zlfile, "Datei", session));

						String user = "";
						try
						{
							DynGenDataObj userDgd = (DynGenDataObj)DAInst.getObject((String)this.get("properties.creator"), "System.User");
							user = (String)userDgd.get("var.name");
						}
						catch(Exception e)
						{}
						params.put(ZinslistenImportThread.USER, user);
						String nowS = net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime());
						cimslog.log("\nZinslistenimport Thread gesetzt auf " + nowS + "\n");
						threadAgent.createThread("Magic.IMS.ZLImport.ZinslistenImportThread", myoid, "Zinslistenimport (" + nowS + ")", userid, params, crecalendar.getTime(), "ZLImportThreads", EThreadGroup.MODE_PARALLEL, EThreadGroup.INFINITE, 1, session, 4);
					}
				}
				catch(Exception ex)
				{
					debug.error(ex);
				}
			}
		}

		else

		{
			// Is executed if files comes from Query source -> e.g. Swagger Query for Shore
			// Diesen Part eventuell in neue Methode auslagern -> ist atm nur bei der SHORE in Verwendung
			// Oder alternativ Abfrage auf die Variable "quellsystem" im zinslistenupload_init.tpl -> nur laufen lassen wenn Wert != default
			try
			{

				if(null == DAInst)
				{
					net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
					DAInst = conn.getDataAgent();
				}

				this.fixFileLink();
				String myoid = DAInst.storeObject(this, this.getTemplateType(), null, session);

				EThreadParams params = new EThreadParams();
				Date now = new Date();
				GregorianCalendar crecalendar = new GregorianCalendar();
				crecalendar.setTime(now);

				params.put(ZinslistenImportThread.SCHEDULE_TIME, net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()));

				String user = "";

				DynGenDataObj userDgd = (DynGenDataObj)DAInst.getObject((String)this.get("properties.creator"), "System.User");
				user = (String)userDgd.get("var.name");
				String userid = (String)userDgd.get("id");

				params.put(ZinslistenImportThread.USER, user);
				String nowS = net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime());
				cimslog.log("\nZinslistenimport Thread gesetzt auf " + nowS + "\n");
				threadAgent.createThread("Magic.IMS.ZLImport.ZinslistenImportThread", myoid, "Zinslistenimport (" + nowS + ")", userid, params, crecalendar.getTime(), "ZLImportThreads", EThreadGroup.MODE_PARALLEL, EThreadGroup.INFINITE, 1, session, 4);
			}
			catch(Exception e)
			{
				debug.log(e);
			}

		}

		return 0;
	}

	/**
	 * Is used for Event Triggered Automated Zinslistenimport.
	 *
	 * @return was steht da drin??? Hashtable nach parse???
	 */
	public Hashtable<String, String> processSAPZinslistenimport()
	{
		// Do the upload!!!!
		try
		{
			// START - this is only a temporary solution because eventengine has no flavour, hostname, ect.

			String domainname = (String)session.get("domainname");
			if(domainname == null || domainname.length() == 0)
			{
				session.set("domainname", "localhost");
			}
			String flavour = (String)session.get("flavour");
			if(flavour == null || flavour.length() == 0)
			{
				session.set("flavour", "icrsare");
			}
			String currentview = (String)session.get("CURRENT_VIEW");
			if(currentview == null || currentview.length() == 0)
			{
				session.set("CURRENT_VIEW", "READ");
			}
			String view = (String)session.get("VIEW");
			if(view == null || view.length() == 0)
			{
				session.set("VIEW", "READ");
			}

			// END - this is only a temporary solution because eventengine has no flavour, hostname, ect.

			String userid = (String)session.get("userid");

			if(null != mailtoamcfg && mailtoamcfg.trim().length() > 0)
			{
				if(mailtoamcfg.equals("1") || mailtoamcfg.equalsIgnoreCase("yes"))
				{
					this.set("var.assetmanagerinfo", "0");
				}
			}
			else
			{
				this.set("var.assetmanagerinfo", "1");
			}

			this.set("var.sapconnection", "1");
			this.set("var.sapimportname", "zinslistenimport");

			TemplateReader tr = TemplateReader.getInstance();
			// DAInst.storeObject(this, templateType, null, session);
			this.parse(tr.getCodeForTemplate(this.getTemplateType()), global, session);
		}
		catch(Exception ex)
		{
			debug.error(ex);
		}
		return result;
	}

	/**
	 * This method returns a Hashtable with alternatives as keys -> this is the opposite as the ValueReplacement getValueMap Method!.
	 *
	 * @param myeselector
	 *            the myeselector
	 * @return the value map
	 */
	public Hashtable getValueMap(String myeselector)
	{
		return getMappingService().getValueMap(myeselector);
	}

	/**
	 * write slots of subobjects<br>
	 * sets "dirty" in dgd: dgd.set("dirty", "yes");
	 *
	 * @param vals
	 *            the vals
	 * @param dgd
	 *            the dgd
	 */
	protected void writeSlots(Hashtable vals, DynGenDataObj dgd)
	{
		writeSlots(vals, dgd, false, false);
	}

	/**
	 * write slots of subobjects<br>
	 * sets "dirty" in dgd: dgd.set("dirty", "yes");
	 *
	 * @param vals
	 *            the vals
	 * @param dgd
	 *            the dgd
	 * @param setOnlySingleValue
	 *            -> nur den neuen Wert im slot setzten und den alten loeschen
	 * @param createObject
	 *            the create object
	 */
	protected void writeSlots(Hashtable vals, DynGenDataObj dgd, boolean setOnlySingleValue, boolean createObject)
	{
		String displayname = "";
		if(flavour.equals("icrsfred") || flavour.equals("icrsare") || flavour.equals("icrswi"))
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
						boolean clearvaluesslot = this.getBoolean("var.clearvalues" + name, Boolean.FALSE);

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
								if(flavour.equals("icrssom") && fid == null && name.equals("vermieterfirma"))
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

									f_dgd = (DynGenDataObj)DAInst.getObject(fid, "");
								}

								if(null == f_dgd && createObject && slotval.length() > 0)
								{

									debug.chat("create " + slotval + " from template " + ttype);
									f_dgd = new DynGenDataObj();
									f_dgd.DAInst = DAInst;
									// build it with templatecode
									if(PBInst == null)
									{
										Connector conn = null;
										conn = new Connector();
										PBInst = conn.getPageBuilder();
									}
									String f_tcode = PBInst.readTemplate(ttype);
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
										r = DAInst.storeObject(f_dgd, "", fid, session);
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
												slot_els.put(vals.get("mieterfirma___uniqueid"), id);
											}
											else if(vals.containsKey("mieterfirma___externalid"))
											{
												slot_els.put(vals.get("mieterfirma___externalid"), id);
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
								if(!f.equalsIgnoreOrderAndDuplicates(oldSlot))
								{
									dgd.set("slot." + name, f);
									if(multiSlotWritten.length() == 0)
									{
										multiSlotWritten.append(" [").append(displayname).append(": ");
									}
									if(multiRealSlotWritten.length() == 0)
									{
										multiRealSlotWritten.append(" [").append(name).append(": ");
									}
									multiSlotWritten.append(" \"").append(DAInst.getDOBFieldValue("name", fid)).append("\"");
									multiRealSlotWritten.append(" \"").append(fid).append("\"");
									dgd.set("dirty", "yes");
								}
							}

						}
						else
						{
							debug.error(this, "No templatetype for slot " + name);
						}
					}
				}
				else
				{
					// errorcode.append("<div class=\"errortext\">" + Tr.t("textLine", mylang) + " " + zeile + " " + Tr.t("errorNoPermissionWrite", mylang) + " " + name + "</div><br>");
					// resulttable.append("<tr><td><div class=\"errortext\">" + zeile + "</div></td><td>" + "</td><td><div class=\"errortext\">" + Tr.t("errorNoPermissionWrite", mylang) +
					// "</div></td><td>" + " \"" + name + "\" " + "!</td></tr>");
					// csvErrorCollector.addError(zeile, Tr.t("errorNoPermissionWrite", mylang), CSVErrorCollector.SEVERITY_MEDIUM, Tr.t("errorNoPermissionWrite", mylang) + " " + name,
					// Tr.t("errorNoPermissionWrite", mylang), null, ses_username);
				}
			}
		}
		catch(Exception xx)
		{
			debug.error(xx);
			// errorcode.append("<dic class=errortext>" + Tr.t("textFatalError", mylang) + "</div>: " + xx.getMessage() + "<br>");
			// resulttable.append("<tr><td><div class=\"errortext\">" + zeile + "</div></td><td>" + "</td><td><div class=\"errortext\">" + Tr.t("textFatalError", mylang) + "</div></td><td>" + " \"" +
			// xx.getMessage() + "\" " + "!</td></tr>");
			// csvErrorCollector.addError(zeile, Tr.t("textFatalError", mylang), CSVErrorCollector.SEVERITY_MEDIUM, xx.getMessage(), Tr.t("textFatalError", mylang), null, ses_username);
		}
	}

	/**
	 * welche objekte gibt es schon? -- Nur mal fuer Mieterfirma.
	 *
	 * @param ttype
	 *            the ttype
	 * @return the mapping
	 */
	public Hashtable<String, String> getMapping(String ttype)
	{
		return getMappingService().getMapping(ttype);
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

			if(DAInst == null)
			{
				Connector conn = new Connector();
				DAInst = conn.getDataAgent();
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
				qr = DAInst.queryPlainSQLwithResult(sql);
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
			if(DAInst == null)
			{
				Connector conn = new Connector();
				DAInst = conn.getDataAgent();
			}
			Vector<Hashtable<String, String>> result;
			QueryResult qr;
			try
			{
				qr = DAInst.queryPlainSQLwithResult("select CAST('" + originalString + "' AS varchar(8000)) COLLATE " + dbEncoding + " encodedstring");
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
	 * macht aus dem zinszeilenhashtable einen String mit Infos zum printout.
	 *
	 * @param h
	 *            zz Hashtable
	 * @return Infostring
	 */
	private String getTopInfoStringFromZZHT(Hashtable h)
	{
		String top = (String)h.get("top");
		if(null == top)
		{
			top = "";
		}

		top = Zinsliste.removeEdvNrIfEqualToNameFromTopName(top);
		String mieteinheitenanz = (String)h.get("mieteinheitenanz");
		if(null != mieteinheitenanz)
		{
			// mieteinheitenanz = Currency.formatCent(mieteinheitenanz);
			if(!mieteinheitenanz.equals("null"))
			{
				if(mieteinheitenanz.endsWith(",00"))
				{
					mieteinheitenanz = mieteinheitenanz.replace(",00", "");
				}
			}
		}
		else
		{
			mieteinheitenanz = "1";
		}

		String tnutzung = (String)h.get("nutzung");
		String tou = (String)h.get("nutzung");

		LinkedHashMap lhm = Selector.getValue2NameMap("CIMS.SelectorNutzung", null, session, null, null);
		String fullnutzung = (String)lhm.get(tnutzung);
		if(null == fullnutzung)
		{
			fullnutzung = tnutzung;
		}

		if(session.getString("language").toUpperCase().equals("EN"))
		{
			String englshortname = ZLImport.englishTypeOfUseShortMap.get(tnutzung);
			if(null != englshortname)
			{
				tnutzung = englshortname;
			}
		}
		if(null == tnutzung)
		{
			tnutzung = "";
			tou = "";
		}
		String tmieter = (String)h.get("mieter");
		if(null == tmieter)
		{
			tmieter = "";
		}

		String tfl = (String)h.get("fl");
		if(null == tfl)
		{
			tfl = (String)h.get("nfl");
			if(null == tfl)
			{
				tfl = "";
			}
			if(tfl.length() == 0)
			{
				tfl = (String)h.get("leerfl");
				if(null == tfl)
				{
					tfl = "";
				}
				if(tfl.length() == 0)
				{
					tfl = (String)h.get("ffl");
					if(null == tfl)
					{
						tfl = "";
					}
				}

			}
		}
		String hmz = (String)h.get("hmz");
		if(null == hmz)
		{
			hmz = "0";
		}

		String hrmiete = hmz;
		String hrmiete_OC = hmz;
		BigDecimal hmzdiversexrateBD = null;

		String hmzdiversecurrency = (String)h.get("hmzdiversecurrency");
		if(null == hmzdiversecurrency)
		{
			hmzdiversecurrency = "";
		}
		String hmzdiversexrate = (String)h.get("hmzdiversexrate");
		if(null == hmzdiversexrate)
		{
			hmzdiversexrate = "1";
		}
		if(null != hmzdiversecurrency && null != hmzdiversexrate)
		{
			if(!hmzdiversecurrency.equals("EUR") && !hmzdiversecurrency.equals(""))
			{
				// Fremdwährung
				Currency hmzdiversexrateC = new Currency(hmzdiversexrate);
				if(null != hmzdiversexrateC)
				{
					hmzdiversexrateBD = hmzdiversexrateC.getBigDecimal();
				}
			}
		}

		Currency anz = new Currency(mieteinheitenanz);
		Currency flaeche = new Currency(tfl);

		if(null == hmzdiversexrateBD)
		{
			hmzdiversexrateBD = BigDecimal.ONE;
		}
		if(hmz.length() > 0)
		{
			Currency miete = new Currency(hmz);
			BigDecimal mieteBD = miete.getBigDecimal();
			BigDecimal mieteBD_OC = miete.getBigDecimal();
			// Originalwaehrung
			Currency moc = new Currency();
			if(mieteBD_OC != null)
			{
				mieteBD_OC = mieteBD_OC.multiply(hmzdiversexrateBD);
				moc = new Currency(mieteBD_OC);
				hrmiete_OC = moc.getFormattedStringValue();
			}
			if(tou.equals("P") || tou.equals("GA") || tou.equals("SP"))
			{

				BigDecimal anzBD = anz.getBigDecimal();
				if(null != mieteBD && null != anzBD && !anzBD.equals(BigDecimal.ZERO))
				{
					mieteBD = mieteBD.divide(anzBD, RoundingMode.HALF_EVEN);
					mieteBD = mieteBD.setScale(2, RoundingMode.HALF_EVEN);
					Currency m = new Currency(mieteBD);
					hrmiete = m.getFormattedStringValue();
					// Originalwaehrung
					if(mieteBD_OC != null)
					{
						mieteBD_OC = mieteBD_OC.divide(anzBD, RoundingMode.HALF_EVEN);
						mieteBD_OC = mieteBD_OC.setScale(2, RoundingMode.HALF_EVEN);
						moc = new Currency(mieteBD_OC);
					}
					hrmiete_OC = moc.getFormattedStringValue();
				}
			}
			else
			{

				BigDecimal flBD = flaeche.getBigDecimal();
				if(null != mieteBD && null != flBD && !flBD.equals(BigDecimal.ZERO))
				{
					mieteBD = mieteBD.divide(flBD, RoundingMode.HALF_EVEN);
					mieteBD = mieteBD.setScale(2, RoundingMode.HALF_EVEN);
					Currency m = new Currency(mieteBD);
					hrmiete = m.getFormattedStringValue();
					// Originalwaehrung
					if(mieteBD_OC != null)
					{
						mieteBD_OC = mieteBD_OC.divide(flBD, RoundingMode.HALF_EVEN);
						mieteBD_OC = mieteBD_OC.setScale(2, RoundingMode.HALF_EVEN);
						moc = new Currency(mieteBD_OC);
					}
					hrmiete_OC = moc.getFormattedStringValue();
				}
			}
		}

		String tmvv = (String)h.get("mietvertragvon");
		if(null != tmvv)
		{
			DateTime dt = new DateTime(tmvv);
			if(null != dt)
			{
				String dtStr = dt.getFormattedStringValueDay();
				if(null != dtStr)
				{
					tmvv = dtStr;
				}
				else
				{
					tmvv = "";
				}
			}
		}
		else
		{
			tmvv = "";
		}

		String tmvb = (String)h.get("mietvertragbis");
		if(null != tmvb)
		{
			DateTime dt = new DateTime(tmvb);
			if(null != dt)
			{
				String dtStr = dt.getFormattedStringValueDay();
				if(null != dtStr)
				{
					tmvb = dtStr;
				}
				else
				{
					tmvb = "";
				}
			}
		}
		else
		{
			tmvb = "";
		}

		if(null == tou)
		{
			tou = "";
		}
		tou = tou.toUpperCase();
		String lang = session.getString("language");
		if(tou.equals("P") || tou.equals("GA") || tou.equals("SP"))
		{
			if(hmzdiversexrateBD.compareTo(BigDecimal.ONE) == 0)
			{ // Umrechnungskurs 1 = Systemwaehrung
				return Tr.t("INFO_WITHOUT_AREA_EUR", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);
			}
			else
			{
				return Tr.t("INFO_WITHOUT_AREA", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);
			}
		}
		else if(tou.equals("S") && flaeche.equalsZero())
		{
			if(hmzdiversexrateBD.compareTo(BigDecimal.ONE) == 0)
			{ // Umrechnungskurs 1 = Systemwaehrung
				return Tr.t("INFO_WITHOUT_AREA_EUR", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);
			}
			else
			{
				return Tr.t("INFO_WITHOUT_AREA", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);
			}
		}
		else
		{
			if(hmzdiversexrateBD.compareTo(BigDecimal.ONE) == 0)
			{ // Umrechnungskurs 1 = Systemwaehrung
				return Tr.t("INFO_WITH_AREA_EUR", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);
			}
			else
			{
				return Tr.t("INFO_WITH_AREA", lang, top, tmieter, fullnutzung, tfl, mieteinheitenanz, hrmiete, tmvv, tmvb, hmzdiversecurrency, hrmiete_OC);

			}
		}
	}

	/**
	 * Generate periodenvergleich.
	 *
	 * @param oid_haus
	 *            the oid haus
	 * @param azl
	 *            the azl
	 */
	private void generatePeriodenvergleich(String oid_haus, Zinsliste azl)
	{
		try
		{
			TopoTool topotool = new TopoTool(session, global);

			if(oid_haus != null && oid_haus.length() > 0)
			{
				TopoQueries topoQueries = new TopoQueries(session, global);
				String[] nutzungBestandsfl = {
					"B",
					"G",
					"W",
					"H",
					"L",
					"S",
					"LG",
					"PR",
					"P",
					"GA",
					"SP"};
				String[] hIDs = new String[1];
				hIDs[0] = oid_haus;
				Hashtable<String, Hashtable<String, String>> mietsummenAktuellePeriode = topoQueries.monatsSummenNachNutzung(azl.monat, azl.jahr, hIDs, nutzungBestandsfl, null, null, null, null, null, null, null, null, null, null, false, true);

				if(mietsummenAktuellePeriode != null && mietsummenAktuellePeriode.size() > 0)
				{
					Hashtable<String, String> vorperiode = topoQueries.getZinslistenMonatForHausVorperiode(oid_haus, azl.monat, azl.jahr);
					Hashtable<String, String> resultVorPeriode = null;
					Hashtable<String, Hashtable<String, String>> mietsummenVorPeriode = null;
					if(vorperiode.containsKey("monat") && vorperiode.get("monat").length() > 0 && vorperiode.containsKey("jahr") && vorperiode.get("jahr").length() > 0)
					{
						mietsummenVorPeriode = topoQueries.monatsSummenNachNutzung(vorperiode.get("monat"), vorperiode.get("jahr"), hIDs, nutzungBestandsfl, null, null, null, null, null, null, null, null, null, null, false, true);
					}
					// Create a nice table and add values to mailinglist!

					String mailAndName = getAssetmanagerMailadressFromObject(topotool.getHausOID(azl));
					// PKO - REMOVE - Only testing purpose
					System.out.println("AM MAILS TO (3): " + mailAndName + " // Hausinfos:" + String.valueOf(azl.edvNr) + " - " + String.valueOf(azl.adresse) + " - " + String.valueOf(azl.ort) + " - " + String.valueOf(azl.plz));

					if(mailinglistKennwerteNachNutzung.containsKey(mailAndName))
					{
						// get email and append link
						StringBuffer mailtext = new StringBuffer();
						mailtext.append(mailinglistKennwerteNachNutzung.get(mailAndName));

						String diffHmzist = "";
						String diffNfl = "";
						String diffLeerfl = "";

						// Werte aktuelle Periode
						for(String key : mietsummenAktuellePeriode.keySet())
						{
							BigDecimal val1 = new BigDecimal(0);
							BigDecimal val2 = new BigDecimal(0);
							BigDecimal val3 = new BigDecimal(0);
							BigDecimal val1vp = new BigDecimal(0);
							BigDecimal val2vp = new BigDecimal(0);
							BigDecimal val3vp = new BigDecimal(0);

							Hashtable<String, String> resultAktuellePeriode = mietsummenAktuellePeriode.get(key);
							if(null != resultAktuellePeriode && null != resultVorPeriode)
							{
								mailtext.append(Tr.t("diffRow", mylang, azl.adresse, resultAktuellePeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultAktuellePeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
							}
							// Werte Vorperiode
							if(mietsummenVorPeriode != null && mietsummenVorPeriode.containsKey(key))
							{
								resultVorPeriode = mietsummenVorPeriode.get(key);

								if(resultVorPeriode != null)
								{
									val1vp = new BigDecimal(resultVorPeriode.get("hmzist"));
									val2vp = new BigDecimal(resultVorPeriode.get("nfl"));
									val3vp = new BigDecimal(resultVorPeriode.get("leerfl"));
									mailtext.append(Tr.t("diffRow", mylang, "", resultVorPeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultVorPeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
								}
								else
								{
									mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
								}
							}
							else
							{
								mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
							}

							// Hier noch eine Differenzzeile einfuegen
							String diff = Tr.t("diff", mylang);

							diffHmzist = CoolStringTool.getFormattedAndCorrectedValue(val1.subtract(val1vp).toString(), false);
							diffNfl = CoolStringTool.getFormattedAndCorrectedValue(val2.subtract(val2vp).toString(), false);
							diffLeerfl = CoolStringTool.getFormattedAndCorrectedValue(val3.subtract(val3vp).toString(), false);

							mailtext.append(Tr.t("diffRow", mylang, diff, "", "", diffHmzist, diffNfl, diffLeerfl));

						}

						boolean sendmailonlyonchange = this.getBoolean("var.sendmailonlyonchange", true);
						if(diffHmzist.equals("0") && diffNfl.equals("0") && diffLeerfl.equals("0") && sendmailonlyonchange)
						{
							// Zeile nicht hinzufuegen, weil keine Aenderung vorhanden!
						}
						else
						{
							mailinglistKennwerteNachNutzung.put(mailAndName, mailtext.toString());
						}
					}
					else
					{
						// add email and headers then append link
						StringBuffer mailtext = new StringBuffer();

						mailtext.append("<br><br>");
						mailtext.append("<table>");

						mailtext.append(Tr.t("diffHeadRow", mylang));

						String diffHmzist = "";
						String diffNfl = "";
						String diffLeerfl = "";

						// Werte aktuelle Periode
						for(String key : mietsummenAktuellePeriode.keySet())
						{
							BigDecimal val1 = new BigDecimal(0);
							BigDecimal val2 = new BigDecimal(0);
							BigDecimal val3 = new BigDecimal(0);
							BigDecimal val1vp = new BigDecimal(0);
							BigDecimal val2vp = new BigDecimal(0);
							BigDecimal val3vp = new BigDecimal(0);

							Hashtable<String, String> resultAktuellePeriode = mietsummenAktuellePeriode.get(key);
							val1 = new BigDecimal(resultAktuellePeriode.get("hmzist"));
							val2 = new BigDecimal(resultAktuellePeriode.get("nfl"));
							val3 = new BigDecimal(resultAktuellePeriode.get("leerfl"));

							mailtext.append(Tr.t("diffRow", mylang, azl.adresse, resultAktuellePeriode.get("monat") + "/" + resultAktuellePeriode.get("jahr"), resultAktuellePeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultAktuellePeriode.get("leerfl"), false)));
							// Werte Vorperiode
							if(mietsummenVorPeriode != null && mietsummenVorPeriode.containsKey(key))
							{
								resultVorPeriode = mietsummenVorPeriode.get(key);

								if(resultVorPeriode != null)
								{
									val1vp = new BigDecimal(resultVorPeriode.get("hmzist"));
									val2vp = new BigDecimal(resultVorPeriode.get("nfl"));
									val3vp = new BigDecimal(resultVorPeriode.get("leerfl"));
									mailtext.append(Tr.t("diffRow", mylang, "", resultVorPeriode.get("monat") + "/" + resultVorPeriode.get("jahr"), resultAktuellePeriode.get("nutzung"), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("hmzist"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("nfl"), false), CoolStringTool.getFormattedAndCorrectedValue(resultVorPeriode.get("leerfl"), false)));
								}
								else
								{
									mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
								}
							}
							else
							{
								mailtext.append(Tr.t("diffRow", mylang, "", "", "", "-", "-", "-"));
							}

							// Hier noch eine Differenzzeile einfuegen
							String diff = Tr.t("diff", mylang);

							diffHmzist = CoolStringTool.getFormattedAndCorrectedValue(val1.subtract(val1vp).toString(), false);
							diffNfl = CoolStringTool.getFormattedAndCorrectedValue(val2.subtract(val2vp).toString(), false);
							diffLeerfl = CoolStringTool.getFormattedAndCorrectedValue(val3.subtract(val3vp).toString(), false);

							mailtext.append(Tr.t("diffRow", mylang, diff, "", "", diffHmzist, diffNfl, diffLeerfl));
							// Leerzeile einfuegen
							mailtext.append(Tr.t("diffRow", mylang, "", "", "", "", "", ""));
						}

						boolean sendmailonlyonchange = this.getBoolean("var.sendmailonlyonchange", true);
						if(diffHmzist.equals("0") && diffNfl.equals("0") && diffLeerfl.equals("0") && sendmailonlyonchange)
						{
							// Zeile nicht hinzufuegen, weil keine Aenderung vorhanden!
						}
						else
						{
							mailinglistKennwerteNachNutzung.put(mailAndName, mailtext.toString());
						}
					}

				}
			}
		}
		catch(Exception ex)
		{
			debug.error(ex);
		}
	}

	/**
	 * generates String with javascript, containing a toplist json object.
	 *
	 * @param top_list
	 *            TopList Object
	 * @return Javascript html String
	 */
	private String getJavascriptTopmatcherString(TopList top_list)
	{
		StringBuffer scriptString = new StringBuffer();
		scriptString.append("<script type=\"text/javascript\">\n");
		scriptString.append("try {\n");
		scriptString.append("	var toplistjson=jQuery.parseJSON('" + top_list.toJSON(session) + "')\n");
		scriptString.append("console.log('parsing json'); generateToplistSelectors(toplistjson);\n");
		scriptString.append("} catch(e) {}\n");
		scriptString.append("</script>\n");
		return scriptString.toString();
	}

	/**
	 * holt var.mappingchanges und erzeugt eine Java Datenstruktur
	 *
	 * @return the mapping changes vector
	 */
	private Vector<Hashtable<String, String>> getMappingChangesVector()
	{
		String mappingchanges = getString("var.mappingchanges");
		Vector mappingchangesV = new Vector<Hashtable<String, String>>();
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
				Hashtable<String, String> data = new Hashtable<String, String>();
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
	 * Gets the var arg.
	 *
	 * @param argName
	 *            the arg name
	 * @return the var arg
	 */
	private String getVarArg(String argName)
	{
		String value = this.getString("var." + argName);
		if(value == null || value.equals(""))
		{
			value = this.getString("arg." + argName);
		}
		if((value == null || value.equals("")) && session != null)
		{
			value = session.getString("arg.oid" + this.getString("id").trim() + "." + argName);
		}
		if((value == null || value.equals("")) && session != null)
		{
			value = session.getString("arg.oid" + this.volatile_id + "." + argName);
		}
		return value;
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

			hausverwaltung = getVarArg("hausverwaltung_" + 0);
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
						if(DAInst == null)
						{
							net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
							DAInst = conn.getDataAgent();
						}

						Date actualDate = new Date(System.currentTimeMillis());
						String actualTime = new SimpleDateFormat("yyyyMMdd_HHmm").format(Calendar.getInstance().getTime());

						TemplateReader tr = TemplateReader.getInstance();
						DynGenDataObj zinslistenconfigfileDgd = tr.getDGDForTemplate("ICRS.administration.zinslistenconfigfile", global, session);
						zinslistenconfigfileDgd.set("var.name", "Zinslistenconfig vom " + actualTime);
						zinslistenconfigfileDgd.setDate("var.aktivierung", actualDate);
						String configOid = DAInst.storeObject(zinslistenconfigfileDgd, zinslistenconfigfileDgd.getTemplateType(), null, session);

						Path path = Path.of(cfg_zlimport);
						byte[] data = Files.readAllBytes(path);

						Hashtable fparams = new Hashtable();
						fparams.put("size", "" + data.length);
						fparams.put("paramname", "zinslistenfile");
						fparams.put("name", "zinslistenconfig.xml");
						fparams.put("type", "xml");
						fparams.put("Content-Type", "application/xml");
						fparams.put("OID", configOid);

						if(FDAInst == null)
						{
							Connector conn = null;
							conn = new Connector();
							FDAInst = conn.getFileDataAgent();
						}

						// Create a unique file reference
						Long ctr = CoolDataTool.generateUniqueSequence(cfg_zlimport);
						String filereferencename = cfg_zlimport.substring(0, cfg_zlimport.indexOf(".xml")) + ctr + ".xml";
						String zlconfigfile = FDAInst.storeObject(filereferencename, data, fparams);

						zinslistenconfigfileDgd.set("var.datei", "FILE_" + zlconfigfile);
						configOid = DAInst.storeObject(zinslistenconfigfileDgd, zinslistenconfigfileDgd.getTemplateType(), configOid, session);

						// File an redmine uebergeben -> feedback_init
						tr = TemplateReader.getInstance();
						DynGenDataObj dgdFeedback = tr.getDGDForTemplate("System.feedback", global, session);
						dgdFeedback.set("var.name", "Zinslistenconfig.xml upgedatet");
						dgdFeedback.set("var.datei1", "FILE_" + zlconfigfile);
						DAInst.storeObject(dgdFeedback, dgdFeedback.getTemplateType(), null, session);
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
	 * Mofify document. Anpassung der Zinslistenconfig (XML File wird geschrieben)
	 *
	 * @param doc
	 *            the doc
	 * @return true, if successful
	 */
	private boolean mofifyDocument(Document doc)
	{
		boolean configHasChanged = false;

		StringBuffer sb_postenchanges = new StringBuffer();
		sb_postenchanges.append("\n");
		sb_postenchanges.append("\nHausverwaltung: " + hausverwaltung);
		sb_postenchanges.append("\n");
		sb_postenchanges.append("\n");

		sb_postenchanges.append("\n<table border=\"1\" cellspacing=\"10\" cellpadding=\"5\">");

		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName("hausverwaltung");

		// Maximal 100 Zapos koennen hier uebers GUI zugeordnet werden....
		for(int i = 0; i < 100; i++)
		{
			String icrszapo = getVarArg("icrszaposelector_" + i);
			String zapovalue = getVarArg("zapovalue_" + i);

			// System.out.println("HV: " + hausverwaltung);
			// System.out.println("Posten: " + icrszapo);
			// System.out.println("Zapo: " + zapovalue);

			if(icrszapo == null || icrszapo.length() == 0)
			{
				continue;
			}

			if(zapovalue.length() == 0)
			{
				// Wenn kein Zapo Value mehr kommt dann aufhoehren
				break;
			}

			String posten = "";
			String subposten = "";

			if(icrszapo.contains(" -> "))
			{
				String[] tmp = icrszapo.split(" -> ");
				posten = tmp[0];
				subposten = tmp[1];
			}

			for(int temp = 0; temp < nList.getLength(); temp++)
			{

				Node nNode = nList.item(temp);

				if(nNode != null && nNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eElement = (Element)nNode;
					// System.out.println("HV name : " + eElement.getAttribute("name"));

					if(eElement.getAttribute("name").equals(hausverwaltung))
					{

						NodeList nListZapos = eElement.getChildNodes();

						// check if exclude node exist
						boolean excludedNodeExists = false;
						Element eElementNutzungswerte = null;

						for(int zapo = 0; zapo < nListZapos.getLength(); zapo++)
						{
							Node nNodeZapo = nListZapos.item(zapo);

							if(nNodeZapo != null && nNodeZapo.getNodeType() == Node.ELEMENT_NODE)
							{
								Element eElementZapo = (Element)nNodeZapo;
								if(eElementZapo.getNodeName().equals("excluded"))
								{
									excludedNodeExists = true;
								}

								if(eElementZapo.getNodeName().equals("nutzungswerte"))
								{
									eElementNutzungswerte = eElementZapo;
								}
							}
						}

						if(!excludedNodeExists && icrszapo.equals("excluded"))
						{
							Element excluded = doc.createElement("excluded");
							// eElement.appendChild(excluded);

							eElement.insertBefore(excluded, eElementNutzungswerte);
						}

						for(int zapo = 0; zapo < nListZapos.getLength(); zapo++)
						{

							Node nNodeZapo = nListZapos.item(zapo);

							if(nNodeZapo != null && nNodeZapo.getNodeType() == Node.ELEMENT_NODE)
							{
								Element eElementZapo = (Element)nNodeZapo;
								// System.out.println("Element: " + eElementZapo.getNodeName() + "//" + eElementZapo.getNodeValue() + " // Zapo name : " +
								// eElementZapo.getAttribute("name"));

								if(eElementZapo.getNodeName().equals("excluded") && icrszapo.equals("excluded"))
								{
									// <alias>vertragsnummer</alias>
									if(zapovalue.length() > 0)
									{
										Element alias = doc.createElement("alias");
										alias.appendChild(doc.createTextNode(zapovalue));
										eElementZapo.appendChild(alias);
										sb_postenchanges.append("\n" + Tr.t("ZLIMPORT_CONFIG_CHANGE_ROW", getLanguage(), zapovalue, "", "ignore"));
										log("Zinslistenconfig Change (" + hausverwaltung + "): " + zapovalue + " ignorieren.");
										configHasChanged = true;
										break;
									}
								}
								else if(eElementZapo.getNodeName().equals("nutzungswerte") && posten.equals("nutzungswerte"))
								{
									// <alias name="Sanitärnutzung" typ="SA" />
									if(zapovalue.length() > 0)
									{
										Element alias = doc.createElement("alias");
										alias.setAttribute("name", zapovalue);
										alias.setAttribute("typ", subposten);
										eElementZapo.appendChild(alias);
										sb_postenchanges.append("\n" + Tr.t("ZLIMPORT_CONFIG_CHANGE_ROW", getLanguage(), zapovalue, "Nutzung", subposten));
										log("Zinslistenconfig Change (" + hausverwaltung + "): " + zapovalue + " Nutzung " + subposten + ".");
										configHasChanged = true;
										break;
									}
								}
								else if(eElementZapo.getAttribute("name").equals(posten) && posten.length() > 0)
								{
									NodeList nListSubZapos = eElementZapo.getChildNodes();
									for(int subzapo = 0; subzapo < nListZapos.getLength(); subzapo++)
									{
										Node nNodeSubZapo = nListSubZapos.item(subzapo);
										if(nNodeSubZapo != null && nNodeSubZapo.getNodeType() == Node.ELEMENT_NODE)
										{
											Element eElementSubZapo = (Element)nNodeSubZapo;
											// System.out.println("SubZapo name : " + eElementSubZapo.getAttribute("name"));

											if(eElementSubZapo.getAttribute("name").equals(subposten))
											{
												// System.out.println("First Alias : " + eElementSubZapo.getElementsByTagName("alias").item(0).getTextContent());
												if(zapovalue.length() > 0)
												{
													Element alias = doc.createElement("alias");
													alias.appendChild(doc.createTextNode(zapovalue));
													eElementSubZapo.appendChild(alias);
													sb_postenchanges.append("\n<br>" + Tr.t("ZLIMPORT_CONFIG_CHANGE_ROW", getLanguage(), zapovalue, "Posten", subposten));
													log("Zinslistenconfig Change (" + hausverwaltung + "): " + zapovalue + " Posten " + subposten + ".");
													configHasChanged = true;
													break;
												}
											}
										}
									}
								}
							}
						}

					}
				}
			}
		}
		sb_postenchanges.append("\n<br></table>");

		if(configHasChanged)
		{
			// Config Change Mail
			String ses_username = (String)session.get("var.username");
			if(null == ses_username)
			{
				ses_username = "unknown";
			}
			String hostip = (String)session.get("hostip");
			if(null == hostip)
			{
				hostip = "unknown";
			}

			String cfgchange_email = CfgSingleton.getInstance().getString("ZLIMPORT_CONFIG_CHANGE_TO_EMAIL");
			String cfgchange_email_cc = CfgSingleton.getInstance().getString("ZLIMPORT_CONFIG_CHANGE_TO_EMAIL_CC");
			if(cfgchange_email.indexOf("@") > 0)
			{
				List<String> to_addresses = new ArrayList<String>();
				to_addresses.add(cfgchange_email);
				List<String> cc_addresses = null;
				if(cfgchange_email_cc.indexOf("@") > 0)
				{
					cc_addresses = new ArrayList<String>();
					cc_addresses.add(cfgchange_email_cc);
				}

				sb_postenchanges.append("\n<br><br>");
				sb_postenchanges.append("\n<br>");
				sb_postenchanges.append("\n<br>" + Tr.t("ZLIMPORT_CONFIG_CHNANGEINFO", getLanguage(), ses_username, hostip));
				String subject = Tr.t("ZLIMPORT_CONFIG_CHANGE_SUBJECT", getLanguage()) + " (" + hausverwaltung + ")";
				LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0003", subject, sb_postenchanges.toString(), "standardemail.html", session, this, null);
				lpmw.sendTo(to_addresses, cc_addresses, null);
			}
		}

		return configHasChanged;
	}

	/**
	 * Gets the SAX builder.
	 *
	 * @return the SAX builder
	 */
	private SAXBuilder getSAXBuilder()
	{
		String SAXPARSERCLASS = "";
		SAXBuilder builder = null;
		try
		{
			if(SAXPARSERCLASS.equals(""))
			{
				builder = new SAXBuilder(true);
			}
			else
			{
				builder = new SAXBuilder(SAXPARSERCLASS, true);
			}
			return builder;
		}
		catch(Exception xc)
		{
			debug.error(this, "Cannot get SAXBuilder :" + SAXPARSERCLASS);
			debug.log(xc);
		}
		return builder;
	}

	/**
	 * Gets the user id.
	 *
	 * @return the user id
	 */
	@Override
	public String getUserId()
	{
		if(null == session)
		{
			return null;
		}
		return session.getString("userid");
	}

	/**
	 * Gets the process name.
	 *
	 * @return the process name
	 */
	@Override
	public String getProcessName()
	{
		String importDisplayName = this.getString("var.name.DISPLAYNAME");
		String thisImportName = this.getString("var.name");
		if(!thisImportName.trim().equals(""))
		{
			importDisplayName += ": " + thisImportName;
		}
		return importDisplayName;
	}

	/**
	 * Gets the process id.
	 *
	 * @return the process id
	 */
	@Override
	public long getProcessId()
	{
		return this.processid;
	}

	/**
	 * Sets the process id.
	 *
	 * @param processId
	 *            the new process id
	 */
	@Override
	public void setProcessId(long processId)
	{
		this.processid = processId;
	}

	/**
	 * Gets the session.
	 *
	 * @return the session
	 */
	@Override
	public DynGenDataObj getSession()
	{
		return session;
	}

	/**
	 * updates Process Progress.
	 *
	 * @param progress
	 *            Percentage
	 * @param progressStatusInfo
	 *            Status Message
	 * @param processStatus
	 *            Status {@link ProcessStatus}
	 */
	protected void updateProgess(BigDecimal progress, String progressStatusInfo, ProcessStatus processStatus)
	{
		if(null != progress)
		{
			setProgress(progress);
		}

		if(pp != null)
		{
			pp.updateProgess(actualProgress, progressStatusInfo, processStatus);
		}
	}

	/**
	 * update actual progress.
	 *
	 * @param progress
	 *            as BigDecimal
	 */
	protected void setProgress(BigDecimal progress)
	{
		if(this.actualProgress.compareTo(progress) > 0)
		{
			BugMe.getInstance().error(this, "cannot decreaseprogress");
		}
		else
		{
			this.actualProgress = progress;
		}
	}

	/**
	 * Gets the sequence number.
	 *
	 * @param filename
	 *            the filename
	 * @return the sequence number
	 * @throws FredProcessException
	 *             the fred process exception
	 */
	public long getSequenceNumber(String filename) throws FredProcessException
	{
		if(StringUtils.isEmpty(filename))
		{
			debug.log(Magic.IMS.icrsfred.csv.processor.ProcessStatus.SEVERE, "File name is empty!");
		}

		Matcher matcher = FILE_NAME_PATTERN.matcher(filename);
		if(!matcher.matches())
		{
			debug.log(Magic.IMS.icrsfred.csv.processor.ProcessStatus.SEVERE, "File name does not match expected pattern: " + filename);
		}

		String seqAsString = matcher.group(2);
		return NumberUtils.toLong(seqAsString);
	}

	/**
	 * Update file upload status.
	 *
	 * @param dao
	 *            the dao
	 * @param fileUpload
	 *            the file upload
	 * @param fileSequenceNumber
	 *            the file sequence number
	 * @param rrstatus
	 *            the rrstatus
	 * @param rrmessage
	 *            the rrmessage
	 * @param rrlink
	 *            the rrlink
	 * @param rrplainlink
	 *            the rrplainlink
	 * @param hausoid
	 *            the hausoid
	 */
	private void updateFileUploadStatus(FredDAO dao, FileUpload fileUpload, long fileSequenceNumber, String rrstatus, String rrmessage, String rrlink, String rrplainlink, String hausoid)
	{
		if(fileUpload != null && fileSequenceNumber > -1)
		{

			if(hausoid != null && hausoid.matches("\\d+"))
			{
				fileUpload.setPropertyId(Long.valueOf(hausoid));
			}

			if(rrstatus != null && rrstatus.length() > 0)
			{
				fileUpload.setRrstatus(rrstatus);
			}

			if(rrmessage != null && rrmessage.length() > 0)
			{
				fileUpload.setRrmessage(rrmessage);
			};

			if(rrlink != null && rrlink.length() > 0)
			{
				fileUpload.setRrlink(rrlink);
			}

			if(rrplainlink != null && rrplainlink.length() > 0)
			{
				fileUpload.setRrplainlink(rrplainlink);
			}

			try
			{
				dao.saveFileUpload(fileUpload);
			}
			catch(PersistenceException e)
			{
				// only log the exception
				debug.error("Status for file-upload with seq " + fileSequenceNumber + " cannot be updated", e);
			}
		}
	}

	/**
	 * Builds the fred link.
	 *
	 * @param id
	 *            the id
	 * @param view
	 *            the view
	 * @param additionalParams
	 *            the additional params
	 * @param displayname
	 *            the displayname
	 * @param msg
	 *            the msg
	 * @param global
	 *            the global
	 * @param session
	 *            the session
	 * @return the string
	 */
	private String buildFredLink(String id, String view, String additionalParams, String displayname, String msg, DynGenDataObj global, DynGenDataObj session)
	{
		String dynurl = (String)CfgSingleton.getInstance().get("DYNAMIC_URLPATH", session, "dynamicurlpath");
		// non-critical errors, set default values if necessary
		// critical errors. we do not like crippled links.. so no link is returned at all.
		if(null == dynurl)
		{
			return "";
		}
		String flavour = session.getString("flavour");
		String sessid = session.getString("SESSIONID");

		String url = dynurl + "?OID=" + id + "&VIEW=" + view + "&HOOK=SET&wrapper=NO&ESSENCEID=" + sessid + "&" + additionalParams;
		String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
		StringBuffer urlSB = new StringBuffer();
		urlSB.append(CoolWebTool.getUsedDomain(session));
		urlSB.append(dynurl);
		String hijaxtarget = CfgSingleton.getHijaxTarget(session);

		urlSB.append("?OID=" + hijaxtarget + "&contenturl=");
		urlSB.append(encodedUrl);
		urlSB.append("&FLAVOUR=");
		urlSB.append(flavour);
		urlSB.append("&ESSENCEID=");
		urlSB.append(sessid);

		String link = urlSB.toString();

		link = link.replaceAll("&FLAVOUR=&", "");
		link = link.replaceAll("&FLAVOUR=null", "");
		link = link.replaceAll("&FLAVOUR=\"", "\"");
		link = link.replaceAll("&ESSENCEID=&", "");
		link = link.replaceAll("&ESSENCEID=\"", "\"");

		try
		{
			link = link.replaceAll(URLEncoder.encode("&FLAVOUR=&", StandardCharsets.UTF_8.toString()), "");
			link = link.replaceAll(URLEncoder.encode("&FLAVOUR=null", StandardCharsets.UTF_8.toString()), "");
			link = link.replaceAll(URLEncoder.encode("&FLAVOUR=\"", StandardCharsets.UTF_8.toString()), "\"");
			link = link.replaceAll(URLEncoder.encode("&ESSENCEID=&", StandardCharsets.UTF_8.toString()), "");
			link = link.replaceAll(URLEncoder.encode("&ESSENCEID=", StandardCharsets.UTF_8.toString()) + "\"", "\"");
			link = CoolTemplateTool.removeEssenceID(link);
		}
		catch(UnsupportedEncodingException e)
		{
			BugMe.getInstance().error(e);
		}

		return urlSB.toString();
	}

	/**
	 * Get the previous ZZ.
	 *
	 * @param id
	 *            the id
	 * @param zl
	 *            the zl
	 * @return the previous ZZ
	 */
	private DynGenDataObj getPreviousZZ(String id, Zinsliste zl)
	{
		DynGenDataObj previousZZ = null;

		try
		{
			ArgsHelper argsHelper = new ArgsHelper(new Hashtable<String, Object>());
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

			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}
			QueryResult qr = null;
			Vector<Hashtable<String, String>> result = null;

			qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			result = qr.getResult();

			if(result != null && result.size() > 0)
			{
				Hashtable row = result.get(0);

				String zzId = String.valueOf(row.get("zzid"));
				if(zzId != null && zzId.length() > 0)
				{
					previousZZ = (DynGenDataObj)DAInst.getObject(zzId, null);
				}
			}
		}
		catch(Exception e)
		{
			debug.error(e);
		}

		return previousZZ;
	}

	/**
	 * Returns the oid from rentalunit and rental agreement area.
	 *
	 * @param session2
	 *            the session 2
	 * @return the oid and area of vacant rentrolls
	 */
	private Hashtable<String, String> getOidAndAreaOfVacantRentrolls(DynGenDataObj session2)
	{
		Hashtable<String, String> oidAndArea = new Hashtable<>();

		try
		{
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();

			ArgsHelper argsHelper = new ArgsHelper();
			argsHelper.setMainTemplateType("CIMS.top");
			argsHelper.setAdvancedFields(true);
			argsHelper.addCondition("leerstehung", "1");
			argsHelper.addCondition("stellplatz", "0");
			argsHelper.addDomainCondition(session);
			argsHelper.addField("ID", "oid");
			argsHelper.addField("ET0.mvflaeche/100.", "mvflaeche");

			// new Connector Class
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();

			if(res != null && res.size() > 0)
			{
				for(int i = 0; i < res.size(); i++)
				{

					Hashtable<String, String> row = res.get(i);

					String oid = row.get("oid");
					String mvflaeche = row.get("mvflaeche");
					mvflaeche = formatString(mvflaeche);

					oidAndArea.put(oid, mvflaeche);
				}
			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().log(e);
		}

		return oidAndArea;
	}

	/**
	 * Format string.
	 *
	 * @param value
	 *            the value
	 * @return the string
	 */
	private String formatString(String value)
	{
		try
		{
			DecimalFormat df = new DecimalFormat("#,##0.00", symbolsDE_DE);
			if(value.contains("\\.") && value.contains(","))
			{
				value = value.replaceAll("\\.", "");
			}

			value = value.replaceAll(",", ".");

			String result = df.format(Double.parseDouble(value));
			return result;
		}
		catch(Exception e)
		{
			return value;
		}
	}

	/**
	 * Liefert Ablaufende Mietvertraege der Uebergebenene Periode.
	 *
	 * @param startDatum
	 *            the start datum
	 * @param endDatum
	 *            the end datum
	 * @param assetmanager
	 *            the assetmanager
	 * @return the ablaufende vertraege for assetmanager
	 */
	private Hashtable<String, String> getAblaufendeVertraegeForAssetmanager(Calendar startDatum, Calendar endDatum, String assetmanager)
	{

		Hashtable<String, String> ablaufendevertraege = new Hashtable<>();

		try
		{
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();

			ArgsHelper argsHelper = new ArgsHelper();

			argsHelper.setAdvancedFields(true);
			argsHelper.setMainTemplateType("CIMS.top");
			argsHelper.addTemplateType("REVtops", "CIMS.haus");
			argsHelper.addTemplateType("REVtops_assetmanager", "ICRScrm.assetmanager");

			argsHelper.addCondition("REVtops_assetmanager_name", assetmanager.substring(assetmanager.indexOf(";") + 1));
			argsHelper.addDomainCondition(session);
			argsHelper.addWhere("ET0.mietvertragbis <= CONVERT(datetime, '" + eDate.stringFromDate(endDatum.getTime()) + "', 104) AND ET0.mietvertragbis >= CONVERT(datetime, '" + eDate.stringFromDate(startDatum.getTime()) + "', 104) AND ET0.status='1'");

			argsHelper.addField("ID", "oid");
			argsHelper.addField("DOB.name", "topname");
			argsHelper.addField("REVtops_name", "adresse");
			argsHelper.addField("REVtops_identadresse1", "sapnummer");
			argsHelper.addField("REVtops_identadresse5", "senummer");
			argsHelper.addField("ET0.vertragid");
			argsHelper.addField("ET0.mieter");
			argsHelper.addField("ET0.istmietepm/100.", "istmietepm");
			argsHelper.addField("ET0.mietvertragbis");

			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();

			if(res != null && res.size() > 0)
			{
				StringBuffer resultLines = new StringBuffer();

				for(int i = 0; i < res.size(); i++)
				{
					Hashtable<String, String> row = res.get(i);

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
					if(mietvertragbis != null && mietvertragbis.contains(" "))
					{
						mietvertragbis = mietvertragbis.substring(0, mietvertragbis.indexOf(" ")).trim();
					}

					String topurl = CoolStringTool.buildLink(oid, "SHOW", "", topname, "", "_blank", "ajaxLink redlink", global, session);

					String line = sapnummer + " " + adresse + " " + topurl + ", Vertragsnummer: " + vertragid + ", Mieter: <b>" + mieter + "</b>, Miete p.M.: " + istmietepm + "&euro; Mietvertragsende: <b>" + mietvertragbis + "</b><br><br>";

					resultLines.append(line);
				}

				if(resultLines.toString().length() > 0)
				{
					StringBuffer salutation = new StringBuffer();

					salutation.append("Sehr geehrte(r) " + assetmanager.substring(assetmanager.indexOf(";") + 1) + "!");
					salutation.append("<br>");
					salutation.append("<br>");
					salutation.append("Die folgenden Verträge laufen in den nächsten 6 Monaten ab:");
					salutation.append("<br>");
					salutation.append("<br>");
					salutation.append(resultLines.toString());
					salutation.append("<br>");
					salutation.append("<br>");
					salutation.append("Sollte eine Verlängerung oder Wiedervermietung geplant sein, ersuchen wir um <b>rechtzeitige Erfassung im SAP</b> bis zum Monatsletzten des Vormonats, um den korrekten Datenstand im PMS abbilden zu können.");
					salutation.append("<br>");
					salutation.append("<br>");
					salutation.append("Eine verzögerte Erfassung kann in einer höheren Leerstandsquote resultieren!");
					salutation.append("<br>");
					salutation.append("<br>");

					if(assetmanagerAndIDs == null || assetmanagerAndIDs.size() == 0)
					{
						assetmanagerAndIDs = getAllAssetmanagerAndIds(session);
					}

					String dynurl = (String)CfgSingleton.getInstance().get("DYNAMIC_URLPATH", session, "dynamicurlpath");
					if(null == dynurl)
					{
						debug.error(this, "cannot read DYNAMIC_URLPATH!");
						dynurl = "/NA";
					}

					String sessid = session.getString("SESSIONID");
					String linkClass = "ajaxLink";
					String linkTarget = "_blank";
					String url = dynurl + "?OID=DIRECT_ICRS.reports.report&reporttemplate=ICRS.reports.icrsare.auslaufendevertraegerepare";

					if(assetmanagerAndIDs.containsKey(assetmanager.substring(assetmanager.indexOf(";") + 1)))
					{
						url += "&addfilterpreselectedvalues=queryassetmanager_ID=" + assetmanagerAndIDs.get(assetmanager.substring(assetmanager.indexOf(";") + 1));
					}

					url += "&VIEW=SHOW&wrapper=NO";
					String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8);
					StringBuffer urlSB = new StringBuffer();
					urlSB.append("<a href=\"");
					urlSB.append(CoolWebTool.getUsedDomain(session));
					urlSB.append(dynurl);
					urlSB.append("?OID=" + CfgSingleton.getHijaxTarget(session) + "&contenturl=");
					urlSB.append(encodedUrl);
					urlSB.append("&FLAVOUR=");
					urlSB.append(flavour);
					urlSB.append("&ESSENCEID=");
					urlSB.append(sessid);
					urlSB.append("\" ");
					if(null != linkClass && linkClass.trim().length() > 0)
					{
						urlSB.append(" class=\"" + linkClass + "\" ");
					}
					if(null != linkTarget && linkTarget.trim().length() > 0)
					{
						urlSB.append(" target= \"" + linkTarget + "\" ");
					}
					urlSB.append(">");
					urlSB.append("hier");
					urlSB.append("</a>");

					salutation.append("Zur Abfrage der aktuell ablaufenden Mietverträge für Ihr Teilportfolio klicken Sie bitte " + urlSB + ".");

					salutation.append("<br>");

					ablaufendevertraege.put(assetmanager, salutation.toString());
				}

			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().log(e);
		}

		return ablaufendevertraege;

	}

	/**
	 * Gets the all assetmanager and ids.
	 *
	 * @param session2
	 *            the session 2
	 * @return the all assetmanager and ids
	 */
	private Hashtable<String, String> getAllAssetmanagerAndIds(DynGenDataObj session2)
	{
		Hashtable<String, String> assetmanagerAndIDs = new Hashtable<>();

		try
		{
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();

			ArgsHelper argsHelper = new ArgsHelper();

			argsHelper.setAdvancedFields(true);
			argsHelper.setMainTemplateType("ICRScrm.assetmanager");
			argsHelper.addDomainCondition(session);
			argsHelper.addField("ID", "oid");
			argsHelper.addField("DOB.name", "assetmanagername");

			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();

			if(res != null && res.size() > 0)
			{
				StringBuffer resultLines = new StringBuffer();

				for(int i = 0; i < res.size(); i++)
				{
					Hashtable<String, String> row = res.get(i);

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
	 * <b>Implemented in {@link ExcelObjectUploadXMLDefined}!</b><br>
	 * Fetch data from xml defined databasesource.<br>
	 *
	 * Vector with one hashtable for each row, key = column name, value = value<br>
	 *
	 * @param databasesource
	 *            the name of element 'databasesource' in the xml config. Location of the config file is defined by {@link ExcelObjectUploadXMLDefined#getXmlUploadConfigDirectory}
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the data from the databasesource query
	 */
	protected Vector getDatabaseContent(String databasesource, String uploadlistetypeconfig)
	{
		Vector excelUploadData = null;

		DatabaseSource uq = executeDatabaseSource(databasesource, uploadlistetypeconfig);
		if(uq == null)
		{
			return null;
		}
		String fileOID = uq.getDataCsvFileOID();
		if(fileOID != null)
		{
			this.set("var.xlsfile", fileOID);
		}

		excelUploadData = uq.getQueryResult().getResult();

		return excelUploadData;
	}

	/**
	 * Triggers setup of XML Configs {@link #setupDatabasesourceXMLConfigs(String)} and executes the databasesource query {@link #executeQuery(String, UploadListeImport)}.
	 *
	 * @param databasesource
	 *            the name of element 'databasesource' in the xml config. Location of the config file is defined by {@link ExcelOpenItemUploadXMLDefined#getXmlUploadConfigDirectory}
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the already executed {@link DatabaseSource}, <code>null</code> if setup failed.
	 */
	protected DatabaseSource executeDatabaseSource(String databasesource, String uploadlistetypeconfig)
	{
		if(!setupDatabasesourceXMLConfigs(databasesource, uploadlistetypeconfig))
		{
			BugMe.getInstance().error(this, "Setup of XML Configs for databasesource '" + databasesource + "' failed! No database content fetched.");
			return null;
		}

		DatabaseSource uq = executeQuery(databasesource, uploadlistetypeconfig);
		return uq;
	}

	/**
	 * setup of xml files and params from xml files (importclass, ...)
	 *
	 * @param databasesource
	 *            the name of element 'databasesource' in the xml config. Location of the config file is defined by {@link ExcelOpenItemUploadXMLDefined#getXmlUploadConfigDirectory}
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	private boolean setupDatabasesourceXMLConfigs(String databasesource, String uploadlistetypeconfig)
	{
		// hole die xml config, fuer die databasesource (Bsp: "Gryphon_IDLkonsis_salden")
		UploadListeTypeConfig ulTypeConfig = getUploadListeTypeConfig(databasesource, uploadlistetypeconfig);

		if(ulTypeConfig == null)
		{
			return false;
		}

		debug.log("File type '" + ulTypeConfig.getName() + "' for databasesource '" + databasesource + "' used");

		// ab hier nur wegen enrichData in BHKontozeileListImport.java...
		String exceluploadxmlconfigdirectory = getExcelUploadXmlConfigDirectory();
		if(exceluploadxmlconfigdirectory == null || exceluploadxmlconfigdirectory.equals(""))
		{
			// csvErrorCollector.addError(-1, "", "", Tr.t("missingEssenceCfgParam", getLanguage()), CSVErrorCollector.SEVERITY_HIGH, Tr.t("missingEssenceCfgParam", getLanguage()) + "
			// \"EXCELUPLOADXMLCONFIGDIRECTORY\".", Tr.t("missingEssenceCfgParam", getLanguage()), "", "");
			// errorrecords++;
			debug.log("Fehlender essence.cfg parameter EXCELUPLOADXMLCONFIGDIRECTORY!");
			return false;
		}

		// kundenspezifische, allgemeingueltige conversions. Z.b. Landselectoron, long to double, ... (wie reportexportconfig)
		String xmlUploadConversioneConfigFileName = exceluploadxmlconfigdirectory + "uploadlisteconversionconfig.xml";
		xmlUploadConversioneConfigFileName = CoolStringTool.getFlavouredFilename(xmlUploadConversioneConfigFileName, session);

		// currency config anhand der currency werte beim import umgerechnet werden TODO: ist das so notwendig? neue MultiCurrency features...
		String xmlCurrencyConfigFileName = (String)CfgSingleton.getInstance().get("ZINSLISTENCURRENCYCONFIG");
		xmlCurrencyConfigFileName = CoolStringTool.getFlavouredFilename(xmlCurrencyConfigFileName, session);

		DynGenDataObj templateDGD = new DynGenDataObj(this, debug);

		String importconfigForExcelObjectUpload = this.getString("var.importconfig");
		if(importconfigForExcelObjectUpload == null || importconfigForExcelObjectUpload.equals(""))
		{
			this.set("var.importconfig", ulTypeConfig.getUploadListeImportConfigFileName());
		}
		else
		{
			debug.log("xml config override '" + ulTypeConfig.getUploadListeImportConfigFileName() + "' with '" + importconfigForExcelObjectUpload + "'");
		}
		return true;
	}

	/**
	 * Fetches the {@link DatabaseSource} with ({@link DatabaseSourceConfig#getDatabaseSource()}) for the {@code databasesource} in the xml config.<br>
	 * The full file path of the xml config is built in {@link #getFlavouredUploadQueryDataConfig(String)}.<br>
	 * Executes {@link DatabaseSource#executeSelect()} and returns it.<br>
	 *
	 * @param databasesource
	 *            the name of element 'databasesource' in the xml config. Location of the config file is defined by {@link ExcelObjectUploadXMLDefined#getXmlUploadConfigDirectory}
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the executed UploadQuery with the data already loaded
	 */
	protected DatabaseSource executeQuery(String databasesource, String uploadlistetypeconfig)
	{
		try
		{
			DatabaseSourceConfig uploadDatabaseSourceConfig = getUploadDatabaseSourceConfig(databasesource, uploadlistetypeconfig);
			DatabaseSource uploadDatabaseSource = uploadDatabaseSourceConfig.getDatabaseSource();
			// TODO statement params
			Hashtable<String, String> selectstatementParams = uploadDatabaseSource.getSelectstatementParams();

			Hashtable<String, String> paramValues = null;

			String month = getString("var.monatvon");
			String year = getString("var.jahrvon");

			if(month.length() == 0 && year.length() == 0)
			{
				month = String.valueOf(new Date().getMonth() + 1);
				year = String.valueOf(new Date().getYear() + 1900);
			}

			Calendar cal = GregorianCalendar.getInstance();
			cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
			cal.set(Calendar.YEAR, Integer.parseInt(year));
			cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));

			String lastDayOfMonth = String.valueOf(cal.getActualMaximum(Calendar.DAY_OF_MONTH));

			String tmpMonth = month;
			if(tmpMonth.length() == 1)
			{
				tmpMonth = "0" + tmpMonth;
			}

			String startOfMonth = "01." + tmpMonth + "." + year;
			String endOfMonth = lastDayOfMonth + "." + tmpMonth + "." + year;

			paramValues = new Hashtable<>();
			paramValues.put("{startofmonth}", startOfMonth);
			paramValues.put("{endofmonth}", endOfMonth);

			uploadDatabaseSource.executeSelectAndStore(paramValues);
			return uploadDatabaseSource;
		}
		catch(Exception e)
		{
			// csvErrorCollector.addError(-1, "", "", e.getMessage(), CSVErrorCollector.SEVERITY_HIGH, Tr.t("executingUploadQueryFailed", getLanguage()), Tr.t("executingUploadQueryFailed",
			// getLanguage()), "", "");
			// errorrecords++;
			debug.error(this, Tr.t("executingUploadQueryFailed", getLanguage()));
			debug.log(e);
			return null;
		}
	}

	/**
	 * Gets the upload database source config.
	 *
	 * @param databasesource
	 *            the databasesource
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the upload database source config
	 */
	protected DatabaseSourceConfig getUploadDatabaseSourceConfig(String databasesource, String uploadlistetypeconfig)
	{
		try
		{
			DatabaseSourceConfig uploadDatabaseSourceConfig = getFlavouredUploadQueryDataConfig(databasesource, uploadlistetypeconfig);
			if(uploadDatabaseSourceConfig == null)
			{
				BugMe.getInstance().error(this, "UploadDatabaseSourceConfig for '" + databasesource + "' is null!");
				return null;
			}
			if(!uploadDatabaseSourceConfig.getXMLConfig())
			{
				BugMe.getInstance().error(this, "Parsing xml UploadDatabaseSourceConfig '" + databasesource + "' failed!");
				return null;
			}
			return uploadDatabaseSourceConfig;
		}
		catch(Exception e)
		{
			// csvErrorCollector.addError(-1, "", "", e.getMessage(), CSVErrorCollector.SEVERITY_HIGH, Tr.t("errorFetchingUploadQueryConfig", getLanguage()), Tr.t("errorFetchingUploadQueryConfig",
			// getLanguage()), "", "");
			// errorrecords++;
			debug.error(this, Tr.t("errorFetchingUploadQueryConfig", getLanguage()));
			debug.log(e);
			return null;
		}
	}

	/**
	 * Gets the flavoured upload query data config.
	 *
	 * @param databasesource
	 *            the databasesource
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the flavoured upload query data config
	 */
	private DatabaseSourceConfig getFlavouredUploadQueryDataConfig(String databasesource, String uploadlistetypeconfig)
	{
		try
		{
			// if(csvErrorCollector == null)
			// {
			// csvErrorCollector = new CSVErrorCollector(session);
			// csvErrorCollector.setOffset(getInteger("var.columnline", Integer.valueOf(0)));
			// }

			UploadListeTypeConfig ulTypeConfig = getUploadListeTypeConfig(databasesource, uploadlistetypeconfig);

			if(ulTypeConfig == null)
			{
				return null;
			}
			String xmlConfigFilename = ulTypeConfig.getUploadListeImportConfigFileName();
			if(xmlConfigFilename == null)
			{
				// TODO Tr.t
				// csvErrorCollector.addError(-1, "", "", Tr.t("noImportConfigFileName", getLanguage()), CSVErrorCollector.SEVERITY_HIGH, Tr.t("noImportConfigFileName", getLanguage()),
				// Tr.t("noImportConfigFileName", getLanguage()), "", "");
				// errorrecords++;
				return null;
			}
			return new DatabaseSourceConfig(xmlConfigFilename, debug);
		}
		catch(Exception e)
		{
			// csvErrorCollector.addError(-1, "", "", e.getMessage(), CSVErrorCollector.SEVERITY_HIGH, Tr.t("noUploadListeTypeConfigFound", getLanguage()), Tr.t("noUploadListeTypeConfigFound",
			// getLanguage()), "", "");
			// errorrecords++;
			debug.error(this, "could net get uploadlistetypeconfig for databasesource '" + databasesource + "'");
			debug.log(e);
			return null;
		}
	}

	/**
	 * Gets the upload liste type config.
	 *
	 * @param databasesource
	 *            the databasesource
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the upload liste type config
	 */
	private UploadListeTypeConfig getUploadListeTypeConfig(String databasesource, String uploadlistetypeconfig)
	{
		try
		{
			// if(csvErrorCollector == null)
			// {
			// csvErrorCollector = new CSVErrorCollector(session);
			// csvErrorCollector.setOffset(getInteger("var.columnline", Integer.valueOf(0)));
			// }

			String xmlUploadTypeConfigFileName = null;
			String dbTable = this.getString("DB_TABLE");

			String exceluploadxmlconfigdirectory = getExcelUploadXmlConfigDirectory();
			if(exceluploadxmlconfigdirectory == null || exceluploadxmlconfigdirectory.equals(""))
			{
				// csvErrorCollector.addError(-1, "", "", Tr.t("missingEssenceCfgParam", getLanguage()), CSVErrorCollector.SEVERITY_HIGH, Tr.t("missingEssenceCfgParam", getLanguage()) + "
				// \"EXCELUPLOADXMLCONFIGDIRECTORY\".", Tr.t("missingEssenceCfgParam", getLanguage()), "", "");
				// errorrecords++;
				debug.log("Fehlender essence.cfg parameter EXCELUPLOADXMLCONFIGDIRECTORY!");
				return null;
			}

			debug.error("exceluploadxmlconfigdirectory: " + exceluploadxmlconfigdirectory);

			if(!exceluploadxmlconfigdirectory.endsWith(File.separator))
			{
				exceluploadxmlconfigdirectory = exceluploadxmlconfigdirectory + File.separator;
			}
			xmlUploadTypeConfigFileName = getXmlUploadTypeConfigFileName(dbTable, exceluploadxmlconfigdirectory, uploadlistetypeconfig);

			UploadListeTypeIdentifyer ulTypeIdentifier = new UploadListeTypeIdentifyer(xmlUploadTypeConfigFileName, debug);

			UploadListeTypeConfig ulTypeConfig = ulTypeIdentifier.getUploadListeType(databasesource);

			if(ulTypeConfig == null)
			{
				// TODO Tr.t
				// csvErrorCollector.addError(-1, "", "", Tr.t("noUploadListeTypeConfigFound", getLanguage()), CSVErrorCollector.SEVERITY_HIGH, Tr.t("noUploadListeTypeConfigFound", getLanguage()),
				// Tr.t("noUploadListeTypeConfigFound", getLanguage()), "", "");
				// errorrecords++;
				debug.log(this, "No UploadListeTypeConfig found for databasesource '" + databasesource + "'");
			}
			return ulTypeConfig;

		}
		catch(Exception e)
		{
			// csvErrorCollector.addError(-1, "", "", e.getMessage(), CSVErrorCollector.SEVERITY_HIGH, Tr.t("noUploadListeTypeConfigFound", getLanguage()), Tr.t("noUploadListeTypeConfigFound",
			// getLanguage()), "", "");
			// errorrecords++;
			debug.error(this, "could net get uploadlistetypeconfig for databasesource '" + databasesource + "'");
			debug.log(e);
			return null;
		}
	}

	/**
	 * Hole EXCELUPLOADXMLCONFIGDIRECTORYaus essence.cfg.<br>
	 * fallback: home aus essence.cfg + "\config"
	 *
	 * @return the excel upload xml config directory
	 */
	private String getExcelUploadXmlConfigDirectory()
	{
		String exceluploadxmlconfigdirectory = CfgSingleton.getInstance().getString("EXCELUPLOADXMLCONFIGDIRECTORY").trim();
		if(exceluploadxmlconfigdirectory.equals(""))
		{
			String homedir = CfgSingleton.getInstance().getString("home").trim();
			if(!homedir.equals(""))
			{
				if(!homedir.endsWith(File.separator))
				{
					homedir = homedir + File.separator;
				}
				exceluploadxmlconfigdirectory = homedir + "config";
			}
		}
		if(!exceluploadxmlconfigdirectory.endsWith(File.separator))
		{
			exceluploadxmlconfigdirectory = exceluploadxmlconfigdirectory + File.separator;
		}
		return exceluploadxmlconfigdirectory;
	}

	/**
	 * Gets the xml upload type config file name.
	 *
	 * @param dbTable
	 *            usually the value of this.getString("DB_TABLE");
	 * @param exceluploadxmlconfigdirectory
	 *            usuall the value from {@link #getExcelUploadXmlConfigDirectory()}
	 * @param uploadlistetypeconfig
	 *            the uploadlistetypeconfig
	 * @return the full xml config file path, null if anything went wrong
	 */
	private String getXmlUploadTypeConfigFileName(String dbTable, String exceluploadxmlconfigdirectory, String uploadlistetypeconfig)
	{
		try
		{
			if(exceluploadxmlconfigdirectory != null)
			{
				String xmlUploadTypeConfigFileName = null;

				// try get it from upload Template
				xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + uploadlistetypeconfig + ".xml";

				if(dbTable != null && xmlUploadTypeConfigFileName == null)
				{
					if(!exceluploadxmlconfigdirectory.endsWith(File.separator))
					{
						exceluploadxmlconfigdirectory = exceluploadxmlconfigdirectory + File.separator;
					}
					if(dbTable.equals("cims_finanzzahlenxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "finanzzahlenlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_hausxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "hauslistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_kreditxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "kreditlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_hausdwhxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "hausdwhlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_top2xls") || dbTable.equals("cims_topxls") || dbTable.equals("cims_caifmxls") || dbTable.equals("cims_skidataxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "toplistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_bhkontozeilexls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "bhkontozeilelistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_zahlungseingangxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "bhkontozeilelistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_bhkontozeilegesellschaftxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "bhkontozeilegesellschaftlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_baukostenxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "baukostenttypeconfig.xml";
					}
					else if(dbTable.equals("cims_kreditdwhxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "kreditdwhlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_openitemxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "openitemlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_kautionxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "kautionlistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_marketdatakennzahlxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "marketdatakennzahllistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_nutscodeplzmappingxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "nutscodeplzmappinglistetypeconfig.xml";
					}
					else if(dbTable.equals("cims_vorlaeufigemietvorschreibungxls"))
					{
						xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "vorlaeufigemietvorschreibunglistetypeconfig.xml";
					}
				}
				// default config
				if(xmlUploadTypeConfigFileName == null)
				{
					xmlUploadTypeConfigFileName = exceluploadxmlconfigdirectory + "uploadlistetypeconfig.xml";
				}

				debug.error("xmlUploadTypeConfigFileName: " + xmlUploadTypeConfigFileName);
				if(session == null)
				{
					debug.error("getXmlUploadTypeConfigFileName - SESSION IS NULL");
				}
				xmlUploadTypeConfigFileName = CoolStringTool.getFlavouredFilename(xmlUploadTypeConfigFileName, session);
				debug.error("xmlUploadTypeConfigFileName Flavoured: " + xmlUploadTypeConfigFileName);

				debug.log(this, Tr.t("xmlUploadTypeConfigFileName", getLanguage()) + " '" + xmlUploadTypeConfigFileName);

				return xmlUploadTypeConfigFileName;
			}
			BugMe.getInstance().error(this, "Cannot build full xml config file path - No exceluploadxmlconfigdirectory!");
		}
		catch(Exception e)
		{
			BugMe.getInstance().error(this, "Cannot build full xml config file path!");
			BugMe.getInstance().log(e);
		}
		return null;
	}

	/**
	 * Gets the language.
	 *
	 * @return the language
	 */
	public String getLanguage()
	{
		if(mylang == null || mylang.equals(""))
		{
			if(session != null)
			{
				mylang = session.getString("language").trim();
			}
			else
			{
				mylang = "";
			}
		}
		if(mylang.equalsIgnoreCase("DE"))
		{
			mylang = "";
		}
		return mylang;
	}

	/**
	 * Who called me.
	 *
	 * @return the string
	 */
	public static String whoCalledMe()
	{
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		StackTraceElement caller = stackTraceElements[4];
		String classname = caller.getClassName();
		String methodName = caller.getMethodName();
		int lineNumber = caller.getLineNumber();
		return classname + "." + methodName + ":" + lineNumber;
	}

	/**
	 * Show call stack.
	 *
	 * @param debug
	 *            the debug
	 */
	public static void showCallStack(BugMe debug)
	{
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for(int i = 2; i < stackTraceElements.length; i++)
		{
			StackTraceElement ste = stackTraceElements[i];
			String classname = ste.getClassName();
			String methodName = ste.getMethodName();
			int lineNumber = ste.getLineNumber();
			debug.error(classname + "." + methodName + ":" + lineNumber);
		}
	}

	/**
	 * Fix file link.
	 * file is a reserved word in mysql so cannot be exposed ... therefore this workaround
	 */
	public void fixFileLink()
	{
		// file is a reserved word in mysql so cannot be exposed ... therefore this workaround
		if(!getString("var.datei").equals(getString("var.file")))
		{
			set("var.datei", getString("var.file"));
		}
		if(!getString("var.edatei").equals(getString("var.efile")))
		{
			set("var.edatei", getString("var.efile"));
		}
	}

	/**
	 * Check leerstand string.
	 *
	 * @param actualmieter
	 *            the actualmieter
	 * @return true, if successful
	 */
	public boolean checkLeerstandString(String actualmieter)
	{
		if(validationService == null)
		{
			validationService = new ZinslistenValidationService(session, debug, DAInst);
		}
		return validationService.checkLeerstandString(actualmieter);
	}

	/**
	 * Gets the hausverwaltung from haus oid.
	 *
	 * @param oid_haus
	 *            the oid haus
	 * @return the hausverwaltung from haus oid
	 */
	private void getHausverwaltungFromHausOid(String oid_haus)
	{
		try
		{
			Vector<Hashtable<String, String>> res = new Vector<Hashtable<String, String>>();

			ArgsHelper argsHelper = new ArgsHelper();
			argsHelper.setMainTemplateType("CIMS.haus");
			argsHelper.addTemplateType("hausverwaltungneu", "ICRScrm.firma");
			argsHelper.setAdvancedFields(true);

			argsHelper.addCondition("ID", oid_haus);
			argsHelper.addDomainCondition(session);
			argsHelper.addField("ID");
			argsHelper.addField("hausverwaltungneu_name", "hausverwaltung");

			// new Connector Class
			if(null == DAInst)
			{
				net.metamagix.essence.Agents.Connector conn = new net.metamagix.essence.Agents.Connector();
				DAInst = conn.getDataAgent();
			}

			QueryResult qr = DAInst.queryObjectWithResult(argsHelper.getArgs());
			res = qr.getResult();

			if(res != null && res.size() == 1)
			{
				Hashtable<String, String> row = res.get(0);
				String hausverwaltung = row.get("hausverwaltung");
				this.hausverwaltung = hausverwaltung;
			}
		}
		catch(Exception e)
		{
			BugMe.getInstance().error(e);
		}
	}

	/**
	 * Creates the ZZ json reply.
	 *
	 * @param status
	 *            the status
	 * @param message
	 *            the message
	 * @return the string
	 */
	public String createZZJsonReply(String status, String message, org.json.simple.JSONArray zl_sel_json, org.json.simple.JSONArray el_sel_json)
	{
		// TODO Auto-generated catch block
		org.json.simple.JSONObject reply = new org.json.simple.JSONObject();
		try
		{
			reply.put("status", status);
			reply.put("message", message);
			if(null != zl_sel_json)
			{
				reply.put("rentrollss", zl_sel_json);
			}
			if(null != el_sel_json)
			{
				reply.put("ownerslists", el_sel_json);
			}
			reply.put("file", getString("var.file"));
			reply.put("efile", getString("var.efile"));
			reply.put("datei", getString("var.datei"));
			reply.put("edatei", getString("var.edatei"));
			reply.put("rentrollimportaftersale", getString("var.rentrollimportaftersale"));
			reply.put("filepath", getString("var.filepath"));
			reply.put("filepathbackup", getString("var.filepathbackup"));
			reply.put("name", getString("var.name"));
			reply.put("nameEN", getString("var.nameEN"));
			reply.put("objektname", getString("var.objektname"));
			reply.put("text", getString("var.text"));
			reply.put("vermietungtopuebeschreibtzinsliste", getString("var.vermietungtopuebeschreibtzinsliste"));
			reply.put("vermietungtopuebeschreibtzinslistemonate", getString("var.vermietungtopuebeschreibtzinslistemonate"));
			reply.put("vermietungtopuebeschreibtzinslisteaction", getString("var.vermietungtopuebeschreibtzinslisteaction"));
			reply.put("resultcode", getString("var.resultcode"));
			reply.put("errorcode", getString("var.errorcode"));
			reply.put("errorcodetxt", getString("var.errorcodetxt"));
			reply.put("zlstatus", getString("var.zlstatus"));
			reply.put("selectedkunde", getString("var.selectedkunde"));
			reply.put("jahr", getString("var.jahr"));
			reply.put("email", getString("var.email"));
			reply.put("mailtext", getString("var.mailtext"));
			reply.put("wertaenderung", getString("var.wertaenderung"));
			reply.put("assetmanagerinfo", getString("var.assetmanagerinfo"));
			reply.put("sendmailonlyonchange", getString("var.sendmailonlyonchange"));
			reply.put("periodenvergleich", getString("var.periodenvergleich"));
			reply.put("leerstandsmail", getString("var.leerstandsmail"));
			reply.put("ablaufendevetraegemail", getString("var.ablaufendevetraegemail"));
			reply.put("altezinszeilenloeschen", getString("var.altezinszeilenloeschen"));
			reply.put("topnamenneusetzten", getString("var.topnamenneusetzten"));
			reply.put("quellsystem", getString("var.quellsystem"));
			reply.put("zinslistendatum", getString("var.zinslistendatum"));
			reply.put("topoanpassung", getString("var.topoanpassung"));
			reply.put("ccemail", getString("var.ccemail"));
			reply.put("monat", getString("var.monat"));
			reply.put("tag", getString("var.tag"));
			reply.put("zinslistenindex", getString("var.zinslistenindex"));
			reply.put("eigentuemerlistenindex", getString("var.eigentuemerlistenindex"));
			reply.put("land", getString("var.land"));
			reply.put("ort", getString("var.ort"));
			reply.put("adresse", getString("var.adresse"));
			reply.put("identadresse5", getString("var.identadresse5"));
			reply.put("importstatus", getString("var.importstatus"));
			reply.put("hausverwaltung", getString("var.hausverwaltung"));
			reply.put("hausverwalter", getString("var.hausverwalter"));
			reply.put("betreuer", getString("var.betreuer"));
			reply.put("duration", getString("var.duration"));
			reply.put("filename", getString("var.filename"));
			reply.put("nighthour", getString("var.nighthour"));
			reply.put("nightminute", getString("var.nightminute"));
			reply.put("width", getString("var.width"));
			reply.put("jahrvon", getString("var.jahrvon"));
			reply.put("monatvon", getString("var.monatvon"));
			reply.put("encoding", getString("var.encoding"));
			reply.put("statusformissingunit", getString("var.statusformissingunit"));
			reply.put("topmatcherselector", getString("var.topmatcherselector"));
			reply.put("ignorealleasyerros", getString("var.ignorealleasyerros"));
			reply.put("importsperrebeidatenfreigabe", getString("var.importsperrebeidatenfreigabe"));

			reply.put("gridimport", getString("var.gridimport"));
			reply.put("starttime", getString("var.starttime"));
			reply.put("endtime", getString("var.endtime"));
			reply.put("runtime", getString("var.runtime"));
			reply.put("checkexistingrentroll", getString("var.checkexistingrentroll"));
			reply.put("zltypename", getString("var.zltypename"));
		}
		catch(Exception e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return reply.toString();
	}

	/**
	 * Creates a Dynamic GUI Definition (DGD) for selecting a rent roll.
	 *
	 * @param zl_sel_json
	 *            A JSON array containing the selectable rent roll entries.
	 *            If null, no selector field is added to the DGD.
	 * @return
	 *         The serialized DGD JSON string representing the rent roll selection view.
	 */
	public String createRentRollObjectSelectDgd(org.json.simple.JSONArray zl_sel_json)
	{
		String myLang = session.getString("language");
		String myId = getId();

		String title = Tr.t("textRentRollImport", myLang);
		DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
		DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
		List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();
		List<DgdJson.Field> fields = new ArrayList<>();

		if(zl_sel_json != null)
		{
			String fieldId = "zinslistenindex";
			fields.add(DgdJson.FieldFactory.selector(fieldId, Tr.t("textRentRoll", myLang), myId + "__" + fieldId, "1", zl_sel_json, ""));
		}
		fieldgroups.add(DgdJson.FieldGroupFactory.group("importobjectgroup", Tr.t("textObjectDataImport", myLang)).addAll(fields));

		dgd.addTab(tab.addAll(fieldgroups));
		dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButonCancel", myLang), "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
		dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButtonContinue", myLang), "primary", "arrow-right-line", "right", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ", "edit")));

		org.json.simple.JSONObject json = DgdJson.toJsonObject(dgd);
		return json.toString();
	}

	/**
	 * Creates a Dynamic GUI Definition (DGD) for the rent roll import error screen.
	 *
	 * The DGD shows:
	 * - An error table built from the rent roll validation results (respecting the ignore list)
	 * - The parsed rent roll line items (zinszeilen) as a JSON data table
	 * - The parsed parking/space line items (stellplaetze) as a JSON data table
	 * - Optionally a preselected import object selector if an object OID is available
	 *
	 * The screen provides buttons to cancel or to continue the import despite errors.
	 *
	 * @param zl
	 *            The rent roll object containing validation errors and parsed line data.
	 * @param ignoreerrors
	 *            Newline-separated list of error identifiers to ignore when building the error table.
	 * @param rutablename
	 *            Legacy table name for rent roll lines; kept for compatibility and passed through.
	 * @param pstablename
	 *            Legacy table name for parking/space lines; kept for compatibility and passed through.
	 *
	 * @return
	 *         A JSON string representing the DGD configuration for the error screen.
	 */

	public String createRentRollImportErrorDgd(Zinsliste zl, String ignoreerrors, String rutablename, String pstablename)
	{
		String myLang = session.getString("language");
		String myId = getId();
		boolean isEnglish = StringUtils.equalsIgnoreCase(myLang, "EN");

		String title = Tr.t("textRentRollImport", myLang);
		DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
		DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
		List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();

		String jsonERR = zl.getErrorsAsJsonDataTable(ignoreerrors, session);
		if(StringUtils.isNotBlank(jsonERR))
		{
			String fieldId = "resulterrjson";
			set("var." + fieldId, jsonERR);
			set("var." + fieldId + ".VGUITYPE", "jsondatatable");
			String displayname = isEnglish ? getString("var." + fieldId + ".DISPLAYNAMEEN") : getString("var." + fieldId + ".DISPLAYNAME");
			DgdJson.Field dt = DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonERR, "");
			fieldgroups.add(DgdJson.FieldGroupFactory.group("errorgroup", Tr.t("textRentRollError", myLang)).add(dt));
		}

		List<DgdJson.Field> fields = new ArrayList<>();

		if(oid_haus != null)
		{
			String url = "http://localhost:8080/icrsdemo/NA?OID=DIRECT_gui.ComboSlotSelector&MARKUPLANGUAGE=JSON&VIEW=VUE&replacetextalternativesfromselector=1&targetfield=" + myId + "__SLOT_mset_importobject&templatetype=CIMS.haus&selected=" + oid_haus;
			fields.add(DgdJson.FieldFactory.autoList("importobject", "Objekt", myId + "__SLOT_mset_importobject", List.of(oid_haus), url, ""));
		}

		String jsonZZ = zl.getZinszeilenAsJsonDataTable(session, rutablename);
		if(StringUtils.isNotBlank(jsonZZ))
		{
			String fieldId = "resultzzjson";
			set("var." + fieldId, jsonZZ);
			set("var." + fieldId + ".VGUITYPE", "jsondatatable");
			String displayname = isEnglish ? getString("var." + fieldId + ".DISPLAYNAMEEN") : getString("var." + fieldId + ".DISPLAYNAME");
			fields.add(DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonZZ, ""));
		}
		String jsonSP = zl.getStellplaetzeAsJsonDataTable(session, pstablename);
		if(StringUtils.isNotBlank(jsonSP))
		{
			String fieldId = "resultspjson";

			set("var." + fieldId, jsonSP);
			set("var." + fieldId + ".VGUITYPE", "jsondatatable");
			String displayname = isEnglish ? getString("var." + fieldId + ".DISPLAYNAMEEN") : getString("var." + fieldId + ".DISPLAYNAME");
			fields.add(DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonSP, ""));
		}
		fieldgroups.add(DgdJson.FieldGroupFactory.group("zzgroup", zl.haus).addAll(fields));

		dgd.addTab(tab.addAll(fieldgroups));

		dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButonCancel", myLang), "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
		String additionalParams = "VIEW=READ&" + myId + "__zinslistenindex=&" + myId + "__eigentuemerlistenindex=&" + myId + "__fehlerabfrage=0&" + myId + "__createhaus=&" + myId + "__createnewtops=&" + myId + "__ignoreerrors=&" + myId + "__topoanpassung=1&" + myId + "__wertaenderung=";
		dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textReimportRentRoll", myLang), "outline", "reset-right-fill", "left", "right", DgdJson.ButtonFactory.action("submit", additionalParams, "edit")));
		dgd.addButton(DgdJson.ButtonFactory.button(Tr.t("textButtonContinueAnyway", myLang), "primary", "arrow-right-line", "right", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ&" + myId + "__fehlerabfrage=1", "edit")));
		setDirty();
		org.json.simple.JSONObject json = DgdJson.toJsonObject(dgd);
		return json.toString();
	}

	/**
	 * Creates a Dynamic GUI Definition (DGD) for starting a new rent roll import
	 * process.
	 *
	 * The DGD contains an import tab and conditionally includes an error data table
	 * if validation errors exist for the provided rent roll. Errors are shown using
	 * a JSON-based data table to replace the legacy HTML output.
	 *
	 * The screen provides navigation controls to either cancel the import or
	 * continue to the next step of the workflow.
	 *
	 * @param zl
	 *            The rent roll object containing the import data and validation results.
	 * @param ignoreerrors
	 *            Newline-separated list of error identifiers that should be ignored when
	 *            building the error data table.
	 *
	 * @return
	 *         A JSON string representing the DGD configuration for the rent roll import.
	 */
	public String createRentRollNewObjectDgd(Zinsliste zl, String ignoreerrors)
	{
		boolean isEnglish = StringUtils.equalsIgnoreCase(session.getString("language"), "EN");
		String myId = getId();

		String title = Tr.t("textRentRollImport", session.getString("language"));
		DgdJson.Dgd dgd = DgdJson.DgdFactory.dgd(myId, title, "");
		DgdJson.FieldTab tab = DgdJson.TabFactory.fieldTab("import", title);
		List<DgdJson.FieldGroup> fieldgroups = new ArrayList<>();
		List<DgdJson.Field> fields = new ArrayList<>();

		String jsonERR = zl.getErrorsAsJsonDataTable(ignoreerrors, session);
		if(StringUtils.isNotBlank(jsonERR))
		{
			String fieldId = "resulterrjson";
			set("var." + fieldId, jsonERR);
			String displayname = isEnglish ? getString("var." + fieldId + ".DISPLAYNAMEEN") : getString("var." + fieldId + ".DISPLAYNAME");
			DgdJson.Field dt = DgdJson.FieldFactory.jsonDataTable(fieldId, displayname, myId + "__" + fieldId, jsonERR, "");
			String fieldgroupDisplayname = isEnglish ? "There's something wrong with the file. Please check it." : "Probleme beim Import!";
			fieldgroups.add(DgdJson.FieldGroupFactory.group("errorgroup", fieldgroupDisplayname).add(dt));
		}

		String displayname = isEnglish ? "Import object data" : "Objektdaten einspielen";
		fieldgroups.add(DgdJson.FieldGroupFactory.group("importobjectgroup", displayname).addAll(fields));

		dgd.addTab(tab.addAll(fieldgroups));

		dgd.addButton(DgdJson.ButtonFactory.button(isEnglish ? "Cancel" : "Abbrechen", "ghost", "arrow-left-line", "left", "left", DgdJson.ButtonFactory.action("back", "", "edit")));
		dgd.addButton(DgdJson.ButtonFactory.button(isEnglish ? "Continue" : "Weiter", "primary", "arrow-right-line", "left", "right", DgdJson.ButtonFactory.action("submit", "VIEW=READ", "edit")));

		org.json.simple.JSONObject json = DgdJson.toJsonObject(dgd);
		return json.toString();
	}

	/**
	 * falls Indexierungsdatum in der Vergangenheit liegt, vergleich zur Mietvertragsbeginn, leer setzen,
	 * falls in der Zukunft liegt, wird die indexierungsdatum aus der letzten zinszeilen history genommen und gesetzt,
	 * diese soll auch groesser als der mietvertragsbeginn sein
	 * falls leerstehung, leer setzen
	 * 
	 * @param ht
	 * @param zl
	 * @param dgd
	 */
	private void modifyLetzteIndexierung(Hashtable ht, Zinsliste zl, DynGenDataObj dgd)
	{
		try
		{
			String mieter = (String)ht.get("mieter");
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

			if(mieter != null && !checkLeerstandString(mieter))
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
							if(!flavour.equals("icrskag"))
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
						if(!flavour.equals("icrskag"))
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