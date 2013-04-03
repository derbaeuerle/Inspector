package de.inspector.hsrm.service.utils;

import android.app.Service;
import android.os.Binder;

public abstract class ServiceBinder extends Binder {

	public abstract Service getService();

}
