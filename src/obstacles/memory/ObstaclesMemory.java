/*
Copyright (C) 2016 Pierre-François Gimenez

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>
*/

package obstacles.memory;

import java.util.LinkedList;

import obstacles.types.ObstacleProximity;
import pathfinding.dstarlite.gridspace.Masque;
import utils.Config;
import utils.ConfigInfo;
import utils.Log;
import utils.Vec2RO;
import container.Service;

/**
 * Mémorise tous les obstacles mobiles qu'on a rencontré jusque là.
 * Il y a un mécanisme de libération de mémoire transparent.
 * @author pf
 *
 */

public class ObstaclesMemory implements Service
{
    // Les obstacles mobiles, c'est-à-dire des obstacles de proximité
    private volatile LinkedList<ObstacleProximity> listObstaclesMobiles = new LinkedList<ObstacleProximity>();
    private volatile LinkedList<ObstacleProximity> listObstaclesMortsTot = new LinkedList<ObstacleProximity>();
    private int dureeAvantPeremption;
	private int rayonEnnemi;
	private volatile int size = 0;
	private volatile int indicePremierObstacle = 0;
	private volatile int firstNotDeadNow = 0;
	private volatile long nextDeathDate = Long.MAX_VALUE;
	
	protected Log log;
	
	public ObstaclesMemory(Log log)
	{
		this.log = log;
	}

	public synchronized ObstacleProximity add(Vec2RO position, Masque masque)
	{
		return add(position, System.currentTimeMillis(), masque);
	}

	private synchronized ObstacleProximity add(Vec2RO position, long date, Masque masque)
	{
        ObstacleProximity obstacle = new ObstacleProximity(position, rayonEnnemi, date+dureeAvantPeremption, masque);
        listObstaclesMobiles.add(obstacle);
        size++;
		return obstacle;
	}
	
	public synchronized int size()
	{
		return size;
	}
	
	@Override
	public void updateConfig(Config config)
	{}

	@Override
	public void useConfig(Config config)
	{
		rayonEnnemi = config.getInt(ConfigInfo.RAYON_ROBOT_ADVERSE);
		dureeAvantPeremption = config.getInt(ConfigInfo.DUREE_PEREMPTION_OBSTACLES);
	}

	public synchronized ObstacleProximity getObstacle(int nbTmp)
	{
		if(nbTmp < indicePremierObstacle)
		{
			log.critical("Erreur : demande d'un vieil obstacle");
			return null;
		}
		return listObstaclesMobiles.get(nbTmp-indicePremierObstacle);
	}

	/**
	 * Supprime cet obstacle
	 * @param indice
	 */
	public synchronized void remove(int indice)
	{
		listObstaclesMortsTot.add(listObstaclesMobiles.get(indice-indicePremierObstacle));
		listObstaclesMobiles.set(indice-indicePremierObstacle, null);
	}
	
	/**
	 * Renvoie vrai s'il y a effectivement suppression.
	 * On conserve les obstacles récemment périmés, car le DStarLite en a besoin.
	 * Une recherche dichotomique ne serait pas plus efficace car on oublie peu d'obstacles à la fois
	 * @return
	 */
	public synchronized boolean deleteOldObstacles()
	{
		long dateActuelle = System.currentTimeMillis();
		int firstNotDeadNowSave = firstNotDeadNow;
		
		// S'il est périmé depuis deux secondes : on vire.
		while(!listObstaclesMobiles.isEmpty() && (listObstaclesMobiles.getFirst() == null || listObstaclesMobiles.getFirst().isDestructionNecessary(dateActuelle-2000)))
		{
			indicePremierObstacle++;
			listObstaclesMobiles.removeFirst();
		}
		
		nextDeathDate = Long.MAX_VALUE;
		firstNotDeadNow = 0;
		while(firstNotDeadNow < listObstaclesMobiles.size())
		{
			ObstacleProximity o = listObstaclesMobiles.get(firstNotDeadNow);
			// s'il est fraîchement périmé, on prévient qu'il y a du changement mais on conserve quand même l'obstacle en mémoire
			if(o == null || o.isDestructionNecessary(dateActuelle))
				firstNotDeadNow++;
			else
			{
				nextDeathDate = o.getDeathDate();
				break;
			}
		}
		firstNotDeadNow += indicePremierObstacle;
		return firstNotDeadNow != firstNotDeadNowSave;
	}
	
	public synchronized long getNextDeathDate()
	{
		return nextDeathDate;
	}
	
	public synchronized int getFirstNotDeadNow()
	{
		return firstNotDeadNow;
	}

	/**
	 * Il s'agit forcément d'une date du futur
	 * @param firstNotDead
	 * @param date
	 * @return
	 */
	public boolean isDestructionNecessary(int indice, long date)
	{
		return indice < firstNotDeadNow || listObstaclesMobiles.get(indice-indicePremierObstacle) == null || listObstaclesMobiles.get(indice-indicePremierObstacle).isDestructionNecessary(date);
	}

	/**
	 * Permet de récupérer les obstacles morts prématurément
	 * @return
	 */
	public ObstacleProximity pollMortTot()
	{
		return listObstaclesMortsTot.poll();
	}
	
}
