package de.hofuniversity.iisys.ldapsync.model;

/**
 * Factory that creates user objects ready to be stored in an LDAP directory.
 * 
 * @author fholzschuher2
 * 
 */
public interface ILdapUserFactory
{
    /**
     * Creates a new user with the given name and some initial values. Name may
     * not be null or empty.
     * 
     * @param name
     *            UID for the user
     * @return newly created user with initial values
     */
    public ILdapUser createUser(String name);
}
