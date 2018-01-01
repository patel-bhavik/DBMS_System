package simpledb.buffer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import simpledb.file.Block;
import simpledb.file.FileMgr;
import simpledb.server.SimpleDB;

/**
 * Manages the pinning and unpinning of buffers to blocks.
 * 
 * @author Edward Sciore
 *
 */
public class BasicBufferMgr {
	
	private static final int POOL_SIZE = 8;
	/* private Buffer[] bufferpool;// array not needed */
	private int numPresent; // to keep track of the total number of buffers
	// initially available
	private int numAvailable;// to keep track currently available
	private static HashMap<Block, Buffer> bufferPoolMap;
	private HashMap<Block, long[]> LRU_Time_Map;
	private Block logBlock;

	private Block[] blocks = new Block[POOL_SIZE];
	private Buffer[] buffers = new Buffer[POOL_SIZE];
	

	public Block[] getBlocks() {
		return blocks;
	}
	
	public Buffer[] getBuffers() {
		return buffers;
	}
	
	/**
	 * Creates a buffer manager having the specified number of buffer slots. This
	 * constructor depends on both the {@link FileMgr} and
	 * {@link simpledb.log.LogMgr LogMgr} objects that it gets from the class
	 * {@link simpledb.server.SimpleDB}. Those objects are created during system
	 * initialization. Thus this constructor cannot be called until
	 * {@link simpledb.server.SimpleDB#initFileAndLogMgr(String)} or is called
	 * first.
	 * 
	 * @param numbuffs
	 *            the number of buffer slots to allocate
	 */
	BasicBufferMgr(int numbuffs) {

		/* linked hashmap is used so that buffers can be iterated in a order */
		bufferPoolMap = new LinkedHashMap<Block, Buffer>(numbuffs);
		/* Time_Map holds the two recent times the block has been pinned */
		LRU_Time_Map = new HashMap<Block, long[]>();
		numAvailable = numbuffs;
		numPresent = numbuffs;

		logBlock = new Block(SimpleDB.logMgr().getLogfile(), 1);

		// /**
		// * TODO: Associate Log page/buffer - Shrikanth
		// */
		// Page logPage = SimpleDB.logMgr().getMypage();
		// Buffer logBuffer = new Buffer();
		// logBuffer.setContents(logPage);
		// logBuffer.setLogNature(true);
		//
		// bufferPoolMap.put(

	}

	public Block getLogBlock() {
		return logBlock;
	}

	public static HashMap<Block, Buffer> getBufferPoolMap() {
		return bufferPoolMap;
	}

	/**
	 * Flushes the dirty buffers modified by the specified transaction.
	 * 
	 * @param txnum
	 *            the transaction's id number
	 */
	synchronized void flushAll(int txnum) {
		Iterator<Entry<Block, Buffer>> iterator = bufferPoolMap.entrySet().iterator();
		if (iterator.hasNext()) {
			Entry<Block, Buffer> entry = iterator.next();
			Buffer buff = entry.getValue();
			if (buff.isModifiedBy(txnum))
				buff.flush();
		}
	}

	/**
	 * Pins a buffer to the specified block. If there is already a buffer assigned
	 * to that block then that buffer is used; otherwise, an unpinned buffer from
	 * the pool is chosen. Returns a null value if there are no available buffers.
	 * 
	 * @param blk
	 *            a reference to a disk block
	 * @return the pinned buffer
	 */
	synchronized Buffer pin(Block blk) {
		long curr_time = System.currentTimeMillis();
		Buffer buff = findExistingBuffer(blk);
		int flag = 1;
		if (buff == null) {
			flag = 0;
			buff = chooseUnpinnedBuffer(curr_time);

			System.out.println("A new buffer is allocated");

			if (buff == null)
				return null;

			if (bufferPoolMap.containsKey(buff.block())) {
				bufferPoolMap.remove(buff.block());
			}

			buff.assignToBlock(blk);
		}
		if (flag == 1) {
			System.out.println("Already existing buffer is pinned");
		}
		if (!buff.isPinned())
			numAvailable--;

		bufferPoolMap.put(blk, buff);
		buff.pin();
		lru_update(blk, curr_time, buff);

		return buff;
	}

	private void lru_update(Block blk, long curr_time, Buffer buff) {
		/* if LRU_Time_Map already exist */
		if (LRU_Time_Map.containsKey(blk)) {
			// to be implemented cr
			long[] time_array = LRU_Time_Map.get(blk);
			long prev = time_array[0];
			time_array[0] = curr_time;
			time_array[1] = prev;
			LRU_Time_Map.put(blk, time_array); // update time_array_map with new Recent time
		}
		/* if LRU_Time_Map doesn't exist */
		else {
			long[] time_array = new long[2];
			time_array[0] = curr_time;
			time_array[1] = (long) -1;
			LRU_Time_Map.put(blk, time_array);
		}
	}

