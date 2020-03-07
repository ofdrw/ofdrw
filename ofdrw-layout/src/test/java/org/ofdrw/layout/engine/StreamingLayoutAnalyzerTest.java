package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Test;

import java.util.LinkedList;


class StreamingLayoutAnalyzerTest {

    @Test
    public void seq(){
        LinkedList<Integer> seq = new LinkedList<>();
        seq.add(1);
        seq.add(2);
        seq.add(3);
        seq.add(4);
        seq.push(0);

        System.out.println(seq.pop());
        System.out.println(seq.pop());
        System.out.println(seq.pop());
        System.out.println(seq.pop());
    }
}