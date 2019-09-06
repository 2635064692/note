#JsonBeans

通过 
    
    mvn clean package 
   
命令打包生成 jar 文件 到 target/JsonBeans; 在 jsonbeans.txt 中配置 json 数据
 
jsonbeans.txt 配置说明:
    
第一行 配置包名和类名
    
    jerry.test AppInfo
    
第二行 开始配置 json 具体数据
        
通过 
       
    java -jar JsonBeans.jar              

执行生成 Java Bean 类    
并得到相应可依赖jar包
1 将json数据转换成java文件
2 将java文件通过javac -cp命令/（-Djava.ext.dirs---- 指定lib路径）将java文件转换成class文件
3 将class文件通过 jar指定操作类转换 打成jar包(可依赖 但不能执行)
