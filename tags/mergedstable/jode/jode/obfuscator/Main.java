/* Main Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.obfuscator;
import jode.bytecode.ClassInfo;
import jode.bytecode.SearchPath;
import jode.GlobalOptions;
import java.util.Vector;
import java.util.StringTokenizer;
import java.lang.reflect.Modifier;
import java.io.PrintWriter;
import java.io.File;
///#ifdef JDK12
///import java.util.Collection;
///import java.util.Arrays;
///import java.util.HashSet;
///#else
import jode.util.Collection;
import jode.util.Arrays;
import jode.util.HashSet;
///#endif

public class Main {
    public static boolean swapOrder   = false;

    public static final int OPTION_STRONGOVERLOAD  = 0x0001;
    public static final int OPTION_PRESERVESERIAL  = 0x0002;
    public static int options = OPTION_PRESERVESERIAL;

    public static final String[] stripNames = {
	"unreach", "inner", "lvt", "lnt", "source"
    };
    public static final int STRIP_UNREACH    = 0x0001;
    public static final int STRIP_INNERINFO  = 0x0002;
    public static final int STRIP_LVT        = 0x0004;
    public static final int STRIP_LNT        = 0x0008;
    public static final int STRIP_SOURCE     = 0x0010;
    public static int stripping = 0x1;

    private static ClassBundle bundle;

    public static void usage() {
	PrintWriter err = GlobalOptions.err;
        err.println("usage: jode.Obfuscator flags* [class | package]*");
        err.println("\t-v                "+
		    "Verbose output (allowed multiple times).");
        err.println("\t--nostrip         "+
		    "Don't strip not needed methods");
        
        err.println("\t--cp <classpath>  "+
                           "The class path; should contain classes.zip");
        err.println("\t-d,--dest <directory>  "+
		    "Destination directory for output classes");
        err.println("Preserve options: ");
        err.println("\t--package         "+
		    "Preserve all package members");
        err.println("\t--protected       "+
		    "Preserve all protected members");
        err.println("\t--public          "+
		    "Preserve all public members");
        err.println("\t--preserve <name> "+
		    "Preserve only the given name (allowed multiple times)");
	err.println("\t--breakserial     "+
		    "Allow the serialized form to change");
        err.println("Obfuscating options: ");
        err.println("\t--rename={strong|weak|unique|none} "+
		    "Rename identifiers with given scheme");
        err.println("\t--strip=...       "+
		    "use --strip=help for more information.");
        err.println("\t--table <file>    "+
		    "Read (some) translation table from file");
        err.println("\t--revtable <file> "+
		    "Write reversed translation table to file");
        err.println("\t--swaporder       "+
		    "Swap the order of fields and methods.");
	err.println("\t--debug=...       "+
		    "use --debug=help for more information.");
    }

    public static void usageStrip() {
	PrintWriter err = GlobalOptions.err;
	err.println("Strip options: --strip=flag1,flag2,...");
	err.println("possible flags:");
	err.println("\tunreach      " +
		    "strip all unreachable methods and classes.");
	err.println("\tinner        " +
		    "strip inner classes info.");
	err.println("\tlvt          " +
		    "strip local variable tables.");
	err.println("\tlnt          " +
		    "strip line number tables.");
	err.println("\tsource       " +
		    "strip the name of the source file.");
	err.println("\tinout        " +
		    "show T1/T2 in/out set analysis.");
	err.println("\tlvt          " +
		    "dump LocalVariableTable.");
	err.println("\tcheck        " +
		    "do time consuming sanity checks.");
	err.println("\tlocals       " +
		    "dump local merging information.");
	err.println("\tconstructors " +
		    "dump constructor simplification.");
	err.println("\tinterpreter  " +
		    "debug execution of interpreter.");
	System.exit(0);
    }

    public static void setStripOptions(String stripString) {
	if (stripString.length() == 0 || stripString.equals("help"))
	    usageStrip();

	StringTokenizer st = new StringTokenizer(stripString, ",");
    next_token:
	while (st.hasMoreTokens()) {
	    String token = st.nextToken().intern();
	    for (int i=0; i < stripNames.length; i++) {
		if (token == stripNames[i]) {
		    stripping |= 1 << i;
		    continue next_token;
		}
	    }
	    GlobalOptions.err.println("Illegal strip flag: "+token);
	    usageStrip();
	}
    }

    public static Renamer getRenamer(String option) {
	if (option.equals("strong"))
	    return new StrongRenamer
	("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz",
	 "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
	else if (option.equals("weak"))
	    return new StrongRenamer
		("abcdefghijklmnopqrstuvwxyz",
		 "0123456789abcdefghijklmnopqrstuvwxyz");
	else if (option.equals("unique")) {
	    return new Renamer() {
		static int serialnr = 0;
		public String generateName(Identifier ident, String lastName) {
		    return ("xxx" + serialnr++);
		}
	    };
	} else if (option.equals("none")) {
	    return new Renamer() {
		public String generateName(Identifier ident, String lastName) {
		    String name = ident.getName();
		    if (lastName == null)
			return name;

		    int nr = 2;
		    if (lastName.length() > name.length())
			nr = 1 + Integer.parseInt
			    (lastName.substring(name.length()));
		    return name + nr;
		}
	    };
	}
	GlobalOptions.err.println("Incorrect value for --rename option: "
				  + option);
	usage();
	System.exit(0);
	return null;
    }

    public static ClassBundle getClassBundle() {
	return bundle;
    }

    public static CodeAnalyzer createCodeAnalyzer() {
	return new SimpleAnalyzer() /*XXX*/;
    }

    static CodeTransformer[] codeTransformers = {
	new LocalOptimizer(),
	new RemovePopAnalyzer()
    };
    
    public static Collection getCodeTransformers() {
	return Arrays.asList(codeTransformers);
    }

    public static void main(String[] params) {
        int i;
        String sourcePath = System.getProperty("java.class.path")
	    .replace(File.pathSeparatorChar, SearchPath.pathSeparatorChar);
        String destPath = ".";

        Collection preservedIdents = new HashSet();

        ModifierMatcher preserveRule = ModifierMatcher.denyAll;
        Renamer renamer = null;
        String table = null;
        String toTable = null;

        for (i=0; i<params.length && params[i].startsWith("-"); i++) {
            if (params[i].equals("-v"))
                GlobalOptions.verboseLevel++;
            else if (params[i].startsWith("--debug")) {
		String flags;
		if (params[i].startsWith("--debug=")) {
		    flags = params[i].substring(8);
		} else if (params[i].length() != 7) {
		    usage();
		    return;
		} else {
		    flags = params[++i];
		}
		GlobalOptions.setDebugging(flags);
            } else if (params[i].equals("--strip"))
		setStripOptions(params[++i]);
	    else if (params[i].startsWith("--strip="))
		setStripOptions(params[i].substring(8));
            else if (params[i].equals("--sourcepath")
		     || params[i].equals("--classpath")
		     || params[i].equals("--cp"))
                sourcePath = params[++i];
            else if (params[i].equals("--dest")
		     || params[i].equals("-d"))
                destPath   = params[++i];

            /* Preserve options */
            else if (params[i].equals("--package"))
                preserveRule = ModifierMatcher.allowAll.forceAccess
		    (0, true);
            else if (params[i].equals("--protected"))
                preserveRule = ModifierMatcher.allowAll.forceAccess
		    (Modifier.PROTECTED, true);
            else if (params[i].equals("--public"))
                preserveRule = ModifierMatcher.allowAll.forceAccess
		    (Modifier.PUBLIC, true);
            else if (params[i].equals("--preserve")) {
                String ident = params[++i];
                preservedIdents.add(ident);
            }
            else if (params[i].equals("--breakserial"))
		options &= ~OPTION_PRESERVESERIAL;

            /* Obfuscate options */
            else if (params[i].equals("--rename"))
		renamer = getRenamer(params[++i]);
	    else if (params[i].startsWith("--rename="))
		renamer = getRenamer(params[i].substring(9));
	    else if (params[i].equals("--table")) {
                table  = params[++i];
            }
            else if (params[i].equals("--revtable")) {
                toTable = params[++i];
            } 
            else if (params[i].equals("--swaporder"))
		swapOrder = true;
            else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].equals("-h") && !params[i].equals("--help"))
                    GlobalOptions.err.println("Unknown option: "+params[i]);
                usage();
                return;
            }
        }
	if (renamer == null)
	    renamer = getRenamer("none");
	
        if (i == params.length) {
            GlobalOptions.err.println("No package or classes specified.");
            usage();
            return;
        }

	GlobalOptions.err.println("Loading classes");
        ClassInfo.setClassPath(sourcePath);
        bundle = new ClassBundle();
        for (; i< params.length; i++)
            bundle.loadClasses(params[i]);

	GlobalOptions.err.println("Computing reachable / preserved settings");
        bundle.setPreserved(preserveRule, preservedIdents);

	GlobalOptions.err.println("Renaming methods");
	if (table != null)
            bundle.readTable(table);
	bundle.buildTable(renamer);
        if (toTable != null)
            bundle.writeTable(toTable);

	GlobalOptions.err.println("Transforming the classes");
	bundle.doTransformations();
	GlobalOptions.err.println("Writing new classes");
        bundle.storeClasses(destPath);
    }
}