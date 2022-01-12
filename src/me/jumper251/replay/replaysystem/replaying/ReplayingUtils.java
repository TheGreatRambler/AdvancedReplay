package me.jumper251.replay.replaysystem.replaying;



import java.util.ArrayDeque;


import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.jumper251.replay.replaysystem.data.types.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.packetwrapper.AbstractPacket;
import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerEntityEquipment;
import com.comphenix.packetwrapper.WrapperPlayServerEntityVelocity;
import com.comphenix.packetwrapper.WrapperPlayServerNamedSoundEffect;
import com.comphenix.packetwrapper.WrapperPlayServerWorldEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;

import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;
import com.comphenix.protocol.wrappers.EnumWrappers.SoundCategory;

import com.cryptomorin.xseries.XBlock;
import com.cryptomorin.xseries.XMaterial;

import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.filesystem.MessageBuilder;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.ActionType;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.replaysystem.recording.PlayerWatcher;
import me.jumper251.replay.replaysystem.utils.MetadataBuilder;
import me.jumper251.replay.replaysystem.utils.NPCManager;
import me.jumper251.replay.replaysystem.utils.entities.FishingUtils;
import me.jumper251.replay.replaysystem.utils.entities.IEntity;
import me.jumper251.replay.replaysystem.utils.entities.INPC;
import me.jumper251.replay.replaysystem.utils.entities.PacketEntity;
import me.jumper251.replay.replaysystem.utils.entities.PacketEntityOld;
import me.jumper251.replay.replaysystem.utils.entities.PacketNPC;
import me.jumper251.replay.replaysystem.utils.entities.PacketNPCOld;
import me.jumper251.replay.utils.MathUtils;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;

public class ReplayingUtils {

	private Replayer replayer;
		
	private Map<String, SignatureData> signatures;
	
	private Deque<ActionData> lastSpawnActions;
	
	private HashMap<Integer, Entity> itemEntities;
	
	private HashMap<Integer, Integer> hooks;

	private BlockFace lastFacing;

	public ReplayingUtils(Replayer replayer) {
		this.replayer = replayer;
		this.itemEntities = new HashMap<Integer, Entity>();
		this.hooks = new HashMap<Integer, Integer>();
		
		this.lastSpawnActions = new ArrayDeque<>();
		this.signatures = new HashMap<>();
	}
	
