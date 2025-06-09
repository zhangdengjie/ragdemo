package com.bxmdm.ragdemo.bean;

public class TranslateResult {
        // json字符串 {"RequestId":"0EE94426-2881-5B17-BE71-39147AC81FBF","Data":{"WordCount":"10","Translated":"What if the camera cannot be connected?"},"Code":"200"}
        private String RequestId;
        private Data Data;
        private String Code;

    public TranslateResult.Data getData() {
        return Data;
    }

    public void setData(TranslateResult.Data data) {
        Data = data;
    }

    public static class Data {
            private String WordCount;
            private String Translated;
            public String getWordCount() {
                return WordCount;
            }

            public void setWordCount(String wordCount) {
                WordCount = wordCount;
            }

            public String getTranslated() {
                return Translated;
            }

            public void setTranslated(String translated) {
                Translated = translated;
            }
        }
    }