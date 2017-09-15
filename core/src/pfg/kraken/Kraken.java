/*
 * Copyright (C) 2013-2017 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.kraken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import pfg.config.Config;
import pfg.config.ConfigInfo;
import pfg.graphic.Vec2RO;
import pfg.graphic.WindowFrame;
import pfg.log.Log;
import pfg.graphic.PrintBuffer;
import pfg.graphic.ConfigInfoGraphic;
import pfg.graphic.DebugTool;
import pfg.injector.Injector;
import pfg.injector.InjectorException;
import pfg.kraken.astar.TentacularAStar;
import pfg.kraken.astar.tentacles.Tentacle;
import pfg.kraken.astar.tentacles.TentacleManager;
import pfg.kraken.astar.tentacles.types.*;
import pfg.kraken.obstacles.Obstacle;
import pfg.kraken.obstacles.container.DynamicObstacles;
import pfg.kraken.obstacles.container.EmptyDynamicObstacles;
import pfg.kraken.obstacles.container.StaticObstacles;
import pfg.kraken.utils.XY;

/**
 * The manager of the tentacular pathfinder.
 * @author pf
 *
 */
public class Kraken
{
	private Log log;
	private Config config;
	private Injector injector;
	private List<TentacleType> tentacleTypesUsed;
	private boolean overrideConfigPossible = false;
	private XY bottomLeftCorner, topRightCorner;
	private List<Obstacle> fixedObstacles;
	private DynamicObstacles dynObs;

	private static Kraken instance;

	/**
	 * Call this function if you want to create a new Kraken.
	 * The graphic interface is stopped.
	 */
	public synchronized void destructor()
	{	
		if(instance != null)
		{
			overrideConfigPossible = false;
			PrintBuffer buffer = injector.getExistingService(PrintBuffer.class);
			// On appelle le destructeur du PrintBuffer
			if(buffer != null)
				buffer.destructor();
	
			// fermeture du log
			log.close();
			instance = null;
		}
	}
	
	/**
	 * Get Kraken with permanent obstacles. Note that Kraken won't be able to deal with dynamic obstacles.
	 * @param fixedObstacles : a list of fixed/permanent obstacles
	 * @return the instance of Kraken
	 */
	public static Kraken getKraken(List<Obstacle> fixedObstacles, XY bottomLeftCorner, XY topRightCorner)
	{
		if(instance == null)
			instance = new Kraken(fixedObstacles, new EmptyDynamicObstacles(), null, bottomLeftCorner, topRightCorner);
		return instance;
	}
	
	/**
	 * Get Kraken with :
	 * @param fixedObstacles : a list of fixed/permanent obstacles
	 * @param dynObs : a dynamic/temporary obstacles manager that implements the DynamicObstacles interface
	 * @param tentacleTypes : 
	 * @return
	 */
	protected static Kraken getKraken(List<Obstacle> fixedObstacles, DynamicObstacles dynObs, TentacleType tentacleTypes, XY bottomLeftCorner, XY topRightCorner)
	{
		// TODO : pas encore disponible
		if(instance == null)
			instance = new Kraken(fixedObstacles, dynObs, tentacleTypes, bottomLeftCorner, topRightCorner);
		return instance;
	}
	
	/**
	 * Instancie le gestionnaire de dépendances et quelques services critiques
	 * (log et config qui sont interdépendants)
	 */
	private Kraken(List<Obstacle> fixedObstacles, DynamicObstacles dynObs, TentacleType tentacleTypes, XY bottomLeftCorner, XY topRightCorner)
	{	
		assert instance == null;
		overrideConfigPossible = true;
		this.bottomLeftCorner = bottomLeftCorner;
		this.topRightCorner = topRightCorner;
		this.dynObs = dynObs;
		this.fixedObstacles = fixedObstacles;
		
		tentacleTypesUsed = new ArrayList<TentacleType>();
		if(tentacleTypes == null)
		{
			for(BezierTentacle t : BezierTentacle.values())
				tentacleTypesUsed.add(t);
			for(ClothoTentacle t : ClothoTentacle.values())
				tentacleTypesUsed.add(t);
			for(TurnoverTentacle t : TurnoverTentacle.values())
				tentacleTypesUsed.add(t);
			for(StraightingTentacle t : StraightingTentacle.values())
				tentacleTypesUsed.add(t);
		}
		
		injector = new Injector();
		config = new Config(ConfigInfoKraken.values(), "kraken.conf", false);
	}

