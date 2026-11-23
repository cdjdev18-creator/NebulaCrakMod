package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class EditGameRulesScreen extends Screen {
   private static final Component TITLE = Component.translatable("editGamerule.title");
   private static final int SPACING = 8;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Consumer exitCallback;
   private final Set invalidEntries = Sets.newHashSet();
   private final GameRules gameRules;
   private RuleList ruleList;
   private @Nullable Button doneButton;

   public EditGameRulesScreen(final GameRules gameRules, final Consumer exitCallback) {
      super(TITLE);
      this.gameRules = gameRules;
      this.exitCallback = exitCallback;
   }

   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.ruleList = (RuleList)this.layout.addToContents(new RuleList(this.gameRules));
      LinearLayout footer = (LinearLayout)this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = (Button)footer.addChild(Button.builder(CommonComponents.GUI_DONE, (button) -> this.exitCallback.accept(Optional.of(this.gameRules))).build());
      footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, (button) -> this.onClose()).build());
      this.layout.visitWidgets((x$0) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(x$0);
      });
      this.repositionElements();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.ruleList != null) {
         this.ruleList.updateSize(this.width, this.layout);
      }

   }

   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   private void updateDoneButton() {
      if (this.doneButton != null) {
         this.doneButton.active = this.invalidEntries.isEmpty();
      }

   }

   private void markInvalid(final RuleEntry invalidEntry) {
      this.invalidEntries.add(invalidEntry);
      this.updateDoneButton();
   }

   private void clearInvalid(final RuleEntry invalidEntry) {
      this.invalidEntries.remove(invalidEntry);
      this.updateDoneButton();
   }

   public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry {
      private final @Nullable List tooltip;

      public RuleEntry(final @Nullable List tooltip) {
         this.tooltip = tooltip;
      }
   }

   public class CategoryRuleEntry extends RuleEntry {
      private final Component label;

      public CategoryRuleEntry(final Component label) {
         super((List)null);
         this.label = label;
      }

      public void renderContent(final GuiGraphics graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         graphics.drawCenteredString(EditGameRulesScreen.this.minecraft.font, (Component)this.label, this.getContentXMiddle(), this.getContentY() + 5, -1);
      }

      public List children() {
         return ImmutableList.of();
      }

      public List narratables() {
         return ImmutableList.of(new NarratableEntry() {
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(final NarrationElementOutput output) {
               output.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
            }
         });
      }
   }

   public abstract class GameRuleEntry extends RuleEntry {
      private final List label;
      protected final List children = Lists.newArrayList();

      public GameRuleEntry(final List tooltip, final Component label) {
         super(tooltip);
         this.label = EditGameRulesScreen.this.minecraft.font.split(label, 175);
      }

      public List children() {
         return this.children;
      }

      public List narratables() {
         return this.children;
      }

      protected void renderLabel(final GuiGraphics graphics, final int rowTop, final int rowLeft) {
         if (this.label.size() == 1) {
            graphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(0)), rowLeft, rowTop + 5, -1);
         } else if (this.label.size() >= 2) {
            graphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(0)), rowLeft, rowTop, -1);
            graphics.drawString(EditGameRulesScreen.this.minecraft.font, (FormattedCharSequence)((FormattedCharSequence)this.label.get(1)), rowLeft, rowTop + 10, -1);
         }

      }
   }

   public class BooleanRuleEntry extends GameRuleEntry {
      private final CycleButton checkbox;

      public BooleanRuleEntry(final Component name, final List tooltip, final String narration, final GameRule gameRule) {
         super(tooltip, name);
         this.checkbox = CycleButton.onOffBuilder((Boolean)EditGameRulesScreen.this.gameRules.get(gameRule)).displayOnlyValue().withCustomNarration((button) -> button.createDefaultNarrationMessage().append("\n").append(narration)).create(10, 5, 44, 20, name, (button, newValue) -> EditGameRulesScreen.this.gameRules.set(gameRule, newValue, (MinecraftServer)null));
         this.children.add(this.checkbox);
      }

      public void renderContent(final GuiGraphics graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         this.renderLabel(graphics, this.getContentY(), this.getContentX());
         this.checkbox.setX(this.getContentRight() - 45);
         this.checkbox.setY(this.getContentY());
         this.checkbox.render(graphics, mouseX, mouseY, a);
      }
   }

   public class IntegerRuleEntry extends GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(final Component label, final List tooltip, final String narration, final GameRule gameRule) {
         super(tooltip, label);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 44, 20, label.copy().append("\n").append(narration).append("\n"));
         this.input.setValue(EditGameRulesScreen.this.gameRules.getAsString(gameRule));
         this.input.setResponder((v) -> {
            DataResult<Integer> value = gameRule.deserialize(v);
            if (value.isSuccess()) {
               this.input.setTextColor(-2039584);
               EditGameRulesScreen.this.clearInvalid(this);
               EditGameRulesScreen.this.gameRules.set(gameRule, (Integer)value.getOrThrow(), (MinecraftServer)null);
            } else {
               this.input.setTextColor(-65536);
               EditGameRulesScreen.this.markInvalid(this);
            }

         });
         this.children.add(this.input);
      }

      public void renderContent(final GuiGraphics graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
         this.renderLabel(graphics, this.getContentY(), this.getContentX());
         this.input.setX(this.getContentRight() - 45);
         this.input.setY(this.getContentY());
         this.input.render(graphics, mouseX, mouseY, a);
      }
   }

   public class RuleList extends ContainerObjectSelectionList {
      private static final int ITEM_HEIGHT = 24;

      public RuleList(final GameRules gameRules) {
         super(Minecraft.getInstance(), EditGameRulesScreen.this.width, EditGameRulesScreen.this.layout.getContentHeight(), EditGameRulesScreen.this.layout.getHeaderHeight(), 24);
         final Map<GameRuleCategory, Map<GameRule<?>, RuleEntry>> entries = Maps.newHashMap();
         gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            public void visitBoolean(final GameRule gameRule) {
               this.addEntry(gameRule, (x$0, x$1, x$2, x$3) -> EditGameRulesScreen.thisx.new BooleanRuleEntry(x$0, x$1, x$2, x$3));
            }

            public void visitInteger(final GameRule gameRule) {
               this.addEntry(gameRule, (x$0, x$1, x$2, x$3) -> EditGameRulesScreen.thisx.new IntegerRuleEntry(x$0, x$1, x$2, x$3));
            }

            private void addEntry(final GameRule gameRule, final EntryFactory factory) {
               Component readableName = Component.translatable(gameRule.getDescriptionId());
               Component actualName = Component.literal(gameRule.id()).withStyle(ChatFormatting.YELLOW);
               Component defaultValue = Component.translatable("editGamerule.default", Component.literal(gameRule.serialize(gameRule.defaultValue()))).withStyle(ChatFormatting.GRAY);
               String descriptionKey = gameRule.getDescriptionId() + ".description";
               List<FormattedCharSequence> tooltip;
               String narration;
               if (I18n.exists(descriptionKey)) {
                  ImmutableList.Builder<FormattedCharSequence> result = ImmutableList.builder().add(actualName.getVisualOrderText());
                  Component description = Component.translatable(descriptionKey);
                  List var10000 = EditGameRulesScreen.this.font.split(description, 150);
                  Objects.requireNonNull(result);
                  var10000.forEach(result::add);
                  tooltip = result.add(defaultValue.getVisualOrderText()).build();
                  String var11 = description.getString();
                  narration = var11 + "\n" + defaultValue.getString();
               } else {
                  tooltip = ImmutableList.of(actualName.getVisualOrderText(), defaultValue.getVisualOrderText());
                  narration = defaultValue.getString();
               }

               ((Map)entries.computeIfAbsent(gameRule.category(), (k) -> Maps.newHashMap())).put(gameRule, factory.create(readableName, tooltip, narration, gameRule));
            }
         });
         entries.entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getDescriptionId))).forEach((e) -> {
            this.addEntry(EditGameRulesScreen.this.new CategoryRuleEntry(((GameRuleCategory)e.getKey()).label().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)));
            ((Map)e.getValue()).entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(GameRule::getDescriptionId))).forEach((v) -> this.addEntry((RuleEntry)v.getValue()));
         });
      }

      public void renderWidget(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
         super.renderWidget(graphics, mouseX, mouseY, a);
         RuleEntry hovered = (RuleEntry)this.getHovered();
         if (hovered != null && hovered.tooltip != null) {
            graphics.setTooltipForNextFrame(hovered.tooltip, mouseX, mouseY);
         }

      }
   }

   @FunctionalInterface
   private interface EntryFactory {
      RuleEntry create(Component name, List tooltip, String narration, GameRule gameRule);
   }
}
