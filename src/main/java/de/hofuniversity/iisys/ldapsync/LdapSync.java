package de.hofuniversity.iisys.ldapsync;

import de.hofuniversity.iisys.ldapsync.config.SyncConfig;
import de.hofuniversity.iisys.ldapsync.config.XMLConfigReader;
import de.hofuniversity.iisys.ldapsync.model.ILdapUserFactory;
import de.hofuniversity.iisys.ldapsync.model.LdapUserFactory;

/**
 * Startup class for the LDAP synchronization program for Apache Shindig and
 * Apache Rave. It reads the configuration and determines which properties to
 * synchronize in which direction.
 * 
 * @author fholzschuher2
 * 
 */
public class LdapSync
{
    private final String fConfigPath;

    /**
     * Creates a new LDAP synchronization instance using the configuration
     * specified under the given path. Throws a NullPointerException if the
     * given path is null or empty.
     * 
     * @param configPath
     *            path to configuration file
     */
    public LdapSync(String configPath)
    {
        if (configPath == null || configPath.isEmpty())
        {
            throw new NullPointerException("no configuration path given");
        }

        fConfigPath = configPath;
    }

    /**
     * Reads the configuration, tests the LDAP connection and if successful
     * starts a thread to regularly perform updates.
     * 
     * @throws Exception
     *             if any part of the startup process fails
     */
    public void start() throws Exception
    {
        // read configuration
        XMLConfigReader cReader = new XMLConfigReader(fConfigPath);
        SyncConfig config = cReader.readConfig();

        // create connector
        ILdapConnector conn = new SimpleLdapConnector(config);

        // test connection
        conn.connect();
        conn.disconnect();

        // user factory
        ILdapUserFactory userFactory = new LdapUserFactory(config);

        // create buffer
        LdapBuffer buffer = new LdapBuffer(conn, userFactory);

        // create end point factory
        ISyncEndpointFactory endPointFactory = new SyncEndpointFactory(config,
            buffer);

        // start scheduler
        SyncScheduler scheduler = new SyncScheduler(config, conn,
            endPointFactory, buffer);
        Thread schedThread = new Thread(scheduler);
        schedThread.start();

        // TODO: deliver external interface to trigger synchronization
    }

    /**
     * Simple startup routine that creates an LDAP synchronizer witch the
     * configuration file at the path specified or with default parameters and
     * starts it.
     * 
     * @param args
     *            first parameter can be the path to a configuration file
     */
    public static void main(String[] args)
    {
        String path = "ldapConfig.xml";

        if (args != null && args.length > 0 && !args[0].isEmpty())
        {
            path = args[0];
        }

        try
        {
            new LdapSync(path).start();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
