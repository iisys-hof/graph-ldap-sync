package de.hofuniversity.iisys.ldapsync.endpoints;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;

import de.hofuniversity.iisys.ldapsync.LdapBuffer;
import de.hofuniversity.iisys.ldapsync.config.SyncDirections;
import de.hofuniversity.iisys.ldapsync.config.SyncEndpointConfig;
import de.hofuniversity.iisys.ldapsync.config.SyncOperations;
import de.hofuniversity.iisys.ldapsync.config.SyncRule;
import de.hofuniversity.iisys.ldapsync.model.ILdapUser;

/**
 * Fake end point that can be filled with operations for testing purposes
 * 
 * @author fholzschuher2
 * 
 */
public class TestEndpoint implements ISyncEndpoint
{
    private final LdapBuffer fLdap;
    private final List<SyncRule> fRules;

    private final boolean fCreateOwn;
    private final boolean fCreateLdap;
    private final boolean fDeleteOwn;
    private final boolean fDeleteLdap;

    private final Map<String, Map<String, String>> fUsers;

    /**
     * Creates a test end point reading and manipulating data in the given
     * buffer. Throws a NullPointerException if any parameter is null.
     * 
     * @param ldap
     *            buffer to read from and write to
     */
    public TestEndpoint(LdapBuffer ldap, SyncEndpointConfig config)
    {
        if (ldap == null)
        {
            throw new NullPointerException("ldap buffer was null");
        }
        if (config == null)
        {
            throw new NullPointerException("configuration object was null");
        }

        fLdap = ldap;
        fRules = config.getMapping();

        fCreateOwn = config.getCreateOwnEntries();
        fCreateLdap = config.getCreateLdapEntries();
        fDeleteOwn = config.getDeleteOwnEntries();
        fDeleteLdap = config.getDeleteLdapEntries();

        // create some fake users
        fUsers = new HashMap<String, Map<String, String>>();
        Map<String, String> user = null;
        String[] names = null;

        for (Entry<String, String> userE : config.getProperties().entrySet())
        {
            user = new HashMap<String, String>();
            user.put("id", userE.getKey());

            names = userE.getValue().split(" ");
            user.put("name.formatted", userE.getValue());
            user.put("name.givenName", names[0]);
            user.put("name.familyName", names[1]);

            fUsers.put(userE.getKey(), user);
        }
    }

    public void sync()
    {
        Map<String, ILdapUser> users = fLdap.getAllUsers();

        Attribute att = null;
        NamingEnumeration<? extends Attribute> atts = null;
        NamingEnumeration<?> vals = null;

        // create new fake users
        if (fCreateOwn)
        {
            for (Entry<String, ILdapUser> userE : users.entrySet())
            {
                if (!fUsers.containsKey(userE.getKey()))
                {
                    System.out.println("create own user " + userE.getKey());

                    try
                    {
                        createLocal(userE.getKey());
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        // create new LDAP users
        if (fCreateLdap)
            ;
        {
            for (Entry<String, Map<String, String>> userE : fUsers.entrySet())
            {
                if (!users.containsKey(userE.getKey()))
                {
                    System.out.println("create LDAP user " + userE.getKey());
                    fLdap.createUser(userE.getKey());

                    try
                    {
                        matchAttributes(userE.getKey());
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        // delete fake users
        if (fDeleteOwn)
        {
            Set<String> toDelete = new HashSet<String>();

            for (Entry<String, Map<String, String>> userE : fUsers.entrySet())
            {
                if (!users.containsKey(userE.getKey()))
                {
                    System.out.println("delete own user " + userE.getKey());
                    toDelete.add(userE.getKey());
                }
            }

            for (String name : toDelete)
            {
                fUsers.remove(name);
            }
        }

        // delete LDAP users
        if (fDeleteLdap)
        {
            Set<String> toDelete = new HashSet<String>();

            for (Entry<String, ILdapUser> userE : users.entrySet())
            {
                if (!fUsers.containsKey(userE.getKey()))
                {
                    System.out.println("delete LDAP user " + userE.getKey());
                    toDelete.add(userE.getKey());
                }
            }

            for (String name : toDelete)
            {
                fLdap.deleteUser(name);
            }
        }

        int num = 0;

        // print
        for (Entry<String, ILdapUser> userE : users.entrySet())
        {
            try
            {
                matchAttributes(userE.getKey());
            } catch (Exception e)
            {
                e.printStackTrace();
            }

            System.out.println("user " + num + ": " + userE.getKey());

            System.out.print("\t(");
            atts = userE.getValue().getAttributes().getAll();
            try
            {
                while (atts.hasMore())
                {
                    att = atts.next();
                    vals = att.getAll();

                    System.out.print(att.getID() + ": ");
                    while (vals.hasMore())
                    {
                        System.out.print(vals.next());

                        if (vals.hasMore())
                        {
                            System.out.print('|');
                        }
                    }

                    if (atts.hasMore())
                    {
                        System.out.print(", ");
                    }
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            System.out.println(')');

            ++num;
        }

        System.out.println(num + " users");
    }

    private void createLocal(String name) throws Exception
    {
        Map<String, String> localUser = new HashMap<String, String>();

        String ldapKey = null;
        String localKey = null;
        Object ldapValue = null;

        for (SyncRule rule : fRules)
        {
            ldapKey = rule.getLdapProp();
            localKey = rule.getEndPointProp();

            if (rule.getDirection() == SyncDirections.FROM_LDAP
                && rule.getOperation() == SyncOperations.COPY_ON_CREATE)
            {
                ldapValue = fLdap.getCurrentAttribute(name, ldapKey).get();
                localUser.put(localKey, ldapValue.toString());
            }
        }

        fUsers.put(name, localUser);
    }

    private void matchAttributes(String name) throws Exception
    {
        Map<String, String> localUser = fUsers.get(name);
        if (localUser == null)
        {
            return;
        }

        String ldapKey = null;
        String localKey = null;
        Object ldapValue = null;
        Object localValue = null;

        for (SyncRule rule : fRules)
        {
            ldapKey = rule.getLdapProp();
            localKey = rule.getEndPointProp();

            switch (rule.getDirection())
            {
                case FROM_LDAP:
                    ldapValue = fLdap.getCurrentAttribute(name, ldapKey).get();
                    switch (rule.getOperation())
                    {
                        case COPY:
                            localUser.put(localKey, ldapValue.toString());
                            break;
                    }
                    break;

                case TO_LDAP:
                    localValue = localUser.get(localKey);
                    switch (rule.getOperation())
                    {
                        case COPY:
                            fLdap.setAttribute(name, ldapKey, localValue);
                            break;
                    }
                    break;

                case BOTH:
                    switch (rule.getOperation())
                    {
                        case ADD_TO_LIST:

                            break;
                    }
                    break;
            }
        }
    }
}
