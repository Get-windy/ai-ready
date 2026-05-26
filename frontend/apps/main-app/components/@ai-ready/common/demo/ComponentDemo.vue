<template>
  <div class="component-demo">
    <h1 class="demo-title">AI-Ready 基础组件库展示</h1>
    <p class="demo-desc">测试环境前端组件库 - 包含基础组件和表单组件</p>

    <!-- Button 组件展示 -->
    <section class="demo-section">
      <h2>ARButton 按钮组件</h2>
      <div class="demo-block">
        <h3>按钮类型</h3>
        <div class="demo-row">
          <ARButton>默认按钮</ARButton>
          <ARButton type="primary">主要按钮</ARButton>
          <ARButton type="success">成功按钮</ARButton>
          <ARButton type="warning">警告按钮</ARButton>
          <ARButton type="danger">危险按钮</ARButton>
        </div>
      </div>

      <div class="demo-block">
        <h3>按钮尺寸</h3>
        <div class="demo-row">
          <ARButton size="large">大号按钮</ARButton>
          <ARButton>默认按钮</ARButton>
          <ARButton size="small">小号按钮</ARButton>
        </div>
      </div>

      <div class="demo-block">
        <h3>禁用状态</h3>
        <div class="demo-row">
          <ARButton disabled>禁用按钮</ARButton>
          <ARButton type="primary" disabled>禁用主按钮</ARButton>
        </div>
      </div>

      <div class="demo-block">
        <h3>加载状态</h3>
        <div class="demo-row">
          <ARButton loading>加载中</ARButton>
          <ARButton type="primary" loading>提交中</ARButton>
        </div>
      </div>
    </section>

    <!-- Input 组件展示 -->
    <section class="demo-section">
      <h2>ARInput 输入框组件</h2>
      <div class="demo-block">
        <h3>基础输入框</h3>
        <div class="demo-row">
          <ARInput v-model="formData.input1" placeholder="请输入内容" />
        </div>
      </div>

      <div class="demo-block">
        <h3>禁用状态</h3>
        <div class="demo-row">
          <ARInput disabled placeholder="禁用状态" />
        </div>
      </div>

      <div class="demo-block">
        <h3>可清空</h3>
        <div class="demo-row">
          <ARInput v-model="formData.input2" clearable placeholder="可清空" />
        </div>
      </div>

      <div class="demo-block">
        <h3>图标</h3>
        <div class="demo-row">
          <ARInput v-model="formData.input3" prefix-icon="User" placeholder="请输入用户名" />
          <ARInput v-model="formData.input4" prefix-icon="Lock" type="password" placeholder="请输入密码" />
        </div>
      </div>

      <div class="demo-block">
        <h3>尺寸</h3>
        <div class="demo-row">
          <ARInput size="large" placeholder="大号输入框" />
          <ARInput placeholder="默认输入框" />
          <ARInput size="small" placeholder="小号输入框" />
        </div>
      </div>
    </section>

    <!-- Card 组件展示 -->
    <section class="demo-section">
      <h2>ARCard 卡片组件</h2>
      <div class="demo-block">
        <h3>基础卡片</h3>
        <div class="demo-row">
          <ARCard>
            <p>这是一张基础卡片</p>
          </ARCard>
        </div>
      </div>

      <div class="demo-block">
        <h3>带标题</h3>
        <div class="demo-row">
          <ARCard header="卡片标题">
            <p>这是一张带标题的卡片</p>
          </ARCard>
        </div>
      </div>

      <div class="demo-block">
        <h3>悬浮效果</h3>
        <div class="demo-row">
          <ARCard hoverable>
            <p>悬浮时会有上浮效果</p>
          </ARCard>
        </div>
      </div>
    </section>

    <!-- Form 组件展示 -->
    <section class="demo-section">
      <h2>ARForm 表单组件</h2>
      <div class="demo-block">
        <h3>基础表单</h3>
        <div class="demo-row">
          <ARCard>
            <ARForm :model="formData.form" :rules="formRules">
              <ARFormItem label="用户名" prop="username">
                <ARInput v-model="formData.form.username" prefix-icon="User" />
              </ARFormItem>
              <ARFormItem label="邮箱" prop="email">
                <ARInput v-model="formData.form.email" prefix-icon="Message" />
              </ARFormItem>
              <ARFormItem label="密码" prop="password">
                <ARInput v-model="formData.form.password" type="password" prefix-icon="Lock" />
              </ARFormItem>
              <ARFormItem>
                <ARButton type="primary">提交</ARButton>
                <ARButton>重置</ARButton>
              </ARFormItem>
            </ARForm>
          </ARCard>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ARButton, ARInput, ARCard, ARForm, ARFormItem } from '../components'

const formData = reactive({
  input1: '',
  input2: '可清空的内容',
  input3: '',
  input4: '',
  form: {
    username: '',
    email: '',
    password: '',
  },
})

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
  ],
}
</script>

<style scoped>
.component-demo {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  background-color: var(--ar-bg-page);
}

.demo-title {
  font-size: 30px;
  font-weight: 600;
  color: var(--ar-text-primary);
  margin-bottom: 8px;
  text-align: center;
}

.demo-desc {
  font-size: 16px;
  color: var(--ar-text-secondary);
  text-align: center;
  margin-bottom: 48px;
}

.demo-section {
  margin-bottom: 48px;
}

.demo-section h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--ar-text-primary);
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid var(--ar-primary);
}

.demo-block {
  margin-bottom: 32px;
}

.demo-block h3 {
  font-size: 16px;
  font-weight: 500;
  color: var(--ar-text-regular);
  margin-bottom: 16px;
}

.demo-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.demo-row .ar-card {
  width: 300px;
}

.demo-row .ar-input,
.demo-row .ar-button {
  margin-right: 8px;
}

/* 深色模式 */
[data-theme='dark'] .component-demo {
  background-color: var(--ar-bg-page);
}

[data-theme='dark'] .demo-title {
  color: var(--ar-text-primary);
}

[data-theme='dark'] .demo-desc {
  color: var(--ar-text-secondary);
}

[data-theme='dark'] .demo-section h2 {
  color: var(--ar-text-primary);
  border-bottom-color: var(--ar-primary);
}

[data-theme='dark'] .demo-block h3 {
  color: var(--ar-text-regular);
}
</style>