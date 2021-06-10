package org.jeecg.modules.business.service;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.modules.online.cgform.enhance.CgformEnhanceJavaInter;
import org.jeecg.modules.online.cgform.mapper.OnlCgformFieldMapper;
import org.jeecg.modules.online.config.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

public class NewButtonEnhanceJava implements CgformEnhanceJavaInter  {
    @Override
    public int execute(String s, Map<String, Object> map) throws BusinessException {
        return 0;
    }

    @Override
    public int execute(String s, JSONObject jsonObject) throws BusinessException {
        // TODO:
        System.out.println("=================click button test enhance java");
        System.out.println("table name: " + s);
        System.out.println("json: " + jsonObject.toString());

        // TODO:
        // get mapper
        OnlCgformFieldMapper onlCgformFieldMapper = SpringContextUtils.getBean(OnlCgformFieldMapper.class);
        // set params
        Map<String, Object> params = new HashMap<>();
        String sql = "update " + s + " set request_id = #{request_id, jdbcType=VARCHAR} where id = #{id, jdbcType=VARCHAR}";
        params.put("execute_sql_string", sql);
        params.put("request_id", jsonObject.getString("id"));
        params.put("id", jsonObject.getString("id"));
        // execute sql
        onlCgformFieldMapper.executeUpdatetSQL(params);

        System.out.println("=================================end");



        return 0;
    }
}
