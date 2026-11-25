package com.nebulacrak.modules;

import com.nebulacrak.gui.NebulaModules;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;

import java.util.Locale;

public class LoginCensorModule extends Module {
    public static LoginCensorModule INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> enabled = sgGeneral.add(new BoolSetting.Builder()
        .name("censor-login")
        .description("Visually mask the password argument of /login in chat input.")
        .defaultValue(true)
        .build());

    public LoginCensorModule() {
        super(NebulaModules.NEBULA_CDJ_DEV, "LoginCensor", "Visually censors /login password in chat input; original command is sent unchanged.");
        INSTANCE = this;
    }

    public boolean shouldCensorInput(String value) {
        if (!isActive() || !enabled.get() || value == null || value.isEmpty()) return false;
        return loginPasswordRange(value) != null;
    }

    public boolean shouldMaskChar(String fullValue, int globalIndex) {
        int[] range = loginPasswordRange(fullValue);
        if (range == null || !isActive() || !enabled.get()) return false;
        return globalIndex >= range[0] && globalIndex < range[1];
    }

    public String sanitizeForHistory(String msg) {
        if (msg == null) return "";
        int[] range = loginPasswordRange(msg);
        if (range == null) return msg;
        StringBuilder out = new StringBuilder(msg.length());
        for (int i = 0; i < msg.length(); i++) {
            out.append(i >= range[0] && i < range[1] ? '*' : msg.charAt(i));
        }
        return out.toString();
    }

    /** Returns [start,end) of password argument for /login only, or null. */
    public int[] loginPasswordRange(String value) {
        if (value == null) return null;
        String trimmedLeading = value.stripLeading();
        int leading = value.length() - trimmedLeading.length();
        if (!trimmedLeading.startsWith("/")) return null;

        int cmdStart = leading + 1;
        int i = cmdStart;
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) i++;
        String cmd = value.substring(cmdStart, i).toLowerCase(Locale.ROOT);
        if (!cmd.equals("login")) return null;

        while (i < value.length() && Character.isWhitespace(value.charAt(i))) i++;
        if (i >= value.length()) return null;
        int argStart = i;
        while (i < value.length() && !Character.isWhitespace(value.charAt(i))) i++;
        int argEnd = i;
        if (argEnd <= argStart) return null;
        return new int[]{argStart, argEnd};
    }
}
