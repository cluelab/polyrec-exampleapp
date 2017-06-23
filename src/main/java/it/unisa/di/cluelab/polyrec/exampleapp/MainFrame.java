package it.unisa.di.cluelab.polyrec.exampleapp;

// MainFrame - run an Applet as an application
//
// Copyright (C)1996,1998 by Jef Poskanzer <jef@mail.acme.com>. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import java.applet.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

/// Run an Applet as an application.
// 
// Using this class you can add a trivial main program to any Applet
// and run it directly, as well as from a browser or the appletviewer.
// And unlike some versions of this concept, MainFrame implements both images and sound.
// 
// Sample main program:
// public static void main( String[] args ){
//     new MainFrame( new ThisApplet(), args, 400, 400 );
// }
// The only methods you need to know about are the constructors.
// 
// You can specify Applet parameters on the command line, as name=value.
// For instance, the equivalent of:
// <PARAM NAME="pause" VALUE="200">
// would just be:
// pause=200
// You can also specify two special parameters:
// width=N          Width of the Applet.
// height=N         Height of the Applet.

public class MainFrame extends JFrame implements Runnable, AppletStub, AppletContext {
    private static final long serialVersionUID = 7516049709113602247L;
    private String name;
    private Applet applet;
    private JLabel label = null;
    private Dimension appletSize;

    private static final String PARAM_PROP_PREFIX = "parameter.";

    public static void main(String[] args) {
        new MainFrame(new TestApplet(), args, 640, 480);
    }

    /// Constructor with everything specified.
    public MainFrame(Applet applet, String[] args, int width, int height) {
        build(applet, args, width, height);
    }

    /// Constructor with no default width/height.
    public MainFrame(Applet applet, String[] args) {
        build(applet, args, -1, -1);
    }

    /// Constructor with no arg parsing.
    public MainFrame(Applet applet, int width, int height) {
        build(applet, null, width, height);
    }

    // Internal constructor routine.
    private void build(Applet applet, String[] args, int width, int height) {
        this.applet = applet;
        applet.setStub(this);
        name = applet.getClass().getName();
        setTitle(name);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set up properties.
        Properties props = System.getProperties();
        props.put("browser", "Acme.MainFrame");
        props.put("browser.version", "11jul96");
        props.put("browser.vendor", "Acme Laboratories");
        props.put("browser.vendor.url", "http://www.acme.com/");

        // Turn args into parameters by way of the properties list.
        if (args != null)
            parseArgs(args, props);

        // If width and height are specified in the parameters, override the compiled-in values.
        String widthStr = getParameter("width");
        if (widthStr != null)
            width = Integer.parseInt(widthStr);
        String heightStr = getParameter("height");
        if (heightStr != null)
            height = Integer.parseInt(heightStr);

        // Were width and height specified somewhere?
        if (width == -1 || height == -1) {
            System.err.println("Width and height must be specified.");
            return;
        }

        // Lay out components.
        setLayout(new BorderLayout());
        add("Center", applet);
        JPanel borderPanel = new JPanel();
        borderPanel.setLayout(new BorderLayout());
        borderPanel.setBorder(new LineBorder(Color.BLACK));
        label = new JLabel("");
        borderPanel.add("Center", label);
        add("South", borderPanel);

        // Set up size.
        pack();
        validate();
        appletSize = applet.getSize();
        applet.setSize(width, height);
        setVisible(true);

        // Start a separate thread to call the applet's init() and start()
        // methods, in case they take a long time.
        (new Thread(this)).start();
    }

    // Turn command-line arguments into Applet parameters, by way of the properties list.
    private static void parseArgs(String[] args, Properties props) {
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            int ind = arg.indexOf('=');
            if (ind == -1)
                props.put(PARAM_PROP_PREFIX + arg.toLowerCase(), "");
            else
                props.put(PARAM_PROP_PREFIX + arg.substring(0, ind).toLowerCase(), arg.substring(ind + 1));
        }
    }

    // Methods from Runnable.

    /// Separate thread to call the applet's init() and start() methods.
    public void run() {
        showStatus(name + " initializing...");
        applet.init();
        validate();
        showStatus(name + " starting...");
        applet.start();
        validate();
        showStatus(name + " running...");
    }

    // Methods from AppletStub.

    public boolean isActive() {
        return true;
    }

    public URL getDocumentBase() {
        // Returns the current directory.
        String dir = System.getProperty("user.dir");
        String urlDir = dir.replace(File.separatorChar, '/');
        try {
            return new URL("file:" + urlDir + "/");
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL getCodeBase() {
        // Hack: loop through each item in CLASSPATH, checking if
        // the appropriately named .class file exists there. But
        // this doesn't account for .zip files.
        String path = System.getProperty("java.class.path");
        StringTokenizer st = new StringTokenizer(path, ":");
        while (st.hasMoreElements()) {
            String dir = (String) st.nextElement();
            String filename = dir + File.separatorChar + name + ".class";
            File file = new File(filename);
            if (file.exists()) {
                String urlDir = dir.replace(File.separatorChar, '/');
                try {
                    return new URL("file:" + urlDir + "/");
                } catch (MalformedURLException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public String getParameter(String name) {
        // Return a parameter via the munged names in the properties list.
        return System.getProperty(PARAM_PROP_PREFIX + name.toLowerCase());
    }

    public void appletResize(int width, int height) {
        // Change the frame's size by the same amount that the applet's size is changing.
        Dimension frameSize = getSize();
        frameSize.width += width - appletSize.width;
        frameSize.height += height - appletSize.height;
        setSize(frameSize);
        appletSize = applet.getSize();
    }

    public AppletContext getAppletContext() {
        return this;
    }

    // Methods from AppletContext.

    public AudioClip getAudioClip(URL url) {
        // This is an internal undocumented routine. However, it
        // also provides needed functionality not otherwise available.
        // I suspect that in a future release, JavaSoft will add an
        // audio content handler which encapsulates this, and then
        // we can just do a getContent just like for images.
        // return new sun.applet.AppletAudioClip( url );
        return Applet.newAudioClip(url);
    }

    public Image getImage(URL url) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        try {
            ImageProducer prod = (ImageProducer) url.getContent();
            return tk.createImage(prod);
        } catch (IOException e) {
            return null;
        }
    }

    public Applet getApplet(String name) {
        // Returns this Applet or nothing.
        if (name.equals(this.name))
            return applet;
        return null;
    }

    public Enumeration<Applet> getApplets() {
        // Just yields this applet.
        Vector<Applet> v = new Vector<Applet>();
        v.addElement(applet);
        return v.elements();
    }

    public void showDocument(URL url) {
        // Ignore.
    }

    public void showDocument(URL url, String target) {
        // Ignore.
    }

    public void showStatus(String status) {
        label.setText(status);
    }

    Map<String, InputStream> streamMap = new HashMap<String, InputStream>();

    public void setStream(String key, InputStream stream) throws IOException {
        streamMap.put(key, stream);
    }

    public InputStream getStream(String key) {
        return streamMap.get(key);
    }

    public Iterator<String> getStreamKeys() {
        return streamMap.keySet().iterator();
    }

}
