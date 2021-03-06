package org.apache.maven.jxr;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.jxr.ant.DirectoryScanner;
import org.apache.maven.jxr.log.Log;
import org.apache.maven.jxr.pacman.FileManager;
import org.apache.maven.jxr.pacman.PackageManager;



/**
 * Main entry point into Maven used to kick off the XReference code building.
 *
 * @author <a href="mailto:burton@apache.org">Kevin A. Burton</a>
 * @version $Id: JXR.java 692692 2008-09-06 17:34:46Z hboutemy $
 */
public class JXR
{
    /**
     * The Log.
     */
    private Log log;

    /**
     * Description of the Notice.
     */
    public static final String NOTICE = "This page was automatically generated by "
        + "<a href=\"http://maven.apache.org/\">Maven</a>";

    /**
     * Footer equal to the notice, by default
     */
    
    public static String FOOTER = NOTICE;
    
    /**
     * The default list of include patterns to use.
     */
    private static final String[] DEFAULT_INCLUDES = {"**/*.java"};

    /**
     * Path to destination.
     */
    private String dest = "";

    private Locale locale;

    private String inputEncoding;

    private String outputEncoding;
    
    public static boolean showHeader = true;
    
    public static boolean showFooter = true;

    /**
     * Relative path to javadocs, suitable for hyperlinking.
     */
    private String javadocLinkDir;

    /**
     * Handles taking .java files and changing them into html. "More than meets
     * the eye!" :)
     */
    private JavaCodeTransform transformer;

    /**
     * The revision of the module currently being processed.
     */
    private String revision;

    /**
     * The list of exclude patterns to use.
     */
    private String[] excludes = null;

    /**
     * The list of include patterns to use.
     */
    private String[] includes = DEFAULT_INCLUDES;

    /**
     * Now that we have instantiated everything. Process this JXR task.
     *
     * @param packageManager
     * @param source
     * @throws IOException
     */
    public void processPath( PackageManager packageManager, String source )
        throws IOException
    {
        this.transformer = new JavaCodeTransform( packageManager );

        DirectoryScanner ds = new DirectoryScanner();
        // I'm not sure why we don't use the directoryScanner in packageManager,
        // but since we don't we need to set includes/excludes here as well
        ds.setExcludes( excludes );
        ds.setIncludes( includes );
        ds.addDefaultExcludes();

        File dir = new File( source );

        if ( !dir.exists() )
        {
            if ( !dir.mkdirs() )
            {
                throw new IllegalStateException(
                    "Your source directory does not exist and could not be created:" + source );
            }
        }

        ds.setBasedir( source );
        ds.scan();

        //now get the list of included files

        String[] files = ds.getIncludedFiles();

        for ( int i = 0; i < files.length; ++i )
        {
            String src = source + System.getProperty( "file.separator" ) + files[i];

            if ( isJavaFile( src ) )
            {
                transform( src, getDestination( source, src ) );
            }

        }
    }

    /**
     * Check to see if the file is a Java source file.
     *
     * @param filename The name of the file to check
     * @return <code>true</true> if the file is a Java file
     */
    public static boolean isJavaFile( String filename )
    {
        File file = new File( filename );
        return filename.endsWith( ".java" ) && file.length() > 0;
    }

    /**
     * Check to see if the file is an HTML file.
     *
     * @param filename The name of the file to check
     * @return <code>true</true> if the file is an HTML file
     */
    public static boolean isHtmlFile( String filename )
    {
        return filename.endsWith( ".html" );
    }

    /**
     * Get the path to the destination files.
     *
     * @return The path to the destination files
     */
    public String getDest()
    {
        return this.dest;
    }

    /**
     * @param dest
     */
    public void setDest( String dest )
    {
        this.dest = dest;
    }

    /**
     * @param locale
     */
    public void setLocale( Locale locale )
    {
        this.locale = locale;
    }

    /**
     * @param footer string - set the footer
     */
    
    public void setFooter ( String footer )
    {
    	JXR.FOOTER = footer;
    }
    
    /**
     * @param inputEncoding
     */
    public void setInputEncoding( String inputEncoding )
    {
        this.inputEncoding = inputEncoding;
    }

    /**
     * @param outputEncoding
     */
    public void setOutputEncoding( String outputEncoding )
    {
        this.outputEncoding = outputEncoding;
    }

    /**
     * @param javadocLinkDir
     */
    public void setJavadocLinkDir( String javadocLinkDir )
    {
        // get a relative link to the javadocs
        this.javadocLinkDir = javadocLinkDir;
    }

    /**
     * @param transformer
     */
    public void setTransformer( JavaCodeTransform transformer )
    {
        this.transformer = transformer;
    }

    /**
     * @param revision
     */
    public void setRevision( String revision )
    {
        this.revision = revision;
    }

    /**
     * @param header boolean - true enables header, false disables it.
     */
    
    public void setHeader( boolean header ){
    	JXR.showHeader = header;
    }
    
    /**
     * @param header boolean - true enables header, false disables it.
     */
    
    public void setFooter( boolean footer ){
    	JXR.showFooter = footer;
    }
    
