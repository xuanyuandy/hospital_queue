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
      <div class="nav-item selected">
        <span
          class="v-link selected"
          onclick="javascript:window.location='/get'"
        >
          取号
        </span>
      </div>
      <div class="nav-item">
        <span
          class="v-link clickable dark"
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
        <span slot="label">姓名</span>
        <el-row>
            <el-input v-model="name" placeholder="请输入姓名" style="width:300px"></el-input>
        </el-row>
        <!-- <span slot="label">到达时间</span>
        <el-row>
            <el-input v-model="arrive" placeholder="请输入取号时间" style="width:300px"></el-input>
        </el-row> -->

        <el-row>
            <el-button type="primary" v-if="flag2==0" @click="go1">预约</el-button>
            <el-button type="primary" v-if="flag2==1" @click="go2">取号</el-button>
        </el-row>

        <el-row>
            <div class="text" v-if="flag==1">
                您当前号码是{{ qid }}
            </div>
        </el-row>
        <el-row>
            <div class="text" v-if="flag==1 && num>0">
                当前前面人数是{{ num }}
            </div>
            <div class="text" v-if="flag==1 && num==-1">
                即将进行处理请做好准备
            </div>
            <div class="text" v-if="flag==1 && num==0">
                正在处理请耐心等候
            </div>
            <div class="text" v-if="flag==1 && num==-2">
                已处理完毕
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
export default {
    data(){
        return{
            name : "test",    // 用户姓名
            arrive : 0,   // 取号时间
            qid : 0,      // 获取号码
            num : 1,      // 前面等待人数
            flag : 0 ,     // 取号后进行不断轮询等待
            flag2 : 0
        };
    },
    created(){
        this.pollData();
    },
    methods:{
        pollData(){
            interval = setInterval(this.query, 1000);
        },
        // 查询当前位置
        query(){
            if(this.num == -2) clearInterval(interval);
            if(this.flag){
                getnumApi.getEnd(this.name).then(
                    (response) => {
                        console.log(response.data)
                        this.num = response.data.num;
                    }
                );
            }
        },
        // 初始挂号顺序
        go1(){
            getnumApi.getBegin(this.name).then(
                (response) => {
                    this.qid = response.data.qid;
                    this.flag2 = 1;
                }
            )
        },
        // 将到达时间进行发送
        go2(){
            var data = new Date().getTime();
            console.log(data);
            this.arrive = data;
            // 需要将data转换为毫秒形式
            getnumApi.getMid(this.name,this.arrive).then(
                (response) => {
                    this.flag = 1;
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