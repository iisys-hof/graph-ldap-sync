package de.hofuniversity.iisys.ldapsync.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

/**
 * Reader that reads configuration properties from an XML file.
 * 
 * @author fholzschuher2
 * 
 */
public class XMLConfigReader
{
    private static final String CONFIG_ROOT = "ldap_config";
    private static final String INIT_OCS = "object_classes";
    private static final String INIT_OUS = "org_units";
    private static final String ENDPOINT_CONF = "endpoint";

    private static final String CLASS = "class";
    private static final String UNIT = "unit";

    private static final String URL = "url";
    private static final String CONTEXT = "context";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String READ_ONLY = "read_only";
    private static final String SUBTREE_SEARCH = "subtree_search";

    private static final String START_SYNC = "sync_on_start";
    private static final String INTERVAL = "interval";
    private static final String TIME = "time";
    private static final String DAY = "day";

    private static final String TYPE = "type";
    private static final String CREATE_OWN = "create_own_entries";
    private static final String DELETE_OWN = "delete_own_entries";
    private static final String CREATE_LDAP = "create_ldap_entries";
    private static final String DELETE_LDAP = "delete_ldap_entries";
    private static final String ENDPOINT_PROPS = "properties";
    private static final String MAPPING = "mapping";

    private static final String RULE = "rule";
    private static final String LDAP_PROP = "ldap_property";
    private static final String END_POINT_PROP = "end_point_property";
    private static final String DIRECTION = "direction";
    private static final String OPERATION = "operation";

    private final String fPath;

    private SyncConfig fConfig;

    /**
     * Creates an XML configuration reader that reads the file at the location
     * specified.
     * 
     * @param path
     *            path to XML file
     */
    public XMLConfigReader(String path)
    {
        if (path == null || path.isEmpty())
        {
            throw new NullPointerException("no path given");
        }

        fPath = path;
    }

