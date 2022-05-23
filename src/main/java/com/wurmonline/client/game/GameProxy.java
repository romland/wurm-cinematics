package com.wurmonline.client.game;

import com.friya.wurmonline.client.hollywurm.TweenPosition;
import com.wurmonline.client.renderer.cell.CellRenderable;

public class GameProxy
{
/*
	public static PlayerObj getFakePlayerObj()
	{
		return (new PlayerObj(CellRenderable.world));
	}
*/
	static public void copyPPRot(PlayerPosition from, PlayerPosition to)
	{
		//WurmHelpers.tellCinematics("Is rot: " + to.xRot + " " + to.yRot);
		to.xRot.reallySetValue(from.xRot.getValue());
		to.yRot.reallySetValue(from.yRot.getValue());
		//WurmHelpers.tellCinematics("Setting rot: " + to.xRot + " " + to.yRot);
	}
	
	static public void copyPPRot(PlayerPosition from, TweenPosition to)
	{
		//WurmHelpers.tellCinematics("Is rot: " + to.xRot + " " + to.yRot);
		to.setXRot(from.xRot.getValue());
		to.setYRot(from.yRot.getValue());
		//WurmHelpers.tellCinematics("Setting rot: " + to.xRot + " " + to.yRot);
	}

	static public void setPPXRot(PlayerPosition pp, float rot)
	{
		pp.xRot.setValue(rot);
	}

	static public void setPPYRot(PlayerPosition pp, float rot)
	{
		pp.xRot.setValue(rot);
	}

/*
	static public void minimalSetNewPlayerPosition(PlayerObj player, float x, float y, float h, float xrot, float yrot)
	{
		PlayerPosition pos = player.getPos();
        pos.setX(x);
        pos.setY(y);
        pos.setH(h);
        player.getPlayerBody().setPos(x, y, h);
        pos.xRot.setValue(xrot);
        pos.yRot.setValue(yrot);
        if (pos.move()) {
        	CellRenderable.world.signalTileChange(pos.getTileX(), pos.getTileY());
        }
        pos.setLayer(player.getLayer());
	}
*/
}
