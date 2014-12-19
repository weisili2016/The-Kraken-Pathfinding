package pathfinding;

import java.util.ArrayList;
import java.util.Iterator;

import container.Service;
import enums.PathfindingNodes;
import exceptions.FinMatchException;
import exceptions.GridSpaceException;
import exceptions.PathfindingException;
import exceptions.PathfindingRobotInObstacleException;
import exceptions.UnknownScriptException;
import exceptions.Locomotion.UnableToMoveException;
import exceptions.serial.SerialConnexionException;
import robot.RobotChrono;
import scripts.Decision;
import smartMath.Vec2;
import strategie.GameState;
import strategie.MemoryManager;
import utils.Config;
import utils.Log;

/**
 * Classe encapsulant les calculs de pathfinding
 * @author pf, Martial
 *
 */

public class AStar implements Service
{
	/**
	 * Les analogies sont:
	 * un noeud est un GameState<RobotChrono> dans les deux cas
	 * un arc est un script pour l'arbre des possibles,
	 *   un pathfindingnode pour le pathfinding
	 */
	
	private static final int nb_max_element = 100;
	
	private ArrayList<GameState<RobotChrono>> openset = new ArrayList<GameState<RobotChrono>>();	 // The set of tentative nodes to be evaluated
	private boolean[] closedset = new boolean[nb_max_element];
	private int[] came_from = new int[nb_max_element];
	private Arc[] came_from_arc = new Arc[nb_max_element];
	private int[] g_score = new int[nb_max_element];
	private int[] f_score = new int[nb_max_element];
	
	private Log log;
	private PathfindingArcManager pfarcmanager;
	private StrategyArcManager stratarcmanager;
	private MemoryManager memorymanager;
	
	/**
	 * Constructeur du système de recherche de chemin
	 */
	public AStar(Log log, Config config, PathfindingArcManager pfarcmanager, StrategyArcManager stratarcmanager, MemoryManager memorymanager)
	{
		this.log = log;
		this.pfarcmanager = pfarcmanager;
		this.stratarcmanager = stratarcmanager;
		this.memorymanager = memorymanager;
		// Afin d'outrepasser la dépendance circulaire qui provient de la double
		// utilisation de l'AStar (stratégie et pathfinding)
		stratarcmanager.setAStar(this);
	}
	
	public ArrayList<Decision> computeStrategy(GameState<RobotChrono> state) throws FinMatchException
	{
		GameState<RobotChrono> depart = memorymanager.getNewGameState();
		state.copy(depart);
		stratarcmanager.reinitHashes();
		ArrayList<Arc> cheminArc;
		ArrayList<Decision> decisions = new ArrayList<Decision>();
		try {
			cheminArc = process(depart, stratarcmanager, true);
			for(Arc arc: cheminArc)
				decisions.add((Decision)arc);
		} catch (PathfindingException e) {
			// impossible car on a appelé process avec shouldReconstruct = true;
			e.printStackTrace();
		}
		return decisions;
	}
	
	public ArrayList<PathfindingNodes> computePath(GameState<RobotChrono> state, PathfindingNodes indice_point_arrivee, boolean shoot_game_element) throws PathfindingException, PathfindingRobotInObstacleException, FinMatchException
	{
		try {
			state.gridspace.setAvoidGameElement(!shoot_game_element);

			PathfindingNodes pointDepart;
			if(state.robot.isAtPathfindingNodes())
				pointDepart = state.robot.getPositionPathfinding();
			else
			{
				pointDepart = state.gridspace.nearestReachableNode(state.robot.getPosition(), state.robot.getTempsDepuisDebutMatch());
			}

			GameState<RobotChrono> depart = memorymanager.getNewGameState();
			state.copy(depart);
			depart.robot.setPositionPathfinding(pointDepart);
			
			pfarcmanager.chargePointArrivee(indice_point_arrivee);
			
//			log.debug("Recherche de chemin entre "+pointDepart+" et "+indice_point_arrivee, this);
			ArrayList<Arc> cheminArc = process(depart, pfarcmanager, false);
			
			ArrayList<PathfindingNodes> chemin = new ArrayList<PathfindingNodes>(); 
			chemin.add(pointDepart);
			for(Arc arc: cheminArc)
				chemin.add((PathfindingNodes)arc);

			// on n'a besoin de lisser que si on ne partait pas d'un pathfindingnode
			if(state.robot.isAtPathfindingNodes())
				chemin = lissage(state.robot.getPosition(), state, chemin);
			
//			log.debug("Recherche de chemin terminée", this);
			return chemin;
		} catch (GridSpaceException e1) {
			throw new PathfindingRobotInObstacleException();
		}
	}
	
	@Override
	public void updateConfig() {
	}
	
	// Si le point de départ est dans un obstacle fixe, le lissage ne changera rien.
	private ArrayList<PathfindingNodes> lissage(Vec2 depart, GameState<RobotChrono> state, ArrayList<PathfindingNodes> chemin)
	{
		// TODO: attention! avec quel robot lisse-t-on? et si on zappe des points, ça modifie le timing et tout... 
		// si on peut sauter le premier point, on le fait
		while(chemin.size() >= 2 && state.gridspace.isTraversable(depart, chemin.get(1).getCoordonnees(), state.robot.getTempsDepuisDebutMatch()))
			chemin.remove(0);

		return chemin;
	}
	
