package org.osehra.integration.core.router;

/**
 * The no operation router.
 * 
 * @author Julian Jewel
 */
public class NoOpRouter implements Router<Object, Object> {
	/**
	 * Route the object.
	 * 
	 * @param arg
	 *            the input message
	 * @return the input message
	 * @throws RouterException
	 *             an error occurred in routing
	 */
	@Override
	public Object route(final Object arg) throws RouterException {
		return arg;
	}
}