	public void handleAction(ActionData action, ReplayData data, boolean reversed) {
		if (action.getType() == ActionType.SPAWN) {
			if (!reversed) {
				spawnNPC(action);
			} else if (reversed && replayer.getNPCList().containsKey(action.getName())){
				INPC npc = this.replayer.getNPCList().get(action.getName());
				npc.remove();
				replayer.getNPCList().remove(action.getName());

			}
		}	
		
		if (action.getType() == ActionType.MESSAGE && !reversed) {
			ChatData message = (ChatData) action.getPacketData();
			replayer.sendMessage(message.getMessage());
		}
		
		if (action.getType() == ActionType.PACKET && this.replayer.getNPCList().containsKey(action.getName())) {
			INPC npc = this.replayer.getNPCList().get(action.getName());

			
			if (action.getPacketData() instanceof MovingData) {
				MovingData movingData = (MovingData) action.getPacketData();
				
				if (VersionUtil.isAbove(VersionEnum.V1_15) || VersionUtil.isCompatible(VersionEnum.V1_8)) {
					Location loc = new Location(npc.getOrigin().getWorld(), movingData.getX(), movingData.getY(), movingData.getZ());

					npc.move(loc, true, movingData.getYaw(), movingData.getPitch());

					loc.setPitch(movingData.getPitch());
					loc.setYaw(movingData.getYaw());
					this.replayer.getSession().getPacketListener().updateLocationIfNeeded(npc.getId(), npc.getVisible(), loc);
				}
				
				if (VersionUtil.isBetween(VersionEnum.V1_9, VersionEnum.V1_14)) {
					Location loc = new Location(npc.getOrigin().getWorld(), movingData.getX(), movingData.getY(), movingData.getZ());

					npc.teleport(loc, true);
					npc.look(movingData.getYaw(), movingData.getPitch());

					loc.setPitch(movingData.getPitch());
					loc.setYaw(movingData.getYaw());
					this.replayer.getSession().getPacketListener().updateLocationIfNeeded(npc.getId(), npc.getVisible(), loc);
				}
		
			}
			
			if (action.getPacketData() instanceof EntityActionData) {
				EntityActionData eaData = (EntityActionData) action.getPacketData();
				if (eaData.getAction() == PlayerAction.START_SNEAKING) {
					data.getWatcher(action.getName()).setSneaking(reversed ? false : true);

					npc.setData(data.getWatcher(action.getName()).getMetadata(new MetadataBuilder(npc.getData())));
				} else if (eaData.getAction() == PlayerAction.STOP_SNEAKING) {
					data.getWatcher(action.getName()).setSneaking(reversed);
					npc.setData(data.getWatcher(action.getName()).getMetadata(new MetadataBuilder(npc.getData())));
				}
				npc.updateMetadata();
				
				
			}
			
			if (action.getPacketData() instanceof AnimationData) {
				AnimationData animationData = (AnimationData) action.getPacketData();
				npc.animate(animationData.getId());
				
				if (animationData.getId() == 1 && !VersionUtil.isCompatible(VersionEnum.V1_8)) {
					replayer.getWatchingPlayer().playSound(npc.getLocation(), Sound.ENTITY_PLAYER_HURT, 5F, 5.0F);
				}
			}

			if (action.getPacketData() instanceof ChatData) {
				ChatData chatData = (ChatData) action.getPacketData();

				replayer.sendMessage(new MessageBuilder(ConfigManager.CHAT_FORMAT)
						.set("name", action.getName())
						.set("message", chatData.getMessage())
						.build());
			}

			if (action.getPacketData() instanceof InvData) {
				InvData invData = (InvData) action.getPacketData();
				
				if (!VersionUtil.isCompatible(VersionEnum.V1_8)) {
			
					List<WrapperPlayServerEntityEquipment> equipment = VersionUtil.isBelow(VersionEnum.V1_15) ? NPCManager.updateEquipment(npc.getId(), invData) : NPCManager.updateEquipmentv16(npc.getId(), invData);
					npc.setLastEquipment(equipment);
					
					for (WrapperPlayServerEntityEquipment packet : equipment) {
						packet.sendPacket(replayer.getWatchingPlayer());
					}
				} else {
					List<com.comphenix.packetwrapper.old.WrapperPlayServerEntityEquipment> equipment = NPCManager.updateEquipmentOld(npc.getId(), invData);
					PacketNPCOld oldNPC = (PacketNPCOld) npc;
					oldNPC.setLastEquipmentOld(equipment);
					
					for (com.comphenix.packetwrapper.old.WrapperPlayServerEntityEquipment packet : equipment) {
						packet.sendPacket(replayer.getWatchingPlayer());
					}
				}
			}
			
			if (action.getPacketData() instanceof MetadataUpdate) {
				MetadataUpdate update = (MetadataUpdate) action.getPacketData();

				data.getWatcher(action.getName()).setBurning(!reversed ? update.isBurning() : false);
				data.getWatcher(action.getName()).setBlocking(!reversed ? update.isBlocking() : false);
				data.getWatcher(action.getName()).setElytra(!reversed ? update.isGliding() : false);
				
				WrappedDataWatcher dataWatcher = data.getWatcher(action.getName()).getMetadata(new MetadataBuilder(npc.getData()));
				npc.setData(dataWatcher);
				
				npc.updateMetadata();
				
			
			}
			
			if (action.getPacketData() instanceof ProjectileData) {
				ProjectileData projectile = (ProjectileData) action.getPacketData();
				
				spawnProjectile(projectile, null, replayer.getWatchingPlayer().getWorld(), 0);
			}
			
			if (action.getPacketData() instanceof BlockChangeData) {
				BlockChangeData blockChange = (BlockChangeData) action.getPacketData();
				
				if (reversed) {
					blockChange = new BlockChangeData(blockChange.getLocation(), blockChange.getAfter(), blockChange.getBefore());
				}
				
				setBlockChange(blockChange);
			}
			
			if (action.getPacketData() instanceof BedEnterData) {
				BedEnterData bed = (BedEnterData) action.getPacketData();
				
				if (VersionUtil.isAbove(VersionEnum.V1_14)) {
					npc.teleport(LocationData.toLocation(bed.getLocation()), true);
					
					npc.setData(new MetadataBuilder(npc.getData())
							.setPoseField("SLEEPING")
							.getData());
					
					npc.updateMetadata();
					npc.teleport(LocationData.toLocation(bed.getLocation()), true);

					
				} else {
					npc.sleep(LocationData.toLocation(bed.getLocation()));
				}
			}
			
			if (action.getPacketData() instanceof EntityItemData) {
				EntityItemData entityData = (EntityItemData) action.getPacketData();

				if (entityData.getAction() == 0 && !reversed) {
					spawnItemStack(entityData);
				} else if (entityData.getAction() == 1){
					if (itemEntities.containsKey(entityData.getId())) {
						despawn(Arrays.asList(new Entity[] { itemEntities.get(entityData.getId()) }), null);
						
						itemEntities.remove(entityData.getId());
					}
				} else {
					if (hooks.containsKey(entityData.getId())) {
						despawn(null, new int[] { hooks.get(entityData.getId()) });

						hooks.remove(entityData.getId());
					}
				}
			}
			
			if (action.getPacketData() instanceof EntityData) {
				EntityData entityData = (EntityData) action.getPacketData();

				if (entityData.getAction() == 0) {
					if (!reversed) {
					IEntity entity = VersionUtil.isCompatible(VersionEnum.V1_8) ? new PacketEntityOld(EntityType.valueOf(entityData.getType()))  : new PacketEntity(EntityType.valueOf(entityData.getType()));
					entity.spawn(LocationData.toLocation(entityData.getLocation()), this.replayer.getWatchingPlayer());
					replayer.getEntityList().put(entityData.getId(), entity);
					} else if (replayer.getEntityList().containsKey(entityData.getId())){
						IEntity ent = replayer.getEntityList().get(entityData.getId());
						ent.remove();

					}
					
				} else if (entityData.getAction() == 1) {
					if (!reversed && replayer.getEntityList().containsKey(entityData.getId())) {
					IEntity ent = replayer.getEntityList().get(entityData.getId());
					ent.remove();
					} else {
						IEntity entity = VersionUtil.isCompatible(VersionEnum.V1_8) ? new PacketEntityOld(EntityType.valueOf(entityData.getType()))  : new PacketEntity(EntityType.valueOf(entityData.getType()));
						entity.spawn(LocationData.toLocation(entityData.getLocation()), this.replayer.getWatchingPlayer());
						replayer.getEntityList().put(entityData.getId(), entity);
					}
				}
			}
			
			if (action.getPacketData() instanceof EntityMovingData) {
				EntityMovingData entityMoving = (EntityMovingData) action.getPacketData();
				if (replayer.getEntityList().containsKey(entityMoving.getId())) {
					IEntity ent = replayer.getEntityList().get(entityMoving.getId());
					
					if (VersionUtil.isAbove(VersionEnum.V1_15) || VersionUtil.isCompatible(VersionEnum.V1_8)) {
						ent.move(new Location(ent.getOrigin().getWorld(), entityMoving.getX(), entityMoving.getY(), entityMoving.getZ()), true, entityMoving.getYaw(), entityMoving.getPitch());
					}
					
					if (VersionUtil.isBetween(VersionEnum.V1_9, VersionEnum.V1_14)) {
						ent.teleport(new Location(ent.getOrigin().getWorld(), entityMoving.getX(), entityMoving.getY(), entityMoving.getZ()), true);
						ent.look(entityMoving.getYaw(), entityMoving.getPitch());
					}
		
				}
			}
			
			if (action.getPacketData() instanceof EntityAnimationData) {
				EntityAnimationData entityAnimating = (EntityAnimationData) action.getPacketData();
				if (replayer.getEntityList().containsKey(entityAnimating.getEntId()) && !reversed) {

					IEntity ent = replayer.getEntityList().get(entityAnimating.getEntId());
					ent.animate(entityAnimating.getId());
				}
			}
			
			if (action.getPacketData() instanceof WorldChangeData) {
				WorldChangeData worldChange = (WorldChangeData) action.getPacketData();
				Location loc = LocationData.toLocation(worldChange.getLocation());

				this.replayer.getSession().getPacketListener().ignoreNextDespawn(replayer.getWatchingPlayer());
				
				npc.despawn();
				npc.setOrigin(loc);
				npc.setLocation(loc);
				
				npc.respawn(replayer.getWatchingPlayer());

				this.replayer.getSession().getPacketListener().goToNewLocation(npc.getId(), replayer.getWatchingPlayer(), loc);
				
			}
			
			if (action.getPacketData() instanceof FishingData) {
				FishingData fishing = (FishingData) action.getPacketData();
				spawnProjectile(null, fishing, replayer.getWatchingPlayer().getWorld(), npc.getId());
				
			}
			
			if (action.getPacketData() instanceof VelocityData) {
				VelocityData velocity = (VelocityData) action.getPacketData();
				int entID = -1;
				if (hooks.containsKey(velocity.getId())) entID = hooks.get(velocity.getId());
				if (replayer.getEntityList().containsKey(velocity.getId())) entID = replayer.getEntityList().get(velocity.getId()).getId();

				if (entID != -1) {
					WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity();
					packet.setEntityID(entID);
					packet.setVelocityX(velocity.getX());
					packet.setVelocityY(velocity.getY());
					packet.setVelocityZ(velocity.getZ());
					
					packet.sendPacket(replayer.getWatchingPlayer());
				}

			}

			if (action.getPacketData() instanceof SoundEffectData) {
				SoundEffectData soundEffect = (SoundEffectData) action.getPacketData();


				WrapperPlayServerNamedSoundEffect packet = new WrapperPlayServerNamedSoundEffect();
				packet.setSoundEffect(soundEffect.getSoundEffect());
				packet.setSoundCategory(SoundCategory.getByKey(soundEffect.getSoundCategory()));
				packet.setEffectPositionX(soundEffect.getEffectX());
				packet.setEffectPositionY(soundEffect.getEffectY());
				packet.setEffectPositionZ(soundEffect.getEffectZ());
				packet.setVolume(soundEffect.getVolume());
				packet.setPitch(soundEffect.getPitch());
				
				packet.sendPacket(replayer.getWatchingPlayer());

			}

			if (action.getPacketData() instanceof WorldEventData) {
				WorldEventData worldEvent = (WorldEventData) action.getPacketData();


				WrapperPlayServerWorldEvent packet = new WrapperPlayServerWorldEvent();
				packet.setEffectId(worldEvent.getEffectId());
				packet.setLocation(worldEvent.getLocation());
				packet.setData(worldEvent.getData());
				packet.setDisableRelativeVolume(worldEvent.getDisableRelativeVolume());
				
				packet.sendPacket(replayer.getWatchingPlayer());

			}
			

		}
		
		if (action.getType() == ActionType.DESPAWN || action.getType() == ActionType.DEATH) {
			if (!reversed  && replayer.getNPCList().containsKey(action.getName())) {
				INPC npc = this.replayer.getNPCList().get(action.getName());
				npc.remove();
				replayer.getNPCList().remove(action.getName());
				
				SpawnData oldSpawnData = new SpawnData(npc.getUuid(), LocationData.fromLocation(npc.getLocation()), signatures.get(action.getName()));
				this.lastSpawnActions.addLast(new ActionData(0, ActionType.SPAWN, action.getName(), oldSpawnData));
				
				if (action.getType() == ActionType.DESPAWN) {
					replayer.sendMessage(new MessageBuilder(ConfigManager.LEAVE_MESSAGE)
							.set("name", action.getName())
							.build());
				} else {
					replayer.sendMessage(new MessageBuilder(ConfigManager.DEATH_MESSAGE)
							.set("name", action.getName())
							.build());
				}
				
			} else {

				if (!this.lastSpawnActions.isEmpty()) {
					spawnNPC(this.lastSpawnActions.pollLast());
				}
				
			}

		}
	}
	
