/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EventListener;

/**
 *
 * @author eew
 */
public class WeakListenerManager {

	public static <L extends EventListener> L weakListener(
			Object target,
			Class<L> listenerClass,
			L listener)
	{
		String removeMethod = "remove"+listenerClass.getSimpleName();
		return weakListener(target, removeMethod, listenerClass, listener);
	}

	public static <L extends EventListener> L weakListener(
			Object target,
			String removeMethod,
			Class<L> listenerClass,
			L listener)
	{
		for (Method m : target.getClass().getMethods()) {
			Class[] params = m.getParameterTypes();
			if (m.getName().equals(removeMethod)
					&& params.length == 1 && params[0] == listenerClass)
			{
				return weakListener(target, m, listenerClass, listener);
			}
		}
		throw new IllegalArgumentException("No suitable remove method found");
	}

	public static <L extends EventListener> L weakListener(
			Object target,
			Method removeMethod,
			Class<L> listenerClass,
			L listener)
	{
		Handler h = new Handler(target, removeMethod, listener);
		Class[] type = { listenerClass };
		Object proxy = Proxy.newProxyInstance(null, type, h);
		return listenerClass.cast(proxy);
	}

	private static class Handler implements InvocationHandler {

		private final Object target;
		private final Method removeMethod;
		private final WeakReference listener;

		private Handler(Object target, Method removeMethod, Object listener) {
			this.target = target;
			this.removeMethod = removeMethod;
			this.listener = new WeakReference(listener);
		}

		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable
		{
			Object l = listener.get();

			if (method.getDeclaringClass() == Object.class) {
				String name = method.getName();
				if (name.equals("equals")) {
					return proxy == args[0];
				}
				else if (name.equals("toString")) {
					return "Proxy@"+System.identityHashCode(proxy)
							+"["+listener.get()+"]";
				}
				else if (name.equals("hashCode")) {
					return this.hashCode();
				}
				else {
					return null;
				}
			}
			else if (l == null) {
				try {
					removeMethod.invoke(target, proxy);
				}
				catch (IllegalAccessException ex) {
					ex.printStackTrace();
				}
				catch (IllegalArgumentException ex) {
					ex.printStackTrace();
				}
				catch (InvocationTargetException ex) {
					ex.printStackTrace();
				}
				return null;
			}
			else {
				return method.invoke(l, args);
			}
		}

	}

}