    /**
     * Reads the configuration as configured.
     * 
     * @return configuration object
     * @throws Exception
     *             if reading fails
     */
    public SyncConfig readConfig() throws Exception
    {
        fConfig = new SyncConfig();
        fConfig.setEndpoints(new ArrayList<SyncEndpointConfig>());

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        InputStream in = new FileInputStream(fPath);
        final XMLEventReader eReader = inputFactory.createXMLEventReader(in);

        XMLEvent event = null;
        String tag = null;
        String value = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                if (tag.equals(CONFIG_ROOT))
                {
                    continue;
                } else if (tag.equals(ENDPOINT_CONF))
                {
                    readEndpointConf(eReader);
                } else if (tag.equals(INIT_OCS))
                {
                    readObjectClasses(eReader);
                } else if (tag.equals(INIT_OUS))
                {
                    readOrgUnits(eReader);
                } else
                {
                    event = eReader.nextEvent();
                    value = event.asCharacters().toString();

                    setConfigProperty(tag, value);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(CONFIG_ROOT))
                {
                    break;
                }
            }
        }

        return fConfig;
    }

    private void setConfigProperty(String name, String value)
    {
        if (name.equals(URL))
        {
            fConfig.setUrl(value);
        } else if (name.equals(CONTEXT))
        {
            fConfig.setContext(value);
        } else if (name.equals(USER))
        {
            fConfig.setUser(value);
        } else if (name.equals(PASSWORD))
        {
            fConfig.setPassword(value.toCharArray());
        } else if (name.equals(READ_ONLY))
        {
            boolean ro = Boolean.parseBoolean(value);
            fConfig.setReadOnly(ro);
        } else if (name.equals(SUBTREE_SEARCH))
        {
            boolean sts = Boolean.parseBoolean(value);
            fConfig.setSubtreeSearch(sts);
        }  else if (name.equals(START_SYNC))
        {
            boolean sync = Boolean.parseBoolean(value);
            fConfig.setSyncOnStart(sync);
        } else if (name.equals(INTERVAL))
        {
            CycleTypes cycle = CycleTypes.valueOf(value);
            fConfig.setInterval(cycle);
        } else if (name.equals(TIME))
        {
            fConfig.setTime(value);
        } else if (name.equals(DAY))
        {
            fConfig.setDay(Integer.parseInt(value));
        } else
        {
            System.out.println("unknown main config property: " + name);
        }
    }

    private void readObjectClasses(final XMLEventReader eReader)
        throws Exception
    {
        List<String> classes = new ArrayList<String>();

        XMLEvent event = null;
        String tag = null;
        String value = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                if (tag.equals(CLASS))
                {
                    event = eReader.nextEvent();
                    value = event.asCharacters().toString();
                    classes.add(value);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(INIT_OCS))
                {
                    break;
                }
            }
        }

        fConfig.setInitialClasses(classes);
    }

    private void readOrgUnits(final XMLEventReader eReader) throws Exception
    {
        List<String> orgUnits = new ArrayList<String>();

        XMLEvent event = null;
        String tag = null;
        String value = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                if (tag.equals(UNIT))
                {
                    event = eReader.nextEvent();
                    value = event.asCharacters().toString();
                    orgUnits.add(value);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(INIT_OUS))
                {
                    break;
                }
            }
        }

        fConfig.setInitialOus(orgUnits);
    }

    private void readEndpointConf(final XMLEventReader eReader)
        throws Exception
    {
        SyncEndpointConfig config = new SyncEndpointConfig();

        XMLEvent event = null;
        String tag = null;
        String value = null;

        Map<String, String> props = null;
        List<SyncRule> rules = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                if (tag.equals(ENDPOINT_PROPS))
                {
                    props = readEndpointProperties(eReader);
                    config.setProperties(props);
                } else if (tag.equals(MAPPING))
                {
                    rules = readEndpointRules(eReader);
                    config.setMapping(rules);
                } else
                {
                    event = eReader.nextEvent();
                    value = event.asCharacters().toString();

                    setEndPointProperty(config, tag, value);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(ENDPOINT_CONF))
                {
                    break;
                }
            }
        }

        fConfig.getEndpoints().add(config);
    }

    private Map<String, String> readEndpointProperties(
        final XMLEventReader eReader) throws Exception
    {
        Map<String, String> properties = new HashMap<String, String>();

        XMLEvent event = null;
        String tag = null;
        String value = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                event = eReader.nextEvent();
                value = event.asCharacters().toString();

                properties.put(tag, value);
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(ENDPOINT_PROPS))
                {
                    break;
                }
            }
        }

        return properties;
    }

    private List<SyncRule> readEndpointRules(final XMLEventReader eReader)
        throws Exception
    {
        List<SyncRule> rules = new ArrayList<SyncRule>();

        XMLEvent event = null;
        String tag = null;
        SyncRule rule = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                if (tag.equals(RULE))
                {
                    rule = readRule(eReader);
                    rules.add(rule);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(MAPPING))
                {
                    break;
                }
            }
        }

        return rules;
    }

    private SyncRule readRule(final XMLEventReader eReader) throws Exception
    {
        SyncRule rule = new SyncRule();

        XMLEvent event = null;
        String tag = null;
        String value = null;

        while (eReader.hasNext())
        {
            event = eReader.nextEvent();

            if (event.isStartElement())
            {
                tag = event.asStartElement().getName().getLocalPart();

                event = eReader.nextEvent();
                value = event.asCharacters().toString();

                if (tag.equals(LDAP_PROP))
                {
                    rule.setLdapProp(value);
                } else if (tag.equals(END_POINT_PROP))
                {
                    rule.setEndPointProp(value);
                } else if (tag.equals(DIRECTION))
                {
                    rule.setDirection(SyncDirections.valueOf(value));
                } else if (tag.equals(OPERATION))
                {
                    rule.setOperation(SyncOperations.valueOf(value));
                } else
                {
                    System.out.println("unknown rule property: " + tag);
                }
            }
            if (event.isEndElement())
            {
                tag = event.asEndElement().getName().getLocalPart();

                if (tag.equals(RULE))
                {
                    break;
                }
            }
        }

        return rule;
    }

    private void setEndPointProperty(SyncEndpointConfig config, String name,
        String value)
    {
        if (name.equals(TYPE))
        {
            config.setType(value);
        } else if (name.equals(CREATE_OWN))
        {
            boolean create = Boolean.parseBoolean(value);
            config.setCreateOwnEntries(create);
        } else if (name.equals(CREATE_LDAP))
        {
            boolean create = Boolean.parseBoolean(value);
            config.setCreateLdapEntries(create);
        } else if (name.equals(DELETE_OWN))
        {
            boolean delete = Boolean.parseBoolean(value);
            config.setDeleteOwnEntries(delete);
        } else if (name.equals(DELETE_LDAP))
        {
            boolean delete = Boolean.parseBoolean(value);
            config.setDeleteLdapEntries(delete);
        } else
        {
            System.out.println("unknown end point property: " + name);
        }
    }

    public static void main(String[] args)
    {
        XMLConfigReader reader = new XMLConfigReader("ldapConfig.xml");

        try
        {
            SyncConfig config = reader.readConfig();
            System.out.println("url: " + config.getUrl());
            System.out.println("context: " + config.getContext());
            System.out.println("read-only: " + config.getReadOnly());
            System.out.println("sync on start: " + config.getSyncOnStart());
            System.out.println("cycle: " + config.getInterval());

            for (String clazz : config.getInitialClasses())
            {
                System.out.println("initial class: " + clazz);
            }

            for (String unit : config.getInitialOus())
            {
                System.out.println("initial ou: " + unit);
            }

            for (SyncEndpointConfig sec : config.getEndpoints())
            {
                System.out.println(sec.getType());

                for (Entry<String, String> propE : sec.getProperties()
                    .entrySet())
                {
                    System.out.println("property: " + propE.getKey() + ": "
                        + propE.getValue());
                }

                for (SyncRule rule : sec.getMapping())
                {
                    System.out.println("rule:");
                    System.out.println("ldap: " + rule.getLdapProp());
                    System.out.println("own: " + rule.getEndPointProp());
                    System.out.println("direction: " + rule.getDirection());
                    System.out.println("operation: " + rule.getOperation());
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
