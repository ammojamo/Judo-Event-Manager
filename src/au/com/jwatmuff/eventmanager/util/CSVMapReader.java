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

package au.com.jwatmuff.eventmanager.util;

import au.com.bytecode.opencsv.CSVReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Reads rows of a CSV file and creates a HashMap for each row, populating the
 * properties of the map from the fields of the row according to a
 * user-specified mapping.
 * 
 * @author James
 */
public class CSVMapReader {
    private static Logger log = Logger.getLogger(CSVMapReader.class);
    
    private CSVReader reader;
    private Map<String,String> mapping;
    private Collection<String> requiredColumns;
    
    private boolean headerRead = false;
    private String[] propertyNames;

    /** Creates a new instance of CSVBeanReader */
    public CSVMapReader(CSVReader reader, Map<String,String> mapping) {
        this(reader, mapping, new ArrayList<String>());
    }
    
    public CSVMapReader(CSVReader reader, Map<String,String> mapping, Collection<String> requiredColumns) {
        this.reader = reader;
        this.mapping = new HashMap<String,String>(mapping);
        this.requiredColumns = new ArrayList<String>(requiredColumns);
    }
    
    public Map<String,String> readRow() {
        if(!headerRead)
            readHeader();

        Map<String,String> row = new HashMap<String,String>();
        try {
            String[] line = reader.readNext();
            if(line == null)
                return null;

            for(int i=0; i<line.length; i++) {
                if(propertyNames[i] != null) {
                    row.put(propertyNames[i], line[i]);
                }
            }
        } catch(IOException ioe) {
            log.error("IOException while reading from CSV file", ioe);
            return null;
        }
        
        return row;
    }
    
    public List<Map<String,String>> readRows() {
        List<Map<String,String>> rows = new ArrayList<Map<String,String>>();
        
        Map<String,String> row;
        while((row = readRow()) != null)
            rows.add(row);
        
        return rows;
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
            StringBuilder sb = new StringBuilder("did not contain required columns: ");
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

