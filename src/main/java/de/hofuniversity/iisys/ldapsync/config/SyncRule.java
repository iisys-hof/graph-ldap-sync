package de.hofuniversity.iisys.ldapsync.config;

/**
 * Rule class providing a mapping of a LDAP property to the property of a
 * certain application and rules on how to synchronize them.
 * 
 * @author fholzschuher2
 * 
 */
public class SyncRule
{
    private String fLdapProp, fEndPointProp;
    private SyncDirections fDirection;
    private SyncOperations fOperation;

    /**
     * @return name of the property in LDAP
     */
    public String getLdapProp()
    {
        return fLdapProp;
    }

    /**
     * @return name of the property at the end point
     */
    public String getEndPointProp()
    {
        return fEndPointProp;
    }

    /**
     * @return in which direction to synchronize
     */
    public SyncDirections getDirection()
    {
        return fDirection;
    }

    /**
     * @return how to handle existing values
     */
    public SyncOperations getOperation()
    {
        return fOperation;
    }

    /**
     * @param ldapProp
     *            name of the property in LDAP
     */
    public void setLdapProp(String ldapProp)
    {
        fLdapProp = ldapProp;
    }

    /**
     * @param endPointProp
     *            name of the property at the end point
     */
    public void setEndPointProp(String endPointProp)
    {
        fEndPointProp = endPointProp;
    }

    /**
     * @param direction
     *            in which direction to synchronize
     */
    public void setDirection(SyncDirections direction)
    {
        fDirection = direction;
    }

    /**
     * @param operation
     *            how to handle existing values
     */
    public void setOperation(SyncOperations operation)
    {
        this.fOperation = operation;
    }
}
