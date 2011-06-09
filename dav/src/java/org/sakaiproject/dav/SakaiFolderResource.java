package org.sakaiproject.dav;

//import SakaidavStatus;

//import SakaidavStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.LockedException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
import com.bradmcevoy.http.HttpManager;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.util.DOMWriter;
import org.apache.catalina.util.MD5Encoder;
import org.apache.catalina.util.RequestUtil;
import org.apache.catalina.util.XMLWriter;
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
import org.sakaiproject.event.cover.UsageSessionService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdLengthException;
import org.sakaiproject.exception.IdUniquenessException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.InconsistentException;
import org.sakaiproject.exception.OverQuotaException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeBreakdown;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.user.api.Authentication;
import org.sakaiproject.user.api.AuthenticationException;
import org.sakaiproject.user.api.Evidence;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.AuthenticationManager;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.IdPwEvidence;
import org.sakaiproject.util.RequestFilter;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.Validator;
import org.sakaiproject.was.login.SakaiWASLoginModule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class SakaiFolderResource  implements FolderResource,LockableResource{
	private static Log M_log = LogFactory.getLog(SakaiFolderResource.class);
	private ContentHostingService contentHostingService;
	private SakaiDavHelper sakaiDavHelper;
	private String path;
	private boolean readOnly;
	ResourceProperties props;
	
	/* The Resource Properties */
	
	public boolean collection;//is it a collection
	public boolean exists;//Does the Resource Specified by the path exists
	public long length;
	public long creationDate;//Date Created
	public String MIMEType;
	public long modificationDate;
	public long date; // From DirContext
	public String displayName;
	public String resourceName; // The "non-display" name
	public String resourceLink; // The resource link (within SAKAI)
	public String eTag; // The eTag
	
	
	
	/**
	 * Repository of the locks put on single resources.
	 * <p>
	 * Key : path <br>
	 * Value : LockInfo
	 */
	private Hashtable<String,LockToken> resourceLocks = new Hashtable<String,LockToken>();//same as in dotcms

	
	public SakaiFolderResource(String path) throws PermissionException, IdUnusedException, EntityPropertyNotDefinedException, EntityPropertyTypeException, TypeException {
		this.path=path;
		sakaiDavHelper=new SakaiDavHelper(path);
		getResourceInfo();

	}
	
	private void getResourceInfo() throws PermissionException, IdUnusedException, EntityPropertyNotDefinedException, EntityPropertyTypeException, TypeException{
		props = contentHostingService.getProperties(sakaiDavHelper.adjustId(path));
		Entity mbr;
		exists = false;
		collection=false;
		collection = props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION);
		resourceName = props.getProperty(ResourceProperties.PROP_DISPLAY_NAME);
		displayName = props.getPropertyFormatted(ResourceProperties.PROP_DISPLAY_NAME);
		exists = true;
		if (!collection)
		{
			mbr = contentHostingService.getResource(sakaiDavHelper.adjustId(path));
			// Props for a file is OK from above
			length = ((ContentResource) mbr).getContentLength();
			MIMEType = ((ContentResource) mbr).getContentType();
			eTag = ((ContentResource) mbr).getId();
		}
		else
		{
			mbr = contentHostingService.getCollection(sakaiDavHelper.adjustId(path));
			props = mbr.getProperties();
			eTag = path;
		}
		modificationDate = props.getTimeProperty(ResourceProperties.PROP_MODIFIED_DATE).getTime();
		eTag = modificationDate + "+" + eTag;
		if (M_log.isDebugEnabled()) M_log.debug("Path=" + path + " eTag=" + eTag);
		creationDate = props.getTimeProperty(ResourceProperties.PROP_CREATION_DATE).getTime();
		resourceLink = mbr.getUrl();
	}
	public CollectionResource createCollection(String path)
			throws NotAuthorizedException, ConflictException,
			BadRequestException {
		if (HttpManager.request().getContentLengthHeader() > 0) {
			//resp.sendError(SakaidavStatus.SC_UNSUPPORTED_MEDIA_TYPE);
			return null;
		}
		
		
		//TODO: add is readonly
		if(sakaiDavHelper.isLocked(contentHostingService, path)){
			return null;
		}
		if (readOnly)
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}

		// path //= getRelativePathSAKAI(HttpManager.request());
		if (sakaiDavHelper.prohibited(path) || (path.toUpperCase().startsWith("/WEB-INF")) || (path.toUpperCase().startsWith("/META-INF")))
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}

		String name = sakaiDavHelper.justName(path);//we have just name

		if ((name.toUpperCase().startsWith("/WEB-INF")) || (name.toUpperCase().startsWith("/META-INF")))
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}

		// Check to see if the parent collection exists. ContentHosting will create a parent folder if it 
		// does not exist, but the WebDAV spec requires this operation to fail (rfc2518, 8.3.1).
		
		String parentId = sakaiDavHelper.isolateContainingId(sakaiDavHelper.adjustId(path));//we have parentid
		
		try {
			contentHostingService.getCollection(parentId);//we have this too
		} catch (IdUnusedException e1) {
			//resp.sendError(SakaidavStatus.SC_CONFLICT);
			return null;		
		} catch (TypeException e1) {
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;						
		} catch (PermissionException e1) {
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;			
		}
		
		String adjustedId = sakaiDavHelper.adjustId(path);//we have adjusted id 
	//How to work with the response object
		// Check to see if collection with this name already exists
		try
		{	
			contentHostingService.getProperties(adjustedId);
			
			// return error (litmus: MKCOL on existing collection should fail, RFC2518:8.3.1 / 8.3.2)
			
			//resp.sendError(SakaidavStatus.SC_METHOD_NOT_ALLOWED);
			return null;

		}
		catch (IdUnusedException e)
		{
			// Resource not found (this is actually the normal case)
		}
		catch (PermissionException e)
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}

		// Check to see if a resource with this name already exists
		if (adjustedId.endsWith("/") && adjustedId.length() > 1)
		try
		{
			String idToCheck = adjustedId.substring(0, adjustedId.length() - 1);
			
			contentHostingService.getProperties(idToCheck);
			
			// don't allow overwriting an existing resource (litmus: mkcol_over_plain)

			//resp.sendError(SakaidavStatus.SC_METHOD_NOT_ALLOWED);
			return null;

		}
		catch (IdUnusedException e)
		{
			// Resource not found (this is actually the normal case)
		}
		catch (PermissionException e)
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		
		// Add the collection

		try
		{
			ContentCollectionEdit edit = contentHostingService.addCollection(sakaiDavHelper.adjustId(path));
			ResourcePropertiesEdit resourceProperties = edit.getPropertiesEdit();
			resourceProperties.addProperty(ResourceProperties.PROP_DISPLAY_NAME, name);
			contentHostingService.commitCollection(edit);
		}

		catch (IdUsedException e)
		{
			// Should not happen because if this esists, we either return or delete above
		}
		catch (IdInvalidException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() IdInvalid:" + e.getMessage());
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (PermissionException e)
		{
			// This is normal
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (InconsistentException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() InconsistentException:" + e.getMessage());
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	
	
	public Resource child(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<? extends Resource> getChildren() {
		ContentCollection collection;
		// TODO Auto-generated method stub
		try {
			collection = contentHostingService.getCollection(sakaiDavHelper.adjustId(((HttpServletRequest) HttpManager.request()).getPathInfo()));
		} catch (IdUnusedException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		} catch (TypeException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		} catch (PermissionException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return null;
		}
		ArrayList<Resource> children = new ArrayList<Resource>();
		List<Resource> members = collection.getMemberResources();
		for(Resource member:members){
			children.add(member);
		}
		return children;
	}

	public Object authenticate(String username, String password) {
		// TODO Auto-generated method stub
		return username;
	}

	public boolean authorise(Request arg0, Method arg1, Auth arg2) {
		// TODO Auto-generated method stub
		return true;
	}

	public String checkRedirect(Request arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getModifiedDate() {
		return new Date(modificationDate);
		//return null;
	}

	public String getName() {
		return resourceName;
	}

	public String getRealm() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUniqueId() {
		
		return contentHostingService.getUuid(path);
	}
	/*
	 *This is analogous to the doPut method 
	 * (non-Javadoc)
	 * @see com.bradmcevoy.http.PutableResource#createNew(java.lang.String, java.io.InputStream, java.lang.Long, java.lang.String)
	 */

	public Resource createNew(String nameOfResource, InputStream instream, Long length,
			String contentType) throws IOException, ConflictException,
			NotAuthorizedException, BadRequestException {
		if (!sakaiDavHelper.isFileNameAllowed())
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		ResourceProperties oldProps = null;
		Collection oldGroups = null;
		boolean oldPubView = false;
		boolean oldHidden = false;
		Time releaseDate = null;
		Time retractDate = null;
		
		boolean newfile = true;
		if (sakaiDavHelper.prohibited(path) || (path.toUpperCase().startsWith("/WEB-INF")) || (path.toUpperCase().startsWith("/META-INF")))
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		if (path.length() > 254)
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		if ((nameOfResource.toUpperCase().startsWith("/WEB-INF")) || (nameOfResource.toUpperCase().startsWith("/META-INF")))
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		// Try to delete the resource
		try
		{
			// The existing document may be a collection or a file.
			boolean isCollection = contentHostingService.getProperties(sakaiDavHelper.adjustId(path)).getBooleanProperty(
					ResourceProperties.PROP_IS_COLLECTION);

			if (isCollection)
			{
				contentHostingService.removeCollection(sakaiDavHelper.adjustId(path));
			}
			else
			{
			    	String id = sakaiDavHelper.adjustId(path);
				// save original properties; we're just updating the file
				oldProps = contentHostingService.getProperties(id);
				newfile = false;

				try {
				    ContentResource resource = contentHostingService.getResource(id);
				    oldGroups = resource.getGroups();
				    oldHidden = resource.isHidden();
				    releaseDate = resource.getReleaseDate();
				    retractDate = resource.getRetractDate();
				} catch (Exception e) {M_log.info("doPut fail 1" + e);} ;

				try {
				    if (!contentHostingService.isInheritingPubView(id))
					if (contentHostingService.isPubView(id)) 
					    oldPubView = true;
				} catch (Exception e) {M_log.info("doPut fail 2" + e);};
				
				contentHostingService.removeResource(sakaiDavHelper.adjustId(path));
			}
		}
		catch (PermissionException e)
		{
			// Normal situation
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (InUseException e)
		{
			// Normal situation
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN); // %%%
			return null;
		}
		catch (IdUnusedException e)
		{
			// Normal situation - nothing to do
		}
		catch (EntityPropertyNotDefinedException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() - EntityPropertyNotDefinedException " + path);
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (TypeException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() - TypeException " + path);
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (EntityPropertyTypeException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() - EntityPropertyType " + path);
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (ServerOverloadException e)
		{
			M_log.warn("SAKAIDavServlet.doMkcol() - ServerOverloadException " + path);
			//resp.sendError(SakaidavStatus.SC_SERVICE_UNAVAILABLE);
			return null;
		}
		if (M_log.isDebugEnabled()) M_log.debug("  req.contentType() =" + contentType);
		if (contentType == null)
		{
			if (M_log.isDebugEnabled()) M_log.debug("Unable to determine contentType");
			contentType = ""; // Still cannot figure it out
		}
		try
		{
			User user = UserDirectoryService.getCurrentUser();

			TimeBreakdown timeBreakdown = TimeService.newTime().breakdownLocal();
			String mycopyright = "copyright (c)" + " " + timeBreakdown.getYear() + ", " + user.getDisplayName()
					+ ". All Rights Reserved. ";

			// use this code rather than the long form of addResource
			// because it doesn't add an extension. Delete doesn't, so we have
			// to match, and I'd just as soon be able to create items with no extension anyway
			
			ContentResourceEdit edit = contentHostingService.addResource(sakaiDavHelper.adjustId(path));
			edit.setContentType(contentType);
			edit.setContent(instream);
			ResourcePropertiesEdit p = edit.getPropertiesEdit();

			try {
			    if (oldGroups != null && !oldGroups.isEmpty())
				edit.setGroupAccess(oldGroups);
			} catch (Exception e) {M_log.info("doPut fail 3 " + e + " " + oldGroups);};

			try {
			    edit.setAvailability(oldHidden, releaseDate, retractDate);
			} catch (Exception e) {M_log.info("doPut fail 4 " + e);};


			// copy old props, if any
			if (oldProps != null)
			{
				Iterator it = oldProps.getPropertyNames();

				while (it.hasNext())
				{
					String pname = (String) it.next();

					// skip any live properties
					if (!oldProps.isLiveProperty(pname))
					{
						p.addProperty(pname, oldProps.getProperty(pname));
					}
				}
			}

			if (newfile)
			{
				p.addProperty(ResourceProperties.PROP_COPYRIGHT, mycopyright);
				p.addProperty(ResourceProperties.PROP_DISPLAY_NAME, nameOfResource);
			}

			// commit the change
			contentHostingService.commitResource(edit, NotificationService.NOTI_NONE);
			if (oldPubView)
			    contentHostingService.setPubView(sakaiDavHelper.adjustId(path), true);

		}
		catch (IdUsedException e)
		{
			// Should not happen because we deleted above (unless two requests at same time)
			M_log.warn("SAKAIDavServlet.doPut() IdUsedException:" + e.getMessage());

			//resp.sendError(HttpServletResponse.SC_CONFLICT);
			return null;
		}
		catch (IdInvalidException e)
		{
			M_log.warn("SAKAIDavServlet.doPut() IdInvalidException:" + e.getMessage());
			//resp.sendError(HttpServletResponse.SC_CONFLICT);
			return null;
		}
		catch (PermissionException e)
		{
			// Normal
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (OverQuotaException e)
		{
			// Normal %%% what's the proper response for over-quota?
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return null;
		}
		catch (InconsistentException e)
		{
			M_log.warn("SAKAIDavServlet.doPut() InconsistentException:" + e.getMessage());
			//resp.sendError(HttpServletResponse.SC_CONFLICT);
			return null;
		}
		catch (ServerOverloadException e)
		{
			M_log.warn("SAKAIDavServlet.doPut() ServerOverloadException:" + e.getMessage());
			//resp.setStatus(SakaidavStatus.SC_SERVICE_UNAVAILABLE);
			return null;
		}
		// TODO Auto-generated method stub
		return null;
	}
	/*
	 * 
	 * (non-Javadoc)
	 * @see com.bradmcevoy.http.CopyableResource#copyTo(com.bradmcevoy.http.CollectionResource, java.lang.String)
	 */
	public void copyTo(CollectionResource arg0, String arg1)
			throws NotAuthorizedException, BadRequestException,
			ConflictException {
		// TODO Auto-generated method stub
		
	}
	/*
	 * doDelete Method 
	 * (non-Javadoc)
	 * @see com.bradmcevoy.http.DeletableResource#delete()
	 */
	public void delete() throws NotAuthorizedException, ConflictException,
			BadRequestException {
		if (readOnly)
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return;
		}

		if (sakaiDavHelper.isLocked(contentHostingService,path))
		{
			//resp.sendError(SakaidavStatus.SC_LOCKED);
			return;
		}
		if (sakaiDavHelper.prohibited(path) || (path.toUpperCase().startsWith("/WEB-INF")) || (path.toUpperCase().startsWith("/META-INF")))
		{
			//resp.sendError(SakaidavStatus.SC_FORBIDDEN);
			return;
		}
		boolean isCollection = false;
		try
		{
			isCollection = contentHostingService.getProperties(sakaiDavHelper.adjustId(path)).getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION);

			if (isCollection)
			{
				contentHostingService.removeCollection(sakaiDavHelper.adjustId(path));
			}
			else
			{
				contentHostingService.removeResource(sakaiDavHelper.adjustId(path));
			}
		}
		catch (PermissionException e)
		{
			return ;
		}
		catch (InUseException e)
		{
			return ;
		}
		catch (IdUnusedException e)
		{
			// Resource not found
			//resp.sendError(SakaidavStatus.SC_NOT_FOUND);
			return ;
		}
		catch (EntityPropertyNotDefinedException e)
		{
			M_log.warn("SAKAIDavServlet.deleteResource() - EntityPropertyNotDefinedException " + path);
			return ;
		}
		catch (EntityPropertyTypeException e)
		{
			M_log.warn("SAKAIDavServlet.deleteResource() - EntityPropertyTypeException " + path);
			return ;
		}
		catch (TypeException e)
		{
			M_log.warn("SAKAIDavServlet.deleteResource() - TypeException " + path);
			return ;
		}
		catch (ServerOverloadException e)
		{
			M_log.warn("SAKAIDavServlet.deleteResource() - ServerOverloadException " + path);
			return ;
		}
		// TODO Auto-generated method stub
		
	}

	/*
	 * 
	 * (non-Javadoc)
	 * @see com.bradmcevoy.http.GetableResource#getContentLength()
	 */
	public Long getContentLength() {
		
		return length;
	}

	public String getContentType(String arg0) {
		// TODO Auto-generated method stub
		return MIMEType;
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void sendContent(OutputStream arg0, Range arg1,
			Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, BadRequestException {
		// TODO Auto-generated method stub
		
	}

	public void moveTo(CollectionResource arg0, String arg1)
			throws ConflictException, NotAuthorizedException,
			BadRequestException {
		// TODO Auto-generated method stub
		
	}

	@SuppressWarnings("deprecation")
	public Date getCreateDate() {
		return new Date(creationDate);
	}
	public LockResult lock(LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException, PreConditionFailedException,
			LockedException {
		    LockToken token = new LockToken();
		    token.info = lockInfo;
		    token.timeout = LockTimeout.parseTimeout("30");
		    token.tokenId = getUniqueId();
		    resourceLocks.put(getUniqueId(), token);
		    return LockResult.success(token);
		// TODO Auto-generated method stub
		//return null;
	}
	public LockResult refreshLock(String token) throws NotAuthorizedException,
			PreConditionFailedException {
		//TODO :refresh LOCK discuss with mentor
		return null;
	}
	public void unlock(String tokenId) throws NotAuthorizedException,
			PreConditionFailedException {
		resourceLocks.remove(getUniqueId());
		
	}
	public LockToken getCurrentLock() {
			return resourceLocks.get(getUniqueId());
		//return null;
	}

}
