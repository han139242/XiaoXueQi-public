<template>
    <div class="contrainer">
        <div class="header">
            <div class="header-left">登录页面</div>
            <!-- <img class="header-right" src="../../assets/close.svg"/> -->
        </div>
        <div class="form-contrainer">
            <input type="text" name="username" placeholder="请输入用户名">
            <input type="password" name="password" placeholder="请输入密码">
            <button>登录</button>
        </div>

    </div>
</template>

<script>
    export default {
    }
</script>

<style lang="scss" scoped>
    .contrainer {
        width: 600px;
        height: 300px;
        background: white;
        margin: 100px auto;
        border-radius: 20px;
        box-shadow: 0 0 15px #ccc;

    }

    .header {
        display: flex;
        height: 45px;
        justify-content: space-between;
        align-items: center;
        padding: 0 20px;
    }

    // .header-right {
    //     width: 16px;
    //     // 显示小手的样式
    //     cursor: pointer;
    // }

    .form-contrainer {
        display: flex;
        flex-direction: column;
        align-items: stretch;
        padding: 30px;
    }

    .form-contrainer input {
        height: 40px;
        margin: 10px 0;
        padding-left: 10px;
        border-radius: 5px;
        border: 1px solid #ccc;
        // 设置输入框获取焦点时默认的轮廓
        outline: none;
    }

    .form-contrainer button {
        height: 40px;
        background: darkgreen;
        // 将边框去掉
        border: none;
        border-radius: 5px;
        color: white;
        cursor: pointer;
        margin: 15px 0;
    }
</style>
