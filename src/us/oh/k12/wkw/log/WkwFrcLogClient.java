/*----------------------------------------------------------------------------*/
/* Copyright (c) 2012 Worthington Kilbourne Robot Club. All Rights Reserved. */
/* Open Source Software - may be modified and shared by FRC teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project. */
/*----------------------------------------------------------------------------*/
package us.oh.k12.wkw.log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import edu.wpi.first.wpilibj.networktables.NetworkTableProvider;
import edu.wpi.first.wpilibj.networktables2.client.NetworkTableClient;
import edu.wpi.first.wpilibj.networktables2.stream.IOStreamFactory;
import edu.wpi.first.wpilibj.networktables2.stream.SocketStreams;

/**
 * Class that receives NetworkTable data (log records).<br>
 * The jar is packaged as runnable, so the normal command is:<br>
 * java -jar ./WfwFrcLogClient.jar<br>
 * <br>
 * To run with (optional) arguments:<br>
 * -p portnumber (default 6595)<br>
 * -p logfilepath (default /tmp/)<br>
 * 
 * @author Dave Truby dave@truby.name
 * @version 1.0.0
 * @since 1.0.0
 */
public class WkwFrcLogClient {

	private static final String VERSION = "3.0.0";

	private static final int PORT = 6595;
	private static final int BUFFER_SIZE = 1024;

	private static final String ARG_PORT = "-p";
	private static final String ARG_TEAMNUMBER = "-t";
	private static final String ARG_IPADDRESS = "-i";
	private static final String ARG_BUFFER_SIZE = "-b";
	private static final String ARG_LOG_FILE_PATH = "-p";
	private static final String ARG_NETWORKTABLE_LISTENER = "-n";
	private static final String ARG_SOCKET_LISTENER = "-s";
	private static final String ARG_HELP = "-h";
	public static final int SERVER_PORT = 1735;
	private static final String NETWORK_TABLE_NAME_LOGGER = "WkwFrcLogger";

	private IOStreamFactory streamFactory = null;
	private NetworkTableClient networkTableClient = null;
	private NetworkTableProvider networkTableProvider = null;
	private String ipAddress = null;
	private boolean networkTableListener = true;
	private boolean socketListener = false;
	private boolean running = false;
	private int port = WkwFrcLogClient.PORT;
	private int bufferSize = WkwFrcLogClient.BUFFER_SIZE;
	private String logFilePath = "";
	private int teamNumber = 4145;
	private FrcLoggerListener loggerListener;

	/**
	 * Default null constructor.
	 */
	public WkwFrcLogClient(final String[] pArgs) {
		super();

		this.parseArguements(pArgs);

	}

	public void listen() {

		if (this.networkTableListener) {

			try {

				this.startListening();

				this.waitOnConsoleInput();

			} finally {

				this.stopListening();

			}
		}

		if (this.socketListener) {

			WkwFrcSocketHandlerThread.init(this.logFilePath);

			this.listenOnSocket();

			this.waitOnConsoleInput();

		}

	}

	private void waitOnConsoleInput() {

		System.err.println("Any key to exit:");

		final Scanner anInput = new Scanner(System.in);

		anInput.hasNext();

		anInput.close();

		System.err.println("Exiting.");
	}

	private void startListening() {

		final String anIpAddress = this.setupIpAddress(this.teamNumber,
				this.ipAddress);

		System.err.println("Using robot ip address=" + anIpAddress + ".");

		try {

			this.streamFactory = SocketStreams.newStreamFactory(anIpAddress,
					WkwFrcLogClient.SERVER_PORT);

			this.networkTableClient = new NetworkTableClient(this.streamFactory);

			this.networkTableProvider = new NetworkTableProvider(
					this.networkTableClient);

			// NetworkTable.setTeam(this.teamNumber);

			this.loggerListener = new FrcLoggerListener(this.teamNumber);

			this.networkTableProvider.getTable(
					WkwFrcLogClient.NETWORK_TABLE_NAME_LOGGER)
					.addTableListener(this.loggerListener);

			// this.networkTableClient.addTableListener(this.loggerListener,
			// false);
			// Robot.getLogger().addListenerToAll(this.loggerListener);
			// Robot.getLogger().addTableListener(this.loggerListener);
			// Robot.getLogger().addAdditionListener(this.loggerListener, true);
			// Robot.getLogger().removeAdditionListener(this.loggerListener);

		} catch (IOException anIoEx) {
			System.err.println("Caught IOException message="
					+ anIoEx.getMessage() + ".");
		}
	}

