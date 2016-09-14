package de.hofuniversity.iisys.ldapsync.model;

import java.util.List;

import de.hofuniversity.iisys.ldapsync.config.SyncConfig;

/**
 * Factory that creates user objects ready to be stored in an LDAP directory as
 * defined by the configuration.
 * 
 * @author fholzschuher2
 * 
 */
public class LdapUserFactory implements ILdapUserFactory
{
    private final List<String> fClasses, fOus;

    /**
     * Creates a user factory, giving users the initial object classes and
     * organizational units configured. Throws a NullPointerException if
     * configuration, initial classes or initial organizational units are null
     * or no organizational units are defined.
     * 
     * @param config
     *            configuration object to use
     */
    public LdapUserFactory(SyncConfig config)
    {
        if (config == null)
        {
            throw new NullPointerException("configuration object was null");
        }
        if (config.getInitialClasses() == null)
        {
            throw new NullPointerException("initial classes list was null");
        }

        fOus = config.getInitialOus();
        if (fOus == null || fOus.isEmpty())
        {
            throw new NullPointerException(
                "no initial organizational units given");
        }

        fClasses = config.getInitialClasses();
    }

    /**
     * Creates a new user with the given name and the configured initial values.
     * Name may not be null or empty.
     * 
     * @param name
     *            UID for the user
     * @return newly created user with the configured initial values
     */
    public ILdapUser createUser(String name)
    {
        return new SimpleLdapUser(name, fClasses, fOus);
    }
}
