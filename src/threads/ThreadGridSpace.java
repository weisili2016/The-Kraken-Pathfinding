package threads;

import container.Service;
import table.GridSpace;
import table.ObstacleManager;
import utils.Config;
import utils.Log;

/**
 * S'occupe de la mise à jour du cache. Surveille obstaclemanager
 * @author pf
 *
 */

public class ThreadGridSpace extends ThreadAvecStop implements Service {

	protected Log log;
	private ObstacleManager obstaclemanager;
	private GridSpace gridspace;
	
	public ThreadGridSpace(Log log, ObstacleManager obstaclemanager, GridSpace gridspace)
	{
		this.log = log;
		this.obstaclemanager = obstaclemanager;
		this.gridspace = gridspace;
	}

	@Override
	public void run()
	{
		while(!finThread)
		{
			synchronized(obstaclemanager)
			{
				try {
					obstaclemanager.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
//			log.debug("Réveil de ThreadGridSpace");	
			
			gridspace.reinitConnections();
		}

	}

	@Override
	public void updateConfig(Config config)
	{}

	@Override
	public void useConfig(Config config)
	{}

}
