package de.hofuniversity.iisys.ldapsync;

import java.util.List;

import de.hofuniversity.iisys.ldapsync.endpoints.ISyncEndpoint;

/**
 * Factory interface for creating ISyncEndpoint objects that can be used for
 * synchronization.
 * 
 * @author fholzschuher2
 * 
 */
public interface ISyncEndpointFactory
{
    /**
     * Creates all configured end points and links them to an LDAP connector.
     * This method should only be called once, unless duplicate end points are
     * desired.
     * 
     * @return newly created end points for synchronization
     */
    public List<ISyncEndpoint> createEndpoints();
}
