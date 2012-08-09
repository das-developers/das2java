/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of SQL Power Library.
 *
 * SQL Power Library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * SQL Power Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Trivial Modifications at the University of Iowa. 2012.
 */
package org.das2.util;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A java.util.prefs.Preferences that does NOT persist anything, so it has no
 * effect on (nor is affected by!) any use of the "regular" Preferences.
 * <p>
 * To use, run with Java command-line option
 * -Djava.util.prefs.PreferencesFactory=org.das2.util.PreferencesFactory
 */
public class MemoryPreferences extends AbstractPreferences{

	private static final Logger logger = Logger.getLogger(MemoryPreferences.class.getName());
	/**
	 * The map of all data in this particular node.
	 */
	final Map<String, String> values = new HashMap<String, String>();
	/** The map of all Preferences nodes immediately below this node
	 */
	final Map<String, Preferences> children = new HashMap<String, Preferences>();
	public final static String SYSTEM_PROPS_ERROR_MESSAGE =
		"Did you remember to run with -D" +
		MemoryPreferencesFactory.PREFS_FACTORY_SYSTEM_PROPERTY +
		"=" + MemoryPreferencesFactory.MY_CLASS_NAME + "?";

	/**
	 * Constructor, non-public, only for use by my PrefencesFactory; should only be called from
	 * the PreferencesFactory and from node() below; node() takes care of finding the full path
	 * if the incoming path is relative.
	 * @param fullPath
	 */
	MemoryPreferences(AbstractPreferences parent, String name){
		super(parent, name);

		// note, logger should never be null because it's statically initialised.  However,
		// it comes out null every time we run the MatchMaker SwingSessionContextTest!  Hmm...
		if(logger != null && logger.isLoggable(Level.FINE)){
			logger.fine(String.format("MemoryPreferences.MemoryPreferences(%s, %s)", parent, name));
		}
	}

	@Override
	protected void putSpi(String key, String value){
		values.put(key, value);
	}

	@Override
	protected String getSpi(String key){
		String value = values.get(key);
		logger.fine(String.format("get: %s=%s", key, value));
		return value;
	}

	@Override
	protected void removeSpi(String key){
		values.remove(key);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException{
		// nothing to do here?
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException{
		return values.keySet().toArray(new String[values.size()]);
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException{
		return children.keySet().toArray(new String[children.size()]);
	}

	@Override
	protected AbstractPreferences childSpi(String name){
		logger.fine(String.format("MemoryPreferences.node(%s)", name));
		AbstractPreferences n = new MemoryPreferences(this, name);
		children.put(name, n);
		return n;
	}

	@Override
	protected void syncSpi() throws BackingStoreException{
		// nothing to do
	}

	@Override
	protected void flushSpi() throws BackingStoreException{
		// nothing to do
	}
}
