/* SearchPath - Copyright (C) 1997-1998 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */
package jode.bytecode;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.StringTokenizer;

/**
 * This class represents a path of multiple directories and/or zip files,
 * where we can search for file names.
 * 
 * @author Jochen Hoenicke
 */
public class SearchPath  {

    File[] dirs;
    ZipFile[] zips;
    
    /**
     * Creates a new search path for the given path.
     * @param path The path where we should search for files.  They
     * should be separated by the system dependent pathSeparator.  The
     * entries may also be zip or jar files.
     */
    public SearchPath(String path) {
        StringTokenizer tokenizer = 
            new StringTokenizer(path, File.pathSeparator);
        int length = tokenizer.countTokens();
        
        dirs = new File[length];
        zips = new ZipFile[length];
        for (int i=0; i< length; i++) {
            dirs[i] = new File(tokenizer.nextToken());
            if (!dirs[i].isDirectory()) {
                try {
                    zips[i] = new ZipFile(dirs[i]);
                } catch (java.io.IOException ex) {
                    /* disable this entry */
                    dirs[i] = null;
                }
            }
        }
    }

    public boolean exists(String filename) {
        for (int i=0; i<dirs.length; i++) {
            if (dirs[i] == null)
                continue;
            if (zips[i] != null) {
                ZipEntry ze = zips[i].getEntry(filename);
                if (ze != null)
                    return true;
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
                File f = new File(dirs[i], filename);
                if (f.exists())
                    return true;
            }
        }
        return false;
    }

    /**
     * Searches for a file in the search path.
     * @param filename the filename. The path components should be separated
     * by <code>/</code>.
     * @return An InputStream for the file.
     */
    public InputStream getFile(String filename) throws IOException {
        for (int i=0; i<dirs.length; i++) {
            if (dirs[i] == null)
                continue;
            if (zips[i] != null) {
                ZipEntry ze = zips[i].getEntry(filename);
                if (ze != null)
                    return zips[i].getInputStream(ze);
            } else {
                if (java.io.File.separatorChar != '/')
                    filename = filename
                        .replace('/', java.io.File.separatorChar);
                File f = new File(dirs[i], filename);
                if (f.exists())
                    return new FileInputStream(f);
            }
        }
        throw new FileNotFoundException(filename);
    }
}
