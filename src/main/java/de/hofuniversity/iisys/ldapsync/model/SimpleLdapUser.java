package de.hofuniversity.iisys.ldapsync.model;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

/**
 * User class containing information to store in an LDAP directory service. Only
 * users marked as new via the isNew() method can be manipulated directly.
 * Consequently, all user modifications should be done through the buffer's
 * methods to keep modifications consistent.
 * 
 * @author fholzschuher2
 * 
 */
public class SimpleLdapUser extends ALdapUser
{
    private final String fUid;
    private final Attributes fAttributes;

    private final boolean fNew;

    /**
     * Creates a new user which can be automatically stored in an LDAP directory
     * service. Initially, UID, OCs and OUs are set, further attributes can be
     * added using the setAttribute()-method. Throws a NullPointerException if
     * the given UID is null.
     * 
     * @param uid
     *            LDAP UID for the user
     * @param ocs
     *            initial object classes for the user
     * @param ous
     *            initial organizational units for the user
     */
    public SimpleLdapUser(String uid, List<String> ocs, List<String> ous)
    {
        if (uid == null || uid.isEmpty())
        {
            throw new NullPointerException("no uid given");
        }

        fUid = uid;
        fAttributes = new BasicAttributes();

        // collect attributes
        fAttributes.put("uid", fUid);

        // object classes
        if (ocs != null)
        {
            Attribute ocSet = new BasicAttribute("objectclass");
            for (String oc : ocs)
            {
                ocSet.add(oc);
            }
            fAttributes.put(ocSet);
        }

        // organizational units
        if (ous != null)
        {
            Attribute ouSet = new BasicAttribute("ou");
            for (String ou : ous)
            {
                ouSet.add(ou);
            }
            fAttributes.put(ouSet);
        }

        fNew = true;
    }

    /**
     * Creates a new user from LDAP query result data. Such instances should not
     * be manipulated directly but via the buffer.
     * 
     * @param uid
     *            LDAP UID for the user
     * @param attributes
     *            collection of attributes
     */
    public SimpleLdapUser(String uid, Attributes attributes)
    {
        if (uid == null || uid.isEmpty())
        {
            throw new NullPointerException("no uid given");
        }
        if (attributes == null)
        {
            throw new NullPointerException("attributes were null");
        }

        fUid = uid;
        fAttributes = attributes;

        fNew = false;
    }

    // own methods

    public void addAttribute(Attribute attribute)
    {
        fAttributes.put(attribute);
    }

    public void setAttribute(String name, Object value)
    {
        fAttributes.put(name, value);
    }

    public void removeAttribute(String name)
    {
        fAttributes.remove(name);
    }

    public String getUid()
    {
        return fUid;
    }

    public List<Object> getAttributeValues(String name)
    {
        List<Object> list = null;
        Attribute att = fAttributes.get(name);

        try
        {
            if (att != null)
            {
                list = new ArrayList<Object>();

                NamingEnumeration<?> elements = att.getAll();

                while (elements.hasMore())
                {
                    list.add(elements.next());
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return list;
    }

    public Attributes getAttributes()
    {
        return fAttributes;
    }

    public Attributes getAttributes(String[] attrIds)
    {
        Attributes atts = new BasicAttributes();
        Attribute a = null;

        for (String id : attrIds)
        {
            a = fAttributes.get(id);

            if (a != null)
            {
                atts.put(a);
            }
        }

        return atts;
    }

    public Attribute getAttribute(String attr)
    {
        return fAttributes.get(attr);
    }

    public boolean isNew()
    {
        return fNew;
    }
}
