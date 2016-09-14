package de.hofuniversity.iisys.ldapsync.model;

import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Abstract class giving stub implementation to all unneeded methods of the
 * DirContext interface and redirecting the rest to ILdapUser methods which
 * still have to be implemented.
 * 
 * @author fholzschuher2
 * 
 */
public abstract class ALdapUser implements ILdapUser
{
    // own methods

    public abstract void addAttribute(Attribute attribute);

    public abstract void setAttribute(String name, Object value);

    public abstract void removeAttribute(String name);

    public abstract String getUid();

    public abstract List<Object> getAttributeValues(String name);

    public abstract Attributes getAttributes();

    public abstract Attributes getAttributes(String[] attrIds);

    public abstract Attribute getAttribute(String attr);

    public abstract boolean isNew();

    // needed methods

    public Attributes getAttributes(Name name) throws NamingException
    {
        return getAttributes(name.toString());
    }

    public Attributes getAttributes(String name) throws NamingException
    {
        if (!name.equals(""))
        {
            throw new NameNotFoundException("this is just a user entity");
        }

        return getAttributes();
    }

    public Attributes getAttributes(Name name, String[] attrIds)
        throws NamingException
    {
        return getAttributes(name.toString(), attrIds);
    }

    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException
    {
        if (!name.equals(""))
        {
            throw new NameNotFoundException("this is just a user entity");
        }

        return getAttributes(attrIds);
    }

    // unneeded methods

    public Object addToEnvironment(String propName, Object propVal)
        throws NamingException
    {
        // not needed
        return null;
    }

    public void bind(Name name, Object obj) throws NamingException
    {
        // not needed
    }

    public void bind(String name, Object obj) throws NamingException
    {
        // not needed
    }

    public void close() throws NamingException
    {
        // not needed
    }

    public Name composeName(Name name, Name prefix) throws NamingException
    {
        // not needed
        return null;
    }

    public String composeName(String name, String prefix)
        throws NamingException
    {
        // not needed
        return null;
    }

    public Context createSubcontext(Name name) throws NamingException
    {
        // not needed
        return null;
    }

    public Context createSubcontext(String name) throws NamingException
    {
        // not needed
        return null;
    }

    public void destroySubcontext(Name name) throws NamingException
    {
        // not needed
    }

    public void destroySubcontext(String name) throws NamingException
    {
        // not needed
    }

    public Hashtable<?, ?> getEnvironment() throws NamingException
    {
        // not needed
        return null;
    }

    public String getNameInNamespace() throws NamingException
    {
        // not needed
        return null;
    }

    public NameParser getNameParser(Name name) throws NamingException
    {
        // not needed
        return null;
    }

    public NameParser getNameParser(String name) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<NameClassPair> list(Name name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<NameClassPair> list(String name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<Binding> listBindings(Name name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public Object lookup(Name name) throws NamingException
    {
        // not needed
        return null;
    }

    public Object lookup(String name) throws NamingException
    {
        // not needed
        return null;
    }

    public Object lookupLink(Name name) throws NamingException
    {
        // not needed
        return null;
    }

    public Object lookupLink(String name) throws NamingException
    {
        // not needed
        return null;
    }

    public void rebind(Name name, Object obj) throws NamingException
    {
        // not needed
    }

    public void rebind(String name, Object obj) throws NamingException
    {
        // not needed
    }

    public Object removeFromEnvironment(String propName) throws NamingException
    {
        // not needed
        return null;
    }

    public void rename(Name oldName, Name newName) throws NamingException
    {
        // not needed
    }

    public void rename(String oldName, String newName) throws NamingException
    {
        // not needed
    }

    public void unbind(Name name) throws NamingException
    {
        // not needed
    }

    public void unbind(String name) throws NamingException
    {
        // not needed
    }

    public void bind(Name name, Object obj, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public void bind(String name, Object obj, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public DirContext createSubcontext(Name name, Attributes attrs)
        throws NamingException
    {
        // not needed
        return null;
    }

    public DirContext createSubcontext(String name, Attributes attrs)
        throws NamingException
    {
        // not needed
        return null;
    }

    public DirContext getSchema(Name name) throws NamingException
    {
        // not needed
        return null;
    }

    public DirContext getSchema(String name) throws NamingException
    {
        // not needed
        return null;
    }

    public DirContext getSchemaClassDefinition(Name name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public DirContext getSchemaClassDefinition(String name)
        throws NamingException
    {
        // not needed
        return null;
    }

    public void modifyAttributes(Name name, ModificationItem[] mods)
        throws NamingException
    {
        // not needed

    }

    public void modifyAttributes(String name, ModificationItem[] mods)
        throws NamingException
    {
        // not needed
    }

    public void modifyAttributes(Name name, int mod_op, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public void modifyAttributes(String name, int mod_op, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public void rebind(Name name, Object obj, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public void rebind(String name, Object obj, Attributes attrs)
        throws NamingException
    {
        // not needed
    }

    public NamingEnumeration<SearchResult> search(Name name,
        Attributes matchingAttributes) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(String name,
        Attributes matchingAttributes) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name,
        Attributes matchingAttributes, String[] attributesToReturn)
        throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(String name,
        Attributes matchingAttributes, String[] attributesToReturn)
        throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name, String filter,
        SearchControls cons) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(String name, String filter,
        SearchControls cons) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(Name name, String filterExpr,
        Object[] filterArgs, SearchControls cons) throws NamingException
    {
        // not needed
        return null;
    }

    public NamingEnumeration<SearchResult> search(String name,
        String filterExpr, Object[] filterArgs, SearchControls cons)
        throws NamingException
    {
        // not needed
        return null;
    }
}