	private void stopListening() {

		if (null != this.networkTableClient) {

			if (null != this.networkTableProvider) {

				this.networkTableProvider.getTable(
						WkwFrcLogClient.NETWORK_TABLE_NAME_LOGGER)
						.removeTableListener(this.loggerListener);

				// Robot.getLogger().removeTableListener(this.loggerListener);
				// Robot.getLogger().removeAdditionListener(this.loggerListener);
				// Robot.getLogger().removeListenerFromAll(this.loggerListener);

				this.networkTableProvider.close();
				this.networkTableProvider = null;
			}

			this.networkTableClient.close();

			this.networkTableClient.stop();
			this.networkTableClient = null;

			this.streamFactory = null;

			this.loggerListener = null;
		}
	}

	private String setupIpAddress(int pTeamNumber, String pIpAddress) {
		String anIpAddress = null;

		if (this.isNotNullAndNotBlank(pIpAddress)) {

			anIpAddress = pIpAddress;

		} else {

			anIpAddress = this.getIpFromTeam(pTeamNumber);

		}

		return anIpAddress;
	}

	private String getIpFromTeam(int pTeam) {
		// add zeros if number isn't 4 digits
		String numString = String.valueOf(pTeam);
		int numDigits = numString.length();
		int zToAdd = 4 - numDigits;
		if (zToAdd > 0) {
			// team number is less than 4 digits long
			String zeros = "";
			for (int i = 0; i < zToAdd; i++) {
				zeros += "0";
			}
			numString = zeros + numString;
		}
		// convert the team number into ip
		return "10." + numString.substring(0, 1) + "."
				+ numString.substring(2, 3) + ".2";
	}

	private boolean isNotNullAndNotBlank(final String pValue) {
		return ((null != pValue) && (pValue.length() > 0));
	}

	/**
	 * Listen on the port for udp packets.
	 */
	public void listenOnSocket() {

		ServerSocket aServerSocket = null;

		try {

			// create the listen socket
			// aSocket = new MulticastSocket(this.port);
			aServerSocket = new ServerSocket(this.port);

			final InetAddress aLocalIpAddress = InetAddress.getLocalHost();
			System.err.println("WkwFrcLogClient local IP address="
					+ aLocalIpAddress.getHostAddress() + ".");

			// setup the broadcast group
			// final InetAddress aBroadcastGroup =
			// InetAddress.getByName(this.ipMulticastAddress);
			// join the group
			// aSocket.joinGroup(aBroadcastGroup);

			System.err.println("WkwFrcLogClient version "
					+ WkwFrcLogClient.VERSION + " is listening on IP port "
					+ Integer.toString(this.port)
					/*+ " for broadcast group " + aBroadcastGroup.getHostAddress() */
					+ ". Press ctrl-c to exit.");

			// DatagramPacket aPacket = null;
			Socket aClientSocket = null;
			// byte[] aBuffer = null;

			this.running = true;

			while (this.running) {

				aClientSocket = aServerSocket.accept();

				// System.err.println("Incomming socket ip="
				// + aClientSocket.getRemoteSocketAddress().toString() + ".");

				new WkwFrcSocketHandlerThread(this.bufferSize, aClientSocket)
						.start();

				/*
				while (this.running) {

					// initalize the buffer
					aBuffer = new byte[this.bufferSize];
					// setup the udp receive packet buffer
					aPacket = new DatagramPacket(aBuffer, aBuffer.length);
					// this waits until a udp packet is received
					aSocket.receive(aPacket);

					// System.err.println("WkwFrcLogClient UDP packet received.");

					new PacketHandlerThread(aPacket.getData()).start();

				}
				*/
			}

		} catch (IOException anIoEx) {

			System.err
					.println("WkwFrcLogClient main loop caught IOException with message="
							+ anIoEx.getMessage() + ", exiting.");
			anIoEx.printStackTrace(System.err);

		} catch (Exception anEx) {

			System.err.println("WkwFrcLogClient main loop caught "
					+ anEx.getClass().getName() + " with message="
					+ anEx.getMessage() + ", exiting.");
			anEx.printStackTrace(System.err);

		} finally {

			System.err.println("WkwFrcLogClient exiting.");

			if (null != aServerSocket) {
				try {
					aServerSocket.close();
				} catch (IOException e) {
					// nothing here
				}
			}
		}

	}

