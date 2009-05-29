/*
 * DataEvent.java
 *
 * Created on 11 August 2008, 23:48
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package au.com.jwatmuff.genericdb.distributed;

import au.com.jwatmuff.eventmanager.util.ObjectCopier;
import java.io.Serializable;

/**
 *
 * @author James
 */
public class DataEvent<T extends Distributable> implements Serializable {
    public enum Type {
        CREATE, UPDATE, DELETE
    }

    public enum TransactionStatus {
        BEGIN, END, CURRENT, NONE
    }
    
    private T data;
    private Class dataClass;
    private Type eventType;
    private TransactionStatus transactionStatus = TransactionStatus.NONE;
    
    /** Creates a new instance of DataEvent */
    @SuppressWarnings("unchecked")
    public DataEvent(T data, Type eventType) {
        if(data instanceof Serializable) {
            data = (T)ObjectCopier.copy((Serializable)data);
        }
        this.data = data;
        this.dataClass = data.getClass();
        this.eventType = eventType;
    }
    
    public Class getDataClass() {
        return dataClass;
    }
    
    public T getData() {
        return data;
    }
    
    public Type getType() {
        return eventType;
    }
    
    public void setTransactionStatus(TransactionStatus transactionStatus) {
        this.transactionStatus = transactionStatus;
    }
    
    public TransactionStatus getTransactionStatus() {
        return transactionStatus;
    }
    
    public Timestamp getTimestamp() {
        return data.getTimestamp();
    }
    
    @Override
    public String toString() {
        String transaction = "";
        switch(transactionStatus) {
            case BEGIN:
                transaction = " (T start)";
                break;
            case END:
                transaction = " (T end)";
                break;
            case CURRENT:
                transaction = " (T)";
                break;
        }

        return eventType + ": " + dataClass.getSimpleName() + transaction;
    }
}

