package Magic.IMS.ZLImport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.text.SimpleDateFormat;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import Magic.IMS.UploadXLS5;
import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Agents.FileDataAgent;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.GenData.ByteArrayDataSource;
import net.metamagix.essence.MConfig.CfgSingleton;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.tools.CoolTemplateTool;
import net.metamagix.essence.tools.CoolWebTool;
import net.metamagix.essence.tools.Liquid.LiquidParserMailWrapper;
import net.metamagix.essence.tools.Translation.Tr;

/**
 * Service class for mail operations in the Zinslisten import process.
 * 
 * Handles all email notifications during and after the Zinslisten import,
 * including error reports, change notifications, and assetmanager communications.
 * 
 * Extracted from UploadXLS5.java for better separation of concerns.
 */
public class ZinslistenMailService
{
	private final UploadXLS5 parent;
	private final DynGenDataObj session;
	private final BugMe debug;
	private final FileDataAgent fileDataAgent;
	private final net.metamagix.essence.Agents.PageBuilder pageBuilder;
	private final String bcc_emails;
	private final String mailtoamcfg;

	/**
	 * Constructor for ZinslistenMailService.
	 * 
	 * @param parent the parent UploadXLS5 instance for accessing get/set/getString/getBoolean methods
	 * @param session the user session
	 * @param debug the debug logger
	 * @param fileDataAgent the File Data Agent instance
	 * @param pageBuilder the Page Builder instance
	 * @param bcc_emails BCC email addresses
	 * @param mailtoamcfg mail to asset manager configuration
	 */
	public ZinslistenMailService(UploadXLS5 parent, DynGenDataObj session, BugMe debug,
	                              FileDataAgent fileDataAgent,
	                              net.metamagix.essence.Agents.PageBuilder pageBuilder,
	                              String bcc_emails, String mailtoamcfg)
	{
		this.parent = parent;
		this.session = session;
		this.debug = debug;
		this.fileDataAgent = fileDataAgent;
		this.pageBuilder = pageBuilder;
		this.bcc_emails = bcc_emails;
		this.mailtoamcfg = mailtoamcfg;
	}

	/**
	 * Send mail with errors.
	 *
	 * @return error message string
	 */
	public String sendMailWithErrors()
	{
		boolean isError = false;
		StringBuffer error = new StringBuffer();

		String view = (String)session.get("VIEW");
		String to = (String)parent.get("var.email");
		to = to + bcc_emails;
		if(null == to)
		{
			session.append("ERROR.MAIN", Tr.t("textErrorValidEmail", session.getString("language")));
			int p = view.indexOf("SEND");
			view = view.substring(0, p) + "MAIL";
			session.set("CURRENT_VIEW", view);
			return null;
		}

		String cc = (String)parent.get("var.ccemail");
		if(null == cc)
		{
			cc = "";
		}
		if(1 > to.indexOf("@"))
		{
			session.append("ERROR.MAIN", Tr.t("textErrorValidEmail", session.getString("language")));
			int p = view.indexOf("SEND");
			view = view.substring(0, p) + "MAIL";
			session.set("CURRENT_VIEW", view);
			return null;
		}

		boolean mailerror = false;

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
			mailerror = true;
			smtp = "localhost";
			debug.error(this, "no smtp host (SMTP_HOST) defined in essence.cfg, using localhost ");
		}
		String from = (String)parent.get("var.from");
		if(null == from || from.equals(""))
		{
			from = (String)session.get("site.fromemail");
		}
		if(null == from || from.equals(""))
		{
			from = (String)CfgSingleton.getInstance().get("fromemail");
		}
		// DEPRECIATED
		if(null == from || from.equals(""))
		{
			from = (String)CfgSingleton.getInstance().get("MAILADDRESS");
		}
		// DEPRECIATED
		if(null == from || from.equals(""))
		{
			from = (String)CfgSingleton.getInstance().get("FROM_MAILADDRESS");
		}
		if(null == from || from.equals(""))
		{
			from = "root@localhost";
			error.append(Tr.t("textErrorNoFrom1", session.getString("language")) + Tr.t("textErrorNoFrom2", session.getString("language")) + " ");
		}

		ArrayList<String> to_addresses = new ArrayList<>();
		ArrayList<String> cc_addresses = new ArrayList<>();

		String subject = (String)parent.get("var.subject");
		if(null == subject)
		{
			subject = (String)parent.get("var.name");
		}

		String text = parent.getString("var.mailtext").trim();
		if(text.length() == 0)
		{
			text = parent.getString("var.text");
		}

		String errtext = parent.getString("var.errorcode").trim();

		StringBuffer mtext = new StringBuffer();

