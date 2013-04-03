package de.hsrm.inspector.services.helper;

import android.app.Service;
import android.os.Binder;

public abstract class ServiceBinder extends Binder {

	public abstract Service getService();

}
