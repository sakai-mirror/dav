package org.sakaiproject.dav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.StringUtil;


public class SakaiResourceFactoryImpl implements ResourceFactory{
	//Content hosting service
	private ContentHostingService contentHostingService;
	//Logger
	private static Log M_log = LogFactory.getLog(SakaiResourceFactoryImpl.class);
	public Resource getResource(String host, String path) {
		contentHostingService = (ContentHostingService) ComponentManager.get(ContentHostingService.class.getName());
		Entity mbr = null;
		try {
			mbr = contentHostingService.getResource(adjustId(path));
		} catch (PermissionException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			M_log.debug("ResourceInfoSAKAI - You do not have permission to view this resource " + path);
		} catch (IdUnusedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			M_log.debug("ResourceInfoSAKAI - This resource does not exist " + path);
		} catch (TypeException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			M_log.warn("ResourceInfoSAKAI - Type Exception " + path);
		}
		// TODO Auto-generated method stub
		return (Resource)mbr;
	}
	protected String adjustId(String id)
	{
		// Note: code stolen and to be kept synced wtih BaseContentService.parseEntityReference() -ggolden

		// map unknown prefix to, if "~", /user/, else /group/
		if (contentHostingService.isShortRefs())
		{
			// ignoring the first separator, get the first item separated from the rest
			String prefix[] = StringUtil.splitFirst((id.length() > 1) ? id.substring(1) : "", Entity.SEPARATOR);
			if (prefix.length > 0)
			{
				// the following are recognized as full reference prefixe; if seen, the sort ref feature is not applied
				if (!(prefix[0].equals("group") || prefix[0].equals("user") || prefix[0].equals("group-user")
						|| prefix[0].equals("public") || prefix[0].equals("private") || prefix[0].equals("attachment")))
				{
					String newPrefix = null;
	
					// a "~" starts a /user/ reference
					if (prefix[0].startsWith("~"))
					{
						newPrefix = Entity.SEPARATOR + "user" + Entity.SEPARATOR + prefix[0].substring(1);
					}
	
					// otherwise a /group/ reference
					else
					{
						newPrefix = Entity.SEPARATOR + "group" + Entity.SEPARATOR + prefix[0];
					}
	
					// reattach the tail (if any) to get the new id (if no taik, make sure we end with a separator if id started out with one)
					id = newPrefix
							+ ((prefix.length > 1) ? (Entity.SEPARATOR + prefix[1])
									: (id.endsWith(Entity.SEPARATOR) ? Entity.SEPARATOR : ""));
				}
			}
		}

		// TODO: alias for site

		// recognize /user/EID and makeit /user/ID
		String parts[] = StringUtil.split(id, Entity.SEPARATOR);
		if (parts.length >= 3)
		{
			if (parts[1].equals("user"))
			{
				try
				{
					// if successful, the context is already a valid user id
					UserDirectoryService.getUser(parts[2]);
				}
				catch (UserNotDefinedException tryEid)
				{
					try
					{
						// try using it as an EID
						String userId = UserDirectoryService.getUserId(parts[2]);
						
						// switch to the ID
						parts[2] = userId;
						String newId = StringUtil.unsplit(parts, Entity.SEPARATOR);

						// add the trailing separator if needed
						if (id.endsWith(Entity.SEPARATOR)) newId += Entity.SEPARATOR;

						id = newId;
					}
					catch (UserNotDefinedException notEid)
					{
						// if context was not a valid EID, leave it alone
					}
				}
			}
		}
		// recognize /group-user/SITE_ID/USER_EID and make it /group-user/SITE_ID/USER_ID 
		if (parts.length >= 4)
		{
			if (parts[1].equals("group-user"))
			{
				try
				{
					// if successful, the context is already a valid user id
					UserDirectoryService.getUser(parts[3]);
				}
				catch (UserNotDefinedException tryEid)
				{
					try
					{
						// try using it as an EID
						String userId = UserDirectoryService.getUserId(parts[3]);

						// switch to the ID
						parts[3] = userId;
						String newId = StringUtil.unsplit(parts, Entity.SEPARATOR);

						// add the trailing separator if needed
						if (id.endsWith(Entity.SEPARATOR)) newId += Entity.SEPARATOR;

						id = newId;
					}
					catch (UserNotDefinedException notEid)
					{
						// if context was not a valid EID, leave it alone
					}
				}
			}
		}

		return id;
	}
	

}