	public void forward() {
		this.replayer.setPaused(true);
		int currentTick = this.replayer.getCurrentTicks();
		int forwardTicks = currentTick + (10 * 20);
		int duration = this.replayer.getReplay().getData().getDuration();

		if ((forwardTicks + 2) >= duration) {
			forwardTicks = duration - 20;
		}

		for (int i = currentTick; i < forwardTicks; i++) {
			this.replayer.executeTick(i, false);
		}
		this.replayer.setCurrentTicks(forwardTicks);
		this.replayer.setPaused(false);
	}
	
	public void backward() {
		this.replayer.setPaused(true);
		int currentTick = this.replayer.getCurrentTicks();
		int backwardTicks = currentTick - (10 * 20);

		if ((backwardTicks - 2) <= 0) {
			backwardTicks = 1;
		}

		for (int i = currentTick; i > backwardTicks; i--) {
			this.replayer.executeTick(i, true);
		}
		this.replayer.setCurrentTicks(backwardTicks);
		this.replayer.setPaused(false);
	}

	public void jumpTo(Integer seconds) {
		int targetTicks = (seconds * 20);
		int currentTick = replayer.getCurrentTicks();
		if (currentTick > targetTicks) {
			this.replayer.setPaused(true);

			if ((targetTicks - 2) > 0) {
				for (int i = currentTick; i > targetTicks; i--) {
					this.replayer.executeTick(i, true);
				}

				this.replayer.setCurrentTicks(targetTicks);
				this.replayer.setPaused(false);
			}
		} else if (currentTick < targetTicks) {
			this.replayer.setPaused(true);
			int duration = replayer.getReplay().getData().getDuration();

			if ((targetTicks + 2) < duration) {
				for (int i = currentTick; i < targetTicks; i++) {
					this.replayer.executeTick(i, false);
				}
				this.replayer.setCurrentTicks(targetTicks);
				this.replayer.setPaused(false);
			}
		}
	}
	
