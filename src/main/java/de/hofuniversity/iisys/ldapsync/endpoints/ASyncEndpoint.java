package de.hofuniversity.iisys.ldapsync.endpoints;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;

import de.hofuniversity.iisys.ldapsync.LdapBuffer;
import de.hofuniversity.iisys.ldapsync.config.SyncEndpointConfig;
import de.hofuniversity.iisys.ldapsync.config.SyncRule;
import de.hofuniversity.iisys.ldapsync.model.ILdapUser;

/**
 * Abstract implementation of an end point providing predefined methods for
 * common synchronization steps and operations. Here, the implementations are
 * responsible for monitoring which values have actually changed. The sequence
 * is: create LDAP users, delete end point users, create end point users, delete
 * LDAP users and rules in the order they were specified.
 * 
 * @author fholzschuher2
 * 
 */
public abstract class ASyncEndpoint implements ISyncEndpoint
{
    private final LdapBuffer fLdap;
    private final List<SyncRule> fRules;

    private final boolean fCreateOwn;
    private final boolean fCreateLdap;
    private final boolean fDeleteOwn;
    private final boolean fDeleteLdap;

    private final Set<String> fCreatedUsers;

    private Set<String> fOwnUsers;
    private Map<String, ILdapUser> fLdapUsers;

    /**
     * Creates an abstract end point, executing a configurable standard
     * synchronization procedure. Throws a NullPointerException if any
     * parameters or the set of rules are null.
     * 
     * @param ldap
     *            LDAP connector to use
     * @param config
     *            configuration object to use
     */
    public ASyncEndpoint(LdapBuffer ldap, SyncEndpointConfig config)
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

