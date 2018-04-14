/*
 * Copyright (C) 2013-2018 Pierre-François Gimenez
 * Distributed under the MIT License.
 */

package pfg.kraken.astar.thread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import pfg.config.Config;
import pfg.kraken.ConfigInfoKraken;
import pfg.kraken.exceptions.NoPathException;
import pfg.kraken.exceptions.PathfindingException;
import pfg.kraken.obstacles.RectangularObstacle;
import pfg.kraken.obstacles.container.DynamicObstacles;
import pfg.kraken.robot.Cinematique;
import pfg.kraken.robot.CinematiqueObs;
import pfg.kraken.robot.ItineraryPoint;
import pfg.log.Log;
import static pfg.kraken.astar.tentacles.Tentacle.PRECISION_TRACE_MM;
/**
 * A path manager that can handle dynamic update
 * @author pf
 *
 */

public class DynamicPath
{
	public enum State
	{
		STANDBY, // aucune recherche en cours
		MODE_WITHOUT_REPLANING, // signale qu'il n'y a pas de replanification à faire
		SEARCH_REQUEST, // on aimerait que le thread commence la recherche initiale
		SEARCHING, // le thread calcule la trajectoire initiale
		UPTODATE_WITH_NEW_PATH, // on a un NOUVEAU chemin vers la destination
		UPTODATE, // pas besoin de replanif
		REPLANNING; // replanification nécessaire / en cours
//		STOP; // la planification doit s'arrêter
	}
	
	protected Log log;
	
	private CinematiqueObs[] path = new CinematiqueObs[1000];
	private volatile State etat = State.STANDBY;
	private volatile int indexFirst; // index du point où est le robot
	private volatile int pathSize; // index du prochain point de la trajectoire
	private volatile int firstDifferentPoint; // index du premier point différent dans la replanification
	private final int margeNecessaire, margeInitiale, margeAvantCollision, margePreferable;
	private volatile PathfindingException e;
	
	public DynamicPath(Log log, Config config, RectangularObstacle vehicleTemplate)
	{
		this.log = log;
		margeNecessaire = (int) Math.ceil(config.getDouble(ConfigInfoKraken.NECESSARY_MARGIN) / PRECISION_TRACE_MM);
		margePreferable = (int) Math.ceil(config.getDouble(ConfigInfoKraken.PREFERRED_MARGIN) / PRECISION_TRACE_MM);
		margeAvantCollision = (int) Math.ceil(config.getInt(ConfigInfoKraken.MARGIN_BEFORE_COLLISION) / PRECISION_TRACE_MM);
		margeInitiale = (int) Math.ceil(config.getInt(ConfigInfoKraken.INITIAL_MARGIN) / PRECISION_TRACE_MM);
		if(margeNecessaire > margePreferable || margeInitiale < margePreferable)
			throw new IllegalArgumentException();
		
		pathSize = 0;
		for(int i = 0; i < path.length; i++)
			path[i] = new CinematiqueObs(vehicleTemplate);
	}
	
	public synchronized void initSearchWithoutPlanning()
	{
		assert pathSize == 0;
		assert etat == State.STANDBY;
		etat = State.MODE_WITHOUT_REPLANING;
	}
	
	public synchronized List<ItineraryPoint> endSearchWithoutPlanning()
	{
		assert etat == State.MODE_WITHOUT_REPLANING;
		List<ItineraryPoint> out = getPath();
		clear();
		return out;
	}
	
	public synchronized void startContinuousSearch()
	{
		assert etat == State.STANDBY;
		assert pathSize == 0;
//		log.write("Search request", LogCategoryKraken.REPLANIF);
		etat = State.SEARCH_REQUEST;
		notifyAll();
	}
	
	public synchronized void endContinuousSearch()
	{
		etat = State.STANDBY;
		notifyAll();
	}
	
	public synchronized void addToEnd(LinkedList<CinematiqueObs> points)
	{
		for(int i = 0; i < points.size(); i++)
			points.get(i).copy(path[pathSize + i]);
		pathSize += points.size();
		notifyAll();
	}

	public synchronized void setUptodate()
	{
//		log.write("A path is available", LogCategoryKraken.REPLANIF);
		etat = State.UPTODATE_WITH_NEW_PATH;
		notifyAll();
	}
	
	public synchronized int margeSupplementaireDemandee()
	{
		/*
		 * Si on a moins de MARGE_PREFERABLE points, on demande à Kraken de compléter jusqu'à avoir MARGE_INITIALE points (c'est un hystérésis)
		 */
		if(etat == State.REPLANNING && pathSize - indexFirst < margePreferable)
			return margeInitiale - (pathSize - indexFirst);
		
		/*
		 * Si on est à jour, pas besoin de regarder le nombre de points restant (car il peut être normalement faible quand on arrive à destination)
		 */
		else
			return 0;
	}

