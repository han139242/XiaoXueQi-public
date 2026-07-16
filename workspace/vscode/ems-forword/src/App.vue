<template>
  <!--
    App.vue 是根组件，但它本身不固定显示某一个业务页面。
    RouterView 是路由出口：router 根据当前 URL 选择登录页、首页或业务页面，
    再把选择出的页面组件渲染到这里。因此切换路由时不需要刷新整个网页。
  -->
  <RouterView />
</template>
