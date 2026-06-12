const axios = require('axios');

async function getToken() {
  try {
    const res = await axios.post('http://localhost:8080/api/auth/login', {
      username: 'admin',
      password: 'admin123',
      tenantName: 'SYSTEM'
    }, {
      headers: { 'Content-Type': 'application/json' }
    });
    
    console.log('\n=== 登录响应 ===');
    console.log('Token:', res.data.data?.token);
    console.log('UserId:', res.data.data?.userId);
    console.log('TenantId:', res.data.data?.tenantId);
    
    // 使用token获取用户信息
    const userInfoRes = await axios.get('http://localhost:8080/api/auth/userinfo', {
      headers: {
        'Authorization': 'Bearer ' + res.data.data?.token,
        'tenantId': '1'
      }
    });
    
    console.log('\n=== 用户信息响应 ===');
    console.log(JSON.stringify(userInfoRes.data, null, 2));
    
  } catch (error) {
    console.error('错误:', error.response?.data || error.message);
  }
}

getToken();
