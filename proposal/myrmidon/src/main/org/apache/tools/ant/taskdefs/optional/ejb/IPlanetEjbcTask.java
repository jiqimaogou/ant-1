/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.xml.sax.SAXException;

/**
 * Task to compile EJB stubs and skeletons for the iPlanet Application Server.
 * The EJBs to be processed are specified by the EJB 1.1 standard XML
 * descriptor, and additional attributes are obtained from the iPlanet
 * Application Server-specific XML descriptor. Since the XML descriptors can
 * include multiple EJBs, this is a convenient way of specifying many EJBs in a
 * single Ant task. The following attributes are allowed:
 * <ul>
 *   <li> <i>ejbdescriptor</i> -- Standard EJB 1.1 XML descriptor (typically
 *   titled "ejb-jar.xml"). This attribute is required.
 *   <li> <i>iasdescriptor</i> -- EJB XML descriptor for iPlanet Application
 *   Server (typically titled "ias-ejb-jar.xml). This attribute is required.
 *
 *   <li> <i>dest</i> -- The is the base directory where the RMI stubs and
 *   skeletons are written. In addition, the class files for each bean (home
 *   interface, remote interface, and EJB implementation) must be found in this
 *   directory. This attribute is required.
 *   <li> <i>classpath</i> -- The classpath used when generating EJB stubs and
 *   skeletons. This is an optional attribute (if omitted, the classpath
 *   specified when Ant was started will be used). Nested "classpath" elements
 *   may also be used.
 *   <li> <i>keepgenerated</i> -- Indicates whether or not the Java source files
 *   which are generated by ejbc will be saved or automatically deleted. If
 *   "yes", the source files will be retained. This is an optional attribute (if
 *   omitted, it defaults to "no").
 *   <li> <i>debug</i> -- Indicates whether or not the ejbc utility should log
 *   additional debugging statements to the standard output. If "yes", the
 *   additional debugging statements will be generated (if omitted, it defaults
 *   to "no").
 *   <li> <i>iashome</i> -- May be used to specify the "home" directory for this
 *   iPlanet Application Server installation. This is used to find the ejbc
 *   utility if it isn't included in the user's system path. This is an optional
 *   attribute (if specified, it should refer to the <code>[install-location]/iplanet/ias6/ias
 *                           </code> directory). If omitted, the ejbc utility
 *   must be on the user's system path.
 * </ul>
 * <p>
 *
 * For each EJB specified, this task will locate the three classes that comprise
 * the EJB. If these class files cannot be located in the <code>dest</code>
 * directory, the task will fail. The task will also attempt to locate the EJB
 * stubs and skeletons in this directory. If found, the timestamps on the stubs
 * and skeletons will be checked to ensure they are up to date. Only if these
 * files cannot be found or if they are out of date will ejbc be called to
 * generate new stubs and skeletons.
 *
 * @author Greg Nelson <a href="mailto:greg@netscape.com">greg@netscape.com</a>
 * @see IPlanetEjbc
 */
public class IPlanetEjbcTask extends Task
{
    private boolean keepgenerated = false;
    private boolean debug = false;
    private Path classpath;
    private File dest;

    /*
     * Attributes set by the Ant build file
     */
    private File ejbdescriptor;
    private File iasdescriptor;
    private File iashome;

    /**
     * Sets the classpath to be used when compiling the EJB stubs and skeletons.
     *
     * @param classpath The classpath to be used.
     */
    public void setClasspath( Path classpath )
    {
        if( this.classpath == null )
        {
            this.classpath = classpath;
        }
        else
        {
            this.classpath.append( classpath );
        }
    }

    /**
     * Sets whether or not debugging output will be generated when ejbc is
     * executed.
     *
     * @param debug A boolean indicating if debugging output should be generated
     */
    public void setDebug( boolean debug )
    {
        this.debug = debug;
    }

    /**
     * Sets the destination directory where the EJB "source" classes must exist
     * and where the stubs and skeletons will be written. The destination
     * directory must exist before this task is executed.
     *
     * @param dest The directory where the compiled classes will be written.
     */
    public void setDest( File dest )
    {
        this.dest = dest;
    }

    /**
     * Sets the location of the standard XML EJB descriptor. Typically, this
     * file is named "ejb-jar.xml".
     *
     * @param ejbdescriptor The name and location of the EJB descriptor.
     */
    public void setEjbdescriptor( File ejbdescriptor )
    {
        this.ejbdescriptor = ejbdescriptor;
    }

    /**
     * Sets the location of the iAS-specific XML EJB descriptor. Typically, this
     * file is named "ias-ejb-jar.xml".
     *
     * @param iasdescriptor The name and location of the iAS-specific EJB
     *      descriptor.
     */
    public void setIasdescriptor( File iasdescriptor )
    {
        this.iasdescriptor = iasdescriptor;
    }

