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
	//Content hosting service
	private ContentHostingService contentHostingService;
	//private ContentCollection collection;
	private SakaiDavHelper sakaiDavHelper;
	public SakaiResourceFactoryImpl(){
		super();
		sakaiDavHelper=new SakaiDavHelper();
		
	}
	//Logger
	private static Log M_log = LogFactory.getLog(SakaiResourceFactoryImpl.class);
	public Resource getResource(String host, String path) {
		if(sakaiDavHelper.prohibited(path)){
			return null;
		}
		boolean isCollection;
		try
		{
			ResourceProperties props;

			//path = fixDirPathSAKAI(path);

			// Do not allow access to /attachments

			if (path.startsWith("/attachments"))
			{
				M_log.info("DirContextSAKAI.lookup - You do not have permission to view this area " + path);
				//throw new NamingException();
			}

			props = contentHostingService.getProperties(sakaiDavHelper.adjustId(path));

			isCollection = props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION);

			if (isCollection)
			{
				return new SakaiFolderResource(path);
			}
			else{
				return new SakaiFileResource(path);
			}
		}
		catch (PermissionException e)
		{
			M_log.debug("DirContextSAKAI.lookup - You do not have permission to view this resource " + path);
			//throw new NamingException();
		}
		catch (IdUnusedException e)
		{
			M_log.debug("DirContextSAKAI.lookup - This resource does not exist: " + path);
			//throw new NamingException();
		}
		catch (EntityPropertyNotDefinedException e)
		{
			M_log.warn("DirContextSAKAI.lookup - This resource is empty: " + path);
			//throw new NamingException();
		}
		catch (EntityPropertyTypeException e)
		{
			M_log.warn("DirContextSAKAI.lookup - This resource has a EntityPropertyTypeException exception: " + path);
			//throw new NamingException();
		} catch (TypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	

}
