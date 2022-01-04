/*******************************************************************************
 * Copyright (c) 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package go.graphics.swing.vulkan;

import go.graphics.swing.vulkan.memory.AbstractVulkanBuffer;
import go.graphics.swing.vulkan.memory.EVulkanBufferUsage;
import go.graphics.swing.vulkan.memory.EVulkanMemoryType;
import go.graphics.swing.vulkan.memory.VulkanBufferHandle;
import go.graphics.swing.vulkan.memory.VulkanImage;
import go.graphics.swing.vulkan.memory.VulkanMemoryManager;
import go.graphics.swing.vulkan.memory.VulkanMultiBufferHandle;
import go.graphics.swing.vulkan.pipeline.EVulkanPipelineType;
import go.graphics.swing.vulkan.pipeline.VulkanPipelineManager;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

import go.graphics.AbstractColor;
import go.graphics.BackgroundDrawHandle;
import go.graphics.BufferHandle;
import go.graphics.EPrimitiveType;
import go.graphics.ETextureType;
import go.graphics.GLDrawContext;
import go.graphics.ManagedHandle;
import go.graphics.MultiDrawHandle;
import go.graphics.TextureHandle;
import go.graphics.UnifiedDrawHandle;
import go.graphics.VkDrawContext;
import go.graphics.swing.text.LWJGLTextDrawer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanDrawContext extends GLDrawContext implements VkDrawContext {

	protected VkDevice device = null;
	private VkPhysicalDevice physicalDevice;

	private long surface = VK_NULL_HANDLE;
	private int surfaceFormat;
	private VkInstance instance;

	private int fbWidth;
	private int fbHeight;

	private VkCommandBuffer graphCommandBuffer = null;
	private VkCommandBuffer memCommandBuffer = null;
	private VkCommandBuffer fbCommandBuffer = null;

	private long renderPass;

	private long fetchFramebufferSemaphore = VK_NULL_HANDLE;
	private long presentFramebufferSemaphore = VK_NULL_HANDLE;

	private VulkanDescriptorPool universalDescPool = null;
	private VulkanDescriptorPool textureDescPool = null;
	private VulkanDescriptorPool multiDescPool = null;

	public VulkanDescriptorSetLayout textureDescLayout = null;
	public VulkanDescriptorSetLayout multiDescLayout = null;

	private final VulkanMemoryManager memoryManager;
	private final VulkanPipelineManager pipelineManager;
	private final QueueManager queueManager;

	final long[] samplers = new long[ETextureType.values().length];

	private final Semaphore resourceMutex = new Semaphore(1);
	private final Semaphore closeMutex = new Semaphore(1);

	protected final List<VulkanTextureHandle> textures = new ArrayList<>();

	private float guiScale;

	public VulkanDrawContext(VkInstance instance, long surface, float guiScale) {
		this.instance = instance;
		this.guiScale = guiScale;

		BiFunction<VkQueueFamilyProperties, Integer, Boolean> presentQueueCond = (queue, index) -> {
			int[] present = new int[1];
			vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, index, surface, present);
			return present[0]==1;
		};

		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkPhysicalDevice[] allPhysicalDevices = VulkanUtils.listPhysicalDevices(stack, instance);
			physicalDevice = VulkanUtils.findPhysicalDevice(allPhysicalDevices);

			VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
			vkGetPhysicalDeviceProperties(physicalDevice, props);
			maxTextureSize = props.limits().maxImageDimension2D();
			maxUniformBlockSize = Integer.MAX_VALUE;

			queueManager = new QueueManager(stack, this, physicalDevice, presentQueueCond);

			if(!queueManager.hasGraphicsSupport()) throw new Error("Could not find any graphics queue.");
			if(!queueManager.hasPresentSupport()) throw new Error("Could not find any present queue.");

			// device extensions
			List<String> deviceExtensions = new ArrayList<>();
			if(queueManager.hasPresentSupport()) {
				deviceExtensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
			}

			device = VulkanUtils.createDevice(stack, physicalDevice, deviceExtensions, queueManager.getQueueIndices());

			queueManager.registerQueues();

			setSurface(surface);

			memoryManager = new VulkanMemoryManager(stack, this);

			graphCommandBuffer = queueManager.createGraphicsCommandBuffer();
			memCommandBuffer = queueManager.createGraphicsCommandBuffer();
			fbCommandBuffer = queueManager.createPresentCommandBuffer();

			swapchainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
					.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
					.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT|VK_IMAGE_USAGE_TRANSFER_SRC_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT)
					.presentMode(VK_PRESENT_MODE_FIFO_KHR) // must be supported by all drivers
					.imageArrayLayers(1)
					.clipped(false);

			if(queueManager.hasUniversalQueue()) {
				swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
			} else {
				swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
						.pQueueFamilyIndices(stack.ints(queueManager.getGraphicsIndex(), queueManager.getPresentIndex()));
			}

			framebufferCreateInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
					.layers(1);


			final Map<Integer, Integer> universalAllocateAmounts = new HashMap<>();
			universalAllocateAmounts.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VulkanUtils.ALLOCATE_UBO_SLOTS);
			universalAllocateAmounts.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VulkanUtils.ALLOCATE_TEXTURE_SLOTS);

			universalDescPool = new VulkanDescriptorPool(device, VulkanUtils.ALLOCATE_SET_SLOTS, universalAllocateAmounts);


			final Map<Integer, Integer> textureAllocateAmounts = new HashMap<>();
			textureAllocateAmounts.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VulkanUtils.TEXTURE_POOL_SIZE);

			textureDescPool = new VulkanDescriptorPool(device, VulkanUtils.TEXTURE_POOL_SIZE, textureAllocateAmounts);


			final Map<Integer, Integer> multiAllocateAmounts = new HashMap<>();
			multiAllocateAmounts.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VulkanUtils.MULTI_POOL_SIZE);

			multiDescPool = new VulkanDescriptorPool(device, VulkanUtils.MULTI_POOL_SIZE, multiAllocateAmounts);


			VkDescriptorSetLayoutBinding.Buffer textureBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
			textureBindings.get(0).set(0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, VK_SHADER_STAGE_FRAGMENT_BIT, null);

			textureDescLayout = new VulkanDescriptorSetLayout(device, textureBindings);


			VkDescriptorSetLayoutBinding.Buffer multiBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
			multiBindings.get(0).set(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1, VK_SHADER_STAGE_VERTEX_BIT, null);

			multiDescLayout = new VulkanDescriptorSetLayout(device, multiBindings);


			pipelineManager = new VulkanPipelineManager(stack, this, universalDescPool, renderPass, graphCommandBuffer);


			LongBuffer semaphoreBfr = stack.callocLong(1);
			fetchFramebufferSemaphore = VulkanUtils.createSemaphore(semaphoreBfr, device);
			presentFramebufferSemaphore = VulkanUtils.createSemaphore(semaphoreBfr, device);


			VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack)
					.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
					.magFilter(VK_FILTER_NEAREST)
					.minFilter(VK_FILTER_NEAREST)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
					.minLod(0)
					.maxLod(0)
					.compareEnable(false)
					.anisotropyEnable(false)
					.unnormalizedCoordinates(false);
			samplers[ETextureType.NEAREST_FILTER.ordinal()] = VulkanUtils.createSampler(stack, samplerCreateInfo, device);

			samplerCreateInfo.minFilter(VK_FILTER_LINEAR)
					.magFilter(VK_FILTER_LINEAR);
			samplers[ETextureType.LINEAR_FILTER.ordinal()] = VulkanUtils.createSampler(stack, samplerCreateInfo, device);

			int globalUniformBufferSize = 4*4*4*(VulkanUtils.MAX_GLOBALTRANS_COUNT+1); // mat4+(1+MAX_GLOBALTRANS_COUNT)
			globalUniformStagingBuffer = memoryManager.createBuffer(globalUniformBufferSize, EVulkanMemoryType.STAGING, EVulkanBufferUsage.UNIFORM_BUFFER);
			globalUniformBufferData = BufferUtils.createByteBuffer(globalUniformBufferSize);
			globalUniformBuffer = memoryManager.createBuffer(globalUniformBufferSize,EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);
			// mat4
			backgroundUniformBfr = memoryManager.createBuffer(4*4*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);
			unifiedUniformBfr = memoryManager.createBuffer(4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);

			if(globalUniformBuffer == null || backgroundUniformBfr == null || unifiedUniformBfr == null) throw new Error("Could not create uniform buffers.");

			pipelineManager.installUniformBuffers(globalUniformBuffer, backgroundUniformBfr, unifiedUniformBfr);

			unifiedArrayBfr = memoryManager.createMultiBuffer(2*100*4*4, EVulkanMemoryType.DYNAMIC, EVulkanBufferUsage.VERTEX_BUFFER);

		} finally {
			if(unifiedUniformBfr == null) invalidate();
		}
	}

	@Override
	public void invalidate() {
		closeMutex.acquireUninterruptibly();
		resourceMutex.acquireUninterruptibly();
		closeMutex.release();

		for(long sampler : samplers) {
			if(sampler != 0) vkDestroySampler(device, sampler, null);
		}

		if(presentFramebufferSemaphore != VK_NULL_HANDLE) vkDestroySemaphore(device, presentFramebufferSemaphore, null);
		if(fetchFramebufferSemaphore != VK_NULL_HANDLE) vkDestroySemaphore(device, fetchFramebufferSemaphore, null);
		if(swapchain != VK_NULL_HANDLE) {
			destroyFramebuffers(-1);
			destroySwapchainViews(-1);
			vkDestroySwapchainKHR(device, swapchain, null);
		}

		if(pipelineManager != null) pipelineManager.destroy();
		if(multiDescLayout != null) multiDescLayout.destroy();
		if(textureDescLayout != null) textureDescLayout.destroy();
		if(multiDescPool != null) multiDescPool.destroy();
		if(textureDescPool != null) textureDescPool.destroy();
		if(universalDescPool != null) universalDescPool.destroy();

		memoryManager.destroy();

		if(renderPass != VK_NULL_HANDLE) vkDestroyRenderPass(device, renderPass, null);
		commandBufferRecording = false;

		queueManager.destroy();

		if(device != null) vkDestroyDevice(device, null);

		fbCommandBuffer = null;
		memCommandBuffer = null;
		graphCommandBuffer = null;

		presentFramebufferSemaphore = VK_NULL_HANDLE;
		fetchFramebufferSemaphore = VK_NULL_HANDLE;
		swapchain = VK_NULL_HANDLE;
		device = null;
		super.invalidate();
		resourceMutex.release();
	}

	@Override
	public void setShadowDepthOffset(float depth) {
		unifiedUniformBfrData.putFloat(0, depth);
		unifiedDataUpdated = true;
	}

	private void updateUnifiedStatic() {
		if(unifiedDataUpdated) {
			updateBufferAt(unifiedUniformBfr, 0, unifiedUniformBfrData);
			unifiedDataUpdated = false;
		}
	}

	protected AbstractVulkanBuffer unifiedUniformBfr = null;
	private final ByteBuffer unifiedUniformBfrData = BufferUtils.createByteBuffer(4);
	private boolean unifiedDataUpdated = false;

	@Override
	public TextureHandle generateTexture(int width, int height, ShortBuffer data, String name) {
		return generateTextureInternal(width, height, data, 0L);
	}

	private TextureHandle generateTextureInternal(int width, int height, ShortBuffer data, long descSet) {
		if(!commandBufferRecording) return null;

		if(width == 0) width = 1;
		if(height == 0) height = 1;

		VulkanTextureHandle vkTexHandle = createTexture(width, height, descSet);
		changeLayout(vkTexHandle.getImage(), VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, true);

		if(data != null) updateTexture(vkTexHandle, 0, 0, width, height, data);
		return vkTexHandle;
	}

	@Override
	protected void drawMulti(MultiDrawHandle call) {
		if(!commandBufferRecording || call == null || call.drawCalls == null || call.sourceQuads == null) return;

		updateUnifiedStatic();

		AbstractVulkanBuffer vkDrawCalls = (AbstractVulkanBuffer) call.drawCalls;

		pipelineManager.bind(EVulkanPipelineType.UNIFIED_MULTI);

		AbstractVulkanBuffer vkQuads = (AbstractVulkanBuffer)call.sourceQuads.vertices;

		long verticesDescSet = multiDescriptorSets.computeIfAbsent(vkQuads, this::createMultiDescriptorSet);

		pipelineManager.bindDescSets(getTextureDescSet(call.sourceQuads.texture), verticesDescSet);

		pipelineManager.bindVertexBuffers(vkDrawCalls.getBufferIdVk());

		vkCmdDraw(graphCommandBuffer, 4, call.used, 0, 0);

		((VulkanMultiBufferHandle)call.drawCalls).inc();
	}

	private final Map<AbstractVulkanBuffer, Long> multiDescriptorSets = new HashMap<>();

	private final VulkanMultiBufferHandle unifiedArrayBfr;
	private final ByteBuffer unifiedArrayStaging = BufferUtils.createByteBuffer(2*100*4*4);

	@Override
	protected void drawUnifiedArray(UnifiedDrawHandle call, int primitive, int vertexCount, float[] trans, float[] colors, int array_len) {
		if(!commandBufferRecording || call == null || call.vertices == null) return;

		updateUnifiedStatic();

		if(primitive != EPrimitiveType.Quad) throw new Error("not implemented primitive: " + primitive);

		FloatBuffer data = unifiedArrayStaging.asFloatBuffer();

		data.put(colors, 0, array_len*4);
		data.position(4*100);
		data.put(trans, 0, array_len*4);
		updateBufferAt(unifiedArrayBfr, 0, unifiedArrayStaging);

		pipelineManager.bind(EVulkanPipelineType.UNIFIED_ARRAY);

		long vb = ((AbstractVulkanBuffer)call.vertices).getBufferIdVk();
		pipelineManager.bindVertexBuffers(vb, vb, unifiedArrayBfr.getBufferIdVk());
		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		vkCmdDraw(graphCommandBuffer, vertexCount, array_len, call.offset, 0);
		unifiedArrayBfr.inc();
	}

	private final Map<Integer, VulkanBufferHandle> lineIndexBfr = new HashMap<>();

	@Override
	protected void drawUnified(UnifiedDrawHandle call, int primitive, int vertices, int mode, float x, float y, float z, float sx, float sy, AbstractColor color, float intensity) {
		if(!commandBufferRecording || call == null || call.vertices == null) return;

		updateUnifiedStatic();

		if(primitive == EPrimitiveType.Triangle || primitive == EPrimitiveType.Quad) {
			pipelineManager.bind(EVulkanPipelineType.UNIFIED_QUAD);
		} else {
			pipelineManager.bind(EVulkanPipelineType.UNIFIED_LINE);
		}

		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		long vb = ((AbstractVulkanBuffer)call.vertices).getBufferIdVk();
		pipelineManager.bindVertexBuffers(vb, vb);

		ByteBuffer unifiedPushConstants = pipelineManager.getPushConstantBfr();
		// 4 padding bytes

		unifiedPushConstants.putFloat(4, sx);
		unifiedPushConstants.putFloat(8, sy);
		unifiedPushConstants.putFloat(12, x);
		unifiedPushConstants.putFloat(16, y);
		unifiedPushConstants.putFloat(20, z);

		if(color != null) {
			unifiedPushConstants.putFloat(28, color.red);
			unifiedPushConstants.putFloat(32, color.green);
			unifiedPushConstants.putFloat(36, color.blue);
			unifiedPushConstants.putFloat(40, color.alpha);
		} else {
			unifiedPushConstants.putFloat(28, 1);
			unifiedPushConstants.putFloat(32, 1);
			unifiedPushConstants.putFloat(36, 1);
			unifiedPushConstants.putFloat(40, 1);
		}

		unifiedPushConstants.putFloat(44, intensity);
		unifiedPushConstants.putInt(48, mode);

		pipelineManager.pushConstants();

		if(primitive == EPrimitiveType.Triangle) {
			vkCmdDraw(graphCommandBuffer, vertices, 1, call.offset, 0);
		} else if(primitive == EPrimitiveType.Quad) {
			vkCmdDraw(graphCommandBuffer, 4, 1, call.offset, 0);
		} else {
			VulkanBufferHandle indexBfr = lineIndexBfr.get(vertices);

			if(indexBfr == null) {
				ByteBuffer indices = BufferUtils.createByteBuffer((vertices)*2*4);
				IntBuffer data = indices.asIntBuffer();

				for(int i = 0; i != vertices; i++) {
					data.put(i*2, i);
					data.put(i*2+1, (i+1)%vertices);
				}

				indexBfr = memoryManager.createBuffer(indices.remaining(), EVulkanMemoryType.STATIC, EVulkanBufferUsage.INDEX_BUFFER);
				updateBufferAt(indexBfr, 0, indices);

				lineIndexBfr.put(vertices, indexBfr);
			}

			vkCmdBindIndexBuffer(graphCommandBuffer, indexBfr.getBufferIdVk(), 0, VK_INDEX_TYPE_UINT32);
			vkCmdDrawIndexed(graphCommandBuffer, (vertices+(primitive==EPrimitiveType.LineLoop?0:-1))*2, 1, 0, call.offset, 0);
		}
	}

	@Override
	public void drawBackground(BackgroundDrawHandle call) {
		if(!commandBufferRecording || call == null || call.texture == null || call.vertices == null || call.colors == null) return;

		AbstractVulkanBuffer vkShape = (AbstractVulkanBuffer) call.vertices;
		AbstractVulkanBuffer vkColor = (AbstractVulkanBuffer) call.colors;

		pipelineManager.bind(EVulkanPipelineType.BACKGROUND);

		if(backgroundDataUpdated) {
			updateBufferAt(backgroundUniformBfr, 0, backgroundUniformBfrData);
			backgroundDataUpdated = false;
		}

		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		pipelineManager.bindVertexBuffers(vkShape.getBufferIdVk(), vkColor.getBufferIdVk());

		int starti = call.offset < 0 ? (int)Math.ceil(-call.offset/(float)call.stride) : 0;
		int draw_lines = call.lines-starti;

		int triangleCount = ((AbstractVulkanBuffer) call.vertices).getSize()/20;

		for (int i = 0; i != draw_lines; i++) {
			int lineStart = (call.offset+call.stride*(i+starti))*3;
			int lineLen = call.width*3;
			if(lineStart >= triangleCount) break;
			else if(lineStart+lineLen >= triangleCount) lineLen = triangleCount-lineStart;

			vkCmdDraw(graphCommandBuffer, lineLen, 1, lineStart, 0);
		}
	}

	@Override
	public void setHeightMatrix(float[] matrix) {
		backgroundUniformBfrData.asFloatBuffer().put(matrix, 0, 16);
		backgroundDataUpdated = true;
	}

	protected final AbstractVulkanBuffer backgroundUniformBfr;
	private final ByteBuffer backgroundUniformBfrData = BufferUtils.createByteBuffer((4*4+2*4+1)*4);
	private boolean backgroundDataUpdated = false;


	protected int globalAttrIndex = 0;
	private final Matrix4f global = new Matrix4f();

	@Override
	public void setGlobalAttributes(float x, float y, float z, float sx, float sy, float sz) {
		if(!commandBufferRecording) return;

		if(globalAttrIndex == VulkanUtils.MAX_GLOBALTRANS_COUNT) throw new Error("Out of globalTrans slots: increase VulkanUtils.MAX_GLOBALTRANS_COUNT");
		globalAttrIndex++;

		finishFrame();

		global.identity();
		global.scale(sx, sy, sz);
		global.translate(x, y, z);
		global.get(4*4*4*(globalAttrIndex+1), globalUniformBufferData);

		if(pipelineManager.isPipelineBound()) pipelineManager.pushGlobalAttr();
	}

	@Override
	public void updateTexture(TextureHandle handle, List<int[]> diff, ByteBuffer data) {
		if(!commandBufferRecording || handle == null) return;

		VulkanTextureHandle vkTexture = (VulkanTextureHandle)handle;
		if(!vkTexture.isValid()) return;

		int stagingPos = prepareStagingData(data);

		int count = diff.size();
		VkBufferImageCopy.Buffer regions = VkBufferImageCopy.create(count);
		for(int i = 0; i != count; i++) {
			VkBufferImageCopy imageCopy = regions.get(i);
			int[] original = diff.get(i);
			imageCopy.imageOffset().set(original[0], original[1], 0);
			imageCopy.imageExtent().set(original[2], original[3], 1);
			imageCopy.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
			imageCopy.bufferOffset(stagingPos+original[4]).bufferRowLength(original[2]);
		}

		changeLayout(vkTexture.getImage(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, true);
		vkCmdCopyBufferToImage(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkTexture.getImage().getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
		changeLayout(vkTexture.getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, true);
	}

	@Override
	public void updateTexture(TextureHandle textureIndex, int left, int bottom, int width, int height, ShortBuffer data) {
		if(!commandBufferRecording || textureIndex == null || width == 0 || height == 0) return;

		VulkanTextureHandle vkTexture = (VulkanTextureHandle) textureIndex;
		if(!vkTexture.isValid()) return;

		int stagingPos = prepareStagingData(data);

		VkBufferImageCopy.Buffer region = VkBufferImageCopy.create(1);
		region.get(0).imageOffset().set(left, bottom, 0);
		region.get(0).imageExtent().set(width, height, 1);
		region.get(0).imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
		region.get(0).bufferOffset(stagingPos).bufferRowLength(width);

		changeLayout(vkTexture.getImage(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, true);
		vkCmdCopyBufferToImage(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkTexture.getImage().getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
		changeLayout(vkTexture.getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, true);
	}

	@Override
	public TextureHandle resizeTexture(TextureHandle textureIndex, int width, int height, ShortBuffer data) {
		if(textureIndex == null) return null;

		((VulkanTextureHandle)textureIndex).setDestroy();
		if(!commandBufferRecording) return null;

		return generateTextureInternal(width, height, data, ((VulkanTextureHandle)textureIndex).descSet);
	}


	private final VkImageMemoryBarrier.Buffer layoutTransition = VkImageMemoryBarrier.create(1)
			.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
			.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT)
			.dstAccessMask(VK_ACCESS_MEMORY_WRITE_BIT|VK_ACCESS_MEMORY_READ_BIT);

	private void changeLayout(VulkanImage texture, int oldLayout, int newLayout, boolean memOrFB) {
		changeLayout(texture.getImage(), oldLayout, newLayout, memOrFB);
	}

	private void changeLayout(long texture, int oldLayout, int newLayout, boolean memOrFB) {
		if(!commandBufferRecording) return;

		layoutTransition.image(texture)
				.oldLayout(oldLayout)
				.newLayout(newLayout);

		layoutTransition.subresourceRange().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);

		vkCmdPipelineBarrier(memOrFB?memCommandBuffer:fbCommandBuffer, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, layoutTransition);
	}

	protected int usedStagingMemory = 0;
	protected int stagingBufferIndex = 0;
	protected final List<VulkanBufferHandle> stagingBuffers = new ArrayList<>();

	private int prepareStagingData(Buffer data) {
		ByteBuffer bdata = (data instanceof ByteBuffer)?(ByteBuffer)data : null;
		ShortBuffer sdata = (data instanceof ShortBuffer)?(ShortBuffer)data : null;


		int size;
		if(bdata != null) size = bdata.remaining();
		else if(sdata != null) size = sdata.remaining()*2;
		else throw new Error("Not yet implemented Buffer variant: " + data.getClass().getName());

		do {
			if(stagingBuffers.size() == stagingBufferIndex) {
				int newSize = 1024*(1<<stagingBufferIndex); // aka 1kB * 2^index

				if(newSize < size) newSize = size; // don't create a too small buffer

				stagingBuffers.add(stagingBufferIndex, memoryManager.createBuffer(newSize, EVulkanMemoryType.STAGING, EVulkanBufferUsage.NONE));
			} else {
				if(usedStagingMemory+size > stagingBuffers.get(stagingBufferIndex).getSize()) {
					stagingBufferIndex++;
					usedStagingMemory = 0;
				} else {
					break;
				}
			}
		} while(true);

		VulkanBufferHandle currentStagingBuffer = stagingBuffers.get(stagingBufferIndex);

		ByteBuffer mapped = currentStagingBuffer.map(usedStagingMemory);

		if(bdata != null) mapped.put(bdata.asReadOnlyBuffer());
		if(sdata != null) mapped.asShortBuffer().put(sdata.asReadOnlyBuffer());

		currentStagingBuffer.unmap();

		int offset = usedStagingMemory;
		usedStagingMemory += size;
		// bufferOffset must be a multiple of 4
		usedStagingMemory -= -usedStagingMemory%4;

		return offset;
	}

	private final VkBufferCopy.Buffer update_buffer_region = VkBufferCopy.create(1);

	@Override
	public void updateBufferAt(BufferHandle handle, int pos, ByteBuffer data) {
		if(!commandBufferRecording || handle == null || data.remaining() == 0) return;

		AbstractVulkanBuffer vkBuffer = (AbstractVulkanBuffer) handle;

		if(vkBuffer.getType() == EVulkanMemoryType.STATIC) {

			if (data.remaining() >= 65536) {
				int writePos = prepareStagingData(data);
				update_buffer_region.get(0).set(writePos, pos, data.remaining());
				vkCmdCopyBuffer(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkBuffer.getBufferIdVk(), update_buffer_region);
			} else {
				vkCmdUpdateBuffer(memCommandBuffer, vkBuffer.getBufferIdVk(), pos, data);
			}

			syncQueues(vkBuffer.getEvent(), vkBuffer.getBufferIdVk());
		} else {
			ByteBuffer mapped = vkBuffer.map();
			mapped.put(data.asReadOnlyBuffer());

			vkBuffer.unmap();
			vkBuffer.flushChanges(pos, data.remaining());
		}
	}

	@Override
	public void updateBufferAt(BufferHandle handle, List<Integer> pos, List<Integer> len, ByteBuffer data) {
		if(!commandBufferRecording || handle == null) return;

		AbstractVulkanBuffer vkBuffer = (AbstractVulkanBuffer)handle;

		int writePos = prepareStagingData(data);
		int count = pos.size();
		VkBufferCopy.Buffer update_buffer_regions = VkBufferCopy.create(count);
		for(int i = 0; i != count; i++) {
			int off = pos.get(i);
			update_buffer_regions.get(i).set(writePos+off, off, len.get(i));
		}

		vkCmdCopyBuffer(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkBuffer.getBufferIdVk(), update_buffer_regions);
		syncQueues(vkBuffer.getEvent(), vkBuffer.getBufferIdVk());
	}

	private int consumedTexSlots = 0;

	private VulkanTextureHandle createTexture(int width, int height, long descSet) {
		VulkanImage image = memoryManager.createImage(width, height, EVulkanImageType.COLOR_IMAGE);

		long textureDescSet = descSet;

		if(textureDescSet == 0) { // only color images can be used
			textureDescSet = textureDescPool.createNewSet(textureDescLayout);
		}

		VulkanTextureHandle vkTexHandle = new VulkanTextureHandle(this, memoryManager, consumedTexSlots, image, textureDescSet);

		if(descSet == 0) {
			vkTexHandle.tick();
		}

		if(descSet == 0) consumedTexSlots++;
		textures.add(vkTexHandle);
		return vkTexHandle;
	}


	private final LongBuffer syncQueueBfr = BufferUtils.createLongBuffer(1);
	private final VkBufferMemoryBarrier.Buffer synQueueArea = VkBufferMemoryBarrier.create(1)
			.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
			.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.size(VK_WHOLE_SIZE)
			.offset(0);

	private void syncQueues(long event, long buffer) {
		syncQueueBfr.put(0, event);
		vkCmdSetEvent(memCommandBuffer, event, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT);
		synQueueArea.srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT).dstAccessMask(VK_ACCESS_MEMORY_READ_BIT).buffer(buffer);
		vkCmdWaitEvents(graphCommandBuffer, syncQueueBfr, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT, null, synQueueArea, null);
	}

	@Override
	public BackgroundDrawHandle createBackgroundDrawCall(int vertices, TextureHandle texture) {
		AbstractVulkanBuffer vertexBfr = memoryManager.createBuffer(vertices*5*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.VERTEX_BUFFER);
		AbstractVulkanBuffer colorBfr = memoryManager.createBuffer(vertices*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.VERTEX_BUFFER);
		return new BackgroundDrawHandle(this, -1, texture, vertexBfr, colorBfr);
	}

	@Override
	public UnifiedDrawHandle createUnifiedDrawCall(int vertices, String name, TextureHandle texture, float[] data) {
		BufferHandle vertexBuffer = memoryManager.createBuffer(vertices*(texture!=null?4:2)*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.VERTEX_BUFFER);
		if (data != null) {
			try(MemoryStack stack = MemoryStack.stackPush()) {
				ByteBuffer dataBfr = stack.malloc(data.length*4);
				dataBfr.asFloatBuffer().put(data);
				updateBufferAt(vertexBuffer, 0, dataBfr);
			}
		}

		return new UnifiedDrawHandle(this, -1, 0, vertices, texture, vertexBuffer);
	}

	@Override
	protected MultiDrawHandle createMultiDrawCall(String name, ManagedHandle source) {
		VulkanMultiBufferHandle drawCallBuffer = memoryManager.createMultiBuffer(MultiDrawHandle.MAX_CACHE_ENTRIES*12*4, EVulkanMemoryType.DYNAMIC, EVulkanBufferUsage.VERTEX_BUFFER);
		drawCallBuffer.reset();
		return new MultiDrawHandle(this, managedHandles.size(), MultiDrawHandle.MAX_CACHE_ENTRIES, source, drawCallBuffer);
	}

	@Override
	public void clearDepthBuffer() {
		if(!commandBufferRecording) return;
		finishFrame();

		VkClearAttachment.Buffer clearAttachment = VkClearAttachment.create(1);
		clearAttachment.get(0).set(VK_IMAGE_ASPECT_DEPTH_BIT, 1, CLEAR_VALUES.get(1));
		VkClearRect.Buffer clearRect = VkClearRect.create(1).layerCount(1).baseArrayLayer(0);
		clearRect.rect().extent().set(fbWidth, fbHeight);

		vkCmdClearAttachments(graphCommandBuffer, clearAttachment, clearRect);
	}

	private long swapchain = VK_NULL_HANDLE;
	private long[] swapchainImages;
	private long[] swapchainViews;
	private long[] framebuffers;
	private final VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.create();
	private final VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.create();
	private final VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.create();

	private void destroySwapchainViews(int count) {
		if(swapchainViews == null) return;
		if(count == -1) count = swapchainViews.length;

		for(int i = 0; i != count; i++) {
			vkDestroyImageView(device, swapchainViews[i], null);
		}
		swapchainViews = null;
	}

	private void destroyFramebuffers(int count) {
		if(framebuffers == null) return;
		if(count == -1) count = framebuffers.length;

		for(int i = 0; i != count; i++) {
			vkDestroyFramebuffer(device, framebuffers[i], null);
		}
		framebuffers = null;
	}

	private VulkanImage depthImage = null;
	protected final AbstractVulkanBuffer globalUniformStagingBuffer;
	protected final AbstractVulkanBuffer globalUniformBuffer;
	private final ByteBuffer globalUniformBufferData;
	private final Matrix4f projMatrix = new Matrix4f();

	private int newWidth;
	private int newHeight;
	private boolean resizeScheduled = false;

	@Override
	public void resize(int width, int height) {
		newWidth = width;
		newHeight = height;
		resizeScheduled = true;
	}

	public void removeSurface() {
		destroyFramebuffers(-1);
		destroySwapchainViews(-1);
		vkDestroySwapchainKHR(device, swapchain, null);
		vkDestroySurfaceKHR(instance, this.surface, null);
		vkDestroyRenderPass(device, renderPass, null);

		swapchain = VK_NULL_HANDLE;
		renderPass = VK_NULL_HANDLE;
	}

	public void setSurface(long surface) {
		this.surface = surface;

		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkSurfaceFormatKHR.Buffer allSurfaceFormats = VulkanUtils.listSurfaceFormats(stack, physicalDevice, surface);
			VkSurfaceFormatKHR surfaceFormat = VulkanUtils.findSurfaceFormat(allSurfaceFormats);
			this.surfaceFormat = surfaceFormat.format();

			IntBuffer present = stack.callocInt(1);
			vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueManager.getPresentIndex(), surface, present);
			if(present.get(0) == 0) {
				System.err.println("[VULKAN] can't present anymore");
				return;
			}

			renderPass = VulkanUtils.createRenderPass(stack, device, surfaceFormat.format());
			renderPassBeginInfo.renderPass(renderPass);
			framebufferCreateInfo.renderPass(renderPass);

			swapchainCreateInfo.surface(surface)
					.imageColorSpace(surfaceFormat.colorSpace())
					.imageFormat(surfaceFormat.format());
		}
	}

	private void doResize(int width, int height) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			destroyFramebuffers(-1);
			destroySwapchainViews(-1);

			if (vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, surfaceCapabilities) != VK_SUCCESS) {
				return;
			}

			fbWidth = width;
			fbHeight = height;
			int imageCount = surfaceCapabilities.minImageCount() + 1;

			VkExtent2D minDim = surfaceCapabilities.maxImageExtent();
			VkExtent2D maxDim = surfaceCapabilities.maxImageExtent();

			fbWidth = Math.max(Math.min(fbWidth, maxDim.width()), minDim.width());
			fbHeight = Math.max(Math.min(fbHeight, maxDim.height()), minDim.height());
			if (surfaceCapabilities.maxImageCount() != 0)
				imageCount = Math.min(imageCount, surfaceCapabilities.maxImageCount());

			swapchainCreateInfo.preTransform(surfaceCapabilities.currentTransform())
					.minImageCount(imageCount)
					.oldSwapchain(swapchain)
					.imageExtent()
					.width(fbWidth)
					.height(fbHeight);

			LongBuffer swapchainBfr = stack.callocLong(1);
			boolean error = vkCreateSwapchainKHR(device, swapchainCreateInfo, null, swapchainBfr) != VK_SUCCESS;
			vkDestroySwapchainKHR(device, swapchain, null);

			if (error) {
				swapchain = VK_NULL_HANDLE;
				return;
			} else {
				swapchain = swapchainBfr.get(0);
			}

			swapchainImages = VulkanUtils.getSwapchainImages(device, swapchain);
			if (swapchainImages == null) {
				vkDestroySwapchainKHR(device, swapchain, null);
				swapchain = VK_NULL_HANDLE;
				return;
			}

			if(depthImage != null) {
				depthImage.destroy();
			}
			depthImage = memoryManager.createImage(fbWidth, fbHeight, EVulkanImageType.DEPTH_IMAGE);

			swapchainViews = new long[swapchainImages.length];
			for (int i = 0; i != swapchainImages.length; i++) {
				long imageView;
				try {
					imageView = VulkanUtils.createImageView(device, swapchainImages[i], surfaceFormat, VK_IMAGE_ASPECT_COLOR_BIT);
				} catch(Throwable thrown) {
					thrown.printStackTrace();
					destroySwapchainViews(i);
					vkDestroySwapchainKHR(device, swapchain, null);
					swapchain = VK_NULL_HANDLE;
					return;
				}
				swapchainViews[i] = imageView;
			}



			framebufferCreateInfo.width(fbWidth)
					.height(fbHeight);

			LongBuffer framebufferBfr = stack.callocLong(1);
			framebuffers = new long[swapchainViews.length];
			for (int i = 0; i != swapchainViews.length; i++) {
				framebufferCreateInfo.pAttachments(stack.longs(swapchainViews[i], depthImage.getImageView()));

				if (vkCreateFramebuffer(device, framebufferCreateInfo, null, framebufferBfr) != VK_SUCCESS) {
					destroyFramebuffers(i);
					destroySwapchainViews(-1);
					vkDestroySwapchainKHR(device, swapchain, null);
					swapchain = VK_NULL_HANDLE;
				}

				framebuffers[i] = framebufferBfr.get(0);
			}

			pipelineManager.resize(fbWidth, fbHeight);
			renderPassBeginInfo.renderArea().extent().width(fbWidth).height(fbHeight);

			projMatrix.identity();
			projMatrix.scale(1.0f, -1.0f, 1.0f);
			projMatrix.ortho(0,  width,0, height, -1, 1, true);
			projMatrix.get(0, globalUniformBufferData);
		}
	}

	private int swapchainImageIndex = -1;
	private boolean commandBufferRecording = false;

	private final VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.create().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).pClearValues(CLEAR_VALUES);
	private final static VkClearValue.Buffer CLEAR_VALUES = VkClearValue.create(2); // only zeros is equal to black
	static {
		CLEAR_VALUES.get(1).depthStencil().set(1, 0);
	}


	private static final VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

	@Override
	public void startFrame() {
		if(swapchainImageIndex != -1) endFrame();

		if(resizeScheduled) {
			doResize(newWidth, newHeight);
			resizeScheduled = false;
		}

		closeMutex.acquireUninterruptibly();
		resourceMutex.acquireUninterruptibly();
		closeMutex.release();

		try(MemoryStack stack = MemoryStack.stackPush()) {
			super.startFrame();

			if(swapchain == VK_NULL_HANDLE || framebuffers == null) {
				swapchainImageIndex = -1;
				return;
			}

			for (VulkanTextureHandle texture : textures) {
				texture.tick();
			}

			IntBuffer swapchainImageIndexBfr = stack.callocInt(1);
			int err = vkAcquireNextImageKHR(device, swapchain, -1L, fetchFramebufferSemaphore, VK_NULL_HANDLE, swapchainImageIndexBfr);
			if(err == VK_ERROR_OUT_OF_DATE_KHR || err == VK_SUBOPTIMAL_KHR) resizeScheduled = true;
			if(err != VK_SUBOPTIMAL_KHR && err != VK_SUCCESS) {
				swapchainImageIndex = -1;
				return;
			}

			swapchainImageIndex = swapchainImageIndexBfr.get(0);
			renderPassBeginInfo.framebuffer(framebuffers[swapchainImageIndex]);

			if(vkBeginCommandBuffer(graphCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) return;
			if(vkBeginCommandBuffer(memCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) {
				vkEndCommandBuffer(graphCommandBuffer);
				return;
			}
			commandBufferRecording = true;
			// reset staging buffer
			usedStagingMemory = 0;
			stagingBufferIndex = 0;
			globalAttrIndex = 0;
			memoryManager.startFrame();

			vkCmdBeginRenderPass(graphCommandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
			pipelineManager.clearLastPipeline();

			update_buffer_region.dstOffset(0).srcOffset(0).size(globalUniformBuffer.getSize());
			vkCmdCopyBuffer(memCommandBuffer, globalUniformStagingBuffer.getBufferIdVk(), globalUniformBuffer.getBufferIdVk(), update_buffer_region);
			syncQueues(globalUniformBuffer.getEvent(), globalUniformBuffer.getBufferIdVk());


			if(textDrawer == null) {
				textDrawer = new LWJGLTextDrawer(this, guiScale);
			}
		} finally {
			if(!commandBufferRecording) resourceMutex.release();
		}
	}

	private AbstractVulkanBuffer framebufferReadBack = null;
	private final VkBufferImageCopy.Buffer readBackRegion = VkBufferImageCopy.create(1);
	private int rbWidth = -1, rbHeight = -1;

	private boolean fbCBrecording = false;

	private static final VkImageSubresourceRange CLEAR_SUBRESOURCE = VkImageSubresourceRange.calloc().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);

	public void clearFramebuffer() {
		if(!commandBufferRecording) return;

		if(!fbCBrecording) {
			if(vkBeginCommandBuffer(fbCommandBuffer, commandBufferBeginInfo)!=VK_SUCCESS) return;
			fbCBrecording = true;
		}

		long image = swapchainImages[swapchainImageIndex];

		changeLayout(image, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, false);
		vkCmdClearColorImage(fbCommandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_VALUES.get(0).color(), CLEAR_SUBRESOURCE);
		changeLayout(image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, false);
	}

	public void readFramebuffer(IntBuffer pixels, int width, int height) {
		if(!commandBufferRecording) return;

		if(width > fbWidth) width = fbWidth;
		if(height > fbHeight) height = fbHeight;

		if(rbWidth != width || rbHeight != height) {
			if(framebufferReadBack != null) {
				framebufferReadBack.destroy();
			}

			rbWidth = width;
			rbHeight = height;
			framebufferReadBack = memoryManager.createBuffer(4*rbHeight*rbWidth, EVulkanMemoryType.READBACK, EVulkanBufferUsage.NONE);
		}

		readBackRegion.bufferOffset(0).bufferRowLength(width);
		readBackRegion.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
		readBackRegion.imageOffset().set(0, 0, 0);
		readBackRegion.imageExtent().set(width, height, 1);

		if(vkBeginCommandBuffer(fbCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) {
			return;
		}
		fbCBrecording = true;

		long image = swapchainImages[swapchainImageIndex];

		changeLayout(image, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, false);
		vkCmdCopyImageToBuffer(fbCommandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, framebufferReadBack.getBufferIdVk(), readBackRegion);
		changeLayout(image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, false);
		vkCmdPipelineBarrier(fbCommandBuffer, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, null, null, null);
		vkCmdClearColorImage(fbCommandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_VALUES.get(0).color(), CLEAR_SUBRESOURCE);
		changeLayout(image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, false);

		endFrame();

		IntBuffer mappedPixels = framebufferReadBack.map().asIntBuffer();
		int[] line = new int[width];
		for(int i = 0; i != height; i++) {
			mappedPixels.position(i*width);
			mappedPixels.get(line);
			pixels.position((height-i-1)*width);
			pixels.put(line);
		}
		pixels.rewind();
		framebufferReadBack.unmap();
	}

	public void endFrame() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			boolean cmdBfrSend = false;
			if(commandBufferRecording) {
				updateBufferAt(globalUniformStagingBuffer, 0, globalUniformBufferData);

				vkCmdEndRenderPass(graphCommandBuffer);
				vkEndCommandBuffer(graphCommandBuffer);
				vkEndCommandBuffer(memCommandBuffer);
				if(fbCBrecording) vkEndCommandBuffer(fbCommandBuffer);
				commandBufferRecording = false;

				VkSubmitInfo graphSubmitInfo = VkSubmitInfo.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
						.pSignalSemaphores(stack.longs(presentFramebufferSemaphore));
				if(fbCBrecording) {
					graphSubmitInfo.pCommandBuffers(stack.pointers(memCommandBuffer.address(), graphCommandBuffer.address(), fbCommandBuffer.address()));
					fbCBrecording = false;
				} else {
					graphSubmitInfo.pCommandBuffers(stack.pointers(memCommandBuffer.address(), graphCommandBuffer.address()));
				}

				if(swapchainImageIndex != -1) {
					graphSubmitInfo.pWaitSemaphores(stack.longs(fetchFramebufferSemaphore))
							.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
							.waitSemaphoreCount(1);
				}

				int error = vkQueueSubmit(queueManager.getGraphicsQueue(), graphSubmitInfo, VK_NULL_HANDLE);
				if(error != VK_SUCCESS) {
					// whatever
					System.out.println("Could not submit CommandBuffers: " + error);
				} else {
					cmdBfrSend = true;
				}
			}

			if(swapchainImageIndex != -1) {
				VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
						.pImageIndices(stack.ints(swapchainImageIndex))
						.swapchainCount(1)
						.pSwapchains(stack.longs(swapchain));
				if(cmdBfrSend) presentInfo.pWaitSemaphores(stack.longs(presentFramebufferSemaphore));
				if(vkQueuePresentKHR(queueManager.getPresentQueue(), presentInfo) != VK_SUCCESS) {
					// should not happen but we can't do anything about it
				}
				vkQueueWaitIdle(queueManager.getPresentQueue());
				swapchainImageIndex = -1;
			}
		} finally {
			resourceMutex.release();
		}
	}


	private long getTextureDescSet(TextureHandle texture) {
		if(texture == null) {
			return textures.stream().filter(tex -> tex.getTextureId() != -1).findAny().get().descSet;
		} else {
			return ((VulkanTextureHandle) texture).descSet;
		}
	}


	private long createMultiDescriptorSet(AbstractVulkanBuffer multiBuffer) {
		long descSet = multiDescPool.createNewSet(multiDescLayout);

		VkDescriptorBufferInfo.Buffer install_uniform_buffer = VkDescriptorBufferInfo.create(1);
		install_uniform_buffer.get(0).set(multiBuffer.getBufferIdVk(), 0, VK_WHOLE_SIZE);


		VkWriteDescriptorSet.Buffer install_uniform_buffer_write = VkWriteDescriptorSet.create(1)
				.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
				.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
				.pBufferInfo(install_uniform_buffer)
				.descriptorCount(1)
				.dstArrayElement(0)
				.dstBinding(0)
				.dstSet(descSet);

		vkUpdateDescriptorSets(device, install_uniform_buffer_write, null);

		return descSet;
	}

	@Override
	public int getMaxManagedQuads() {
		return super.getMaxManagedQuads();
	}

	public VkDevice getDevice() {
		return device;
	}

	public int getGlobalAttrIndex() {
		return globalAttrIndex;
	}

	public VkInstance getInstance() {
		return instance;
	}
}
