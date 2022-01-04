package go.graphics.swing.vulkan.memory;

import go.graphics.swing.vulkan.EVulkanImageType;
import go.graphics.swing.vulkan.VulkanDrawContext;
import go.graphics.swing.vulkan.VulkanUtils;
import org.lwjgl.util.vma.Vma;

public class VulkanImage {
	private final long image;
	private final long imageView;
	private final long allocation;
	private final VulkanMemoryManager memoryManager;

	public VulkanImage(VulkanDrawContext dc, VulkanMemoryManager manager, long image, long allocation, EVulkanImageType imageType) {
		this(dc, manager, image, allocation, imageType.getFormat(), imageType.getAspect());
	}

	public VulkanImage(VulkanDrawContext dc, VulkanMemoryManager manager, long image, long allocation, int format, int aspect) {
		this.image = image;
		this.imageView = VulkanUtils.createImageView(dc.getDevice(), image, format, aspect);
		this.allocation = allocation;
		this.memoryManager = manager;
	}

	public long getImage() {
		return image;
	}

	public long getImageView() {
		return imageView;
	}

	public void destroy() {
		free();
		if(memoryManager != null) {
			memoryManager.remove(this);
		}
	}

	public void free() {
		if(memoryManager != null) {
			Vma.vmaDestroyImage(memoryManager.getAllocator(), image, allocation);
		}
	}

	@Override
	public String toString() {
		return "VulkanImage{" +
				"image=" + image +
				", imageView=" + imageView +
				", allocation=" + allocation +
				", memoryManager=" + memoryManager +
				'}';
	}
}
