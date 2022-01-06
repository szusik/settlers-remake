package go.graphics.swing.contextcreator;

import go.graphics.swing.ContextContainer;
import go.graphics.swing.vulkan.VulkanUtils;
import java.awt.Canvas;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.VK10.*;

public class OffscreenVulkanContextCreator extends ContextCreator<Canvas> {

	private final VkInstance instance;
	private final long debugCallback;

	public OffscreenVulkanContextCreator(ContextContainer ac, boolean debug) {
		super(ac, debug);

		try(MemoryStack stack = MemoryStack.stackPush()) {
			List<String> extensions = new ArrayList<>();
			if(debug) {
				extensions.add(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);
			}

			instance = VulkanUtils.createInstance(stack, extensions, debug);
			debugCallback = debug ? VulkanUtils.setupDebugging(instance) : 0;

			parent.wrapNewVkContext(instance, null); //TODO
		}
	}

	@Override
	public void stop() {
		if(debug) vkDestroyDebugReportCallbackEXT(instance, debugCallback, null);
		vkDestroyInstance(instance, null);
	}

	@Override
	public void initSpecific() {
		canvas = new Canvas() {
			@Override
			public void update(Graphics graphics) {
				if (isShowing()) paint(graphics);
			}

			public void paint(Graphics graphics) {

			}
		};
	}
}
