package de.hofuniversity.iisys.ldapsync.endpoints;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hofuniversity.iisys.ldapsync.LdapBuffer;
import de.hofuniversity.iisys.ldapsync.config.SyncEndpointConfig;
import de.hofuniversity.iisys.ldapsync.model.ILdapUser;
import de.hofuniversity.iisys.ldapsync.util.JsonObject;

/**
 * End point implementation for the Apache Shindig graph back-end.
 * 
 * @author fholzschuher2
 * 
 */
public class ShindigGraphEndpoint extends ASyncEndpoint
{
    private static final String ID_ATT = "id";

    private static final String HOST = "host";
    private static final String USER_ID = "user";
    private static final String FIELDS = "fields";
    
    
    
    private static final String PIC_FOLDER = "pic-folder";
    private static final String PIC_URL = "pic-url";
    
    //special LDAP attributes and shindig properties
    private static final String THUMB_ATTR = "thumbnail";
    private static final String THUMB_PROP = "thumbnailUrl";
    
    private static final String MAIL_PROP = "emails";
    private static final String PHONE_PROP = "phoneNumbers";
    
    private static final String ORG_ATTR = "organization";
    private static final String ORG_PROP = "organizations";
    private static final String ORG_NAME_PROP = "name";
    
    private static final String ORG_LOCATION_ATTR = "org_location";
    private static final String ORG_LOCATION_PROP = "location";
    
    private static final String ORG_SITE_ATTR = "org_site";
    private static final String ORG_SITE_PROP = "site";
    
    private static final String JOB_TITLE_ATTR = "job_title";
    private static final String JOB_TITLE_PROP = "title";
    
    //extended model fields
    private static final String MANAGER_ID_PROP = "managerId";
    private static final String SECRETARY_ID_PROP = "secretaryId";
    private static final String DEPARTMENT_PROP = "department";
    private static final String DEPARTMENT_HEAD_PROP = "departmentHead";
    private static final String ORG_UNIT_PROP = "orgUnit";
    

    private static final String ALL_FRAGMENT = "rpc?method=user.getAll";
    private static final String COUNT_FRAGMENT = "&count=0";
    private static final String FIELDS_FRAGMENT = "&fields=";

    private static final String CREATE_METHOD = "user.create";
    private static final String UPDATE_METHOD = "people.update";
    private static final String DELETE_METHOD = "user.delete";

    private final String fHost, fUserId, fFields;
    private final String fPicFolder, fPicUrl;

    private final Map<String, JsonObject> fUsers;
    private final Set<String> fUserNames, fCreatedUsers, fDeletedUsers;
    private final Set<JsonObject> fChangedUsers;

    /**
     * Creates an end point connecting to the shindig graph back-end specified
     * by the given configuration object. Throws a NullPointerException if any
     * parameter or needed property is null.
     * 
     * @param config
     *            configuration object to use
     * @param buffer
     *            LDAP buffer to use
     */
    public ShindigGraphEndpoint(SyncEndpointConfig config, LdapBuffer buffer)
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

        fHost = config.getProperties().get(HOST);
        if (fHost == null || fHost.isEmpty())
        {
            throw new NullPointerException("host to connect to was null");
        }

        fUserId = config.getProperties().get(USER_ID);
        if (fUserId == null || fUserId.isEmpty())
        {
            throw new NullPointerException("user ID to use was null");
        }

        fFields = config.getProperties().get(FIELDS);
        if (fFields == null || fFields.isEmpty())
        {
            throw new NullPointerException("fields to fetch were null");
        }
        
        fPicFolder = config.getProperties().get(PIC_FOLDER);
        fPicUrl = config.getProperties().get(PIC_URL);

