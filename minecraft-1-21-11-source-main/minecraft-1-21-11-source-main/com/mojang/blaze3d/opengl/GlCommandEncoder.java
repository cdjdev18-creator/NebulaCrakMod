package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;
import org.slf4j.Logger;

public class GlCommandEncoder implements CommandEncoder {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final GlDevice device;
   private final int readFbo;
   private final int drawFbo;
   private @Nullable RenderPipeline lastPipeline;
   private boolean inRenderPass;
   private @Nullable GlProgram lastProgram;
   private @Nullable GlTimerQuery activeTimerQuery;

   protected GlCommandEncoder(final GlDevice device) {
      this.device = device;
      this.readFbo = device.directStateAccess().createFrameBufferObject();
      this.drawFbo = device.directStateAccess().createFrameBufferObject();
   }

   public RenderPass createRenderPass(final Supplier label, final GpuTextureView colorTexture, final OptionalInt clearColor) {
      return this.createRenderPass(label, colorTexture, clearColor, (GpuTextureView)null, OptionalDouble.empty());
   }

   public RenderPass createRenderPass(final Supplier label, final GpuTextureView colorTexture, final OptionalInt clearColor, final @Nullable GpuTextureView depthTexture, final OptionalDouble clearDepth) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         if (clearDepth.isPresent() && depthTexture == null) {
            LOGGER.warn("Depth clear value was provided but no depth texture is being used");
         }

