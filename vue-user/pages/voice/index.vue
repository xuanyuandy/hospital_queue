<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/user'"
          >实名认证
        </span>
      </div>
      <div class="nav-item">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/order'"
        >
          挂号订单
        </span>
      </div>
      <div class="nav-item clickable dark">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/patient'"
        >
          就诊人管理
        </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark"> 修改账号信息 </span>
      </div>
      <div class="nav-item dark">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/get'"
        >
          取号
        </span>
      </div>
      <div class="nav-item selected">
        <span
          class="v-link selected"
          onclick="javascript:window.location='/voice'"
        >
          叫号
        </span>
      </div>
      <div class="nav-item">
        <span
          class="v-link clickable dark"
          onclick="javascript:window.location='/special'"
        >
          会员
        </span>
      </div>
    </div>
    <!-- 左侧导航 #end -->

    <div class="page-container">
        
        <!-- 管理员查看界面 -->
        <el-row>
          <div class="text">
            当前人数是{{ whole }}
          </div>
          <el-button type="primary" v-if="flag==0" @click="go">叫人</el-button>
          <el-button type="primary" v-if="flag==1" @click="go">结束</el-button>
        </el-row>
        <el-row>
          <div class="text" v-if="flag==1">
            当前处理对象是{{ now }}
          </div>
        </el-row>
    </div>
  </div>
  <!-- footer -->
</template>

<script>
import "~/assets/css/hospital_personal.css";
import "~/assets/css/hospital.css";
import "~/assets/css/personal.css";

import getnumApi from "@/api/get/getnum"

var interval;
var interval2;
export default {
    data(){
        return{
            whole : 0,
            flag : 0,
            now : ""
        };
    },
    created(){
        this.pollData();
    },
    destroyed(){
      if(interval){
        console.log("destory");
        clearTimeout(interval);
        interval = null;
      }
      if(interval2){
        console.log("destory");
        clearTimeout(interval2);
        interval2 = null;
      }
    },
    methods:{
        pollData(){
            interval = setInterval(this.query, 1000);
            interval2 = setInterval(this.query2,1000);
        },
        // 查询当前队列中的人数
        query(){
          if(this.$route.path == "/voice"){
            getnumApi.getWhole().then(
                (response) => {
                    this.whole = response.data.whole;
                }
            )
          }else{
            clearTimeout(interval);
            interval = null;
          }
        },
        // 查询当前正在处理的对象
        query2(){
          if(this.$route.path == "/voice"){
            getnumApi.getNow().then(
                (response) => {
                    this.now = response.data.now;
                }
            )
          }else{
            clearTimeout(interval2);
            interval2 = null;
          }
        },
        go(){
            this.flag = 1 - this.flag;
            getnumApi.getModify().then(
                (response) => {
                    console.log(response.data);
                }
            )
        }
    }
};
</script>

<style scoped>
 .el-row {
    margin-bottom: 20px;
  }
</style>