package de.hsrm.inspector.exceptions;

import de.hsrm.inspector.gadgets.intf.Gadget;

/**
 * Custom {@link Exception} for thrown exceptions inside {@link Gadget} objects.
 * Optional there is a error code as {@link Integer} to parse error on browser
 * interface.
 */
public class GadgetException extends Exception {

	private static final long serialVersionUID = 2788066028132522302L;
	private int mErrorCode = Integer.MIN_VALUE;
	private Object mRequest;

	/**
	 * Constructor of {@link GadgetException} with only a {@link String} as
	 * message.
	 * 
	 * @param msg
	 *            {@link String}
	 */
	public GadgetException(String msg) {
		super(msg);
	}

	/**
	 * Constructor of {@link GadgetException} with {@link String} as message and
	 * {@link Integer} as error code.
	 * 
	 * @param msg
	 *            {@link String}
	 * @param errorCode
	 *            {@link Integer}
	 */
	public GadgetException(String msg, int errorCode) {
		this(msg);
		mErrorCode = errorCode;
	}

	/**
	 * Constructor of {@link GadgetException} with {@link String} as message,
	 * {@link Integer} as error code and request {@link Object}.
	 * 
	 * @param msg
	 *            {@link String}
	 * @param errorCode
	 *            {@link Integer}
	 * @param request
	 *            {@link Object}
	 */
	public GadgetException(String msg, int errorCode, Object request) {
		this(msg, errorCode);
		mRequest = request;
	}

	/**
	 * Getter for error code of thrown {@link GadgetException}. If no error code
	 * has been set this method will return {@link Integer#MIN_VALUE}.
	 * 
	 * @return {@link Integer}
	 */
	public int getErrorCode() {
		return mErrorCode;
	}

	/**
	 * Returns request object.
	 * 
	 * @return {@link Object}
	 */
	public Object getRequest() {
		return mRequest;
	}

}
