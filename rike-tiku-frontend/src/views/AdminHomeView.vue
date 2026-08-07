<template>
  <section class="admin-welcome">
    <div>
      <h1>管理员工作台</h1>
      <p>从班级管理开始维护教学组织，再通过 Excel 安全导入学生账号。</p>
    </div>
    <div class="admin-welcome-actions">
      <el-button type="primary" @click="$router.push('/admin/classes')">管理班级</el-button>
      <el-button @click="$router.push('/admin/students')">管理学生</el-button>
      <el-button @click="$router.push('/admin/students/import')">导入学生</el-button>
    </div>
  </section>
</template>
