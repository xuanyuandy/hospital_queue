<template>
  <div class="app-container">

    <el-form label-width="120px">
      <el-form-item label="医院名称">
        <el-input v-model="hospitalSet.hosname" />
      </el-form-item>
      <el-form-item label="医院编号">
        <el-input v-model="hospitalSet.hoscode" />
      </el-form-item>
      <el-form-item label="api基础路径">
        <el-input v-model="hospitalSet.apiUrl" />
      </el-form-item>
      <el-form-item label="联系人姓名">
        <el-input v-model="hospitalSet.contactsName" />
      </el-form-item>
      <el-form-item label="联系人手机">
        <el-input v-model="hospitalSet.contactsPhone" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="saveOrUpdate">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { saveHospSet, getHospSet, updateHospSet } from '@/api/hospset'
export default {
  data() {
    return {
      hospitalSet: {}
    }
  },
  created() {
    if (this.$route.params && this.$route.params.id) {
      const id = this.$route.params
      // 此处获取的id是object
      this.getHospSetDo(id)
    } else {
      this.hospitalSet = {}
    }
  },
  methods: {
    save() {
      saveHospSet(this.hospitalSet)
        .then(response => {
          this.$message({
            type: 'success',
            message: '添加成功!'
          })
          this.$router.push({ path: '/hospSet/list' })
        })
    },
    update() {
      updateHospSet(this.hospitalSet)
        .then(response => {
          this.$message({
            type: 'success',
            message: '修改成功!'
          })
          this.$router.push({ path: '/hospSet/list' })
        })
    },
    saveOrUpdate() {
      // 判断添加还是修改
      if (this.hospitalSet.id) {
        this.update()
      } else {
        this.save()
      }
    },
    getHospSetDo(id) {
      // 注意需要取出object中的id值
      getHospSet(id['id'])
        .then(response => {
          console.log(response)
          this.hospitalSet = response.data
          console.log('success')
        })
        .catch(error => {
          console.log(error)
        })
    }
  }
}
</script>
