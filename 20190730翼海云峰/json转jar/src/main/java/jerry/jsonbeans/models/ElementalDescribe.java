package jerry.jsonbeans.models;

import java.util.List;
import java.util.Map;

/**
 * @author zhanghai by 2019/8/16
 */
public class ElementalDescribe {
    public String _type;
    public Boolean optional;
    public Object defaultValue;
    public String note;
    public Object inner;


    @Override
    public String toString() {
        return "ElementalDescribe{" +
                "_type='" + _type + '\'' +
                ", optional=" + optional +
                ", defaultValue=" + defaultValue +
                ", note='" + note + '\'' +
                ", inner=" + inner +
                '}';
    }
}