		if(!to.equals(""))
		{
			if(to.contains(","))
			{
				String[] to_addresses_arr = to.split(",");
				for(int i = 0; i < to_addresses_arr.length; i++)
				{
					String mail_to = to_addresses_arr[i];
					to_addresses.add(mail_to);
				}

			}
			else
			{
				to_addresses.add(to);
			}
		}
		if(!cc.equals(""))
		{
			cc_addresses.add(cc);
		}

		ArrayList<MimeBodyPart> parts = new ArrayList<>();

		try
		{
			// Zinsliste
			String txt = (String)parent.get("var.name");
			String fid = (String)parent.get("var.file");
			if(null != fid)
			{
				// ATTACHMENT
				// System.err.println("ZLU2: ATTACH:"+fid);
				MimeBodyPart part = getFilePart(fid);
				if(null != part)
				{
					parts.add(part);
					String fn = part.getFileName();
					// if(null != fn)
					// {
					// mtext.append(fn + "\n");
					// }
				}
			}
			// else
			// {
			// if(null != txt)
			// {
			// mtext.append(txt + "\n");
			// }
			// }
			String csvErrorString = parent.getString("var.errorcodecsv");
			if(csvErrorString.length() > 0)
			{
				// attach Error CSV

				MimeBodyPart mbp = new MimeBodyPart();

				try
				{
					DataSource source = new net.metamagix.essence.GenData.StringBufferDataSource(new StringBuffer(csvErrorString));
					mbp.setDataHandler(new DataHandler(source));
					mbp.setFileName("error.csv");
					mbp.setHeader("Content-Type", "text/csv;charset=utf-8");
					parts.add(mbp);
				}
				catch(Exception e)
				{
					debug.error(this, "cannot set datahandler in attachment " + e.getMessage());
				}

			}

			// Eigentuemerliste
			fid = (String)parent.get("var.efile");
			if(null != fid)
			{
				if(fid.length() > 0)
				{
					// ATTACHMENT 2
					MimeBodyPart part = getFilePart(fid);
					if(null != part)
					{
						parts.add(part);
						String fn = part.getFileName();
						if(null != fn)
						{
							mtext.append(fn + "\n");
						}
					}
				}
			}
			else
			{
				if(null != txt)
				{
					mtext.append(txt + "\n");
				}
			}

			mtext.append(text + "\n\n");

		}
		catch(Exception x)
		{
			debug.error(this, "Loading of element failed : " + x.getMessage());
		}
		if(!mailerror)
		{

			LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0027", subject, new String(mtext), "standardemail.html", session, parent, null);

			if(parent.getBoolean("var.mailattachment", Boolean.TRUE))
			{
				try
				{
					if(parts != null && parts.size() > 0)
					{
						for(int i = 0; i < parts.size(); i++)
						{
							lpmw.addMimeBodyPart(parts.get(i));
						}
					}
				}
				catch(Exception e)
				{
					debug.error(this, "Cannot set file as attachment " + e.getMessage());
				}
			}
			String res = lpmw.sendMailTo(new ArrayList<>(to_addresses), new ArrayList<>(cc_addresses), new ArrayList<>());

			if(!res.equals(""))
			{
				debug.error(this, "mailer had error" + res);
				session.append("ERROR.MAIN", Tr.t("textMailProblem", session.getString("language")) + " " + res);
				return new String(Tr.t("textMailProblem", session.getString("language")) + " " + res);
			}
			else
			{
				debug.error(this, "mailer sent it");
			}
		}

