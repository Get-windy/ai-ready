/**
 * 菜单图标名称到 Ant Design 图标组件的映射表
 * 由 BasicLayout.vue 提取到独立文件，降低耦合度
 */
import {
  DashboardOutlined,
  ShopOutlined,
  TeamOutlined,
  SettingOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  ContainerOutlined,
  FileTextOutlined,
  UserOutlined,
  SafetyOutlined,
  AccountBookOutlined,
  MoneyCollectOutlined,
  BarChartOutlined,
  AppstoreOutlined,
  BranchesOutlined,
  StarOutlined,
  StarFilled,
  UserAddOutlined,
  FileOutlined,
  InboxOutlined,
  SendOutlined,
  AuditOutlined,
  RestOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  ApartmentOutlined,
  IdcardOutlined,
  UnorderedListOutlined,
  QuestionCircleOutlined,
  MonitorOutlined,
  LineChartOutlined,
  CheckSquareOutlined,
  RollbackOutlined,
  SwapOutlined,
  SearchOutlined,
  BellOutlined,
  LogoutOutlined,
  LinkOutlined
} from '@ant-design/icons-vue'

const iconMap: Record<string, any> = {
  'DashboardOutlined': DashboardOutlined,
  'ShopOutlined': ShopOutlined,
  'TeamOutlined': TeamOutlined,
  'SettingOutlined': SettingOutlined,
  'ShoppingOutlined': ShoppingOutlined,
  'ShoppingCartOutlined': ShoppingCartOutlined,
  'ContainerOutlined': ContainerOutlined,
  'FileTextOutlined': FileTextOutlined,
  'UserOutlined': UserOutlined,
  'SafetyOutlined': SafetyOutlined,
  'AccountBookOutlined': AccountBookOutlined,
  'MoneyCollectOutlined': MoneyCollectOutlined,
  'BarChartOutlined': BarChartOutlined,
  'AppstoreOutlined': AppstoreOutlined,
  'BranchesOutlined': BranchesOutlined,
  'StarOutlined': StarOutlined,
  'UserAddOutlined': UserAddOutlined,
  'FileOutlined': FileOutlined,
  'InboxOutlined': InboxOutlined,
  'SendOutlined': SendOutlined,
  'AuditOutlined': AuditOutlined,
  'RestOutlined': RestOutlined,
  'DollarOutlined': DollarOutlined,
  'CheckCircleOutlined': CheckCircleOutlined,
  'ApartmentOutlined': ApartmentOutlined,
  'IdcardOutlined': IdcardOutlined,
  'UnorderedListOutlined': UnorderedListOutlined,
  'QuestionCircleOutlined': QuestionCircleOutlined,
  'MonitorOutlined': MonitorOutlined,
  'LineChartOutlined': LineChartOutlined,
  'CheckSquareOutlined': CheckSquareOutlined,
  'RollbackOutlined': RollbackOutlined,
  'SwapOutlined': SwapOutlined
}

/**
 * 根据图标名称返回对应的 Ant Design 图标组件
 * @param iconName 图标名称（如 'DashboardOutlined'）
 * @returns 图标组件，未找到时返回 DashboardOutlined
 */
export function getIcon(iconName?: string): any {
  if (!iconName) return null
  return iconMap[iconName] || DashboardOutlined
}

export { iconMap }
export default iconMap
