package de.hofuniversity.iisys.ldapsync.endpoints;

/**
 * Interface for a synchronization end point providing the functionality to
 * extract and set people's properties in another application based on LDAP in-
 * and output.
 * 
 * @author fholzschuher2
 * 
 */
public interface ISyncEndpoint
{
    /**
     * Tells the end point to synchronize with the LDAP service, based on the
     * current state of the buffer. If any connections are needed, they should
     * be established when called and discarded afterwards as there can be a
     * long time span between calls.
     * 
     * @param ldapContents
     *            result from the latest full query
     */
    public void sync();
}
