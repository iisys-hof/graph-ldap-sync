package de.hofuniversity.iisys.ldapsync.config;

import java.util.List;
import java.util.Map;

/**
 * Generic configuration that defines how to connect to an end point of a
 * certain type and the associated synchronization rules.
 * 
 * @author fholzschuher2
 * 
 */
public class SyncEndpointConfig
{
    private String fType;
    private boolean fCreateOwnEntries, fDeleteOwnEntries;
    private boolean fCreateLdapEntries, fDeleteLdapEntries;
    private Map<String, String> fProperties;
    private List<SyncRule> fMapping;

    /**
     * @return name of the end point type
     */
    public String getType()
    {
        return fType;
    }

    /**
     * @return map of configuration properties for the end point
     */
    public Map<String, String> getProperties()
    {
        return fProperties;
    }

    /**
     * @return rules that determine how to store attributes
     */
    public List<SyncRule> getMapping()
    {
        return fMapping;
    }

    /**
     * @param type
     *            name of the end point type
     */
    public void setType(String type)
    {
        fType = type;
    }

    /**
     * @param properties
     *            map of configuration properties for the end point
     */
    public void setProperties(Map<String, String> properties)
    {
        fProperties = properties;
    }

    /**
     * @param mapping
     *            rules that determine how to store attributes
     */
    public void setMapping(List<SyncRule> mapping)
    {
        fMapping = mapping;
    }

    /**
     * @return whether to create end point entries for LDAP entries
     */
    public boolean getCreateOwnEntries()
    {
        return fCreateOwnEntries;
    }

    /**
     * @param createOwnEntries
     *            whether to create end point entries for LDAP entries
     */
    public void setCreateOwnEntries(boolean createOwnEntries)
    {
        fCreateOwnEntries = createOwnEntries;
    }

    /**
     * @return whether to delete end point entries that are not in LDAP
     */
    public boolean getDeleteOwnEntries()
    {
        return fDeleteOwnEntries;
    }

    /**
     * @param deleteOwnEntries
     *            whether to delete end point entries that are not in LDAP
     */
    public void setDeleteOwnEntries(boolean deleteOwnEntries)
    {
        fDeleteOwnEntries = deleteOwnEntries;
    }

    /**
     * @return whether to delete LDAP entries that have no end point equivalent
     */
    public boolean getDeleteLdapEntries()
    {
        return fDeleteLdapEntries;
    }

    /**
     * @param deleteLdapEntries
     *            whether to delete LDAP entries that have no end point
     *            equivalent
     */
    public void setDeleteLdapEntries(boolean deleteLdapEntries)
    {
        fDeleteLdapEntries = deleteLdapEntries;
    }

    /**
     * @return whether to create LDAP entries for end point entries
     */
    public boolean getCreateLdapEntries()
    {
        return fCreateLdapEntries;
    }

    /**
     * @param fCreateLdapEntries
     *            whether to create LDAP entries for end point entries
     */
    public void setCreateLdapEntries(boolean createLdapEntries)
    {
        fCreateLdapEntries = createLdapEntries;
    }
}
