package de.hofuniversity.iisys.ldapsync;

import java.util.ArrayList;
import java.util.List;

import de.hofuniversity.iisys.ldapsync.config.SyncConfig;
import de.hofuniversity.iisys.ldapsync.config.SyncEndpointConfig;
import de.hofuniversity.iisys.ldapsync.endpoints.ISyncEndpoint;
import de.hofuniversity.iisys.ldapsync.endpoints.RaveEndpoint;
import de.hofuniversity.iisys.ldapsync.endpoints.ShindigGraphEndpoint;
import de.hofuniversity.iisys.ldapsync.endpoints.TestEndpoint;

/**
 * Factory creating ISyncEndpoint objects from configuration parameters that can
 * be used for synchronization.
 * 
 * @author fholzschuher2
 * 
 */
public class SyncEndpointFactory implements ISyncEndpointFactory
{
    private final List<SyncEndpointConfig> fConfigs;
    private final LdapBuffer fLdap;

    /**
     * Creates an end point factory, using configurations from the given
     * configuration object and linking them to the given LDAP buffer.
     * 
     * @param config
     *            configuration to use
     * @param ldap
     *            LDAP buffer to use
     */
    public SyncEndpointFactory(SyncConfig config, LdapBuffer ldap)
    {
        if (config == null)
        {
            throw new NullPointerException("configuration was null");
        }
        if (config.getEndpoints() == null)
        {
            throw new NullPointerException("list of endpoint configurations "
                + "was null");
        }
        if (ldap == null)
        {
            throw new NullPointerException("ldap buffer was null");
        }

        fConfigs = config.getEndpoints();
        fLdap = ldap;
    }

    public List<ISyncEndpoint> createEndpoints()
    {
        List<ISyncEndpoint> endPoints = new ArrayList<ISyncEndpoint>();

        ISyncEndpoint ep = null;
        for (SyncEndpointConfig config : fConfigs)
        {
            ep = getEndpoint(config);
            if (ep != null)
            {
                endPoints.add(ep);
            }
        }

        return endPoints;
    }

    private ISyncEndpoint getEndpoint(SyncEndpointConfig config)
    {
        final String type = config.getType();
        ISyncEndpoint ep = null;

        if ("shindig-graph".equalsIgnoreCase(type))
        {
            ep = new ShindigGraphEndpoint(config, fLdap);
        } else if ("rave".equalsIgnoreCase(type))
        {
            ep = new RaveEndpoint(config, fLdap);
        } else if ("test".equalsIgnoreCase(type))
        {
            ep = new TestEndpoint(fLdap, config);
        } else
        {
            System.err.println("unknown endpoint type: " + type);
        }

        return ep;
    }
}
