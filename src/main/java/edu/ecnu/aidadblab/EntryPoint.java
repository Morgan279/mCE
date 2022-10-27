package edu.ecnu.aidadblab;

import edu.ecnu.aidadblab.config.GlobalConfig;
import edu.ecnu.aidadblab.tool.ExperimentExecutor;

public class EntryPoint {

    public static void main(String[] args) {
        GlobalConfig.datasetDir = GlobalConfig.REMOTE_DATASET_DIR;
        ExperimentExecutor experimentExecutor = new ExperimentExecutor();
        experimentExecutor.fuzzyLevelTest();
    }


}
