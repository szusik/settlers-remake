package go.graphics.swing.vulkan;

import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import go.graphics.ETextureType;
import go.graphics.TextureHandle;

public class VulkanTextureHandle extends TextureHandle {

	private final long bfr;
	private long imageView;
	private final long allocation;
	private boolean installed = false;
	private boolean shouldDestroy = false;
	final long descSet;

	public VulkanTextureHandle(VulkanDrawContext dc, int id, long bfr, long allocation, long imageView, long descSet) {
		super(dc, id);
		this.allocation = allocation;
		this.imageView = imageView;
		this.bfr = bfr;
		this.descSet = descSet;
	}

	public long getImageViewId() {
		return imageView;
	}

	public long getTextureIdVk() {
		return bfr;
	}

	public long getAllocation() {
		return allocation;
	}

	public void destroy() {
		if(imageView == VK10.VK_NULL_HANDLE) return;

		VK10.vkDestroyImageView(((VulkanDrawContext)dc).device, imageView, null);
		Vma.vmaDestroyImage(((VulkanDrawContext)dc).allocator, bfr, allocation);
		imageView = VK10.VK_NULL_HANDLE;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [dc=" + dc + ", allocation=" + allocation + ", bfr=" + bfr + "]";
	}

	void tick() {
		if(!installed) {
			installed = true;
			install();
		}

		if(shouldDestroy) {
			destroy();
		}
	}

	private void install() {
		if(descSet == 0) return;

		VkDescriptorImageInfo.Buffer install_texture_image = VkDescriptorImageInfo.create(1)
				.imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

		VkWriteDescriptorSet.Buffer install_texture_write = VkWriteDescriptorSet.create(1)
				.sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
				.descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
				.dstBinding(0)
				.descriptorCount(1)
				.dstArrayElement(0)
				.dstSet(descSet)
				.pImageInfo(install_texture_image);


		install_texture_image.imageView(imageView)
				.sampler(((VulkanDrawContext) dc).samplers[getType().ordinal()]);

		VK10.vkUpdateDescriptorSets(((VulkanDrawContext)dc).device, install_texture_write, null);
	}

	public void setInstalled() {
		installed = true;
	}

	public void setDestroy() {
		shouldDestroy = true;
	}

	@Override
	public void setType(ETextureType type) {
		super.setType(type);
		installed = false;
	}

	@Override
	public boolean isValid() {
		return imageView != VK10.VK_NULL_HANDLE && super.isValid();
	}
}
