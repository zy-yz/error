package com.zy.error.one;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * 案例:
 * 有一个含 900 个元素的 Map，现在再补充 100 个元素进去，这个补充操作由 10 个线程并发进行。
 * 开发人员误以为使用了 ConcurrentHashMap 就不会有线程安全问题，于是不加思索地写出了下面的代码：
 * 在每一个线程的代码逻辑中先通过 size 方法拿到当前元素数量，计算 ConcurrentHashMap 目前还需要
 * 补充多少元素，并在日志中输出了这个值，然后通过 putAll 方法把缺少的元素添加进去。
 * */
@Controller
@RequestMapping("/ConcurrentHashMap")
public class ConcurrentHashMapTest {

    //线程个数
    private static int THREAD_COUNT = 10;

    //总元素数量
    private static int ITEM_COUNT = 1000;

    private final Logger log = LoggerFactory.getLogger(getClass());

    //帮助方法,用来获得一个指定元素数量模拟数据的ConcurrentHashMap
    private ConcurrentHashMap<String,Long> getData(int count){
        return LongStream.rangeClosed(1,count)
                .boxed()
                .collect(Collectors.toConcurrentMap(i -> UUID.randomUUID().toString(), Function.identity(),(o1, o2) ->o1,ConcurrentHashMap::new));
    }

    @GetMapping("wrong")
    public String wrong() throws InterruptedException{
        ConcurrentHashMap<String, Long> concurrentHashMap = getData(ITEM_COUNT - 100);
        //初始化900个元素
        log.info("init size:()",concurrentHashMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        //使用线程池并发处理逻辑
        forkJoinPool.execute(() -> IntStream.rangeClosed(1,10).parallel().forEach(i ->{
            //查询多个元素
            int gap = ITEM_COUNT - concurrentHashMap.size();
            log.info("gap size:{}",gap);
            //补充元素
            concurrentHashMap.putAll(getData(gap));
        }));

        //等待所有任务完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //最后的元素个数
        log.info("finish size:{}",concurrentHashMap.size());
        return "ok";



    }
}
