package de.hofuniversity.iisys.ldapsync.endpoints;

import java.util.List;
import java.util.Set;

import de.hofuniversity.iisys.ldapsync.LdapBuffer;
import de.hofuniversity.iisys.ldapsync.config.SyncEndpointConfig;
import de.hofuniversity.iisys.ldapsync.config.SyncRule;
import de.hofuniversity.iisys.ldapsync.model.ILdapUser;

/**
 * End point implementation for Apache Rave's default back-end.
 * 
 * @author fholzschuher2
 * 
 */
public class RaveEndpoint extends ASyncEndpoint
{
    // TODO: not working ... why?
    private static final String USER_API = "api/rpc/users/";

    private static final String HOST = "host";
    private static final String USER = "user";
    private static final String PASSWORD = "password";

    private final LdapBuffer fBuffer;
    private final List<SyncRule> fRules;

    private final String fHost, fUser, fPassword;

    /**
     * Creates an end point connecting to Apache Rave specified by the given
     * configuration object. Throws a NullPointerException if any parameter or
     * needed property is null.
     * 
     * @param config
     *            configuration object to use
     * @param buffer
     *            LDAP buffer to use
     */
    public RaveEndpoint(SyncEndpointConfig config, LdapBuffer buffer)
    {
        super(buffer, config);

        if (config == null)
        {
            throw new NullPointerException("configuration was null");
        }
        if (config.getMapping() == null)
        {
            throw new NullPointerException("synchronization rules were null");
        }
        if (buffer == null)
        {
            throw new NullPointerException("ldap buffer was null");
        }

        fBuffer = buffer;
        fRules = config.getMapping();

        fHost = config.getProperties().get(HOST);
        if (fHost == null || fHost.isEmpty())
        {
            throw new NullPointerException("host to connect to was null");
        }

        fUser = config.getProperties().get(USER);
        if (fUser == null || fUser.isEmpty())
        {
            throw new NullPointerException("user ID to use was null");
        }

        fPassword = config.getProperties().get(PASSWORD);
        if (fPassword == null || fPassword.isEmpty())
        {
            throw new NullPointerException("password to use was null");
        }
    }

    @Override
    protected void preHook()
    {
        // TODO: establish connection

        // TODO Auto-generated method stub
    }

    @Override
    protected void postHook()
    {
        // TODO Auto-generated method stub

        // TODO: disconnect
    }

    @Override
    protected List<Object> getValues(String name, String att)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setAttribute(String name, String att, Object val)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void setAttribute(String name, String att, List<Object> vals)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void addValues(String name, String att, List<Object> vals)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void removeAttribute(String name, String att)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected Set<String> getUserNames()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void createUser(ILdapUser user)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void deleteUser(String name)
    {
        // TODO Auto-generated method stub

    }
}
