package de.inspector.hsrm.service.utils;

import de.inspector.hsrm.gadgets.Gadget;
import android.app.Service;
import android.os.Binder;

/**
 * Abstract class to unify {@link Binder} of android system. Developer need to
 * use this class as {@link Binder} for {@link Service}, if the service can be
 * used in a {@link Gadget}. The {@link ServiceBinder} can retrieve the
 * {@link Service} via unified {@link #getService()} method.
 * 
 * @author Dominic Baeuerle
 * 
 */
public abstract class ServiceBinder extends Binder {

	/**
	 * Returns {@link Service} of {@link ServiceBinder}.
	 * 
	 * @return {@link Service}
	 */
	public abstract Service getService();

}