	public synchronized CinematiqueObs setCurrentTrajectoryIndex(int index)
	{
		if(index >= pathSize)
			return path[pathSize-1]; // ça peut potentiellement arrivé à cause de la latence de la communication…
		
		indexFirst = index;
		return path[index];
	}
	
	public void checkException() throws PathfindingException
	{
		if(e != null)
		{
			assert etat == State.STANDBY;
			PathfindingException tmp = e;
			e = null;
			throw tmp;
		}
		if(etat == State.REPLANNING && pathSize - indexFirst < margeNecessaire)
			throw new NoPathException("Pas assez de marge !");
	}
	
	public boolean needToStopReplaning()
	{
		return (etat == State.REPLANNING && pathSize - indexFirst < margeNecessaire);
	}
	
	/**
	 * La recherche est terminée, on retourne en STANDBY
	 */
	public synchronized void clear()
	{
//		log.write("Search ended, returns to STANDBY", LogCategoryKraken.REPLANIF);
		pathSize = 0;
		etat = State.STANDBY;
	}
	
	public List<ItineraryPoint> getPath()
	{
		assert etat == State.UPTODATE_WITH_NEW_PATH || etat == State.MODE_WITHOUT_REPLANING;
		List<ItineraryPoint> pathIP = new ArrayList<ItineraryPoint>();
		for(int i = 0; i < pathSize; i++)
			pathIP.add(new ItineraryPoint(path[i]));
		etat = State.UPTODATE;
		return pathIP;
	}

	public synchronized void updateCollision(DynamicObstacles dynObs)
	{
		// dans tous les cas, on vérifie les collisions afin de vider la liste des nouveaux obstacles
		firstDifferentPoint = dynObs.isThereCollision(path, indexFirst, pathSize);

		if(!needCollisionCheck())
			return;
		
		if(firstDifferentPoint != pathSize)
		{
			// on retire des points corrects mais trop proche de la collision
			firstDifferentPoint -= margeAvantCollision;
			
			// Si la trajectoire restante est plus petite que la marge initiale désirée, il faut s'arrêter
			if(firstDifferentPoint - indexFirst <= margeInitiale)
			{
				endContinuousSearchWithException(new NoPathException("Pas assez de marge"));
				pathSize = indexFirst;
			}
			else
			{
				// Sinon on prévient qu'il faut une replanification et on détruit
				etat = State.REPLANNING;
				pathSize = firstDifferentPoint;
//				assert !needToStopReplaning();
			}
			notifyAll();
		}
	}

	/**
	 * Nouveau départ en replanification (après avoir calculé un bout de chemin partiel)
	 * @return
	 */
	public Cinematique getNewStart()
	{
		assert etat == State.REPLANNING;
		return path[firstDifferentPoint - 1];
	}

	/**
	 * Is a new, complete path available ?
	 * @return
	 * @throws PathfindingException 
	 */
	public synchronized boolean isNewPathAvailable() throws PathfindingException
	{
		checkException();
		return etat == State.UPTODATE_WITH_NEW_PATH;
	}

	public synchronized boolean needReplanning()
	{
		return etat == State.REPLANNING;
	}
	
	public boolean isStarted()
	{
		return etat != State.STANDBY;
	}
	
	public boolean isModeWithReplanning()
	{
		assert etat != State.STANDBY;
		// La replanification est désactivée lors d'une recherche "manuelle"
		return etat != State.MODE_WITHOUT_REPLANING;
	}

	public boolean isInitialSearch()
	{
		return etat == State.SEARCHING;
	}
	
	public boolean isThereSearchRequest()
	{
		return etat == State.SEARCH_REQUEST;
	}

	public void setSearchInProgress()
	{
		assert etat == State.SEARCH_REQUEST;
//		log.write("Search begin", LogCategoryKraken.REPLANIF);
		etat = State.SEARCHING;
	}

	public boolean needCollisionCheck()
	{
		return etat == State.REPLANNING || etat == State.UPTODATE_WITH_NEW_PATH || etat == State.UPTODATE;
	}

/*	public synchronized void stopResearch()
	{
		etat = State.STOP;
		notifyAll();
	}*/
	
	public synchronized List<ItineraryPoint> waitNewPath() throws InterruptedException, PathfindingException
	{
		while(!isNewPathAvailable())
			wait();

		return getPath();
	}

	public void endContinuousSearchWithException(PathfindingException e)
	{
		this.e = e;
		endContinuousSearch();
	}
}