    /**
     * @param log
     */
    public void setLog( Log log )
    {
        this.log = log;
    }

    /**
     * @param sourceDirs
     * @param templateDir
     * @param windowTitle
     * @param docTitle
     * @param bottom
     * @throws IOException
     * @throws JxrException
     */
    public void xref( List sourceDirs, String templateDir, String windowTitle, String docTitle, String bottom )
        throws IOException, JxrException
    {
        // first collect package and class info
        FileManager fileManager = new FileManager();
        fileManager.setEncoding( inputEncoding );

        PackageManager pkgmgr = new PackageManager( log, fileManager );
        pkgmgr.setExcludes( excludes );
        pkgmgr.setIncludes( includes );

        // go through each source directory and xref the java files
        for ( Iterator i = sourceDirs.iterator(); i.hasNext(); )
        {
            String path = (String) i.next();
            path = new File( path ).getCanonicalPath();

            pkgmgr.process( path );

            processPath( pkgmgr, path );
        }

        // once we have all the source files xref'd, create the index pages
        DirectoryIndexer indexer = new DirectoryIndexer( pkgmgr, dest );
        indexer.setOutputEncoding( outputEncoding );
        indexer.setTemplateDir( templateDir );
        indexer.setWindowTitle( windowTitle );
        indexer.setDocTitle( docTitle );
        indexer.setBottom( bottom );
        indexer.process( log );
    }

    // ----------------------------------------------------------------------
    // private methods
    // ----------------------------------------------------------------------

    /**
     * Given a filename get the destination on the filesystem of where to store
     * the to be generated HTML file. Pay attention to the package name.
     *
     * @param source
     * @param filename
     * @return A String with the store destination.
     */
    private String getDestination( String source, String filename )
    {
        //remove the source directory from the filename.

        String dest = filename.substring( source.length(), filename.length() );

        int start = 0;
        int end = dest.indexOf( ".java" );

        if ( end != -1 )
        {
            //remove the .java from the filename
            dest = dest.substring( start, end );
        }

        //add the destination directory to the filename.
        dest = this.getDest() + dest;

        //add .html to the filename

        dest = dest + ".html";

        return dest;
    }

    /**
     * Given a source file transform it into HTML and write it to the
     * destination (dest) file.
     *
     * @param source The java source file
     * @param dest The directory to put the HTML into
     * @throws IOException Thrown if the transform can't happen for some reason.
     */
    private void transform( String source, String dest )
        throws IOException
    {
        log.debug( source + " -> " + dest );

        // get a relative link to the javadocs
        String javadoc = javadocLinkDir != null ? getRelativeLink( dest, javadocLinkDir ) : null;
        transformer.transform( source, dest, locale, inputEncoding, outputEncoding, javadoc, this.revision);
    }

    /**
     * Creates a relative link from one directory to another.
     *
     * Example:
     * given <code>/foo/bar/baz/oink</code>
     * and <code>/foo/bar/schmoo</code>
     *
     * this method will return a string of <code>"../../schmoo/"</code>
     *
     * @param fromDir The directory from which the link is relative.
     * @param toDir The directory into which the link points.
     * @return a String of format <code>"../../schmoo/"</code>
     * @throws java.io.IOException If a problem is encountered while navigating through the directories.
     */
    private static String getRelativeLink( String fromDir, String toDir )
        throws IOException
    {
        StringBuffer toLink = new StringBuffer();   // up from fromDir
        StringBuffer fromLink = new StringBuffer(); // down into toDir

        // create a List of toDir's parent directories
        List parents = new LinkedList();
        File f = new File( toDir );
        f = f.getCanonicalFile();
        while ( f != null )
        {
            parents.add( f );
            f = f.getParentFile();
        }

        // walk up fromDir to find the common parent
        f = new File( fromDir );
        if ( !f.isDirectory() )
        {
            // Passed in a fromDir with a filename on the end - strip it
            f = f.getParentFile();
        }
        f = f.getCanonicalFile();
        f = f.getParentFile();
        boolean found = false;
        while ( f != null && !found )
        {
            for ( int i = 0; i < parents.size(); ++i )
            {
                File parent = (File) parents.get( i );
                if ( f.equals( parent ) )
                {
                    // when we find the common parent, add the subdirectories
                    // down to toDir itself
                    for ( int j = 0; j < i; ++j )
                    {
                        File p = (File) parents.get( j );
                        toLink.insert( 0, p.getName() + "/" );
                    }
                    found = true;
                    break;
                }
            }
            f = f.getParentFile();
            fromLink.append( "../" );
        }

        if ( !found )
        {
            throw new FileNotFoundException( fromDir + " and " + toDir + " have no common parent." );
        }

        return fromLink.append( toLink.toString() ).toString();
    }

    public void setExcludes( String[] excludes )
    {
        this.excludes = excludes;
    }


    public void setIncludes( String[] includes )
    {
        if ( includes == null )
        {
            // We should not include non-java files, so we use a sensible default pattern
            this.includes = DEFAULT_INCLUDES;
        }
        else
        {
            this.includes = includes;
        }
    }
}
