package de.inspector.hsrm.gadgets;

import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;

import android.app.Service;
import android.net.Uri;
import de.inspector.hsrm.converter.intf.IResponseConverter;

public abstract class Gadget {

	private String mService;
	private Service mBoundService;
	private String mPattern;
	private IResponseConverter mConverter;

	public abstract Object gogo(HttpRequest request, HttpContext context, Uri requestLine) throws Exception;

	public boolean usesService() {
		return mService != null;
	}

	/**
	 * @return the mBoundService
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
	 * @return the mService
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
	 * @return the mPattern
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
	 * @return the mConverter
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
