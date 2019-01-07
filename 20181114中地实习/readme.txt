spring could项目
	由n个子module boot微服务组成
	其中 agent 作为监控微服务 负责启动其他为服务      ---------通过jar包启动微服务
		负责监控其他微服务的cpu和内存的使用情况 执行定时任务          --------------cpu和内存的检测
		
	
	agent中有新建站点功能， 通过新建站点时将各个微服务信息存储到sqlite数据库中，（如微服务的名称，所在路径，启动端口号，当前运行时的pid，状态等）
		如若要重启或关闭单个微服务的话 就可以通过数据库中的pid信息来执行相应cmd命令 taskkill /f /pid pid来完成相应操作
		
	
	gateway反向代理（通过一个暴露的端口号来进行其他服务的调用）网关微服务
		gateway:
		  default-filters:
		  routes:
		  - id:  cityoms3-server
			uri: http://192.168.12.6:9108
			predicates:
			- Path=/cityoms3/**
		在过滤部分url时 url中包含部分unicode使得请求出错
		通过修改了源码org.springframework.http.server.reactive 包下的ReactorServerHttpRequest类将url中得unicode转换成中文来完成相应得请求操作
		在src目录下新建相同名称的包名和类名， 优先读取src下的类
		
		通过实现GlobalFilter, Ordered接口 来完成gateway 的全局过滤器 过滤url中的其他符号。
	
	db工程
		配置Druid连接池，开启其监控sql功能
		由mogodb数据库实现文件的上传下载功能
	