/*
 * CSVBeanReader.java
 *
 * Created on 5 May 2008, 22:47
 */

package au.com.jwatmuff.eventmanager.util;

import au.com.bytecode.opencsv.CSVReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.beanutils.WrapDynaBean;
import org.apache.log4j.Logger;

/**
 * Reads rows of a CSV file and creates a Java bean for each row, populating the
 * properties of the bean from the fields of the row according to a
 * user-specified mapping.
 * 
 * @author James
 */
public class CSVBeanReader<T> {
    private static Logger log = Logger.getLogger(CSVBeanReader.class);
    
    private CSVReader reader;
    private Map<String,String> mapping;
    private Class<T> clazz;
    private Collection<String> requiredColumns;
    
    private boolean headerRead = false;
    private String[] propertyNames;

    /** Creates a new instance of CSVBeanReader */
    public CSVBeanReader(CSVReader reader, Map<String,String> mapping, Class<T> clazz) {
        this(reader, mapping, clazz, new ArrayList<String>());
    }
    
    public CSVBeanReader(CSVReader reader, Map<String,String> mapping, Class<T> clazz, Collection<String> requiredColumns) {
        this.reader = reader;
        this.clazz = clazz;
        this.mapping = new HashMap<String,String>(mapping);
        this.requiredColumns = new ArrayList<String>(requiredColumns);
    }
    
    public T readBean() {
        if(!headerRead)
            readHeader();

        T bean;
        try {
            bean = (T)clazz.newInstance();
            WrapDynaBean dynaBean = new EnumConvertingWrapDynaBean(bean);
            
            String[] line = reader.readNext();
            if(line == null)
                return null;

            for(int i=0; i<line.length; i++) {
                if(propertyNames[i] != null) {
                    dynaBean.set(propertyNames[i], line[i]);
                }
            }
        } catch(InstantiationException e) {
            throw new RuntimeException("Unable to create bean of class " + clazz, e);
        } catch(IllegalAccessException iae) {
            throw new RuntimeException("Unable to create bean of class " + clazz, iae);
        } catch(IOException ioe) {
            log.error("IOException while reading from CSV file", ioe);
            return null;
        }
        
        return bean;
    }
    
    public List<T> readBeans() {
        List<T> beans = new ArrayList<T>();
        
        T bean;
        while((bean = readBean()) != null)
            beans.add(bean);
        
        return beans;
    }
    
    private void readHeader() {
        headerRead = true;
        
        try {
            String[] line = reader.readNext();   
            if(line == null)
                return;
            
            propertyNames = new String[line.length];
            
            int i = 0;
            for(String value : line) {
                String columnPattern = findFirstMatchingColumnPattern(value);
                requiredColumns.remove(columnPattern);
                propertyNames[i] = mapping.get(columnPattern);
                log.debug("Mapped column " + value + " to property " + propertyNames[i]);
                i++;
            }

        } catch(IOException e) {
            throw new RuntimeException("Unable to read CSV file", e);
        }
        
        if(!requiredColumns.isEmpty()) {
            StringBuilder sb = new StringBuilder("CSV did not contain required columns: ");
            int i = 0;
            for(String heading : requiredColumns) {
                if(i++ > 0) sb.append(", ");
                sb.append(heading);
            }
            throw new RuntimeException(sb.toString());
        }
    }
    
    private String findFirstMatchingColumnPattern(String columnName) {
        for(String columnPattern : mapping.keySet())
            if(columnName.matches("(?i).*" + columnPattern + ".*"))
                return columnPattern;
        
        return null;
    }
}
