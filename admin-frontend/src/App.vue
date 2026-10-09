<template><router-view /></template>
<style>
:root{--admin-sidebar-width:220px;--admin-content-max-width:1440px}
html,body,#app{margin:0;min-height:100%;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:#f5f7fb;color:#26364a}
*{box-sizing:border-box}
#app .admin-shell>main.main-content,#app .shell>main.main,#app .task-shell>main.main{
  width:min(var(--admin-content-max-width),calc(100% - var(--admin-sidebar-width)));
  margin-left:max(var(--admin-sidebar-width),calc((100% + var(--admin-sidebar-width) - var(--admin-content-max-width))/2));
}
@media(max-width:900px){
  :root{--admin-sidebar-width:180px}
}
</style>