		return new String(error);

	}

	/**
	 * Send mail with changes.
	 */
	public void sendMailWithChanges()
	{
		// Get zlprotocol from parent
		Magic.IMS.ZLImport.ZLImportProtocol zlprotocol = parent.getZlprotocol();
		HashMap<String, String> mailinglist = parent.getMailinglist();
		String shortinfo = parent.getShortinfo();
		
		// keine Eintraege -> keine Mail mit Änderungen
		if(zlprotocol.isCSVEmpty())
		{
			return;
		}

		StringBuffer error = new StringBuffer();

		String useremail = session.getString("var.useremail");

		if(useremail.indexOf("@") < 0)
		{
			return;
		}

		if(session.getBoolean("var.emailjanein", Boolean.TRUE))
		{

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

			String from = (String)parent.get("var.from");
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

			ArrayList<String> to_addresses = new ArrayList<>();

			// BCC hier dazugegeben
			if(bcc_emails.length() > 0)
			{
				if(bcc_emails.contains(","))
				{
					String[] tmp = bcc_emails.split(",");

					for(int i = 0; i < tmp.length; i++)
					{
						to_addresses.add(tmp[i].trim());
					}
				}
				else
				{
					to_addresses.add(bcc_emails);
				}
			}

			// ArrayList<String> cc_addresses = new ArrayList<>();

			if(!useremail.equals(""))
			{
				to_addresses.add(useremail);
			}

			shortinfo = shortinfo.replaceAll("\\r", "").trim();

			String filenameforemail = parent.getString("var.filename");
			String subject = Tr.t("textChangeMailSubject", session.getString("language")) + " " + shortinfo + " - (" + filenameforemail + ")";

			String value = "";
			if(mailinglist != null && mailinglist.size() > 0)
			{

				mailinglist = parent.formatMailContent(mailinglist, Boolean.TRUE);

				for(String mailadress : mailinglist.keySet())
				{
					value = mailinglist.get(mailadress);
					value = CoolTemplateTool.removeEssenceID(value);
					break;
				}
			}
			else
			{
				value = zlprotocol.getMailMsg();
			}

			if(CfgSingleton.getInstance().hasLLMModule())
			{ // make a message summary
				// initialize client
				String lang = session.getString("language").toUpperCase();
				if(!lang.equals("EN")) lang = "DE";
				net.metamagix.essence.tools.CoolLLMWrapperTool cllw = new net.metamagix.essence.tools.CoolLLMWrapperTool();

				String username = "";
				if(null != session)
				{
					username = session.getString("var.username");
				}

				net.metamagix.essence.LLM.Message msgtextSummaryMsg = cllw.getSummary(value, lang, username, 100);
				String msgtextSummary = msgtextSummaryMsg.getContent().toString();

				if(null != msgtextSummary)
				{
					if(lang.equals("DE"))
					{
						value = "AI-Zusammenfassung:\n</br>" + msgtextSummary + "\n\n</br></br>Vollständiger Bericht:\n</br></br>" + value;
					}
					else
					{
						value = "AI Summary:\n</br>" + msgtextSummary + "\n\n</br></br>Full Report:\n</br></br>" + value;
					}
				}
			}

			LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0026", subject, value.toString(), "standardemail.html", session, parent, null);
			if(parent.getBoolean("var.mailattachment", Boolean.TRUE))
			{
				try
				{
					MimeBodyPart mbp = new MimeBodyPart();
					DataSource source = new net.metamagix.essence.GenData.StringBufferDataSource(zlprotocol.getCSVStringBuffer());
					mbp.setDataHandler(new DataHandler(source));
					mbp.setFileName("zinslistenaenderungen.csv");
					mbp.setHeader("Content-Type", "text/csv;charset=utf-8");
					lpmw.addMimeBodyPart(mbp);
				}
				catch(Exception e)
				{
					debug.error(this, "Cannot set file as attachment " + e.getMessage());
					return;
				}
			}
			lpmw.sendTo(to_addresses);

		}
		else

		{
			BugMe.getInstance("maillog").log("Not sending changes email to executor, to " + useremail + ", user turned off email!");
		}
	}

	/**
	 * Sends mails to assetmanagers with infos for their objects.
	 *
	 * @param mailinglist Hashtable
	 * @param subject the subject
	 */
	public void sendMailToAssetmanager(HashMap<String, String> mailinglist, String subject)
	{
		if(mailinglist == null || mailinglist.size() == 0)
		{
			return;
		}

		// PKO - REMOVE - Only testing purpose
		for(String mailadress : mailinglist.keySet())
		{
			System.out.println("AM MAILS AN FOLGENDE ADRESSEN (sendMailToAssetmanager): " + mailadress);
			BugMe.getInstance("maillogfile").log("RRIMPORT> [" + parent.getName() + "] preparing rr mail to " + mailadress);

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

		String from = (String)parent.get("var.from");
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
		}

		// BUILD URL
		String port = (String)cfg.get("STANDARD_PORT");
		if(null == port)
		{
			port = "80";
		}
		String proto = (String)cfg.get("STANDARD_PROTOCOLL");
		if(null == proto)
		{
			proto = "http";
		}
		String domain = proto + "://" + CoolWebTool.getUsedHost(session);
		if(!port.equals("80"))
		{
			domain = domain + ":" + port;
		}

		Date now = new Date();
		GregorianCalendar crecalendar = new GregorianCalendar();
		crecalendar.setTime(now);

		if(subject.length() == 0)
		{
			subject = Tr.t("textAssetmanagerMailSubject", session.getString("language")) + " (" + net.metamagix.essence.eSSENCETypes.DateTime.stringFromDate(crecalendar.getTime()) + ")";
		}

		// Send Assetmanager Mails
		if(mailinglist != null && mailinglist.size() > 0)
		{
			for(String mailadress : mailinglist.keySet())
			{
				ArrayList<String> to_addresses = new ArrayList<>();
				ArrayList<String> cc_addresses = new ArrayList<>();
				ArrayList<String> bcc_addresses = new ArrayList<>();

				String value = mailinglist.get(mailadress);

				if(value == null || value.trim().length() == 0)
				{
					// Kein Mail ohne Inhalt schicken
					continue;
				}
				mailadress = mailadress + bcc_emails;

				if(mailadress.length() > 0)
				{
					if(mailadress.contains(","))
					{
						String[] tmp = mailadress.split(",");

						to_addresses.add(tmp[0].trim());

						for(int i = 1; i < tmp.length; i++)
						{
							cc_addresses.add(tmp[i].trim());
						}
					}
					else
					{
						to_addresses.add(mailadress);
					}
				}

				value = CoolTemplateTool.removeEssenceID(value);

				value = "<html><head><base href=\"" + domain + "\" /></head><body>" + value + "</body></html>";

				// Saves the mail on the Filesystem

				createMessage(from, to_addresses, cc_addresses, bcc_addresses, subject, value, new ArrayList<File>());

				// MailThread mt = new MailThread(to_addresses, cc_addresses, bcc_addresses, subject, value, from, smtp, from, session);
				// // only the header, parts hav to do it themselves
				// mt.setCharacterEncoding("UTF-8");
				// mt.setContentType(MailingModule.HTML_CONTENT);
				// mt.start();

				LiquidParserMailWrapper lpmw = new LiquidParserMailWrapper("M0022", subject, value, "standardemail.html", session, parent, null);
				lpmw.sendTo(to_addresses, cc_addresses, bcc_addresses);
			}
		}

	}

	/**
	 * Creates the message.
	 *
	 * @param from the from
	 * @param to_addresses the to addresses
	 * @param cc_addresses the cc addresses
	 * @param bcc_addresses the bcc addresses
	 * @param subject the subject
	 * @param body the body
	 * @param attachments the attachments
	 */
	public static void createMessage(String from, List<String> to_addresses, List<String> cc_addresses, List<String> bcc_addresses, String subject, String body, List<File> attachments)
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
					message.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc_addresses.get(i).toString()));
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
	 * Get file part for attachment.
	 *
	 * @param OID the OID
	 * @return MimeBodyPart
	 */
	MimeBodyPart getFilePart(String OID)
	{

		OID = new String(OID.substring(5));
		HashMap<String, Object> fparams = new HashMap<>();
		// String filename = new String(CfgSingleton.getInstance().get("filestorage") + File.separator + OID);
		try
		{
			FileDataAgent FDAInst = fileDataAgent;
			if(FDAInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				FDAInst = conn.getFileDataAgent();
			}
			fparams = FDAInst.getParams(OID);
		}
		catch(Exception e)
		{
			debug.error(this, "NA FDAInst:" + e.getMessage());
			return null;
		}
		// access check
		String fileOID = null;
		String fileParamName = null;
		if(null != fparams)
		{
			fileOID = (String)fparams.get("OID");
			fileParamName = (String)fparams.get("paramname");
		}

		// get Object and check read-permission
		if(null != fileOID)
		{
			try
			{
				net.metamagix.essence.Agents.PageBuilder PBInst = pageBuilder;
				if(PBInst == null)
				{
					Connector conn = null;
					conn = new Connector();
					PBInst = conn.getPageBuilder();
				}
				DynGenDataObj fdgd = PBInst.getDynGenDataObject(fileOID);
				if(null != fdgd)
				{
					if(!fdgd.read_access(session, "var." + fileParamName))
					{
						debug.error(this, "file access check..." + fileParamName + " (" + fileOID + ")");
						debug.error(this, "not granted.");
						debug.error(this, "NA no right to read file from " + fileOID + fileParamName);
						return null;
					}
				}
			}
			catch(Exception e)
			{
				String info = new String("fileOID is \"" + fileOID + fileParamName + "\"");
				debug.error(this, "NA default parse:" + info + e.getMessage());
				// System.err.println(e.getMessage());
				debug.log(e);
				return null;
			}
		}

		String fname = null;
		if(null != fparams)
		{
			fname = (String)fparams.get("name");
		}

		MimeBodyPart mbp = new MimeBodyPart();

		try
		{
			FileDataAgent FDAInst = fileDataAgent;
			if(FDAInst == null)
			{
				Connector conn = null;
				conn = new Connector();
				FDAInst = conn.getFileDataAgent();
			}
			byte[] content = FDAInst.getObject(OID);
			DataSource source = new ByteArrayDataSource(content, "application/octet-stream");
			mbp.setDataHandler(new DataHandler(source));
			// mbp.setDataHandler(new DataHandler(fds));
		}
		catch(Exception e)
		{
			debug.error(this, "cannot set datahandler in attachment " + e.getMessage());
			return null;
		}
		try
		{
			if(null != fname)
			{
				mbp.setFileName(fname);
			}
		}
		catch(Exception e)
		{
			debug.error(this, "ICRS.zinslisten.zinslistenupload cannot set filename in attachment " + e.getMessage());
			return null;
		}
		return mbp;
	}
}
