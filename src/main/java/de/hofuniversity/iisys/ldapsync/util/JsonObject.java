package de.hofuniversity.iisys.ldapsync.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class simplifying the use of JSON objects by converting themselves from and
 * to JSON as well as providing attribute access. The toString()-method
 * generates a JSON representation of the object.
 * 
 * @author fholzschuher2
 * 
 */
public class JsonObject
{
    private final Map<String, String> fAttributes;
    private final Map<String, List<String>> fListAttributes;
    private final Map<String, JsonObject> fSubObjects;
    private final Map<String, List<JsonObject>> fObjectLists;

    public static int getCloseBracketIndex(final String json, int start)
    {
        final int length = json.length();
        int depth = 0;

        final char opBracket = json.charAt(start);
        char clBracket = ']';
        if (opBracket == '{')
        {
            clBracket = '}';
        }

        int i = start;
        char c = ' ';
        for (; i < length; ++i)
        {
            c = json.charAt(i);

            if (c == opBracket)
            {
                ++depth;
            } else if (c == clBracket)
            {
                --depth;
            }

            if (depth == 0)
            {
                break;
            }
        }

        return i;
    }

    /**
     * Creates a blank JSON object that can be filled with attributes and
     * converted to a JSON String.
     */
    public JsonObject()
    {
        fAttributes = new HashMap<String, String>();
        fListAttributes = new HashMap<String, List<String>>();
        fSubObjects = new HashMap<String, JsonObject>();
        fObjectLists = new HashMap<String, List<JsonObject>>();
    }

    /**
     * Creates a JSON object from a JSON string, setting all attributes and
     * sub-objects.
     * 
     * @param json
     *            JSON string
     */
    public JsonObject(final String json)
    {
        fAttributes = new HashMap<String, String>();
        fListAttributes = new HashMap<String, List<String>>();
        fSubObjects = new HashMap<String, JsonObject>();
        fObjectLists = new HashMap<String, List<JsonObject>>();

        final int length = json.length();
        char c = ' ';
        for (int i = 1; i < length; ++i)
        {
            c = json.charAt(i);

            if (c == '"')
            {
                i = readAttribute(json, i);
            }

            // TODO: lists
        }
    }

    private int readAttribute(final String json, int start)
    {
        int stop = ++start;

        char c = json.charAt(stop);
        while (c != '"')
        {
            ++stop;
            c = json.charAt(stop);
        }

        String name = json.substring(start, stop);

        stop += 2;
        c = json.charAt(stop);

        // singular attributes
        if (c == '"')
        {
            start = ++stop;

            c = json.charAt(stop);
            while (c != '"')
            {
                ++stop;
                c = json.charAt(stop);
            }
            String value = json.substring(start, stop);

            fAttributes.put(name, value);
        }
        // objects
        else if (c == '{')
        {
            start = stop;
            stop = getCloseBracketIndex(json, start) + 1;

            String subString = json.substring(start, stop);
            JsonObject subObject = new JsonObject(subString);

            fSubObjects.put(name, subObject);
        }
        // list attributes
        else if (c == '[')
        {
            start = stop;
            stop = getCloseBracketIndex(json, start) + 1;

            c = json.charAt(start + 1);
            if (c == '"')
            {
                List<String> values = readList(json, start);
                fListAttributes.put(name, values);
            } else
            {
                // TODO: object lists?
            }
        }

        return stop;
    }

    private List<String> readList(final String json, int start)
    {
        final List<String> list = new ArrayList<String>();
        int pos = ++start;

        boolean reading = false;
        char c = json.charAt(pos);
        while (c != ']')
        {
            if (c == '"')
            {
                if (reading)
                {
                    reading = false;
                    list.add(json.substring(start, pos));
                } else
                {
                    reading = true;
                    start = pos + 1;
                }
            }

            ++pos;
            c = json.charAt(pos);
        }

        return list;
    }

