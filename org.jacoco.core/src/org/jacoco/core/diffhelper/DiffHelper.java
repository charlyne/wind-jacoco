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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class DiffHelper {
    //diffreport会反射修改diffFile,diffFilePath可以是文件名或者是diffmethodslist字符串
    private static final String diffFilePath=new String("initvalue");
    //存储class类和对应的diffmethods
    final private HashMap<String,ArrayList> map;

    public DiffHelper(){
            this.map = getMapper(diffFilePath);

    }

    public boolean isDiffFileExists(){
        //表面diffFilePath值没有被改变过，或者获取difffile路径为空，所以走的是全量覆盖率
        if(diffFilePath==null||diffFilePath.equals("initvalue")||diffFilePath.equals("")||map.isEmpty()){
            return false;
        }
        else return true;

    }
    public String getDiffFilePath(){
        return diffFilePath;
    }

    public static HashMap<String,ArrayList> getMapper(String file) {
        HashMap<String, ArrayList> map = new HashMap<String, ArrayList>();

        if (file==null||file.equals("initvalue")||file.equals("")) {
            //if语句必不可少,不然第一次initvalue执行if外的语句会抛一次异常java.io.FileNotFoundException
            return map;
        }
        File filename = new File(diffFilePath);
        if (filename.exists()) {
            //传入的是文件名字
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(diffFilePath);
                BufferedReader in = new BufferedReader(fileReader);
                String str;
                while ((str = in.readLine()) != null) {
                    //这块逻辑为什么写的这么麻烦
                    String[] tmps = str.split(":");
                    String classname = tmps[0];
                    String methodstr = tmps[1];
                    if (tmps.length == 2) {
                        ArrayList<String> list = new ArrayList<String>();
                        list = new ArrayList<String>(Arrays.asList(methodstr.split(",")));
                        map.put(classname, list);
                    } else {
                        return map;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fileReader != null)
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            return map;
        } else {
            //传入的是medthodlist  class的分隔符%，classname1:method1,method2%classname2:method11,method22,method33
            String[] classlist = file.split("%");
            for (int i = 0; i < classlist.length; i++) {
                String[] tmps = classlist[i].split(":");
                String classname = tmps[0];
                String methodstr = tmps[1];
                if (tmps.length == 2) {
                    ArrayList<String> list = new ArrayList<String>();
                    list = new ArrayList<String>(Arrays.asList(methodstr.split(",")));
                    map.put(classname, list);
                }

            }

        }
        return map;
    }
    //这里因为key和classname的路径可能不一样,所以不能直接map.get(className)
    // TODO: 2019/8/30 优化一下，如何是add类型，应该直接返回true，不直接调用getDiffMethod 
    // TODO: 2019/8/30  diffhelper中应该是classname：类型是add直接返回true；、
    // TODO: 2019/8/30 如果是modify就使用asm解析的class文件，进行比较 
    public ArrayList getDiffMethod(String className){
        for(String key:map.keySet()){
            if(key.endsWith(className)){
                return map.get(key);
            }else
                continue;
        }
        return null;
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

}