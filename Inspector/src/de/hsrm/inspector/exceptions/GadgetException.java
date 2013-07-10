package de.hsrm.inspector.exceptions;

/**
 * Created by dobae on 27.05.13.
 */
public class GadgetException extends Exception {

	private static final long serialVersionUID = 2788066028132522302L;
	private int mErrorCode = Integer.MIN_VALUE;

	public GadgetException(String msg) {
		super(msg);
	}

	public GadgetException(String msg, int errorCode) {
		super(msg);
		mErrorCode = errorCode;
	}

	public int getErrorCode() {
		return mErrorCode;
	}

}
