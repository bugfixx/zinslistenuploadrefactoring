package Magic.IMS.ZLImport;

import java.util.Date;

import net.metamagix.essence.Agents.Connector;
import net.metamagix.essence.Bugs.BugMe;
import net.metamagix.essence.TePar.DynGenDataObj;
import net.metamagix.essence.tools.Translation.Tr;

import Magic.IMS.ErrorInfo;
import Magic.IMS.TopoTool;
import Magic.IMS.Zinsliste;

/**
 * Service class for validation operations in the Zinslisten import process.
 * 
 * Extracted from UploadXLS5.java for better separation of concerns.
 */
public class ZinslistenValidationService
{
	private final DynGenDataObj session;
	private final BugMe debug;
	private net.metamagix.essence.Agents.DataAgent DAInst;

	public ZinslistenValidationService(DynGenDataObj session, BugMe debug, net.metamagix.essence.Agents.DataAgent DAInst)
	{
		this.session = session;
		this.debug = debug;
		this.DAInst = DAInst;
	}

	/**
	 * Gets the ignore errors for a specific Haus.
	 *
	 * @param hausid the haus ID
	 * @return the ignore errors string
	 */
	public String getIgnoreErrorsForHaus(String hausid)
	{
		if(DAInst == null)
		{
			Connector conn = null;
			conn = new Connector();
			DAInst = conn.getDataAgent();
		}
		try
		{
			DynGenDataObj hdgd = (DynGenDataObj)DAInst.getObject(hausid, "");
			String ignoreerrors = (String)hdgd.get("var.ignoreerrors");
			if(null == ignoreerrors)
			{
				ignoreerrors = "";
			}
			return ignoreerrors;
		}
		catch(Exception x)
		{
			debug.log(x);
			return "";
		}
	}

	/**
	 * Checks the status of a Haus and validates it for import.
	 * 20110125 RK: klärt ob das Haus bereits verkauft wurde oder Verkaufsdatum vor Importdatum liegt hierher gehören ähnliche Prüfungen am Haus<br>
	 * 20130919 DN: dies methode muss immer <b>VOR zl.ignoreErrors(ignoreerrors)</b> aufgerufen werden, damit die fehler aus der Hauspruefung bei ignoreerros beruecksichtigt werden!
	 *
	 * @param hausid the haus ID
	 * @param zlnew the Zinsliste
	 * @param zlTypeConfig the ZL type config
	 * @param importhausstatusinaktiv flag for importing inactive house status
	 * @param rentrollimportaftersale flag for rent roll import after sale
	 * @param flavour the flavour
	 * @return the validated Zinsliste
	 */
	public Zinsliste checkHausStatus(String hausid, Zinsliste zlnew, ZLTypeConfig zlTypeConfig, boolean importhausstatusinaktiv, boolean rentrollimportaftersale, String flavour)
	{
		DynGenDataObj hausObj = null;
		String status = null;
		if(null != hausid && hausid.length() > 0)
		{
			try
			{
				hausObj = (DynGenDataObj)DAInst.getObject(hausid, "");
				status = (String)hausObj.get("var.status");
				Date dvk = hausObj.getDate("var.verkaufsdatum");
				// Status "-9" = inaktiv
				if(null != dvk && !importhausstatusinaktiv)
				{
					Date dzl = zlnew.getZinslistenDatum();
					if(null != dzl)
					{
						if(dzl.after(dvk))
						{
							// Zinsliste ist jünger als Verkaufsdatum

							if(rentrollimportaftersale)
							{
								zlnew.addError(Tr.t("textRentRollYounger", session.getString("language")), dzl.toString() + " > " + dvk.toString(), ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, "");
							}
							else
							{
								zlnew.addError(Tr.t("textRentRollYounger", session.getString("language")), dzl.toString() + " > " + dvk.toString(), ErrorInfo.SCHWER, ErrorInfo.FORMATFEHLER, "");
							}
						}
					}
				}
				Date lastImport = TopoTool.getLastZinszeileDateforHaus(hausid);
				zlnew.setLastImportForThisHaus(lastImport);
			}
			catch(Exception xc)
			{
				debug.log(xc);
			}
		}
		if(null == status)
		{
			status = "";
		}

		if(status.equals("-1"))
		{
			// das Haus ist NICHT im Besitz ...
			zlnew.addError(Tr.t("textObjectSold", session.getString("language")), "Status-verkauft", ErrorInfo.SCHWER, ErrorInfo.EINTRAGSFEHLER, "");
		}

		// Check Assetmanager, Gesellschaft, Geschäftsfeld
		String hv = zlnew.getTyp();
		if(!status.equals("9") && !hv.equals("egiriskmieter"))
		{
			if(0 == hausObj.getSlotsize("slot.assetmanager"))
			{
				if(zlTypeConfig != null && !zlTypeConfig.isIgnorekeinassetmanager())
				{
					zlnew.addError(Tr.t("textObjectNoAM", session.getString("language")), "", ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, "");
				}
			}
			if(0 == hausObj.getSlotsize("slot.gfeld") && !flavour.equals("icrskag")) // 9545
			{
				if(zlTypeConfig != null && !zlTypeConfig.isIgnorekeingfeld())
				{
					zlnew.addError(Tr.t("textObjectNoAoB", session.getString("language")), "", ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, "");
				}
			}
		}
		if(0 == hausObj.getSlotsize("slot.gschaft"))
		{
			if(zlTypeConfig != null && !zlTypeConfig.isIgnorekeingschaft())
			{
				zlnew.addError(Tr.t("textObjectNoCompany", session.getString("language")), "", ErrorInfo.LEICHT, ErrorInfo.EINTRAGSFEHLER, "");
			}
		}

		return zlnew;
	}

	/**
	 * Writes ignore errors for a Haus.
	 *
	 * @param hausid the haus ID
	 * @param errs the errors string
	 * @return the updated errors string
	 */
	public String writeIgnoreErrorsForHaus(String hausid, String errs)
	{
		if(DAInst == null)
		{
			Connector conn = null;
			conn = new Connector();
			DAInst = conn.getDataAgent();
		}
		try
		{
			DynGenDataObj hdgd = (DynGenDataObj)DAInst.getObject(hausid, "");
			String olderrs = (String)hdgd.get("var.ignoreerrors");
			if(null == olderrs)
			{
				olderrs = "";
			}
			if(olderrs.length() > 0)
			{
				errs = new String(olderrs + "\n" + errs);
			}
			hdgd.set("var.ignoreerrors", errs);
			DAInst.storeObject(hdgd, "CIMS.haus", hausid, session);
		}
		catch(Exception x)
		{
			debug.error(x);
			return errs;
		}
		return errs;
	}

	/**
	 * Checks if a string represents a Leerstand (vacancy).
	 *
	 * @param actualmieter the mieter string to check
	 * @return true if it represents Leerstand
	 */
	public boolean checkLeerstandString(String actualmieter)
	{
		if(actualmieter.equalsIgnoreCase("leerstehung") || actualmieter.equalsIgnoreCase("vacant") || actualmieter.equalsIgnoreCase("vacancy") || actualmieter.equalsIgnoreCase("leer") || actualmieter.equalsIgnoreCase("(leer)") || actualmieter.contains("leerstand") || actualmieter.equalsIgnoreCase("not rented surface"))
		{
			return true;
		}

		return false;
	}
}