	private void parseArguements(final String[] pArgs) {

		// setup default values.

		this.port = WkwFrcLogClient.PORT;
		this.bufferSize = WkwFrcLogClient.BUFFER_SIZE;
		this.logFilePath = PacketHandlerThread.LOG_FILE_PATH;
		this.networkTableListener = true;
		this.socketListener = false;

		if (null != pArgs) {

			// loop through arguments looking for arg name.

			for (int idx = 0; idx < pArgs.length; idx++) {

				// is the argument port?

				if (WkwFrcLogClient.ARG_PORT.equalsIgnoreCase(pArgs[idx])) {

					// port argument, get the next arg as the value.

					if (pArgs.length > idx) {

						final String aPort = pArgs[++idx];

						try {

							// convert the string to an int

							this.port = Integer.parseInt(aPort);

						} catch (NumberFormatException aNfEx) {
							System.err.println("WkwFrcLogClient port '" + aPort
									+ "' is not a number.");
						}
					}

				} else if (WkwFrcLogClient.ARG_TEAMNUMBER
						.equalsIgnoreCase(pArgs[idx])) {

					if (pArgs.length > idx) {

						final String aTeamNumber = pArgs[++idx];

						try {

							// convert the string to an int

							this.teamNumber = Integer.parseInt(aTeamNumber);

						} catch (NumberFormatException aNfEx) {
							System.err.println("WkwFrcLogClient team number '"
									+ aTeamNumber + "' is not a number.");
						}
					}

				} else if (WkwFrcLogClient.ARG_IPADDRESS
						.equalsIgnoreCase(pArgs[idx])) {

					if (pArgs.length > idx) {

						this.ipAddress = pArgs[++idx];

					}

				} else if (WkwFrcLogClient.ARG_LOG_FILE_PATH
						.equalsIgnoreCase(pArgs[idx])) {

					// log file path argument, get the next arg as the value.

					if (pArgs.length > idx) {

						this.logFilePath = pArgs[++idx];

					}

				} else if (WkwFrcLogClient.ARG_BUFFER_SIZE
						.equalsIgnoreCase(pArgs[idx])) {

					// buffer size argument, get the next arg as the value.

					if (pArgs.length > idx) {

						final String aBufferSize = pArgs[++idx];

						try {

							// convert the string to an int

							this.bufferSize = Integer.parseInt(aBufferSize);

						} catch (NumberFormatException aNfEx) {
							System.err.println("WkwFrcLogClient buffer size '"
									+ aBufferSize + "' is not a number.");
						}

					}

				} else if (WkwFrcLogClient.ARG_NETWORKTABLE_LISTENER
						.equalsIgnoreCase(pArgs[idx])) {

					this.networkTableListener = true;

				} else if (WkwFrcLogClient.ARG_SOCKET_LISTENER
						.equalsIgnoreCase(pArgs[idx])) {

					this.socketListener = true;

				} else if (WkwFrcLogClient.ARG_HELP
						.equalsIgnoreCase(pArgs[idx])) {

					// help argument, print out help.

					this.showHelp();

				} // no else, we ignore any other arguments

			}
		}

	}

	private void showHelp() {

		System.out
				.println("WkwFrcLogClient is a UDP receiver that listens on a socket for log messages from the robot.");
		System.out
				.println("Usage: WkwFrcLogClient [-h] [-n] [-s] [-p port] [-i ipmulticastgroup] [-p logfiledirectory].");
		System.out.println("Where:");
		System.out.println(" -h is this help.");
		System.out.println(" -n use networktable listener.");
		System.out.println(" -s use socket listener.");
		System.out
				.println(" -p is the port number to listen on. The default is "
						+ Integer.toString(WkwFrcLogClient.PORT) + ".");
		// System.out.println(" -i is the ip multicast address to join. The default is "
		// + WkwFrcLogClient.IPGROUP + ".");
		System.out
				.println(" -p is the directory path where the log file is written. The default is "
						+ PacketHandlerThread.LOG_FILE_PATH
						+ ". If you do not want a log file supply -p (as the last argument) without a logfiledirectory value.");

	}

	/**
	 * @param args
	 */
	public static void main(final String[] pArgs) {
		new WkwFrcLogClient(pArgs).listen();
	}

}
