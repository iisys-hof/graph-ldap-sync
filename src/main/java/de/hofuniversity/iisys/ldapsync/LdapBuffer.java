package de.hofuniversity.iisys.ldapsync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;

import de.hofuniversity.iisys.ldapsync.model.ILdapUser;
import de.hofuniversity.iisys.ldapsync.model.ILdapUserFactory;
import de.hofuniversity.iisys.ldapsync.model.SimpleLdapUser;

/**
 * Class that holds a copy of the current LDAP data that can be modified by
 * several end point implementations and then synchronized back in a single
 * operation.
 * 
 * @author fholzschuher2
 * 
 */
public class LdapBuffer
{
    private final ILdapConnector fLdap;
    private final ILdapUserFactory fUserFactory;

    private final Map<String, Map<String, ModificationItem>> fModifications;
    private final Map<String, ILdapUser> fLdapUsers, fNewUsers, fAllUsers;
    private final Set<String> fDeletedUsers;

    /**
     * Creates an empty buffer that can hold changes to be written to an LDAP
     * directory service. Throws a NullPointerException if any argument is null.
     * 
     * @param ldap
     *            LDAP connector to use for writing changes
     * @param factory
     *            factory to use for user creation
     */
    public LdapBuffer(ILdapConnector ldap, ILdapUserFactory factory)
    {
        if (ldap == null)
        {
            throw new NullPointerException("ldap connector was null");
        }
        if (factory == null)
        {
            throw new NullPointerException("user factory was null");
        }

        fLdap = ldap;
        fUserFactory = factory;

        fModifications = new HashMap<String, Map<String, ModificationItem>>();
        fLdapUsers = new HashMap<String, ILdapUser>();
        fNewUsers = new HashMap<String, ILdapUser>();
        fAllUsers = new HashMap<String, ILdapUser>();
        fDeletedUsers = new HashSet<String>();
    }

    /**
     * Sets a fresh set of data from an LDAP directory service as the buffer's
     * state. As a consequence all stored changes are discarded. If the given
     * object is null, the buffer will be blank.
     * 
     * @param ldapContents
     * @throws Exception
     *             if handling results causes an Exception
     */
    @SuppressWarnings("rawtypes")
    public void setData(NamingEnumeration ldapContents) throws Exception
    {
        fNewUsers.clear();
        fDeletedUsers.clear();
        fModifications.clear();
        fLdapUsers.clear();

        /*
         * read and copy all users and their attributes from the result to
         * prevent further unnecessary access
         */
        if (ldapContents != null)
        {
            SearchResult result = null;
            NamingEnumeration<? extends Attribute> atts = null;
            NamingEnumeration<?> vals = null;
            Attributes localAtts = null;
            String name = null;
            String ouString = null;
            Attribute a = null;
            Attribute localA = null;

            while (ldapContents.hasMore())
            {
                result = (SearchResult) ldapContents.next();
                localAtts = new BasicAttributes();

                // comes in format "uid=name"
                // in a subtree search, it's "uid=name,ou=..."
                name = result.getName();
                ouString = null;
                if(name.indexOf(',') > 0)
                {
                    String[] split = name.split(",");
                    name = split[0];
                    ouString = split[1];
                }
                name = name.split("=")[1];

                // copy all to prevent additional lookups
                atts = result.getAttributes().getAll();
                while (atts.hasMore())
                {
                    a = atts.next();
                    localA = new BasicAttribute(a.getID());

                    // copy all values
                    vals = a.getAll();
                    while (vals.hasMore())
                    {
                        localA.add(vals.next());
                    }

                    localAtts.put(localA);
                }
                
                //add organizational unit hierarchy
                if(ouString != null)
                {
                    localA = new BasicAttribute("orgUnitString");
                    localA.add(ouString);
                    localAtts.put(localA);
                }

                // add to map
                fLdapUsers.put(name, new SimpleLdapUser(name, localAtts));
            }

            fAllUsers.putAll(fLdapUsers);
        }
    }

    /**
     * Checks if there already is a user with the given UID in the current LDAP
     * query result or among the newly created users in the buffer. Name may not
     * be null.
     * 
     * @param name
     *            UID of the user in question
     * @return whether the user already exists in the buffer
     */
    public boolean hasUser(String name)
    {
        boolean has = fAllUsers.containsKey(name);

        return has;
    }

