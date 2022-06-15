package com.example.yygh.test;

import com.alibaba.fastjson.JSONObject;
import com.example.yygh.common.result.Result;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


//@SpringBootApplication
@RestController
public class test {

    void timePrint(String s,long t){
        Date d = new Date(t);
        System.out.println(s);
        System.out.println(d);
    }
    @Autowired
    private Scheduler scheduler;

    // 各种触发器绑定方法
    // 可以通过JobExecutionContext调用进行定时服务所传递的参数
    // 提醒用户方法
    public  class Reminder implements Job{
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            String now_id = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("id");
            timePrint("reminder ",System.currentTimeMillis());
            wr("go " + now_id + "\n");
            // 关闭触发器
            try {
                scheduler.pauseTrigger(TriggerKey.triggerKey(now_id));//暂停触发器
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey(now_id));//移除触发器
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            try {
                scheduler.deleteJob(JobKey.jobKey(now_id));//删除Job
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }


    // Tout终止监听触发
    public class end implements Job{
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            // 获取当前节点的状态
            String now_id = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("id");
            int i_id = Integer.parseInt(now_id) - shift1;
            int state = syncQueue.getState(i_id);
            if(state != 2) {
                syncQueue.modify(i_id,4);
                System.out.println("gg");
                wr("expire " + now_id + "\n");
            }
            else System.out.println("user " + i_id + " have arrived and enjoy the service");

