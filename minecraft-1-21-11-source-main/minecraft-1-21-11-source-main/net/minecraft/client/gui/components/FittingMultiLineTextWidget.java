package net.minecraft.client.gui.components;

import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class FittingMultiLineTextWidget extends AbstractTextAreaWidget {
   private final Font font;
   private final MultiLineTextWidget multilineWidget;

   public FittingMultiLineTextWidget(final int x, final int y, final int width, final int height, final Component message, final Font font) {
      super(x, y, width, height, message);
      this.font = font;
      this.multilineWidget = (new MultiLineTextWidget(message, font)).setMaxWidth(this.getWidth() - this.totalInnerPadding());
   }

   public void setWidth(final int width) {
      super.setWidth(width);
      this.multilineWidget.setMaxWidth(this.getWidth() - this.totalInnerPadding());
   }

   protected int getInnerHeight() {
      return this.multilineWidget.getHeight();
   }

   public void minimizeHeight() {
      if (!this.showingScrollBar()) {
         this.setHeight(this.getInnerHeight() + this.totalInnerPadding());
      }

   }

   protected double scrollRate() {
      Objects.requireNonNull(this.font);
      return (double)9.0F;
   }

   protected void renderBackground(final GuiGraphics graphics) {
      super.renderBackground(graphics);
   }

   public boolean showingScrollBar() {
      return super.scrollbarVisible();
   }

   protected void renderContents(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
      graphics.pose().pushMatrix();
      graphics.pose().translate((float)this.getInnerLeft(), (float)this.getInnerTop());
      this.multilineWidget.render(graphics, mouseX, mouseY, a);
      graphics.pose().popMatrix();
   }

   protected void updateWidgetNarration(final NarrationElementOutput output) {
      output.add(NarratedElementType.TITLE, this.getMessage());
   }

   public void setMessage(final Component message) {
      super.setMessage(message);
      this.multilineWidget.setMessage(message);
   }
}