    /**
     * Retrieves the user with the given name from the existing LDAP users or
     * the newly created users and returns null if there is no such user.
     * 
     * @param name
     *            name of the user
     * @return user or null
     */
    public ILdapUser getUser(String name)
    {
        ILdapUser user = fAllUsers.get(name);

        return user;
    }

    /**
     * Returns a map of all currently available users including existing LDAP
     * users as well as newly created users. Users that have already been
     * deleted from the buffer are not included.
     * 
     * @return map of all available users
     */
    public Map<String, ILdapUser> getAllUsers()
    {
        return fAllUsers;
    }

    /**
     * Returns the current value of an attribute of a person or null if there is
     * no value. The value returned by this method includes all modifications
     * that are stored but haven't been written yet, unlike direct object
     * access.
     * 
     * @param name
     *            name of the person
     * @param att
     *            name of the attribute
     * @return projected value of the attribute after the next update
     */
    public Attribute getCurrentAttribute(String name, String att)
    {
        Attribute attribute = null;

        // determine whether it's a new or an existing user
        ILdapUser user = fLdapUsers.get(name);
        if (user != null)
        {
            // check for queued modifications
            ModificationItem mod = getModification(name, att);

            if (mod != null)
            {
                int op = mod.getModificationOp();

                switch (op)
                {
                    case DirContext.REPLACE_ATTRIBUTE:
                        attribute = mod.getAttribute();
                        break;

                    case DirContext.REMOVE_ATTRIBUTE:
                        // remains null, won't exist anymore
                        break;

                    case DirContext.ADD_ATTRIBUTE:
                        // collect values
                        attribute = new BasicAttribute(att);

                        try
                        {
                            // old values
                            NamingEnumeration<?> vals = user.getAttribute(att)
                                .getAll();
                            while (vals.hasMore())
                            {
                                attribute.add(vals.next());
                            }

                            // new values
                            vals = mod.getAttribute().getAll();
                            while (vals.hasMore())
                            {
                                attribute.add(vals.next());
                            }
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        break;
                }
            } else
            {
                // still the original value
                attribute = user.getAttribute(att);
            }
        } else
        {
            user = fNewUsers.get(name);

            if (user != null)
            {
                attribute = user.getAttribute(att);
            }
        }

        return attribute;
    }

    private ModificationItem getModification(String name, String att)
    {
        ModificationItem mod = null;

        Map<String, ModificationItem> mods = fModifications.get(name);
        if (mods != null)
        {
            mod = mods.get(att);
        }

        return mod;
    }

    private void setModification(String name, String att, ModificationItem mod)
    {
        Map<String, ModificationItem> mods = fModifications.get(name);
        if (mods == null)
        {
            mods = new HashMap<String, ModificationItem>();
            fModifications.put(name, mods);
        }
        mods.put(att, mod);
    }

    private void removeModification(String name, String att)
    {
        Map<String, ModificationItem> mods = fModifications.get(name);
        if (mods != null)
        {
            mods.remove(att);
        }
    }

    /**
     * Adds a list of values to an attribute with multiple values without
     * checking for duplicates. If the attribute does not yet exist, it is
     * created. The calling class should filter out duplicates if that is the
     * required behavior. Parameters may not be null or empty.
     * 
     * @param name
     *            name of the entity the attribute belongs to
     * @param att
     *            name of the attribute
     * @param vals
     *            list of values to add
     */
    public void addToAttribute(String name, String att, List<Object> vals)
    {
        // existing users
        ILdapUser user = fLdapUsers.get(name);
        Attribute attr = null;

        if (user != null)
        {
            // existing changes
            ModificationItem mod = getModification(name, att);
            Attribute oldAtt = null;

            if (mod != null)
            {
                switch (mod.getModificationOp())
                {
                    case DirContext.ADD_ATTRIBUTE:
                        // aggregate with existing values to add
                        attr = mod.getAttribute();
                        for (Object val : vals)
                        {
                            attr.add(val);
                        }
                        mod = new ModificationItem(DirContext.ADD_ATTRIBUTE,
                            attr);

                        break;

                    case DirContext.REPLACE_ATTRIBUTE:
                        /*
                         * aggregate with existing values which will replace old
                         * ones
                         */
                        attr = mod.getAttribute();
                        for (Object val : vals)
                        {
                            attr.add(val);
                        }

                        oldAtt = user.getAttribute(att);
                        if (!areEqualLists(oldAtt, attr))
                        {
                            mod = new ModificationItem(
                                DirContext.REPLACE_ATTRIBUTE, attr);
                        }

                        break;

                    case DirContext.REMOVE_ATTRIBUTE:
                        // replace with new values
                        attr = new BasicAttribute(att);
                        for (Object val : vals)
                        {
                            attr.add(val);
                        }

                        oldAtt = user.getAttribute(att);
                        if (!areEqualLists(oldAtt, attr))
                        {
                            mod = new ModificationItem(
                                DirContext.REPLACE_ATTRIBUTE, attr);
                        }

                        break;
                }
            } else
            {
                attr = new BasicAttribute(att);
                for (Object val : vals)
                {
                    attr.add(val);
                }
                mod = new ModificationItem(DirContext.ADD_ATTRIBUTE, attr);
            }

            setModification(name, att, mod);
        } else
        {
            // users that haven't been created yet
            user = fNewUsers.get(name);
            if (user != null)
            {
                attr = user.getAttribute(att);

                if (attr == null)
                {
                    attr = new BasicAttribute(att);
                    user.addAttribute(attr);
                }

                for (Object val : vals)
                {
                    attr.add(val);
                }
            }
        }
    }

    private boolean areEqualLists(Attribute a1, Attribute a2)
    {
        boolean match = true;

        try
        {
            NamingEnumeration<?> ne1 = a1.getAll();
            NamingEnumeration<?> ne2 = a2.getAll();

            // compare all a1 values to a2
            while (ne1.hasMore())
            {
                if (!ne2.hasMore() || !ne1.next().equals(ne2.next()))
                {
                    match = false;
                    break;
                }
            }

            // if a2 has more values than a1, they're not equal
            if (ne2.hasMore())
            {
                match = false;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            match = false;
        }

        return match;
    }

    /**
     * Sets an attribute of an entity, overwriting any potential previous
     * values. Parameters may not be null or empty.
     * 
     * @param name
     *            name of the entity the attribute belongs
     * @param att
     *            name of the attribute
     * @param val
     *            value to set for the attribute
     */
    public void setAttribute(String name, String att, Object val)
    {
        // existing users
        ILdapUser user = fLdapUsers.get(name);

        if (user != null)
        {
            // check if the value matches the original value
            Object orgVal = null;
            Attribute a = user.getAttribute(att);
            if (a != null)
            {
                try
                {
                    orgVal = a.get();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            if (val.equals(orgVal))
            {
                // no modifications necessary
                removeModification(name, att);
            } else
            {
                // attribute set to new value, other modifications irrelevant
                Attribute attr = new BasicAttribute(att, val);
                ModificationItem mod = new ModificationItem(
                    DirContext.REPLACE_ATTRIBUTE, attr);
                setModification(name, att, mod);
            }
        }
        // users that haven't been created yet
        else
        {
            user = fNewUsers.get(name);
            if (user != null)
            {
                user.setAttribute(att, val);
            }
        }
    }

    /**
     * Sets an attribute with multiple values of an entity, overwriting any
     * potential previous values. Parameters may not be null or empty.
     * 
     * @param name
     *            name of the entity the attribute belongs
     * @param att
     *            name of the attribute
     * @param vals
     *            new values
     */
    public void setAttribute(String name, String att, List<Object> vals)
    {
        // existing users
        ILdapUser user = fLdapUsers.get(name);

        if (user != null)
        {
            // check if values match the original values
            Attribute a = user.getAttribute(att);
            boolean match = false;

            if (a != null)
            {
                try
                {
                    match = true;
                    NamingEnumeration<?> oldVals = a.getAll();

                    for (Object val : vals)
                    {
                        if (!oldVals.hasMore() || !oldVals.next().equals(val))
                        {
                            match = false;
                            break;
                        }
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            if (match)
            {
                // no modifications necessary
                removeModification(name, att);
            } else
            {
                // attribute set to new value, other modifications irrelevant
                Attribute attr = new BasicAttribute(att);
                for (Object val : vals)
                {
                    attr.add(val);
                }
                ModificationItem mod = new ModificationItem(
                    DirContext.REPLACE_ATTRIBUTE, attr);
                setModification(name, att, mod);
            }
        }
        // users that haven't been created yet
        else
        {
            user = fNewUsers.get(name);

            if (user != null)
            {
                Attribute attr = new BasicAttribute(att);
                for (Object val : vals)
                {
                    attr.add(val);
                }
                user.addAttribute(attr);
            }
        }
    }

    /**
     * Removes an attribute from an entity. If the attribute does not exist, the
     * call is ignored. Parameters should not be null or empty.
     * 
     * @param name
     *            name of the entity the attribute belongs to
     * @param att
     *            name of the attribute
     */
    public void removeAttribute(String name, String att)
    {
        // existing users
        ILdapUser user = fLdapUsers.get(name);

        if (user != null)
        {
            // attribute removed, other modifications irrelevant
            removeModification(name, att);

            // check whether the attribute was there in the first place
            Attribute orgVal = user.getAttribute(att);
            if (orgVal != null)
            {
                Attribute attr = new BasicAttribute(att);
                ModificationItem mod = new ModificationItem(
                    DirContext.REMOVE_ATTRIBUTE, attr);
                setModification(name, att, mod);
            }
        } else
        {
            // users that haven't been created yet
            user = fNewUsers.get(name);
            if (user != null)
            {
                user.removeAttribute(att);
            }
        }
    }

    /**
     * Creates a new user with the given name and configured initial data. If
     * there already is a user with the given name, a RuntimeException is
     * thrown. Name may not be null or empty.
     * 
     * @param name
     *            UID for the new user
     * @return newly created user
     */
    public ILdapUser createUser(String name)
    {
        // check for existing users
        if (hasUser(name))
        {
            throw new RuntimeException("user \"" + name + "\" already exists");
        }

        ILdapUser user = fUserFactory.createUser(name);
        fNewUsers.put(name, user);
        fAllUsers.put(name, user);

        return user;
    }

    /**
     * Queues the deletion of the user with the given name and removes it from
     * the cached result set. The call is ignored if there is no such user.
     * 
     * @param name
     *            UID of the user to delete
     */
    public void deleteUser(String name)
    {
        // check if user exists and isn't already being deleted
        if (fLdapUsers.containsKey(name) && !fDeletedUsers.contains(name))
        {
            fDeletedUsers.add(name);
            fLdapUsers.remove(name);
        }

        // delete from new users as well?
        fNewUsers.remove(name);

        fAllUsers.remove(name);
    }

    /**
     * @return whether there are changes in the buffer that can be written
     */
    public boolean hasChanges()
    {
        return !fNewUsers.isEmpty() || !fDeletedUsers.isEmpty()
            || !fModifications.isEmpty();
    }

    /**
     * Writes all changes in the buffer to the already connected LDAP directory
     * service. The connection is not closed after writing. Changes are written
     * in this order: user deletions, user creations, attribute updates.
     * 
     * @throws Exception
     *             if an Exception occurs during writing
     */
    public void writeToLdap() throws Exception
    {
        // delete users
        for (String name : fDeletedUsers)
        {
            fLdap.remove(name);
        }
        fDeletedUsers.clear();

        // create new users
        for (Entry<String, ILdapUser> userE : fNewUsers.entrySet())
        {
            fLdap.create(userE.getKey(), userE.getValue());
        }
        fNewUsers.clear();

        // update attributes of existing users
        String name = null;
        List<ModificationItem> modList = null;
        ModificationItem[] modArr = null;
        for (Entry<String, Map<String, ModificationItem>> entry : fModifications
            .entrySet())
        {
            name = entry.getKey();
            modList = new ArrayList<ModificationItem>();

            for (Entry<String, ModificationItem> mod : entry.getValue()
                .entrySet())
            {
                modList.add(mod.getValue());
            }

            modArr = new ModificationItem[modList.size()];
            fLdap.update(name, modList.toArray(modArr));
        }
        fModifications.clear();
    }
}
