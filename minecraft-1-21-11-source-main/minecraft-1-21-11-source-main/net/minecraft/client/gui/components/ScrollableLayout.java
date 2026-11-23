package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jspecify.annotations.Nullable;

public class ScrollableLayout implements Layout {
   private static final int SCROLLBAR_SPACING = 4;
   private static final int SCROLLBAR_RESERVE = 10;
   private final Layout content;
   private final Container container;
   private int minWidth;
   private int maxHeight;

   public ScrollableLayout(final Minecraft minecraft, final Layout content, final int maxHeight) {
      this.content = content;
      this.container = new Container(minecraft, 0, maxHeight);
   }

   public void setMinWidth(final int minWidth) {
      this.minWidth = minWidth;
      this.container.setWidth(Math.max(this.content.getWidth(), minWidth));
   }

   public void setMaxHeight(final int maxHeight) {
      this.maxHeight = maxHeight;
      this.container.setHeight(Math.min(this.content.getHeight(), maxHeight));
      this.container.refreshScrollAmount();
   }

   public void arrangeElements() {
      this.content.arrangeElements();
      int contentWidth = this.content.getWidth();
      this.container.setWidth(Math.max(contentWidth + 20, this.minWidth));
      this.container.setHeight(Math.min(this.content.getHeight(), this.maxHeight));
      this.container.refreshScrollAmount();
   }

   public void visitChildren(final Consumer layoutElementVisitor) {
      layoutElementVisitor.accept(this.container);
   }

   public void setX(final int x) {
      this.container.setX(x);
   }

   public void setY(final int y) {
      this.container.setY(y);
   }

   public int getX() {
      return this.container.getX();
   }

   public int getY() {
      return this.container.getY();
   }

   public int getWidth() {
      return this.container.getWidth();
   }

   public int getHeight() {
      return this.container.getHeight();
   }

   private class Container extends AbstractContainerWidget {
      private final Minecraft minecraft;
      private final List children = new ArrayList();

      public Container(final Minecraft minecraft, final int width, final int height) {
         super(0, 0, width, height, CommonComponents.EMPTY);
         this.minecraft = minecraft;
         Layout var10000 = ScrollableLayout.this.content;
         List var10001 = this.children;
         Objects.requireNonNull(var10001);
         var10000.visitWidgets(var10001::add);
      }

      protected int contentHeight() {
         return ScrollableLayout.this.content.getHeight();
      }

      protected double scrollRate() {
         return (double)10.0F;
      }

      protected void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
         graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

         for(AbstractWidget child : this.children) {
            child.render(graphics, mouseX, mouseY, a);
         }

         graphics.disableScissor();
         this.renderScrollbar(graphics, mouseX, mouseY);
      }

      protected void updateWidgetNarration(final NarrationElementOutput output) {
      }

      public ScreenRectangle getBorderForArrowNavigation(final ScreenDirection opposite) {
         return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
      }

      public void setFocused(final @Nullable GuiEventListener focused) {
         super.setFocused(focused);
         if (focused != null && this.minecraft.getLastInputType().isKeyboard()) {
            ScreenRectangle area = this.getRectangle();
            ScreenRectangle focusedRect = focused.getRectangle();
            int topDelta = focusedRect.top() - area.top();
            int bottomDelta = focusedRect.bottom() - area.bottom();
            if (topDelta < 0) {
               this.setScrollAmount(this.scrollAmount() + (double)topDelta - (double)14.0F);
            } else if (bottomDelta > 0) {
               this.setScrollAmount(this.scrollAmount() + (double)bottomDelta + (double)14.0F);
            }

         }
      }

      public void setX(final int x) {
         super.setX(x);
         ScrollableLayout.this.content.setX(x + 10);
      }

      public void setY(final int y) {
         super.setY(y);
         ScrollableLayout.this.content.setY(y - (int)this.scrollAmount());
      }

      public void setScrollAmount(final double scrollAmount) {
         super.setScrollAmount(scrollAmount);
         ScrollableLayout.this.content.setY(this.getRectangle().top() - (int)this.scrollAmount());
      }

      public List children() {
         return this.children;
      }

      public Collection getNarratables() {
         return this.children;
      }
   }
}
