package com.example.yygh.test;

import io.swagger.annotations.ApiOperation;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

//@SpringBootApplication
//@RestController
public class Quartz {

    @Autowired
    private Scheduler scheduler;

    @GetMapping("/test")
    public Object ds() {
        System.out.println("enter");
        return "ok";
    }
    @PostMapping("/Quartz")
    @ApiOperation(value = "定时任务_创建", notes = "创建")
    @ResponseBody
    public Object quartz(@RequestParam("orderNo")  String orderNo) throws Exception {
        Date start=new Date(System.currentTimeMillis() + 7 * 1000);//当前时间7秒之后

        /**通过JobBuilder.newJob()方法获取到当前Job的具体实现(以下均为链式调用)
         * 这里是固定Job创建，所以代码写死XXX.class
         * 如果是动态的，根据不同的类来创建Job，则 ((Job)Class.forName("com.zy.job.TestJob").newInstance()).getClass()
         * 即是 JobBuilder.newJob(((Job)Class.forName("com.zy.job.TestJob").newInstance()).getClass())
         * */
        JobDetail jobDetail = JobBuilder.newJob(TimerTest.class)
                /**给当前JobDetail添加参数，K V形式*/
                .usingJobData("name","zy")
                /**给当前JobDetail添加参数，K V形式，链式调用，可以传入多个参数，在Job实现类中，可以通过jobExecutionContext.getJobDetail().getJobDataMap().get("age")获取值*/
                .usingJobData("age",23)
                /**添加认证信息，有3种重写的方法，我这里是其中一种，可以查看源码看其余2种*/
                .withIdentity(orderNo)
                .build();//执行


        Trigger trigger = TriggerBuilder.newTrigger()
                /**给当前JobDetail添加参数，K V形式，链式调用，可以传入多个参数，在Job实现类中，可以通过jobExecutionContext.getTrigger().getJobDataMap().get("orderNo")获取值*/
                .usingJobData("orderNo", orderNo)
                /**添加认证信息，有3种重写的方法，我这里是其中一种，可以查看源码看其余2种*/
                .withIdentity(orderNo)
                /**立即生效*/
//      .startNow()
                /**开始执行时间*/
                .startAt(start)
                /**结束执行时间*/
//        .endAt(start)
                /**添加执行规则，SimpleTrigger、CronTrigger的区别主要就在这里*/
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                /**每隔1s执行一次*/
                                .withIntervalInSeconds(3)
                                /**一直执行，*/
                                .repeatForever()
                )
                .build();//执行

//CronTrigger  trigger = TriggerBuilder.newTrigger()
//        /**给当前JobDetail添加参数，K V形式，链式调用，可以传入多个参数，在Job实现类中，可以通过jobExecutionContext.getTrigger().getJobDataMap().get("orderNo")获取值*/
//        .usingJobData("orderNo", orderNo)
//        /**添加认证信息，有3种重写的方法，我这里是其中一种，可以查看源码看其余2种*/
//        .withIdentity(orderNo)
//        /**开始执行时间*/
//        .startAt(start)
//        /**结束执行时间*/
//        .endAt(start)
//        /**添加执行规则，SimpleTrigger、CronTrigger的区别主要就在这里*/
//        .withSchedule(CronScheduleBuilder.cronSchedule("* 30 10 ? * 1/5 2018"))
//        .build();//执行


        /**添加定时任务*/
        scheduler.scheduleJob(jobDetail, trigger);
        if (!scheduler.isShutdown()) {
            /**启动*/
            scheduler.start();
        }
        System.err.println("--------定时任务启动成功 "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" ------------");
        return "ok";
    }

    @PostMapping("/shutdown")
    @ApiOperation(value = "定时任务_停止", notes = "停止")
    @ResponseBody
    public Object shutdown(@RequestParam("orderNo")  String orderNo) throws IOException, SchedulerException {
        scheduler.pauseTrigger(TriggerKey.triggerKey(orderNo));//暂停Trigger
        return "";
    }

    @PostMapping("/resume")
    @ApiOperation(value = "定时任务_恢复", notes = "恢复")
    @ResponseBody
    public Object resume(@RequestParam("orderNo")  String orderNo) throws IOException, SchedulerException {
        scheduler.resumeTrigger(TriggerKey.triggerKey(orderNo));//恢复Trigger
        return "ok";
    }

    @PostMapping("/del")
    @ApiOperation(value = "定时任务_删除", notes = "删除")
    @ResponseBody
    public Object del(@RequestParam("orderNo")  String orderNo) throws IOException, SchedulerException {
        scheduler.pauseTrigger(TriggerKey.triggerKey(orderNo));//暂停触发器
        scheduler.unscheduleJob(TriggerKey.triggerKey(orderNo));//移除触发器
        scheduler.deleteJob(JobKey.jobKey(orderNo));//删除Job
        return "ok";
    }

    public static void main(String[] args) {
        SpringApplication.run(Quartz.class,args);
    }
}
