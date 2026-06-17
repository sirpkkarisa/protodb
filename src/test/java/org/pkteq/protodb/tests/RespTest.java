package org.pkteq.protodb.tests;


import org.pkteq.protodb.core.Lexer;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class RespTest {
    static void main() {

//        String str1 = "+OK\r\n";
//        System.out.println(decodeString(str1));
//        String str2 = ":0\r\n";
//        System.out.println(decodeString(str2));
////
//        String str3 = ":1000\r\n";
//        System.out.println(decodeString(str3));
//
//        String str4 = "$5\r\nhello\r\n";
//        System.out.println(decodeString(str4));
////
//        String str5 = "$0\r\n\r\n";
//        System.out.println(decodeString(str5));
//
//        String str7 = "*2\r\n$5\r\nhello\r\n$5\r\nworld\r\n";
//        System.out.println(decodeString(str7));
//
//        String str8 = "*3\r\n:1\r\n:2\r\n:3\r\n";
//        System.out.println(decodeString(str8));
//
        String str9 = "*5\r\n:1\r\n:2\r\n:3\r\n:4\r\n$5\r\nhello\r\n";
//        System.out.println(decodeString(str9));
//
        String str10 = "*2\r\n*3\r\n:1\r\n:2\r\n:3\r\n*2\r\n+Hello\r\n-World\r\n";
////
//        Object ob = decodeString(str10);
//        if (ob instanceof List<?> object) {
//            System.out.println(object.getLast());
//        }
//        System.out.println(ob);
    }
}
