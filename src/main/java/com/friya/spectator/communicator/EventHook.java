package com.friya.spectator.communicator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class EventHook 
{
	private static Logger logger = Logger.getLogger(EventHook.class.getName());

	private String className;
	private String methodName;
	private boolean invokeBeforeCalling;
	private HashMap<String, String> variables;

	public EventHook copyTo(EventHook copy)
	{
		copy.className = className;
		copy.methodName = methodName;
		copy.setInvokeBeforeCalling(invokeBeforeCalling);
		if(variables != null) {
			copy.variables = new HashMap<String, String>();
			for (Map.Entry<String, String> entry : variables.entrySet()) {
				copy.variables.put(entry.getKey(), entry.getValue());
			}
		}
		return copy;
	}
	
	public Object getVariablesResult(String variable, Object sourceObject, Object[] args)
	{
		if(variables.containsKey(variable) == false) {
			logger.severe("The event does not describe how to get data for \"" + variable + "\"");
			throw new RuntimeException("missing variable");
		}

		return parseVariable(variables.get(variable), sourceObject, args);
	}

	private Object parseVariable(String variable, Object sourceObject, Object[] args)
	{
//		logger.info("parseVariable " + variable + " " + sourceObject + " " + Arrays.toString(args));
		
		if(variable == null) {
			logger.severe("Attempted to parse null variable");
			return null;
		}
		
		String[] tokens = variable.split("\\.");

		if(tokens.length < 2) {
			logger.severe("A variable must have at least 2 tokens (tokens are separated by a '.')");
			return null;
		}
		
		// arg.0.method | arg.0.variable
		Object currentTarget = null;
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals("arg")) {
				i++;
				int index = Integer.parseInt(tokens[i]);
				currentTarget = args[index];
//				logger.info("got args[" + index + "]");

			} else if(tokens[i].equals("sourceObject")) {
				currentTarget = sourceObject;
//				logger.info("getting sourceObject: " + currentTarget);

			} else {
//				logger.info("getting " + currentTarget + "." + tokens[i]);
				currentTarget = fetch(currentTarget, tokens[i]);
			}

//			logger.info("currentTarget: " + currentTarget);
		}
		
		return currentTarget;
	}
	
	private Object fetch(Object target, String targetVarOrMethod)
	{
		try {
			if(targetVarOrMethod.endsWith("()")) {
				targetVarOrMethod = targetVarOrMethod.replace("()", "");
//				logger.info("Getting from method: " + targetVarOrMethod);
				return getFromMethodIn(target, targetVarOrMethod);

			} else {
//				logger.info("Getting from variable: " + targetVarOrMethod);
				return getFromVariableIn(target, targetVarOrMethod);
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchFieldException e) {
			logger.severe("Failed to fetch from " + target.toString() + "." + targetVarOrMethod + ". Is method or field there? Public? Anything odd about it?");
			throw new RuntimeException(e);
		}
	}
	
	private Object getFromMethodIn(Object target, String method) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
        Method m = target.getClass().getMethod(method);
        return m.invoke(target);
	}
	
	private Object getFromVariableIn(Object target, String variable) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Field field = target.getClass().getDeclaredField(variable);
		return field.get(target);
	}
	
	public boolean hasVariable(String variable)
	{
		return variables != null && variables.containsKey(variable);
	}
	

	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getMethodName() {
		return methodName;
	}
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public boolean isInvokeBeforeCalling() {
		return invokeBeforeCalling;
	}
	public void setInvokeBeforeCalling(boolean invokeBeforeCalling) {
		this.invokeBeforeCalling = invokeBeforeCalling;
	}

	public HashMap<String, String> getVariables() {
		return variables;
	}
	public void setVariables(HashMap<String, String> variables) {
		this.variables = variables;
	}
}