         if (colorTexture.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
         } else if ((colorTexture.texture().usage() & 8) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
         } else if (colorTexture.texture().getDepthOrLayers() > 1) {
            throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
         } else {
            if (depthTexture != null) {
               if (depthTexture.isClosed()) {
                  throw new IllegalStateException("Depth texture is closed");
               }

               if ((depthTexture.texture().usage() & 8) == 0) {
                  throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
               }

               if (depthTexture.texture().getDepthOrLayers() > 1) {
                  throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported as an attachment");
               }
            }

            this.inRenderPass = true;
            this.device.debugLabels().pushDebugGroup(label);
            int fbo = ((GlTextureView)colorTexture).getFbo(this.device.directStateAccess(), depthTexture == null ? null : depthTexture.texture());
            GlStateManager._glBindFramebuffer(36160, fbo);
            int clearMask = 0;
            if (clearColor.isPresent()) {
               int argb = clearColor.getAsInt();
               GL11.glClearColor(ARGB.redFloat(argb), ARGB.greenFloat(argb), ARGB.blueFloat(argb), ARGB.alphaFloat(argb));
               clearMask |= 16384;
            }

            if (depthTexture != null && clearDepth.isPresent()) {
               GL11.glClearDepth(clearDepth.getAsDouble());
               clearMask |= 256;
            }

            if (clearMask != 0) {
               GlStateManager._disableScissorTest();
               GlStateManager._depthMask(true);
               GlStateManager._colorMask(true, true, true, true);
               GlStateManager._clear(clearMask);
            }

            GlStateManager._viewport(0, 0, colorTexture.getWidth(0), colorTexture.getHeight(0));
            this.lastPipeline = null;
            return new GlRenderPass(this, depthTexture != null);
         }
      }
   }

   public void clearColorTexture(final GpuTexture colorTexture, final int clearColor) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(colorTexture);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)colorTexture).id, 0, 0, 36160);
         GL11.glClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
         GlStateManager._disableScissorTest();
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16384);
         GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   public void clearColorAndDepthTextures(final GpuTexture colorTexture, final int clearColor, final GpuTexture depthTexture, final double clearDepth) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(colorTexture);
         this.verifyDepthTexture(depthTexture);
         int fbo = ((GlTexture)colorTexture).getFbo(this.device.directStateAccess(), depthTexture);
         GlStateManager._glBindFramebuffer(36160, fbo);
         GlStateManager._disableScissorTest();
         GL11.glClearDepth(clearDepth);
         GL11.glClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16640);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   public void clearColorAndDepthTextures(final GpuTexture colorTexture, final int clearColor, final GpuTexture depthTexture, final double clearDepth, final int regionX, final int regionY, final int regionWidth, final int regionHeight) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyColorTexture(colorTexture);
         this.verifyDepthTexture(depthTexture);
         this.verifyRegion(colorTexture, regionX, regionY, regionWidth, regionHeight);
         int fbo = ((GlTexture)colorTexture).getFbo(this.device.directStateAccess(), depthTexture);
         GlStateManager._glBindFramebuffer(36160, fbo);
         GlStateManager._scissorBox(regionX, regionY, regionWidth, regionHeight);
         GlStateManager._enableScissorTest();
         GL11.glClearDepth(clearDepth);
         GL11.glClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         GlStateManager._clear(16640);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   private void verifyRegion(final GpuTexture colorTexture, final int regionX, final int regionY, final int regionWidth, final int regionHeight) {
      if (regionX >= 0 && regionX < colorTexture.getWidth(0)) {
         if (regionY >= 0 && regionY < colorTexture.getHeight(0)) {
            if (regionWidth <= 0) {
               throw new IllegalArgumentException("regionWidth should be greater than 0");
            } else if (regionX + regionWidth > colorTexture.getWidth(0)) {
               throw new IllegalArgumentException("regionWidth + regionX should be less than the texture width");
            } else if (regionHeight <= 0) {
               throw new IllegalArgumentException("regionHeight should be greater than 0");
            } else if (regionY + regionHeight > colorTexture.getHeight(0)) {
               throw new IllegalArgumentException("regionWidth + regionX should be less than the texture height");
            }
         } else {
            throw new IllegalArgumentException("regionY should not be outside of the texture");
         }
      } else {
         throw new IllegalArgumentException("regionX should not be outside of the texture");
      }
   }

   public void clearDepthTexture(final GpuTexture depthTexture, final double clearDepth) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before creating a new one!");
      } else {
         this.verifyDepthTexture(depthTexture);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)depthTexture).id, 0, 36160);
         GL11.glDrawBuffer(0);
         GL11.glClearDepth(clearDepth);
         GlStateManager._depthMask(true);
         GlStateManager._disableScissorTest();
         GlStateManager._clear(256);
         GL11.glDrawBuffer(36064);
         GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
         GlStateManager._glBindFramebuffer(36160, 0);
      }
   }

   private void verifyColorTexture(final GpuTexture colorTexture) {
      if (!colorTexture.getFormat().hasColorAspect()) {
         throw new IllegalStateException("Trying to clear a non-color texture as color");
      } else if (colorTexture.isClosed()) {
         throw new IllegalStateException("Color texture is closed");
      } else if ((colorTexture.usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT");
      } else if (colorTexture.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   private void verifyDepthTexture(final GpuTexture depthTexture) {
      if (!depthTexture.getFormat().hasDepthAspect()) {
         throw new IllegalStateException("Trying to clear a non-depth texture as depth");
      } else if (depthTexture.isClosed()) {
         throw new IllegalStateException("Depth texture is closed");
      } else if ((depthTexture.usage() & 8) == 0) {
         throw new IllegalStateException("Depth texture must have USAGE_RENDER_ATTACHMENT");
      } else if (depthTexture.getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Clearing a texture with multiple layers or depths is not yet supported");
      }
   }

   public void writeToBuffer(final GpuBufferSlice slice, final ByteBuffer data) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer buffer = (GlBuffer)slice.buffer();
         if (buffer.closed) {
            throw new IllegalStateException("Buffer already closed");
         } else if ((buffer.usage() & 8) == 0) {
            throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
         } else {
            int length = data.remaining();
            if ((long)length > slice.length()) {
               throw new IllegalArgumentException("Cannot write more data than the slice allows (attempting to write " + length + " bytes into a slice of length " + slice.length() + ")");
            } else if (slice.length() + slice.offset() > buffer.size()) {
               throw new IllegalArgumentException("Cannot write more data than this buffer can hold (attempting to write " + length + " bytes at offset " + slice.offset() + " to " + buffer.size() + " size buffer)");
            } else {
               this.device.directStateAccess().bufferSubData(buffer.handle, slice.offset(), data, buffer.usage());
            }
         }
      }
   }

   public GpuBuffer.MappedView mapBuffer(final GpuBuffer buffer, final boolean read, final boolean write) {
      return this.mapBuffer(buffer.slice(), read, write);
   }

   public GpuBuffer.MappedView mapBuffer(final GpuBufferSlice slice, final boolean read, final boolean write) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer buffer = (GlBuffer)slice.buffer();
         if (buffer.closed) {
            throw new IllegalStateException("Buffer already closed");
         } else if (!read && !write) {
            throw new IllegalArgumentException("At least read or write must be true");
         } else if (read && (buffer.usage() & 1) == 0) {
            throw new IllegalStateException("Buffer is not readable");
         } else if (write && (buffer.usage() & 2) == 0) {
            throw new IllegalStateException("Buffer is not writable");
         } else if (slice.offset() + slice.length() > buffer.size()) {
            long var10002 = slice.length();
            throw new IllegalArgumentException("Cannot map more data than this buffer can hold (attempting to map " + var10002 + " bytes at offset " + slice.offset() + " from " + buffer.size() + " size buffer)");
         } else {
            int flags = 0;
            if (read) {
               flags |= 1;
            }

            if (write) {
               flags |= 34;
            }

            return this.device.getBufferStorage().mapBuffer(this.device.directStateAccess(), buffer, slice.offset(), slice.length(), flags);
         }
      }
   }

   public void copyToBuffer(final GpuBufferSlice source, final GpuBufferSlice target) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         GlBuffer sourceBuffer = (GlBuffer)source.buffer();
         if (sourceBuffer.closed) {
            throw new IllegalStateException("Source buffer already closed");
         } else if ((sourceBuffer.usage() & 16) == 0) {
            throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
         } else {
            GlBuffer targetBuffer = (GlBuffer)target.buffer();
            if (targetBuffer.closed) {
               throw new IllegalStateException("Target buffer already closed");
            } else if ((targetBuffer.usage() & 8) == 0) {
               throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
            } else if (source.length() != target.length()) {
               long var6 = source.length();
               throw new IllegalArgumentException("Cannot copy from slice of size " + var6 + " to slice of size " + target.length() + ", they must be equal");
            } else if (source.offset() + source.length() > sourceBuffer.size()) {
               long var5 = source.length();
               throw new IllegalArgumentException("Cannot copy more data than the source buffer holds (attempting to copy " + var5 + " bytes at offset " + source.offset() + " from " + sourceBuffer.size() + " size buffer)");
            } else if (target.offset() + target.length() > targetBuffer.size()) {
               long var10002 = target.length();
               throw new IllegalArgumentException("Cannot copy more data than the target buffer can hold (attempting to copy " + var10002 + " bytes at offset " + target.offset() + " to " + targetBuffer.size() + " size buffer)");
            } else {
               this.device.directStateAccess().copyBufferSubData(sourceBuffer.handle, targetBuffer.handle, source.offset(), target.offset(), source.length());
            }
         }
      }
   }

   public void writeToTexture(final GpuTexture destination, final NativeImage source) {
      int width = destination.getWidth(0);
      int height = destination.getHeight(0);
      if (source.getWidth() == width && source.getHeight() == height) {
         if (destination.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
         } else if ((destination.usage() & 1) == 0) {
            throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
         } else {
            this.writeToTexture(destination, source, 0, 0, 0, 0, width, height, 0, 0);
         }
      } else {
         throw new IllegalArgumentException("Cannot replace texture of size " + width + "x" + height + " with image of size " + source.getWidth() + "x" + source.getHeight());
      }
   }

   public void writeToTexture(final GpuTexture destination, final NativeImage source, final int mipLevel, final int depthOrLayer, final int destX, final int destY, final int width, final int height, final int sourceX, final int sourceY) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (mipLevel >= 0 && mipLevel < destination.getMipLevels()) {
         if (sourceX + width <= source.getWidth() && sourceY + height <= source.getHeight()) {
            if (destX + width <= destination.getWidth(mipLevel) && destY + height <= destination.getHeight(mipLevel)) {
               if (destination.isClosed()) {
                  throw new IllegalStateException("Destination texture is closed");
               } else if ((destination.usage() & 1) == 0) {
                  throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
               } else if (depthOrLayer >= destination.getDepthOrLayers()) {
                  throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + destination.getDepthOrLayers());
               } else {
                  int target;
                  if ((destination.usage() & 16) != 0) {
                     target = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
                     GL11.glBindTexture(34067, ((GlTexture)destination).id);
                  } else {
                     target = 3553;
                     GlStateManager._bindTexture(((GlTexture)destination).id);
                  }

                  GlStateManager._pixelStore(3314, source.getWidth());
                  GlStateManager._pixelStore(3316, sourceX);
                  GlStateManager._pixelStore(3315, sourceY);
                  GlStateManager._pixelStore(3317, source.format().components());
                  GlStateManager._texSubImage2D(target, mipLevel, destX, destY, width, height, GlConst.toGl(source.format()), 5121, source.getPointer());
               }
            } else {
               throw new IllegalArgumentException("Dest texture (" + width + "x" + height + ") is not large enough to write a rectangle of " + width + "x" + height + " at " + destX + "x" + destY + " (at mip level " + mipLevel + ")");
            }
         } else {
            int var10002 = source.getWidth();
            throw new IllegalArgumentException("Copy source (" + var10002 + "x" + source.getHeight() + ") is not large enough to read a rectangle of " + width + "x" + height + " from " + sourceX + "x" + sourceY);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + destination.getMipLevels());
      }
   }

   public void writeToTexture(final GpuTexture destination, final ByteBuffer source, final NativeImage.Format format, final int mipLevel, final int depthOrLayer, final int destX, final int destY, final int width, final int height) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (mipLevel >= 0 && mipLevel < destination.getMipLevels()) {
         if (width * height * format.components() > source.remaining()) {
            throw new IllegalArgumentException("Copy would overrun the source buffer (remaining length of " + source.remaining() + ", but copy is " + width + "x" + height + " of format " + String.valueOf(format) + ")");
         } else if (destX + width <= destination.getWidth(mipLevel) && destY + height <= destination.getHeight(mipLevel)) {
            if (destination.isClosed()) {
               throw new IllegalStateException("Destination texture is closed");
            } else if ((destination.usage() & 1) == 0) {
               throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (depthOrLayer >= destination.getDepthOrLayers()) {
               throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + destination.getDepthOrLayers());
            } else {
               int target;
               if ((destination.usage() & 16) != 0) {
                  target = GlConst.CUBEMAP_TARGETS[depthOrLayer % 6];
                  GL11.glBindTexture(34067, ((GlTexture)destination).id);
               } else {
                  target = 3553;
                  GlStateManager._bindTexture(((GlTexture)destination).id);
               }

               GlStateManager._pixelStore(3314, width);
               GlStateManager._pixelStore(3316, 0);
               GlStateManager._pixelStore(3315, 0);
               GlStateManager._pixelStore(3317, format.components());
               GlStateManager._texSubImage2D(target, mipLevel, destX, destY, width, height, GlConst.toGl(format), 5121, source);
            }
         } else {
            throw new IllegalArgumentException("Dest texture (" + destination.getWidth(mipLevel) + "x" + destination.getHeight(mipLevel) + ") is not large enough to write a rectangle of " + width + "x" + height + " at " + destX + "x" + destY);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + destination.getMipLevels());
      }
   }

   public void copyTextureToBuffer(final GpuTexture source, final GpuBuffer destination, final long offset, final Runnable callback, final int mipLevel) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         this.copyTextureToBuffer(source, destination, offset, callback, mipLevel, 0, 0, source.getWidth(mipLevel), source.getHeight(mipLevel));
      }
   }

   public void copyTextureToBuffer(final GpuTexture source, final GpuBuffer destination, final long offset, final Runnable callback, final int mipLevel, final int x, final int y, final int width, final int height) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (mipLevel >= 0 && mipLevel < source.getMipLevels()) {
         if ((long)(source.getWidth(mipLevel) * source.getHeight(mipLevel) * source.getFormat().pixelSize()) + offset > destination.size()) {
            long var12 = destination.size();
            throw new IllegalArgumentException("Buffer of size " + var12 + " is not large enough to hold " + width + "x" + height + " pixels (" + source.getFormat().pixelSize() + " bytes each) starting from offset " + offset);
         } else if ((source.usage() & 2) == 0) {
            throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
         } else if ((destination.usage() & 8) == 0) {
            throw new IllegalArgumentException("Buffer needs USAGE_COPY_DST to be a destination for a copy");
         } else if (x + width <= source.getWidth(mipLevel) && y + height <= source.getHeight(mipLevel)) {
            if (source.isClosed()) {
               throw new IllegalStateException("Source texture is closed");
            } else if (destination.isClosed()) {
               throw new IllegalStateException("Destination buffer is closed");
            } else if (source.getDepthOrLayers() > 1) {
               throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
            } else {
               GlStateManager.clearGlErrors();
               this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)source).glId(), 0, mipLevel, 36008);
               GlStateManager._glBindBuffer(35051, ((GlBuffer)destination).handle);
               GlStateManager._pixelStore(3330, width);
               GlStateManager._readPixels(x, y, width, height, GlConst.toGlExternalId(source.getFormat()), GlConst.toGlType(source.getFormat()), offset);
               RenderSystem.queueFencedTask(callback);
               GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, mipLevel);
               GlStateManager._glBindFramebuffer(36008, 0);
               GlStateManager._glBindBuffer(35051, 0);
               int error = GlStateManager._getError();
               if (error != 0) {
                  String var10002 = source.getLabel();
                  throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + var10002 + ": GL error " + error);
               }
            }
         } else {
            throw new IllegalArgumentException("Copy source texture (" + source.getWidth(mipLevel) + "x" + source.getHeight(mipLevel) + ") is not large enough to read a rectangle of " + width + "x" + height + " from " + x + "," + y);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels());
      }
   }

   public void copyTextureToTexture(final GpuTexture source, final GpuTexture destination, final int mipLevel, final int destX, final int destY, final int sourceX, final int sourceY, final int width, final int height) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (mipLevel >= 0 && mipLevel < source.getMipLevels() && mipLevel < destination.getMipLevels()) {
         if (destX + width <= destination.getWidth(mipLevel) && destY + height <= destination.getHeight(mipLevel)) {
            if (sourceX + width <= source.getWidth(mipLevel) && sourceY + height <= source.getHeight(mipLevel)) {
               if (source.isClosed()) {
                  throw new IllegalStateException("Source texture is closed");
               } else if (destination.isClosed()) {
                  throw new IllegalStateException("Destination texture is closed");
               } else if ((source.usage() & 2) == 0) {
                  throw new IllegalArgumentException("Texture needs USAGE_COPY_SRC to be a source for a copy");
               } else if ((destination.usage() & 1) == 0) {
                  throw new IllegalArgumentException("Texture needs USAGE_COPY_DST to be a destination for a copy");
               } else if (source.getDepthOrLayers() > 1) {
                  throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
               } else if (destination.getDepthOrLayers() > 1) {
                  throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for copying");
               } else {
                  GlStateManager.clearGlErrors();
                  GlStateManager._disableScissorTest();
                  boolean isDepth = source.getFormat().hasDepthAspect();
                  int sourceId = ((GlTexture)source).glId();
                  int destId = ((GlTexture)destination).glId();
                  this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, isDepth ? 0 : sourceId, isDepth ? sourceId : 0, 0, 0);
                  this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, isDepth ? 0 : destId, isDepth ? destId : 0, 0, 0);
                  this.device.directStateAccess().blitFrameBuffers(this.readFbo, this.drawFbo, sourceX, sourceY, width, height, destX, destY, width, height, isDepth ? 256 : 16384, 9728);
                  int error = GlStateManager._getError();
                  if (error != 0) {
                     String var10002 = source.getLabel();
                     throw new IllegalStateException("Couldn't perform copyToTexture for texture " + var10002 + " to " + destination.getLabel() + ": GL error " + error);
                  }
               }
            } else {
               throw new IllegalArgumentException("Source texture (" + source.getWidth(mipLevel) + "x" + source.getHeight(mipLevel) + ") is not large enough to read a rectangle of " + width + "x" + height + " at " + sourceX + "x" + sourceY);
            }
         } else {
            throw new IllegalArgumentException("Dest texture (" + destination.getWidth(mipLevel) + "x" + destination.getHeight(mipLevel) + ") is not large enough to write a rectangle of " + width + "x" + height + " at " + destX + "x" + destY);
         }
      } else {
         throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + source.getMipLevels() + " and < " + destination.getMipLevels());
      }
   }

   public void presentTexture(final GpuTextureView textureView) {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else if (!textureView.texture().getFormat().hasColorAspect()) {
         throw new IllegalStateException("Cannot present a non-color texture!");
      } else if ((textureView.texture().usage() & 8) == 0) {
         throw new IllegalStateException("Color texture must have USAGE_RENDER_ATTACHMENT to presented to the screen");
      } else if (textureView.texture().getDepthOrLayers() > 1) {
         throw new UnsupportedOperationException("Textures with multiple depths or layers are not yet supported for presentation");
      } else {
         GlStateManager._disableScissorTest();
         GlStateManager._viewport(0, 0, textureView.getWidth(0), textureView.getHeight(0));
         GlStateManager._depthMask(true);
         GlStateManager._colorMask(true, true, true, true);
         this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)textureView.texture()).glId(), 0, 0, 0);
         this.device.directStateAccess().blitFrameBuffers(this.drawFbo, 0, 0, 0, textureView.getWidth(0), textureView.getHeight(0), 0, 0, textureView.getWidth(0), textureView.getHeight(0), 16384, 9728);
      }
   }

   public GpuFence createFence() {
      if (this.inRenderPass) {
         throw new IllegalStateException("Close the existing render pass before performing additional commands");
      } else {
         return new GlFence();
      }
   }

   protected void executeDrawMultiple(final GlRenderPass renderPass, final Collection draws, final @Nullable GpuBuffer defaultIndexBuffer, VertexFormat.@Nullable IndexType defaultIndexType, final Collection dynamicUniforms, final Object uniformArgument) {
      if (this.trySetup(renderPass, dynamicUniforms)) {
         if (defaultIndexType == null) {
            defaultIndexType = VertexFormat.IndexType.SHORT;
         }

         for(RenderPass.Draw draw : draws) {
            VertexFormat.IndexType indexType = draw.indexType() == null ? defaultIndexType : draw.indexType();
            renderPass.setIndexBuffer(draw.indexBuffer() == null ? defaultIndexBuffer : draw.indexBuffer(), indexType);
            renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer());
            if (GlRenderPass.VALIDATION) {
               if (renderPass.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (renderPass.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if (renderPass.vertexBuffers[0] == null) {
                  throw new IllegalStateException("Missing vertex buffer at slot 0");
               }

               if (renderPass.vertexBuffers[0].isClosed()) {
                  throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
               }
            }

            BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer = draw.uniformUploaderConsumer();
            if (uniformUploaderConsumer != null) {
               uniformUploaderConsumer.accept(uniformArgument, (RenderPass.UniformUploader)(name, buffer) -> {
                  Uniform patt1$temp = renderPass.pipeline.program().getUniform(name);
                  if (patt1$temp instanceof Uniform.Ubo $b$0) {
                     Uniform.Ubo var10000 = $b$0;

                     try {
                        var8 = var10000.blockBinding();
                     } catch (Throwable var7) {
                        throw new MatchException(var7.toString(), var7);
                     }

                     int patt2$temp = var8;
                     GL32.glBindBufferRange(35345, patt2$temp, ((GlBuffer)buffer.buffer()).handle, buffer.offset(), buffer.length());
                  }

               });
            }

            this.drawFromBuffers(renderPass, 0, draw.firstIndex(), draw.indexCount(), indexType, renderPass.pipeline, 1);
         }

      }
   }

   protected void executeDraw(final GlRenderPass renderPass, final int baseVertex, final int firstIndex, final int drawCount, final VertexFormat.@Nullable IndexType indexType, final int instanceCount) {
      if (this.trySetup(renderPass, Collections.emptyList())) {
         if (GlRenderPass.VALIDATION) {
            if (indexType != null) {
               if (renderPass.indexBuffer == null) {
                  throw new IllegalStateException("Missing index buffer");
               }

               if (renderPass.indexBuffer.isClosed()) {
                  throw new IllegalStateException("Index buffer has been closed!");
               }

               if ((renderPass.indexBuffer.usage() & 64) == 0) {
                  throw new IllegalStateException("Index buffer must have GpuBuffer.USAGE_INDEX!");
               }
            }

            GlRenderPipeline pipeline = renderPass.pipeline;
            if (renderPass.vertexBuffers[0] == null && pipeline != null && !pipeline.info().getVertexFormat().getElements().isEmpty()) {
               throw new IllegalStateException("Vertex format contains elements but vertex buffer at slot 0 is null");
            }

            if (renderPass.vertexBuffers[0] != null && renderPass.vertexBuffers[0].isClosed()) {
               throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
            }

            if (renderPass.vertexBuffers[0] != null && (renderPass.vertexBuffers[0].usage() & 32) == 0) {
               throw new IllegalStateException("Vertex buffer must have GpuBuffer.USAGE_VERTEX!");
            }
         }

         this.drawFromBuffers(renderPass, baseVertex, firstIndex, drawCount, indexType, renderPass.pipeline, instanceCount);
      }
   }

   private void drawFromBuffers(final GlRenderPass renderPass, final int baseVertex, final int firstIndex, final int drawCount, final VertexFormat.@Nullable IndexType indexType, final GlRenderPipeline pipeline, final int instanceCount) {
      this.device.vertexArrayCache().bindVertexArray(pipeline.info().getVertexFormat(), (GlBuffer)renderPass.vertexBuffers[0]);
      if (indexType != null) {
         GlStateManager._glBindBuffer(34963, ((GlBuffer)renderPass.indexBuffer).handle);
         if (instanceCount > 1) {
            if (baseVertex > 0) {
               GL32.glDrawElementsInstancedBaseVertex(GlConst.toGl(pipeline.info().getVertexFormatMode()), drawCount, GlConst.toGl(indexType), (long)firstIndex * (long)indexType.bytes, instanceCount, baseVertex);
            } else {
               GL31.glDrawElementsInstanced(GlConst.toGl(pipeline.info().getVertexFormatMode()), drawCount, GlConst.toGl(indexType), (long)firstIndex * (long)indexType.bytes, instanceCount);
            }
         } else if (baseVertex > 0) {
            GL32.glDrawElementsBaseVertex(GlConst.toGl(pipeline.info().getVertexFormatMode()), drawCount, GlConst.toGl(indexType), (long)firstIndex * (long)indexType.bytes, baseVertex);
         } else {
            GlStateManager._drawElements(GlConst.toGl(pipeline.info().getVertexFormatMode()), drawCount, GlConst.toGl(indexType), (long)firstIndex * (long)indexType.bytes);
         }
      } else if (instanceCount > 1) {
         GL31.glDrawArraysInstanced(GlConst.toGl(pipeline.info().getVertexFormatMode()), baseVertex, drawCount, instanceCount);
      } else {
         GlStateManager._drawArrays(GlConst.toGl(pipeline.info().getVertexFormatMode()), baseVertex, drawCount);
      }

   }

   private boolean trySetup(final GlRenderPass renderPass, final Collection dynamicUniforms) {
      if (!GlRenderPass.VALIDATION) {
         if (renderPass.pipeline == null || renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            return false;
         }
      } else {
         if (renderPass.pipeline == null) {
            throw new IllegalStateException("Can't draw without a render pipeline");
         }

         if (renderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            throw new IllegalStateException("Pipeline contains invalid shader program");
         }

         for(RenderPipeline.UniformDescription uniform : renderPass.pipeline.info().getUniforms()) {
            GpuBufferSlice value = (GpuBufferSlice)renderPass.uniforms.get(uniform.name());
            if (!dynamicUniforms.contains(uniform.name())) {
               if (value == null) {
                  String var10002 = uniform.name();
                  throw new IllegalStateException("Missing uniform " + var10002 + " (should be " + String.valueOf(uniform.type()) + ")");
               }

               if (uniform.type() == UniformType.UNIFORM_BUFFER) {
                  if (value.buffer().isClosed()) {
                     throw new IllegalStateException("Uniform buffer " + uniform.name() + " is already closed");
                  }

                  if ((value.buffer().usage() & 128) == 0) {
                     throw new IllegalStateException("Uniform buffer " + uniform.name() + " must have GpuBuffer.USAGE_UNIFORM");
                  }
               }

               if (uniform.type() == UniformType.TEXEL_BUFFER) {
                  if (value.offset() != 0L || value.length() != value.buffer().size()) {
                     throw new IllegalStateException("Uniform texel buffers do not support a slice of a buffer, must be entire buffer");
                  }

                  if (uniform.textureFormat() == null) {
                     throw new IllegalStateException("Invalid uniform texel buffer " + uniform.name() + " (missing a texture format)");
                  }
               }
            }
         }

         for(Map.Entry entry : renderPass.pipeline.program().getUniforms().entrySet()) {
            if (entry.getValue() instanceof Uniform.Sampler) {
               String name = (String)entry.getKey();
               GlRenderPass.TextureViewAndSampler viewAndSampler = (GlRenderPass.TextureViewAndSampler)renderPass.samplers.get(name);
               if (viewAndSampler == null) {
                  throw new IllegalStateException("Missing sampler " + name);
               }

               GlTextureView textureView = viewAndSampler.view();
               if (textureView.isClosed()) {
                  throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
               }

               if ((textureView.texture().usage() & 4) == 0) {
                  throw new IllegalStateException("Texture view " + name + " (" + textureView.texture().getLabel() + ") must have USAGE_TEXTURE_BINDING!");
               }

               if (viewAndSampler.sampler().isClosed()) {
                  throw new IllegalStateException("Sampler for " + name + " (" + textureView.texture().getLabel() + ") has been closed!");
               }
            }
         }

         if (renderPass.pipeline.info().wantsDepthTexture() && !renderPass.hasDepthTexture()) {
            LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", renderPass.pipeline.info().getLocation());
         }
      }

      RenderPipeline pipeline = renderPass.pipeline.info();
      GlProgram glProgram = renderPass.pipeline.program();
      this.applyPipelineState(pipeline);
      boolean differentProgram = this.lastProgram != glProgram;
      if (differentProgram) {
         GlStateManager._glUseProgram(glProgram.getProgramId());
         this.lastProgram = glProgram;
      }

      for(Map.Entry entry : glProgram.getUniforms().entrySet()) {
         String name = (String)entry.getKey();
         boolean isDirty = renderPass.dirtyUniforms.contains(name);
         Uniform.Ubo var10000 = (Uniform)entry.getValue();
         Objects.requireNonNull(var10000);
         Uniform var10 = var10000;
         byte var11 = 0;
         //$FF: var11->value
         //0->com/mojang/blaze3d/opengl/Uniform$Ubo
         //1->com/mojang/blaze3d/opengl/Uniform$Utb
         //2->com/mojang/blaze3d/opengl/Uniform$Sampler
         switch (var10.typeSwitch<invokedynamic>(var10, var11)) {
            case 0:
               Uniform.Ubo var12 = (Uniform.Ubo)var10;
               var10000 = var12;

               try {
                  var63 = var10000.blockBinding();
               } catch (Throwable var32) {
                  throw new MatchException(var32.toString(), var32);
               }

               int var41 = var63;
               int blockBinding = var41;
               if (isDirty) {
                  GpuBufferSlice bufferView = (GpuBufferSlice)renderPass.uniforms.get(name);
                  GL32.glBindBufferRange(35345, blockBinding, ((GlBuffer)bufferView.buffer()).handle, bufferView.offset(), bufferView.length());
               }
               break;
            case 1:
               Uniform.Utb bufferView = (Uniform.Utb)var10;
               Uniform.Utb var54 = bufferView;

               try {
                  var55 = var54.location();
               } catch (Throwable var31) {
                  throw new MatchException(var31.toString(), var31);
               }

               int texture = var55;
               int location = texture;
               var54 = bufferView;

               try {
                  var57 = var54.samplerIndex();
               } catch (Throwable var30) {
                  throw new MatchException(var30.toString(), var30);
               }

               texture = var57;
               int samplerIndex = texture;
               var54 = bufferView;

               try {
                  var59 = var54.format();
               } catch (Throwable var29) {
                  throw new MatchException(var29.toString(), var29);
               }

               TextureFormat texture = var59;
               TextureFormat format = texture;
               var54 = bufferView;

               try {
                  var61 = var54.texture();
               } catch (Throwable var28) {
                  throw new MatchException(var28.toString(), var28);
               }

               int texture = var61;
               if (differentProgram || isDirty) {
                  GlStateManager._glUniform1i(location, samplerIndex);
               }

               GlStateManager._activeTexture('蓀' + samplerIndex);
               GL11C.glBindTexture(35882, texture);
               if (isDirty) {
                  GpuBufferSlice bufferView = (GpuBufferSlice)renderPass.uniforms.get(name);
                  GL31.glTexBuffer(35882, GlConst.toGlInternalId(format), ((GlBuffer)bufferView.buffer()).handle);
               }
               break;
            case 2:
               Uniform.Sampler bufferView = (Uniform.Sampler)var10;
               Uniform.Sampler var50 = bufferView;

               try {
                  var51 = var50.location();
               } catch (Throwable var27) {
                  throw new MatchException(var27.toString(), var27);
               }

               int var22 = var51;
               int location = var22;
               var50 = bufferView;

               try {
                  var53 = var50.samplerIndex();
               } catch (Throwable var26) {
                  throw new MatchException(var26.toString(), var26);
               }

               var22 = var53;
               int samplerIndex = var22;
               GlRenderPass.TextureViewAndSampler viewAndSampler = (GlRenderPass.TextureViewAndSampler)renderPass.samplers.get(name);
               if (viewAndSampler == null) {
                  break;
               }

               GlTextureView textureView = viewAndSampler.view();
               if (differentProgram || isDirty) {
                  GlStateManager._glUniform1i(location, samplerIndex);
               }

               GlStateManager._activeTexture('蓀' + samplerIndex);
               GlTexture texture = textureView.texture();
               int target;
               if ((texture.usage() & 16) != 0) {
                  target = 34067;
                  GL11.glBindTexture(34067, texture.id);
               } else {
                  target = 3553;
                  GlStateManager._bindTexture(texture.id);
               }

               GL33C.glBindSampler(samplerIndex, viewAndSampler.sampler().getId());
               GlStateManager._texParameter(target, 33084, textureView.baseMipLevel());
               GlStateManager._texParameter(target, 33085, textureView.baseMipLevel() + textureView.mipLevels() - 1);
               break;
            default:
               throw new MatchException((String)null, (Throwable)null);
         }
      }

      renderPass.dirtyUniforms.clear();
      if (renderPass.isScissorEnabled()) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight());
      } else {
         GlStateManager._disableScissorTest();
      }

      return true;
   }

   private void applyPipelineState(final RenderPipeline pipeline) {
      if (this.lastPipeline != pipeline) {
         this.lastPipeline = pipeline;
         if (pipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GlConst.toGl(pipeline.getDepthTestFunction()));
         } else {
            GlStateManager._disableDepthTest();
         }

         if (pipeline.isCull()) {
            GlStateManager._enableCull();
         } else {
            GlStateManager._disableCull();
         }

         if (pipeline.getBlendFunction().isPresent()) {
            GlStateManager._enableBlend();
            BlendFunction blendFunction = (BlendFunction)pipeline.getBlendFunction().get();
            GlStateManager._blendFuncSeparate(GlConst.toGl(blendFunction.sourceColor()), GlConst.toGl(blendFunction.destColor()), GlConst.toGl(blendFunction.sourceAlpha()), GlConst.toGl(blendFunction.destAlpha()));
         } else {
            GlStateManager._disableBlend();
         }

         GlStateManager._polygonMode(1032, GlConst.toGl(pipeline.getPolygonMode()));
         GlStateManager._depthMask(pipeline.isWriteDepth());
         GlStateManager._colorMask(pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteColor(), pipeline.isWriteAlpha());
         if (pipeline.getDepthBiasConstant() == 0.0F && pipeline.getDepthBiasScaleFactor() == 0.0F) {
            GlStateManager._disablePolygonOffset();
         } else {
            GlStateManager._polygonOffset(pipeline.getDepthBiasScaleFactor(), pipeline.getDepthBiasConstant());
            GlStateManager._enablePolygonOffset();
         }

         switch (pipeline.getColorLogic()) {
            case NONE:
               GlStateManager._disableColorLogicOp();
               break;
            case OR_REVERSE:
               GlStateManager._enableColorLogicOp();
               GlStateManager._logicOp(5387);
         }

      }
   }

   public void finishRenderPass() {
      this.inRenderPass = false;
      GlStateManager._glBindFramebuffer(36160, 0);
      this.device.debugLabels().popDebugGroup();
   }

   protected GlDevice getDevice() {
      return this.device;
   }

   public GpuQuery timerQueryBegin() {
      RenderSystem.assertOnRenderThread();
      if (this.activeTimerQuery != null) {
         throw new IllegalStateException("A GL_TIME_ELAPSED query is already active");
      } else {
         int queryId = GL32C.glGenQueries();
         GL32C.glBeginQuery(35007, queryId);
         this.activeTimerQuery = new GlTimerQuery(queryId);
         return this.activeTimerQuery;
      }
   }

   public void timerQueryEnd(final GpuQuery query) {
      RenderSystem.assertOnRenderThread();
      if (query != this.activeTimerQuery) {
         throw new IllegalStateException("Mismatched or duplicate GpuQuery when ending timerQuery");
      } else {
         GL32C.glEndQuery(35007);
         this.activeTimerQuery = null;
      }
   }
}
