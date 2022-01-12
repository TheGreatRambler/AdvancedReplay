package me.jumper251.replay.replaysystem.data.types;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

public class ItemData extends PacketData{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3882181315164039909L;

	
	Material id;
	int subId;
	BlockFace facing;
	
	public ItemData(Material id, int subId, BlockFace facing) {
		this.id = id;
		this.subId = subId;
		this.facing = facing;
	}
	
	public Material getId() {
		return id;
	}
	
	public int getSubId() {
		return subId;
	}

	public BlockFace getFacing() {
		return facing;
	}

}
