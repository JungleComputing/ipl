package ibis.ipl.impl.net;

import java.util.HashMap;
import java.util.Vector;

public class NetPropertyTree {

        private HashMap valueMap = null;
        private HashMap levelMap = null;


        protected NetPropertyTree() {
                valueMap = new HashMap();
                levelMap = new HashMap();
        }

        private class Found extends Throwable {
                //
        }

        private String[] split(String s, String chars, int n) {

                Vector    v        = new Vector();
                final int nb_chars = chars.length();
                int       count    = 0;

                int a = 0;
                final int l = s.length();
                
                while ((n<1) || (count++ < n)) {
                        String sub = null;
                        
                        while (a < l) {
                                int b = a;

                                try {
                                        while (b < l) {
                                                final char c = chars.charAt(b);
                                                int  i = 0;

                                                while (i < nb_chars) {
                                                        final char c2 = chars.charAt(i);
                                                
                                                        if (c == c2) {
                                                                throw new Found();
                                                        }

                                                        i++;
                                                }

                                                b++;
                                        }

                                        sub = s.substring(a, b);
                                } catch (Found t) {
                                        if (a == b) {
                                                sub = "";
                                        } else {
                                                sub = s.substring(a, b - 1);
                                        }
                                        b++;
                                }

                                a = b;
                        }

                        v.add(sub);
                }

                int size = v.size();
                
                if (n < 0) {
                        while (size > 0) {
                                String str = (String)v.elementAt(size-1);
                                if (!str.equals("")) {
                                        break;
                                }

                                size--;
                        }
                }

                String[] result = new String[size];

                for (size--; size > 0; size--) {
                        result[size] = (String)v.elementAt(size);
                }

                return result;
        }
        

        protected void put (String name, Object value) {
                //System.err.println("put: "+name+" = "+value);
                String levels  = null;
                String property = null;
                
                {
                        String [] a = name.split(":");
                        
                        if (a.length == 1) {
                                property = a[0];
                        } else if (a.length == 2) {
                                levels   = a[0];
                                property = a[1];
                        } else {
                                throw new Error("invalid property name");
                        }
                }

                //System.err.println("put: levels = "+levels+", property = "+property);
                if (levels != null) {
                        String level     = null;
                        String subLevels = null;

                        {
                                String [] a = levels.split("/", 2);
                                
                                if (a.length == 1) {
                                        level = a[0];
                                } else if (a.length == 2) {
                                        level = a[0];
                                        subLevels = a[1];
                                } else {
                                        throw new Error("invalid state");
                                }
                        }

                        //System.err.println("put: level = "+level+", subLevels = "+subLevels);

                        String context = null;
                        {
                                String [] a = level.split("#");
                                if (a.length == 1) {
                                        level = a[0];
                                } else if (a.length == 2) {
                                        level = a[0];
                                        context = a[1];
                                } else {
                                        throw new Error("invalid property name");
                                }
                        }
                        
                        HashMap contextMap = (HashMap)levelMap.get(level);
                        if (contextMap == null) {
                                contextMap = new HashMap();
                                levelMap.put(level, contextMap);
                        }

                        NetPropertyTree propertyTree = (NetPropertyTree)contextMap.get(context);
                        if (propertyTree == null) {
                                propertyTree = new NetPropertyTree();
                                contextMap.put(context, propertyTree);
                        }
                        
                        if (subLevels != null) {
                                propertyTree.put(subLevels+":"+property, value);
                        } else {
                                propertyTree.put(property, value);
                        }
                } else {
                        valueMap.put(property, value);
                }                
        }
        
        public Object get(String name) {
                //System.err.println("get: "+name);
                String levels  = null;
                String property = null;

                {
                        String [] a = name.split(":");
                        
                        if (a.length == 1) {
                                property = a[0];
                        } else if (a.length == 2) {
                                levels  = a[0];
                                property = a[1];
                        } else {
                                throw new Error("invalid property name");
                        }
                }
                
                if (levels != null) {
                        //
                        String level     = null;
                        String subLevels = null;

                        {
                                String [] a = levels.split("/", 2);
                                
                                if (a.length == 1) {
                                        level = a[0];
                                } else if (a.length == 2) {
                                        level = a[0];
                                        subLevels = a[1];
                                } else {
                                        throw new Error("invalid state");
                                }
                        }

                        Object result = null;
                        
                        do {
                                String context = null;
                                {
                                        String [] a = level.split("#");
                                        if (a.length == 1) {
                                                level = a[0];
                                        } else if (a.length == 2) {
                                                level = a[0];
                                                context = a[1];
                                        } else {
                                                throw new Error("invalid property name");
                                        }
                                }
                        
                                HashMap contextMap = (HashMap)levelMap.get(level);
                                if (contextMap == null) {
                                        break;
                                }

                                NetPropertyTree propertyTree = null;

                                if (context != null) {
                                        propertyTree = (NetPropertyTree)contextMap.get(context);
                                        if (propertyTree != null) {
                                                propertyTree.get(subLevels+":"+property);

                                                if (result != null) {
                                                        return result;
                                                } else {
                                                        propertyTree = null;
                                                }
                                        }

                                        context = null;
                                }

                                propertyTree = (NetPropertyTree)contextMap.get(null);

                                if (propertyTree == null) {
                                        break;
                                } 

                                if (subLevels != null) {
                                        result = propertyTree.get(subLevels+":"+property);
                                } else {
                                        result = propertyTree.get(property);
                                }   
                        }
                        while (false);

                        if (result == null) {
                                if (subLevels != null) {
                                        result = get(subLevels+":"+property);
                                } else {
                                        result = get(property);
                                }
                        }

                        return result;
                } else {
                        return valueMap.get(property);
                }
        }
        
}

