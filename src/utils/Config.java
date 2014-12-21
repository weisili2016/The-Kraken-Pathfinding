package utils;

import container.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import enums.ConfigInfo;
import enums.RobotColor;


/**
 * Gère le fichier de configuration externe.
 * @author pf, marsu
 *
 */
public class Config implements Service
{
	// Permet de savoir si le match a démarré et quand
	private static long dateDebutMatch = 0;	
	public static boolean matchDemarre = false;	

	private String name_local_file = "local.ini";
	private String name_config_file = "config.ini";
	private String path;
	private Properties config = new Properties();
	private Properties local = new Properties();
	
    Enumeration<?> e = local.propertyNames();
	
	public Config(String path) throws Exception
	{
		this.path = path;
	//	log.debug("Loading config from current directory : " +  System.getProperty("user.dir"), this)
		try
		{
			this.config.load(new FileInputStream(this.path+this.name_config_file));
		}
		catch  (IOException e)
		{
			e.printStackTrace();
			throw new Exception("Erreur ouverture de config.ini");
		}
		
		try
		{
			this.config.load(new FileInputStream(this.path+this.name_local_file));
		}
		catch  (IOException e)
		{
			try
			{
				FileOutputStream fileOut = new FileOutputStream(this.path+this.name_local_file);
				this.local.store(fileOut, "Ce fichier est un fichier généré par le programme.\nVous pouvez redéfinir les variables de config.ini dans ce fichier dans un mode de votre choix.\nPS : SopalINT RULEZ !!!\n");
			}
			catch (IOException e2)
			{
				e2.printStackTrace();
				throw new Exception("Erreur création de local.ini");
			}	
			throw new Exception("Erreur ouverture de local.ini");
		}	
		affiche_tout();
	}
	
	/**
	 * Récupère un entier de la config
	 * @param nom
	 * @return
	 */
	public int getInt(ConfigInfo nom)
	{
		return Integer.parseInt(getString(nom));
	}
	
	/**
	 * Récupère un booléen de la config
	 * @param nom
	 * @return
	 */
	public boolean getBoolean(ConfigInfo nom)
	{
		return Boolean.parseBoolean(getString(nom));
	}
	
	/**
	 * Récupère un double de la config
	 * @param nom
	 * @return
	 */	
	public double getDouble(ConfigInfo nom)
	{
		return Double.parseDouble(getString(nom));
	}
	
	/**
	 * Méthode de récupération des paramètres de configuration
	 * @param nom
	 * @return
	 * @throws ConfigException
	 */
	public String getString(ConfigInfo nom)
	{
		String out = null;
		out = config.getProperty(nom.toString());
		if(out == null)
		{
			System.out.println("Erreur config: "+nom+" introuvable.");
		}
		return out;
	}

	/**
	 * Méthode utilisée seulement par les tests
	 * @param nom
	 * @return
	 */
	private void set(ConfigInfo nom, String value)
	{
		System.out.println(nom+" = "+value+" (ancienne valeur: "+config.getProperty(nom.toString())+")");
		config.setProperty(nom.toString(), value);
	}
	
	/**
	 * Set en version user-friendly
	 * @param nom
	 * @param value
	 */
	public void set(ConfigInfo nom, Object value)
	{
		set(nom, value.toString());
	}

	private void affiche_tout()
	{
		if(Boolean.parseBoolean(config.getProperty("affiche_debug")))
		{
			System.out.println("Configuration initiale");
			for(Object o: config.keySet())
				System.out.println(o+": "+config.get(o));
		}
	}
	
	public void updateConfig()
	{
	}
	
	public void setDateDebutMatch()
	{
		dateDebutMatch = System.currentTimeMillis();
	}
	
	public static long getDateDebutMatch()
	{
		// Si le match n'a pas encore commencé, on dit qu'il vient de commencer (sinon les calculs bug)
		if(dateDebutMatch == 0)
			return System.currentTimeMillis();
		return dateDebutMatch;
	}

	/**
	 * Récupère la couleur du robot
	 * @return
	 */
	public RobotColor getColor()
	{
		return RobotColor.parse(getString(ConfigInfo.COULEUR));
	}
	
}
