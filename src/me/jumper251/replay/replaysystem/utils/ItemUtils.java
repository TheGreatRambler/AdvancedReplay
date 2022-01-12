package me.jumper251.replay.replaysystem.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.cryptomorin.xseries.XMaterial;

import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;



public class ItemUtils {

	  private static final List<Material> INTERACTABLE = new ArrayList<>();

	  public static boolean isInteractable(Material mat) {
		  if (mat == null) return false;
		  
		  return INTERACTABLE.contains(mat);
	  }
	  
	  public static boolean isUsable(Material mat) {
		  if (mat == null) return false;

		  return mat.isEdible() || mat == Material.POTION || mat == Material.MILK_BUCKET || mat == Material.BOW || (!VersionUtil.isCompatible(VersionEnum.V1_8) && mat == Material.SHIELD) || (VersionUtil.isCompatible(VersionEnum.V1_8) && isSword(mat));
	  }
	  
	  public static boolean isSword(Material mat) {
		  return mat == Material.WOODEN_SWORD || mat == Material.GOLDEN_SWORD || mat == Material.IRON_SWORD || mat == Material.DIAMOND_SWORD;
	  }
	  
	  static {
		  INTERACTABLE.add(XMaterial.STONE_BUTTON.parseMaterial());
		    INTERACTABLE.add(XMaterial.LEVER.parseMaterial());
		    INTERACTABLE.add(XMaterial.CHEST.parseMaterial());
		    INTERACTABLE.add(XMaterial.OAK_BUTTON.parseMaterial());
		    INTERACTABLE.add(XMaterial.OAK_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.COMPARATOR.parseMaterial());
		    //INTERACTABLE.add(Material.REDSTONE_COMPARATOR_OFF);
		    //INTERACTABLE.add(Material.REDSTONE_COMPARATOR_ON);
		    if(!VersionUtil.isCompatible(VersionEnum.V1_8)){
		    	INTERACTABLE.add(XMaterial.COMMAND_BLOCK.parseMaterial());
		    	INTERACTABLE.add(XMaterial.CHAIN_COMMAND_BLOCK.parseMaterial());
		    	INTERACTABLE.add(XMaterial.COMMAND_BLOCK_MINECART.parseMaterial());
		    	INTERACTABLE.add(XMaterial.REPEATING_COMMAND_BLOCK.parseMaterial());
		    }
		    INTERACTABLE.add(XMaterial.BREWING_STAND.parseMaterial());
		    INTERACTABLE.add(XMaterial.FURNACE.parseMaterial());
		    //INTERACTABLE.add(Material.BURNING_FURNACE);
		    INTERACTABLE.add(XMaterial.OAK_SIGN.parseMaterial());
		    //INTERACTABLE.add(Material.SIGN_POST);
		    INTERACTABLE.add(XMaterial.OAK_WALL_SIGN.parseMaterial());
		    INTERACTABLE.add(XMaterial.TRAPPED_CHEST.parseMaterial());
		    INTERACTABLE.add(XMaterial.OAK_TRAPDOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.ENCHANTING_TABLE.parseMaterial());
		    INTERACTABLE.add(XMaterial.DROPPER.parseMaterial());
		    INTERACTABLE.add(XMaterial.DISPENSER.parseMaterial());
		    INTERACTABLE.add(XMaterial.ENDER_CHEST.parseMaterial());
		    INTERACTABLE.add(XMaterial.OAK_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.BEACON.parseMaterial());
		    INTERACTABLE.add(XMaterial.NOTE_BLOCK.parseMaterial());
		    INTERACTABLE.add(XMaterial.JUKEBOX.parseMaterial());
		    INTERACTABLE.add(XMaterial.HOPPER.parseMaterial());
		    INTERACTABLE.add(XMaterial.SPRUCE_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.ACACIA_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.DARK_OAK_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.JUNGLE_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.BIRCH_DOOR.parseMaterial());
		    INTERACTABLE.add(XMaterial.SPRUCE_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.ACACIA_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.JUNGLE_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.BIRCH_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.DARK_OAK_FENCE_GATE.parseMaterial());
		    INTERACTABLE.add(XMaterial.OAK_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.SPRUCE_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.JUNGLE_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.ACACIA_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.BIRCH_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.DARK_OAK_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.NETHER_BRICK_FENCE.parseMaterial());
		    INTERACTABLE.add(XMaterial.ANVIL.parseMaterial());
		    INTERACTABLE.add(XMaterial.DAYLIGHT_DETECTOR.parseMaterial());
		    //INTERACTABLE.add(Material.DAYLIGHT_DETECTOR_INVERTED);
		    INTERACTABLE.add(XMaterial.CRAFTING_TABLE.parseMaterial());
	  }
}
