package simpledb;

import java.util.Iterator;

import simpledb.buffer.BufferMgr;
import simpledb.file.Block;
import simpledb.log.BasicLogRecord;
import simpledb.log.LogMgr;
import simpledb.server.SimpleDB;
import simpledb.server.Startup;

public class TestSuite {

	public static void main(String[] args) {

		System.out.println();
		System.out.println("Starting SimpleDB (Server)....");
		try {
			Startup.main(null);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Port may still be in use restart server....");
			return;
		}

		System.out.println();
		System.out.println("Starting SimpleDB (Server).... Started!");
		System.out.println();

		System.out.println("Initializing Buffers...");

		SimpleDB.initBuffersAndLinkLogPage();

		int POOL_SIZE = SimpleDB.BUFFER_SIZE;
		// BufferMgr bufferManager = new BufferMgr(POOL_SIZE);
		BufferMgr bufferManager = SimpleDB.bufferMgr();

		// SimpleDB.initFileLogAndBufferMgr("bufferSearchTest");
		// number of buffers

		System.out.println("Testing Buffer Management...");
		bufferManager.pin(bufferManager.getBufferMgr().getBlocks()[3]);
		bufferManager.pin(bufferManager.getBufferMgr().getBlocks()[1]);
		bufferManager.pin(bufferManager.getBufferMgr().getBlocks()[6]);
		bufferManager.pin(bufferManager.getBufferMgr().getBlocks()[0]);

		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[6]);
		// bufferManager.unpin(buffers[5]);
		// bufferManager.unpin(buffers[4]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[3]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[0]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[6]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[3]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[1]);
		bufferManager.unpin(bufferManager.getBufferMgr().getBuffers()[1]);

		bufferManager.printBuffers();

		Block b = new Block("Buffer [" + POOL_SIZE + "]", (POOL_SIZE + 1));
		bufferManager.pin(b);

		bufferManager.printBuffers();

		System.out.println();
		System.out.println("Testing Buffer Management... Successful!");

		System.out.println();
		System.out.println("Testing Log Management...");

		LogMgr logmgr = SimpleDB.logMgr();

		// int lsn1 = logmgr.append(new Object[] { "a", "b" });
		// System.out.println(lsn1);
		// int lsn2 = logmgr.append(new Object[] { "c", "d" });
		// System.out.println(lsn2);
		// int lsn3 = logmgr.append(new Object[] { "e", "f" });
		// System.out.println(lsn3);
		// logmgr.flush(lsn3);

		Iterator<BasicLogRecord> iter = logmgr.iterator();

		logmgr.getMypage().setInt(12, 55);
		logmgr.getMypage().setString(19, "dbms proj");
		System.out.print(SimpleDB.bufferMgr().getBufferMgr().getBufferPoolMap()
				.get(SimpleDB.bufferMgr().getBufferMgr().getLogBlock()).getContents().getInt(12) + "\n");
		System.out.print(SimpleDB.bufferMgr().getBufferMgr().getBufferPoolMap()
				.get(SimpleDB.bufferMgr().getBufferMgr().getLogBlock()).getContents().getString(19) + "\n");

		System.out.println();
		System.out.println("Testing Log Management... Successful!");
	}

}
