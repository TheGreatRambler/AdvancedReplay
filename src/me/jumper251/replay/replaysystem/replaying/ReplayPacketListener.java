package me.jumper251.replay.replaysystem.replaying;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.packetwrapper.WrapperPlayClientUseEntity;
import com.comphenix.packetwrapper.WrapperPlayServerCamera;
import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerGameStateChange;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;

import com.cryptomorin.xseries.XMaterial;

import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.listener.AbstractListener;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;

public class ReplayPacketListener extends AbstractListener {
	
	private PacketAdapter packetAdapter;
	
	private Replayer replayer;
	
	private int previous;
	
	private HashMap<Player, Integer> spectating;
	private HashMap<Player, Location> lastLoc;
	private HashSet<Player> ignoreNextDespawnForSpectate;
	private HashSet<Player> worldChangeHappening;
	
	public ReplayPacketListener(Replayer replayer) {
		this.replayer = replayer;
		this.spectating = new HashMap<Player, Integer>();
		this.lastLoc = new HashMap<Player, Location>();
		this.ignoreNextDespawnForSpectate = new HashSet<Player>();
		this.worldChangeHappening = new HashSet<Player>();
		this.previous = -1;
		
		if (!isRegistered()) register();
	}

	@Override
	public void register() {
		this.packetAdapter = new PacketAdapter(ReplaySystem.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Client.USE_ENTITY, PacketType.Play.Server.ENTITY_DESTROY) {
			
			@SuppressWarnings("deprecation")
			@Override
			public void onPacketReceiving(PacketEvent event) {
				WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
				Player p = event.getPlayer();
				
				if (packet.getType() == EntityUseAction.ATTACK && ReplayHelper.replaySessions.containsKey(p.getName()) && replayer.getNPCList().values().stream().anyMatch(ent -> packet.getTargetID() == ent.getId())) {
					if (p.getGameMode() != GameMode.SPECTATOR) previous = p.getGameMode().getValue();
					setCamera(p, packet.getTargetID(), 2F, true);
					
				}
			}
			
			@Override
			public void onPacketSending(PacketEvent event) {
				Player p = event.getPlayer();
				if (event.getPacketType() == PacketType.Play.Server.ENTITY_DESTROY) {
					WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy(event.getPacket());
					
					if (ReplayHelper.replaySessions.containsKey(p.getName()) && isSpectating(p)) {
						
						List<Integer> entityIds;
						if (VersionUtil.isAbove(VersionEnum.V1_17)) {
							entityIds = packet.getHandle().getIntLists().read(0);
							
						} else {
							entityIds = IntStream.of(packet.getEntityIDs()).boxed().collect(Collectors.toList());
						}
						
						for (int id : entityIds) {
							if (id == spectating.get(p)) {
								setCamera(p, p.getEntityId(), previous, false);

								fixSpectatingLocation(p);
							}
						}
					}
				}
			}
			
		};
		
	    ProtocolLibrary.getProtocolManager().addPacketListener(this.packetAdapter);
		Bukkit.getPluginManager().registerEvents(this, ReplaySystem.getInstance());
	}
	
	@Override
	public void unregister() {
		ProtocolLibrary.getProtocolManager().removePacketListener(this.packetAdapter);
		HandlerList.unregisterAll(this);
	}
	
	public boolean isRegistered() {
		return this.packetAdapter != null;
	}
	
	public int getPrevious() {
		return previous;
	}
	
	public boolean isSpectating(Player p) {
		return this.spectating.containsKey(p);
	}

	public void ignoreNextDespawn(Player p) {
		ignoreNextDespawnForSpectate.add(p);
	}

	public void goToNewLocation(int id, Player p, Location loc) {
		if (spectating.containsKey(p)) {
			spectating.put(p, id);
			worldChangeHappening.add(p);

			new BukkitRunnable() {
				@Override
				public void run() {
					p.teleport(loc);
				}
			}.runTask(ReplaySystem.getInstance());

			new BukkitRunnable() {
				@Override
				public void run() {
					setCamera(p, id, 2F, true);
				}
			}.runTaskLater(ReplaySystem.getInstance(), 3L);
		}
	}

	public void updateLocationIfNeeded(int id, Player[] players, Location loc) {
		for (Player p : players) {
			if (spectating.containsKey(p) && id == spectating.get(p)) {
				this.lastLoc.put(p, loc);

				if (loc.getWorld() == p.getWorld()) {
					new BukkitRunnable() {
						@Override
						public void run() {
							TeleportUtils.teleport(p, loc);
							setCamera(p, id, 2F, true);
						}
					}.runTask(ReplaySystem.getInstance());
				}
			}

			// Entities appear to spawn by default in Location{world=CraftWorld{name=0_nether},x=8.5,y=65.0,z=8.5,pitch=0.0,yaw=-180.0}
			// Ignore that
			if (worldChangeHappening.contains(p) && loc.getWorld() == p.getWorld() && loc.getX() != 8.5 && loc.getZ() != 8.5) {
				this.lastLoc.put(p, loc);

				new BukkitRunnable() {
					@Override
					public void run() {
						TeleportUtils.teleport(p, loc);
						setCamera(p, id, 2F, true);
					}
				}.runTaskLater(ReplaySystem.getInstance(), 3L);
			}

			Material matHere = loc.getBlock().getType();
			boolean isPortal = matHere == XMaterial.NETHER_PORTAL.parseMaterial()
				|| matHere == XMaterial.END_PORTAL.parseMaterial();
			if (worldChangeHappening.contains(p) && loc.getWorld() == p.getWorld() && !isPortal && loc.getX() != 8.5 && loc.getZ() != 8.5) {
				new BukkitRunnable() {
					@Override
					public void run() {
						TeleportUtils.teleport(p, loc);
						setCamera(p, id, 2F, true);
						worldChangeHappening.remove(p);
					}
				}.runTask(ReplaySystem.getInstance());
			}
		}
	}

	public void fixSpectatingLocation(Player p) {
		Location loc = this.lastLoc.get(p);

		if (loc != null) {
			p.teleport(loc);
			this.lastLoc.put(p, null);
		}
	}
	
	public void setCamera(Player p, int entityID, float gamemode, boolean enable) {
		WrapperPlayServerCamera camera = new WrapperPlayServerCamera();
		camera.setCameraId(entityID);
		
		WrapperPlayServerGameStateChange state = new WrapperPlayServerGameStateChange();
		
		if (VersionUtil.isAbove(VersionEnum.V1_16)) {
			state.getHandle().getGameStateIDs().write(0, 3);
		} else {
			state.setReason(3);
		}
		
		state.setValue(gamemode < 0 ? 0 : gamemode);
		
		state.sendPacket(p);
		camera.sendPacket(p);
		
		if (!enable && this.spectating.containsKey(p)) {
			if (ignoreNextDespawnForSpectate.contains(p)) {
				ignoreNextDespawnForSpectate.remove(p);
			} else {
				this.spectating.remove(p);
			}
		}

		if (enable) {
			this.spectating.put(p, entityID);
		}
	}

	@EventHandler
	void onTNTBeginExplode(ExplosionPrimeEvent event) {
		if(event.getEntityType() == EntityType.PRIMED_TNT) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onPlayerTeleport(PlayerTeleportEvent event) {
		if(event.getCause() == TeleportCause.NETHER_PORTAL || event.getCause() == TeleportCause.END_PORTAL) {
			if (worldChangeHappening.contains(event.getPlayer())) {
				event.setCancelled(true);
			}
		}
	}
}
