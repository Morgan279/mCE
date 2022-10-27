package edu.ecnu.aidadblab.config;

import edu.ecnu.aidadblab.constant.FuzzyLevel;
import edu.ecnu.aidadblab.constant.IndexType;

public abstract class GlobalConfig {

    public static int MAX_READ_LINE = (int) 1e6;

    public static String FUZZY_LEVEL = FuzzyLevel.EGO_NETWORK;

    public static boolean DEBUG = false;

    public static boolean ENABLE_INDEX = false;

    public static boolean TIME_RATIO_TEST = false;

    public static boolean SCALEABLE_TEST = false;

    public static String INDEX_TYPE = IndexType.BLOOM;

    public static final String REMOTE_DATASET_DIR = System.getProperty("user.dir") + "/../dataset";

    public static final String LOCAL_DATASET_DIR = System.getProperty("user.dir") + "/src/main/resources/dataset";

    public static String datasetDir = LOCAL_DATASET_DIR;

    public static String getDatasetDir() {
        return datasetDir;
    }

}
