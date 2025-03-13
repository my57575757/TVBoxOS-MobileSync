package com.github.tvbox.osc.util;

import android.os.StrictMode;

import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.VodCollect;
import com.github.tvbox.osc.cache.VodRecord;
import com.github.tvbox.osc.data.AppDataManager;
import com.google.android.exoplayer2.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.orhanobut.hawk.Hawk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncUtil {
    public static List<VodCollect> getCollectAll(){
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //远端获取Collect
        String user = Hawk.get(HawkConfig.SYNC_USER, null);
        String url = Hawk.get(HawkConfig.SYNC_URL, null);
        Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
            @Override
            public void write(JsonWriter out, Long value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public Long read(JsonReader in) throws IOException {
                return in.nextLong();
            }
        }).create();

        Map param = new HashMap<>();
        param.put("userName",user);
        String res = post(url + "/tvBox/getCollectAll", param);

        List<VodCollect> recordList = new ArrayList<>();
        if (res!=null&&!"".equals(res)){
            VodCollect[] array = gson.fromJson(res,VodCollect[].class);
            recordList = Arrays.asList(array);
        }else {
            recordList = AppDataManager.get().getVodCollectDao().getAll();
        }
        return recordList;
    }
    public static VodCollect getCollect(String sourceKey,String vodId){
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        VodCollect record;
        //远端获取Collect
        String user = Hawk.get(HawkConfig.SYNC_USER, null);
        String url = Hawk.get(HawkConfig.SYNC_URL, null);
        Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
            @Override
            public void write(JsonWriter out, Long value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public Long read(JsonReader in) throws IOException {
                return in.nextLong();
            }
        }).create();

        Map param = new HashMap<>();
        param.put("userName",user);
        param.put("sourceKey",sourceKey);
        param.put("vodId",vodId);
        String res = post(url + "/tvBox/getCollect", param);
        if (res!=null&&!"".equals(res)){
            record = gson.fromJson(res,VodCollect.class);
        }else {
            record = AppDataManager.get().getVodCollectDao().getVodCollect(sourceKey, vodId);
        }
        return record;
    }
    public static void delCollect(String sourceKey,String vodId){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //远端删除Collect
                String user = Hawk.get(HawkConfig.SYNC_USER, null);
                String url = Hawk.get(HawkConfig.SYNC_URL, null);
                new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
                    @Override
                    public void write(JsonWriter out, Long value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public Long read(JsonReader in) throws IOException {
                        return in.nextLong();
                    }
                }).create();
                Map param = new HashMap();
                param.put("userName",user);
                param.put("sourceKey",sourceKey);
                param.put("vodId",vodId);
                post(url+"/tvBox/delCollect",param);
            }
        }).start();
    }
    public static void addCollect(VodCollect finalRecord){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //远端增加Collect
                String user = Hawk.get(HawkConfig.SYNC_USER, null);
                String url = Hawk.get(HawkConfig.SYNC_URL, null);
                Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
                    @Override
                    public void write(JsonWriter out, Long value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public Long read(JsonReader in) throws IOException {
                        return in.nextLong();
                    }
                }).create();
                Map<String,Object> param = gson.fromJson(gson.toJson(finalRecord), Map.class);
                param.put("userName",user);
                post(url+"/tvBox/addCollect",param);
            }
        }).start();
    }
    public static List<VodRecord> getAllVodRecord(int limit){
        //远端获取vod记录
        String user = Hawk.get(HawkConfig.SYNC_USER, null);
        String url = Hawk.get(HawkConfig.SYNC_URL, null);
        Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
            @Override
            public void write(JsonWriter out, Long value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public Long read(JsonReader in) throws IOException {
                return in.nextLong();
            }
        }).create();

        Map param = new HashMap<>();
        param.put("userName",user);
        param.put("limit",String.valueOf(limit));
        String res = post(url + "/tvBox/getRecordAll", param);

        List<VodRecord> recordList;
        if (res!=null&&!"".equals(res)){
            VodRecord[] array = gson.fromJson(res,VodRecord[].class);
            recordList = Arrays.asList(array);
        }else {
            int count = AppDataManager.get().getVodRecordDao().getCount();
            Integer index = Hawk.get(HawkConfig.HISTORY_NUM, 0);
            Integer hisNum = HistoryHelper.getHisNum(index);
            if ( count > hisNum ) {
                AppDataManager.get().getVodRecordDao().reserver(hisNum);
            }
            recordList = AppDataManager.get().getVodRecordDao().getAll(limit);
        }
        return recordList;
    }
    public static void deleteVodRecord(String sourceKey, VodInfo vodInfo){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //远端删除vod记录
                String user = Hawk.get(HawkConfig.SYNC_USER, null);
                String url = Hawk.get(HawkConfig.SYNC_URL, null);
                Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
                    @Override
                    public void write(JsonWriter out, Long value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public Long read(JsonReader in) throws IOException {
                        return in.nextLong();
                    }
                }).create();
                Map param = new HashMap();
                param.put("userName",user);
                param.put("sourceKey",sourceKey);
                param.put("vodId",vodInfo.id);
                post(url+"/tvBox/delRecord",param);
            }
        }).start();
    }
    public static VodRecord getRecord(String sourceKey, String vodId){
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //远端获取vod记录
        String user = Hawk.get(HawkConfig.SYNC_USER, null);
        String url = Hawk.get(HawkConfig.SYNC_URL, null);
        Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
            @Override
            public void write(JsonWriter out, Long value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public Long read(JsonReader in) throws IOException {
                return in.nextLong();
            }
        }).create();
        Map param = new HashMap<>();
        param.put("userName",user);
        param.put("sourceKey",sourceKey);
        param.put("vodId",vodId);
        String res = post(url + "/tvBox/getRecord", param);

        VodRecord record = AppDataManager.get().getVodRecordDao().getVodRecord(sourceKey, vodId);
        if (res!=null&&!"".equals(res)){
            record = gson.fromJson(res, VodRecord.class);
        }
        return record;
    }
    public static void addRecord(VodRecord finalRecord){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //远端插入vod记录
                String user = Hawk.get(HawkConfig.SYNC_USER, null);
                String url = Hawk.get(HawkConfig.SYNC_URL, null);
                Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
                    @Override
                    public void write(JsonWriter out, Long value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public Long read(JsonReader in) throws IOException {
                        return in.nextLong();
                    }
                }).create();
                Map<String,Object> param = gson.fromJson(gson.toJson(finalRecord), Map.class);
                param.put("userName",user);
                post(url+"/tvBox/addRecord",param);
            }
        }).start();
    }
    public static Long getCache(String key){
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        Long returnVal = null;
        try {
            String user = Hawk.get(HawkConfig.SYNC_USER, null);
            String url = Hawk.get(HawkConfig.SYNC_URL, null);
            if (url!=null && user!=null){
                //调用远端获取进度
                Map param = new HashMap();
                param.put("userName",user);
                param.put("key",key);
                String res = post(url+"/tvBox/getCache",param);
                if (res!=null&&!"".equals(res)){
                    Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
                        @Override
                        public void write(JsonWriter out, Long value) throws IOException {
                            out.value(value.toString());
                        }

                        @Override
                        public Long read(JsonReader in) throws IOException {
                            return in.nextLong();
                        }
                    }).create();
                    Map<String,String> map = gson.fromJson(res, Map.class);
                    if (map.get("data")!=null){
                        returnVal = Long.parseLong(map.get("data").toString());
                    }
                }
            }
        }catch (Exception e){

        }
        return returnVal;
    }
    public static <T> void deleteSync(String key,T body){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String user = Hawk.get(HawkConfig.SYNC_USER, null);
                    String url = Hawk.get(HawkConfig.SYNC_URL, null);
                    if (body!=null && url!=null && user!=null){
                        Map<String,Object> param = new HashMap<>();
                        param.put("key",key);
                        param.put("userName",user);
                        //调用远端保存进度
                        post(url+"/tvBox/delCache",param);
                    }
                }catch (Exception e){

                }
            }
        }).start();
    }

    public static <T> void saveSync(String key,T body){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String user = Hawk.get(HawkConfig.SYNC_USER, null);
                    String url = Hawk.get(HawkConfig.SYNC_URL, null);
                    if (body!=null && url!=null && user!=null){
                        Map<String,Object> param = new HashMap<>();
                        param.put("key",key);
                        param.put("userName",user);
                        param.put("data",body.toString());
                        //调用远端保存进度
                        post(url+"/tvBox/addCache",param);
                    }
                }catch (Exception e){

                }
            }
        }).start();
    }
    private static String post(String url, Map<String,Object> param) {
        String res = null;
        HttpURLConnection conn = null;
        Gson gson = new GsonBuilder().registerTypeAdapter(Long.class, new TypeAdapter<Long>() {
            @Override
            public void write(JsonWriter out, Long value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public Long read(JsonReader in) throws IOException {
                return in.nextLong();
            }
        }).create();
        try {
            LOG.e("请求开始:"+url);
            URL urlU = new URL(url);
            conn = (HttpURLConnection) urlU.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(1000);
            // 设置请求头，如Content-Type
            conn.setRequestProperty("Content-Type", "application/json");
            String postParams = gson.toJson(param);
            LOG.e("请求参数:"+postParams);
            byte[] outputInBytes = postParams.getBytes("UTF-8");
            OutputStream os = conn.getOutputStream();
            os.write(outputInBytes);
            os.close();
            // 获取响应码
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 成功响应
                // 处理响应内容
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                res = response.toString();
            }
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter,true));
            LOG.e(e.toString());
            Log.e("TAG", "Exception occurred", e);
        }finally {
            if (conn!=null){
                conn.disconnect();
            }
        }
        LOG.e("请求返回值:"+res);
        return res;
    }

    private static String toJSONString(Map<String, Object> param) {
        StringBuffer str = new StringBuffer();
        str.append("{");
        if (param!=null && !param.isEmpty()){
            Set<Map.Entry<String, Object>> entries = param.entrySet();
            for (Map.Entry<String, Object> entry : entries) {
                str.append("\""+entry.getKey()+"\":"+"\""+entry.getValue()+"\",");
            }
            str.replace(str.length()-1,str.length(),"");
        }
        str.append("}");
        return str.toString();
    }
}
