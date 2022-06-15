package com.example.yygh.test;

import com.alibaba.fastjson.JSONObject;
import com.example.yygh.common.result.Result;
import com.example.yygh.test.util.dateFormatUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


@SpringBootApplication
public class ServiceTestApplication {
    static class Node implements Comparable<Node> {
        String name;
        long arrive;
        int id;
        // 此处arrive直接通过对时间戳处理,转换成整数处理
        private Node(String _name,long _arrive,int _id){
            name = _name;
            arrive = _arrive;
            id = _id;
        }

        @Override
        public int compareTo(Node o) {
            if(this.id > o.id) return 1;
            else if(this.id < o.id) return -1;
            else {
                return 0;
            }
        }
    }
    static int id = 1;
    static int special_id = 1;
    static long t = Long.MAX_VALUE; // t直接使用long类型的时间戳
    static Map<String,Integer> map = new HashMap<String,Integer>();
    static String now = ""; // 表示当前正在处理哪一个
    static boolean flag = false;

    // 表示是否决定从队列继续处理
    static boolean ok = false;
    static final ReentrantLock doLock = new ReentrantLock();
    static final Condition canDo = doLock.newCondition();

    // 用于迟到直接往后顺延3个号
    static volatile ReentrantLock  delayLock = new ReentrantLock();
    static volatile int delayNum = -1;   // 表示初始化没有人是delay的
    static volatile Condition delayDo = delayLock.newCondition();

    static class SyncQueue{
        private static final int CAPACITY = 10;
        private final LinkedList<Node> queue = new LinkedList<Node>();

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition bufferNotFull = lock.newCondition();
        private final Condition bufferNotEmpty = lock.newCondition();

        private boolean Empty(){
            lock.lock();
            try{
                return queue.isEmpty();
            }finally {
                lock.unlock();
            }
        }

        private boolean Full(){
            lock.lock();
            try{
                return queue.size() == CAPACITY;
            }finally {
                lock.unlock();
            }
        }
        private int getNumber(String name){
            lock.lock();
            try {
                if(flag && now.equals(name)) return 0; // 表示正在处理
                if(Empty()) return -2;
                int begin;
                for(begin = 0;begin < queue.size();begin ++){
                    if(Objects.equals(queue.get(begin).name, name)){
                        break;
                    }
                }
                if(begin == queue.size()) return -2; // 表示已经完成
                return begin + (flag ? 1 : 0);
            }finally {
                lock.unlock();
            }
        }

        public int get_whole(){
            lock.lock();
            try {
                return queue.size();
            }finally {
                lock.unlock();
            }
        }

        public void put(String name,long arrive,int id) throws InterruptedException {
            lock.lock();
            try {
                while (Full()) {
                    System.out.println("Buffer is full, waiting");
                    bufferNotEmpty.await();
                }
                Node node = new Node(name,arrive,id);
                // insert into suitable position
                queue.add(node);
                bufferNotFull.signal();

            } finally {
                lock.unlock();
            }
        }
        // 每次对arrive time <= t的队列中根据id进行排序
        public void sort() throws InterruptedException{
            lock.lock();
            try {
                // find the segment
                // deal with the segment [0,index - 1]
                LinkedList<Node> temp = new LinkedList<Node>();
                int index = 0;
                while(index < queue.size() && queue.get(index).arrive <= t) {
                    Node node = queue.get(index);
                    temp.add(node);
                    index ++;
                }
                Collections.sort(temp);
                if (index > 0) {
                    queue.subList(0, index).clear();
                }
                for(int i = index - 1;i >= 0;i --){
                    queue.add(0,temp.get(i));
                }
            }finally {
                lock.unlock();
            }
        }
        //  get the head element of the queue
        public Node get() throws InterruptedException {
            lock.lock();
            try {
                while (Empty()) {
                    System.out.println("Buffer is empty, waiting");
                    bufferNotFull.await();
                }
                Node node;
                node = queue.get(0);
                queue.remove(0);
                bufferNotEmpty.signal();
                return node;
            } finally {
                lock.unlock();
            }
        }
    }

    static class Producer implements Runnable {

        private SyncQueue producerConsumer;
        private Node node;
        // 表示当前用户是正常时间到达,早到,晚到
        // 0,1,2
        private int type;

