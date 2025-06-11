package com.bxmdm.ragdemo.service;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import com.bxmdm.ragdemo.bean.TranslateResult;
import com.bxmdm.ragdemo.utils.Sender;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;

@Service
public class AliService {
    public String translate(String source) {
        // 这里可以添加测试代码来验证阿里云服务器的翻译功能
        String serviceURL = "http://mt.cn-hangzhou.aliyuncs.com/api/translate/web/general";
        JsonObject gsonObject = new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("aliyun.json")), JsonObject.class);
        String accessKeyId = gsonObject.get("key").getAsString();
        String accessKeySecret = gsonObject.get("secret").getAsString();
        String postBody = "{\n" +
                " \"FormatType\": \"text\",\n" +
                " \"SourceLanguage\": \"auto\",\n" +
                " \"TargetLanguage\": \"en\",\n" +
                " \"SourceText\": \"" + source + "\",\n" +
                " \"Scene\": \"general\"\n" +
                "}";
        // Sender代码请参考帮助文档“签名方法”
        String result =  Sender.sendPost(serviceURL, postBody, accessKeyId, accessKeySecret);
        System.out.println(result);
        JSON json = new JSONObject(result);
        TranslateResult bean = json.toBean(TranslateResult.class);
        return bean.getData().getTranslated();
    }
}
