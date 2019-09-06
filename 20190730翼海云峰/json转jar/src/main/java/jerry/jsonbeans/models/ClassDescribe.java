package jerry.jsonbeans.models;

import java.util.List;

/**
 * @author Jerry Ou
 * @version 1.0 2016-05-03 13:56
 * @since JDK 1.6
 */
public class ClassDescribe {

    public String packageName;
    public String className;
    public String classType;
    public List<ClassDescribe> classDescribes;

    public ClassDescribe(String packageName, String className,String classType) {
        this.packageName = packageName;
        this.className = className;
        this.classType = classType;
    }
}
