### 🚀 Wechat IM In Android 
* * *
##### 该项目是仿微信7.0实现一个IM APP，APP端基于Kotlin语言以及C++，Server端基于C++，利用Google Databingding Library、Lifecycle、LiveData、RxKotlin、Retrofit、Mars（IM组件）、Muduo（Linux IM组件）、Protobuf 等框架进行开发， 项目包括Android端、Linux服务端。

[![](https://storage.dreambigcareer.com/simple/mars-v1.2.2-red.svg)](https://www.baidu.com) [![](https://storage.dreambigcareer.com/simple/muduo-v2.0.0-blue.svg)](https://www.baidu.com)  [![](https://storage.dreambigcareer.com/simple/mvvm-databinding%20-yellowgreen.svg)](https://www.baidu.com)  [![](https://storage.dreambigcareer.com/simple/kotlin-1.3.11-orange.svg)](https://www.baidu.com) [![](https://storage.dreambigcareer.com/simple/wechat-7.0.0-brightgreen.svg)](https://www.baidu.com) 

#### 项目预览
![23917a9ef79a310b0a564d4dc7484f62.jpeg](en-resource://database/859:0)![11fe0cce24fee545a443ad10413bc848.jpeg](en-resource://database/865:0)![2042a60d77c9e0a300ab4b9d579c62ca.jpeg](en-resource://database/863:0)

#### 项目结构
##### 依赖框架
| 框架名称 |描述  |
| --- | --- |
| Google Databingding Library |基于Mvvm模式开发  |
| Lifecycle | 辅助Mvvm框架 |
| RxKotlin | 优雅的异步处理，以及优化代码逻辑 |
| Retrofit | 处理api |
| LiveData | 处理事件 |
| Mars | 前端通信框架 |
| Muduo | 服务端框架 |
| Protobuf | 超高性能通信协议 |
##### 项目结构
[WechatBySimple](https://github.com/ftylitak/qzxing)：Android端代码

| Module |描述  |
| --- | --- |
| [app](https://github.com/ftylitak/qzxing) |UI层  |
| [imlib](https://github.com/ftylitak/qzxing) | IM封装 |
| [uploadservice](https://github.com/ftylitak/qzxing) | 为imlib提供ftp服务 |
| [wrapper](https://github.com/ftylitak/qzxing) | 利用mars为imlib提供TCP链接|

[IMServer](https://github.com/ftylitak/qzxing)：IM端代码

[IMApiServer](https://github.com/ftylitak/qzxing)：提供基本Api
#### 如何运行部署项目：
##### 1.编译Android端代码
环境要求：
Android studio 3.0以上
##### 2.编译Server IM服务代码
环境要求：
Linux
##### 2.编译Server Api服务代码
环境要求：
Linux
