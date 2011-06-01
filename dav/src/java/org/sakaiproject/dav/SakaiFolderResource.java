package org.sakaiproject.dav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FolderResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
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

public class SakaiFolderResource  implements FolderResource {
	private static Log M_log = LogFactory.getLog(SakaiFolderResource.class);
	private ContentHostingService contentHostingService;
	private SakaiDavHelper sakaiDavHelper;
	public SakaiFolderResource(){
		sakaiDavHelper=new SakaiDavHelper();
	}
	public CollectionResource createCollection(String path)
			throws NotAuthorizedException, ConflictException,
			BadRequestException {
		if (HttpManager.request().getContentLengthHeader() > 0) {
			//resp.sendError(SakaidavStatus.SC_UNSUPPORTED_MEDIA_TYPE);
			return null;
		}
		
		
		//TODO: add is readonly
		//TODO: add is locked 

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
		ArrayList children = new ArrayList();
		List<Entity> members = collection.getMemberResources();
		for(Entity member:members){
			children.add(member);
		}
		return children;
	}

	public Object authenticate(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean authorise(Request arg0, Method arg1, Auth arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	public String checkRedirect(Request arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getModifiedDate() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getRealm() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUniqueId() {
		// TODO Auto-generated method stub
		return null;
	}

	public Resource createNew(String arg0, InputStream arg1, Long arg2,
			String arg3) throws IOException, ConflictException,
			NotAuthorizedException, BadRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	public void copyTo(CollectionResource arg0, String arg1)
			throws NotAuthorizedException, BadRequestException,
			ConflictException {
		// TODO Auto-generated method stub
		
	}

	public void delete() throws NotAuthorizedException, ConflictException,
			BadRequestException {
		// TODO Auto-generated method stub
		
	}

	public Long getContentLength() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContentType(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Long getMaxAgeSeconds(Auth arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public void sendContent(OutputStream arg0, Range arg1,
			Map<String, String> arg2, String arg3) throws IOException,
			NotAuthorizedException, BadRequestException {
		// TODO Auto-generated method stub
		
	}

	public void moveTo(CollectionResource arg0, String arg1)
			throws ConflictException, NotAuthorizedException,
			BadRequestException {
		// TODO Auto-generated method stub
		
	}

	public Date getCreateDate() {
		// TODO Auto-generated method stub
		return null;
	}

}
