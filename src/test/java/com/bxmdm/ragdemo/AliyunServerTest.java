package com.bxmdm.ragdemo;

import cn.hutool.http.HttpRequest;
import com.bxmdm.ragdemo.utils.Sender;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

public class AliyunServerTest {
    @Test
    public void testTranslate() throws Exception {
        // 这里可以添加测试代码来验证阿里云服务器的翻译功能
        String serviceURL = "http://mt.cn-hangzhou.aliyuncs.com/api/translate/web/general";
        String accessKeyId = "";// 使用您的阿里云访问密钥 AccessKeyId
        String accessKeySecret = ""; // 使用您的阿里云访问密钥
        String postBody = "{\n" +
                " \"FormatType\": \"text\",\n" +
                " \"SourceLanguage\": \"auto\",\n" +
                " \"TargetLanguage\": \"en\",\n" +
                " \"SourceText\": \"摄像头无法连接怎么办\",\n" +
                " \"Scene\": \"general\"\n" +
                "}";
        // Sender代码请参考帮助文档“签名方法”
        String result =  Sender.sendPost(serviceURL, postBody, accessKeyId, accessKeySecret);
        System.out.println(result);
    }
}
