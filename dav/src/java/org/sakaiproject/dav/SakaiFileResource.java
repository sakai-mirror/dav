/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/dav/trunk/dav/src/java/org/sakaiproject/dav/DavServlet.java $
 * $Id: DavServlet.java 76766 2010-04-27 16:13:42Z hedrick@rutgers.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008, 2009 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.dav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.LockedException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.exceptions.PreConditionFailedException;
public class SakaiFileResource implements FileResource,LockableResource{
	private String path;
	private ContentResource contentResource;
	/**
	 * Size of buffer for streaming downloads 
	 */
	protected static final int STREAM_BUFFER_SIZE = 102400;
	ResourceProperties props;
	/**
	 * Repository of the locks put on single resources.
	 * <p>
	 * Key : path <br>
	 * Value : LockInfo
	 */
	private Hashtable<String,LockToken> resourceLocks = new Hashtable<String,LockToken>();
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
	private ContentHostingService contentHostingService;
	private SakaiDavHelper sakaiDavHelper;
	private static Log M_log = LogFactory.getLog(SakaiFileResource.class);
	
	
	public SakaiFileResource(ContentResource contentResource, String path) throws PermissionException, IdUnusedException, EntityPropertyNotDefinedException, EntityPropertyTypeException, TypeException {
		this.path=path;
		sakaiDavHelper=new SakaiDavHelper();
		getResourceInfo();
		this.contentResource=contentResource;
	}


	/**
	 *  Get the resource meta data
	 * @throws PermissionException
	 * @throws IdUnusedException
	 * @throws EntityPropertyNotDefinedException
	 * @throws EntityPropertyTypeException
	 * @throws TypeException
	 */
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

	/**
	 * toCollection is the collection to copy the resource to 
	 */
	public void copyTo(CollectionResource toCollection, String name)
			throws NotAuthorizedException, BadRequestException,
			ConflictException {
		if(toCollection instanceof SakaiFolderResource){
			
		}
		
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
		return new Date(modificationDate);//is this correct??
	}

	public String getName() {
		return resourceName;
	}

	public String getRealm() {//what does realm mean
		// TODO Auto-generated method stub
		return null;
	}

	public String getUniqueId() {
		 return contentHostingService.getUuid(this.path);//ask Seth if this is the case
	}

	public void delete() throws NotAuthorizedException, ConflictException,
			BadRequestException {
		boolean readOnly = false;//What is this readonly ask seth tomorrow
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
		
	}

	public Long getContentLength() {
		return length;
	}

	public String getContentType(String arg0) {
		return MIMEType;
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		return null;
	}

	public void sendContent(OutputStream out, Range arg1,
			Map<String, String> arg2, String contentType) throws IOException,
			NotAuthorizedException, BadRequestException {
		if (sakaiDavHelper.prohibited(path))
		{
	        	
	        	
	        }

		// resource or collection? check the properties (also finds bad id and checks permissions)
		boolean isCollection = false;
		try
		{
		    ResourceProperties props = null;
		    try {
			props = contentHostingService.getProperties(sakaiDavHelper.adjustId(path));
		    } catch (IdUnusedException x) {
			if (!path.endsWith(Entity.SEPARATOR)) {
			    String tempid = path + Entity.SEPARATOR;
			    props = contentHostingService.getProperties(sakaiDavHelper.adjustId(tempid));
			    path = tempid;
			} else {
			   ;
			}
		    }

		    isCollection = props.getBooleanProperty(ResourceProperties.PROP_IS_COLLECTION);
		}
		catch (PermissionException e)
		{
			;
		}
		catch (IdUnusedException e)
		{
			;
		}
		catch (EntityPropertyNotDefinedException e)
		{
			;
		}
		catch (EntityPropertyTypeException e)
		{
			;
		}

		// for resources
		if (!isCollection)
		{
			if (M_log.isDebugEnabled()) M_log.debug("SAKAIAccess doContent is resource " + path);

			InputStream contentStream = null;
			
			
			try
			{
				ContentResource resource = contentHostingService.getResource(sakaiDavHelper.adjustId(path));
				long len = resource.getContentLength();
				

				// for URL content type, encode a redirect to the body URL
					// Similar to handleAccessResource() in BaseContentService.java
					
					contentStream = resource.streamContent();

					if (contentStream == null || len == 0)
					{
						return ;
					}

					
				//	if (!processHead(req, res)) return "Error setting header values";

					//out = res.getOutputStream();
					
					// chunk content stream to response
					byte[] chunk = new byte[STREAM_BUFFER_SIZE];
					int lenRead;
					while ((lenRead = contentStream.read(chunk)) != -1)
					{
						out.write(chunk, 0, lenRead);
					}
				
			}
			catch (Throwable e)
			{
				// M_log.warn(this + ".doContent(): exception: id: " + id + " : " + e.toString());
				//return e.toString();
			}
			finally
			{
				if (contentStream != null) {
					try {
						contentStream.close();
					} catch (IOException e) {
						// ignore
					}
				}

				if (out != null)
				{
					try
					{
						out.close();
					}
					catch (Throwable ignore)
					{
						// ignore
					}
				}
			}
		}
		
	}

	public void moveTo(CollectionResource arg0, String arg1)
			throws ConflictException, NotAuthorizedException,
			BadRequestException {
		// TODO Auto-generated method stub
		
	}

	public String processForm(Map<String, String> arg0,
			Map<String, FileItem> arg1) throws BadRequestException,
			NotAuthorizedException, ConflictException {
		// TODO Auto-generated method stub
		return null;
	}

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
		
	}

	public LockResult refreshLock(String token) throws NotAuthorizedException,
			PreConditionFailedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void unlock(String tokenId) throws NotAuthorizedException,
			PreConditionFailedException {
		resourceLocks.remove(getUniqueId());
		
	}

	public LockToken getCurrentLock() {
		return resourceLocks.get(getUniqueId());
	}

}
