package me.jumper251.replay.replaysystem.data.types;

import org.bukkit.Sound;

public class SoundEffectData extends PacketData{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1984318917396292472L;

	
	Sound soundEffect;
	String soundCategory;
	int effectX;
	int effectY;
	int effectZ;
	float volume;
	float pitch;
	
	public SoundEffectData(Sound soundEffect, String soundCategory, int effectX, int effectY, int effectZ, float volume, float pitch) {
		this.soundEffect = soundEffect;
		this.soundCategory = soundCategory;
		this.effectX = effectX;
		this.effectY = effectY;
		this.effectZ = effectZ;
		this.volume = volume;
		this.pitch = pitch;
	}
	
	public Sound getSoundEffect() {
		return soundEffect;
	}
	
	public String getSoundCategory() {
		return soundCategory;
	}

	public int getEffectX() {
		return effectX;
	}
	
	public int getEffectY() {
		return effectY;
	}

	public int getEffectZ() {
		return effectZ;
	}

	public float getVolume() {
		return volume;
	}

	public float getPitch() {
		return pitch;
	}

}
