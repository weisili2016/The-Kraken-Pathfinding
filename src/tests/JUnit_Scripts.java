package tests;

import java.util.ArrayList;

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

import robot.RobotChrono;
import robot.RobotReal;
import scripts.Script;
import scripts.ScriptManager;
import smartMath.Vec2;
import strategie.GameState;
import enums.PathfindingNodes;
import enums.ScriptNames;
import enums.ServiceNames;

/**
 * Tests unitaires des scripts.
 * Utilisé pour voir en vrai comment agit le robot et si la table est bien mise à jour.
 * @author pf
 *
 */

// TODO: vérifier que les points d'entrée sont de basse précision

public class JUnit_Scripts extends JUnit_Test {

	private ScriptManager scriptmanager;
	private GameState<RobotReal> gamestate;
	private GameState<RobotChrono> state_chrono;
		
    @SuppressWarnings("unchecked")
	@Before
    public void setUp() throws Exception {
        super.setUp();
        gamestate = (GameState<RobotReal>) container.getService(ServiceNames.REAL_GAME_STATE);
        scriptmanager = (ScriptManager) container.getService(ServiceNames.SCRIPT_MANAGER);
        gamestate.robot.setPosition(new Vec2(1100, 1000));
        state_chrono = gamestate.cloneGameState();
    }

    @Test
    public void test_script_tapis_chrono() throws Exception
    {
    	long hash_avant = state_chrono.getHash();
    	Script s = scriptmanager.getScript(ScriptNames.ScriptTapis);
    	Assert.assertTrue(s.meta_version(state_chrono).size() == 1);
    	state_chrono.robot.setPositionPathfinding(s.point_entree(0));
    	s.agit(0, state_chrono);
    	Assert.assertTrue(s.meta_version(state_chrono).size() == 0);
    	Assert.assertNotEquals(hash_avant, state_chrono.getHash());
    }

    @Test
    public void test_script_hash() throws Exception
    {
    	GameState<RobotChrono> state_chrono2 = state_chrono.cloneGameState();
    	Script tapis = scriptmanager.getScript(ScriptNames.ScriptTapis);
    	Script clap = scriptmanager.getScript(ScriptNames.ScriptClap);

    	log.debug(state_chrono.robot.getPosition(), this);
    	log.debug(state_chrono2.robot.getPosition(), this);
    	Assert.assertEquals(state_chrono.getHash(), state_chrono2.getHash());

    	state_chrono.robot.setPositionPathfinding(tapis.point_entree(0));
    	state_chrono2.robot.setPositionPathfinding(clap.point_entree(1));
    	tapis.agit(0, state_chrono);
    	clap.agit(1, state_chrono2);
    	log.debug(state_chrono.robot.getPosition(), this);
    	log.debug(state_chrono2.robot.getPosition(), this);
    	Assert.assertNotEquals(state_chrono.getHash(), state_chrono2.getHash());

    	state_chrono.robot.setPositionPathfinding(clap.point_entree(1));
    	state_chrono2.robot.setPositionPathfinding(tapis.point_entree(0));
    	clap.agit(1, state_chrono);
    	tapis.agit(0, state_chrono2);

    	Assert.assertNotEquals(state_chrono.getHash(), state_chrono2.getHash());

    	state_chrono.robot.setPositionPathfinding(clap.point_entree(0));
    	state_chrono2.robot.setPositionPathfinding(clap.point_entree(0));
    	clap.agit(0, state_chrono);
    	clap.agit(0, state_chrono2);
    	Assert.assertEquals(state_chrono.getHash(), state_chrono2.getHash());
    }

    @Test
    public void test_script_clap_chrono() throws Exception
    {
    	long hash_avant = state_chrono.gridspace.getHashTable();
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	Assert.assertTrue(s.meta_version(state_chrono).size() == 2);
    	state_chrono.robot.setPositionPathfinding(s.point_entree(0));
    	s.agit(0, state_chrono);
    	Assert.assertNotEquals(hash_avant, state_chrono.gridspace.getHashTable());
    	long hash_apres = state_chrono.gridspace.getHashTable();
    	Assert.assertTrue(s.meta_version(state_chrono).size() == 1);
    	state_chrono.robot.setPositionPathfinding(s.point_entree(1));
    	s.agit(1, state_chrono);
    	Assert.assertTrue(s.meta_version(state_chrono).size() == 0);
    	Assert.assertNotEquals(hash_apres, state_chrono.gridspace.getHashTable());
    }

    @Test
    public void test_script_tapis() throws Exception
    {
    	// TODO: utiliser le pathfinding
    	Script s = scriptmanager.getScript(ScriptNames.ScriptTapis);
    	ArrayList<PathfindingNodes> chemin = new ArrayList<PathfindingNodes>(); 
    	chemin.add(s.point_entree(0));
    	gamestate.robot.suit_chemin(chemin, null);
    	s.agit(0, gamestate);
    }

    @Test
    public void test_script_clap() throws Exception
    {
    	Script s = scriptmanager.getScript(ScriptNames.ScriptClap);
    	ArrayList<PathfindingNodes> chemin = new ArrayList<PathfindingNodes>(); 
    	chemin.add(s.point_entree(0));
    	gamestate.robot.suit_chemin(chemin, null);
    	s.agit(0, gamestate);
    }

}