	/**
	 * Returns the WindowFrame. If the graphic display is disabled, returns null.
	 * @return
	 */
	public WindowFrame getWindowFrame()
	{
		return injector.getExistingService(WindowFrame.class);
	}
	
	/**
	 * Return the tentacular pathfinder
	 * @return
	 */
	public TentacularAStar getAStar()
	{
		try {
			TentacularAStar aStar = injector.getExistingService(TentacularAStar.class);
			if(aStar == null)
			{
				overrideConfigPossible = false;

				
				HashMap<ConfigInfo, Object> overrideGraphic = new HashMap<ConfigInfo, Object>();
				overrideGraphic.put(ConfigInfoGraphic.SIZE_X_WITH_UNITARY_ZOOM, (int) (topRightCorner.getX() - bottomLeftCorner.getX()));
				overrideGraphic.put(ConfigInfoGraphic.SIZE_Y_WITH_UNITARY_ZOOM, (int) (topRightCorner.getY() - bottomLeftCorner.getY()));
				
				DebugTool debug = new DebugTool("graphic-kraken.conf", overrideGraphic, SeverityCategoryKraken.INFO);
				log = debug.getLog();

				injector.addService(Log.class, log);
				injector.addService(Config.class, config);
				injector.addService(DynamicObstacles.class, dynObs);
		
				injector.addService(Kraken.class, this);
		
				if(config.getBoolean(ConfigInfoKraken.GRAPHIC_ENABLE))
				{
					WindowFrame f = debug.getWindowFrame(new Vec2RO((topRightCorner.getX() + bottomLeftCorner.getX()) / 2, (topRightCorner.getY() + bottomLeftCorner.getY()) / 2));
					injector.addService(WindowFrame.class, f);
//					if(config.getBoolean(ConfigInfoKraken.GRAPHIC_EXTERNAL))
//						injector.addService(PrintBufferInterface.class, f.getBu);
//					else
						injector.addService(PrintBuffer.class, f.getPrintBuffer());
				}
				else
				{
					HashMap<ConfigInfo, Object> override = new HashMap<ConfigInfo, Object>();
					List<ConfigInfo> graphicConf = ConfigInfoKraken.getGraphicConfigInfo();
					for(ConfigInfo c : graphicConf)
						override.put(c, false);
					config.override(override);
					injector.addService(PrintBuffer.class, null);
				}
		
				Obstacle.set(log, injector.getService(PrintBuffer.class));
				Obstacle.useConfig(config);
				Tentacle.useConfig(config);
				if(fixedObstacles != null)
					injector.getService(StaticObstacles.class).addAll(fixedObstacles);
				injector.getService(TentacleManager.class).setTentacle(tentacleTypesUsed);	
			}
			return injector.getService(TentacularAStar.class);
		} catch (InjectorException e) {
			throw new RuntimeException("Fatal error : "+e);
		}
	}
	
	public void overrideConfig(ConfigInfoKraken key, Object newValue)
	{
		if(overrideConfigPossible)
			config.override(key, newValue);
	}

	public void overrideConfig(HashMap<ConfigInfo, Object> override)
	{
		if(overrideConfigPossible)
			config.override(override);
	}

	/**
	 * Used by the unit tests
	 * @return
	 */
	protected Injector getInjector()
	{
		return injector;
	}

	public PrintBuffer getPrintBuffer()
	{
		return injector.getExistingService(PrintBuffer.class);
	}
}
