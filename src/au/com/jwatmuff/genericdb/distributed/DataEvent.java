/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

