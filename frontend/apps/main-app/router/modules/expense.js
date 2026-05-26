import ExpenseList from './views/expense/ExpenseList.vue';

export default {
  path: '/expense',
  name: 'expense',
  component: ExpenseList,
  meta: {
    title: '费用单管理',
    icon: 'dollar'
  }
};