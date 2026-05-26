import { createRouter, createWebHistory } from 'vue-router'
import TestComponents from '../pages/TestComponents.vue'

const routes = [
  {
    path: '/',
    name: 'TestComponents',
    component: TestComponents
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router