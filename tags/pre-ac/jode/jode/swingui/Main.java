/* Main Copyright (C) 1999 Jochen Hoenicke.
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

package jode.swingui;
import jode.GlobalOptions;
import jode.decompiler.*;
import jode.bytecode.ClassInfo;
import jode.bytecode.SearchPath;
///#ifndef OLDSWING
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
///#else
///import com.sun.java.swing.*;
///import com.sun.java.swing.event.*;
///import com.sun.java.swing.tree.*;
///#endif
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Main 
    implements ActionListener, Runnable, TreeSelectionListener {
    JFrame frame;
    JTree classTree;
    PackagesTreeModel classModel;
    JTextArea  sourcecodeArea, errorArea;
    Thread decompileThread;
    String currentClassPath, lastClassName;

    public Main(String classpath) {
	setClasspath(classpath);
	frame = new JFrame(GlobalOptions.copyright);
	fillContentPane(frame.getContentPane());
	addMenu(frame);
	frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
    }

    public void show() {
	frame.pack();
	frame.show();
    }

    public void fillContentPane(Container contentPane) {
	Font monospaced = new Font("monospaced", Font.PLAIN, 12);
	classModel = new PackagesTreeModel();
	classTree = new JTree(classModel);
	classTree.setRootVisible(false);
	DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
	selModel.setSelectionMode(selModel.SINGLE_TREE_SELECTION);
	classTree.setSelectionModel(selModel);
	classTree.addTreeSelectionListener(this);
        JScrollPane spClassTree = new JScrollPane(classTree);
	sourcecodeArea = new JTextArea(20, 80);
	sourcecodeArea.setFont(monospaced);
	JScrollPane spText = new JScrollPane(sourcecodeArea);
	errorArea = new JTextArea(3, 80);
	errorArea.setFont(monospaced);
	JScrollPane spError = new JScrollPane(errorArea);

	JSplitPane rightPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					      spText, spError);
	JSplitPane allPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					    spClassTree, rightPane);
	contentPane.add(allPane);
	rightPane.setDividerLocation(300);
	rightPane.setDividerSize(4);
	allPane.setDividerLocation(200);
	allPane.setDividerSize(4);
	GlobalOptions.err = new PrintWriter(new AreaWriter(errorArea), true);
    }

    public synchronized void valueChanged(TreeSelectionEvent e) {
	if (decompileThread != null)
	    return;
	TreePath path = e.getNewLeadSelectionPath();
	if (path == null)
	    return;
	Object node = path.getLastPathComponent();
	if (node != null && classModel.isLeaf(node)) {
	    lastClassName = classModel.getFullName(node);
	    decompileThread = new Thread(this);
	    decompileThread.setPriority(Thread.MIN_PRIORITY);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	}
    }

    public synchronized void actionPerformed(ActionEvent e) {
	if (e.getSource() == classTree && decompileThread == null) {
//  	    startButton.setEnabled(false);
	    decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
//  	} else if (e.getSource() == saveButton) {
//  	    if (frame == null)
//  		frame = new Frame(); //XXX
//  	    FileDialog fd = new FileDialog(frame, 
//  					   "Save decompiled code", 
//  					   FileDialog.SAVE);
//  	    fd.setFile(lastClassName.substring
//  		       (lastClassName.lastIndexOf('.')+1).concat(".java"));
//  	    fd.show();
//  	    String fileName = fd.getFile();
//  	    if (fileName == null)
//  		return;
//  	    try {
//  		File f = new File(new File(fd.getDirectory()), fileName);
//  		FileWriter out = new FileWriter(f);
//  		out.write(sourcecodeArea.getText());
//  		out.close();
//  	    } catch (IOException ex) {
//  		errorArea.setText("");
//  		GlobalOptions.err.println("Couldn't write to file " 
//  				       + fileName + ": ");
//  		ex.printStackTrace(GlobalOptions.err);
//  	    }
	}
    }

    public class AreaWriter extends Writer {
	boolean initialized = false;
	private JTextArea area;

	public AreaWriter(JTextArea a) {
	    area = a;
	}

	public void write(char[] b, int off, int len) throws IOException {
	    if (!initialized) {
		area.setText("");
		initialized = true;
	    }
///#ifdef AWT10
///	    area.appendText(new String(b, off, len));
///#else
	    area.append(new String(b, off, len));
///#endif
	}

	public void flush() {
	}

	public void close() {
	}
    }

    public void run() {
//  	Decompiler.isVerbose = verboseCheck.getState();
//  	if (prettyCheck.getState())
//  	    Decompiler.options |= Decompiler.OPTION_PRETTY;
//  	else
//  	    Decompiler.options &= ~Decompiler.OPTION_PRETTY;
	errorArea.setText("");
//  	saveButton.setEnabled(false);

	ImportHandler imports = new ImportHandler();
	try {
	    ClassInfo clazz = ClassInfo.forName(lastClassName);

	    imports.init(lastClassName);
	    ClassAnalyzer clazzAna = new ClassAnalyzer(null, clazz, imports);
	    clazzAna.analyze();
	    clazzAna.analyzeInnerClasses();
	    clazzAna.makeDeclaration();
	    
	    sourcecodeArea.setText("");
	    TabbedPrintWriter writer = 
		new TabbedPrintWriter
		(new BufferedWriter
		 (new AreaWriter(sourcecodeArea), 1024), imports, false);

	    imports.dumpHeader(writer);
	    clazzAna.dumpSource(writer);
	    writer.close();

//  	    saveButton.setEnabled(true);
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace(GlobalOptions.err);
	} finally {
	    synchronized(this) {
		decompileThread = null;
//  		startButton.setEnabled(true);
	    }
	}
    }

    public void addMenu(JFrame frame) {
	JMenuBar bar = new JMenuBar();
	JMenu menu;
	JMenuItem item;
	menu = new JMenu("File");
	item = new JMenuItem("Garbage collect");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		System.gc();
		System.runFinalization();
	    }
	});
	menu.add(item);
	item = new JMenuItem("Exit");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		System.exit(0);
	    }
	});
	menu.add(item);
	bar.add(menu);
	menu = new JMenu("Options");
	item = new JMenuItem("Set classpath...");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		
		String newClassPath = (String) JOptionPane.showInputDialog
		    (null, "New classpath:", null, 
		     JOptionPane.QUESTION_MESSAGE, null,
		     null, currentClassPath);
		if (newClassPath != null
		    && !newClassPath.equals(currentClassPath))
		    setClasspath(newClassPath);
	    }
	});
	menu.add(item);
	item = new JMenuItem("Reload classpath");
	item.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent ev) {
		setClasspath(currentClassPath);
	    }
	});
	menu.add(item);
	bar.add(menu);
	frame.setJMenuBar(bar);
    }

    public void setClasspath(String classpath) {
	if (classpath == null || classpath.length() == 0)
	    classpath = ".";
	currentClassPath = classpath;
	ClassInfo.setClassPath(classpath);
	if (classModel != null) {
	    classTree.clearSelection();
	    classModel.rebuild();
	}
    }

    public static void main(String[] params) {
	String cp = System.getProperty("java.class.path", "");
	cp = cp.replace(File.pathSeparatorChar, SearchPath.pathSeparatorChar);
	for (int i=0; i<params.length; i++) {
	    if (params[i].equals("--cp"))
		cp = params[++i];
	    else
		return;
	}
	GlobalOptions.verboseLevel = 1;
	Main win = new Main(cp);
	win.show();
    }
}