        fUsers = new HashMap<String, JsonObject>();
        fUserNames = new HashSet<String>();
        fCreatedUsers = new HashSet<String>();
        fDeletedUsers = new HashSet<String>();
        fChangedUsers = new HashSet<JsonObject>();
    }

    @Override
    protected void preHook()
    {
        // establish connection, read all users
        String result = "";
        try
        {
            URL shindigUrl = new URL(fHost + ALL_FRAGMENT + COUNT_FRAGMENT
                + FIELDS_FRAGMENT + fFields);
            HttpURLConnection connection = (HttpURLConnection) shindigUrl
                .openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

            String line = reader.readLine();
            while (line != null)
            {
                result += line;
                line = reader.readLine();
            }

            reader.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // build a set of available users
        int start = result.indexOf("list") + 7;
        int stop = start - 2;
        final int lastStop = result.lastIndexOf("}],");
        String objString = null;
        JsonObject person = null;
        String name = null;
        do
        {
            stop = JsonObject.getCloseBracketIndex(result, start);

            if (stop + 1 < result.length())
            {
                objString = result.substring(start, stop + 1);

                person = new JsonObject(objString);
                name = person.getSingleAttribute(ID_ATT);
                fUserNames.add(name);
                fUsers.put(name, person);
            }

            start = stop + 2;
        } while (stop < lastStop);
    }

    @Override
    protected void postHook()
    {
        if (!fChangedUsers.isEmpty() || !fDeletedUsers.isEmpty())
        {
            writeChanges();
        }

        // clear
        fUsers.clear();
        fUserNames.clear();
        fChangedUsers.clear();
        fCreatedUsers.clear();
        fDeletedUsers.clear();
    }

    private void writeChanges()
    {
        String method = null;
        final StringBuffer buffer = new StringBuffer("[");

        // collect changes into JSON RPC call batch
        String id = null;
        String jsonPerson = null;
        
        //create new users with only IDs (so that links can be created)
        for(String userId : fCreatedUsers)
        {
            JsonObject user = new JsonObject();
            user.setSingleAttribute("id", userId);
            method = CREATE_METHOD;
            jsonPerson = user.toString();
            
            buffer.append("{\"method\":\"" + method + "\",\"id\":\"" + userId);
            buffer.append("\",\"params\":{\"userId\":\"" + userId + "\",");
            buffer.append("\"person\":" + jsonPerson + "}},");
        }
        
        //update changed users (including new)
        for (JsonObject user : fChangedUsers)
        {
            id = user.getSingleAttribute(ID_ATT);
            jsonPerson = user.toString();

            method = UPDATE_METHOD;

            buffer.append("{\"method\":\"" + method + "\",\"id\":\"" + id);
            buffer.append("\",\"params\":{\"userId\":\"" + id + "\",");
            buffer.append("\"person\":" + jsonPerson + "}},");
        }

        // queue deletion requests
        for (String name : fDeletedUsers)
        {
            buffer.append("{\"method\":\"" + DELETE_METHOD + "\",\"id\":\""
                + name);
            buffer.append("\",\"params\":{\"userId\":\"" + name + "\"}},");
        }

        buffer.setCharAt(buffer.length() - 1, ']');

        // open connection and send batch
        try
        {
            URL shindigUrl = new URL(fHost + "rpc");
            HttpURLConnection connection = (HttpURLConnection) shindigUrl
                .openConnection();

            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length",
                String.valueOf(buffer.length()));

            OutputStreamWriter writer = new OutputStreamWriter(
                connection.getOutputStream());
            writer.write(buffer.toString());
            writer.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

            String line = reader.readLine();
            while (line != null)
            {
                line = reader.readLine();
            }

            reader.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected List<Object> getValues(String name, String att)
    {
        List<Object> values = null;

        JsonObject user = fUsers.get(name);
        if (user != null)
        {
            String value = user.getSingleAttribute(att);

            if (value != null)
            {
                values = new ArrayList<Object>();
                values.add(value);
            } else
            {
                List<String> valList = user.getListAttribute(att);

                if (valList != null)
                {
                    values = new ArrayList<Object>(valList);
                }
            }
        }

        return values;
    }

    @Override
    protected void setAttribute(String name, String att, Object val)
    {
        JsonObject user = fUsers.get(name);
        if (user != null)
        {
            //handle special attributes
            if(att.equals(THUMB_ATTR))
            {
                byte[] picData = (byte[]) val;
                String url = storePicture(name, picData);
                
                user.setSingleAttribute(THUMB_PROP, url);
                fChangedUsers.add(user);
            }
            else if(att.equals(MAIL_PROP))
            {
                //create fake list if list does not contain address
                List<JsonObject> mails = user.getObjectList(MAIL_PROP);
                
                if(mails == null)
                {
                    mails = new ArrayList<JsonObject>();
                }
                
                if(mails.isEmpty())
                {
                    JsonObject mail = new JsonObject();
                    mail.setSingleAttribute("value", val.toString());
                    mails.add(mail);
                    
                    user.setObjectList(MAIL_PROP, mails);
                }
            }
            else if(att.equals(PHONE_PROP))
            {
                //create fake list if list does not contain number
                List<JsonObject> phones = user.getObjectList(PHONE_PROP);
                
                if(phones == null)
                {
                    phones = new ArrayList<JsonObject>();
                }
                
                if(phones.isEmpty())
                {
                    JsonObject phone = new JsonObject();
                    phone.setSingleAttribute("value", val.toString());
                    phones.add(phone);
                    
                    user.setObjectList(PHONE_PROP, phones);
                }
            }
            //TODO: refactor to hashset-based check + set-based name lookup
            else if(att.equals(ORG_ATTR))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(ORG_NAME_PROP, val.toString());
            }
            else if(att.equals(ORG_LOCATION_ATTR))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(ORG_LOCATION_PROP, val.toString());
            }
            else if(att.equals(ORG_SITE_ATTR))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(ORG_SITE_PROP, val.toString());
            }
            else if(att.equals(JOB_TITLE_ATTR))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(JOB_TITLE_PROP, val.toString());
            }
            else if(att.equals(MANAGER_ID_PROP))
            {
                //extract uid from the ldap path given
                //TODO: only works for our case
                String managerId = val.toString();
                if(managerId.indexOf(',') > 0)
                {
                    //get only uid property
                    managerId = managerId.substring(0, managerId.indexOf(','));
                }
                if(managerId.indexOf('=') > 0)
                {
                    //get only uid value
                    managerId = managerId.substring(
                        managerId.indexOf('=') + 1, managerId.length());
                }
                
                if(!managerId.isEmpty()
                    && !managerId.equals("-"))
                {
                    JsonObject org = getPrimaryOrganization(user);
                    org.setSingleAttribute(MANAGER_ID_PROP, managerId);
                }
            }
            else if(att.equals(SECRETARY_ID_PROP))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(SECRETARY_ID_PROP, val.toString());
            }
            else if(att.equals(DEPARTMENT_PROP))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(DEPARTMENT_PROP, val.toString());
            }
            else if(att.equals(DEPARTMENT_HEAD_PROP))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(DEPARTMENT_HEAD_PROP, val.toString());
            }
            else if(att.equals(ORG_UNIT_PROP))
            {
                JsonObject org = getPrimaryOrganization(user);
                org.setSingleAttribute(ORG_UNIT_PROP, val.toString());
            }
            //handle simple properties
            else
            {
                String valString = val.toString();
                String oldVal = user.getSingleAttribute(att);

                if (!valString.equals(oldVal))
                {
                    user.setSingleAttribute(att, valString);
                    fChangedUsers.add(user);
                }
            }
        }
    }
    
    private String storePicture(String name, byte[] data)
    {
        File file = new File(fPicFolder + name + ".png");
        String url = null;
        
        try
        {
            BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(file));
            
            bos.write(data);
            bos.flush();
            bos.close();
            
            url = fPicUrl + name + ".png";
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return url;
    }
    
    private JsonObject getPrimaryOrganization(JsonObject user)
    {
        JsonObject org = null;
        
        //get first organization from list
        List<JsonObject> organizations = user.getObjectList(ORG_PROP);
        
        if(organizations == null)
        {
            organizations = new ArrayList<JsonObject>();
            user.setObjectList(ORG_PROP, organizations);
        }
        
        if(organizations.isEmpty())
        {
            org = new JsonObject();
            organizations.add(org);
        }
        else
        {
            org = organizations.get(0);
        }
        
        return org;
    }

    @Override
    protected void setAttribute(String name, String att, List<Object> vals)
    {
        JsonObject user = fUsers.get(name);
        if (user != null)
        {
            List<String> oldVals = user.getListAttribute(att);

            boolean replace = true;
            if (oldVals != null)
            {
                // check for equality
                if (vals.size() == oldVals.size())
                {
                    replace = false;

                }
            }

            if (replace)
            {
                List<String> newVals = new ArrayList<String>();
                for (Object o : vals)
                {
                    newVals.add(o.toString());
                }

                user.setListAttribute(att, newVals);
                fChangedUsers.add(user);
            }
        }
    }

    @Override
    protected void addValues(String name, String att, List<Object> vals)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void removeAttribute(String name, String att)
    {
        JsonObject user = fUsers.get(name);
        if (user != null)
        {
            boolean changed = user.removeAttribute(att);

            if (changed)
            {
                fChangedUsers.add(user);
            }
        }
    }

    @Override
    protected Set<String> getUserNames()
    {
        return new HashSet<String>(fUserNames);
    }

    @Override
    protected void createUser(ILdapUser user)
    {
        String id = user.getUid();

        fCreatedUsers.add(id);
        fUserNames.add(id);

        // TODO: provide via COPY_ON_CREATE
        JsonObject jsonUser = new JsonObject();
        jsonUser.setSingleAttribute(ID_ATT, id);

        jsonUser.setSingleAttribute("displayName", user
            .getAttributeValues("cn").get(0).toString());
        jsonUser.setSingleAttribute("name.formatted",
            user.getAttributeValues("cn").get(0).toString());
        jsonUser.setSingleAttribute("name.givenName",
            user.getAttributeValues("givenName").get(0).toString());
        jsonUser.setSingleAttribute("name.familyName",
            user.getAttributeValues("sn").get(0).toString());

        fUsers.put(id, jsonUser);
        fChangedUsers.add(jsonUser);
    }

    @Override
    protected void deleteUser(String name)
    {
        JsonObject user = fUsers.remove(name);
        if (user != null)
        {
            fDeletedUsers.add(name);
            fChangedUsers.remove(user);
            fCreatedUsers.remove(name);
        }
    }
}
