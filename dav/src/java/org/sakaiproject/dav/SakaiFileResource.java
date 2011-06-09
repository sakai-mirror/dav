package org.sakaiproject.dav;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

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
	public SakaiFileResource(String path) {
		// TODO Auto-generated constructor stub
		this.path=path;
	}

	public void copyTo(CollectionResource arg0, String arg1)
			throws NotAuthorizedException, BadRequestException,
			ConflictException {
		// TODO Auto-generated method stub
		
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

	public String processForm(Map<String, String> arg0,
			Map<String, FileItem> arg1) throws BadRequestException,
			NotAuthorizedException, ConflictException {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getCreateDate() {
		// TODO Auto-generated method stub
		return null;
	}

	public LockResult lock(LockTimeout timeout, LockInfo lockInfo)
			throws NotAuthorizedException, PreConditionFailedException,
			LockedException {
		// TODO Auto-generated method stub
		return null;
	}

	public LockResult refreshLock(String token) throws NotAuthorizedException,
			PreConditionFailedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void unlock(String tokenId) throws NotAuthorizedException,
			PreConditionFailedException {
		// TODO Auto-generated method stub
		
	}

	public LockToken getCurrentLock() {
		// TODO Auto-generated method stub
		return null;
	}

}
