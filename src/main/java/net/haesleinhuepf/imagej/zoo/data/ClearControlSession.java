package net.haesleinhuepf.imagej.zoo.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.FileHandler;

public class ClearControlSession {
    private String path;
    private String [] datasetnames = null;
    private ClearControlDataSet[] dataSets = null;

    public ClearControlSession(String path) {
        this.path = path;
        this.path = this.path.replace("\\","/");
        if (!this.path.endsWith("/")){
            this.path = this.path + "/";
        }
    }

    public String[] getDataSetNames() {
        if (datasetnames == null) {
            File folder = new File(path);

            ArrayList<String> names = new ArrayList<>();

            for (File file : folder.listFiles()) {
                if (!file.isDirectory()) {
                    String filename = file.getName();
                    //System.out.println("Checking " + filename);
                    if (filename.endsWith("index.txt")) {
                        //System.out.println("Checking2 " + filename);

                        String otherFilename = filename.replace("index.txt", "metadata.txt");
                        if (new File(path + otherFilename).exists()) {
                            //System.out.println("Checking3 " + filename);

                            names.add(filename.replace(".index.txt", ""));
                        }
                    }
                }
            }
            Collections.sort(names);

            datasetnames = new String[names.size()];
            if (names.size() > 0) {
                names.toArray(datasetnames);
            }

            dataSets = new ClearControlDataSet[names.size()];
            for (int i = 0; i < datasetnames.length; i++) {
                dataSets[i] = null;
            }
        }
        return datasetnames;
    }

    public ClearControlDataSet getDataSet(String name) {
        for (int i = 0; i < datasetnames.length; i++) {
            if (datasetnames[i].compareTo(name) == 0) {
                if (dataSets[i] == null) {
                    dataSets[i] = new ClearControlDataSet(path, name);
                }
                return dataSets[i];
            }
        }
        return null;
    }

    public String getName() {
        return new File(path).getName();
    }

    public String getPath() {
        return path;
    }
}
