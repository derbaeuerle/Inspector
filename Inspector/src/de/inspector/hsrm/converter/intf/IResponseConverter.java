package de.inspector.hsrm.converter.intf;

/**
 * Interface for response converter. Every converter is able to convert the
 * response data into a specific type and to deliver a mime type according to
 * converted type.
 * 
 * @author Dominic Baeuerle
 * 
 */
public interface IResponseConverter {

	/**
	 * Converting {@link Object} data and returns {@link Object}.
	 * 
	 * @param data
	 *            Object of response data before converting.
	 * @return {@link Object} (mostly {@link String} like JSON or XML).
	 */
	public Object convert(Object data);

	/**
	 * Returns {@link String} of mime type description.
	 * 
	 * @return {@link String}
	 */
	public String getMimeType();

}
