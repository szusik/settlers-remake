package go.graphics;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import java.util.function.Supplier;

public class AdvancedUpdateBufferCache {
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private int bfr_data_steps;
	private Supplier<GLDrawContext> ctx_supp;
	private Supplier<BufferHandle> bfr_supp;
	private BitSet[] updated;
	private int line_width;

	public AdvancedUpdateBufferCache(ByteBuffer buffer, int bfr_data_steps, Supplier<GLDrawContext> ctx_supp, Supplier<BufferHandle> bfr_supp, int line_width) {
		this.bfr_data_steps = bfr_data_steps;
		this.line_width = line_width;
		this.ctx_supp = ctx_supp;
		this.bfr_supp = bfr_supp;
		this.readBuffer = buffer.slice();
		this.writeBuffer = buffer;

		int lines = buffer.capacity()/bfr_data_steps/line_width;
		updated = new BitSet[lines];
		for(int i = 0;i != lines;i++) updated[i] = new BitSet(line_width);
	}

	public void setPosition(int line, int x) {
		writeBuffer.position((line*line_width+x) * bfr_data_steps);
	}

	public void markLine(int line, int start, int count) {
		synchronized (updated[line]) {
			updated[line].set(start, start + count);
		}
	}

	public void clearCache() throws IllegalBufferException {
		GLDrawContext dc = ctx_supp.get();
		if(dc instanceof VkDrawContext) {

			List<Integer> start = new ArrayList<>();
			List<Integer> size = new ArrayList<>();

			int lineStart = 0;
			int lineEnd = 0;
			int globalStart = -1;
			int globalEnd = 0;
			int line = 0;
			int bfrEnd = line_width*updated.length;
			do {
				synchronized (updated[line]) {
					if (globalStart == -1) {
						lineStart = updated[line].nextSetBit(lineEnd);
						if (lineStart != -1) {
							globalStart = line * line_width + lineStart;
						} else {
							updated[line].clear();
							line++;
							lineEnd = 0;
						}
					}
					if (globalStart != -1) {
						lineEnd = updated[line].nextClearBit(lineStart);
						if (lineEnd != -1) {
							globalEnd = line * line_width + lineEnd;
							start.add(globalStart * bfr_data_steps);
							size.add((globalEnd - globalStart) * bfr_data_steps);
							globalStart = -1;
						} else {
							updated[line].clear();
							line++;
							lineStart = 0;
						}
					}
				}
			} while(globalEnd < bfrEnd && line < updated.length);

			readBuffer.position(0);
			readBuffer.limit(readBuffer.capacity());
			if(start.size() > 0) {
				((VkDrawContext) dc).updateBufferAt(bfr_supp.get(), start, size, readBuffer);
			}
		} else {
			for(int i = 0; i != updated.length; i++) clearCacheRegion(i, 0, line_width);
		}
	}

	public void clearCacheRegion(int line, int start, int end) throws IllegalBufferException {
		synchronized (updated[line]) {
			int urEnd = start;
			while (urEnd < end) {
				int urStart = updated[line].nextSetBit(urEnd);
				if (urStart > end || urStart == -1) return;
				urEnd = updated[line].nextClearBit(urStart);
				if (urEnd > end || urEnd == -1) urEnd = end;
				updateRegion(line, urStart, urEnd);
				updated[line].clear(urStart, urEnd);
			}
		}
	}

	private void updateRegion(int line, int start, int end) throws IllegalBufferException {
		start += line*line_width;
		end += line*line_width;

		readBuffer.limit(end * bfr_data_steps);
		readBuffer.position(start * bfr_data_steps);
		ctx_supp.get().updateBufferAt(bfr_supp.get(), start * bfr_data_steps, readBuffer);
		readBuffer.limit(readBuffer.capacity());
	}
}
