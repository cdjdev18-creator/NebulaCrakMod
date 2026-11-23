package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class PopupScreen extends Screen {
   private static final Identifier BACKGROUND_SPRITE = Identifier.withDefaultNamespace("popup/background");
   private static final int SPACING = 12;
   private static final int BG_BORDER_WITH_SPACING = 18;
   private static final int BUTTON_SPACING = 6;
   private static final int IMAGE_SIZE_X = 130;
   private static final int IMAGE_SIZE_Y = 64;
   private static final int POPUP_DEFAULT_WIDTH = 250;
   private final Screen backgroundScreen;
   private final @Nullable Identifier image;
   private final Component message;
   private final List buttons;
   private final @Nullable Runnable onClose;
   private final int contentWidth;
   private final LinearLayout layout = LinearLayout.vertical();

   private PopupScreen(final Screen backgroundScreen, final int backgroundWidth, final @Nullable Identifier image, final Component title, final Component message, final List buttons, final @Nullable Runnable onClose) {
      super(title);
      this.backgroundScreen = backgroundScreen;
      this.image = image;
      this.message = message;
      this.buttons = buttons;
      this.onClose = onClose;
      this.contentWidth = backgroundWidth - 36;
   }

   public void added() {
      super.added();
      this.backgroundScreen.clearFocus();
   }

   protected void init() {
      this.backgroundScreen.init(this.width, this.height);
      this.layout.spacing(12).defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild((new MultiLineTextWidget(this.title.copy().withStyle(ChatFormatting.BOLD), this.font)).setMaxWidth(this.contentWidth).setCentered(true));
      if (this.image != null) {
         this.layout.addChild(ImageWidget.texture(130, 64, this.image, 130, 64));
      }

      this.layout.addChild((new MultiLineTextWidget(this.message, this.font)).setMaxWidth(this.contentWidth).setCentered(true));
      this.layout.addChild(this.buildButtonRow());
      this.layout.visitWidgets((x$0) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(x$0);
      });
      this.repositionElements();
   }

   private LinearLayout buildButtonRow() {
      int totalSpacing = 6 * (this.buttons.size() - 1);
      int buttonWidth = Math.min((this.contentWidth - totalSpacing) / this.buttons.size(), 150);
      LinearLayout row = LinearLayout.horizontal();
      row.spacing(6);

      for(ButtonOption button : this.buttons) {
         row.addChild(Button.builder(button.message(), (b) -> button.action().accept(this)).width(buttonWidth).build());
      }

      return row;
   }

   protected void repositionElements() {
      this.backgroundScreen.resize(this.width, this.height);
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
      this.backgroundScreen.renderBackground(graphics, mouseX, mouseY, a);
      graphics.nextStratum();
      this.backgroundScreen.render(graphics, -1, -1, a);
      graphics.nextStratum();
      this.renderTransparentBackground(graphics);
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND_SPRITE, this.layout.getX() - 18, this.layout.getY() - 18, this.layout.getWidth() + 36, this.layout.getHeight() + 36);
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.title, this.message);
   }

   public void onClose() {
      if (this.onClose != null) {
         this.onClose.run();
      }

      this.minecraft.setScreen(this.backgroundScreen);
   }

   public static class Builder {
      private final Screen backgroundScreen;
      private final Component title;
      private Component message;
      private int width;
      private @Nullable Identifier image;
      private final List buttons;
      private @Nullable Runnable onClose;

      public Builder(final Screen backgroundScreen, final Component title) {
         this.message = CommonComponents.EMPTY;
         this.width = 250;
         this.buttons = new ArrayList();
         this.onClose = null;
         this.backgroundScreen = backgroundScreen;
         this.title = title;
      }

      public Builder setWidth(final int width) {
         this.width = width;
         return this;
      }

      public Builder setImage(final Identifier image) {
         this.image = image;
         return this;
      }

      public Builder setMessage(final Component message) {
         this.message = message;
         return this;
      }

      public Builder addButton(final Component message, final Consumer action) {
         this.buttons.add(new ButtonOption(message, action));
         return this;
      }

      public Builder onClose(final Runnable onClose) {
         this.onClose = onClose;
         return this;
      }

      public PopupScreen build() {
         if (this.buttons.isEmpty()) {
            throw new IllegalStateException("Popup must have at least one button");
         } else {
            return new PopupScreen(this.backgroundScreen, this.width, this.image, this.title, this.message, List.copyOf(this.buttons), this.onClose);
         }
      }
   }

   private static record ButtonOption(Component message, Consumer action) {
   }
}
