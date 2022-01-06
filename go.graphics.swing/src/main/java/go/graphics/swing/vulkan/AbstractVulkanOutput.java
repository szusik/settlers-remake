package go.graphics.swing.vulkan;

import go.graphics.swing.vulkan.memory.VulkanImage;
import java.awt.Dimension;
import java.util.function.BiFunction;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;

public abstract class AbstractVulkanOutput {

	protected VulkanDrawContext dc;
	private long waitSemaphore;
	private long signalSemaphore;

	abstract BiFunction<VkQueueFamilyProperties, Integer, Boolean> getPresentQueueCond(VkPhysicalDevice physicalDevice);

	void init(VulkanDrawContext dc) {
		this.dc = dc;

		waitSemaphore = VulkanUtils.createSemaphore(dc.getDevice());
		signalSemaphore = VulkanUtils.createSemaphore(dc.getDevice());
	}

	void destroy() {
		if(waitSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), waitSemaphore, null);
		}
		if(signalSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), signalSemaphore, null);
		}
	}

	abstract Dimension resize(Dimension preferredSize);

	abstract void createFramebuffers(VulkanImage depthImage);

	abstract boolean startFrame();

	abstract void endFrame(boolean wait);

	abstract VulkanImage getFramebufferImage();

	abstract long getFramebuffer();


	long getWaitSemaphore() {
		return waitSemaphore;
	}

	long getSignalSemaphore() {
		return signalSemaphore;
	}
}