            // 关闭触发器
            try {
                scheduler.pauseTrigger(TriggerKey.triggerKey(now_id));//暂停触发器
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            try {
                scheduler.unscheduleJob(TriggerKey.triggerKey(now_id));//移除触发器
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
            try {
                scheduler.deleteJob(JobKey.jobKey(now_id));//删除Job
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    // Tin不断在start和target之间寻找合适时间段进行插入
    public class search implements Job{

        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            // 获取person一些必要参数
            int type = (int) jobExecutionContext.getJobDetail().getJobDataMap().get("type");
            long Tp = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("Tp");
            long t0 = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("t0");
            long Ts = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("Ts");
            long Te = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("Te");
            long Tout = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("Tout");
            long Tn = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("Tn");
            int state = (int) jobExecutionContext.getJobDetail().getJobDataMap().get("state");

            String now_id = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("id");
            long target = (long) jobExecutionContext.getJobDetail().getJobDataMap().get("target");
            // 每次判断当前时刻是否是插入到队列的合适时刻
            long now_time = System.currentTimeMillis();
            long need_wait = syncQueue.getWaitTime() * 1000;

            int flag = syncQueue.getState(Integer.parseInt(now_id) - shift2);
            // 成功将当前节点放入
            if(flag == - 1 && now_time + need_wait >= target){
                Person person = new Person();
                person.id = Integer.parseInt(now_id) - shift2;
                timePrint("user " + person.id + " we successful put node into the queue",now_time);
                person.type = type;
                person.Tp = Tp;
                person.t0 = t0;
                person.Ts = Ts;
                person.Te = Te;
                person.Tout = Tout;
                person.Tn = Tn;
                person.state = 1;   // 虚拟放入
                try {
                    syncQueue.put(person);
                    wr("put " + now_id + "\n");
                    System.out.println("enter once is ok");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 关闭触发器
                try {
                    scheduler.pauseTrigger(TriggerKey.triggerKey(now_id));//暂停触发器
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
                try {
                    scheduler.unscheduleJob(TriggerKey.triggerKey(now_id));//移除触发器
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
                try {
                    scheduler.deleteJob(JobKey.jobKey(now_id));//删除Job
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // 节点定义
     class Person implements Comparable<Person>{
        // 唯一标识
        int id;
        // 预约类型
        int type;   // 0 立即进行处理 1 期望在某时间段到达 2 用户期望某时刻T左右到达
        // 初始状态
        long Tp;    // 预约提交时刻
        long t0;    // 客户预计路途时间
        long Ts;    // 客户预约起始时刻
        long Te;    // 客户预约终止时刻
        // 运行状态
        long Tin;   // (虚拟)客户进入队列时刻
        long Tout;  // 客户预约有效期结束时刻
        long Tn;    // 通知客户出发时间
        long Ta;    // 客户到达时间
        int state; //0 初始 1 虚拟到达排队 2 用户到达 3 等待到达 4 取消

        @Override
        public int compareTo(Person o) {
            return 0;
        }
    }

    // 节点队列定义
    static SyncQueue syncQueue = new SyncQueue();
    static class SyncQueue{
        private static final int CAPACITY = 10;
        private static final long waitStandard = 2000; // ms
        private final LinkedList<Person> queue = new LinkedList<Person>();

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition bufferNotFull = lock.newCondition();
        private final Condition bufferNotEmpty = lock.newCondition();

        private static int arrive = 0;

        // 获取当前队列中有那些元素在排队,其中出去那些已经取消或者迟到人的状态
        private int getExistSize(){
            lock.lock();
            try {
                int res = 0;
                for(int i = 0;i < queue.size();i ++){
                    Person tmp = queue.get(i);
                    if(tmp.id != 4)
                        res ++;
                }
                return res;
            }finally {
                lock.unlock();
            }
        }

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

        long getWaitTime(){
            lock.lock();
            try {
                int sz = queue.size();
                return sz * waitStandard;
            }finally {
                lock.unlock();
            }
        }

        // 主要检测当前队列中是否有到达元素可以进行处理
        private boolean realEmpty(){
            lock.lock();
            try {
                return arrive == 0;
            }finally {
                lock.unlock();
            }
        }

        private void put(Person person) throws InterruptedException{
            lock.lock();
            try {
                while (Full()) {
                    System.out.println("Buffer is full, waiting");
                    bufferNotEmpty.await();
                }
                System.out.println("adding " + person.id);
                queue.add(person);
                bufferNotFull.signal();
            }finally {
                lock.unlock();
            }
        }

        private int getNow(){
            lock.lock();
            try {
                return arrive;
            }finally {
                lock.unlock();
            }
        }

        // 返回标识为id的对象
        private @Nullable Person find(int id) throws InterruptedException{
            lock.lock();
            try {
                for(int i = 0;i < queue.size();i ++){
                    Person tmp = queue.get(i);
                    if(tmp.id == id) return tmp;
                }
                return null;
            }finally {
                lock.unlock();
            }
        }

        // 取出队头元素,此时保证节点一定是到达状态
        private @Nullable Person get() throws InterruptedException{
            lock.lock();
            try {
                while (Empty()) {
                    System.out.println("Buffer is empty, waiting");
                    bufferNotFull.await();
                }
                Person tmp = queue.get(0);
                queue.remove(0);
                arrive --;
                return tmp;
            }finally {
                lock.unlock();
            }
        }

        // 获取指定id节点的当前状态
        private int getState(int id){
            lock.lock();
            try {
                for(int i = 0;i < queue.size();i ++){
                    Person tmp = queue.get(i);
                    if(tmp.id == id)
                        return tmp.state;
                }
                return -1;
            }finally {
                lock.unlock();
            }
        }

        // 修改指定id节点的状态,此时变成到达状态才会将arrive ++
        private boolean modify(int id,int state){
            lock.lock();
            try {
                for(int i = 0;i < queue.size();i ++){
                    Person tmp = queue.get(i);
                    if(tmp.id == id){
                        tmp.state = state;
                        if(state == 2) arrive ++;
                        queue.set(i,tmp);
                    }
                }
                return true;
            }finally {
                lock.unlock();
            }
        }

        // 修改指定id节点的到达状态
        private boolean modifyArrive(int id,long arrive){
            lock.lock();
            try {
                for(int i = 0;i < queue.size();i ++){
                    Person tmp = queue.get(i);
                    if(tmp.id == id){
                        tmp.Ta = arrive;
                        queue.set(i,tmp);
                    }
                }
                return true;
            }finally {
                lock.unlock();
            }
        }

        // 根据state进行排队,实际上就是找到第一个state == 2将其移动到队首部
        public boolean sort(){
            lock.lock();
            try {
                if(arrive == 0) return true;
                assert (arrive > 0);
                int begin = -1;
                Person tmp = null;
                for(int i = 0;i < queue.size();i ++){
                    if(queue.get(i).state == 2){
                        begin = i;
                        tmp = queue.get(i);
                        break;
                    }
                }
                queue.remove(begin);
                queue.add(0,tmp);
                return true;
            }finally {
                lock.unlock();
            }
        }

        // 打印当前队列中所有元素的信息
        public void print(){
            lock.lock();
            try {
                System.out.println("the whole content in the queue is following");
                for(int i = 0;i < queue.size();i ++){
                    System.out.println("the now is " + queue.get(i).id);
                }
            }finally {
                lock.unlock();
            }
        }
    }


    // 每个客户所对应的机器人
    static long delta = 10000; // 宽松时限(ms)
    static int id = 0;    // 每个节点唯一标识

    // 设置触发器规则
    // Tn shift = 0
    // Tout shift1 = 1000
    // Tin shift2 = 2000
    static final int shift1 = 1000;
    static final int shift2 = 2000;
    class Producer implements Runnable{
        private SyncQueue syncQueue;
        private Person person;

        public Producer(SyncQueue syncQueue,Person person){
            this.syncQueue = syncQueue;
            this.person = person;
        }

        @SneakyThrows
        @Override
        public void run() {
            if(person.type == 0){
                // 需要直接传入参数
                person.t0 = 2000;

                person.Tp = System.currentTimeMillis();
                person.id = id ++;
                person.state = 1;
                person.Tin = person.Tp;
                person.Tout = person.Tp + (Math.max(person.t0,syncQueue.getWaitTime()) + delta);
                person.Tn = person.Tp + (Math.max(person.t0,syncQueue.getWaitTime()) - person.t0);

                // 此会直接加入元素到队列所以不需要对添加元素到队列增加监视器
                try {
                    syncQueue.put(person);
                }catch (Exception e){
                    e.printStackTrace();
                }

                // Tn触发器
                Date start=new Date(person.Tn);
                int now_id = person.id;
                //创建调度器
                JobDetail jobDetail = JobBuilder.newJob(Reminder.class)
                        .usingJobData("name","dy")
                        .usingJobData("id",Integer.toString(now_id))
                        .withIdentity(Integer.toString(now_id))
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .usingJobData("id",Integer.toString(now_id))
                        .withIdentity(Integer.toString(now_id))
                        .startAt(start)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInSeconds(1)
                                        .repeatForever()
                        )
                        .build();

                // 将Tn注册到调度器中
                try {
                    scheduler.scheduleJob(jobDetail,trigger);
                }catch (SchedulerException e){
                    e.printStackTrace();
                }


                // Tout触发器
                Date end = new Date(person.Tout);
                JobDetail jobDetail2 = JobBuilder.newJob(end.class)
                        .usingJobData("name","dy")
                        .usingJobData("id",Integer.toString(now_id + shift1))
                        .withIdentity(Integer.toString(now_id + shift1))
                        .build();

                Trigger trigger2 = TriggerBuilder.newTrigger()
                        .usingJobData("id",Integer.toString(now_id + shift1))
                        .withIdentity(Integer.toString(now_id + shift1))
                        .startAt(end)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInSeconds(1)
                                        .repeatForever()
                        )
                        .build();

                // 将Tout注册到调度器中
                try {
                    scheduler.scheduleJob(jobDetail2,trigger2);
                }catch (SchedulerException e){
                    e.printStackTrace();
                }

                // 启动调度器,此处调度器只会执行一次
                try {
                    if(!scheduler.isShutdown()){
                        scheduler.start();
                    }
                }catch (SchedulerException e){
                    e.printStackTrace();
                }

            }else if(person.type == 1){
                person.Tp = System.currentTimeMillis();
                person.id = id ++;
                person.state = 0;

                // 期望在某一时间段到达
                long target = 0,start = 0;
                if(person.Te - person.Ts <= 30 * 60 * 1000){
                    target = person.Ts;
                }else{
                    target = person.Ts + (person.Te - person.Ts) / 3;
                }
                start = person.Ts - syncQueue.getWaitTime();

                person.Tout = person.Te + delta;
                person.Tn = target - person.t0;

                System.out.println("user is "+ person.id);
                timePrint("submit",person.Tp);
                timePrint("Ts",person.Ts);
                timePrint("Te",person.Te);
                timePrint("begin",start);
                timePrint("end",target);
                timePrint("attention",person.Tn);
                timePrint("deadline",person.Tout);
                System.out.println("\n");
                System.out.println("\n");
                System.out.println("\n");


                // Tn触发器
                Date startTime = new Date(person.Tn);
                int now_id = person.id;
                //创建调度器
                JobDetail jobDetail = JobBuilder.newJob(Reminder.class)
                        .usingJobData("name","dy")
                        .usingJobData("id",Integer.toString(now_id))
                        .withIdentity(Integer.toString(now_id))
                        .build();

                Trigger trigger = TriggerBuilder.newTrigger()
                        .usingJobData("id",Integer.toString(now_id))
                        .withIdentity(Integer.toString(now_id))
                        .startAt(startTime)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInSeconds(1)
                                        .repeatForever()
                        )
                        .build();

                // 将Tn注册到调度器中
                try {
                    scheduler.scheduleJob(jobDetail,trigger);
                }catch (SchedulerException e){
                    e.printStackTrace();
                }


                // Tout触发器
                Date end = new Date(person.Tout);
                JobDetail jobDetail2 = JobBuilder.newJob(end.class)
                        .usingJobData("name","dy")
                        .usingJobData("id",Integer.toString(now_id + shift1))
                        .withIdentity(Integer.toString(now_id + shift1))
                        .build();

                Trigger trigger2 = TriggerBuilder.newTrigger()
                        .usingJobData("id",Integer.toString(now_id + shift1))
                        .withIdentity(Integer.toString(now_id + shift1))
                        .startAt(end)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInSeconds(1)
                                        .repeatForever()
                        )
                        .build();
                // 将Tout注册到调度器中
                try {
                    scheduler.scheduleJob(jobDetail2,trigger2);
                }catch (SchedulerException e){
                    e.printStackTrace();
                }

                // Tin调度器
                // 由于此时将节点放入到队列是需要在调用触发器的过程中进行调用的
                // 由于无法直接传递类,所以只能将所需要参数进行传递
                Date enter = new Date(start);
                JobDetail jobDetail3 = JobBuilder.newJob(search.class)
                        .usingJobData("id",Integer.toString(now_id + shift2))
                        // person必要参数传递
                        .usingJobData("type",person.type)
                        .usingJobData("Tp",person.Tp)
                        .usingJobData("t0",person.t0)
                        .usingJobData("Ts",person.Ts)
                        .usingJobData("Te",person.Te)
                        .usingJobData("Tout",person.Tout)
                        .usingJobData("Tn",person.Tn)
                        .usingJobData("state",person.state)
                        .usingJobData("target",target)
                        .withIdentity(Integer.toString(now_id + shift2))
                        .build();

                Trigger trigger3 = TriggerBuilder.newTrigger()
                        .usingJobData("id",Integer.toString(now_id + shift2))
                        .withIdentity(Integer.toString(now_id + shift2))
                        .startAt(enter)
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInSeconds(1)
                                        .repeatForever()
                        )
                        .build();

                // 将Tout注册到调度器中
                try {
                    scheduler.scheduleJob(jobDetail3,trigger3);
                }catch (SchedulerException e){
                    e.printStackTrace();
                }

                // 启动调度器,此处调度器只会执行一次
                try {
                    if(!scheduler.isShutdown()){
                        scheduler.start();
                    }
                }catch (SchedulerException e){
                    e.printStackTrace();
                }
            }
        }
    }

    static boolean ok = false;
    static final ReentrantLock doLock = new ReentrantLock();
    static final Condition canDo = doLock.newCondition();

    static class Consumer implements Runnable{

        private SyncQueue syncQueue;
        public Consumer(SyncQueue syncQueue) {
            this.syncQueue = syncQueue;
        }
        @Override
        public void run() {
            // 依旧需要加入信号量阻塞
            while(true){
                while(!syncQueue.realEmpty()){
                    doLock.lock();
                    try {
                        while(!ok) {
                            try {
                                canDo.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }finally {
                        doLock.unlock();
                    }
                    // 从队列中取出第一个人已经到达的节点,也就是state = 2
                    if(!syncQueue.realEmpty()){
                        syncQueue.sort();
                        int tmpid = -1;
                        try {
                            Person deal = syncQueue.get();
                            tmpid = deal.id;
                            swr("deal " + deal.id + "\n");
                            // consume
                            if(deal != null)
                                System.out.println("we are now consuming  " + deal.id);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        doLock.lock();
                        try {
                            while(ok) {
                                canDo.await();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            doLock.unlock();
                        }
                        if(tmpid != -1)
                            swr("finish " + tmpid + "\n");
                        System.out.println("consume is over,wait for next consumer");
                    }
                }
            }
        }
    }

    // 立即想要预约到达的用户
    @GetMapping("/test1")
    public Result do1(){
        Person person = new Person();
        person.type = 0;
        Thread producer = new Thread(new Producer(syncQueue,person));
        producer.start();
        JSONObject map = new JSONObject();
        return Result.ok(map);
    }

    // 预约一个时间段进行到达的用户
    @GetMapping("/test2")
    public Result do2(){
        Person person = new Person();
        person.type = 1;
        person.t0 = 2000;
        person.Ts = System.currentTimeMillis() + 5000;
        person.Te = person.Ts + 50000;

        Thread producer = new Thread(new Producer(syncQueue,person));
        producer.start();
        JSONObject map = new JSONObject();
        return Result.ok(map);
    }

    // 用户到达
    @GetMapping("/arrive/{id}")
    public Result do3(@PathVariable int id){
        // 取当前队列中寻找唯一标识为id的节点,修改其状态为可执行的
        int state = syncQueue.getState(id);
        if(state == 4) {
            System.out.println("you are late");
        }else{
            if(syncQueue.modify(id,2)){
                timePrint("modify",System.currentTimeMillis());
            }
        }
        JSONObject map = new JSONObject();
        return Result.ok(map);
    }

    // 使用信号量实现同步
    @GetMapping("/ok")
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

    @GetMapping("/print")
    public Result do6() throws InterruptedException{
        doLock.lock();
        try {
            syncQueue.print();
        }finally {
            doLock.unlock();
        }
        JSONObject map = new JSONObject();
        return Result.ok(map);
    }
    public static void clear(){
        try{
            File file =new File("test_appendfile.txt");
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fileWritter = new FileWriter(file.getName(),false);
            fileWritter.write("");
            fileWritter.flush();
            fileWritter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void wr(String s){
        try{
            File file =new File("test_appendfile.txt");
            FileWriter fileWritter = new FileWriter(file.getName(),true);
            fileWritter.write(s);
            fileWritter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void swr(String s){
        try{
            File file =new File("test_appendfile.txt");
            FileWriter fileWritter = new FileWriter(file.getName(),true);
            fileWritter.write(s);
            fileWritter.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws SchedulerException, InterruptedException {
        SpringApplication.run(test.class, args);
        clear();
        Thread consumer = new Thread(new Consumer(syncQueue));
        consumer.start();
    }
}
