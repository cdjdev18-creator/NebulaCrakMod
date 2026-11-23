package com.nebulacrak.gui;

import com.nebulacrak.language.LanguageManager;
import com.nebulacrak.modules.MuteChatPlusModule;
import com.nebulacrak.modules.TranslatePlusModule;
import com.nebulacrak.translation.TranslationManager;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaModules {
    public static final Category NEBULA_PLAYER = new Category("nebula.player");
    public static final Category NEBULA_CHAT = new Category("nebula.chat");
    public static final Category NEBULA_MISC = new Category("nebula.misc");

    public static class CDJCategory {
        private final String id;
        private final String translationKey;
        private final String defaultName;
        private final Category meteorCategory;
        private final List<Module> modules = new ArrayList<>();

        public CDJCategory(String id, String translationKey, String defaultName, Category meteorCategory) {
            this.id = id;
            this.translationKey = translationKey;
            this.defaultName = defaultName;
            this.meteorCategory = meteorCategory;
        }

        public String getId() {
            return id;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public String getDefaultName() {
            return defaultName;
        }

        public String getDisplayName() {
            return TranslationManager.get(translationKey, defaultName);
        }

        public Category getMeteorCategory() {
            return meteorCategory;
        }

        public List<Module> getModules() {
            return modules;
        }

        public CDJCategory add(Module module) {
            this.modules.add(module);
            return this;
        }
    }

    public static final List<CDJCategory> CDJ_CATEGORIES = new ArrayList<>();

    public static final CDJCategory CATEGORY_PLAYER = new CDJCategory("player", "nebula.category.player", "Player", NEBULA_PLAYER);
    public static final CDJCategory CATEGORY_CHAT = new CDJCategory("chat", "nebula.category.chat", "Chat", NEBULA_CHAT);
    public static final CDJCategory CATEGORY_MISC = new CDJCategory("misc", "nebula.category.misc", "Misc", NEBULA_MISC);

    static {
        CDJ_CATEGORIES.add(CATEGORY_PLAYER);
        CDJ_CATEGORIES.add(CATEGORY_CHAT);
        CDJ_CATEGORIES.add(CATEGORY_MISC);

        CATEGORY_PLAYER.add(new PlayerDetectorModule());
        CATEGORY_CHAT.add(new ChatDetectorModule());
        CATEGORY_CHAT.add(new MuteChatPlusModule());
        CATEGORY_MISC.add(new TranslatePlusModule());
    }

    public static final List<Module> PLAYER_MODULES = CATEGORY_PLAYER.getModules();
    public static final List<Module> CHAT_MODULES = CATEGORY_CHAT.getModules();
    public static final List<Module> MISC_MODULES = CATEGORY_MISC.getModules();

    /**
     * Registra las categorías técnicas en Meteor para que los módulos propios
     * sean reconocidos internamente, pero la interfaz de NebulaCrakMod
     * los filtrará con el prefijo "nebula." para no mostrarlos como columnas
     * separadas en la sección ModulosMeteor.
     */
    public static void registerCategories() {
        for (CDJCategory cat : CDJ_CATEGORIES) {
            if (cat.getMeteorCategory() != null) {
                Modules.registerCategory(cat.getMeteorCategory());
            }
        }
    }

    public static void registerModules() {
        for (CDJCategory cat : CDJ_CATEGORIES) {
            for (Module module : cat.getModules()) {
                Modules.get().add(module);
            }
        }
    }

    /**
     * API pública: agrega una nueva categoría CDJ+ con sus módulos.
     * Registra la categoría técnicamente en Meteor y la añade a la lista
     * que muestra NebulaGUIScreen dentro del desplegable NebulaCrakMod.
     */
    public static void registerCDJCategory(CDJCategory category) {
        CDJ_CATEGORIES.add(category);
        if (category.getMeteorCategory() != null) {
            Modules.registerCategory(category.getMeteorCategory());
        }
        for (Module module : category.getModules()) {
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
            super(NEBULA_PLAYER, "player-detector", "Detects and displays player state information");
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
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.displayClientMessage(
                    Text.literal(
                        TranslationManager.get("nebula.player.detector.enabled", "[PlayerDetector] Enabled")
                    ), false);
            }
        }

        @Override
        public void onDeactivate() {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.displayClientMessage(
                    Text.literal(
                        TranslationManager.get("nebula.player.detector.disabled", "[PlayerDetector] Disabled")
                    ), false);
            }
        }

        public PlayerState getPlayerState() {
            if (!enabled.get()) return null;
            LocalPlayer player = MinecraftClient.getInstance().player;
            if (player == null || player.getWorld() == null) return null;

            Vec3d pos = player.getPos();
            Vec3d vel = player.getVelocity();
            boolean isMoving = vel.horizontalLengthSquared() > 1.0E-6;
            return new PlayerState(
                player.getUuid(),
                pos.x, pos.y, pos.z,
                isMoving,
                player.isSprinting(),
                player.isJumping(),
                player.isOnGround(),
                player.isSneaking()
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
            super(NEBULA_CHAT, "chat-detector", "Detects chat messages and events");
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
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.displayClientMessage(
                    Text.literal(
                        TranslationManager.get("nebula.chat.detector.enabled", "[ChatDetector] Enabled")
                    ), false);
            }
        }

        @Override
        public void onDeactivate() {
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.displayClientMessage(
                    Text.literal(
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