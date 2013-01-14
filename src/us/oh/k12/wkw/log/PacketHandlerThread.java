/*----------------------------------------------------------------------------*/
/* Copyright (c) 2012 Worthington Kilbourne Robot Club. All Rights Reserved. */
/* Open Source Software - may be modified and shared by FRC teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project. */
/*----------------------------------------------------------------------------*/
package us.oh.k12.wkw.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * thread to process a received udp packet.
 */
public class PacketHandlerThread extends Thread {

	public static final String LOG_FILE_PATH = "/tmp/";
	private static final String LOG_FILE_NAME_PREFIX = "WkwFrcLog";
	private static final String LOG_FILE_NAME_SUFIX = ".txt";

	private static Object lock = new Object();
	private static boolean logToFile = true;
	private static String logFileName = null;
	private static String logFilePath = PacketHandlerThread.LOG_FILE_PATH;

	private static final List<byte[]> packetQueue = new ArrayList<byte[]>();

	/**
	 * default constructor with packet.
	 */
	public PacketHandlerThread(final byte[] pDatagramPacketData) {
		super("PacketHandlerThread");

		if (null == pDatagramPacketData) {
			throw new IllegalArgumentException("pDatagramPacketData is null");
		}

		synchronized (PacketHandlerThread.lock) {
			PacketHandlerThread.packetQueue.add(pDatagramPacketData);
		}
	}

	public static void _init(final String pLogFilePath) {

		synchronized (PacketHandlerThread.lock) {

			PacketHandlerThread.logFileName = null;

			if ((null == pLogFilePath) || (pLogFilePath.length() == 0)) {

				PacketHandlerThread.logFilePath = PacketHandlerThread.LOG_FILE_PATH;
				PacketHandlerThread.logToFile = false;

			} else {

				PacketHandlerThread.logFilePath = pLogFilePath;

				final File aDir = new File(PacketHandlerThread.logFilePath);

				if (aDir.exists() && aDir.isDirectory()) {

					PacketHandlerThread.logToFile = true;

					PacketHandlerThread.logFileName = PacketHandlerThread.LOG_FILE_NAME_PREFIX
							+ PacketHandlerThread.setupLogFileIndex()
							+ PacketHandlerThread.LOG_FILE_NAME_SUFIX;

					System.err.println("WkwFrcLogClient logging to file '"
							+ PacketHandlerThread.logFilePath + PacketHandlerThread.logFileName
							+ "'.");

				} else {

					System.err.println("WkwFrcLogClient log file directory '"
							+ PacketHandlerThread.logFilePath
							+ "' does not exist, not logging to a file.");
					PacketHandlerThread.logToFile = false;

				}

			}

		}
	}

	private static String setupLogFileIndex() {
		final SimpleDateFormat aFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		return aFormat.format(new Date());
	}

	private boolean isLogToFile() {

		boolean isLogToFile = false;

		synchronized (PacketHandlerThread.lock) {

			isLogToFile = PacketHandlerThread.logToFile;

		}

		return isLogToFile;
	}

	private String getLogFilePath() {

		String aLogFilePath = PacketHandlerThread.LOG_FILE_PATH;

		synchronized (PacketHandlerThread.lock) {

			aLogFilePath = PacketHandlerThread.logFilePath;

		}

		return aLogFilePath;
	}

	private String getLogFileName() {
		String aLogFileName = null;

		synchronized (PacketHandlerThread.lock) {

			aLogFileName = PacketHandlerThread.logFileName;

		}

		return aLogFileName;
	}

	public void _run() {

		// System.err.println("WkwFrcLogClient run() Started.");

		byte[] aPacketData = null;
		// DatagramPacket aDatagramPacket = null;

		synchronized (PacketHandlerThread.lock) {
			if (PacketHandlerThread.packetQueue.size() > 0) {
				aPacketData = PacketHandlerThread.packetQueue.remove(0);
			}
		}

		if (null != aPacketData) {

			// byte[] aPacketData = aDatagramPacket.getData();

			final byte aMsgLength = aPacketData[0];

			final byte[] aMsgData = new byte[aMsgLength];

			for (int idx = 0; idx < aMsgLength; idx++) {
				aMsgData[idx] = aPacketData[idx + 1];
			}

			this.logMessage(new String(aMsgData));

		}

		// System.err.println("WkwFrcLogClient run() Ended.");

	}

	private void logMessage(final String pMessage) {

		if (null != pMessage) {

			System.out.println(pMessage);

			this.logToFile(pMessage);

		}
	}

	private void logToFile(final String pMessage) {

		if (this.isLogToFile()) {

			final String aPath = this.getLogFilePath();
			final String aFileName = this.getLogFileName();

			Writer aWriter = null;

			try {

				aWriter = new BufferedWriter(new FileWriter(aPath + aFileName, true));

				aWriter.write(pMessage);
				aWriter.write('\n');

				aWriter.flush();

			} catch (IOException anIoEx) {

				System.err
						.println("WkwFrcLogClient while writing to log file caught IOException with message="
								+ anIoEx.getMessage() + ".");
				anIoEx.printStackTrace(System.err);

			} finally {
				if (null != aWriter) {
					try {
						aWriter.close();
					} catch (IOException e) {
						// nothing
					}
				}
			}
		}

	}

}