        fCreatedUsers = new HashSet<String>();
    }

    // methods to implement

    /**
     * Gets all values of a certain attribute belonging to a certain user. None
     * of the parameters may be null. Returns null if there is no such user or
     * attribute.
     * 
     * @param name
     *            name of the user the attribute belongs to
     * @param att
     *            name of the attribute to get
     * @return list of attribute values or null
     */
    protected abstract List<Object> getValues(String name, String att);

    /**
     * Sets an attribute belonging to a certain user to a certain value. None of
     * the parameters may be null.
     * 
     * @param name
     *            name of the user the attribute belongs to
     * @param att
     *            name of the attribute to set
     * @param val
     *            value to set as the attribute's value
     */
    protected abstract void setAttribute(String name, String att, Object val);

    /**
     * Sets multiple values for an attribute of a certain user. None of the
     * attributes may be null.
     * 
     * @param name
     *            name of the user the attribute belongs to
     * @param att
     *            name of the attribute to set
     * @param vals
     *            list of values for the attribute
     */
    protected abstract void setAttribute(String name, String att,
        List<Object> vals);

    /**
     * Adds a list of attributes to an attribute of a certain user, without
     * deleting old values. If the attribute does not exist it should be
     * created. None of the parameters may be null.
     * 
     * @param name
     *            name of the user the attribute belongs to
     * @param att
     *            attribute to add values to
     * @param vals
     *            values to add to the attribute
     */
    protected abstract void
        addValues(String name, String att, List<Object> vals);

    /**
     * Removes an attribute from a certain user. Calls with non-existent
     * attributes attributes should be ignored. None of the parameters may be
     * null.
     * 
     * @param name
     *            name of the user the attribute belongs to
     * @param att
     *            name of the attribute to remove
     */
    protected abstract void removeAttribute(String name, String att);

    /**
     * Retrieves a set of all users that exist at the end point.
     * 
     * @return set of all known users
     */
    protected abstract Set<String> getUserNames();

    /**
     * Creates a new user at the end point based on the given LDAP user.
     * Parameter may not be null.
     * 
     * @param user
     *            LDAP user to base the user on
     */
    protected abstract void createUser(ILdapUser user);

    /**
     * Deletes a user at the end point. Parameter may not be null.
     * 
     * @param name
     *            name of the user to delete
     */
    protected abstract void deleteUser(String name);

    /**
     * Hook to execute before all other operations. Can be left blank.
     */
    protected abstract void preHook();

    /**
     * Hook to execute after all other operations. Can be left blank.
     */
    protected abstract void postHook();

    // default synchronization routine

    public void sync()
    {
        preHook();

        fOwnUsers = getUserNames();
        fLdapUsers = fLdap.getAllUsers();

        // create users in LDAP that only exist for the end point
        if (fCreateLdap)
        {
            for (String name : fOwnUsers)
            {
                if (!fLdapUsers.containsKey(name))
                {
                    fLdap.createUser(name);
                }
            }
        }

        // delete users at the end point that don't exist in LDAP
        if (fDeleteOwn)
        {
            for (String name : fOwnUsers)
            {
                if (!fLdapUsers.containsKey(name))
                {
                    deleteUser(name);
                }
            }
        }

        // get a fresh list of users
        fOwnUsers = getUserNames();

        // create users that only exist in LDAP
        if (fCreateOwn)
        {
            for (Entry<String, ILdapUser> userE : fLdapUsers.entrySet())
            {
                if (!fOwnUsers.contains(userE.getKey()))
                {
                    createUser(userE.getValue());
                    fCreatedUsers.add(userE.getKey());
                }
            }
        }

        // get a fresh list of users
        fOwnUsers = getUserNames();

        // delete users in LDAP that only exist at the end point
        if (fDeleteLdap)
        {
            Set<String> toDelete = new HashSet<String>();

            for (Entry<String, ILdapUser> userE : fLdapUsers.entrySet())
            {
                if (!fOwnUsers.contains(userE.getKey()))
                {
                    toDelete.add(userE.getKey());
                }
            }

            for (String name : toDelete)
            {
                fLdap.deleteUser(name);
            }
        }

        handleRules();

        fCreatedUsers.clear();

        postHook();
    }

    private void handleRules()
    {
        for (SyncRule rule : fRules)
        {
            for (String user : fOwnUsers)
            {
                switch (rule.getDirection())
                {
                    case TO_LDAP:
                        handleToLdap(user, rule);
                        break;

                    case FROM_LDAP:
                        handleFromLdap(user, rule);
                        break;

                    case BOTH:
                        handleBoth(user, rule);
                        break;
                }
            }
        }
    }

    private void handleToLdap(String name, SyncRule rule)
    {
        String ldapAtt = rule.getLdapProp();
        List<Object> values = getValues(name, rule.getEndPointProp());
        if (values == null || values.isEmpty())
        {
            // break if not found or not set
            return;
        }

        switch (rule.getOperation())
        {
            case ADD_TO_LIST:
                fLdap.addToAttribute(name, ldapAtt, values);
                break;

            case COPY:
                fLdap.setAttribute(name, ldapAtt, values);
                break;

            case COPY_FIRST_ELEMENT:
                fLdap.setAttribute(name, ldapAtt, values.get(0));
                break;

            case COPY_IF_NEWER:
                // TODO: how?
                break;

            case COPY_IF_NULL:
                Attribute att = fLdap.getCurrentAttribute(name, ldapAtt);
                boolean empty = true;
                try
                {
                    if (att != null && att.get() != null
                        && !att.get().toString().isEmpty())
                    {
                        empty = false;
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                if (empty)
                {
                    fLdap.setAttribute(name, ldapAtt, values);
                }
                break;

            case COPY_ON_CREATE:
                if (fLdap.getUser(name).isNew())
                {
                    fLdap.setAttribute(name, ldapAtt, values);
                }
                break;
        }
    }

    private void handleFromLdap(String name, SyncRule rule)
    {
        String ownAtt = rule.getEndPointProp();
        Attribute att = fLdap.getCurrentAttribute(name, rule.getLdapProp());
        if (att == null)
        {
            // break if not found
            return;
        }

        List<Object> ldapValues = new ArrayList<Object>();
        try
        {
            NamingEnumeration<?> valEnum = att.getAll();
            while (valEnum.hasMore())
            {
                ldapValues.add(valEnum.next());
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        switch (rule.getOperation())
        {
            case ADD_TO_LIST:
                addValues(name, ownAtt, ldapValues);
                break;

            case COPY:
                if (ldapValues.size() > 1)
                {
                    setAttribute(name, ownAtt, ldapValues);
                } else
                {
                    setAttribute(name, ownAtt, ldapValues.get(0));
                }
                break;

            case COPY_FIRST_ELEMENT:
                setAttribute(name, ownAtt, ldapValues.get(0));
                break;

            case COPY_IF_NEWER:
                // TODO: how?
                break;

            case COPY_IF_NULL:
                List<Object> ownValues = getValues(name, ownAtt);
                if (ownValues == null || ownValues.isEmpty())
                {
                    if (ldapValues.size() > 1)
                    {
                        setAttribute(name, ownAtt, ldapValues);
                    } else
                    {
                        setAttribute(name, ownAtt, ldapValues.get(0));
                    }
                }
                break;

            case COPY_ON_CREATE:
                if (fCreatedUsers.contains(name))
                {
                    if (ldapValues.size() > 1)
                    {
                        setAttribute(name, ownAtt, ldapValues);
                    } else
                    {
                        setAttribute(name, ownAtt, ldapValues.get(0));
                    }
                }
                break;
        }
    }

    private void handleBoth(String name, SyncRule rule)
    {
        String ldapAtt = rule.getLdapProp();
        String ownAtt = rule.getEndPointProp();
        List<Object> ownValues = getValues(name, ownAtt);
        Attribute att = fLdap.getCurrentAttribute(name, ldapAtt);

        List<Object> ldapValues = new ArrayList<Object>();
        if (att != null)
        {
            try
            {
                NamingEnumeration<?> valEnum = att.getAll();
                while (valEnum.hasMore())
                {
                    ldapValues.add(valEnum.next());
                }
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        switch (rule.getOperation())
        {
            case ADD_TO_LIST:
                // TODO: merging strategy?
                break;

            case COPY_IF_NEWER:
                // TODO: how?
                break;

            case COPY_IF_NULL:
                boolean ldapNull = false;
                boolean ownNull = false;

                if (ldapValues.isEmpty())
                {
                    ldapNull = true;
                }
                if (ownValues == null || ownValues.isEmpty())
                {
                    ownNull = true;
                }

                if (ldapNull && !ownNull)
                {
                    fLdap.setAttribute(name, ldapAtt, ownValues);
                } else if (ownNull && !ldapNull)
                {
                    setAttribute(name, ownAtt, ldapValues);
                }
                break;
            case COPY_ON_CREATE:
                /*
                 * check which side the user was created on and copy from the
                 * other side
                 */
                if (fCreatedUsers.contains(name))
                {
                    // user created at end point
                    if (ldapValues.size() > 1)
                    {
                        setAttribute(name, ownAtt, ldapValues);
                    } else
                    {
                        setAttribute(name, ownAtt, ldapValues.get(0));
                    }
                }

                ILdapUser ldapUser = fLdap.getUser(name);
                if (ldapUser != null && ldapUser.isNew())
                {
                    // user created in LDAP
                    fLdap.setAttribute(name, ldapAtt, ownValues);
                }
                break;
        }
    }
}
