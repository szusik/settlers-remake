package go.graphics.swing.vulkan;

import java.util.ArrayList;
import java.util.List;

public class VulkanMultiBufferHandle extends AbstractVulkanBufferHandle {

    private final int size;
    private final int type;
    private int bufferIndex = -1;
    private final List<VulkanBufferHandle> buffers = new ArrayList<>();

    public VulkanMultiBufferHandle(VulkanDrawContext dc, int type, int size) {
        super(dc, type);
        this.size = size;
        this.type = type;
    }

    public void inc() {
        bufferIndex++;
        if(bufferIndex == buffers.size()) buffers.add(getDC().createBuffer(size, type));
    }

    public void reset() {
        if(bufferIndex == -1) buffers.add(getDC().createBuffer(size, type));
        bufferIndex = 0;
    }

    @Override
    public long getBufferIdVk() {
        return buffers.get(bufferIndex).getBufferIdVk();
    }

    @Override
    public long getAllocation() {
        return buffers.get(bufferIndex).getAllocation();
    }

    @Override
    public long getEvent() {
        return buffers.get(bufferIndex).getEvent();
    }

    @Override
    public void destroy() {}

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[dc=" + dc + ", size=" + size + ", bufferCount=" + buffers.size() + "]";
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getSize() {
        return size;
    }
}
