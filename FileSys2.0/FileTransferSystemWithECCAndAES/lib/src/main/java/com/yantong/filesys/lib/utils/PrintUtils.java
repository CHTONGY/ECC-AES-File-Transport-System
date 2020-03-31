package com.yantong.filesys.lib.utils;

import java.util.List;

public class PrintUtils {
    public static void printList(List<String> list) {
        if(list.isEmpty()) {
            System.out.println("The list is empty.");
            return;
        }
        for(String str : list) {
            System.out.print(str + " ");
        }
        System.out.println();
    }
}
