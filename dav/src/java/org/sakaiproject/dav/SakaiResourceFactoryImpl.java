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
	private SakaiDavHelper sakaiDavHelper;
	public SakaiResourceFactoryImpl(){
		super();
		sakaiDavHelper=new SakaiDavHelper();
		
	}
	//Logger
	private static Log M_log = LogFactory.getLog(SakaiResourceFactoryImpl.class);
	public Resource getResource(String host, String path) {
		contentHostingService = (ContentHostingService) ComponentManager.get(ContentHostingService.class.getName());
		Entity mbr = null;
		try {
			mbr = contentHostingService.getResource(sakaiDavHelper.adjustId(path));
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
	
	

}
