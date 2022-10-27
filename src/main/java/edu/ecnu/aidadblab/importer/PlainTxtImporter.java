package edu.ecnu.aidadblab.importer;

import cn.hutool.core.io.FileUtil;
import edu.ecnu.aidadblab.config.GlobalConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlainTxtImporter {

    private String inputDir;

    public PlainTxtImporter(String inputDir) {
        this.inputDir = inputDir;
    }

    public List<String> readLine(String filePath) {
        InputStream inputStream = FileUtil.getInputStream(this.inputDir + filePath);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        List<String> res = new ArrayList<>();
        String text;
        int cnt = 0;

        try {
            while ((text = bufferedReader.readLine()) != null) {
                ++cnt;
                res.add(text);
                if (cnt == GlobalConfig.MAX_READ_LINE) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return res;
    }

}
