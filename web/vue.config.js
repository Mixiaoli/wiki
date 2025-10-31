// vue.config.js
module.exports = {
    productionSourceMap: false,//关闭告警

    //下面这个是直接跳过eslintrc警告
    lintOnSave: process.env.NODE_ENV !== 'production',
    devServer: {
        // 注意：属性名是 allowedHosts（复数形式）
        allowedHosts: [
            '.r11.cpolar.top', // 允许所有以 .r11.cpolar.top 结尾的域名
            '31cbc7da.r11.cpolar.top' // 也可以直接指定完整域名
        ],
        host: '0.0.0.0', // 确保监听所有网络接口
        port: 8080,
        compress: true,
    },
    configureWebpack: {
        externals: { echarts: 'echarts' },//压缩包
        performance: {
            //hints: false,                 // 直接关闭提示
            // 或者把阈值调大（可二选一）
            //  maxEntrypointSize: 5 * 1024 * 1024,
            //  maxAssetSize: 3 * 1024 * 1024,
        }
    },
}