package go.graphics.swing.vulkan;

import java.util.function.BiFunction;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSurfaceManager {

	private long surface;

	private long waitSemaphore;
	private long signalSemaphore;
	private VulkanDrawContext dc;

	public VulkanSurfaceManager(long surface) {
		this.surface = surface;
	}

	public BiFunction<VkQueueFamilyProperties, Integer, Boolean> getPresentQueueCond(VkPhysicalDevice physicalDevice) {
		return (queue, index) -> {
			int[] present = new int[1];
			if(surface == 0) return true;

			vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, index, surface, present);
			return present[0]==1;
		};
	}

	public long getWaitSemaphore() {
		return waitSemaphore;
	}

	public long getSignalSemaphore() {
		return signalSemaphore;
	}

	public void setSurface(long surface) {
		this.surface = surface;
	}

	public void init(VulkanDrawContext dc) {
		this.dc = dc;

		waitSemaphore = VulkanUtils.createSemaphore(dc.getDevice());
		signalSemaphore = VulkanUtils.createSemaphore(dc.getDevice());

		dc.setupNewSurface();
	}

	public long getSurface() {
		return surface;
	}

	public void destroy() {
		if(waitSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), waitSemaphore, null);
		}
		if(signalSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), signalSemaphore, null);
		}
	}
}
