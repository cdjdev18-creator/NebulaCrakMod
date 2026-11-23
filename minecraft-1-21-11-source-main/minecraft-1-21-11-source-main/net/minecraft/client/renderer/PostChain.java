package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class PostChain implements AutoCloseable {
   public static final Identifier MAIN_TARGET_ID = Identifier.withDefaultNamespace("main");
   private final List passes;
   private final Map internalTargets;
   private final Set externalTargets;
   private final Map persistentTargets = new HashMap();
   private final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer;

   private PostChain(final List passes, final Map internalTargets, final Set externalTargets, final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer) {
      this.passes = passes;
      this.internalTargets = internalTargets;
      this.externalTargets = externalTargets;
      this.projectionMatrixBuffer = projectionMatrixBuffer;
   }

   public static PostChain load(final PostChainConfig config, final TextureManager textureManager, final Set allowedExternalTargets, final Identifier id, final CachedOrthoProjectionMatrixBuffer projectionMatrixBuffer) throws ShaderManager.CompilationException {
      Stream<Identifier> referencedTargets = config.passes().stream().flatMap(PostChainConfig.Pass::referencedTargets);
      Set<Identifier> referencedExternalTargets = (Set)referencedTargets.filter((targetId) -> !config.internalTargets().containsKey(targetId)).collect(Collectors.toSet());
      Set<Identifier> invalidExternalTargets = Sets.difference(referencedExternalTargets, allowedExternalTargets);
      if (!invalidExternalTargets.isEmpty()) {
         throw new ShaderManager.CompilationException("Referenced external targets are not available in this context: " + String.valueOf(invalidExternalTargets));
      } else {
         ImmutableList.Builder<PostPass> passes = ImmutableList.builder();

         for(int i = 0; i < config.passes().size(); ++i) {
            PostChainConfig.Pass pass = (PostChainConfig.Pass)config.passes().get(i);
            passes.add(createPass(textureManager, pass, id.withSuffix("/" + i)));
         }

         return new PostChain(passes.build(), config.internalTargets(), referencedExternalTargets, projectionMatrixBuffer);
      }
   }

   private static PostPass createPass(final TextureManager textureManager, final PostChainConfig.Pass config, final Identifier id) throws ShaderManager.CompilationException {
      RenderPipeline.Builder pipelineBuilder = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET).withFragmentShader(config.fragmentShaderId()).withVertexShader(config.vertexShaderId()).withLocation(id);

      for(PostChainConfig.Input input : config.inputs()) {
         pipelineBuilder.withSampler(input.samplerName() + "Sampler");
      }

      pipelineBuilder.withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER);

      for(String uniformGroupName : config.uniforms().keySet()) {
         pipelineBuilder.withUniform(uniformGroupName, UniformType.UNIFORM_BUFFER);
      }

      RenderPipeline pipeline = pipelineBuilder.build();
      List<PostPass.Input> inputs = new ArrayList();

      for(PostChainConfig.Input input : config.inputs()) {
         Objects.requireNonNull(input);
         byte var9 = 0;
         //$FF: var9->value
         //0->net/minecraft/client/renderer/PostChainConfig$TextureInput
         //1->net/minecraft/client/renderer/PostChainConfig$TargetInput
         switch (input.typeSwitch<invokedynamic>(input, var9)) {
            case 0:
               PostChainConfig.TextureInput var10 = (PostChainConfig.TextureInput)input;
               PostChainConfig.TextureInput var51 = var10;

               try {
                  var52 = var51.samplerName();
               } catch (Throwable var30) {
                  throw new MatchException(var30.toString(), var30);
               }

               String var35 = var52;
               String samplerName = var35;
               var51 = var10;

               try {
                  var54 = var51.location();
               } catch (Throwable var29) {
                  throw new MatchException(var29.toString(), var29);
               }

               Identifier var36 = var54;
               Identifier location = var36;
               var51 = var10;

               try {
                  var56 = var51.width();
               } catch (Throwable var28) {
                  throw new MatchException(var28.toString(), var28);
               }

               int var37 = var56;
               int width = var37;
               var51 = var10;

               try {
                  var58 = var51.height();
               } catch (Throwable var27) {
                  throw new MatchException(var27.toString(), var27);
               }

               var37 = var58;
               int height = var37;
               var51 = var10;

               try {
                  var60 = var51.bilinear();
               } catch (Throwable var26) {
                  throw new MatchException(var26.toString(), var26);
               }

               var37 = var60;
               boolean bilinear = (boolean)var37;
               AbstractTexture texture = textureManager.getTexture(location.withPath((UnaryOperator)((path) -> "textures/effect/" + path + ".png")));
               inputs.add(new PostPass.TextureInput(samplerName, texture, width, height, bilinear));
               break;
            case 1:
               PostChainConfig.TargetInput texture = (PostChainConfig.TargetInput)input;
               PostChainConfig.TargetInput var10000 = texture;

               try {
                  var44 = var10000.samplerName();
               } catch (Throwable var25) {
                  throw new MatchException(var25.toString(), var25);
               }

               String bilinear = var44;
               String samplerName = bilinear;
               var10000 = texture;

               try {
                  var46 = var10000.targetId();
               } catch (Throwable var24) {
                  throw new MatchException(var24.toString(), var24);
               }

               Identifier bilinear = var46;
               Identifier targetId = bilinear;
               var10000 = texture;

               try {
                  var48 = var10000.useDepthBuffer();
               } catch (Throwable var23) {
                  throw new MatchException(var23.toString(), var23);
               }

               boolean bilinear = var48;
               boolean useDepthBuffer = bilinear;
               var10000 = texture;

               try {
                  var50 = var10000.bilinear();
               } catch (Throwable var22) {
                  throw new MatchException(var22.toString(), var22);
               }

               bilinear = var50;
               inputs.add(new PostPass.TargetInput(samplerName, targetId, useDepthBuffer, bilinear));
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
         }
      }

      return new PostPass(pipeline, config.outputTarget(), config.uniforms(), inputs);
   }

   public void addToFrame(final FrameGraphBuilder frame, final int screenWidth, final int screenHeight, final TargetBundle providedTargets) {
      GpuBufferSlice projectionBuffer = this.projectionMatrixBuffer.getBuffer((float)screenWidth, (float)screenHeight);
      Map<Identifier, ResourceHandle<RenderTarget>> targets = new HashMap(this.internalTargets.size() + this.externalTargets.size());

      for(Identifier id : this.externalTargets) {
         targets.put(id, providedTargets.getOrThrow(id));
      }

      for(Map.Entry entry : this.internalTargets.entrySet()) {
         Identifier id = (Identifier)entry.getKey();
         PostChainConfig.InternalTarget target = (PostChainConfig.InternalTarget)entry.getValue();
         RenderTargetDescriptor descriptor = new RenderTargetDescriptor((Integer)target.width().orElse(screenWidth), (Integer)target.height().orElse(screenHeight), true, target.clearColor());
         if (target.persistent()) {
            RenderTarget persistentTarget = this.getOrCreatePersistentTarget(id, descriptor);
            targets.put(id, frame.importExternal(id.toString(), persistentTarget));
         } else {
            targets.put(id, frame.createInternal(id.toString(), descriptor));
         }
      }

      for(PostPass pass : this.passes) {
         pass.addToFrame(frame, targets, projectionBuffer);
      }

      for(Identifier id : this.externalTargets) {
         providedTargets.replace(id, (ResourceHandle)targets.get(id));
      }

   }

   /** @deprecated */
   @Deprecated
   public void process(final RenderTarget mainTarget, final GraphicsResourceAllocator resourceAllocator) {
      FrameGraphBuilder frame = new FrameGraphBuilder();
      TargetBundle targets = PostChain.TargetBundle.of(MAIN_TARGET_ID, frame.importExternal("main", mainTarget));
      this.addToFrame(frame, mainTarget.width, mainTarget.height, targets);
      frame.execute(resourceAllocator);
   }

   private RenderTarget getOrCreatePersistentTarget(final Identifier id, final RenderTargetDescriptor descriptor) {
      RenderTarget target = (RenderTarget)this.persistentTargets.get(id);
      if (target == null || target.width != descriptor.width() || target.height != descriptor.height()) {
         if (target != null) {
            target.destroyBuffers();
         }

         target = descriptor.allocate();
         descriptor.prepare(target);
         this.persistentTargets.put(id, target);
      }

      return target;
   }

   public void close() {
      this.persistentTargets.values().forEach(RenderTarget::destroyBuffers);
      this.persistentTargets.clear();

      for(PostPass pass : this.passes) {
         pass.close();
      }

   }

   public interface TargetBundle {
      static TargetBundle of(final Identifier targetId, final ResourceHandle target) {
         return new TargetBundle() {
            private ResourceHandle handle = target;

            public void replace(final Identifier id, final ResourceHandle handle) {
               if (id.equals(targetId)) {
                  this.handle = handle;
               } else {
                  throw new IllegalArgumentException("No target with id " + String.valueOf(id));
               }
            }

            public @Nullable ResourceHandle get(final Identifier id) {
               return id.equals(targetId) ? this.handle : null;
            }
         };
      }

      void replace(Identifier id, ResourceHandle handle);

      @Nullable ResourceHandle get(Identifier id);

      default ResourceHandle getOrThrow(final Identifier id) {
         ResourceHandle<RenderTarget> handle = this.get(id);
         if (handle == null) {
            throw new IllegalArgumentException("Missing target with id " + String.valueOf(id));
         } else {
            return handle;
         }
      }
   }
}
