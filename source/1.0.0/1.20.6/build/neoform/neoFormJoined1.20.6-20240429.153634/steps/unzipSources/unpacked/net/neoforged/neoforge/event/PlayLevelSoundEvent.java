/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event;

import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * PlayLevelSoundEvent is fired when a sound is played on a {@link Level}.
 * This event is fired from {@link Level#playSound}, {@link Level#playSeededSound}, and {@link LocalPlayer#playSound}.
 * <p>
 * {@link #getLevel()} contains the level the sound is being played in.
 * {@link #getSound()} contains the sound event to be played.
 * {@link #getOriginalVolume()} contains the original volume for the sound to be played at.
 * {@link #getOriginalPitch()} contains the original pitch for the sound to be played at.
 * {@link #getNewVolume()} contains the volume the sound will be played at.
 * {@link #getNewPitch()} contains the pitch the sound will be played at.
 * <p>
 * This event is {@link ICancellableEvent cancelable}.
 * If this event is canceled, the sound is not played.
 * <p>
 * This event does not have a result.
 * <p>
 * This event is fired on the {@link NeoForge#EVENT_BUS}.
 */
public class PlayLevelSoundEvent extends Event implements ICancellableEvent {
    private final Level level;
    private final float originalVolume;
    private final float originalPitch;
    private Holder<SoundEvent> sound;
    private SoundSource source;
    private float newVolume;
    private float newPitch;

    public PlayLevelSoundEvent(Level level, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
        this.level = level;
        this.sound = sound;
        this.source = source;
        this.originalVolume = volume;
        this.originalPitch = pitch;
        this.newVolume = volume;
        this.newPitch = pitch;
    }

    /**
     * {@return the level the sound is being played in}
     */
    public Level getLevel() {
        return this.level;
    }

    /**
     * {@return the sound event to be played}
     */
    @Nullable
    public Holder<SoundEvent> getSound() {
        return this.sound;
    }

    /**
     * Sets the sound event to be played.
     */
    public void setSound(@Nullable Holder<SoundEvent> sound) {
        this.sound = sound;
    }

    /**
     * {@return the sound source}
     */
    public SoundSource getSource() {
        return this.source;
    }

    /**
     * Sets the sound source.
     */
    public void setSource(SoundSource source) {
        Objects.requireNonNull(source, "Sound source cannot be null");
        this.source = source;
    }

    /**
     * {@return the original volume for the sound to be played at}
     */
    public float getOriginalVolume() {
        return this.originalVolume;
    }

    /**
     * {@return the original pitch for the sound to be played at}
     */
    public float getOriginalPitch() {
        return this.originalPitch;
    }

    /**
     * {@return the volume the sound will be played at}
     */
    public float getNewVolume() {
        return this.newVolume;
    }

    /**
     * Sets the volume the sound will be played at.
     */
    public void setNewVolume(float newVolume) {
        this.newVolume = newVolume;
    }

    /**
     * {@return the pitch the sound will be played at}
     */
    public float getNewPitch() {
        return this.newPitch;
    }

    /**
     * Sets the pitch the sound will be played at.
     */
    public void setNewPitch(float newPitch) {
        this.newPitch = newPitch;
    }

    /**
     * PlayLevelSoundEvent.AtEntity is fired when a sound is played on the {@link Level} at an {@link Entity Entity}'s position.
     * This event is fired from {@link Level#playSound}, {@link Level#playSeededSound}, and {@link LocalPlayer#playSound}.
     * <p>
     * This event is {@link ICancellableEvent cancelable}.
     * If this event is canceled, the sound is not played.
     * <p>
     * This event does not have a result.
     * <p>
     * This event is fired on the {@link NeoForge#EVENT_BUS}.
     */
    public static class AtEntity extends PlayLevelSoundEvent {
        private final Entity entity;

        public AtEntity(Entity entity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
            super(entity.level(), sound, source, volume, pitch);
            this.entity = entity;
        }

        /**
         * {@return the entity the sound is being played on}
         */
        public Entity getEntity() {
            return this.entity;
        }
    }

    /**
     * PlayLevelSoundEvent.AtPosition is fired when a sound is played on the {@link Level} at a specific position.
     * This event is fired from {@link Level#playSound} and {@link Level#playSeededSound}.
     * <p>
     * This event is {@link ICancellableEvent cancelable}.
     * If this event is canceled, the sound is not played.
     * <p>
     * This event does not have a result.
     * <p>
     * This event is fired on the {@link NeoForge#EVENT_BUS}.
     */
    public static class AtPosition extends PlayLevelSoundEvent {
        private final Vec3 position;

        public AtPosition(Level level, Vec3 position, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch) {
            super(level, sound, source, volume, pitch);
            this.position = position;
        }

        /**
         * {@return the position the sound is being played at}
         */
        public Vec3 getPosition() {
            return this.position;
        }
    }
}
