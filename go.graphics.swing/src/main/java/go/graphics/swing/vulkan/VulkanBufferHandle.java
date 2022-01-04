package go.graphics.swing.vulkan;

import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK10;

import go.graphics.GLDrawContext;

public class VulkanBufferHandle extends AbstractVulkanBufferHandle {

	private final long bfr, allocation, event;
	private final int size, type;

	public VulkanBufferHandle(GLDrawContext dc, int type, long bfr, long allocation, long event, int size) {
		super(dc, -1);
		this.allocation = allocation;
		this.event = event;
		this.type = type;
		this.size = size;
		this.bfr = bfr;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public long getEvent() {
		return event;
	}

	@Override
	public long getBufferIdVk() {
		return bfr;
	}

	@Override
	public long getAllocation() {
		return allocation;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public void destroy() {
		Vma.vmaDestroyBuffer(((VulkanDrawContext)dc).allocator, bfr, allocation);
		VK10.vkDestroyEvent(((VulkanDrawContext) dc).device, event, null);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [dc=" + dc + ", allocation=" + allocation + ", bfr=" + bfr + "]";
	}
}
