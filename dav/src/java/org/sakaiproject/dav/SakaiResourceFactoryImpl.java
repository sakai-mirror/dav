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
//import org.sakaiproject.exception.pathUnusedException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.StringUtil;


public class SakaiResourceFactoryImpl implements ResourceFactory{
	private ContentHostingService contentHostingService;//content hosting service variable
	private ContentCollection contentCollection;
	private SakaiDavHelper sakaiDavHelper;//object containing all the helper methods this is for eliminating code reuse
	private ContentResource contentResource;
	public SakaiResourceFactoryImpl(){
		super();
		sakaiDavHelper=new SakaiDavHelper();
	}
	/**
	 * Logger
	 */
	private static Log M_log = LogFactory.getLog(SakaiResourceFactoryImpl.class);
	
	/**
	 * getResource returns an instance of either a fileResource or a FOlderresource or null based on the path 
	 * in the URL .I think authentication is not handled here it is handled in the file and folder resource implementations respectively.
	 */
	public Resource getResource(String host, String path) {
		if(sakaiDavHelper.prohibited(path)){
			return null;
		}
		boolean isCollection;
		try
		{
			ResourceProperties props;
			if (path.startsWith("/attachments"))
			{
				M_log.info("DirContextSAKAI.lookup - You do not have permission to view this area " + path);
			}
			props = contentHostingService.getProperties(sakaiDavHelper.adjustId(path));
			isCollection = props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION);
			//Check if the path represents a collection if so return an instance of FolderResource 
			//if not a collection return an instance of a FileResource
			if (isCollection)
			{
				contentCollection=contentHostingService.getCollection(path);
				if (contentCollection==null)
					return null;
				return new SakaiFolderResource(contentCollection,path);
			}
			else{
				if (contentResource==null)
					return null;
				contentResource=contentHostingService.getResource(path);
				return new SakaiFileResource(contentResource,path);
			}
		}
		catch (PermissionException e)
		{
			M_log.debug("DirContextSAKAI.lookup - You do not have permission to view this resource " + path);
			return null;
		}
		catch (IdUnusedException e)
		{
			M_log.debug("DirContextSAKAI.lookup - This resource does not exist: " + path);
			return null;
		}
		catch (EntityPropertyNotDefinedException e)
		{
			M_log.warn("DirContextSAKAI.lookup - This resource is empty: " + path);
			return null;
		}
		catch (EntityPropertyTypeException e)
		{
			M_log.warn("DirContextSAKAI.lookup - This resource has a EntityPropertyTypeException exception: " + path);
			return null;
		} catch (TypeException e) {
			return null;
		}	
	}
}
