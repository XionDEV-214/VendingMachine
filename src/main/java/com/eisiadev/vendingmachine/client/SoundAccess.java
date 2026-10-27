package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.VendingMachine;

import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.resources.sounds.SoundInstance;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

final class SoundAccess {

    private static boolean initialised = false;
    private static boolean broken = false;

    private static Field musicManagerCurrentMusic;
    private static Field soundManagerEngine;
    private static Field engineChannels;

    private SoundAccess() {}

    private static boolean init() {
        if (initialised) return !broken;
        initialised = true;
        try {
            musicManagerCurrentMusic = findField(MusicManager.class, SoundInstance.class);
            soundManagerEngine = findField(SoundManager.class, SoundEngine.class);
            for (Field f : SoundEngine.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType() != Map.class) continue;
                Type type = f.getGenericType();
                if (type instanceof ParameterizedType pt) {
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length == 2 && args[0] == SoundInstance.class && args[1] == ChannelAccess.ChannelHandle.class) {
                        f.setAccessible(true);
                        engineChannels = f;
                        break;
                    }
                }
            }
            if (engineChannels == null) throw new NoSuchFieldException("SoundEngine instance -> channel map");
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            VendingMachine.LOG.warn("Cannot fade the game music while the vending machine is open: {}", e.toString());
        }
        return !broken;
    }

    private static Field findField(Class<?> owner, Class<?> type) throws NoSuchFieldException {
        for (Field f : owner.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == type) {
                f.setAccessible(true);
                return f;
            }
        }
        throw new NoSuchFieldException(owner.getSimpleName() + " field of type " + type.getSimpleName());
    }

    @Nullable
    static SoundInstance currentMusic(MusicManager manager) {
        if (!init()) return null;
        try {
            return (SoundInstance) musicManagerCurrentMusic.get(manager);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    static boolean setVolume(SoundManager manager, SoundInstance sound, float volume) {
        if (!init()) return false;
        try {
            SoundEngine engine = (SoundEngine) soundManagerEngine.get(manager);
            Map<?, ?> channels = (Map<?, ?>) engineChannels.get(engine);
            Object handle = channels.get(sound);
            if (!(handle instanceof ChannelAccess.ChannelHandle channel)) return false;
            channel.execute(c -> c.setVolume(volume));
            return true;
        } catch (IllegalAccessException | RuntimeException e) {
            return false;
        }
    }
}
