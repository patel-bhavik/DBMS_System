package simpledb.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import simpledb.file.Block;

/**
 * 
 * @author abhis
 *
 */
public class CustomLRU {

	private Map<Block, Buffer> pool;

	private int k = -1;

	public CustomLRU(int k, Map<Block, Buffer> pool) {

		this.k = k;
		this.pool = pool;

	}

	private long getLatestTimestamp(Buffer buffer) {

		if (!buffer.getTimestamps().isEmpty()) {

			return buffer.getTimestamps().peek();

		}

		return -1;
	}

	public static void main(String[] args) {

		Stack<Integer> ts = new Stack<Integer>();

		ts.push(123);
		ts.push(13);
		ts.push(1234);

		System.out.println(ts.get(0));
	}

	private long getSecondLastTimestamp(Buffer buffer) {

		if (buffer.getTimestamps().size() > 1) {

			return buffer.getTimestamps().get(buffer.getTimestamps().size() - 2).longValue();

		}

		return -1;
	}

	private int getPinCount(Buffer buffer) {

		return buffer.getPinCount();
	}

	private Block computeReplacement() {

		Block blockToReplace = null;

		List<Buffer> bufferList = getBufferList();

		Collections.sort(bufferList, new Comparator<Buffer>() {

			@Override
			public int compare(Buffer buffer_1, Buffer buffer_2) {

				if (buffer_1.getPinCount() == buffer_2.getPinCount()) {

				}

				return 0;
			}

		});

		return blockToReplace;
	}

	private List<Buffer> getBufferList() {

		for (Buffer buff : pool.values()) {

		}

		return null;
	}

}
