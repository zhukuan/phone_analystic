/**
 * Copyright (C), 2015-2018, XXX有限公司
 * FileName: NewUserReducer
 * Author:   14751
 * Date:     2018/9/20 17:38
 * Description:
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间         版本号            描述
 */
package com.phone.analystic.mr.nm;

import com.phone.analystic.modle.StatsUserDimension;
import com.phone.analystic.modle.value.map.TimeOutputValue;
import com.phone.analystic.modle.value.reduce.OutputWritable;
import com.phone.common.KpiType;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * 新增会员的reducer类
 */
public class NewMemberReducer extends Reducer<StatsUserDimension,TimeOutputValue,StatsUserDimension,OutputWritable> {
    private static final Logger logger = Logger.getLogger(NewMemberReducer.class);
    private OutputWritable v = new OutputWritable();
    private Set<String> unique = new HashSet();//用于去重，利用HashSet
    private MapWritable map = new MapWritable();
    private Map<String,List<Long>> li = new HashMap<String,List<Long>>();

    @Override
    protected void reduce(StatsUserDimension key, Iterable<TimeOutputValue> values, Context context) throws IOException, InterruptedException {
        map.clear();//清空map，因为map是在外面定义的，每一个key都需要调用一次reduce方法，也就是说上次操作会保留map中的key-value

        for(TimeOutputValue tv : values){//循环
            /*this.unique.add(tv.getId());//将uuid取出添加到set中进行去重操作
            this.li.add(tv.getTime());*/
            if(li.containsKey(tv.getId())){
                li.get(tv.getId()).add(tv.getTime());
            } else {
                List<Long> list = new ArrayList<Long>();
                list.add(tv.getTime());
                li.put(tv.getId(),list);
            }
        }

        //循环输出  用于插入到member_info表中
        for (Map.Entry<String,List<Long>> en:li.entrySet()){
            this.v.setKpi(KpiType.MEMBER_INFO);
            this.map.put(new IntWritable(-2),new Text(en.getKey()));
            Collections.sort(en.getValue());
            this.map.put(new IntWritable(-3),new LongWritable(en.getValue().get(0)));
            this.v.setValue(this.map);
            context.write(key,this.v);
        }

        //构造输出的value
        //根据kpi别名获取kpi类型（比较灵活） --- 第一种方法
        this.v.setKpi(KpiType.valueOfKpiName(key.getStatsCommonDimension().getKpiDimension().getKpiName()));

        //通过集合的size统计新增用户uuid的个数，前面的key可以随便设置，就是用来标识新增用户个数的（比较难理解）
        this.map.put(new IntWritable(-1),new IntWritable(this.li.size()));
        this.v.setValue(this.map);
        //输出
        context.write(key,this.v);
        this.unique.clear();//清空操作
    }
}