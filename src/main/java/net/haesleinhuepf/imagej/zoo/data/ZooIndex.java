package net.haesleinhuepf.imagej.zoo.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ZooIndex {
    private String path;
    private String [] sessionnames = null;
    private ClearControlSession[] seession = null;

    public ZooIndex(String path) {
        this.path = path;
        this.path = this.path.replace("\\","/");
        if (!this.path.endsWith("/")){
            this.path = this.path + "/";
        }
    }

    public String[] getSessionNames() {
        if (sessionnames == null) {
            File folder = new File(path);

            ArrayList<String> names = new ArrayList<>();

            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    String subfoldername = file.getName();
                    //System.out.println("Checking " + subfoldername);
                    if (new ClearControlSession(path + subfoldername).getDataSetNames().length > 0) {
                        //System.out.println("Adding " + subfoldername);
                        names.add(subfoldername);
                    }
                }
            }
            Collections.sort(names);

            sessionnames = new String[names.size()];
            if (names.size() > 0) {
                names.toArray(sessionnames);
            }

            seession = new ClearControlSession[names.size()];
            for (int i = 0; i < sessionnames.length; i++) {
                seession[i] = new ClearControlSession(path + sessionnames[i]);
            }
        }
        return sessionnames;
    }

    public ClearControlSession getSession(String name) {
        for (int i = 0; i < sessionnames.length; i++) {
            if (sessionnames[i].compareTo(name) == 0) {
                return seession[i];
            }
        }
        return null;
    }

}
