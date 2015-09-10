package pathfinding.thetastar;

import java.util.ArrayList;

import pathfinding.dstarlite.GridSpace;
import permissions.ReadOnly;
import permissions.ReadWrite;
import utils.Vec2;

/**
 * Un arc de trajectoire courbe
 * Utilisé par le pathfinding et envoyable par série
 * @author pf
 *
 */

public class LocomotionArc
{
	private int pointDepartGridpoint;
	private VitesseCourbure rayonCourbure;
	private int gridpointArrivee;
	private double angleDepart;
	private Vec2<ReadWrite> pointDepart = new Vec2<ReadWrite>();
	private Vec2<ReadWrite> centreCercleRotation = new Vec2<ReadWrite>();
	private Vec2<ReadWrite> normaleAuDemiPlan = new Vec2<ReadWrite>();
	private Vec2<ReadWrite> destination = new Vec2<ReadWrite>();
	private Vec2<ReadWrite> tmp = new Vec2<ReadWrite>();
	private boolean sensTrigo;
	
	public LocomotionArc()
	{}
		
	/**
	 * La copie est utilisée juste avant l'envoi.
	 * @param other
	 */
	public void copy(LocomotionArc other)
	{
		other.pointDepartGridpoint= pointDepartGridpoint;
		Vec2.copy(normaleAuDemiPlan.getReadOnly(), other.normaleAuDemiPlan);
		other.rayonCourbure = rayonCourbure;
		other.gridpointArrivee = gridpointArrivee;
	}
	
	public final boolean getSensTrigo()
	{
		return sensTrigo;
	}
	
	public final Vec2<ReadOnly> getCentreCercleRotation()
	{
		return centreCercleRotation.getReadOnly();
	}

	public final Vec2<ReadOnly> getNormaleAuDemiPlan()
	{
		return normaleAuDemiPlan.getReadOnly();
	}

	public final Vec2<ReadOnly> getPointDepart()
	{
		return pointDepart.getReadOnly();
	}

	public final Vec2<ReadOnly> getDestination()
	{
		return destination.getReadOnly();
	}

	public final int getGridpointArrivee()
	{
		return gridpointArrivee;
	}
	
	public void update(GridSpace gridspace, int pointDepartGridpoint,
			VitesseCourbure rayonCourbure, int gridpointArrivee, double angleDepart)
	{
		this.angleDepart = angleDepart;
		this.pointDepartGridpoint = pointDepartGridpoint;
		this.rayonCourbure = rayonCourbure;
		this.gridpointArrivee = gridpointArrivee;
		Vec2.setAngle(normaleAuDemiPlan, angleDepart);
		gridspace.computeVec2(pointDepart, pointDepartGridpoint);
		Vec2.copy(pointDepart.getReadOnly(), centreCercleRotation);
		gridspace.computeVec2(destination, gridpointArrivee);
		tmp.x = normaleAuDemiPlan.y;
		tmp.y = -normaleAuDemiPlan.x;
		Vec2.scalar(tmp, rayonCourbure.rayon/1000.);
		if(tmp.x * (destination.x - pointDepart.x) + tmp.y * (destination.y - pointDepart.y) > 0)
		{
			sensTrigo = false;
			Vec2.plus(centreCercleRotation, tmp);
		}
		else
		{
			sensTrigo = true;
			Vec2.minus(centreCercleRotation, tmp);
		}
	}
	
	public ArrayList<String> toSerial(GridSpace gridspace)
	{
		Vec2<ReadOnly> pointDuDemiPlan = gridspace.computeVec2(pointDepartGridpoint);
//		angleConsigne = Math.atan2(destination.y - pointDuDemiPlan.y, destination.x - pointDuDemiPlan.x);
//		if(enMarcheAvant)
//			angleConsigne += Math.PI;
		ArrayList<String> out = new ArrayList<String>();
		out.add(String.valueOf(pointDuDemiPlan.x));
		out.add(String.valueOf(pointDuDemiPlan.y));
		out.add(String.valueOf(normaleAuDemiPlan.x));
		out.add(String.valueOf(normaleAuDemiPlan.y));
		out.add(String.valueOf(destination.x));
		out.add(String.valueOf(destination.y));
//		out.add(String.valueOf(new String(Long.toString(Math.round(angleConsigne*1000)))));
		out.add(String.valueOf(rayonCourbure));
		return out;
	}

	public ArrayList<String> toSerialFirst(GridSpace gridspace)
	{
		ArrayList<String> out = new ArrayList<String>();
		out.add(String.valueOf(destination.x));
		out.add(String.valueOf(destination.y));
//		out.add(String.valueOf(new String(Long.toString(Math.round(angleConsigne*1000)))));
		out.add(String.valueOf(rayonCourbure));
		return out;
	}

	public String toString()
	{
		return "Arc de "+pointDepartGridpoint+" à "+gridpointArrivee+" avec courbure "+rayonCourbure.rayon;
	}

	public final VitesseCourbure getRayonCourbure()
	{
		return rayonCourbure;
	}

	public double getAngleDepart()
	{
		return angleDepart;
	}
	
}