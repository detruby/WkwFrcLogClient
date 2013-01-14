/*----------------------------------------------------------------------------*/
/* Copyright (c) 2012 Worthington Kilbourne Robot Club. All Rights Reserved. */
/* Open Source Software - may be modified and shared by FRC teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project. */
/*----------------------------------------------------------------------------*/
package us.oh.k12.wkw.log;

import java.text.SimpleDateFormat;
import java.util.Date;

import edu.wpi.first.wpilibj.networking.NetworkAdditionListener;
import edu.wpi.first.wpilibj.networking.NetworkListener;

/**
 * 
 */
public class FrcLoggerListener implements NetworkListener, NetworkAdditionListener {

	private static final String NETWORK_TABLE_FIELD_NAME = "log";

	// private Object /*ContentResolver*/contentResolver;
	private int teamNumber = 0;
	// private Date time;
	private String level;
	private String clazz;
	private String method;
	private String message;

	// example:
	// 1969/12/31_16:03:23.563,D,WkwFrcRobot2012,teleopInit(),Called.

	protected FrcLoggerListener() {
		super();
	}

	public FrcLoggerListener(final Object pContentResolver, final int pTeamNumber) {
		super();
		// this.contentResolver = pContentResolver;
		this.teamNumber = pTeamNumber;
	}

	public FrcLoggerListener(final int pTeamNumber, final String[] pTokenArray) {
		super();

		if ((null == pTokenArray) || (pTokenArray.length < 5)) {
			throw new IllegalArgumentException("pTokenArray is null or less columns");
		}

		this.teamNumber = pTeamNumber;
		// this.time = new Date(); // pTokenArray[0].replaceAll("[\\\"]", "");
		this.level = pTokenArray[1].replaceAll("[\\\"]", "");
		this.clazz = pTokenArray[2].replaceAll("[\\\"]", "");
		this.method = pTokenArray[3].replaceAll("[\\\"]", "");
		this.message = pTokenArray[4].replaceAll("[\\\"]", "");
	}

	public void log() {
		System.out.println(this.formatNow() + "," + this.level + "," + this.clazz + ","
				+ this.method + "," + this.message);
	}

	private String formatNow() {
		final SimpleDateFormat aFormat = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss.sss");
		return aFormat.format(new Date());
	}

	/*
	private void create(final ContentResolver pContentResolver) {
		if (null != pContentResolver) {

			final FrcActivity aFrcActivity = new FrcActivity(this.teamNumber, this.clazz,
					this.level + " " + this.method + " " + this.message);

			aFrcActivity.create(pContentResolver);

		}
	}
	*/

	@Override
	public void fieldAdded(final String pName, final Object pValue) {

		try {

			this.valueChanged(pName, pValue);

		} catch (Exception anEx) {
			this.error("fieldAdded()", anEx);
		}
	}

	@Override
	public void valueChanged(final String pName, final Object pValue) {

		try {

			if ((null != pName) && (FrcLoggerListener.NETWORK_TABLE_FIELD_NAME.equals(pName))
					&& (null != pValue)) {

				if (pValue instanceof String) {

					String aValue = (String) pValue;

					final FrcLoggerListener aLogRecord = new FrcLoggerListener(this.teamNumber,
							aValue.split("[,]", 5));

					aLogRecord.log();
					// aLogRecord.create(this.contentResolver);

				} else {
					this.debug("valueChanged()",
							pValue.getClass().getName() + "=" + pValue.toString());
				}
			}

		} catch (Exception anEx) {
			this.error("valueChanged()", anEx);
		}
	}

	@Override
	public void valueConfirmed(final String pName, final Object pValue) {

		try {

			this.valueChanged(pName, pValue);

		} catch (Exception anEx) {
			this.error("valueConfirmed()", anEx);
		}
	}

	protected void debug(final String pMethod, final String pMessage) {
		System.err.println(this.getClassName() + " " + pMethod + " " + pMessage);
	}

	private void error(final String pMethod, final Exception anEx) {
		this.error(
				pMethod,
				"Caught " + anEx.getClass().getName() + ", with message=" + anEx.getMessage() + ".",
				anEx);
	}

	private void error(final String pMethod, final String pMessage, final Exception anEx) {
		System.err.println(this.getClassName() + " " + pMethod + " " + pMessage);
		if (null != anEx) {
			anEx.printStackTrace();
		}
	}

	private String getClassName() {
		final String aClassName = this.getClass().getName();
		return aClassName.substring(aClassName.lastIndexOf('.') + 1);
	}

}