    /**
     * Retrieves the single value of a named attribute. Returns null if there is
     * no such attribute or there are multiple values or only objects.
     * 
     * @param name
     *            name of the attribute
     * @return value of the attribute or null
     */
    public String getSingleAttribute(String name)
    {
        String value = null;

        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject != null)
            {
                value = subObject.getSingleAttribute(att);
            }
        } else
        {
            value = fAttributes.get(name);
        }

        return value;
    }

    /**
     * Retrieves all values of the attribute with the given name. Returns null
     * if there is only a singular value or only objects.
     * 
     * @param name
     *            name of the attribute
     * @return list of values of the attribute or null
     */
    public List<String> getListAttribute(String name)
    {
        List<String> values = null;

        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject != null)
            {
                values = subObject.getListAttribute(att);
            }
        } else
        {
            values = fListAttributes.get(name);
        }

        return values;
    }

    /**
     * Retrieves a named subordinate object of this JSON object. Returns null if
     * it does not exist.
     * 
     * @param name
     *            name of the object
     * @return a JSON object or null
     */
    public JsonObject getSubObject(String name)
    {
        JsonObject object = null;

        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String sub = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject != null)
            {
                object = subObject.getSubObject(sub);
            }
        } else
        {
            object = fSubObjects.get(name);
        }

        return object;
    }

    /**
     * Retrieves all values of the object list with the given name. Returns null
     * if there is no such list or the attribute is only listed under singular
     * values or sub objects.
     * 
     * @param name
     *            name of the attribute
     * @return list of values or null
     */
    public List<JsonObject> getObjectList(String name)
    {
        List<JsonObject> list = null;

        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject != null)
            {
                list = subObject.getObjectList(att);
            }
        } else
        {
            list = fObjectLists.get(name);
        }

        return list;
    }

    /**
     * Sets the value of a singular attribute, overwrites any existing values
     * and removes list and object values with the same name.
     * 
     * @param name
     *            name of the attribute to set
     * @param value
     *            value to set the attribute to
     */
    public void setSingleAttribute(String name, String value)
    {
        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject == null)
            {
                subObject = new JsonObject();
                fSubObjects.put(name, subObject);
            }

            subObject.setSingleAttribute(att, value);
        } else
        {
            removeAttribute(name);

            fAttributes.put(name, value);
        }
    }

    /**
     * Sets an attribute to multiple values, overwriting any existing values and
     * removing singular values and object values with the same name.
     * 
     * @param name
     *            name of the attribute to set
     * @param values
     *            list of values to set the attribute to
     */
    public void setListAttribute(String name, List<String> values)
    {
        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject == null)
            {
                subObject = new JsonObject();
                fSubObjects.put(name, subObject);
            }

            subObject.setListAttribute(att, values);
        } else
        {
            removeAttribute(name);

            fListAttributes.put(name, values);
        }
    }

    /**
     * Sets the sub object registered under a certain name, overwrites any
     * existing values and removes list and object values with the same name.
     * 
     * @param name
     *            name of the sub object to set
     * @param object
     *            object to set
     */
    public void setSubObject(String name, JsonObject object)
    {
        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String sub = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject == null)
            {
                subObject = new JsonObject();
                fSubObjects.put(name, subObject);
            }

            subObject.setSubObject(sub, object);
        } else
        {
            removeAttribute(name);

            fSubObjects.put(name, object);
        }
    }

    /**
     * Sets the values of an object list, overwrites any existing singular
     * values and removes list and object values with the same name.
     * 
     * @param name
     *            name of the object list to set
     * @param list
     *            list of values to set
     */
    public void setObjectList(String name, List<JsonObject> list)
    {
        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject == null)
            {
                subObject = new JsonObject();
                fSubObjects.put(name, subObject);
            }

            subObject.setObjectList(att, list);
        } else
        {
            removeAttribute(name);

            fObjectLists.put(name, list);
        }
    }

    /**
     * Removes all attributes and sub objects with the given name. Ignores calls
     * with names of non-existent attributes and objects.
     * 
     * @param name
     *            name of the attribute to remove
     * @return whether an attribute was removed
     */
    public boolean removeAttribute(String name)
    {
        boolean removed = false;

        if (name.contains("."))
        {
            int dot = name.indexOf('.');
            String att = name.substring(dot + 1);
            name = name.substring(0, dot);
            JsonObject subObject = fSubObjects.get(name);

            if (subObject != null)
            {
                removed |= subObject.removeAttribute(att);
            }
        } else
        {
            Object oldVal = null;

            oldVal = fAttributes.remove(name);
            if (oldVal != null)
            {
                removed = true;
            }

            oldVal = fListAttributes.remove(name);
            if (oldVal != null)
            {
                removed = true;
            }

            oldVal = fObjectLists.remove(name);
            if (oldVal != null)
            {
                removed = true;
            }
            oldVal = fSubObjects.remove(name);
            if (oldVal != null)
            {
                removed = true;
            }
        }

        return removed;
    }

    @Override
    public String toString()
    {
        final StringBuffer buffer = new StringBuffer("{");

        // whether there are preceding attributes, so that a comma is needed
        boolean needsComma = false;

        // singular attributes
        final Iterator<Entry<String, String>> atts = fAttributes.entrySet()
            .iterator();
        if (atts.hasNext())
        {
            needsComma = true;
        }

        Entry<String, String> att = null;
        while (atts.hasNext())
        {
            att = atts.next();
            buffer.append("\"" + att.getKey() + "\"");
            buffer.append(":\"" + att.getValue() + "\"");

            if (atts.hasNext())
            {
                buffer.append(',');
            }
        }

        // list attributes
        final Iterator<Entry<String, List<String>>> listAtts = fListAttributes
            .entrySet().iterator();
        if (needsComma && listAtts.hasNext())
        {
            buffer.append(',');
        }
        if (listAtts.hasNext())
        {
            needsComma = true;
        }

        Entry<String, List<String>> attList = null;
        while (listAtts.hasNext())
        {
            attList = listAtts.next();
            buffer.append("\"" + attList.getKey() + "\":[");

            Iterator<String> attIt = attList.getValue().iterator();
            while (attIt.hasNext())
            {
                buffer.append('"' + attIt.next() + '"');

                if (attIt.hasNext())
                {
                    buffer.append(',');
                }
            }

            buffer.append(']');

            if (atts.hasNext())
            {
                buffer.append(',');
            }
        }

        // sub objects
        final Iterator<Entry<String, JsonObject>> objects = fSubObjects
            .entrySet().iterator();

        if (needsComma && objects.hasNext())
        {
            buffer.append(',');
        }
        if (objects.hasNext())
        {
            needsComma = true;
        }

        Entry<String, JsonObject> obj = null;
        while (objects.hasNext())
        {
            obj = objects.next();
            buffer.append("\"" + obj.getKey() + "\"");
            buffer.append(":" + obj.getValue() + "");

            if (objects.hasNext())
            {
                buffer.append(',');
            }
        }

        // object lists
        final Iterator<Entry<String, List<JsonObject>>> lists = fObjectLists
            .entrySet().iterator();

        if (needsComma && lists.hasNext())
        {
            buffer.append(',');
        }
        if (lists.hasNext())
        {
            needsComma = true;
        }

        Entry<String, List<JsonObject>> list = null;
        while (lists.hasNext())
        {
            list = lists.next();
            buffer.append("\"" + list.getKey() + "\":[");

            Iterator<JsonObject> objIt = list.getValue().iterator();
            while (objIt.hasNext())
            {
                buffer.append(objIt.next().toString());

                if (objIt.hasNext())
                {
                    buffer.append(',');
                }
            }

            buffer.append("]");

            if (lists.hasNext())
            {
                buffer.append(',');
            }
        }

        return buffer.append("}").toString();
    }
}
