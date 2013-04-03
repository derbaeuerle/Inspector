package de.hsrm.inspector.services;

import android.app.Service;
import android.os.Binder;

public abstract class BinderInjection extends Binder {

	public abstract Service getService();

}