	/**
	 * Allocates a new block in the specified file, and pins a buffer to it. Returns
	 * null (without allocating the block) if there are no available buffers.
	 * 
	 * @param filename
	 *            the name of the file
	 * @param fmtr
	 *            a pageformatter object, used to format the new block
	 * @return the pinned buffer
	 */
	synchronized Buffer pinNew(String filename, PageFormatter fmtr) {
		Buffer buff = chooseUnpinnedBuffer(System.currentTimeMillis());
		if (buff == null)
			return null;
		buff.assignToNew(filename, fmtr);
		numAvailable--;
		bufferPoolMap.put(buff.block(), buff);
		buff.pin();
		long curr_time = System.currentTimeMillis();
		lru_update(buff.block(), curr_time, buff);
		return buff;
	}

	/**
	 * Unpins the specified buffer.
	 * 
	 * @param buff
	 *            the buffer to be unpinned
	 */
	synchronized void unpin(Buffer buff) {

		if (isLogBuffer(buff))
			return;

		buff.unpin();
		if (!buff.isPinned())
			numAvailable++;
	}

	private boolean isLogBuffer(Buffer buff) {

		return buff.isLogNature();
	}

	/**
	 * Returns the number of available (i.e. unpinned) buffers.
	 * 
	 * @return the number of available buffers
	 */
	int available() {
		return numAvailable;
	}

	private Buffer findExistingBuffer(Block blk) {

		return bufferPoolMap.get(blk);
	}

	private Buffer chooseUnpinnedBuffer(long time) {
		/*
		 * numPresent and numAvailable are not same because the later is allocated but
		 * only not pinned
		 */
		if (bufferPoolMap.size() < numPresent) {
			Buffer buffer = new Buffer();
			return buffer;
		} else {
			Buffer tmp = null;
			// long curr_time = time;
			long min = time;
			int i = 1;
			tmp = get_lru2_buff(tmp, min, i);
			if (tmp == null) {
				i = 0;
				tmp = get_lru2_buff(tmp, min, i);
			}

			// System.out.println("buffer with block number - " + tmp.block().number() + "
			// is replaced");
			return tmp;
		}
	}

	private Buffer get_lru2_buff(Buffer tmp, long min, int i) {
		long[] time_array = new long[2];
		Iterator<Entry<Block, Buffer>> iterator = bufferPoolMap.entrySet().iterator();
		while (iterator.hasNext()) {
			int negative_count = 0;
			Entry<Block, Buffer> entry = iterator.next();
			Buffer buff = entry.getValue();

			// // TODO: Shrikanth
			// if (buff.isLogNature())
			// continue;

			time_array = LRU_Time_Map.get(buff.block());
			/*
			 * if(i==1) {
			 */
			if (time_array[i] == (long) -1) {
				negative_count++;
				if (negative_count > 1)
					return null;
			}
			if (!buff.isPinned() && time_array[i] < min) {
				min = time_array[i];
				tmp = buff;
			}
			// }
			/*
			 * if(i==0) { if(time_array[i+1] != (long) -1) { if (!buff.isPinned() &&
			 * time_array[i+1] < min) { min = time_array[i+1]; tmp = buff; } } else { if
			 * (!buff.isPinned() && time_array[i] < min) { min = time_array[i]; tmp = buff;
			 * } } }
			 */

		}
		return tmp;
	}

	/**
	 * Determines whether the map has a mapping from the block to some buffer.
	 * 
	 * @paramblk the block to use as a key
	 * @return true if there is a mapping; false otherwise
	 */
	boolean containsMapping(Block blk) {
		return bufferPoolMap.containsKey(blk);
	}

	/**
	 * Returns the buffer that the map maps the specified block to.
	 * 
	 * @paramblk the block to use as a key
	 * @return the buffer mapped to if there is a mapping; null otherwise
	 */
	Buffer getMapping(Block blk) {
		return bufferPoolMap.get(blk);
	}

	public void printBuffers() {
		System.out.println("---------\t STATUS -----------\n\n");
		int i = 0;
		for (Block block : bufferPoolMap.keySet()) {

			Buffer buffer = bufferPoolMap.get(block);

			System.out.println(++i + "" + buffer.toString() + " - " + block.toString());
		}

	}
}