	private ArrayList<Arc> process(GameState<RobotChrono> depart, ArcManager arcmanager, boolean shouldReconstruct) throws PathfindingException, FinMatchException
	{
		int hash_depart = arcmanager.getHash(depart);
		// optimisation si depart == arrivee
		if(arcmanager.isArrive(hash_depart))
		{
			depart = memorymanager.destroyGameState(depart);
			return new ArrayList<Arc>();
		}

		for(int i = 0; i < nb_max_element; i++)
		{
			closedset[i] = false;
			came_from_arc[i] = null;
			came_from[i] = -1;
			g_score[i] = Integer.MAX_VALUE;
			f_score[i] = Integer.MAX_VALUE;
		}
		
		openset.clear();
		openset.add(depart);	// The set of tentative nodes to be evaluated, initially containing the start node
		g_score[hash_depart] = 0;	// Cost from start along best known path.
		// Estimated total cost from start to goal through y.
		f_score[hash_depart] = g_score[hash_depart] + arcmanager.heuristicCost(depart);
		
		GameState<RobotChrono> current;
		Iterator<GameState<RobotChrono>> nodeIterator;

		while (openset.size() != 0)
		{
			// TODO: openset trié automatiquement à l'insertion
			// current is affected by the node in openset having the lowest f_score[] value
			nodeIterator = openset.iterator();
			current = nodeIterator.next();
			
			while(nodeIterator.hasNext())
			{
				GameState<RobotChrono> tmp = nodeIterator.next();
				if(f_score[arcmanager.getHash(tmp)] < f_score[arcmanager.getHash((current))])
					current = tmp;
			}

			int hash_current = arcmanager.getHash(current);
			
			if(arcmanager.isArrive(hash_current))
			{
				freeGameStateOpenSet(openset);
				return reconstruct(hash_current);
			}

			openset.remove(current);
			closedset[hash_current] = true;
			arcmanager.reinitIterator(current);
		    	
			while(arcmanager.hasNext(current))
			{
				Arc voisin = arcmanager.next();

//				log.debug("Voisin de "+current.robot.getPositionPathfinding()+": "+voisin, this);
				GameState<RobotChrono> successeur = memorymanager.getNewGameState();
				current.copy(successeur);
				
				// successeur est modifié lors du "distanceTo"
				// si ce successeur dépasse la date limite, on l'annule
				int tentative_g_score;
					try {
						tentative_g_score = g_score[hash_current] + arcmanager.distanceTo(successeur, voisin);
					} catch (UnknownScriptException | SerialConnexionException
							| UnableToMoveException e) {
						continue;
					}
				

//				if(arcmanager instanceof StrategyArcManager)
//					log.debug(voisin+" "+successeur.robot.areTapisPoses(), this);
				int hash_successeur = arcmanager.getHash(successeur);
				
				if(closedset[hash_successeur]) // si closedset contient ce hash
				{
					successeur = memorymanager.destroyGameState(successeur);
					continue;
				}

				if(!contains(openset, hash_successeur, arcmanager) || tentative_g_score < g_score[hash_successeur])
				{
					came_from[hash_successeur] = hash_current;
					came_from_arc[hash_successeur] = voisin;
					g_score[hash_successeur] = tentative_g_score;
					f_score[hash_successeur] = tentative_g_score + arcmanager.heuristicCost(successeur);
					// TODO: remplacer celui qui existe déjà par successeur?
					if(!contains(openset, hash_successeur, arcmanager))
						openset.add(successeur);
					else
						successeur = memorymanager.destroyGameState(successeur);
				}
				else
					successeur = memorymanager.destroyGameState(successeur);
			}
			
			current = memorymanager.destroyGameState(current);	
		}
		
		// Pathfinding terminé sans avoir atteint l'arrivée
		freeGameStateOpenSet(openset);
		
		// La stratégie renvoie un chemin partiel, pas le pathfinding qui lève une exception
		if(!shouldReconstruct)
			throw new PathfindingException();

		/**
		 * Même si on n'a pas atteint l'objectif, on reconstruit un chemin partiel
		 */
		int best = 0;
		int note_best = arcmanager.getNoteReconstruct(0);
		for(int h = 1; h < nb_max_element; h++)
		{
			int potentiel_note_best = arcmanager.getNoteReconstruct(h);
			if(f_score[h] != Integer.MAX_VALUE && potentiel_note_best > note_best)
			{
				best = h;
				note_best = potentiel_note_best;
			}
		}
		
		// best est nécessairement non nul car f_score contient au moins le point de départ
		return reconstruct(best);
		
	}
	
	
	private ArrayList<Arc> reconstruct(int hash) {
		ArrayList<Arc> chemin = new ArrayList<Arc>();
		int noeud_parent = came_from[hash];
		Arc arc_parent = came_from_arc[hash];
		while (noeud_parent != -1)
		{
			chemin.add(0, arc_parent);
			arc_parent = came_from_arc[noeud_parent];
			noeud_parent = came_from[noeud_parent];
		}
		return chemin;	//  reconstructed path
	}

	/**
	 * Return true si openset contient successeur, compte tenu de l'égalité de
	 * gamestate fournie par l'arcmanager.
	 * @param openset
	 * @param successeur
	 * @param arcmanager
	 * @return
	 */
	private boolean contains(ArrayList<GameState<RobotChrono>> openset,
			int hash_successeur, ArcManager arcmanager) {
		for(GameState<RobotChrono> state: openset)
			if(hash_successeur == arcmanager.getHash(state))
				return true;
		return false;
	}

	public void freeGameStateOpenSet(ArrayList<GameState<RobotChrono>> openset)
	{
		for(GameState<RobotChrono> state: openset)
			state = memorymanager.destroyGameState(state);
	}
	
}