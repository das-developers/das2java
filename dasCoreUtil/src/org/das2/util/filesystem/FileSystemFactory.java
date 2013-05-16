/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * FileSystemFactory.java
 *
 * Created on November 15, 2007, 9:26 AM
 */

package org.das2.util.filesystem;

import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;
import java.net.URI;
import java.net.UnknownHostException;
import java.io.FileNotFoundException;

/**
 * creates a new instance of a type of filesystem
 * @author jbf
 */
public interface FileSystemFactory {
    FileSystem createFileSystem( URI root ) throws FileSystemOfflineException, UnknownHostException, FileNotFoundException;
}
