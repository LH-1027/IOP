<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* { 
  margin: 0; 
  padding: 0; 
  box-sizing: border-box; 
}

:root {
  --brand: #6366f1;
  --brand-dark: #4f46e5;
  --brand-soft: #eef2ff;
  --ink: #0f172a;
  --muted: #64748b;
  --line: #eef2f7;
  --bg: #f6f7fb;
  --radius: 16px;
  --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.04), 0 1px 3px rgba(15, 23, 42, 0.06);
  --shadow-md: 0 4px 12px rgba(15, 23, 42, 0.06), 0 12px 32px rgba(15, 23, 42, 0.05);
  --shadow-lg: 0 10px 30px rgba(99, 102, 241, 0.18);
}

body {
  font-family: "PingFang SC", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  background-color: var(--bg);
  background-image:
    radial-gradient(900px 500px at 12% -8%, rgba(99, 102, 241, 0.10), transparent 60%),
    radial-gradient(800px 500px at 100% 0%, rgba(139, 92, 246, 0.08), transparent 55%);
  background-attachment: fixed;
  color: var(--ink);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 纤细圆润滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 8px;
}
::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>