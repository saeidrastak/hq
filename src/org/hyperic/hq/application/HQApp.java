package org.hyperic.hq.application;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.action.Executable;
import org.hibernate.impl.SessionImpl;
import org.hyperic.hibernate.Util;
import org.hyperic.util.callback.CallbackDispatcher;

/**
 * This class represents the central concept of the Hyperic HQ application.  
 * (not the Application resource)
 */
public class HQApp { 
    private static final HQApp INSTANCE = new HQApp(); 
    private final Log _log = LogFactory.getLog(HQApp.class);
    
    private ThreadLocal        _txListeners    = new ThreadLocal();
    private List               _startupClasses = new ArrayList();
    private CallbackDispatcher _callbacks = new CallbackDispatcher();
    
    /**
     * @see CallbackDispatcher#generateCaller(Class)
     */
    public Object registerCallbackCaller(Class iFace) {
        return _callbacks.generateCaller(iFace);
    }
    
    /**
     * @see CallbackDispatcher#registerListener(Class, Object)
     */
    public void registerCallbackListener(Class iFace, Object listener) {
        _callbacks.registerListener(iFace, listener);
    }
    
    /**
     * Adds a class to the list of classes to invoke when the application has
     * started.
     */
    public void addStartupClass(String className) {
        synchronized (_startupClasses) {
            _startupClasses.add(className);
        }
    }
    
    /**
     * Execute the registered startup classes.
     */
    public void runStartupClasses() {
        List classNames;
        
        synchronized (_startupClasses) {
            classNames = new ArrayList(_startupClasses);
        }
        
        for (Iterator i=classNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            
            try {
                Class c = Class.forName(name);
                StartupListener l = (StartupListener)c.newInstance();
     
                _log.info("Executing startup: " + name);
                l.hqStarted();
            } catch(Exception e) {
                _log.warn("Error executing startup listener [" + name + "]", e);
            }
        }
    }
    
    private void scheduleCommitCallback() {
        SessionImpl s = (SessionImpl)
            Util.getSessionFactory().getCurrentSession();
        
        s.getActionQueue().execute(new Executable() {
            public void afterTransactionCompletion(boolean success) {
                runPostCommitListeners(success);
            }

            public void beforeExecutions() {}

            public void execute() {}

            public Serializable[] getPropertySpaces() {
                return new Serializable[0];
            }

            public boolean hasAfterTransactionCompletion() {
                return true;
            }
        });
    }
    
    /**
     * Register a listener to be called after a tx has been committed.
     */
    public void addTransactionListener(TransactionListener listener) {
        List listeners = (List)_txListeners.get();
        
        if (listeners == null) {
            listeners = new ArrayList(1);
            _txListeners.set(listeners);
            scheduleCommitCallback();
        }
        
        listeners.add(listener);
    }

    /**
     * Execute all the post-commit listeners registered with the current thread
     */
    private void runPostCommitListeners(boolean success) {
        List list = (List)_txListeners.get();
        
        if (list == null)
            return;
        
        try {
            for (Iterator i=list.iterator(); i.hasNext(); ) {
                TransactionListener l = (TransactionListener)i.next();
            
                try {
                    l.afterCommit(success);
                } catch(Exception e) {
                    _log.warn("Error running commit listener [" + l + "]", e);
                }
            } 
        } finally {
            _txListeners.set(null);
        }
    }
    
    public static HQApp getInstance() {
        return INSTANCE;
    }
}
