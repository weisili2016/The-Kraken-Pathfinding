package obstacles;

import permissions.Permission;
import permissions.ReadOnly;
import utils.Vec2;

/**
 * Classe abstraite regroupant les obstacles de collision formés d'une collection
 * de rectangles
 * @author pf
 *
 */

public abstract class ObstacleRectanglesCollection<T extends Permission> extends Obstacle<T>
{
	public ObstacleRectanglesCollection(Vec2<T> position)
	{
		super(position);
	}

	protected ObstacleRectangular<ReadOnly>[] ombresRobot;
	protected int nb_rectangles;

	@Override
	public boolean isProcheObstacle(Vec2<ReadOnly> point, int distance)
	{
		for(int i = 0; i < nb_rectangles; i++)
			if(ombresRobot[i].isProcheObstacle(point, distance))
				return true;
		return false;
	}

	@Override
	public boolean isInObstacle(Vec2<ReadOnly> point)
	{
		for(int i = 0; i < nb_rectangles; i++)
			if(ombresRobot[i].isInObstacle(point))
				return true;
		return false;
	}	
	
	/**
	 * Y a-t-il collision avec un obstacle fixe?
	 * Il y a collision s'il y a collision avec au moins un des robots tournés.
	 * @param o
	 * @return
	 */
	public boolean isColliding(Obstacle<ReadOnly> o)
	{
		for(int i = 0; i < nb_rectangles; i++)
			if(ombresRobot[i].isColliding(o))
				return true;
		return false;
	}

	/**
	 * Utilisé pour l'affichage
	 * @return
	 */
	public ObstacleRectangular<ReadOnly>[] getOmbresRobot()
	{
		return ombresRobot;
	}

	/**
	 * Y a-t-il collision avec un obstacle fixe?
	 * @return
	 */
	public boolean isCollidingObstacleFixe()
	{
		for(ObstaclesFixes o: ObstaclesFixes.values)
			if(isColliding(o.getObstacle()))
				return true;
		return false;
	}

}