	private void spawnNPC(ActionData action) {
		SpawnData spawnData = (SpawnData)action.getPacketData();
		
		int tabMode = Bukkit.getPlayer(action.getName()) != null ? 0 : 2;
		
		if (VersionUtil.isAbove(VersionEnum.V1_17) && Bukkit.getPlayer(action.getName()) != null) {
			tabMode = 2;
			spawnData.setUuid(UUID.randomUUID());
		}

		INPC npc = !VersionUtil.isCompatible(VersionEnum.V1_8) ? new PacketNPC(MathUtils.randInt(10000, 20000), spawnData.getUuid(), action.getName()) : new PacketNPCOld(MathUtils.randInt(10000, 20000), spawnData.getUuid(), action.getName());
		this.replayer.getNPCList().put(action.getName(), npc);
		this.replayer.getReplay().getData().getWatchers().put(action.getName(), new PlayerWatcher(action.getName()));

		Location spawn = LocationData.toLocation(spawnData.getLocation());
		
		if(VersionUtil.isCompatible(VersionEnum.V1_8)) {
			npc.setData(new MetadataBuilder(this.replayer.getWatchingPlayer()).resetValue().getData());
		} else {
			npc.setData(new MetadataBuilder(this.replayer.getWatchingPlayer()).setArrows(0).resetValue().getData());

		}
		
		if (ConfigManager.HIDE_PLAYERS && !action.getName().equals(this.replayer.getWatchingPlayer().getName())) {
			tabMode = 2;
		}
		
		if ((spawnData.getSignature() != null && (Bukkit.getPlayer(action.getName()) == null || VersionUtil.isAbove(VersionEnum.V1_17))) || (spawnData.getSignature() != null && ConfigManager.HIDE_PLAYERS && !action.getName().equals(this.replayer.getWatchingPlayer().getName()))) {
			WrappedGameProfile profile = new WrappedGameProfile(spawnData.getUuid(), action.getName());
			WrappedSignedProperty signed = new WrappedSignedProperty(spawnData.getSignature().getName(), spawnData.getSignature().getValue(), spawnData.getSignature().getSignature());
			profile.getProperties().put(spawnData.getSignature().getName(), signed);
			npc.setProfile(profile);
			
			if (!this.signatures.containsKey(action.getName())) {
				this.signatures.put(action.getName(), spawnData.getSignature());
			}
		}

		npc.spawn(spawn, tabMode, this.replayer.getWatchingPlayer());
		npc.look(spawnData.getLocation().getYaw(), spawnData.getLocation().getPitch());	  
	}
	
