package me.jumper251.replay.replaysystem.data.types;

import com.comphenix.protocol.wrappers.BlockPosition;

public class WorldEventData extends PacketData{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1831237983552014380L;

	
	int effectId;
	int locX;
	int locY;
	int locZ;
	int data;
	boolean disableRelativeVolume;

	
	public WorldEventData(int effectId, BlockPosition location, int data, boolean disableRelativeVolume) {
		this.effectId = effectId;
		this.locX = location.getX();
		this.locY = location.getY();
		this.locZ = location.getZ();
		this.data = data;
		this.disableRelativeVolume = disableRelativeVolume;
	}
	
	public int getEffectId() {
		return effectId;
	}

	public BlockPosition getLocation() {
		return new BlockPosition(locX, locY, locZ);
	}

	public int getData() {
		return data;
	}

	public boolean getDisableRelativeVolume() {
		return disableRelativeVolume;
	}

}
