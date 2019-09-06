SensitiveHandler.java
	通过传入的json将其解析成对应路径
	如
{
	people:{
	           a:1,
	           b:2
	}

}
能够找到对应路径下 的 root.people.a=1
		root.*.a=1/root.*.b=2