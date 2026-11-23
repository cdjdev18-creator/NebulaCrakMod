package com.nebulacrak.gui;

import com.nebulacrak.modules.MuteChatPlusModule;
import com.nebulacrak.modules.TranslatePlusModule;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class NebulaModules {
    // Single Meteor dropdown category
    public static final Category NEBULA = new Category("NebulaCrakMod+");

    public static final List<Module> ALL_MODULES = new ArrayList<>();

    static {
        ALL_MODULES.add(new PlayerDetectorModule());
        ALL_MODULES.add(new ChatDetectorModule());
        ALL_MODULES.add(new MuteChatPlusModule());
        ALL_MODULES.add(new TranslatePlusModule());
    }

    public static void registerCategories() {
        Modules.registerCategory(NEBULA);
    }

    public static void registerModules() {
        for (Module module : ALL_MODULES) {
            Modules.get().add(module);
        }
    }

    public static class PlayerDetectorModule extends Module {
        private final Setting<Boolean> enabled;
        private final Setting<Boolean> showPosition;
        private final Setting<Boolean> showMovement;
        private final Setting<Boolean> showSneaking;
        private final Setting<Boolean> showSprinting;
        private final Setting<Boolean> showJumping;
        private final Setting<Boolean> showOnGround;

        public PlayerDetectorModule() {
            super(NEBULA, "player-detector", "Detects and displays player state information");
            SettingGroup sg = settings.getDefaultGroup();
            enabled = sg.add(new BoolSetting.Builder()
                .name("enabled")
                .description("Enable player detection")
                .defaultValue(true)
                .build());
            showPosition = sg.add(new BoolSetting.Builder()
                .name("show-position")
                .description("Show player position")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            showMovement = sg.add(new BoolSetting.Builder()
                .name("show-movement")
                .description("Show movement state")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            showSneaking = sg.add(new BoolSetting.Builder()
                .name("show-sneaking")
                .description("Show sneaking state")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            showSprinting = sg.add(new BoolSetting.Builder()
                .name("show-sprinting")
                .description("Show sprinting state")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            showJumping = sg.add(new BoolSetting.Builder()
                .name("show-jumping")
                .description("Show jumping state")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            showOnGround = sg.add(new BoolSetting.Builder()
                .name("show-on-ground")
                .description("Show on ground state")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
        }

        @Override
        public void onActivate() {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(
                        TranslationManager.get("nebula.player.detector.enabled", "[PlayerDetector] Enabled")
                    ), false);
            }
        }

        @Override
        public void onDeactivate() {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(
                        TranslationManager.get("nebula.player.detector.disabled", "[PlayerDetector] Disabled")
                    ), false);
            }
        }

        public PlayerState getPlayerState() {
            if (!enabled.get()) return null;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.level() == null) return null;

            Vec3 pos = player.position();
            Vec3 vel = player.getDeltaMovement();
            boolean isMoving = vel.horizontalDistanceSqr() > 1.0E-6;
            return new PlayerState(
                player.getUUID(),
                pos.x, pos.y, pos.z,
                isMoving,
                player.isSprinting(),
                player.isJumping(),
                player.onGround(),
                player.isShiftKeyDown()
            );
        }

        public static class PlayerState {
            public final java.util.UUID uuid;
            public final double x, y, z;
            public final boolean isMoving;
            public final boolean isSprinting;
            public final boolean isJumping;
            public final boolean onGround;
            public final boolean isSneaking;

            public PlayerState(java.util.UUID uuid, double x, double y, double z,
                               boolean isMoving, boolean isSprinting, boolean isJumping,
                               boolean onGround, boolean isSneaking) {
                this.uuid = uuid;
                this.x = x;
                this.y = y;
                this.z = z;
                this.isMoving = isMoving;
                this.isSprinting = isSprinting;
                this.isJumping = isJumping;
                this.onGround = onGround;
                this.isSneaking = isSneaking;
            }

            @Override
            public String toString() {
                return String.format(
                    "PlayerState{uuid=%s, pos=(%.2f, %.2f, %.2f), moving=%s, sprinting=%s, jumping=%s, onGround=%s, sneaking=%s}",
                    uuid, x, y, z, isMoving, isSprinting, isJumping, onGround, isSneaking
                );
            }
        }
    }

    public static class ChatDetectorModule extends Module {
        private final Setting<Boolean> enabled;
        private final Setting<Boolean> detectReceived;
        private final Setting<Boolean> detectSent;
        private final Setting<Boolean> detectSystem;

        public ChatDetectorModule() {
            super(NEBULA, "chat-detector", "Detects chat messages and events");
            SettingGroup sg = settings.getDefaultGroup();
            enabled = sg.add(new BoolSetting.Builder()
                .name("enabled")
                .description("Enable chat detection")
                .defaultValue(true)
                .build());
            detectReceived = sg.add(new BoolSetting.Builder()
                .name("detect-received")
                .description("Detect received messages")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            detectSent = sg.add(new BoolSetting.Builder()
                .name("detect-sent")
                .description("Detect sent messages")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
            detectSystem = sg.add(new BoolSetting.Builder()
                .name("detect-system")
                .description("Detect system messages")
                .defaultValue(true)
                .visible(enabled::get)
                .build());
        }

        @Override
        public void onActivate() {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(
                        TranslationManager.get("nebula.chat.detector.enabled", "[ChatDetector] Enabled")
                    ), false);
            }
        }

        @Override
        public void onDeactivate() {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                    Component.literal(
                        TranslationManager.get("nebula.chat.detector.disabled", "[ChatDetector] Disabled")
                    ), false);
            }
        }

        public boolean isDetectionEnabled() {
            return enabled.get();
        }

        public boolean shouldDetectReceived() {
            return detectReceived.get();
        }

        public boolean shouldDetectSent() {
            return detectSent.get();
        }

        public boolean shouldDetectSystem() {
            return detectSystem.get();
        }

        public static class ChatEvent {
            public final String message;
            public final String sender;
            public final Type type;
            public final long timestamp;

            public ChatEvent(String message, String sender, Type type) {
                this.message = message;
                this.sender = sender;
                this.type = type;
                this.timestamp = System.currentTimeMillis();
            }

            public enum Type {
                RECEIVED,
                SENT,
                SYSTEM
            }
        }
    }
}