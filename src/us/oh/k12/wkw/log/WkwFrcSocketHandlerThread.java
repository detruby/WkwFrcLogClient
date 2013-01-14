/*----------------------------------------------------------------------------*/
/* Copyright (c) 2012 Worthington Kilbourne Robot Club. All Rights Reserved. */
/* Open Source Software - may be modified and shared by FRC teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project. */
/*----------------------------------------------------------------------------*/
package us.oh.k12.wkw.log;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 */
public class WkwFrcSocketHandlerThread extends Thread {

	public static final String LOG_FILE_PATH = "/tmp/";
	private static final String LOG_FILE_NAME_PREFIX = "WkwFrcLog";
	private static final String LOG_FILE_NAME_SUFIX = ".txt";

	private static Object lock = new Object();
	private static boolean logToFile = true;
	private static String logFileName = null;
	private static String logFilePath = PacketHandlerThread.LOG_FILE_PATH;

	// private static final List<String> packetQueue = new ArrayList<String>();

	// private int bufferSize;
	private Socket clientSocket;

	public WkwFrcSocketHandlerThread(final int pBufferSize, final Socket pSocket) {

		super("WkwFrcSocketHandlerThread" + Long.toString(System.currentTimeMillis()));

		if (null == pSocket) {
			throw new IllegalArgumentException("pSocket is null");
		}

		// this.bufferSize = pBufferSize;
		this.clientSocket = pSocket;

	}

	public void run() {

		DataInputStream anInputStream = null;
		PrintStream anOutputStream = null;
		int aMsgLength = 0;
		final StringBuffer aMsg = new StringBuffer();
		byte aByte = 0;
		final long aTimeout = System.currentTimeMillis() + 2000;

		// System.err.println("run() started, name=" + this.getName() + ".");

		try {

			anInputStream = new DataInputStream(this.clientSocket.getInputStream());

			try {

				aByte = anInputStream.readByte();

				while (System.currentTimeMillis() < aTimeout) {

					if (aMsgLength == 0) {

						aMsgLength = (int) aByte;
						// System.err.println("run() aMsgLength=" + aMsgLength + ".");

					} else {

						// System.err.println("run() aByte=" + aByte + ".");
						aMsg.append((char) aByte);

						if (aMsg.length() >= aMsgLength) {
							// System.err.println("run() break.");
							break;
						}

					}

					aByte = anInputStream.readByte();
				}

			} catch (EOFException anEofEx) {
				// nothing System.err.println("run() EOFException.");
			}

			anOutputStream = new PrintStream(this.clientSocket.getOutputStream());

			anOutputStream.print(2);
			anOutputStream.print("ok");
			// System.err.println("run() sent response, name=" + this.getName() + ".");
			anOutputStream.flush();

			synchronized (WkwFrcSocketHandlerThread.lock) {

				this.logMessage(aMsg.toString());

			}

		} catch (IOException anIoEx) {

			System.err
					.println("WkwFrcLogClient while reading request caught IOException with message="
							+ anIoEx.getMessage() + ".");
			anIoEx.printStackTrace(System.err);

		} catch (Exception anEx) {

			System.err.println("WkwFrcLogClient while reading request caught "
					+ anEx.getClass().getName() + " with message=" + anEx.getMessage() + ".");
			anEx.printStackTrace(System.err);

		} finally {

			if (null != anInputStream) {
				try {
					anInputStream.close();
				} catch (IOException e) {
					// nothing here
				}
			}

			if (null != anOutputStream) {
				anOutputStream.close();
			}

		}

		// System.err.println("run() ended, name=" + this.getName() + ".");
	}

	public static void init(final String pLogFilePath) {

		synchronized (WkwFrcSocketHandlerThread.lock) {

			WkwFrcSocketHandlerThread.logFileName = null;

			if ((null == pLogFilePath) || (pLogFilePath.length() == 0)) {

				WkwFrcSocketHandlerThread.logFilePath = PacketHandlerThread.LOG_FILE_PATH;
				WkwFrcSocketHandlerThread.logToFile = false;

			} else {

				WkwFrcSocketHandlerThread.logFilePath = pLogFilePath;

				final File aDir = new File(WkwFrcSocketHandlerThread.logFilePath);

				if (aDir.exists() && aDir.isDirectory()) {

					WkwFrcSocketHandlerThread.logToFile = true;

					WkwFrcSocketHandlerThread.logFileName = WkwFrcSocketHandlerThread.LOG_FILE_NAME_PREFIX
							+ WkwFrcSocketHandlerThread.setupLogFileIndex()
							+ WkwFrcSocketHandlerThread.LOG_FILE_NAME_SUFIX;

					System.err.println("WkwFrcLogClient logging to file '"
							+ WkwFrcSocketHandlerThread.logFilePath
							+ WkwFrcSocketHandlerThread.logFileName + "'.");

				} else {

					System.err.println("WkwFrcLogClient log file directory '"
							+ WkwFrcSocketHandlerThread.logFilePath
							+ "' does not exist, not logging to a file.");
					WkwFrcSocketHandlerThread.logToFile = false;

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

		synchronized (WkwFrcSocketHandlerThread.lock) {

			isLogToFile = WkwFrcSocketHandlerThread.logToFile;

		}

		return isLogToFile;
	}

	private String getLogFilePath() {

		String aLogFilePath = WkwFrcSocketHandlerThread.LOG_FILE_PATH;

		synchronized (WkwFrcSocketHandlerThread.lock) {

			aLogFilePath = WkwFrcSocketHandlerThread.logFilePath;

		}

		return aLogFilePath;
	}

	private String getLogFileName() {
		String aLogFileName = null;

		synchronized (WkwFrcSocketHandlerThread.lock) {

			aLogFileName = WkwFrcSocketHandlerThread.logFileName;

		}

		return aLogFileName;
	}

	private void logMessage(final String pMessage) {

		if ((null != pMessage) && (!"ping".equals(pMessage))) {

			if (pMessage.length() == 0) {

				System.err.println("pMessage length is zero.");

			} else {

				// System.err.println("pMessage=" + pMessage + ".");
				System.out.println(pMessage);

				this.logToFile(pMessage);
			}
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
