package tests;

import java.util.ArrayList;
import java.util.Random;

import obstacles.ObstacleManager;

import org.junit.Before;
import org.junit.Test;

import pathfinding.AStar;
import pathfinding.PathfindingArcManager;
import pathfinding.StrategyArcManager;
import enums.PathfindingNodes;
import enums.ScriptNames;
import enums.ServiceNames;
import robot.RobotChrono;
import robot.RobotReal;
import scripts.Decision;
import smartMath.Vec2;
import strategie.GameState;
import tests.graphicLib.Fenetre;
import utils.Sleep;

/**
 * Tests unitaires disposant d'une interface graphique.
 * Utilisé pour la vérification humaine.
 * @author pf
 *
 */

public class JUnit_Test_Graphic extends JUnit_Test {

	private Fenetre fenetre;
	private ObstacleManager obstaclemanager;
	private AStar<PathfindingArcManager, PathfindingNodes> pathfinding;
	private GameState<RobotChrono> state_chrono;
	private GameState<RobotReal> state;
	private AStar<StrategyArcManager, Decision> strategic_astar;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception
	{
		super.setUp();
        pathfinding = (AStar<PathfindingArcManager, PathfindingNodes>) container.getService(ServiceNames.A_STAR_PATHFINDING);
		obstaclemanager = (ObstacleManager) container.getService(ServiceNames.OBSTACLE_MANAGER);
		state = (GameState<RobotReal>)container.getService(ServiceNames.REAL_GAME_STATE);
		strategic_astar = (AStar<StrategyArcManager, Decision>)container.getService(ServiceNames.A_STAR_STRATEGY);

		fenetre = new Fenetre();
		fenetre.setDilatationObstacle(obstaclemanager.getDilatationObstacle());
		for(PathfindingNodes n : PathfindingNodes.values())
		{
			fenetre.addPoint(n.getCoordonnees());
			for(PathfindingNodes m : PathfindingNodes.values())
				if(!obstaclemanager.obstacle_fixe_dans_segment_pathfinding(n.getCoordonnees(), m.getCoordonnees()))
						fenetre.addSegment(m.getCoordonnees(), n.getCoordonnees());
		}
		updateAffichage();
		fenetre.showOnFrame();
	}
	
	public void updateAffichage()
	{
		fenetre.setGameElement(obstaclemanager.getListGameElement());
		fenetre.setObstaclesFixes(obstaclemanager.getListObstaclesFixes());
		// TODO: affiche aussi les obstacles périmés
		fenetre.setObstaclesMobiles(obstaclemanager.getListObstaclesMobiles());		
	}

	@Test
    public void test_verification_humaine() throws Exception
    {
		Random randomgenerator = new Random();
		for(int k = 0; k < 20; k++)
		{
			PathfindingNodes i = PathfindingNodes.values()[randomgenerator.nextInt(PathfindingNodes.values().length)];
			PathfindingNodes j = PathfindingNodes.values()[randomgenerator.nextInt(PathfindingNodes.values().length)];
			log.debug("Recherche chemin entre "+i+" et "+j, this);
			Vec2 entree = i.getCoordonnees().plusNewVector(new Vec2(randomgenerator.nextInt(100)-50, randomgenerator.nextInt(100)-50));
			config.setDateDebutMatch(); // afin d'avoir toujours une haute précision
			state_chrono = state.cloneGameState();
			double orientation_initiale = state_chrono.robot.getOrientation();
			state_chrono.robot.setPosition(entree);
			ArrayList<PathfindingNodes> chemin = pathfinding.computePath(state_chrono, j, true);
    		ArrayList<Vec2> cheminVec2 = new ArrayList<Vec2>();
    		cheminVec2.add(entree);
    		for(PathfindingNodes n: chemin)
    		{
    			log.debug(n, this);
    			cheminVec2.add(n.getCoordonnees());
    		}
    		fenetre.setPath(orientation_initiale, cheminVec2);
    		fenetre.repaint();
    		Sleep.sleep(1000);
		}
    }


    @Test
    public void test_strategy_verification_humaine() throws Exception
    {
    	Decision decision = new Decision(null, ScriptNames.SortieZoneDepart, 0);
    	config.setDateDebutMatch();
    	GameState<RobotChrono> chronostate = state.cloneGameState();
		ArrayList<Decision> decisions = strategic_astar.computeStrategyAfter(chronostate, decision);
		for(Decision d: decisions)
		{
    		ArrayList<Vec2> cheminVec2 = new ArrayList<Vec2>();
    		for(PathfindingNodes n: d.chemin)
    		{
    			log.debug(n, this);
    			cheminVec2.add(n.getCoordonnees());
    		}
			fenetre.setPath(null, cheminVec2);
			fenetre.repaint();
			Sleep.sleep(1000);
			log.debug(d, this);
		}
    }

}
