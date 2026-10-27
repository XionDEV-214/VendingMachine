package com.eisiadev.vendingmachine.client;

import com.eisiadev.vendingmachine.VMConfig;
import com.eisiadev.vendingmachine.VendingMachine;
import com.eisiadev.vendingmachine.gui.MusicTrack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = VendingMachine.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class VendingMusic {

    private static final int FADE_TICKS = 20;
    private static final float SILENT = 1.0E-4f;

    private static boolean inGui;
    @Nullable
    private static TrackSound current;

    private static float gameFade = 1;
    private static boolean gameMusicTouched = false;

    private VendingMusic() {}

    public static void start() {
        inGui = true;
        ensurePlaying();
    }

    public static void stop() {
        inGui = false;
    }

    public static void onTrackChanged() {
        if (current != null && current.track != VMConfig.MUSIC_TRACK.get()) {
            current.dying = true;
            current = null;
        }
        if (inGui) ensurePlaying();
    }

    private static void ensurePlaying() {
        MusicTrack track = VMConfig.MUSIC_TRACK.get();
        SoundEvent event = track.getSound();
        if (event == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (current != null && !current.isStopped() && mc.getSoundManager().isActive(current)) return;
        current = new TrackSound(track, event);
        mc.getSoundManager().play(current);
    }

    private static boolean vendingMusicWanted() {
        return inGui && VMConfig.MUSIC_TRACK.get().getSound() != null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();

        if (inGui && current != null && !mc.getSoundManager().isActive(current)) {
            current = null;
            ensurePlaying();
        }

        float step = 1f / FADE_TICKS;
        gameFade = vendingMusicWanted() ? Math.max(0, gameFade - step) : Math.min(1, gameFade + step);
        if (gameFade < 1 || gameMusicTouched) applyGameMusicVolume(mc);
    }

    private static void applyGameMusicVolume(Minecraft mc) {
        SoundInstance music = SoundAccess.currentMusic(mc.getMusicManager());
        if (music == null || !mc.getSoundManager().isActive(music)) {
            gameMusicTouched = false;
            return;
        }
        float base = Mth.clamp(music.getVolume() * mc.options.getSoundSourceVolume(SoundSource.MUSIC), 0, 1);
        if (base <= 0) return;
        float volume = Math.max(SILENT, base * gameFade);
        if (SoundAccess.setVolume(mc.getSoundManager(), music, volume)) {
            gameMusicTouched = gameFade < 1;
        }
    }

    private static final class TrackSound extends AbstractTickableSoundInstance {

        final MusicTrack track;
        float fade = 0;
        boolean dying = false;

        TrackSound(MusicTrack track, SoundEvent event) {
            super(event, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.track = track;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.x = 0;
            this.y = 0;
            this.z = 0;
            this.volume = SILENT;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            boolean audible = inGui && !dying && current == this;
            float step = 1f / FADE_TICKS;
            fade = audible ? Math.min(1, fade + step) : Math.max(0, fade - step);
            volume = Math.max(SILENT, fade * VMConfig.MUSIC_VOLUME.get().floatValue());

            if (fade <= 0 && (dying || current != this)) {
                stop();
            }
        }
    }
}