        public Producer(SyncQueue producerConsumer,Node node,int type) {
            this.producerConsumer = producerConsumer;
            this.node = node;
            this.type = type;
        }

        @Override
        public void run() {
            try {
                // 此处需要对晚到的特殊处理，也就是延迟加入到队列中
                if(type == 0)
                    producerConsumer.put(node.name,node.arrive,node.id);
                else if(type == 1){
                    // 记录其当前已经处理了几个元素,等到处理个数达到延后限制,再开始插入
                    delayLock.lock();
                    try {
                        delayNum = 2;
                        while(delayNum != 0){
                            System.out.println("delay is block");
                            delayDo.await();
                        }
                        System.out.println("delay wake up");
                        producerConsumer.put(node.name,node.arrive,node.id);
                        delayNum = -1;
                    }finally {
                        delayLock.unlock();
                    }
                }else{

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer implements Runnable {

        private SyncQueue producerConsumer;
        private SyncQueue specialProducerConsumer;

        public Consumer(SyncQueue producerConsumer,SyncQueue specialProducerConsumer) {
            this.producerConsumer = producerConsumer;
            this.specialProducerConsumer = specialProducerConsumer;
        }

        @Override
        public void run() {
            try {
                while(true){
                    // 每次优先处理special队列中元素
                    while(!specialProducerConsumer.Empty() || !producerConsumer.Empty()){
                        doLock.lock();
                        try {
                            while(!ok) {
                                canDo.await();
                            }
                        }finally {
                            doLock.unlock();
                        }

                        if(!specialProducerConsumer.Empty()){
                            System.out.println("special block");

                            System.out.println("I am out");
                            flag = true;
                            Node node = specialProducerConsumer.get();
                            now = node.name;
                            long begin = System.currentTimeMillis();
                            // 需要进行阻塞
                            System.out.println(dateFormatUtil.timeToFormat(begin));
                            System.out.println("we can visit node name " + now);
                            doLock.lock();
                            try {
                                while(ok) {
                                    canDo.await();
                                }
                            }finally {
                                doLock.unlock();
                            }

                            long end = System.currentTimeMillis();
                            t = end;
                            System.out.println(dateFormatUtil.timeToFormat(end));
                            System.out.println("consume " + node.name + " from " + begin + " to " + end);

                            flag = false;
                            now = "";

                            // 处理完如果需要修改当前已经处理的delayNum
                            delayLock.lock();
                            try {
                                if(delayNum > 0){
                                    delayNum --;
                                    if(delayNum == 0){
                                        delayDo.signal();
                                    }
                                }
                            }finally {
                                delayLock.unlock();
                            }
                            continue;
                        }
                        if(!producerConsumer.Empty()){
                            System.out.println("block");

                            System.out.println("I am out");
                            producerConsumer.sort();
                            flag = true;
                            Node node = producerConsumer.get();
                            now = node.name;
                            long begin = System.currentTimeMillis();
                            // 需要进行阻塞
                            System.out.println(dateFormatUtil.timeToFormat(begin));
                            System.out.println("we can visit node name" + now);
                            doLock.lock();
                            try {
                                while(ok) {
                                    canDo.await();
                                }
                            }finally {
                                doLock.unlock();
                            }

                            long end = System.currentTimeMillis();
                            t = end;
                            System.out.println(dateFormatUtil.timeToFormat(end));
                            System.out.println("consume " + node.name + " from " + begin + " to " + end);

                            flag = false;
                            now = "";
                            // 处理完如果需要修改当前已经处理的delayNum
                            delayLock.lock();
                            try {
                                if(delayNum > 0){
                                    delayNum --;
                                    if(delayNum == 0){
                                        delayDo.signal();
                                    }
                                }
                            }finally {
                                delayLock.unlock();
                            }
                        }
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    static SyncQueue syncQueue = new SyncQueue();
    static SyncQueue specialSyncQueue = new SyncQueue();

    @RestController
    public static class AsyncController {
        @Autowired
        JdbcTemplate jdbcTemplate;

        // 预约顺序
        @GetMapping("/get/begin/{name}")
        public Result do1(@PathVariable String name) throws InterruptedException{
            System.out.println("normal" + name);
            map.put(name,id);
            id ++;
            JSONObject map = new JSONObject();
            map.put("qid", id - 1);
            return Result.ok(map);
        }

        // 取号顺序
        @GetMapping("/get/mid/{name}/{arrive}")
        public Result do2(@PathVariable String name, @PathVariable long arrive) throws InterruptedException {
            String sql = "insert into get_info (name,arrive_time) values (?,?)";
            Object[] objects = new Object[2];
            objects[0] = name;
            objects[1] = arrive;
//            jdbcTemplate.update(sql,objects);
            Node node = new Node(name,arrive,map.get(name));
            Thread producer = new Thread(new Producer(syncQueue,node,0));
            producer.start();
            producer.join();
            return Result.ok(1);
        }
        // 询问前面有多少人,此处将优先队列的因素也加进去了
        @GetMapping("/get/end/{name}")
        public Result do3(@PathVariable String name) throws InterruptedException{
            syncQueue.sort();
            int num = syncQueue.getNumber(name);
            int special_num = specialSyncQueue.get_whole();
            num += special_num;
            if(num == 0 && Objects.equals(now, "")){
                num = -1;
            }
            JSONObject map = new JSONObject();
            map.put("num", num);
            return Result.ok(map);
        }
        // 询问当前队列中有多少人
        @GetMapping("/get/whole")
        public Result do4() throws InterruptedException{
            JSONObject map = new JSONObject();
            map.put("whole", syncQueue.get_whole() + specialSyncQueue.get_whole());
            return Result.ok(map);
        }
        @GetMapping("/get/modify")
        public Result do5() throws InterruptedException{
            doLock.lock();
            try {
                ok = !ok;
                canDo.signal();
            }finally {
                doLock.unlock();
            }
            JSONObject map = new JSONObject();
            return Result.ok(map);
        }
        @GetMapping("/get/now")
        public Result do6() throws InterruptedException{
            JSONObject map = new JSONObject();
            map.put("now",now);
            return Result.ok(map);
        }

        // special不需要进行排序直接插入special_queue
        @GetMapping("/get/special/{name}")
        public Result do7(@PathVariable String name) throws InterruptedException{
            System.out.println("special " + name);
            map.put(name,special_id);
            special_id ++;
            Node node = new Node(name,0,map.get(name));
            Thread producer = new Thread(new Producer(specialSyncQueue,node,0));
            producer.start();
            producer.join();

            JSONObject map = new JSONObject();
            map.put("special_qid",special_id - 1);
            return Result.ok(map);
        }

        // 获取当前name在special队列中的长度
        @GetMapping("/get/special/size/{name}")
        public Result do8(@PathVariable String name) throws InterruptedException{
            JSONObject map = new JSONObject();
            int num = specialSyncQueue.getNumber(name);
            if(num == 0 && Objects.equals(now, "")){
                num = -1;
            }
            map.put("special_size",num);
            return Result.ok(map);
        }

        // 暂时的延迟测试方法，其中对于当前用户依然需要提前进行预约挂号，此处只是取号时间相对于规定时间迟到
        @GetMapping("/get/delay/{name}/{arrive}")
        public Result do9(@PathVariable String name,@PathVariable long arrive) throws InterruptedException{
            Node node = new Node(name,arrive,map.get(name));
            Thread producer = new Thread(new Producer(syncQueue,node,1));
            producer.start();
            return Result.ok(1);
        }
        // TODO 需要对arrive时间进行修改,修改为其所预约时间段的开始
        @GetMapping("/get/early/{name}{arrive}")
        public Result do10(@PathVariable String name,@PathVariable long arrive) throws InterruptedException{
            Node node = new Node(name,arrive,map.get(name));
            Thread producer = new Thread(new Producer(syncQueue,node,1));
            producer.start();
            return Result.ok(1);
        }
    }

    // 传递一个name arrive进行排序取号
    // 每次传递name查询当前位置
    public static void main(String[] args) {
        SpringApplication.run(ServiceTestApplication.class, args);
        Thread consumer = new Thread(new Consumer(syncQueue,specialSyncQueue));
        consumer.start();
    }
}
