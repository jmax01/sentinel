package sentinel.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sentinel.apis.API;
import sentinel.apis.APIManager;
import sentinel.apis.Action;
import sentinel.apis.GET;
import sentinel.apis.POST;
import sentinel.apis.PUT;
import sentinel.exceptions.NoSuchActionException;

public abstract class ActionFunctions {

	private static final Logger log = LogManager.getLogger(ActionFunctions.class);
	
    /**
     * Returns a PageElement object for a given elementName string from current page. Gets current page reference, replaces page name space characters qith '_'
     * 
     * TODO: Unit test - successfully create an action
     * TODO: Unit test - fail to create an action because the API object doesn't exist
     * TODO: Unit test - fail to create an action because the action doesn't exist on the API object
     * TODO: Unit test - successfully create a GET action
     * TODO: Unit test - successfully create a PUT action
     * TODO: Unit test - successfully create a POST action
     * 
     * @param actionName String the name of the requested action
     * @param uid String the unique identifier to find the correct API for multi-threading
     * @return Action the requested action
     * @throws NoSuchActionException if action is not found or defined.
     */
    public static Action getAction(String actionName, String uid) throws NoSuchActionException {
    	API api = APIManager.getAPI(uid);
        actionName = actionName.replaceAll("\\s+", "_").toLowerCase();
        Method actionMethod = null;
        try { // Create a Method object to store the Action we want to exercise;
        	actionMethod = api.getClass().getMethod(actionName);
        } catch (NoSuchMethodException e) {
            String errorMessage = StringUtils.format("Action {} is not defined for the API object {}. Make sure you have spelled the API name correctly in your Cucumber step definition and in the API object.", 
            		actionName, api.getName());
            log.error(errorMessage);
            throw new NoSuchActionException(errorMessage, e);
        }
        Action action = null;
        try {  // Invoke the creation of the PageElement and return it into a variable if no exception is thrown.
        	action = (Action) actionMethod.invoke(api);
            log.trace("PageElement Name: " + actionMethod.getName());
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        	String errorMessage = StringUtils.format("Action {} could not be found in API {}. Please ensure the Action is defined on the page.", actionMethod.getName(), api.getName());
        	log.error(errorMessage);
        	throw new NoSuchActionException(errorMessage);
        } 

        return action;
    }

    public static GET getActionAsGet(String actionName, String uid) throws NoSuchActionException {
    	return (GET) getAction(actionName, uid);
    }
    
    public static PUT getActionAsPut(String actionName, String uid) throws NoSuchActionException {
    	return (PUT) getAction(actionName, uid);
    }
    
    public static POST getActionAsPost(String actionName, String uid) throws NoSuchActionException {
    	return (POST) getAction(actionName, uid);
    }
}
