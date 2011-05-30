package org.sakaiproject.dav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

public class SakaiFolderResource implements FolderResource{

	public CollectionResource createCollection(String arg0)
			throws NotAuthorizedException, ConflictException,
			BadRequestException {
		// TODO Auto-generated method stub
		return null;
	}

	public Resource child(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<? extends Resource> getChildren() {
		// TODO Auto-generated method stub
		return null;
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
