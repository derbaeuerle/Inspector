package de.inspector.hsrm.gadgets;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.net.Uri;
import de.inspector.hsrm.converter.intf.IResponseConverter;
import de.inspector.hsrm.service.utils.AsyncServiceBinder;

/**
 * A {@link Gadget} represents a restful handler and contains following
 * attributes. {@link String} {@link #mService} for optional needed
 * {@link Service}, {@link Service} {@link #mService} for
 * {@link AsyncServiceBinder} to inject the bound {@link Service} object,
 * {@link String} {@link #mPattern} to describe the restful URL pattern via
 * regular expression and {@link IResponseConverter} {@link #mConverter} to
 * convert the processed data into suitable data form.
 * 
 * @author Dominic Baeuerle
 * 
 */
public abstract class Gadget {

	private String mService;
	private Service mBoundService;
	private String mPattern;
	private IResponseConverter mConverter;

	/**
	 * Abstract class to overwrite in specific {@link Gadget}. Processes
	 * requests into repsonse content.
	 * 
	 * @param request
	 *            {@link HttpRequest} of current request.
	 * @param context
	 *            {@link HttpContext} of current request.
	 * @param requestLine
	 *            {@link Uri} to provide the whole request URL.
	 * @return {@link Object}
	 * @throws Exception
	 */
	public abstract Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception;

	public boolean usesService() {
		return mService != null;
	}

	/**
	 * @return the bound service.
	 */
	public Service getBoundService() {
		return mBoundService;
	}

	/**
	 * @param mBoundService
	 *            the mBoundService to set
	 */
	public void setBoundService(Service mBoundService) {
		this.mBoundService = mBoundService;
	}

	/**
	 * @return the service path.
	 */
	public String getService() {
		return mService;
	}

	/**
	 * @param mService
	 *            the mService to set
	 */
	public void setService(String mService) {
		this.mService = mService;
	}

	/**
	 * @return the restful url pattern.
	 */
	public String getPattern() {
		return mPattern;
	}

	/**
	 * @param mPattern
	 *            the mPattern to set
	 */
	public void setPattern(String mPattern) {
		this.mPattern = mPattern;
	}

	/**
	 * @return the {@link IResponseConverter} converter.
	 */
	public IResponseConverter getConverter() {
		return mConverter;
	}

	/**
	 * @param mConverter
	 *            the mConverter to set
	 */
	public void setConverter(IResponseConverter mConverter) {
		this.mConverter = mConverter;
	}

}
