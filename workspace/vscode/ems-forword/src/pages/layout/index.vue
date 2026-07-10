<template>
    <el-container class="contrainer">
        <el-header>Header</el-header>
        <el-container>
            <el-aside width="200px">Aside</el-aside>
            <el-main>Main</el-main>
        </el-container>
    </el-container>
</template>

<script>
    export default {

    }
</script>

<style lang="scss" scoped>
.contrainer {
    height: 100%;
}
.el-header {
    background: green;
}

.el-aside {
    background: red;
}
</style>
