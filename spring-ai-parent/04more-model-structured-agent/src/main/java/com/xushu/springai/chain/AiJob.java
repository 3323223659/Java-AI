package com.xushu.springai.chain;

import java.util.Map;

public class AiJob {
     record Job(JobType jobType, Map<String,String> keyInfos) {
    }

    public enum JobType{
        CANCEL,
        QUERY,
        OTHER,
    }
}