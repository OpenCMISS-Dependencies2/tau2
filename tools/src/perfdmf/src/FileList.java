/* 
 FileList.java

 Title:      ParaProf
 Author:     Robert Bell
 Description:  Some useful functions for the system.
 */

package edu.uoregon.tau.perfdmf;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

class ProfileFileFilter implements FilenameFilter {

    String prefix;

    public ProfileFileFilter(String prefix) {
        this.prefix = prefix;
    }

    public boolean accept(File okplace, String name) {
        if (name.startsWith(prefix + ".")) {

            // try to parse into n,c,t, if it craps out, it must not be a valid name
            try {
                String nctPart = name.substring(name.indexOf(".") + 1);
                String n = nctPart.substring(0, nctPart.indexOf("."));
                String c = nctPart.substring(nctPart.indexOf(".") + 1, nctPart.lastIndexOf("."));
                String t = nctPart.substring(nctPart.lastIndexOf(".") + 1);

                int testInt = Integer.parseInt(n);
                if (testInt < 0)
                    return false;
                testInt = Integer.parseInt(c);
                if (testInt < 0)
                    return false;
                testInt = Integer.parseInt(t);
                if (testInt < 0)
                    return false;

                return true;
            } catch (Exception e) {
                return false;
            }

        }

        return false;
    }
}

class MultiFileFilter implements FilenameFilter {
    public MultiFileFilter() {
    }

    public boolean accept(File okplace, String name) {
        if (name.startsWith("MULTI_")) {
            return true;
        }
        return false;
    }
}

public class FileList {


    public List helperFindProfilesPrefix(String path, String prefix) {

        //String prefix = "\\Aprofile\\..*\\..*\\..*\\z";
        List v = new ArrayList();

        File file = new File(path);
        if (file.isDirectory() == false) {
            return v;
        }
        //FilenameFilter prefixFilter = new FileFilter(prefix);
        FilenameFilter prefixFilter = new ProfileFileFilter(prefix);
        File files[] = file.listFiles(prefixFilter);

        if (files.length == 0) {
            // we didn't find any profile files here, now look for MULTI_ directories
            //FilenameFilter multiFilter = new FileFilter("MULTI__.*");
            FilenameFilter multiFilter = new MultiFileFilter();
            File multiDirs[] = file.listFiles(multiFilter);

            for (int i = 0; i < multiDirs.length; i++) {
                File finalFiles[] = multiDirs[i].listFiles(prefixFilter);
                v.add(finalFiles);
            }
        } else {
            v.add(files);
            return v;
        }
        
        
        
        return v;
    }

    public List helperFindProfiles(String path) {

        List v = helperFindProfilesPrefix(path, "profile");
        if (v.size() == 0) {
            v = helperFindProfilesPrefix(path, "dump");
        }
        return v;
    }

    public static String getPathReverse(String string) {
        String fileSeparator = System.getProperty("file.separator");
        String reverse = "";

        StringTokenizer st = new StringTokenizer(string, fileSeparator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            reverse = token + fileSeparator + reverse;
        }
        return reverse;
    }

}
