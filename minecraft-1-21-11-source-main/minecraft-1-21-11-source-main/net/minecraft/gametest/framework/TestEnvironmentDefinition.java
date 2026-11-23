package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

public interface TestEnvironmentDefinition {
   Codec DIRECT_CODEC = BuiltInRegistries.TEST_ENVIRONMENT_DEFINITION_TYPE.byNameCodec().dispatch(TestEnvironmentDefinition::codec, (c) -> c);
   Codec CODEC = RegistryFileCodec.create(Registries.TEST_ENVIRONMENT, DIRECT_CODEC);

   static MapCodec bootstrap(final Registry registry) {
      Registry.register(registry, (String)"all_of", TestEnvironmentDefinition.AllOf.CODEC);
      Registry.register(registry, (String)"game_rules", TestEnvironmentDefinition.SetGameRules.CODEC);
      Registry.register(registry, (String)"time_of_day", TestEnvironmentDefinition.TimeOfDay.CODEC);
      Registry.register(registry, (String)"weather", TestEnvironmentDefinition.Weather.CODEC);
      return (MapCodec)Registry.register(registry, (String)"function", TestEnvironmentDefinition.Functions.CODEC);
   }

   void setup(ServerLevel level);

   default void teardown(final ServerLevel level) {
   }

   MapCodec codec();

   public static record Weather(Type weather) implements TestEnvironmentDefinition {
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(Weather::weather)).apply(i, Weather::new));

      public void setup(final ServerLevel level) {
         this.weather.apply(level);
      }

      public void teardown(final ServerLevel level) {
         level.resetWeatherCycle();
      }

      public MapCodec codec() {
         return CODEC;
      }

      public static enum Type implements StringRepresentable {
         CLEAR("clear", 100000, 0, false, false),
         RAIN("rain", 0, 100000, true, false),
         THUNDER("thunder", 0, 100000, true, true);

         public static final Codec CODEC = StringRepresentable.fromEnum(Type::values);
         private final String id;
         private final int clearTime;
         private final int rainTime;
         private final boolean raining;
         private final boolean thundering;

         private Type(final String id, final int clearTime, final int rainTime, final boolean raining, final boolean thundering) {
            this.id = id;
            this.clearTime = clearTime;
            this.rainTime = rainTime;
            this.raining = raining;
            this.thundering = thundering;
         }

         void apply(final ServerLevel level) {
            level.setWeatherParameters(this.clearTime, this.rainTime, this.raining, this.thundering);
         }

         public String getSerializedName() {
            return this.id;
         }

         // $FF: synthetic method
         private static Type[] $values() {
            return new Type[]{CLEAR, RAIN, THUNDER};
         }
      }
   }

   public static record TimeOfDay(int time) implements TestEnvironmentDefinition {
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(ExtraCodecs.NON_NEGATIVE_INT.fieldOf("time").forGetter(TimeOfDay::time)).apply(i, TimeOfDay::new));

      public void setup(final ServerLevel level) {
         level.setDayTime((long)this.time);
      }

      public MapCodec codec() {
         return CODEC;
      }
   }

   public static record SetGameRules(GameRuleMap gameRulesMap) implements TestEnvironmentDefinition {
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(GameRuleMap.CODEC.fieldOf("rules").forGetter(SetGameRules::gameRulesMap)).apply(i, SetGameRules::new));

      public void setup(final ServerLevel level) {
         GameRules gameRules = level.getGameRules();
         MinecraftServer server = level.getServer();
         gameRules.setAll(this.gameRulesMap, server);
      }

      public void teardown(final ServerLevel level) {
         this.gameRulesMap.keySet().forEach((gameRule) -> this.resetRule(level, gameRule));
      }

      private void resetRule(final ServerLevel level, final GameRule gameRule) {
         level.getGameRules().set(gameRule, gameRule.defaultValue(), level.getServer());
      }

      public MapCodec codec() {
         return CODEC;
      }
   }

   public static record Functions(Optional setupFunction, Optional teardownFunction) implements TestEnvironmentDefinition {
      private static final Logger LOGGER = LogUtils.getLogger();
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(Identifier.CODEC.optionalFieldOf("setup").forGetter(Functions::setupFunction), Identifier.CODEC.optionalFieldOf("teardown").forGetter(Functions::teardownFunction)).apply(i, Functions::new));

      public void setup(final ServerLevel level) {
         this.setupFunction.ifPresent((p) -> run(level, p));
      }

      public void teardown(final ServerLevel level) {
         this.teardownFunction.ifPresent((p) -> run(level, p));
      }

      private static void run(final ServerLevel level, final Identifier functionId) {
         MinecraftServer server = level.getServer();
         ServerFunctionManager functions = server.getFunctions();
         Optional<CommandFunction<CommandSourceStack>> function = functions.get(functionId);
         if (function.isPresent()) {
            CommandSourceStack source = server.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER).withSuppressedOutput().withLevel(level);
            functions.execute((CommandFunction)function.get(), source);
         } else {
            LOGGER.error("Test Batch failed for non-existent function {}", functionId);
         }

      }

      public MapCodec codec() {
         return CODEC;
      }
   }

   public static record AllOf(List definitions) implements TestEnvironmentDefinition {
      public static final MapCodec CODEC = RecordCodecBuilder.mapCodec((i) -> i.group(TestEnvironmentDefinition.CODEC.listOf().fieldOf("definitions").forGetter(AllOf::definitions)).apply(i, AllOf::new));

      public AllOf(final TestEnvironmentDefinition... defs) {
         this(Arrays.stream(defs).map(Holder::direct).toList());
      }

      public void setup(final ServerLevel level) {
         this.definitions.forEach((b) -> ((TestEnvironmentDefinition)b.value()).setup(level));
      }

      public void teardown(final ServerLevel level) {
         this.definitions.forEach((b) -> ((TestEnvironmentDefinition)b.value()).teardown(level));
      }

      public MapCodec codec() {
         return CODEC;
      }
   }
}
