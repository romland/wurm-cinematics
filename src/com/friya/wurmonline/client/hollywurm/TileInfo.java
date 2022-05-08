package com.friya.wurmonline.client.hollywurm;
/*
package com.wurmonline.shared.constants;

public interface CounterTypes {
    public static final byte COUNTER_TYPE_PLAYERS = 0;
    public static final byte COUNTER_TYPE_CREATURES = 1;
    public static final byte COUNTER_TYPE_ITEMS = 2;
    public static final byte COUNTER_TYPE_TILES = 3;
    public static final byte COUNTER_TYPE_STRUCTURES = 4;
    public static final byte COUNTER_TYPE_WALLS = 5;
    public static final byte COUNTER_TYPE_TEMPITEMS = 6;
    public static final byte COUNTER_TYPE_FENCES = 7;
    public static final byte COUNTER_TYPE_WOUNDS = 8;
    public static final byte COUNTER_TYPE_CREATURESKILL = 9;
    public static final byte COUNTER_TYPE_PLAYERSKILL = 10; 
    public static final byte COUNTER_TYPE_TEMPLATESKILL = 11; 
    public static final byte COUNTER_TYPE_TILEBORDER = 12; 
    public static final byte COUNTER_TYPE_BANKS = 13; 
    public static final byte COUNTER_TYPE_PLANETS = 14; 
    public static final byte COUNTER_TYPE_SPELLS = 15; 
    public static final byte COUNTER_TYPE_PLANS = 16; 
    public static final byte COUNTER_TYPE_CAVETILES = 17; 
    public static final byte COUNTER_TYPE_SKILLIDS = 18; 
    public static final byte COUNTER_TYPE_BODYIDS = 19; 
    public static final byte COUNTER_TYPE_COINIDS = 20; 
    public static final byte COUNTER_TYPE_WCCOMMANDS = 21; 
    public static final byte COUNTER_TYPE_MISSIONPERFORMED = 22; 
    public static final byte COUNTER_TYPE_FLOORS = 23; 
    public static final byte COUNTER_TYPE_ILLUSIONS = 24; 
    public static final byte COUNTER_TYPE_TICKETS = 25; 
    public static final byte COUNTER_TYPE_POIIDS = 26; 
    public static final byte COUNTER_TYPE_TILECORNER = 27; 
    public static final byte COUNTER_TYPE_BRIDGE_PARTS = 28; 
    public static final byte COUNTER_TYPE_REDEEM_IDS = 29; 
    public static final byte COUNTER_TYPE_MENU_REQUEST = 30; 
    public static final byte COUNTER_TYPE_TEMPSKILLS = 31; 
    public static final byte COUNTER_TYPE_TEMPWOUNDS = 32; 
}
*/

import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.mesh.Tiles.Tile;

public class TileInfo 
{
	private int tileX;
	private int tileY;
	private int heightOffset;
	private int actionType;
	private float height;
	private Tile tileType;

	public TileInfo(long tileId)
	{
		tileX = decodeTileX(tileId);
		tileY = decodeTileY(tileId);
		heightOffset = decodeHeightOffset(tileId);

		// com.wurmonline.shared.constants.CounterTypes
		actionType = Actions.getType(tileId);
		height = CellRenderable.world.getNearTerrainBuffer().getHeight(tileX, tileY);
		tileType = CellRenderable.world.getNearTerrainBuffer().getTileType(tileX, tileY);
	}

    // taken from com/wurmonline/mesh/Tiles.java
	private short decodeTileX(long clientWurmId) {
		return (short)(clientWurmId >> 32 & 65535);
	}
	
    // taken from com/wurmonline/mesh/Tiles.java
	private int decodeTileY(long clientWurmId) {
		return (short)(clientWurmId >> 16 & 65535);
	}
	
    // taken from com/wurmonline/mesh/Tiles.java
	private int decodeHeightOffset(long clientWurmId) {
		return (short)(clientWurmId >> 48 & 65535);
	}

    // taken from com/wurmonline/mesh/Tiles.java
    @SuppressWarnings("unused")
	private byte decodeType(int encodedTile) {
        int type = encodedTile >> 24 & 255;
        return (byte)type;
    }

	int getTileX() {
		return tileX;
	}

	void setTileX(int tileX) {
		this.tileX = tileX;
	}

	int getTileY() {
		return tileY;
	}

	void setTileY(int tileY) {
		this.tileY = tileY;
	}

	int getHeightOffset() {
		return heightOffset;
	}

	void setHeightOffset(int heightOffset) {
		this.heightOffset = heightOffset;
	}

	int getActionType() {
		return actionType;
	}

	void setActionType(int actionType) {
		this.actionType = actionType;
	}

	float getHeight() {
		return height;
	}

	void setHeight(float height) {
		this.height = height;
	}

	Tile getTileType() {
		return tileType;
	}

	void setTileType(Tile tileType) {
		this.tileType = tileType;
	}
}
