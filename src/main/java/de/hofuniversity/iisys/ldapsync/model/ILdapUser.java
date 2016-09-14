package de.hofuniversity.iisys.ldapsync.model;

import java.util.List;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

/**
 * User class containing information to store in an LDAP directory service. Only
 * users marked as new via the isNew() method may be manipulated directly with
 * most implementations. Consequently, all user modifications should be done
 * through the buffer's methods to keep modifications consistent.
 * 
 * @author fholzschuher2
 * 
 */
public interface ILdapUser extends DirContext
{
    /**
     * Adds an arbitrary attribute to the person. Parameter must not be null.
     * 
     * @param attribute
     *            attribute to add
     */
    public void addAttribute(Attribute attribute);

    /**
     * Adds or replaces a named attribute. Parameters must not be null.
     * 
     * @param name
     *            id or name for the attribute
     * @param value
     *            value of the attribute
     */
    public void setAttribute(String name, Object value);

    /**
     * Removes a named attribute.
     * 
     * @param name
     *            id or name of the attribute
     */
    public void removeAttribute(String name);

    /**
     * @return user's LDAP UID
     */
    public String getUid();

    /**
     * Returns all values of a certain attribute, as LDAP attributes can have
     * multiple values the result is a list of objects.
     * 
     * @param name
     *            name of the attribute to get
     * @return list of values of the attribute
     */
    public List<Object> getAttributeValues(String name);

    /**
     * @return all of the user's attributes
     */
    public Attributes getAttributes();

    /**
     * Returns a set of attributes, whose IDs are defined by the given array.
     * Attributes that aren't found are not contained in the result. Array of
     * IDs may not be null.
     * 
     * @param attrIds
     *            array of attribute IDs
     * @return collection of requested attributes
     */
    public Attributes getAttributes(String[] attrIds);

    /**
     * @param attr
     *            name of the attribute
     * @return attribute or null if it doesn't exist
     */
    public Attribute getAttribute(String attr);

    /**
     * @return whether this is a newly created or existing user
     */
    public boolean isNew();
}