    /**
     * Setter method used to store the "home" directory of the user's iAS
     * installation. The directory specified should typically be <code>[install-location]/iplanet/ias6/ias</code>
     * .
     *
     * @param iashome The home directory for the user's iAS installation.
     */
    public void setIashome( File iashome )
    {
        this.iashome = iashome;
    }

    /**
     * Sets whether or not the Java source files which are generated by the ejbc
     * process should be retained or automatically deleted.
     *
     * @param keepgenerated A boolean indicating if the Java source files for
     *      the stubs and skeletons should be retained.
     */
    public void setKeepgenerated( boolean keepgenerated )
    {
        this.keepgenerated = keepgenerated;
    }

    /**
     * Creates a nested classpath element.
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classpath == null )
        {
            classpath = new Path();
        }
        Path path1 = classpath;
        final Path path = new Path();
        path1.addPath( path );
        return path;
    }

    /**
     * Does the work.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        checkConfiguration();

        executeEjbc( getParser() );
    }

    /**
     * Returns the CLASSPATH to be used when calling EJBc. If no user CLASSPATH
     * is specified, the System classpath is returned instead.
     *
     * @return Path The classpath to be used for EJBc.
     */
    private Path getClasspath()
    {
        if( classpath == null )
        {
            classpath = Path.systemClasspath;
        }

        return classpath;
    }

    /**
     * Returns a SAXParser that may be used to process the XML descriptors.
     *
     * @return Parser which may be used to process the EJB descriptors.
     * @throws TaskException If the parser cannot be created or configured.
     */
    private SAXParser getParser()
        throws TaskException
    {

        SAXParser saxParser = null;
        try
        {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating( true );
            saxParser = saxParserFactory.newSAXParser();
        }
        catch( SAXException e )
        {
            String msg = "Unable to create a SAXParser: " + e.getMessage();
            throw new TaskException( msg, e );
        }
        catch( ParserConfigurationException e )
        {
            String msg = "Unable to create a SAXParser: " + e.getMessage();
            throw new TaskException( msg, e );
        }

        return saxParser;
    }

    /**
     * Verifies that the user selections are valid.
     *
     * @throws TaskException If the user selections are invalid.
     */
    private void checkConfiguration()
        throws TaskException
    {

        if( ejbdescriptor == null )
        {
            String msg = "The standard EJB descriptor must be specified using "
                + "the \"ejbdescriptor\" attribute.";
            throw new TaskException( msg );
        }
        if( ( !ejbdescriptor.exists() ) || ( !ejbdescriptor.isFile() ) )
        {
            String msg = "The standard EJB descriptor (" + ejbdescriptor
                + ") was not found or isn't a file.";
            throw new TaskException( msg );
        }

        if( iasdescriptor == null )
        {
            String msg = "The iAS-speific XML descriptor must be specified using"
                + " the \"iasdescriptor\" attribute.";
            throw new TaskException( msg );
        }
        if( ( !iasdescriptor.exists() ) || ( !iasdescriptor.isFile() ) )
        {
            String msg = "The iAS-specific XML descriptor (" + iasdescriptor
                + ") was not found or isn't a file.";
            throw new TaskException( msg );
        }

        if( dest == null )
        {
            String msg = "The destination directory must be specified using "
                + "the \"dest\" attribute.";
            throw new TaskException( msg );
        }
        if( ( !dest.exists() ) || ( !dest.isDirectory() ) )
        {
            String msg = "The destination directory (" + dest + ") was not "
                + "found or isn't a directory.";
            throw new TaskException( msg );
        }

        if( ( iashome != null ) && ( !iashome.isDirectory() ) )
        {
            String msg = "If \"iashome\" is specified, it must be a valid "
                + "directory (it was set to " + iashome + ").";
            throw new TaskException( msg );
        }
    }

    /**
     * Executes the EJBc utility using the SAXParser provided.
     *
     * @param saxParser SAXParser that may be used to process the EJB
     *      descriptors
     * @throws TaskException If there is an error reading or parsing the XML
     *      descriptors
     */
    private void executeEjbc( SAXParser saxParser )
        throws TaskException
    {
        IPlanetEjbc ejbc = new IPlanetEjbc( ejbdescriptor,
                                            iasdescriptor,
                                            dest,
                                            getClasspath().toString(),
                                            saxParser );
        ejbc.setRetainSource( keepgenerated );
        ejbc.setDebugOutput( debug );
        if( iashome != null )
        {
            ejbc.setIasHomeDir( iashome );
        }

        try
        {
            ejbc.execute();
        }
        catch( IOException e )
        {
            String msg = "An IOException occurred while trying to read the XML "
                + "descriptor file: " + e.getMessage();
            throw new TaskException( msg, e );
        }
        catch( SAXException e )
        {
            String msg = "A SAXException occurred while trying to read the XML "
                + "descriptor file: " + e.getMessage();
            throw new TaskException( msg, e );
        }
        catch( IPlanetEjbc.EjbcException e )
        {
            String msg = "An exception occurred while trying to run the ejbc "
                + "utility: " + e.getMessage();
            throw new TaskException( msg, e );
        }
    }
}
