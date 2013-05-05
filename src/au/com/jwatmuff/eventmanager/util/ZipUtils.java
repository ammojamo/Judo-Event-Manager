/*
 * BASED ON CODE FROM http://www.lalitmehta.com/home/wp-content/uploads/2007/09/zipfile.txt
 *
 * CHANGED
 *
 * Copyright (c) 2007 Lalit Mehta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package au.com.jwatmuff.eventmanager.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author James
 */
public class ZipUtils {

    private static final Logger log = Logger.getLogger(ZipUtils.class);

    public static void unzipFile(File destFolder, File zipFile) throws IOException {
        ZipInputStream zipStream = null;
        try {
            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }

            BufferedInputStream in = new BufferedInputStream(new FileInputStream(zipFile));
            zipStream = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = zipStream.getNextEntry()) != null) {
                // get output file
                String name = entry.getName();
                if(name.startsWith("/") || name.startsWith("\\")) name = name.substring(1);
                File file = new File(destFolder, name);
                // ensure directory exists
                File dir = file.getParentFile();
                if(!dir.exists()) dir.mkdirs();
                IOUtils.copy(zipStream, new FileOutputStream(file));
            }
        } finally {
            if(zipStream != null) zipStream.close();
        }
    }

    public static void zipFolder(File srcFolder, File zipFile, boolean rootFolder) throws IOException {
        ZipOutputStream out = null;
        try {
            //create ZipOutputStream object
            out = new ZipOutputStream(new FileOutputStream(zipFile));

            String baseName;
            if (rootFolder) {
                //get path prefix so that the zip file does not contain the whole path
                // eg. if folder to be zipped is /home/lalit/test
                // the zip file when opened will have test folder and not home/lalit/test folder
                int len = srcFolder.getAbsolutePath().lastIndexOf(File.separator);
                baseName = srcFolder.getAbsolutePath().substring(0, len + 1);
            } else {
                baseName = srcFolder.getAbsolutePath();
            }

            addFolderToZip(srcFolder, out, baseName);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static void addFolderToZip(File folder, ZipOutputStream zip, String baseName) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, zip, baseName);
            } else {
                String name = file.getAbsolutePath().substring(baseName.length());
                ZipEntry zipEntry = new ZipEntry(name);
                zip.putNextEntry(zipEntry);
                IOUtils.copy(new FileInputStream(file), zip);
                zip.closeEntry();
            }
        }
    }
}
