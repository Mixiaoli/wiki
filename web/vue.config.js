// vue.config.js
module.exports = {
    devServer: {
        // 注意：属性名是 allowedHosts（复数形式）
        allowedHosts: [
            '.r11.cpolar.top', // 允许所有以 .r11.cpolar.top 结尾的域名
            '31cbc7da.r11.cpolar.top' // 也可以直接指定完整域名
        ],
        host: '0.0.0.0', // 确保监听所有网络接口
        port: 8080,
        compress: true,
    }
}