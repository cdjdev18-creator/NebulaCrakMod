package com.nebulacrak.mixin;

import com.nebulacrak.modules.LoginCensorModule;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {
    @Shadow private String value;
    @Shadow private int displayPos;

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;applyFormat(Ljava/lang/String;I)Lnet/minecraft/util/FormattedCharSequence;"))
    private FormattedCharSequence nebulacrak$censorApplyFormat(EditBox instance, String text, int offset) {
        LoginCensorModule mod = LoginCensorModule.INSTANCE;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mod == null || !mod.isActive() || mc == null || !(mc.screen instanceof ChatScreen)) {
            return ((EditBoxAccessor) instance).nebulacrak$invokeApplyFormat(text, offset);
        }

        String full = this.value;
        if (!mod.shouldCensorInput(full)) {
            return ((EditBoxAccessor) instance).nebulacrak$invokeApplyFormat(text, offset);
        }

        StringBuilder masked = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            int global = offset + i;
            masked.append(mod.shouldMaskChar(full, global) ? '*' : text.charAt(i));
        }
        // Do not call original formatter for sensitive text to avoid leaking original chars via formatter/suggestion style paths.
        return FormattedCharSequence.forward(masked.toString(), Style.EMPTY);
    }
}
