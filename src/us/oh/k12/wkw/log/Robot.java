/*----------------------------------------------------------------------------*/
/* Copyright (c) 2012 Worthington Kilbourne Robot Club. All Rights Reserved. */
/* Open Source Software - may be modified and shared by FRC teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project. */
/*----------------------------------------------------------------------------*/
package us.oh.k12.wkw.log;

import edu.wpi.first.wpilibj.networking.NetworkTable;

/**
 * 
 */
public class Robot {

	public Robot() {
		super();
	}

	public static final String PREF_SAVE_FIELD = "~S A V E~";
	public static final String TABLE_NAME = "SmartDashboard";
	public static final String PREFERENCES_NAME = "Preferences";
	private static final String NETWORK_TABLE_NAME_LOGGER = "WkwFrcLogger";

	public static NetworkTable getTable() {
		return NetworkTable.getTable(Robot.TABLE_NAME);
	}

	public static NetworkTable getPreferences() {
		return NetworkTable.getTable(Robot.PREFERENCES_NAME);
	}

	public static NetworkTable getLogger() {
		return NetworkTable.getTable(Robot.NETWORK_TABLE_NAME_LOGGER);
	}

}
