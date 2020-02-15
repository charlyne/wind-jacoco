/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.diffhelper;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class DiffHelper {
    //diffreport会反射修改diffFile,diffFilePath可以是文件名或者是diffmethodslist字符串
    private static final String diffFilePath=new String("initvalue");
    //存储class类和对应的diffmethods
    final private HashMap<String,ArrayList> classMethods;

    public DiffHelper(){
        this.classMethods = getclassMethodsMapper(diffFilePath);

    }

    public boolean isDiffFileExists(){
        //表面diffFilePath值没有被改变过，或者获取difffile路径为空，所以走的是全量覆盖率
        if(diffFilePath==null||diffFilePath.equals("initvalue")||diffFilePath.equals("")||classMethods.isEmpty()){
            return false;
        }
        else return true;

    }
    public String getDiffFile(){
        return diffFilePath;
    }

    public static HashMap<String,ArrayList> getclassMethodsMapper(String file) {
        HashMap<String, ArrayList> diffMap = new HashMap<String, ArrayList>();
        if (file==null||file.equals("initvalue")||file.equals("")) {
            //if语句必不可少,不然第一次initvalue执行if外的语句会抛一次异常java.io.FileNotFoundException
            return diffMap;
        }

        // 传入的是medthodlist  class的分隔符%，classname1:method1,method2%classname2:method11,method22,method33
        //传入的是classname1:methodname1,parameter1,parameter2#methodname2%classname2.....
        String[] classlist = file.split("%");
        for (int i = 0; i < classlist.length; i++) {
            String[] tmps = classlist[i].split(":");
            String classname = tmps[0];
            String methodstr = tmps[1];
            ArrayList<String> list = new ArrayList<String>();
            list = new ArrayList<String>(Arrays.asList(methodstr.split("#")));
            diffMap.put(classname, list);
        }
        return diffMap;
    }
    public boolean isDiffMethod(String className,String methodName,String methodDesc){
        for(String key:classMethods.keySet()){
            // TODO: 2020/2/14 这里endsWith是否有待商榷
            if(key.endsWith(className)){
                ArrayList<String> methodList=classMethods.get(key);
                if(methodList.get(0).equals("true")){
                    return true;
                }
                //将asm框架对方法的desc提取出来method,param1,param2
                String[] paramArr=methodDesc.split("\\)")[0].split(";");
                StringBuilder builder=new StringBuilder(methodName);
                    for(String param:paramArr){
                        String[] tt=param.split("/");
                        if(tt.length>=2){
                            builder.append(","+tt[tt.length-1]);
                        }
                    }
                    for(String method:methodList){
                        if(method.equals(builder.toString())){
                            return true;
                        }
                    }



            }
        }
        return false;
    }


    //修改final变量

    public static void modify(String fieldName, Object newFieldValue) throws Exception {

        if(!diffFilePath.equals("initvalue")){
            return;
        }
        // TODO: 2019/6/28 需不需要加一条 如果fileName不存在，就不设置diffFilePath？？？
        Class clazz=Class.forName("org.jacoco.core.diffhelper.DiffHelper");
        DiffHelper helper=(DiffHelper)clazz.newInstance();
        Field field = helper.getClass().getDeclaredField(fieldName);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        if(!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(helper, newFieldValue);
    }
    //MD5 加密工具类
    public static String getMD5Value(String dataStr) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}