	private void spawnProjectile(ProjectileData projData, FishingData fishing, World world, int id) {
		if (projData != null && projData.getType() != EntityType.FISHING_HOOK) {
			
			if (projData.getType() == EntityType.ENDER_PEARL && VersionUtil.isCompatible(VersionEnum.V1_8)) return;
			
			new BukkitRunnable() {
			
				@Override
				public void run() {
					Projectile proj = (Projectile) world.spawnEntity(LocationData.toLocation(projData.getSpawn()), projData.getType());
					proj.setVelocity(LocationData.toLocation(projData.getVelocity()).toVector());
				
				}
			}.runTask(ReplaySystem.getInstance());
		} 
		
		if (fishing != null) {			
			int rndID = MathUtils.randInt(2000, 30000);
			AbstractPacket packet = VersionUtil.isCompatible(VersionEnum.V1_8) ? FishingUtils.createHookPacketOld(fishing, id, rndID) : FishingUtils.createHookPacket(fishing, id, rndID);	
			
			hooks.put(fishing.getId(), rndID);
			packet.sendPacket(replayer.getWatchingPlayer());
		}
	}
	
	private void setBlockChange(BlockChangeData blockChange) {
		final Location loc = LocationData.toLocation(blockChange.getLocation());
		
		if (ConfigManager.WORLD_RESET && ! this.replayer.getBlockChanges().containsKey(loc)) {
			this.replayer.getBlockChanges().put(loc, blockChange.getBefore());
		}
		
		
		new BukkitRunnable() {
			
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				if (blockChange.getAfter().getId() == Material.AIR && blockChange.getBefore().getId() != Material.AIR && blockChange.getBefore().getId() != Material.FIRE && blockChange.getBefore().getId() != Material.LAVA && blockChange.getBefore().getId() != Material.WATER) {
					loc.getWorld().playEffect(loc, Effect.STEP_SOUND, blockChange.getBefore().getId(), 15);
					
				}
				Material id = blockChange.getAfter().getId();
				int subId = blockChange.getAfter().getSubId();
				BlockFace facing = blockChange.getAfter().getFacing();

				if (ConfigManager.REAL_CHANGES) {
					boolean isDoor = id == XMaterial.OAK_DOOR.parseMaterial() ||
						id == XMaterial.IRON_DOOR.parseMaterial() ||
						id == XMaterial.DARK_OAK_DOOR.parseMaterial() || 
						id == XMaterial.ACACIA_DOOR.parseMaterial() || 
						id == XMaterial.BIRCH_DOOR.parseMaterial() || 
						id == XMaterial.JUNGLE_DOOR.parseMaterial() || 
						id == XMaterial.SPRUCE_DOOR.parseMaterial() || 
						id == XMaterial.CRIMSON_DOOR.parseMaterial() || 
						id == XMaterial.WARPED_DOOR.parseMaterial();
					boolean isBed = id.name().contains("_BED");
					
					boolean applyPhysics = !(id == XMaterial.NETHER_PORTAL.parseMaterial() || id == Material.OBSIDIAN || id == Material.OBSIDIAN || isDoor || isBed);

					Material previousId = loc.getBlock().getType();

					if (previousId != id && !isBed) {
						loc.getBlock().setType(id, applyPhysics);
						loc.getBlock().getState().setRawData((byte) subId);
						XBlock.setOrient(loc.getBlock(), subId);
					}

					Material obsidianMaterial = XMaterial.OBSIDIAN.parseMaterial();
					if (id == XMaterial.NETHER_PORTAL.parseMaterial()){
						// Partially fixes a quirk in nether portal creation
						if (loc.clone().add(0, -1, 1).getBlock().getType() == obsidianMaterial || loc.clone().add(0, -1, -1).getBlock().getType() == obsidianMaterial) {
							XBlock.setOrient(loc.getBlock(), 2);
						}
					}

					if (facing != null) {
						XBlock.setDirection(loc.getBlock(), facing);
					}

					if (id == XMaterial.END_PORTAL_FRAME.parseMaterial() && subId > 3) {
						XBlock.setEnderPearlOnFrame(loc.getBlock(), true);
					}

					if (isDoor && loc.clone().add(0, -1, 0).getBlock().getType() != id) {
						Block topDoor = loc.clone().add(0, 1, 0).getBlock();
						topDoor.setType(id, false);
						XBlock.setDoorTop(topDoor, true);
						XBlock.setDirection(topDoor, facing);
					}

					if (isBed) {
						if (previousId != id) {
							XBlock.setBed(loc.getBlock(), facing, id);
							lastFacing = facing;
						} else {
							XBlock.setBed(loc.getBlock().getRelative(lastFacing), lastFacing, id);
						}
					}
				} else {
					if (VersionUtil.isCompatible(VersionEnum.V1_13) || VersionUtil.isCompatible(VersionEnum.V1_14) || VersionUtil.isCompatible(VersionEnum.V1_15) || VersionUtil.isCompatible(VersionEnum.V1_16) || VersionUtil.isCompatible(VersionEnum.V1_17)) {
						replayer.getWatchingPlayer().sendBlockChange(loc, getBlockMaterial(blockChange.getAfter()), (byte) subId);
					} else {
						replayer.getWatchingPlayer().sendBlockChange(loc, id.createBlockData());
					}
				}
				
				
			}
		}.runTask(ReplaySystem.getInstance());
	}
	
	private Material getBlockMaterial(ItemData data) {
		return data.getId();
	}
	
	private void spawnItemStack(EntityItemData entityData) {
		final Location loc = LocationData.toLocation(entityData.getLocation());
		
		new BukkitRunnable() {
			
			@Override
			public void run() {
				Item item = loc.getWorld().dropItemNaturally(loc, NPCManager.fromID(entityData.getItemData()));
				item.setVelocity(LocationData.toLocation(entityData.getVelocity()).toVector());
				
				itemEntities.put(entityData.getId(), item);
				
			}
		}.runTask(ReplaySystem.getInstance());
	}
	
	public void despawn(List<Entity> entities, int[] ids) {
		
		if (entities != null && entities.size() > 0) {
			new BukkitRunnable() {
			
				@Override
				public void run() {
					for (Entity en : entities) {
						if (en != null) en.remove();
					}
				}
			}.runTask(ReplaySystem.getInstance());
		}
		
		if (ids != null && ids.length > 0) {
			WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy();
			if (VersionUtil.isAbove(VersionEnum.V1_17)) {
				packet.getHandle().getIntLists().write(0, IntStream.of(ids).boxed().collect(Collectors.toList()));
			} else {
				packet.setEntityIds(ids);
			}
			
			
			packet.sendPacket(replayer.getWatchingPlayer());
		}
	}
	
	public void resetChanges(Map<Location, ItemData> changes) {
		if (!Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTask(ReplaySystem.getInstance(), () -> setBlocks(changes));
		} else {
			setBlocks(changes);
		}

	}
	
	@SuppressWarnings("deprecation")
	private void setBlocks(Map<Location, ItemData> changes) {
		changes.forEach((location, itemData) -> {
			if (VersionUtil.isAbove(VersionEnum.V1_13)) {
				location.getBlock().setType(getBlockMaterial(itemData));
			} else {
				location.getBlock().setType(itemData.getId(), false);
				location.getBlock().getState().setRawData((byte) itemData.getSubId());
			}
		});
	}
	
	public HashMap<Integer, Entity> getEntities() {
		return itemEntities;
	}
}
