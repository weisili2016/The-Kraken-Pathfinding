package container;

/**
 * Enumération des différents services. Plus d'informations sur les services dans Container.
 * @author pf
 *
 */
public enum ServiceNames {
	 LOG,
	 CONFIG,
	 TABLE,
	 CAPTEURS,
	 ROBOT_REAL,
	 REAL_GAME_STATE,
	 SERIE,
//	 SERIE_XBEE,
	 HEURISTIQUE_SIMPLE,
	 D_STAR_LITE,
	 GRID_SPACE,

	 A_STAR_COURBE_PLANIFICATION,
	 A_STAR_COURBE_DYNAMIQUE,
	 A_STAR_COURBE_MEMORY_MANAGER_PLANIF,
	 A_STAR_COURBE_MEMORY_MANAGER_DYN,
	 A_STAR_COURBE_ARC_MANAGER_PLANIF,
	 A_STAR_COURBE_ARC_MANAGER_DYN,

	 //	 THETA_STAR,
//	 THETA_STAR_ARC_MANAGER,

	 CHEMIN_PATHFINDING,
	 CHEMIN_PLANIF,
	 INCOMING_DATA_BUFFER,
//	 THETA_STAR_MEMORY_MANAGER,
	 SERIAL_LOW_LEVEL,
	 SERIAL_OUTPUT_BUFFER,
	 INCOMING_DATA_DEBUG_BUFFER,
	 MOTEUR_PHYSIQUE,
	 OBSTACLES_MEMORY,
	 CLOTHOIDES_COMPUTER,
	 
	 // Les threads
	 THREAD_SERIAL_INPUT,
	 THREAD_SERIAL_OUTPUT,
	 THREAD_SERIAL_OUTPUT_TIMEOUT,
	 THREAD_CONFIG,
	 THREAD_PEREMPTION,
	 THREAD_EVITEMENT,
	 THREAD_PATHFINDING,
	 THREAD_CAPTEURS;

	 private boolean isThread = false;
	 
	 private ServiceNames()
	 {
		 isThread = name().startsWith("THREAD_");
	 }
	 
	 public boolean isThread()
	 {
		 return isThread;
	 }
	 
}
